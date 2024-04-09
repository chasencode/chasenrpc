package cn.chasen.rpc.core.annotation;

import cn.chasen.rpc.core.config.ConsumerConfig;
import cn.chasen.rpc.core.config.ProviderConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Import({ProviderConfig.class, ConsumerConfig.class})
public @interface EnableRpc {
}
