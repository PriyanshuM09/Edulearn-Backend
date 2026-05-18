package com.edulearn.progress;

import com.edulearn.progress.config.RestTemplateConfig;
import com.edulearn.progress.config.SwaggerConfig;
import com.edulearn.progress.dto.CertificateResponseDto;
import com.edulearn.progress.dto.ProgressResponseDto;
import com.edulearn.progress.dto.WatchProgressRequest;
import com.edulearn.progress.dto.external.CourseDto;
import com.edulearn.progress.dto.external.EnrollmentDto;
import com.edulearn.progress.dto.external.LessonDto;
import com.edulearn.progress.dto.external.UserProfileDto;
import com.edulearn.progress.entity.Certificate;
import com.edulearn.progress.entity.Progress;
import com.edulearn.progress.exception.CertificateNotFoundException;
import com.edulearn.progress.exception.ErrorResponse;
import com.edulearn.progress.exception.GlobalExceptionHandler;
import com.edulearn.progress.exception.ProgressNotFoundException;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ProgressCoverageTest {

    @Test
    void dataObjectsCoverAccessorsBuildersAndObjectMethods() {
        List.of(
                WatchProgressRequest.class,
                ProgressResponseDto.class,
                CertificateResponseDto.class,
                CourseDto.class,
                EnrollmentDto.class,
                LessonDto.class,
                UserProfileDto.class,
                Progress.class,
                Certificate.class,
                ErrorResponse.class
        ).forEach(type -> BeanProbe.exercise(type, "com.edulearn.progress"));
    }

    @Test
    void exceptionHandlersConfigAndMainAreCovered() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/progress/1");

        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        assertEquals(404, handler.handleProgressNotFound(new ProgressNotFoundException(1), request)
                .getBody().getStatus());
        assertEquals(404, handler.handleCertificateNotFound(new CertificateNotFoundException("missing"), request)
                .getBody().getStatus());
        assertEquals(500, handler.handleGeneric(new RuntimeException("boom"), request)
                .getBody().getStatus());

        RestTemplateConfig rest = new RestTemplateConfig();
        assertNotNull(rest.restTemplate());
        assertNotNull(rest.directRestTemplate());
        assertNotNull(new SwaggerConfig().progressServiceOpenAPI().getInfo().getTitle());

        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            String[] args = {"--test"};
            ProgressServiceApplication.main(args);
            spring.verify(() -> SpringApplication.run(ProgressServiceApplication.class, args));
        }
    }

    private static final class BeanProbe {
        private BeanProbe() {
        }
        static void exercise(Class<?> type, String packagePrefix) {
            Object bean = instantiate(type, packagePrefix);
            writeAndReadFields(bean, packagePrefix);
            exerciseBuilder(type, packagePrefix);
            invokeObjectMethods(bean, packagePrefix);
        }
        private static Object instantiate(Class<?> type, String packagePrefix) {
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
                    for (int i = 0; i < parameterTypes.length; i++) args[i] = sample(parameterTypes[i], packagePrefix);
                    return ctor.newInstance(args);
                } catch (ReflectiveOperationException ex) {
                    throw new AssertionError("Cannot instantiate " + type.getName(), ex);
                }
            }
        }
        private static void writeAndReadFields(Object bean, String packagePrefix) {
            for (Field field : bean.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                invokeIfPresent(bean, setterName(field), field.getType(), sample(field.getType(), packagePrefix));
                invokeIfPresent(bean, getterName(field), null);
                invokeIfPresent(bean, booleanGetterName(field), null);
            }
        }
        private static void exerciseBuilder(Class<?> type, String packagePrefix) {
            try {
                Method builderMethod = type.getMethod("builder");
                Object builder = builderMethod.invoke(null);
                Object defaultBuilt = builder.getClass().getMethod("build").invoke(builder);
                invokeObjectMethods(defaultBuilt, packagePrefix);
                for (Field field : type.getDeclaredFields()) {
                    if (!Modifier.isStatic(field.getModifiers())) invokeIfPresent(builder, field.getName(), field.getType(), sample(field.getType(), packagePrefix));
                }
                Object built = builder.getClass().getMethod("build").invoke(builder);
                writeAndReadFields(built, packagePrefix);
                invokeObjectMethods(built, packagePrefix);
            } catch (NoSuchMethodException ignored) {
                // Some DTOs/entities do not use Lombok builders.
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError("Cannot exercise builder for " + type.getName(), ex);
            }
        }
        private static void invokeObjectMethods(Object bean, String packagePrefix) {
            assertNotNull(bean.toString());
            assertEquals(true, bean.equals(bean));
            assertFalse(equalsNull(bean));
            assertNotEquals(bean, new Object());
            bean.hashCode();
            Object same = instantiate(bean.getClass(), packagePrefix);
            writeAndReadFields(same, packagePrefix);
            if (bean.equals(same)) {
                same.hashCode();
            }
            same.hashCode();
            for (Field field : bean.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) {
                    continue;
                }
                Object left = instantiate(bean.getClass(), packagePrefix);
                Object right = instantiate(bean.getClass(), packagePrefix);
                writeAndReadFields(left, packagePrefix);
                writeAndReadFields(right, packagePrefix);
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
        private static Object invokeIfPresent(Object target, String methodName, Class<?> parameterType, Object... args) {
            try {
                Method method = parameterType == null ? target.getClass().getMethod(methodName) : target.getClass().getMethod(methodName, parameterType);
                method.setAccessible(true);
                return method.invoke(target, args);
            } catch (NoSuchMethodException ignored) {
                return null;
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError("Cannot invoke " + methodName, ex);
            }
        }

        private static boolean equalsNull(Object bean) {
            try {
                return (Boolean) bean.getClass()
                        .getMethod("equals", Object.class)
                        .invoke(bean, new Object[]{null});
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError("Cannot invoke equals", ex);
            }
        }
        private static Object sample(Class<?> type, String packagePrefix) {
            if (type == String.class) return "value";
            if (type == Integer.class || type == int.class) return 1;
            if (type == Double.class || type == double.class) return 1.5d;
            if (type == Boolean.class || type == boolean.class) return true;
            if (type == LocalDateTime.class) return LocalDateTime.of(2026, 1, 1, 12, 0);
            if (List.class.isAssignableFrom(type)) return new ArrayList<>();
            if (type.getName().startsWith(packagePrefix)) return instantiate(type, packagePrefix);
            return null;
        }
        private static String setterName(Field field) { return "set" + capitalize(field.getName()); }
        private static String getterName(Field field) { return "get" + capitalize(field.getName()); }
        private static String booleanGetterName(Field field) { return "is" + capitalize(field.getName()); }
        private static String capitalize(String value) { return Character.toUpperCase(value.charAt(0)) + value.substring(1); }
    }
}
