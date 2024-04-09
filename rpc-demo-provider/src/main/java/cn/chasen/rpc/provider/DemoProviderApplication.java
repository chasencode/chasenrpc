package cn.chasen.rpc.provider;

import cn.chasen.rpc.core.api.RpcRequest;
import cn.chasen.rpc.core.api.RpcResponse;
import cn.chasen.rpc.core.config.ProviderConfig;
import cn.chasen.rpc.core.transport.SpringBootTransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.RestController;
import cn.chasen.rpc.core.provider.ProviderInvoker;

@SpringBootApplication
@RestController
@Import({ProviderConfig.class})
@Slf4j
public class DemoProviderApplication {

    @Autowired
    ProviderInvoker providerInvoker;

    public static void main(String[] args) {
        SpringApplication.run(DemoProviderApplication.class, args);
    }
    // 使用 HTTP + JSON 来时间序列化和通信


    @Autowired
    SpringBootTransport transport;
    @Bean
    ApplicationRunner providerRun() {
        return x -> {
            RpcRequest request = new RpcRequest();
            request.setService("cn.chasen.rpc.demo.api.UserService");
            request.setMethodSign("findById@1_int");
            request.setArgs(new Object[]{100});
            RpcResponse<Object> rpcResponse = transport.invoke(request);
            log.info("return:" + rpcResponse.getData());

            RpcRequest request2 = new RpcRequest();
            request2.setService("cn.chasen.rpc.demo.api.UserService");
            request2.setMethodSign("findById@2_int_java.long.long.String");
            request2.setArgs(new Object[]{101, "Chasen"});
            RpcResponse<Object> rpcResponse2 = transport.invoke(request);
            log.info("return2:" + rpcResponse2.getData());
        };
    }
}
