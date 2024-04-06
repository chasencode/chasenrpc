package cn.chasen.rpc.core.api;

import java.util.List;

/**
 * @program: chasenrpc
 * @description: 路由接口
 * @author: Chasen
 * @create: 2024-04-06 22:15
 **/
public interface Router<T> {

    List<T> route(List<T> providers);

    Router Default = p -> p;

}
