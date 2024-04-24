package cn.chasen.rpc.core.provider;

import cn.chasen.rpc.core.annotation.ChasenProvider;
import cn.chasen.rpc.core.api.RegistryCenter;
import cn.chasen.rpc.core.config.AppConfigProperties;
import cn.chasen.rpc.core.config.ProviderProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import cn.chasen.rpc.core.meta.InstanceMeta;
import cn.chasen.rpc.core.meta.ProviderMeta;
import cn.chasen.rpc.core.meta.ServiceMeta;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import cn.chasen.rpc.core.util.MethodUtils;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;

@Data
@Slf4j
public class  ProviderBootstrap implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    private MultiValueMap<String, ProviderMeta> skeleton = new LinkedMultiValueMap<>();

    private InstanceMeta instance;

    private RegistryCenter rc;

    private String port;

    private AppConfigProperties appProperties;

    private ProviderProperties providerProperties;

    public ProviderBootstrap(String port, AppConfigProperties appProperties,
                             ProviderProperties providerProperties) {
        this.port = port;
        this.appProperties = appProperties;
        this.providerProperties = providerProperties;
    }

    @PostConstruct
    public void init() {
        // 获取所有使用这个注解的bean
        Map<String, Object> providers = applicationContext.getBeansWithAnnotation(ChasenProvider.class);
        rc = applicationContext.getBean(RegistryCenter.class);
        providers.values().forEach(this::getInterface);

    }
    public void start() {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            instance = InstanceMeta.http(ip, Integer.valueOf(port)).addParams(providerProperties.getMetas());
            rc.start();
            skeleton.keySet().forEach(this::registerService); // zk有了， 但spring 还未注册完成
        } catch (UnknownHostException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    @PreDestroy
    public void stop() {
        log.info("客户端关闭");
        skeleton.keySet().forEach(this::unregisterService);
        rc.stop();
    }

    private void registerService(String service) {
        ServiceMeta serviceMeta = ServiceMeta.builder()
                .app(appProperties.getId()).namespace(appProperties.getNamespace()).env(appProperties.getEnv()).name(service).build();
        rc.register(serviceMeta, instance);
    }


    private void unregisterService(String service) {
        ServiceMeta serviceMeta = ServiceMeta.builder()
                .app(appProperties.getId()).namespace(appProperties.getNamespace()).env(appProperties.getEnv()).name(service).build();
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
        ProviderMeta providerMeta = ProviderMeta.builder()
                .method(method)
                .serviceImpl(impl)
                .methodSign(MethodUtils.methodSign(method))
                .build();
        log.info(" create a provider: " + providerMeta);
        skeleton.add(anInterface.getCanonicalName(), providerMeta);
    }
}
