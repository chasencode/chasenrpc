package util;

import java.lang.reflect.Method;
import java.util.Arrays;

public class MethodUtils {

    public static boolean checkLocalMethod(final String method) {
        return "toString".equals(method) ||
                "hashCode".equals(method) ||
                "notifyAll".equals(method) ||
                "equals".equals(method) ||
                "wait".equals(method) ||
                "getClass".equals(method) ||
                "notify".equals(method);
    }

    public static boolean checkLocalMethod(final Method method) {
        return method.getDeclaringClass().equals(Object.class);
    }

    public static String methodSign(Method method) {
        StringBuilder sb = new StringBuilder(method.getName());
        int parameterCount = method.getParameterCount();
        if (parameterCount > 0) {
            sb.append("@").append(parameterCount);
            Arrays.stream(method.getParameterTypes()).forEach(
                    c-> sb.append("_").append(c.getCanonicalName())
            );
        }
        return sb.toString();
    }


}
