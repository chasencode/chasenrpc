package provider;

import annotation.ChasenProvider;
import demo.api.RpcRequest;
import demo.api.RpcResponse;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import meta.ProviderMeta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import util.MethodUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ProviderBootstrap implements ApplicationContextAware {

    ApplicationContext applicationContext;

    private MultiValueMap<String, ProviderMeta> skeleton = new LinkedMultiValueMap<>();

    public RpcResponse invokeRequest(RpcRequest request) {
        String methodSign = request.getMethodSign();
        List<ProviderMeta> providerMetaList = skeleton.get(request.getService());

        RpcResponse rpcResponse = new RpcResponse();
        try {

            ProviderMeta meta = findProviderMeta(providerMetaList, methodSign);
            if (meta == null) {
                throw new RuntimeException(request.getService() + " not find");
            }
            Method method = meta.getMethod();
            Object result = method.invoke(meta.getServiceImpl(), request.getArgs());
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

    private ProviderMeta findProviderMeta(List<ProviderMeta> providerMetaList, String methodSign) {
        return providerMetaList.stream().filter(
                x -> x.getMethodSign().equals(methodSign)
        ).findFirst().orElse(null);
    }


    @PostConstruct
    public void start() {
        // 获取所有使用这个注解的bean
        Map<String, Object> providers = applicationContext.getBeansWithAnnotation(ChasenProvider.class);
        providers.forEach((String beanName, Object beanObject) -> System.out.println(beanName));
//        skeleton.putAll(providers);

        providers.values().forEach(
                this::getInterface
        );
    }

    private void getInterface(Object beanObject) {
        // 拿到接口的全限定的名字和实现类
        Class<?> anInterface = beanObject.getClass().getInterfaces()[0];
        Method[] methods = anInterface.getMethods();
        for (Method method : methods) {
            if (MethodUtils.checkLocalMethod(method)) {
                continue;
            }
            createProvider(anInterface, beanObject, method);
        }
    }

    private void createProvider(Class<?> anInterface, Object beanObject, Method method) {
        ProviderMeta meta = new ProviderMeta();
        meta.setMethodSign(MethodUtils.methodSign(method));
        meta.setServiceImpl(beanObject);
        meta.setMethod(method);
        System.out.printf(" create a provider: " + meta);
        skeleton.add(anInterface.getCanonicalName(), meta);
    }
}
