package consuemer;

import annotation.ChasenConsumer;
import annotation.ChasenProvider;
import api.LoadBalancer;
import api.RegistryCenter;
import api.Router;
import api.RpcContext;
import cluster.RoundRibonLoadBalancer;
import demo.api.RpcRequest;
import demo.api.RpcResponse;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.val;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ConsumerBootstrap implements ApplicationContextAware {

    ApplicationContext applicationContext;


    private Map<String, Object> stub = new HashMap<>();

    Environment environment;


    public void start() {

        Router router = applicationContext.getBean(Router.class);
        LoadBalancer loadBalancer = applicationContext.getBean(RoundRibonLoadBalancer.class);

        RpcContext context = new RpcContext();
        context.setRouter(router);
        context.setLoadBalancer(loadBalancer);


        RegistryCenter rc = applicationContext.getBean(RegistryCenter.class);

        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        for (String beanDefinitionName : beanDefinitionNames) {
            Object bean = applicationContext.getBean(beanDefinitionName);
            // 这里获取到了 CGlib 增强提升的子类
            List<Field> fieldList = findAnnotatedField(bean.getClass());

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
        List<String> providers = rc.fetchAll(serviceName);
        return createConsumer(service, context, providers);
    }

    private Object createConsumer(Class<?> service, RpcContext context, List<String> providers) {
        return Proxy.newProxyInstance(
                service.getClassLoader(),
                new Class[]{service},
                new ChasenInvocationHandler(service, context, providers)
        );
    }

    private List<Field> findAnnotatedField(Class<?> aClass) {
        List<Field> result = new ArrayList<>();
        while (aClass != null) {
            Field[] fields = aClass.getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(ChasenConsumer.class)) {
                    result.add(field);
                }
            }
            aClass = aClass.getSuperclass();
        }
        return result;

    }
}
