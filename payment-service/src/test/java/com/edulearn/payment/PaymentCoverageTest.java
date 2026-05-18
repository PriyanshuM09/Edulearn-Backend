package com.edulearn.payment;

import com.edulearn.payment.config.RabbitMQConfig;
import com.edulearn.payment.config.SwaggerConfig;
import com.edulearn.payment.dto.PaymentRequestDto;
import com.edulearn.payment.dto.PaymentResponseDto;
import com.edulearn.payment.dto.PaymentVerifyDto;
import com.edulearn.payment.dto.RefundRequestDto;
import com.edulearn.payment.dto.SubscriptionRequestDto;
import com.edulearn.payment.dto.SubscriptionResponseDto;
import com.edulearn.payment.dto.WalletResponseDto;
import com.edulearn.payment.dto.WalletTransactionDto;
import com.edulearn.payment.entity.Payment;
import com.edulearn.payment.entity.RefundRequest;
import com.edulearn.payment.entity.Subscription;
import com.edulearn.payment.entity.Wallet;
import com.edulearn.payment.entity.WalletTransaction;
import com.edulearn.payment.event.PaymentEvent;
import com.edulearn.payment.exception.DuplicatePaymentException;
import com.edulearn.payment.exception.ErrorResponse;
import com.edulearn.payment.exception.GlobalExceptionHandler;
import com.edulearn.payment.exception.PaymentNotFoundException;
import com.edulearn.payment.exception.SubscriptionNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
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

class PaymentCoverageTest {

    @Test
    void dataObjectsCoverAccessorsBuildersAndObjectMethods() {
        List.of(
                PaymentRequestDto.class,
                PaymentResponseDto.class,
                PaymentVerifyDto.class,
                RefundRequestDto.class,
                SubscriptionRequestDto.class,
                SubscriptionResponseDto.class,
                WalletResponseDto.class,
                WalletTransactionDto.class,
                Payment.class,
                RefundRequest.class,
                Subscription.class,
                Wallet.class,
                WalletTransaction.class,
                PaymentEvent.class,
                ErrorResponse.class
        ).forEach(type -> BeanProbe.exercise(type, "com.edulearn.payment"));
    }

    @Test
    void exceptionHandlersConfigAndMainAreCovered() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/payments/1");

        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        assertEquals(404, handler.handlePaymentNotFound(new PaymentNotFoundException(1), request)
                .getBody().getStatus());
        assertEquals(404, handler.handleSubscriptionNotFound(new SubscriptionNotFoundException("missing"), request)
                .getBody().getStatus());
        assertEquals(409, handler.handleDuplicatePayment(new DuplicatePaymentException(1, 2), request)
                .getBody().getStatus());
        assertEquals(500, handler.handleGeneric(new RuntimeException("boom"), request)
                .getBody().getStatus());

        RabbitMQConfig rabbit = new RabbitMQConfig();
        assertEquals("edulearn.exchange", rabbit.exchange().getName());
        assertNotNull(rabbit.jsonMessageConverter());
        assertNotNull(rabbit.amqpTemplate(mock(ConnectionFactory.class)));
        assertNotNull(new SwaggerConfig().paymentServiceOpenAPI().getInfo().getTitle());

        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            String[] args = {"--test"};
            PaymentServiceApplication.main(args);
            spring.verify(() -> SpringApplication.run(PaymentServiceApplication.class, args));
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
