package cn.chasen.rpc.core.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class MockUtils {

    public static Object mock(Class type) {
        if (type.equals(Integer.class) || type.equals(Integer.TYPE)) {
            return 1;
        } else if (type.equals(Long.class) || type.equals(Long.TYPE)) {
            return 10000L;
        }
        if (Number.class.isAssignableFrom(type)) {
            return 1;
        }
        if (type.equals(String.class)) {
            return "this is a mock string";
        }
        return mockPojo(type);
    }

    private static Object mockPojo(Class type) {
        try {
            // 考虑递归
            Object result = type.getDeclaredConstructor().newInstance();
            final Field[] declaredFields = type.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                declaredField.setAccessible(true);
                final Class<?> fType = declaredField.getType();
                Object fValue = mock(fType);
                declaredField.set(result, fValue);
            }
            return result;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
