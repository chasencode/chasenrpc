package filter;

import api.Filter;
import api.RpcRequest;
import api.RpcResponse;
import util.MethodUtils;

import java.lang.reflect.Method;
import java.util.Arrays;

public class MockFilter implements Filter {
    @Override
    public Object prefilter(RpcRequest request) {
        final Method method = getMethod(request);
        final Class<?> declaringClass = method.getDeclaringClass();
        return null;
    }

    @Override
    public Object postfilter(RpcRequest request, RpcResponse response, Object result) {
        return null;
    }

    private Method getMethod(RpcRequest request) {
        try {
            final Class<?> aClass = Class.forName(request.getService());
            return Arrays.stream(aClass.getMethods()).
                    filter(method -> !MethodUtils.checkLocalMethod(method))
                    .filter(method -> MethodUtils.methodSign(method).equals(request.getMethodSign()))
                    .findFirst()
                    .orElse(null);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
