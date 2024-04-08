package cn.chasen.rpc.core.annotation;

import cn.chasen.rpc.core.consuemer.ConsumerConfig;
import cn.chasen.rpc.core.provider.ProviderConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Import({ProviderConfig.class, ConsumerConfig.class})
public @interface EnableRpc {
}
