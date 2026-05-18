package com.edulearn.assessment.service.impl;

import com.edulearn.assessment.dto.QuizRequestDto;
import com.edulearn.assessment.dto.QuizResponseDto;
import com.edulearn.assessment.dto.QuestionRequestDto;
import com.edulearn.assessment.dto.QuestionResponseDto;
import com.edulearn.assessment.dto.AttemptRequestDto;
import com.edulearn.assessment.dto.AttemptResponseDto;
import com.edulearn.assessment.entity.Quiz;
import com.edulearn.assessment.entity.Question;
import com.edulearn.assessment.entity.Attempt;
import com.edulearn.assessment.exception.AttemptNotFoundException;
import com.edulearn.assessment.exception.MaxAttemptsExceededException;
import com.edulearn.assessment.exception.QuestionNotFoundException;
import com.edulearn.assessment.exception.QuizNotFoundException;
import com.edulearn.assessment.mapper.QuizMapper;
import com.edulearn.assessment.repository.AttemptRepository;
import com.edulearn.assessment.repository.QuestionRepository;
import com.edulearn.assessment.repository.QuizRepository;
import com.edulearn.assessment.mapper.QuestionMapper;
import com.edulearn.assessment.mapper.AttemptMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceImplTest {

    @Mock
    private QuizRepository quizRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private AttemptRepository attemptRepository;
    @Mock
    private QuizMapper quizMapper;
    @Mock
    private QuestionMapper questionMapper;
    @Mock
    private AttemptMapper attemptMapper;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private AssessmentServiceImpl assessmentService;

    @Test
    @DisplayName("Test Create Quiz - Success")
    void createQuiz_Success() {
        // given
        QuizRequestDto request = new QuizRequestDto();
        request.setCourseId(1);
        request.setTitle("Java Quiz");

        Quiz quiz = new Quiz();
        quiz.setQuizId(1);
        QuizResponseDto response = new QuizResponseDto();
        response.setQuizId(1);

        when(quizMapper.toEntity(any(QuizRequestDto.class))).thenReturn(quiz);
        when(quizRepository.save(any(Quiz.class))).thenReturn(quiz);
        when(quizMapper.toDto(any(Quiz.class))).thenReturn(response);

        // when
        QuizResponseDto result = assessmentService.createQuiz(request);

        // then
        assertNotNull(result);
        assertEquals(1, result.getQuizId());
        verify(quizRepository, times(1)).save(any(Quiz.class));
    }

    @Test
    @DisplayName("Test Get Quiz By ID - Success")
    void getQuizById_Success() {
        // given
        Integer id = 1;
        Quiz quiz = new Quiz();
        QuizResponseDto response = new QuizResponseDto();

        when(quizRepository.findById(id)).thenReturn(Optional.of(quiz));
        when(quizMapper.toDto(quiz)).thenReturn(response);

        // when
        QuizResponseDto result = assessmentService.getQuizById(id);

        // then
        assertNotNull(result);
    }

    @Test
    @DisplayName("Test Get Quiz By ID - Not Found")
    void getQuizById_NotFound() {
        // given
        Integer id = 99;
        when(quizRepository.findById(id)).thenReturn(Optional.empty());

        // when & then
        assertThrows(QuizNotFoundException.class, () -> assessmentService.getQuizById(id));
    }

    @Test
    @DisplayName("Test Publish Quiz - Success")
    void publishQuiz_Success() {
        // given
        Integer id = 1;
        Quiz quiz = new Quiz();
        quiz.setQuizId(id);
        quiz.setIsPublished(false);
        QuizResponseDto response = new QuizResponseDto();

        when(quizRepository.findById(id)).thenReturn(Optional.of(quiz));
        when(quizRepository.save(any(Quiz.class))).thenReturn(quiz);
        when(quizMapper.toDto(any(Quiz.class))).thenReturn(response);

        // when
        QuizResponseDto result = assessmentService.publishQuiz(id);

        // then
        assertNotNull(result);
        assertTrue(quiz.getIsPublished());
        verify(quizRepository, times(1)).save(quiz);
    }

    @Test
    @DisplayName("Test Quiz Update/List/Delete Operations")
    void quizOperations_CoverBranches() {
        Quiz quiz = quiz(1);
        QuizRequestDto request = quizRequest();
        QuizResponseDto response = new QuizResponseDto();

        when(quizRepository.findByCourseId(10)).thenReturn(List.of(quiz));
        when(quizMapper.toDto(quiz)).thenReturn(response);
        assertEquals(1, assessmentService.getQuizzesByCourse(10).size());

        when(quizRepository.findById(1)).thenReturn(Optional.of(quiz));
        when(quizRepository.save(quiz)).thenReturn(quiz);
        assertSame(response, assessmentService.updateQuiz(1, request));
        assertEquals("Updated Quiz", quiz.getTitle());

        assessmentService.deleteQuiz(1);
        verify(quizRepository).delete(quiz);
    }

    @Test
    @DisplayName("Test Question Operations")
    void questionOperations_CoverBranches() {
        Quiz quiz = quiz(1);
        Question question = question(3);
        QuestionRequestDto request = questionRequest();
        QuestionResponseDto response = new QuestionResponseDto();

        when(quizRepository.findById(1)).thenReturn(Optional.of(quiz));
        when(questionMapper.toEntity(request)).thenReturn(question);
        when(questionRepository.save(question)).thenReturn(question);
        when(questionMapper.toDto(question)).thenReturn(response);
        assertSame(response, assessmentService.addQuestion(1, request));
        assertSame(quiz, question.getQuiz());

        when(questionRepository.findByQuizQuizIdOrderByOrderIndex(1)).thenReturn(List.of(question));
        assertEquals(1, assessmentService.getQuestionsByQuiz(1).size());

        when(questionRepository.findById(3)).thenReturn(Optional.of(question));
        assertSame(response, assessmentService.updateQuestion(3, request));
        assertEquals("Updated question?", question.getText());

        assessmentService.deleteQuestion(3);
        verify(questionRepository).delete(question);
    }

    @Test
    @DisplayName("Test Question Not Found")
    void questionOperations_NotFound() {
        when(questionRepository.findById(55)).thenReturn(Optional.empty());

        assertThrows(QuestionNotFoundException.class,
                () -> assessmentService.updateQuestion(55, questionRequest()));
    }

    @Test
    @DisplayName("Test Start Attempt - New Attempt")
    void startAttempt_NewAttempt_StoresTimer() {
        Quiz quiz = quiz(1);
        Attempt saved = attempt(6, 2, 1);
        AttemptResponseDto response = new AttemptResponseDto();

        when(quizRepository.findById(1)).thenReturn(Optional.of(quiz));
        when(attemptRepository.findFirstByStudentIdAndQuizIdAndIsSubmittedFalse(2, 1))
                .thenReturn(Optional.empty());
        when(attemptRepository.countByStudentIdAndQuizId(2, 1)).thenReturn(0L);
        when(attemptRepository.save(any(Attempt.class))).thenReturn(saved);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.expire(anyString(), anyLong(), eq(TimeUnit.MINUTES))).thenReturn(true);
        when(attemptMapper.toDto(saved)).thenReturn(response);

        assertSame(response, assessmentService.startAttempt(2, 1));
        verify(hashOperations).putAll(eq("quiz_timer:2:6"), any(Map.class));
    }

    @Test
    @DisplayName("Test Start Attempt - Active Attempt Restores Timer")
    void startAttempt_ActiveAttempt_RestoresTimer() {
        Quiz quiz = quiz(1);
        Attempt active = attempt(6, 2, 1);
        active.setStartedAt(LocalDateTime.now().minusMinutes(1));
        AttemptResponseDto response = new AttemptResponseDto();

        when(quizRepository.findById(1)).thenReturn(Optional.of(quiz));
        when(attemptRepository.findFirstByStudentIdAndQuizIdAndIsSubmittedFalse(2, 1))
                .thenReturn(Optional.of(active));
        when(redisTemplate.hasKey("quiz_timer:2:6")).thenReturn(false);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.expire(eq("quiz_timer:2:6"), anyLong(), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(attemptMapper.toDto(active)).thenReturn(response);

        assertSame(response, assessmentService.startAttempt(2, 1));
        verify(hashOperations).putAll(eq("quiz_timer:2:6"), any(Map.class));
    }

    @Test
    @DisplayName("Test Start Attempt - Max Attempts Exceeded")
    void startAttempt_MaxAttemptsExceeded() {
        Quiz quiz = quiz(1);
        quiz.setMaxAttempts(1);

        when(quizRepository.findById(1)).thenReturn(Optional.of(quiz));
        when(attemptRepository.findFirstByStudentIdAndQuizIdAndIsSubmittedFalse(2, 1))
                .thenReturn(Optional.empty());
        when(attemptRepository.countByStudentIdAndQuizId(2, 1)).thenReturn(1L);

        assertThrows(MaxAttemptsExceededException.class,
                () -> assessmentService.startAttempt(2, 1));
    }

    @Test
    @DisplayName("Test Submit Attempt - Grades Passing Attempt")
    void submitAttempt_GradesPassingAttempt() {
        Quiz quiz = quiz(1);
        quiz.setPassingScore(60);
        Attempt attempt = attempt(6, 2, 1);
        Question question = question(3);
        question.setCorrectAnswer("A");
        question.setMarks(4);
        AttemptResponseDto response = new AttemptResponseDto();
        AttemptRequestDto request = new AttemptRequestDto();
        request.setAnswers(Map.of(3, " a "));

        when(attemptRepository.findById(6)).thenReturn(Optional.of(attempt));
        when(quizRepository.findById(1)).thenReturn(Optional.of(quiz));
        when(redisTemplate.hasKey("quiz_timer:2:6")).thenReturn(true);
        when(questionRepository.findByQuizQuizIdOrderByOrderIndex(1)).thenReturn(List.of(question));
        when(attemptRepository.save(attempt)).thenReturn(attempt);
        when(redisTemplate.delete("quiz_timer:2:6")).thenReturn(true);
        when(attemptMapper.toDto(attempt)).thenReturn(response);

        AttemptResponseDto result = assessmentService.submitAttempt(6, request);

        assertTrue(attempt.getPassed());
        assertEquals(100, attempt.getScore());
        assertTrue(result.getResultMessage().contains("Congratulations"));
    }

    @Test
    @DisplayName("Test Submit Attempt - Already Submitted")
    void submitAttempt_AlreadySubmitted() {
        Attempt attempt = attempt(6, 2, 1);
        attempt.setIsSubmitted(true);
        when(attemptRepository.findById(6)).thenReturn(Optional.of(attempt));

        assertThrows(IllegalStateException.class,
                () -> assessmentService.submitAttempt(6, new AttemptRequestDto()));
    }

    @Test
    @DisplayName("Test Attempt Query Operations")
    void attemptQueries_CoverBranches() {
        Attempt attempt = attempt(6, 2, 1);
        AttemptResponseDto response = new AttemptResponseDto();

        when(attemptRepository.findByStudentId(2)).thenReturn(List.of(attempt));
        when(attemptMapper.toDto(attempt)).thenReturn(response);
        assertEquals(1, assessmentService.getAttemptsByStudent(2).size());

        when(attemptRepository.findByStudentIdAndQuizId(2, 1)).thenReturn(List.of(attempt));
        assertEquals(1, assessmentService.getAttemptsByStudentAndQuiz(2, 1).size());

        when(attemptRepository.findTopByStudentIdAndQuizIdOrderByScoreDesc(2, 1))
                .thenReturn(Optional.of(attempt));
        assertSame(response, assessmentService.getBestAttempt(2, 1));
        assertEquals(attempt.getScore(), assessmentService.getBestScore(2, 1));

        when(attemptRepository.findTopByStudentIdAndQuizIdOrderByScoreDesc(9, 1))
                .thenReturn(Optional.empty());
        assertThrows(AttemptNotFoundException.class,
                () -> assessmentService.getBestAttempt(9, 1));
        assertEquals(0, assessmentService.getBestScore(9, 1));
    }

    private Quiz quiz(Integer id) {
        Quiz quiz = new Quiz();
        quiz.setQuizId(id);
        quiz.setCourseId(10);
        quiz.setLessonId(20);
        quiz.setTitle("Java Quiz");
        quiz.setDescription("Basics");
        quiz.setTimeLimitMinutes(10);
        quiz.setPassingScore(60);
        quiz.setMaxAttempts(3);
        quiz.setIsPublished(false);
        return quiz;
    }

    private QuizRequestDto quizRequest() {
        QuizRequestDto request = new QuizRequestDto();
        request.setTitle("Updated Quiz");
        request.setDescription("Updated");
        request.setTimeLimitMinutes(15);
        request.setPassingScore(65);
        request.setMaxAttempts(2);
        return request;
    }

    private Question question(Integer id) {
        Question question = new Question();
        question.setQuestionId(id);
        question.setText("Question?");
        question.setType("MCQ");
        question.setOptions(List.of("A", "B"));
        question.setCorrectAnswer("A");
        question.setMarks(1);
        question.setOrderIndex(0);
        return question;
    }

    private QuestionRequestDto questionRequest() {
        QuestionRequestDto request = new QuestionRequestDto();
        request.setText("Updated question?");
        request.setType("MCQ");
        request.setOptions(List.of("A", "B"));
        request.setCorrectAnswer("B");
        request.setMarks(2);
        request.setOrderIndex(1);
        return request;
    }

    private Attempt attempt(Integer id, Integer studentId, Integer quizId) {
        Attempt attempt = new Attempt();
        attempt.setAttemptId(id);
        attempt.setStudentId(studentId);
        attempt.setQuizId(quizId);
        attempt.setStartedAt(LocalDateTime.now());
        attempt.setScore(75);
        attempt.setEarnedMarks(3);
        attempt.setTotalMarks(4);
        attempt.setPassed(false);
        attempt.setIsSubmitted(false);
        return attempt;
    }
}
