package com.edulearn.auth;

import com.edulearn.auth.config.SwaggerConfig;
import com.edulearn.auth.dto.ForgotPasswordRequest;
import com.edulearn.auth.dto.GoogleLoginRequest;
import com.edulearn.auth.dto.LoginRequest;
import com.edulearn.auth.dto.LoginResponse;
import com.edulearn.auth.dto.RegisterRequest;
import com.edulearn.auth.dto.ResetPasswordRequest;
import com.edulearn.auth.dto.UpdateProfileRequest;
import com.edulearn.auth.entity.User;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mockStatic;

class AuthCoverageTest {

    @Test
    void swaggerConfigBuildsAuthApiMetadata() {
        assertEquals("Auth Service API", new SwaggerConfig().authServiceOpenAPI().getInfo().getTitle());
    }

    @Test
    void mainDelegatesToSpringApplication() {
        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            String[] args = {"--test"};
            AuthServiceApplication.main(args);
            spring.verify(() -> SpringApplication.run(AuthServiceApplication.class, args));
        }
    }

    @Test
    void dataObjectsExposeAccessorsConstructorsAndObjectMethods() {
        List.of(
                ForgotPasswordRequest.class,
                GoogleLoginRequest.class,
                LoginRequest.class,
                LoginResponse.class,
                RegisterRequest.class,
                ResetPasswordRequest.class,
                UpdateProfileRequest.class,
                User.class
        ).forEach(BeanProbe::exercise);
    }

    @Test
    void userPrePersistSetsCreatedAt() throws Exception {
        User user = new User();
        Method onCreate = User.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);

        onCreate.invoke(user);

        assertNotNull(user.getCreatedAt());
    }

    private static final class BeanProbe {
        private BeanProbe() {
        }

        static void exercise(Class<?> type) {
            Object bean = instantiate(type);
            writeAndReadFields(bean);
            invokeObjectMethods(bean);
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
                    Class<?>[] parameterTypes = ctor.getParameterTypes();
                    Object[] args = new Object[parameterTypes.length];
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
                return (Boolean) bean.getClass()
                        .getMethod("equals", Object.class)
                        .invoke(bean, new Object[] { null });
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError("Cannot invoke equals", ex);
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
            if (type == int.class || type == Integer.class) {
                return 1;
            }
            if (type == long.class || type == Long.class) {
                return 1L;
            }
            if (type == boolean.class || type == Boolean.class) {
                return true;
            }
            if (type == LocalDateTime.class) {
                return LocalDateTime.of(2026, 1, 1, 12, 0);
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
