package provider;

import annotation.ChasenProvider;
import api.RegistryCenter;
import demo.api.RpcRequest;
import demo.api.RpcResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import meta.ProviderMeta;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import util.MethodUtils;
import util.TypeUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Data
public class  ProviderBootstrap implements ApplicationContextAware {

    ApplicationContext applicationContext;

    private MultiValueMap<String, ProviderMeta> skeleton = new LinkedMultiValueMap<>();

    private String instance;

    private RegistryCenter rc;

    @Value("${server.port}")
    private String port;

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


    @PostConstruct
    public void init() {
        // 获取所有使用这个注解的bean
        Map<String, Object> providers = applicationContext.getBeansWithAnnotation(ChasenProvider.class);
        providers.forEach((String beanName, Object beanObject) -> System.out.println(beanName));
//        skeleton.putAll(providers);
        rc = applicationContext.getBean(RegistryCenter.class);
        providers.values().forEach(
                this::getInterface
        );

    }
    public void start() {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            instance = ip + "_" +port;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        skeleton.keySet().forEach(this::registerService); // zk有了， 但spring 还未注册完成
    }

    @PreDestroy
    public void stop() {
        System.out.printf("客户端关闭");
        skeleton.keySet().forEach(this::unregisterService);
    }

    private void registerService(String service) {
//        final RegistryCenter rc = applicationContext.getBean(RegistryCenter.class);
        rc.register(service, instance);
    }


    private void unregisterService(String service) {
//        final RegistryCenter rc = applicationContext.getBean(RegistryCenter.class);
        rc.unregister(service, instance);
    }

    private void getInterface(Object beanObject) {
        // 拿到接口的全限定的名字和实现类
        Arrays.stream(beanObject.getClass().getInterfaces()).forEach(anInterface -> {
            Method[] methods = anInterface.getMethods();
            for (Method method : methods) {
                if (MethodUtils.checkLocalMethod(method)) {
                    continue;
                }
                createProvider(anInterface, beanObject, method);
            }
        });
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
