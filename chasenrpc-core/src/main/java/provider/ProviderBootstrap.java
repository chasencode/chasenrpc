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
            rc.start();
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
        rc.stop();
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
