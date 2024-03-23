package provider;

import annotation.ChasenProvider;
import api.RegistryCenter;
import demo.api.RpcRequest;
import demo.api.RpcResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import meta.InstanceMeta;
import meta.ProviderMeta;
import meta.ServiceMeta;
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
@Slf4j
public class  ProviderBootstrap implements ApplicationContextAware {

    ApplicationContext applicationContext;

    private MultiValueMap<String, ProviderMeta> skeleton = new LinkedMultiValueMap<>();

    private InstanceMeta instance;

    private RegistryCenter rc;

    @Value("${server.port}")
    private String port;

    @Value("${app.id}")
    private String app;

    @Value("${app.namespace}")
    private String namespace;

    @Value("${app.env}")
    private String env;



    @PostConstruct
    public void init() {
        // 获取所有使用这个注解的bean
        Map<String, Object> providers = applicationContext.getBeansWithAnnotation(ChasenProvider.class);
//        providers.forEach((String beanName, Object beanObject) -> log.infoln(beanName));
//        skeleton.putAll(providers);
        rc = applicationContext.getBean(RegistryCenter.class);
        providers.values().forEach(this::getInterface);

    }
    public void start() {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            instance = InstanceMeta.http(ip, Integer.valueOf(port));
            rc.start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        skeleton.keySet().forEach(this::registerService); // zk有了， 但spring 还未注册完成
    }

    @PreDestroy
    public void stop() {
        log.info("客户端关闭");
        skeleton.keySet().forEach(this::unregisterService);
        rc.stop();
    }

    private void registerService(String service) {
        ServiceMeta serviceMeta = ServiceMeta.builder()
                .app(app).namespace(namespace).env(env).name(service).build();
        rc.register(serviceMeta, instance);
    }


    private void unregisterService(String service) {
        ServiceMeta serviceMeta = ServiceMeta.builder()
                .app(app).namespace(namespace).env(env).name(service).build();
        rc.unregister(serviceMeta, instance);
    }

    private void getInterface(Object impl) {
        // 拿到接口的全限定的名字和实现类
        Arrays.stream(impl.getClass().getInterfaces()).forEach(anInterface -> {
            Method[] methods = anInterface.getMethods();
            for (Method method : methods) {
                if (MethodUtils.checkLocalMethod(method)) {
                    continue;
                }
                createProvider(anInterface, impl, method);
            }
        });
    }

    private void createProvider(Class<?> anInterface, Object impl, Method method) {
        ProviderMeta providerMeta = ProviderMeta.builder().method(method)
                .serviceImpl(impl).methodSign(MethodUtils.methodSign(method)).build();
        log.info(" create a provider: " + providerMeta);
        skeleton.add(anInterface.getCanonicalName(), providerMeta);
    }
}
