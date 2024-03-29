package demo.consumer;

import demo.provider.DemoProviderApplication;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import test.TestZKServer;

@SpringBootTest(classes = {DemoConsumerApplication.class})
class DemoConsumerApplicationTests {

    static ApplicationContext context;

    static TestZKServer zkServer = new TestZKServer();

    @BeforeAll
    static void init() {
        System.out.println(" ====================================== ");
        System.out.println(" ====================================== ");
        System.out.println(" ====================================== ");
        System.out.println(" ====================================== ");
        System.out.println(" ====================================== ");
        System.out.println(" ====================================== ");

        zkServer.start();
        //embeddedZookeeper =  new EmbeddedZookeeper("localhost:2182");
        context = SpringApplication.run(DemoProviderApplication.class,
                "--server.port=8094", "--kkrpc.zkServer=localhost:2182",
                "--logging.level.cn.kimmking.kkrpc=info");
    }

    @Test
    void contextLoads() {
        System.out.println(" ===> aaaa  .... ");
    }

    @AfterAll
    static void destory() {
        SpringApplication.exit(context, () -> 1);
        zkServer.stop();
    }

}
