package cn.chasen.rpc.core.provider;

import cn.chasen.rpc.core.api.RpcContext;
import cn.chasen.rpc.core.api.RpcRequest;
import cn.chasen.rpc.core.api.RpcResponse;
import cn.chasen.rpc.core.exception.RpcException;
import cn.chasen.rpc.core.meta.ProviderMeta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.MultiValueMap;
import cn.chasen.rpc.core.util.TypeUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
@Slf4j
public class ProviderInvoker {

    private MultiValueMap<String, ProviderMeta> skeleton;

    public ProviderInvoker(ProviderBootstrap providerBootstrap) {
        this.skeleton = providerBootstrap.getSkeleton();
    }

    public RpcResponse<Object> invoke(RpcRequest request) {
        String methodSign = request.getMethodSign();
        log.debug(" ===> ProviderInvoker.invoke(request:{})", request);
        // 将consumer 端传递过来的额外信息，写入到RPC 山下文中
        if(!request.getParams().isEmpty()) {
            request.getParams().forEach(RpcContext::setContextParameter);
        }
        List<ProviderMeta> providerMetaList = skeleton.get(request.getService());

        RpcResponse<Object> rpcResponse = new RpcResponse<Object>();
        try {
            ProviderMeta meta = findProviderMeta(providerMetaList, methodSign);
            if (meta == null) {
                throw new RuntimeException(request.getService() + " not find");
            }
            Method method = meta.getMethod();
            Object[] args = processArgs(request.getArgs(), method.getParameterTypes());
            Object result = method.invoke(meta.getServiceImpl(), args);
            rpcResponse.setStatus(true);
            rpcResponse.setData(result);
        } catch (InvocationTargetException e) {
            rpcResponse.setEx(new RpcException(e.getTargetException().getMessage()));
        } catch (IllegalAccessException e) {
            rpcResponse.setEx(new RpcException(   e.getMessage()));
        } finally {
            // 去除上下文中 要传递的参数
            RpcContext.ContextParameters.get().clear();
        }
        return rpcResponse;
    }

    private Object[] processArgs(Object[] args, Class<?>[] parameterTypes) {
        if (args == null || args.length == 0) {
            return args;
        }
        Object[] actuals = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            actuals[i] = TypeUtils.cast(args[i], parameterTypes[i]);
        }
        return actuals;
    }

    private ProviderMeta findProviderMeta(List<ProviderMeta> providerMetaList, String methodSign) {
        return providerMetaList.stream().filter(
                x -> x.getMethodSign().equals(methodSign)
        ).findFirst().orElse(null);
    }
}
