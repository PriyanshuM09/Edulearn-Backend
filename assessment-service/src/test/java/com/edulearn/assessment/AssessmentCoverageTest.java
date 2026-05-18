package com.edulearn.assessment;

import com.edulearn.assessment.config.RedisConfig;
import com.edulearn.assessment.config.SwaggerConfig;
import com.edulearn.assessment.dto.AttemptRequestDto;
import com.edulearn.assessment.dto.AttemptResponseDto;
import com.edulearn.assessment.dto.QuestionRequestDto;
import com.edulearn.assessment.dto.QuestionResponseDto;
import com.edulearn.assessment.dto.QuizRequestDto;
import com.edulearn.assessment.dto.QuizResponseDto;
import com.edulearn.assessment.entity.Attempt;
import com.edulearn.assessment.entity.Question;
import com.edulearn.assessment.entity.Quiz;
import com.edulearn.assessment.exception.AttemptNotFoundException;
import com.edulearn.assessment.exception.ErrorResponse;
import com.edulearn.assessment.exception.GlobalExceptionHandler;
import com.edulearn.assessment.exception.MaxAttemptsExceededException;
import com.edulearn.assessment.exception.QuestionNotFoundException;
import com.edulearn.assessment.exception.QuizNotFoundException;
import com.edulearn.assessment.mapper.AttemptMapper;
import com.edulearn.assessment.mapper.QuestionMapper;
import com.edulearn.assessment.mapper.QuizMapper;
import com.edulearn.assessment.resource.AssessmentResource;
import com.edulearn.assessment.service.AssessmentService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import com.edulearn.assessment.repository.AttemptRepository;
import com.edulearn.assessment.repository.QuestionRepository;
import com.edulearn.assessment.repository.QuizRepository;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.Optional;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.data.redis.core.HashOperations;

class AssessmentCoverageTest {

    @Test
    void dataObjectsExposeAccessorsBuildersAndObjectMethods() {
        List.of(
                QuizRequestDto.class,
                QuizResponseDto.class,
                QuestionRequestDto.class,
                QuestionResponseDto.class,
                AttemptRequestDto.class,
                AttemptResponseDto.class,
                Quiz.class,
                Question.class,
                Attempt.class,
                ErrorResponse.class
        ).forEach(BeanProbe::exercise);
    }

    @Test
    void exceptionHandlersAndConfigBeansReturnExpectedResponses() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/assessments/1");

        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        assertEquals(404, handler.handleQuizNotFound(new QuizNotFoundException("missing quiz"), request)
                .getBody().getStatus());
        assertEquals(404, handler.handleQuestionNotFound(new QuestionNotFoundException("missing question"), request)
                .getBody().getStatus());
        assertEquals(404, handler.handleAttemptNotFound(new AttemptNotFoundException("missing attempt"), request)
                .getBody().getStatus());
        assertEquals(403, handler.handleMaxAttempts(new MaxAttemptsExceededException("too many"), request)
                .getBody().getStatus());
        assertEquals(400, handler.handleIllegalState(new IllegalStateException("already submitted"), request)
                .getBody().getStatus());
        assertEquals(500, handler.handleGeneral(new RuntimeException("boom"), request)
                .getBody().getStatus());

