package demo.provider;

import demo.api.RpcRequest;
import demo.api.RpcResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import provider.ProviderBootstrap;
import provider.ProviderConfig;

@SpringBootApplication
@RestController
@Import({ProviderConfig.class})
public class ChasenDemoProviderApplication {

    @Autowired
    ProviderBootstrap providerBootstrap;

    public static void main(String[] args) {
        SpringApplication.run(ChasenDemoProviderApplication.class, args);
    }
    // 使用 HTTP + JSON 来时间序列化和通信


    @RequestMapping("/")
    public RpcResponse invoke(@RequestBody RpcRequest request) {
        return  providerBootstrap.invokeRequest(request);
    }

    @Bean
    ApplicationRunner providerRun() {
        return x -> {
            RpcRequest request = new RpcRequest();
            request.setService("demo.api.UserService");
            request.setMethodSign("findById@1_int");
            request.setArgs(new Object[]{100});
            RpcResponse rpcResponse = providerBootstrap.invokeRequest(request);
            System.out.println("return:" + rpcResponse.getData());


            RpcRequest request2 = new RpcRequest();
            request2.setService("demo.api.UserService");
            request2.setMethodSign("findById@2_int_java.long.long.String");
            request2.setArgs(new Object[]{101, "Chasen"});
            RpcResponse rpcResponse2 = providerBootstrap.invokeRequest(request);
            System.out.println("return2:" + rpcResponse2.getData());
        };
    }
}
