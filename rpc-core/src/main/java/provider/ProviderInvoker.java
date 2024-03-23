package provider;

import api.RpcRequest;
import api.RpcResponse;
import meta.ProviderMeta;
import org.springframework.util.MultiValueMap;
import util.TypeUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class ProviderInvoker {

    private MultiValueMap<String, ProviderMeta> skeleton;

    public ProviderInvoker(ProviderBootstrap providerBootstrap) {
        this.skeleton = providerBootstrap.getSkeleton();
    }

    public RpcResponse<Object> invoke(RpcRequest request) {
        String methodSign = request.getMethodSign();
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
            e.printStackTrace();
            // 不想给客户端暴露服务端的堆栈信息，所以只需要返回 execption msg 就可以了
            // 反射目标一场 需要拿到目标信息的一场
            rpcResponse.setEx(new RuntimeException(e.getTargetException().getMessage()));

        } catch (IllegalAccessException e) {
            rpcResponse.setEx(new RuntimeException(e.getMessage()));
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
