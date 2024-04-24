package cn.chasen.rpc.core.provider;

import cn.chasen.rpc.core.api.RpcContext;
import cn.chasen.rpc.core.api.RpcRequest;
import cn.chasen.rpc.core.api.RpcResponse;
import cn.chasen.rpc.core.config.ProviderProperties;
import cn.chasen.rpc.core.exception.RpcException;
import cn.chasen.rpc.core.meta.ProviderMeta;
import cn.chasen.rpc.core.util.SlidingTimeWindow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.MultiValueMap;
import cn.chasen.rpc.core.util.TypeUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.chasen.rpc.core.exception.RpcException.ExceedLimitEx;

@Slf4j
public class ProviderInvoker {

    private MultiValueMap<String, ProviderMeta> skeleton;

    final Map<String, SlidingTimeWindow> windows = new HashMap<>();

    final ProviderProperties providerProperties;

    public ProviderInvoker(ProviderBootstrap providerBootstrap) {
        this.skeleton = providerBootstrap.getSkeleton();
        this.providerProperties = providerBootstrap.getProviderProperties();
    }

    public RpcResponse<Object> invoke(RpcRequest request) {
        log.debug(" ===> ProviderInvoker.invoke(request:{})", request);
        String methodSign = request.getMethodSign();
        // 将consumer 端传递过来的额外信息，写入到RPC 上下文中 如：traceId
        if(!request.getParams().isEmpty()) {
            request.getParams().forEach(RpcContext::setContextParameter);
        }
        String service = request.getService();
        // 限流
        int trafficControl = Integer.parseInt(providerProperties.getMetas().getOrDefault("tc", "20"));
        log.debug(" ===>> trafficControl:{} for {}", trafficControl, service);
        synchronized (windows) {
            SlidingTimeWindow window = windows.computeIfAbsent(service, k -> new SlidingTimeWindow());
            if (window.calcSum() >= trafficControl) {
                log.info(" ===>> trafficControl limit reached for {}", service);
                throw new RpcException("service " + service + " invoked in 30s/[" +
                        window.getSum() + "] larger than tpsLimit = " + trafficControl, ExceedLimitEx);
            }
            window.record(System.currentTimeMillis());
        }
        List<ProviderMeta> providerMetaList = skeleton.get(request.getService());
        RpcResponse<Object> rpcResponse = new RpcResponse<Object>();
        try {
            ProviderMeta meta = findProviderMeta(providerMetaList, methodSign);
            if (meta == null) {
                throw new RuntimeException(request.getService() + " not find");
            }
            Method method = meta.getMethod();
            Object[] args = processArgs(request.getArgs(), method.getParameterTypes(), method.getGenericParameterTypes());
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

    private Object[] processArgs(Object[] args, Class<?>[] parameterTypes, Type[] genericParameterTypes) {
        if (args == null || args.length == 0) {
            return args;
        }
        Object[] actuals = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            actuals[i] = TypeUtils.castGeneric(args[i], parameterTypes[i], genericParameterTypes[i]);
        }
        return actuals;
    }

    private ProviderMeta findProviderMeta(List<ProviderMeta> providerMetaList, String methodSign) {
        return providerMetaList.stream().filter(
                x -> x.getMethodSign().equals(methodSign)
        ).findFirst().orElse(null);
    }
}
