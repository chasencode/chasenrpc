package provider;

import annotation.ChasenProvider;
import demo.api.RpcRequest;
import demo.api.RpcResponse;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Component
@Data
public class ProviderBootstrap implements ApplicationContextAware {
    @Autowired
    ApplicationContext applicationContext;

    public RpcResponse invokeRequest(RpcRequest request) {
        Object bean = skeleton.get(request.getService());
        try {
            Method method = findMethod(bean.getClass(), request.getMethod());
            Object result = method.invoke(bean, request.getArgs());
            return new RpcResponse(true, result);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    private Method findMethod(Class<?> aClass, String methodName) {
        for (Method method : aClass.getMethods()) {
            if (methodName.equals(method.getName())) {
                return method;
            }
        }
        return null;
    }



    private Map<String, Object> skeleton = new HashMap<>();

    @PostConstruct
    public void buildProviders() {
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
        // 这里要 改成多个接口类的实现
        skeleton.put(anInterface.getCanonicalName(), beanObject);
    }
}
