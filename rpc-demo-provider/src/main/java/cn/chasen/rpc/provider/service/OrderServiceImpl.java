package cn.chasen.rpc.provider.service;

import cn.chasen.rpc.core.annotation.ChasenProvider;
import cn.chasen.rpc.demo.api.Order;
import cn.chasen.rpc.demo.api.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Component
@ChasenProvider
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Override
    public Order findById(Integer id) {
        log.debug("订单ID 为" + id);
        if(id == 404) {
            throw new RuntimeException("404 exception");
        }

        return new Order(id.longValue(), 15.6f);
    }
}
