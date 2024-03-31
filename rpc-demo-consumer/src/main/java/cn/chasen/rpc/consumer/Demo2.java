package cn.chasen.rpc.consumer;


import cn.chasen.rpc.core.annotation.ChasenConsumer;
import cn.chasen.rpc.demo.api.User;
import cn.chasen.rpc.demo.api.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class Demo2 {

    @ChasenConsumer
    UserService userService;

    public void test() {
        User user = userService.findById(100);
        log.debug(user.toString());
    }

}
