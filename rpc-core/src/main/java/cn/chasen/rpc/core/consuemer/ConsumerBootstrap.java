package cn.chasen.rpc.core.consuemer;

import cn.chasen.rpc.core.annotation.ChasenConsumer;
import cn.chasen.rpc.core.api.Filter;
import cn.chasen.rpc.core.api.RegistryCenter;
import cn.chasen.rpc.core.api.Router;
import cn.chasen.rpc.core.api.RpcContext;
import cn.chasen.rpc.core.cluster.RoundRibonLoadBalancer;
import cn.chasen.rpc.core.meta.InstanceMeta;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import cn.chasen.rpc.core.meta.ServiceMeta;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import cn.chasen.rpc.core.util.FiledUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消费端动态启动累
 */
@Data
@Slf4j

public class ConsumerBootstrap implements ApplicationContextAware {

    ApplicationContext applicationContext;


    private Map<String, Object> stub = new HashMap<>();

    Environment environment;



    public void start() {
        // 需要注意，现在的context 是全局的，如果需要每个服务（请求）有自己的参数，需要增加clone
        RpcContext context = applicationContext.getBean(RpcContext.class);
        RegistryCenter rc = applicationContext.getBean(RegistryCenter.class);
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        for (String beanDefinitionName : beanDefinitionNames) {
            Object bean = applicationContext.getBean(beanDefinitionName);
            // 这里获取到了 CGlib 增强提升的子类
            List<Field> fieldList = FiledUtils.findAnnotatedField(bean.getClass(), ChasenConsumer.class);
            fieldList.forEach(filed -> {
                log.info("===> " + filed.getName());
                try {
                    Class<?> service = filed.getType();
                    String serviceName = service.getCanonicalName();
                    Object consumer = stub.get(serviceName);
                    if (consumer == null) {
//                        consumer = createConsumer(service, context, List.of(providers));
                        consumer = createFromRegistry(service, context, rc);
                        stub.put(serviceName, consumer);
                    }
                    filed.setAccessible(true);
                    filed.set(bean, consumer);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private Object createFromRegistry(Class<?> service, RpcContext context, RegistryCenter rc) {
        ServiceMeta serviceMeta = ServiceMeta.builder()
                .app(context.param("app.id")).namespace(context.param("app.namespace"))
                .env(context.param("app.env")).name(service.getCanonicalName()).build();
        List<InstanceMeta> providers = rc.fetchAll(serviceMeta);
        log.info("===》 map to provider");
        //  @todo 这里是全部清除，会有问题，需要修改
        rc.subscribe(serviceMeta, event -> {
            providers.clear();
            providers.addAll(event.getData());
        });

        return createConsumer(service, context, providers);
    }


    private Object createConsumer(Class<?> service, RpcContext context, List<InstanceMeta> providers) {
        return Proxy.newProxyInstance(
                service.getClassLoader(),
                new Class[]{service},
                new RpcInvocationHandler(service, context, providers)
        );
    }



}
