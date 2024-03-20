package consuemer;

import annotation.ChasenConsumer;
import api.RegistryCenter;
import api.Router;
import api.RpcContext;
import cluster.RoundRibonLoadBalancer;
import lombok.Data;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import util.FiledUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 消费端动态启动累
 */
@Data
public class ConsumerBootstrap implements ApplicationContextAware {

    ApplicationContext applicationContext;


    private Map<String, Object> stub = new HashMap<>();

    Environment environment;


    public void start() {

        Router router = applicationContext.getBean(Router.class);
        RoundRibonLoadBalancer loadBalancer = applicationContext.getBean(RoundRibonLoadBalancer.class);

        RpcContext context = new RpcContext();
        context.setRouter(router);
        context.setLoadBalancer(loadBalancer);


        RegistryCenter rc = applicationContext.getBean(RegistryCenter.class);

        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        for (String beanDefinitionName : beanDefinitionNames) {
            Object bean = applicationContext.getBean(beanDefinitionName);
            // 这里获取到了 CGlib 增强提升的子类
            List<Field> fieldList = FiledUtils.findAnnotatedField(bean.getClass(), ChasenConsumer.class);

//            if (beanDefinitionName.contains("chasenrpcDemoConsumerApplication")) return;

            fieldList.stream().forEach(filed -> {
                System.out.printf("===> " + filed.getName());
                try {
                    Class<?> service = filed.getType();
                    String serviceName = service.getCanonicalName();
                    Object consumer = stub.get(serviceName);
                    if (consumer == null) {
//                        consumer = createConsumer(service, context, List.of(providers));
                        consumer = createFromRegistry(service, context, rc);
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
        String serviceName = service.getCanonicalName();
        List<String> providers = mapUrls(rc.fetchAll(serviceName));
        System.out.printf("===》 map to provider");
        providers.forEach(System.out::println);

        rc.subscribe(serviceName, event -> {
            providers.clear();
            providers.addAll(mapUrls(event.getData()));
        });

        return createConsumer(service, context, providers);
    }


    private Object createConsumer(Class<?> service, RpcContext context, List<String> providers) {
        return Proxy.newProxyInstance(
                service.getClassLoader(),
                new Class[]{service},
                new ChasenInvocationHandler(service, context, providers)
        );
    }

    private List<String> mapUrls(List<String> nodes) {
        return nodes.stream()
                .map(x -> "http://" + x.replace('_', ':')).collect(Collectors.toList());
    }


}
