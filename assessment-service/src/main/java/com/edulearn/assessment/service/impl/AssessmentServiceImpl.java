package com.edulearn.assessment.service.impl;

import com.edulearn.assessment.dto.*;
import com.edulearn.assessment.entity.*;
import com.edulearn.assessment.exception.*;
import com.edulearn.assessment.mapper.*;
import com.edulearn.assessment.repository.*;
import com.edulearn.assessment.service.AssessmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AssessmentServiceImpl implements AssessmentService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final AttemptRepository attemptRepository;
    private final QuizMapper quizMapper;
    private final QuestionMapper questionMapper;
    private final AttemptMapper attemptMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String TIMER_KEY_PREFIX = "quiz_timer:";
    private static final int DEFAULT_TIME_LIMIT = 15;

    // ── Quiz Operations ───────────────────────────────────────────

    @Override
    @Transactional
    public QuizResponseDto createQuiz(QuizRequestDto request) {
        log.info("Creating quiz for course: {}", request.getCourseId());
        Quiz quiz = quizMapper.toEntity(request);
        return quizMapper.toDto(quizRepository.save(quiz));
    }

    @Override
    public QuizResponseDto getQuizById(Integer quizId) {
        return quizMapper.toDto(findQuizById(quizId));
    }

    @Override
    public List<QuizResponseDto> getQuizzesByCourse(
            Integer courseId) {
        log.info("Fetching quizzes for course: {}", courseId);
        return quizRepository.findByCourseId(courseId)
                .stream()
                .map(quizMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public QuizResponseDto updateQuiz(Integer quizId,
            QuizRequestDto request) {
        log.info("Updating quiz: {}", quizId);
        Quiz quiz = findQuizById(quizId);
        quiz.setTitle(request.getTitle());
        quiz.setDescription(request.getDescription());
        quiz.setTimeLimitMinutes(request.getTimeLimitMinutes());
        quiz.setPassingScore(request.getPassingScore());
        quiz.setMaxAttempts(request.getMaxAttempts());
        return quizMapper.toDto(quizRepository.save(quiz));
    }

    @Override
    @Transactional
    public QuizResponseDto publishQuiz(Integer quizId) {
        log.info("Publishing quiz: {}", quizId);
        Quiz quiz = findQuizById(quizId);
        quiz.setIsPublished(true);
        return quizMapper.toDto(quizRepository.save(quiz));
    }

    @Override
    @Transactional
    public void deleteQuiz(Integer quizId) {
        log.info("Deleting quiz: {}", quizId);
        Quiz quiz = findQuizById(quizId);
        quizRepository.delete(quiz);
    }

    // ── Question Operations ───────────────────────────────────────

    @Override
    @Transactional
    public QuestionResponseDto addQuestion(Integer quizId,
            QuestionRequestDto request) {
        log.info("Adding question to quiz: {}", quizId);
        Quiz quiz = findQuizById(quizId);
        Question question = questionMapper.toEntity(request);
        question.setQuiz(quiz);
        return questionMapper.toDto(
                questionRepository.save(question));
    }

    @Override
    public List<QuestionResponseDto> getQuestionsByQuiz(
            Integer quizId) {
        return questionRepository
                .findByQuizQuizIdOrderByOrderIndex(quizId)
                .stream()
                .map(questionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public QuestionResponseDto updateQuestion(Integer questionId,
            QuestionRequestDto request) {
        log.info("Updating question: {}", questionId);
        Question question = findQuestionById(questionId);
        question.setText(request.getText());
        question.setType(request.getType());
        question.setOptions(request.getOptions());
        question.setCorrectAnswer(request.getCorrectAnswer());
        question.setMarks(request.getMarks());
        question.setOrderIndex(request.getOrderIndex());
        return questionMapper.toDto(
                questionRepository.save(question));
    }

    @Override
    @Transactional
    public void deleteQuestion(Integer questionId) {
        log.info("Deleting question: {}", questionId);
        questionRepository.delete(findQuestionById(questionId));
    }

    // ── Attempt Operations ────────────────────────────────────────

    @Override
    @Transactional
    public AttemptResponseDto startAttempt(Integer studentId,
            Integer quizId) {
        log.info("Student {} starting quiz {}", studentId, quizId);

        Quiz quiz = findQuizById(quizId);

        // CHECK: Is there already an active (unsubmitted) attempt?
        Optional<Attempt> activeAttempt = attemptRepository
                .findFirstByStudentIdAndQuizIdAndIsSubmittedFalse(studentId, quizId);

        if (activeAttempt.isPresent()) {
            Attempt attempt = activeAttempt.get();
            log.info("Resuming active attempt {} for student {}", attempt.getAttemptId(), studentId);
            
            // Ensure Redis timer is present
            ensureTimerInRedis(attempt, quiz);

            return attemptMapper.toDto(attempt);
        }

        // Check max attempts (only for NEW attempts)
        long attemptCount = attemptRepository
                .countByStudentIdAndQuizId(studentId, quizId);

        if (attemptCount >= quiz.getMaxAttempts()) {
            throw new MaxAttemptsExceededException(
                    "Maximum attempts (" + quiz.getMaxAttempts() +
                            ") reached for this quiz");
        }

        LocalDateTime startedAt = LocalDateTime.now();
        Attempt attempt = Attempt.builder()
                .quizId(quizId)
                .studentId(studentId)
                .startedAt(startedAt)
                .score(0)
                .passed(false)
                .isSubmitted(false)
                .build();

        Attempt saved = attemptRepository.save(attempt);

        // Store timer in Redis
        try {
            int limit = getQuizTimeLimit(quiz);
            String redisKey = TIMER_KEY_PREFIX + studentId + ":" + saved.getAttemptId();
            Map<String, String> timerData = new HashMap<>();
            timerData.put("attemptId", String.valueOf(saved.getAttemptId()));
            timerData.put("quizId", String.valueOf(quizId));
            timerData.put("studentId", String.valueOf(studentId));
            timerData.put("startedAt", startedAt.toString());
            timerData.put("timeLimitMinutes", String.valueOf(limit));
            timerData.put("expiresAt", startedAt.plusMinutes(limit).toString());

            redisTemplate.opsForHash().putAll(redisKey, timerData);
            redisTemplate.expire(redisKey, limit, TimeUnit.MINUTES);
            log.info("Timer stored in Redis: {}", redisKey);
        } catch (Exception e) {
            log.error("Failed to store timer in Redis: {}", e.getMessage());
        }

        return attemptMapper.toDto(saved);
    }

    @Override
    @Transactional
    public AttemptResponseDto submitAttempt(Integer attemptId,
            AttemptRequestDto request) {
        log.info("Submitting attempt: {}", attemptId);

        Attempt attempt = findAttemptById(attemptId);

        if (attempt.getIsSubmitted()) {
            throw new IllegalStateException(
                    "Attempt already submitted");
        }

        Quiz quiz = findQuizById(attempt.getQuizId());

        // Check Redis for timer key
        String redisKey = TIMER_KEY_PREFIX + attempt.getStudentId() + ":" + attemptId;
        try {
            Boolean hasTimer = redisTemplate.hasKey(redisKey);
            if (Boolean.FALSE.equals(hasTimer)) {
                log.warn("Timer expired for attempt: {}", attemptId);
            }
        } catch (Exception e) {
            log.error("Redis check failed in submitAttempt: {}", e.getMessage());
            // Fallback to MySQL check if Redis is down
            int limit = getQuizTimeLimit(quiz);
            LocalDateTime deadline = attempt.getStartedAt().plusMinutes(limit);
            if (LocalDateTime.now().isAfter(deadline)) {
                log.warn("Attempt {} auto-submitted due to timer expiry (MySQL fallback)", attemptId);
            }
        }

        // Save student answers
        attempt.setAnswers(request.getAnswers());
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setIsSubmitted(true);

        // Auto-grade
        int[] grades = autoGradeDetailed(quiz, request.getAnswers());
        int earnedMarks = grades[0];
        int totalMarks = grades[1];
        int percentage = totalMarks > 0 ? (int) Math.round((earnedMarks * 100.0) / totalMarks) : 0;

        attempt.setEarnedMarks(earnedMarks);
        attempt.setTotalMarks(totalMarks);
        attempt.setScore(percentage);

        // Check if passed
        boolean passed = percentage >= quiz.getPassingScore();
        attempt.setPassed(passed);

        Attempt saved = attemptRepository.save(attempt);

        // DELETE timer from Redis
        try {
            redisTemplate.delete(redisKey);
            log.info("Timer deleted from Redis: {}", redisKey);
        } catch (Exception e) {
            log.error("Failed to delete timer from Redis: {}", e.getMessage());
        }

        AttemptResponseDto response = attemptMapper.toDto(saved);
        response.setResultMessage(passed
                ? "Congratulations! You passed with " + percentage + "%"
                : "You scored " + percentage + "%. Passing score is "
                        + quiz.getPassingScore() + "%");

        return response;
    }

    @Override
    public Map<String, Object> getRemainingTime(Integer studentId, Integer attemptId) {
        String redisKey = TIMER_KEY_PREFIX + studentId + ":" + attemptId;
        Map<String, Object> result = new HashMap<>();

        try {
            Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);

            if (ttl == null || ttl <= 0) {
                // Try to restore from DB if it was just a Redis cache miss
                Attempt attempt = findAttemptById(attemptId);
                if (!attempt.getIsSubmitted()) {
                    Quiz quiz = findQuizById(attempt.getQuizId());
                    long remaining = ensureTimerInRedis(attempt, quiz);
                    if (remaining > 0) {
                        ttl = remaining;
                    }
                }
            }

            if (ttl == null || ttl <= 0) {
                result.put("remainingSeconds", 0);
                result.put("expired", true);
                result.put("message", "Time is up! Quiz auto-submitted.");
            } else {
                result.put("remainingSeconds", ttl);
                result.put("expired", false);
                result.put("attemptId", attemptId);

                // Fetch answers from Redis if they exist
                Object savedAnswers = redisTemplate.opsForHash().get(redisKey, "answers");
                if (savedAnswers != null) {
                    result.put("answers", savedAnswers);
                }
            }
        } catch (Exception e) {
            log.error("Redis failed in getRemainingTime: {}", e.getMessage());
            // Minimal fallback
            result.put("remainingSeconds", -1); // Unknown
            result.put("expired", false);
            result.put("error", "Could not fetch remaining time from cache.");
        }

        return result;
    }

    @Override
    @Transactional
    public void saveProgress(Integer studentId, Integer attemptId, Map<Integer, String> answers) {
        log.info("Saving progress for student {} attempt {}", studentId, attemptId);
        String redisKey = TIMER_KEY_PREFIX + studentId + ":" + attemptId;
        
        try {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
                redisTemplate.opsForHash().put(redisKey, "answers", answers);
                log.debug("Progress updated in Redis for attempt {}", attemptId);
            } else {
                log.warn("Cannot save progress, timer key missing in Redis for attempt {}", attemptId);
                // Optional: Restore key if missing? Or just let it be.
            }
        } catch (Exception e) {
            log.error("Failed to save progress in Redis: {}", e.getMessage());
        }
    }

    @Override
    public List<AttemptResponseDto> getAttemptsByStudent(
            Integer studentId) {
        return attemptRepository.findByStudentId(studentId)
                .stream()
                .map(attemptMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AttemptResponseDto> getAttemptsByStudentAndQuiz(
            Integer studentId, Integer quizId) {
        return attemptRepository
                .findByStudentIdAndQuizId(studentId, quizId)
                .stream()
                .map(attemptMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public AttemptResponseDto getBestAttempt(Integer studentId,
            Integer quizId) {
        Attempt best = attemptRepository
                .findTopByStudentIdAndQuizIdOrderByScoreDesc(
                        studentId, quizId)
                .orElseThrow(() -> new AttemptNotFoundException(
                        "No attempts found for student " + studentId +
                                " on quiz " + quizId));
        return attemptMapper.toDto(best);
    }

    @Override
    public Integer getBestScore(Integer studentId,
            Integer quizId) {
        return attemptRepository
                .findTopByStudentIdAndQuizIdOrderByScoreDesc(
                        studentId, quizId)
                .map(Attempt::getScore)
                .orElse(0);
    }

    // ── Private Helpers ───────────────────────────────────────────

    private Quiz findQuizById(Integer quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new QuizNotFoundException(
                        "Quiz not found with ID: " + quizId));
    }

    private Question findQuestionById(Integer questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(
                        "Question not found with ID: " + questionId));
    }

    private Attempt findAttemptById(Integer attemptId) {
        return attemptRepository.findById(attemptId)
                .orElseThrow(() -> new AttemptNotFoundException(
                        "Attempt not found with ID: " + attemptId));
    }

    private long ensureTimerInRedis(Attempt attempt, Quiz quiz) {
        String redisKey = TIMER_KEY_PREFIX + attempt.getStudentId() + ":" + attempt.getAttemptId();
        try {
            if (Boolean.FALSE.equals(redisTemplate.hasKey(redisKey))) {
                // Calculate remaining time using a more robust method
                // We use LocalDateTime but we must be careful about timezone mismatches between app and DB
                // Since application.properties specifies serverTimezone=UTC, let's assume UTC for safety if local fails
                
                LocalDateTime now = LocalDateTime.now();
                long elapsedSeconds = java.time.Duration.between(attempt.getStartedAt(), now).getSeconds();
                
                int limit = getQuizTimeLimit(quiz);
                long limitSeconds = limit * 60L;

                // If elapsed is negative or absurdly large (e.g. > limit + 1 hour), it's likely a Timezone mismatch
                // (For example, IST is +5:30 -> 19800 seconds difference)
                if (elapsedSeconds < 0 || elapsedSeconds > limitSeconds + 3600) {
                    log.warn("Timezone mismatch detected? Elapsed: {}s. Attempting UTC fallback.", elapsedSeconds);
                    now = LocalDateTime.now(java.time.ZoneOffset.UTC);
                    elapsedSeconds = java.time.Duration.between(attempt.getStartedAt(), now).getSeconds();
                }

                long remainingSeconds = limitSeconds - elapsedSeconds;

                if (remainingSeconds > 0) {
                    Map<String, Object> timerData = new HashMap<>();
                    timerData.put("attemptId", String.valueOf(attempt.getAttemptId()));
                    timerData.put("quizId", String.valueOf(quiz.getQuizId()));
                    timerData.put("studentId", String.valueOf(attempt.getStudentId()));
                    timerData.put("startedAt", attempt.getStartedAt().toString());
                    timerData.put("timeLimitMinutes", String.valueOf(limit));
                    timerData.put("expiresAt", attempt.getStartedAt().plusMinutes(limit).toString());
                    
                    // Restore answers from DB if any
                    if (attempt.getAnswers() != null && !attempt.getAnswers().isEmpty()) {
                        timerData.put("answers", attempt.getAnswers());
                    }

                    redisTemplate.opsForHash().putAll(redisKey, timerData);
                    redisTemplate.expire(redisKey, remainingSeconds, TimeUnit.SECONDS);
                    log.info("Restored timer in Redis: {}s remaining", remainingSeconds);
                    return remainingSeconds;
                } else {
                    log.info("Attempt {} is already expired. Elapsed: {}s", attempt.getAttemptId(), elapsedSeconds);
                    return 0;
                }
            } else {
                return redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.error("Failed to ensure timer in Redis: {}", e.getMessage());
            return -1;
        }
    }

    private int getQuizTimeLimit(Quiz quiz) {
        return quiz.getTimeLimitMinutes() != null ? quiz.getTimeLimitMinutes() : DEFAULT_TIME_LIMIT;
    }

    private int[] autoGradeDetailed(Quiz quiz,
            java.util.Map<Integer, String> answers) {
        if (answers == null || answers.isEmpty()) {
            List<Question> questions = questionRepository
                    .findByQuizQuizIdOrderByOrderIndex(quiz.getQuizId());
            int totalMarks = questions.stream().mapToInt(Question::getMarks).sum();
            return new int[] { 0, totalMarks };
        }

        List<Question> questions = questionRepository
                .findByQuizQuizIdOrderByOrderIndex(quiz.getQuizId());

        int totalMarks = questions.stream()
                .mapToInt(Question::getMarks)
                .sum();

        if (totalMarks == 0)
            return new int[] { 0, 0 };

        int earnedMarks = 0;
        for (Question question : questions) {
            String studentAnswer = answers.get(
                    question.getQuestionId());
            if (studentAnswer != null &&
                    studentAnswer.trim().equalsIgnoreCase(
                            question.getCorrectAnswer().trim())) {
                earnedMarks += question.getMarks();
            }
        }

        return new int[] { earnedMarks, totalMarks };
    }
}