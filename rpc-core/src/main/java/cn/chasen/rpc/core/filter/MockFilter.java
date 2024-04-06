package cn.chasen.rpc.core.filter;

import cn.chasen.rpc.core.api.Filter;
import cn.chasen.rpc.core.api.RpcRequest;
import cn.chasen.rpc.core.api.RpcResponse;
import cn.chasen.rpc.core.util.MethodUtils;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @program: chasenrpc
 * @description: MockFilter
 * @author: Chasen
 * @create: 2024-04-06 22:15
 **/
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
