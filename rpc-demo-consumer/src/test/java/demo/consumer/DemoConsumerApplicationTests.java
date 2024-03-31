package demo.consumer;

import cn.chasen.rpc.consumer.DemoConsumerApplication;
import demo.provider.DemoProviderApplicationTests;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import cn.chasen.rpc.core.test.TestZKServer;

@SpringBootTest(classes = {DemoConsumerApplication.class})
class DemoConsumerApplicationTests {

    static ApplicationContext context1;
    static ApplicationContext context2;

    static TestZKServer zkServer = new TestZKServer();

    @BeforeAll
    static void init() {
        System.out.println(" ====================================== ");
        System.out.println(" ====================================== ");
        System.out.println(" =============     ZK2182    ========== ");
        System.out.println(" ====================================== ");
        System.out.println(" ====================================== ");
        zkServer.start();
        System.out.println(" ====================================== ");
        System.out.println(" ====================================== ");
        System.out.println(" =============      P8094    ========== ");
        System.out.println(" ====================================== ");
        System.out.println(" ====================================== ");
        context1 = SpringApplication.run(DemoProviderApplicationTests.class,
                "--server.port=8094", "--rpc.zkServer=127.0.0.1:2182", "--rpc.zkRoot=rpctest",
                "--logging.level.cn.chasen.rpc=info");
//        System.out.println(" ====================================== ");
//        System.out.println(" ====================================== ");
//        System.out.println(" =============      P8095    ========== ");
//        System.out.println(" ====================================== ");
//        System.out.println(" ====================================== ");
//        context2 = SpringApplication.run(DemoProviderApplicationTests.class,
//                "--server.port=8095", "--rpc.zkServer=127.0.0.1:2182",
//                "--logging.level.cn.chasen.rpc=info");
    }

    @Test
    void contextLoads() {
        System.out.println(" ===> aaaa  .... ");
    }

    @AfterAll
    static void destory() {
        SpringApplication.exit(context1, () -> 1);
//        SpringApplication.exit(context2, () -> 1);
        zkServer.stop();
    }

}
