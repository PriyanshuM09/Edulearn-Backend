package com.edulearn.course;

import com.edulearn.course.config.RedisConfig;
import com.edulearn.course.config.RestTemplateConfig;
import com.edulearn.course.config.SwaggerConfig;
import com.edulearn.course.dto.CourseRequestDto;
import com.edulearn.course.dto.CourseResponseDto;
import com.edulearn.course.dto.NotificationRequestDto;
import com.edulearn.course.dto.RejectionRequest;
import com.edulearn.course.dto.ReviewDto;
import com.edulearn.course.dto.UserDto;
import com.edulearn.course.dto.UserProfileDto;
import com.edulearn.course.entity.Course;
import com.edulearn.course.entity.Review;
import com.edulearn.course.exception.CourseNotFoundException;
import com.edulearn.course.exception.ErrorResponse;
import com.edulearn.course.exception.GlobalExceptionHandler;
import com.edulearn.course.resource.CourseResource;
import com.edulearn.course.resource.ReviewResource;
import com.edulearn.course.service.CourseService;
import com.edulearn.course.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class CourseCoverageTest {

    @Test
    void dataObjectsCoverAccessorsBuildersAndEqualityBranches() {
        List.of(
                CourseRequestDto.class,
                CourseResponseDto.class,
                NotificationRequestDto.class,
                RejectionRequest.class,
                UserDto.class,
                UserProfileDto.class,
                ReviewDto.class,
                Course.class,
                Review.class,
                ErrorResponse.class
        ).forEach(BeanProbe::exercise);
    }

    @Test
    void resourceMethodsDelegateToService() {
        CourseService service = mock(CourseService.class);
        CourseResource resource = new CourseResource(service);
        CourseRequestDto request = new CourseRequestDto();
        RejectionRequest rejection = new RejectionRequest();

        when(service.createCourse(request)).thenReturn(new CourseResponseDto());
        when(service.getAllCourses()).thenReturn(List.of(new CourseResponseDto()));
        when(service.getCourseById(1)).thenReturn(new CourseResponseDto());
        when(service.getCoursesByInstructor(2)).thenReturn(List.of(new CourseResponseDto()));
        when(service.updateCourse(1, request)).thenReturn(new CourseResponseDto());
        when(service.submitForReview(1)).thenReturn(new CourseResponseDto());
        when(service.unpublishCourse(1)).thenReturn(new CourseResponseDto());
        when(service.approveCourse(1)).thenReturn(new CourseResponseDto());
        when(service.rejectCourse(1, rejection)).thenReturn(new CourseResponseDto());
        when(service.getPendingCourses()).thenReturn(List.of(new CourseResponseDto()));
        when(service.getAllCoursesForAdmin()).thenReturn(List.of(new CourseResponseDto()));
        when(service.searchCourses("java")).thenReturn(List.of(new CourseResponseDto()));
        when(service.getCoursesByCategory("dev")).thenReturn(List.of(new CourseResponseDto()));

        assertEquals(201, resource.createCourse(request).getStatusCode().value());
        assertEquals(1, resource.getAllCourses().getBody().size());
        assertEquals(200, resource.getCourseById(1).getStatusCode().value());
        assertEquals(1, resource.getCoursesByInstructor(2).getBody().size());
        assertEquals(200, resource.updateCourse(1, request).getStatusCode().value());
        assertEquals(204, resource.deleteCourse(1).getStatusCode().value());
        assertEquals(200, resource.submitForReview(1).getStatusCode().value());
        assertEquals(200, resource.unpublishCourse(1).getStatusCode().value());
        assertEquals(200, resource.approveCourse(1).getStatusCode().value());
        assertEquals(200, resource.rejectCourse(1, rejection).getStatusCode().value());
        assertEquals(1, resource.getPendingCourses().getBody().size());
        assertEquals(1, resource.getAllCoursesForAdmin().getBody().size());
        assertEquals(1, resource.searchCourses("java").getBody().size());
        assertEquals(1, resource.getByCategory("dev").getBody().size());
    }

    @Test
    void reviewResourceMethodsDelegateToService() {
        ReviewService service = mock(ReviewService.class);
        ReviewResource resource = new ReviewResource(service);
        ReviewDto review = new ReviewDto();

        when(service.addReview(review)).thenReturn(review);
        when(service.getReviewsByCourseId(1)).thenReturn(List.of(review));

        assertEquals(201, resource.addReview(review).getStatusCode().value());
        assertEquals(1, resource.getReviews(1).getBody().size());
    }

    @Test
    void exceptionHandlersConfigAndMainAreCovered() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/courses/99");

        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        assertEquals(404, handler.handleCourseNotFound(new CourseNotFoundException(99), request)
                .getBody().getStatus());
        assertEquals(500, handler.handleGeneric(new RuntimeException("boom"), request)
                .getBody().getStatus());

        assertNotNull(new SwaggerConfig().courseServiceOpenAPI().getInfo().getTitle());
        assertNotNull(new RedisConfig().redisTemplate(mock(RedisConnectionFactory.class)));
        assertNotNull(new RestTemplateConfig().restTemplate());

        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            String[] args = {"--test"};
            CourseServiceApplication.main(args);
            spring.verify(() -> SpringApplication.run(CourseServiceApplication.class, args));
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
                invokeIfPresent(bean, setterName(field), field.getType(), sample(field.getType()));
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
            if (type == LocalDate.class) {
                return LocalDate.of(2026, 1, 1);
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
            if (type.getName().startsWith("com.edulearn.course")) {
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
