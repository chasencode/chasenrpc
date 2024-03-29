package demo.provider;

import api.RpcRequest;
import api.RpcResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import provider.ProviderConfig;
import provider.ProviderInvoker;

@SpringBootApplication
@RestController
@Import({ProviderConfig.class})
public class KkrpcDemoProviderApplication {

//    public static void main(String[] args) {
//        SpringApplication.run(KkrpcDemoProviderApplication.class, args);
//    }

    // 使用HTTP + JSON 来实现序列化和通信

    @Autowired
    ProviderInvoker providerInvoker;

    @RequestMapping("/")
    public RpcResponse<Object> invoke(@RequestBody RpcRequest request) {
        return providerInvoker.invoke(request);
    }


    @Bean
    ApplicationRunner providerRun() {
        return x -> {
            // test 1 parameter method
            RpcRequest request = new RpcRequest();
            request.setService("cn.kimmking.kkrpc.demo.api.UserService");
            request.setMethodSign("findById@1_int");
            request.setArgs(new Object[]{100});

            RpcResponse<Object> rpcResponse = invoke(request);
            System.out.println("return : "+rpcResponse.getData());

            // test 2 parameters method
            RpcRequest request1 = new RpcRequest();
            request1.setService("cn.kimmking.kkrpc.demo.api.UserService");
            request1.setMethodSign("findById@2_int_java.lang.String");
            request1.setArgs(new Object[]{100, "CC"});

            RpcResponse<Object> rpcResponse1 = invoke(request1);
            System.out.println("return : "+rpcResponse1.getData());

        };
    }


}
