package com.edulearn.lesson;

import com.edulearn.lesson.config.SwaggerConfig;
import com.edulearn.lesson.dto.LessonRequestDto;
import com.edulearn.lesson.dto.LessonResponseDto;
import com.edulearn.lesson.dto.ResourceRequestDto;
import com.edulearn.lesson.dto.ResourceResponseDto;
import com.edulearn.lesson.exception.ErrorResponse;
import com.edulearn.lesson.exception.GlobalExceptionHandler;
import com.edulearn.lesson.exception.LessonNotFoundException;
import com.edulearn.lesson.exception.ResourceNotFoundException;
import com.edulearn.lesson.resource.LessonResource;
import com.edulearn.lesson.service.LessonService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class LessonCoverageTest {

    @Test
    void dataObjectsCoverAccessorsBuildersAndEqualityBranches() {
        List.of(
                LessonRequestDto.class,
                LessonResponseDto.class,
                ResourceRequestDto.class,
                ResourceResponseDto.class,
                com.edulearn.lesson.entity.Lesson.class,
                com.edulearn.lesson.entity.Resource.class,
                ErrorResponse.class
        ).forEach(BeanProbe::exercise);
    }

    @Test
    void resourceMethodsDelegateToService() {
        LessonService service = mock(LessonService.class);
        LessonResource resource = new LessonResource(service);
        LessonRequestDto lessonRequest = new LessonRequestDto();
        ResourceRequestDto resourceRequest = new ResourceRequestDto();

        when(service.addLesson(lessonRequest)).thenReturn(new LessonResponseDto());
        when(service.getLessonsByCourse(1)).thenReturn(List.of(new LessonResponseDto()));
        when(service.getLessonById(2)).thenReturn(new LessonResponseDto());
        when(service.updateLesson(2, lessonRequest)).thenReturn(new LessonResponseDto());
        when(service.addResource(2, resourceRequest)).thenReturn(new ResourceResponseDto());
        when(service.getPreviewLessons(1)).thenReturn(List.of(new LessonResponseDto()));

        assertEquals(201, resource.addLesson(lessonRequest).getStatusCode().value());
        assertEquals(1, resource.getLessonsByCourse(1).getBody().size());
        assertEquals(200, resource.getLessonById(2).getStatusCode().value());
        assertEquals(200, resource.updateLesson(2, lessonRequest).getStatusCode().value());
        assertEquals(204, resource.deleteLesson(2).getStatusCode().value());
        assertEquals(200, resource.reorderLessons(1, List.of(2, 3)).getStatusCode().value());
        assertEquals(201, resource.addResource(2, resourceRequest).getStatusCode().value());
        assertEquals(204, resource.removeResource(2, 4).getStatusCode().value());
        assertEquals(1, resource.getPreviewLessons(1).getBody().size());
    }

    @Test
    void exceptionHandlersConfigAndMainAreCovered() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/lessons/99");

        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        assertEquals(404, handler.handleLessonNotFound(new LessonNotFoundException(99), request)
                .getBody().getStatus());
        assertEquals(404, handler.handleResourceNotFound(new ResourceNotFoundException(44), request)
                .getBody().getStatus());
        assertEquals(500, handler.handleGeneric(new RuntimeException("boom"), request)
                .getBody().getStatus());

        assertNotNull(new SwaggerConfig().lessonServiceOpenAPI().getInfo().getTitle());

        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            String[] args = {"--test"};
            LessonServiceApplication.main(args);
            spring.verify(() -> SpringApplication.run(LessonServiceApplication.class, args));
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
                // Some DTOs/entities do not use Lombok builders.
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
                if (left.equals(right)) {
                    left.hashCode();
                }
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
            if (type.getName().startsWith("com.edulearn.lesson")) {
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
