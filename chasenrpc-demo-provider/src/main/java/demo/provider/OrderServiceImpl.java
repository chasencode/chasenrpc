package demo.provider;

import annotation.ChasenProvider;
import demo.api.Order;
import demo.api.OrderService;
import org.springframework.stereotype.Component;


@Component
@ChasenProvider
public class OrderServiceImpl implements OrderService {
    @Override
    public Order findById(Integer id) {
        System.out.printf("订单ID 为" + id);
        if(id == 404) {
            throw new RuntimeException("404 exception");
        }

        return new Order(id.longValue(), 15.6f);
    }
}