        assertEquals("Assessment Service API", new SwaggerConfig()
                .assessmentServiceOpenAPI().getInfo().getTitle());
        assertNotNull(new RedisConfig().redisTemplate(mock(RedisConnectionFactory.class)));
    }

    @Test
    void selectedResourceMethodsDelegateToAssessmentService() {
        AssessmentService service = mock(AssessmentService.class);
        AssessmentResource resource = new AssessmentResource(service);
        QuizRequestDto quizRequest = new QuizRequestDto();
        QuestionRequestDto questionRequest = new QuestionRequestDto();
        AttemptRequestDto attemptRequest = new AttemptRequestDto();

        when(service.createQuiz(quizRequest)).thenReturn(new QuizResponseDto());
        when(service.getQuizzesByCourse(2)).thenReturn(List.of(new QuizResponseDto()));
        when(service.updateQuiz(1, quizRequest)).thenReturn(new QuizResponseDto());
        when(service.addQuestion(1, questionRequest)).thenReturn(new QuestionResponseDto());
        when(service.submitAttempt(5, attemptRequest)).thenReturn(new AttemptResponseDto());
        when(service.getBestScore(4, 1)).thenReturn(88);

        assertEquals(201, resource.createQuiz(quizRequest).getStatusCode().value());
        assertEquals(1, resource.getQuizzesByCourse(2).getBody().size());
        assertEquals(200, resource.updateQuiz(1, quizRequest).getStatusCode().value());
        assertEquals(201, resource.addQuestion(1, questionRequest).getStatusCode().value());
        assertEquals(200, resource.submitAttempt(5, attemptRequest).getStatusCode().value());
        assertEquals(88, resource.getBestScore(4, 1).getBody().get("bestScore"));

        // New Resource Method Coverage
        when(service.publishQuiz(1)).thenReturn(new QuizResponseDto());
        when(service.getQuestionsByQuiz(1)).thenReturn(List.of(new QuestionResponseDto()));
        when(service.getAttemptsByStudent(2)).thenReturn(List.of(new AttemptResponseDto()));
        when(service.getAttemptsByStudentAndQuiz(2, 1)).thenReturn(List.of(new AttemptResponseDto()));
        when(service.getBestAttempt(2, 1)).thenReturn(new AttemptResponseDto());
        when(service.getRemainingTime(2, 5)).thenReturn(Map.of("remainingSeconds", 300L));

        assertEquals(200, resource.publishQuiz(1).getStatusCode().value());
        assertEquals(200, resource.getQuestions(1).getStatusCode().value());
        assertEquals(200, resource.getAttemptsByStudent(2).getStatusCode().value());
        assertEquals(200, resource.getAttemptsByStudentAndQuiz(2, 1).getStatusCode().value());
        assertEquals(200, resource.getBestAttempt(2, 1).getStatusCode().value());
        assertEquals(300L, resource.getRemainingTime(5, 2).getBody().get("remainingSeconds"));

        resource.deleteQuiz(1);
        resource.deleteQuestion(1);
        resource.startAttempt(2, 1);
        resource.saveProgress(5, 2, Map.of(1, "A"));
    }

    @Test
    void applicationMainDelegatesToSpringApplication() {
        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            String[] args = {"--test"};
            AssessmentServiceApplication.main(args);
            spring.verify(() -> SpringApplication.run(AssessmentServiceApplication.class, args));
        }
    }

    @Test
    void complexServiceLogicCoversRedisAndGrading() {
        QuizRepository quizRepo = mock(QuizRepository.class);
        QuestionRepository questionRepo = mock(QuestionRepository.class);
        AttemptRepository attemptRepo = mock(AttemptRepository.class);
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        HashOperations hashOps = mock(HashOperations.class);
        QuizMapper quizMapper = mock(QuizMapper.class);
        QuestionMapper questionMapper = mock(QuestionMapper.class);
        AttemptMapper attemptMapper = mock(AttemptMapper.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(attemptMapper.toDto(any(Attempt.class))).thenAnswer(invocation -> {
            Attempt attempt = invocation.getArgument(0);
            AttemptResponseDto dto = new AttemptResponseDto();
            dto.setAttemptId(attempt.getAttemptId());
            dto.setQuizId(attempt.getQuizId());
            dto.setStudentId(attempt.getStudentId());
            dto.setScore(attempt.getScore());
            dto.setEarnedMarks(attempt.getEarnedMarks());
            dto.setTotalMarks(attempt.getTotalMarks());
            dto.setPassed(attempt.getPassed());
            dto.setStartedAt(attempt.getStartedAt());
            dto.setSubmittedAt(attempt.getSubmittedAt());
            dto.setAnswers(attempt.getAnswers());
            dto.setIsSubmitted(attempt.getIsSubmitted());
            return dto;
        });

        AssessmentService service = new com.edulearn.assessment.service.impl.AssessmentServiceImpl(
                quizRepo, questionRepo, attemptRepo,
                quizMapper,
                questionMapper,
                attemptMapper,
                redisTemplate
        );

        Quiz quiz = Quiz.builder().quizId(1).maxAttempts(2).passingScore(70).timeLimitMinutes(10).build();
        Question q1 = Question.builder().questionId(101).correctAnswer("Java").marks(10).build();
        when(quizRepo.findById(1)).thenReturn(Optional.of(quiz));
        when(questionRepo.findByQuizQuizIdOrderByOrderIndex(1)).thenReturn(List.of(q1));

        // Test startAttempt (New)
        when(attemptRepo.findFirstByStudentIdAndQuizIdAndIsSubmittedFalse(2, 1)).thenReturn(Optional.empty());
        when(attemptRepo.countByStudentIdAndQuizId(2, 1)).thenReturn(0L);
        when(attemptRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        assertNotNull(service.startAttempt(2, 1));

        // Test startAttempt (Resume)
        Attempt active = Attempt.builder().attemptId(5).studentId(2).quizId(1).startedAt(LocalDateTime.now().minusMinutes(5)).isSubmitted(false).build();
        when(attemptRepo.findFirstByStudentIdAndQuizIdAndIsSubmittedFalse(2, 1)).thenReturn(Optional.of(active));
        when(redisTemplate.hasKey(anyString())).thenReturn(false); // Trigger restore
        service.startAttempt(2, 1);
        verify(hashOps, atLeastOnce()).putAll(anyString(), any(Map.class));

        // Test submitAttempt
        when(attemptRepo.findById(5)).thenReturn(Optional.of(active));
        AttemptRequestDto submitReq = new AttemptRequestDto();
        submitReq.setAnswers(Map.of(101, "java ")); // Test trim and ignore case
        AttemptResponseDto result = service.submitAttempt(5, submitReq);
        assertEquals(100, result.getScore());
        assertTrue(result.getPassed());

        // Test getRemainingTime
        when(redisTemplate.getExpire(anyString(), any())).thenReturn(300L);
        Map<String, Object> time = service.getRemainingTime(2, 5);
        assertEquals(300L, time.get("remainingSeconds"));

        // Test saveProgress
        when(redisTemplate.hasKey(anyString())).thenReturn(true);
        service.saveProgress(2, 5, Map.of(101, "Python"));
        verify(hashOps, times(1)).put(anyString(), eq("answers"), any());

        // Test Auto-grading variations
        AttemptRequestDto wrongReq = new AttemptRequestDto();
        wrongReq.setAnswers(Map.of(101, "C++"));
        active.setIsSubmitted(false);
        AttemptResponseDto wrongResult = service.submitAttempt(5, wrongReq);
        assertEquals(0, wrongResult.getScore());
        assertFalse(wrongResult.getPassed());
        
        // Test Max attempts
        when(attemptRepo.findFirstByStudentIdAndQuizIdAndIsSubmittedFalse(2, 1)).thenReturn(Optional.empty());
        when(attemptRepo.countByStudentIdAndQuizId(2, 1)).thenReturn(2L);
        try {
            service.startAttempt(2, 1);
        } catch (com.edulearn.assessment.exception.MaxAttemptsExceededException expected) {
            // Expected branch for the max-attempt guard.
        }
    }

    private static final class BeanProbe {
        private BeanProbe() {
        }

        static void exercise(Class<?> type) {
            Object bean = instantiate(type);
            writeAndReadFields(bean);
            invokeObjectMethods(bean);
            exerciseBuilder(type);
        }

        private static Object instantiate(Class<?> type) {
            try {
                Constructor<?> ctor = type.getDeclaredConstructor();
                ctor.setAccessible(true);
                return ctor.newInstance();
            } catch (ReflectiveOperationException ignored) {
                try {
                    Constructor<?> ctor = type.getDeclaredConstructors()[0];
                    ctor.setAccessible(true);
                    Object[] args = new Object[ctor.getParameterCount()];
                    Class<?>[] parameterTypes = ctor.getParameterTypes();
                    for (int i = 0; i < parameterTypes.length; i++) {
                        args[i] = sample(parameterTypes[i]);
                    }
                    return ctor.newInstance(args);
                } catch (ReflectiveOperationException ex) {
                    throw new AssertionError("Cannot instantiate " + type.getName(), ex);
                }
            }
        }

        private static void writeAndReadFields(Object bean) {
            for (Field field : bean.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                Object value = sample(field.getType());
                invokeIfPresent(bean, setterName(field), field.getType(), value);
                invokeIfPresent(bean, getterName(field), null);
                invokeIfPresent(bean, booleanGetterName(field), null);
            }
        }

        private static void exerciseBuilder(Class<?> type) {
            try {
                Method builderMethod = type.getMethod("builder");
                Object builder = builderMethod.invoke(null);
                for (Field field : type.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    invokeIfPresent(builder, field.getName(), field.getType(), sample(field.getType()));
                }
                Object built = builder.getClass().getMethod("build").invoke(builder);
                writeAndReadFields(built);
                invokeObjectMethods(built);
            } catch (NoSuchMethodException ignored) {
                return;
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError("Cannot exercise builder for " + type.getName(), ex);
            }
        }

        private static void invokeObjectMethods(Object bean) {
            assertNotNull(bean.toString());
            assertEquals(true, bean.equals(bean));
            assertFalse(equalsNull(bean));
            assertNotEquals(bean, new Object());
            bean.hashCode();

            Object same = instantiate(bean.getClass());
            writeAndReadFields(same);
            assertEquals(bean, same);
            assertEquals(bean.hashCode(), same.hashCode());

            for (Field field : bean.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) {
                    continue;
                }

                Object left = instantiate(bean.getClass());
                Object right = instantiate(bean.getClass());
                writeAndReadFields(left);
                writeAndReadFields(right);

                invokeIfPresent(left, setterName(field), field.getType(), (Object) null);
                assertNotEquals(left, right);

                invokeIfPresent(right, setterName(field), field.getType(), (Object) null);
                assertEquals(left, right);
                left.hashCode();
                right.hashCode();
            }
        }

        private static boolean equalsNull(Object bean) {
            try {
                Method equalsMethod = bean.getClass().getMethod("equals", Object.class);
                return (boolean) equalsMethod.invoke(bean, new Object[] { null });
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError("Cannot invoke equals on " + bean.getClass().getName(), ex);
            }
        }

        private static Object invokeIfPresent(Object target, String methodName, Class<?> parameterType, Object... args) {
            try {
                Method method = parameterType == null
                        ? target.getClass().getMethod(methodName)
                        : target.getClass().getMethod(methodName, parameterType);
                method.setAccessible(true);
                return method.invoke(target, args);
            } catch (NoSuchMethodException ignored) {
                return null;
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError("Cannot invoke " + methodName + " on " + target.getClass().getName(), ex);
            }
        }

        private static Object sample(Class<?> type) {
            if (type == String.class) {
                return "value";
            }
            if (type == Integer.class || type == int.class) {
                return 1;
            }
            if (type == Long.class || type == long.class) {
                return 1L;
            }
            if (type == Double.class || type == double.class) {
                return 1.5d;
            }
            if (type == Boolean.class || type == boolean.class) {
                return true;
            }
            if (type == LocalDateTime.class) {
                return LocalDateTime.of(2026, 1, 1, 12, 0);
            }
            if (List.class.isAssignableFrom(type)) {
                return new ArrayList<>();
            }
            if (Map.class.isAssignableFrom(type)) {
                return new LinkedHashMap<>();
            }
            if (type.getName().startsWith("com.edulearn.assessment")) {
                return instantiate(type);
            }
            return null;
        }

        private static String setterName(Field field) {
            return "set" + capitalize(field.getName());
        }

        private static String getterName(Field field) {
            return "get" + capitalize(field.getName());
        }

        private static String booleanGetterName(Field field) {
            return "is" + capitalize(field.getName());
        }

        private static String capitalize(String value) {
            return Character.toUpperCase(value.charAt(0)) + value.substring(1);
        }
    }
}
