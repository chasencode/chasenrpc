package cn.chasen.rpc.core.registry.zk;

import cn.chasen.rpc.core.api.RegistryCenter;
import cn.chasen.rpc.core.exception.RpcException;
import cn.chasen.rpc.core.registry.ChangedListener;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import cn.chasen.rpc.core.meta.InstanceMeta;
import cn.chasen.rpc.core.meta.ServiceMeta;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Value;
import cn.chasen.rpc.core.registry.Event;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ZkRegistryCenter implements RegistryCenter {

    private CuratorFramework client = null;


    @Value("${rpc.zkServer}")
    String servers;

    @Value("${rpc.zkRoot}")
    String root;

    @Override
    public void start() {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        client = CuratorFrameworkFactory.builder()
                .connectString(servers)
                .namespace(root)
                .retryPolicy(retryPolicy)
                .build();
        log.info(" ===> zk client starting to server[" + servers + "/" + root + "].");
        client.start();
    }

    @Override
    public void stop() {
        log.info(" ===> zk client stopped.");
        client.close();
    }

    @Override
    public void register(ServiceMeta service, InstanceMeta instance) {
        String servicePath = "/" + service.toPath();
        log.info(" ===> register to zk register: " + servicePath);
        try {
            // 创建服务的持久化节点
            if (client.checkExists().forPath(servicePath) == null) {
                client.create().withMode(CreateMode.PERSISTENT).forPath(servicePath, "service".getBytes());
            }
            // 创建实例的临时节点
            String instancePath = servicePath + "/" + instance.toPath();
            log.info(" ===> register to zk: " + instancePath);
            client.create().withMode(CreateMode.EPHEMERAL).forPath(instancePath, instance.toMetas().getBytes());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RpcException(e);
        }
    }

    @Override
    public void unregister(ServiceMeta service, InstanceMeta instance) {
        String servicePath = "/" + service.toPath();
        try {
            // 创建服务的持久化节点
            if (client.checkExists().forPath(servicePath) == null) {
                return;
            }
            // 删除实例的临时节点
            String instancePath = servicePath + "/" + instance.toPath();
            log.info(" ===> unregister to zk: " + instancePath);
            client.delete().quietly().forPath(instancePath);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RpcException(e);
        }
    }

    @Override
    public void subscribe(ServiceMeta service, ChangedListener listener) {

        try {
            final TreeCache cache = TreeCache.newBuilder(client, "/" + service)
                    .setCacheData(true).setMaxDepth(2).build();
            cache.getListenable().addListener(
                    (curator, event) -> {
                        // 有任何节点变动这里会执行
                        log.info("zk subscribe event: " + event);
                        List<InstanceMeta> nodes = fetchAll(service);
                        listener.fire(new Event(nodes));
                    }
            );
            cache.start();
        } catch (Exception e) {
            throw new RpcException(e);
        }
    }

    @Override
    public List<InstanceMeta> fetchAll(ServiceMeta service) {
        String servicePath = "/" + service.toPath();
        try {
            log.info(" ===> fetchAll from zk: " + servicePath);
            final List<String> nodes = client.getChildren().forPath(servicePath);
            log.info("===> fetchAll from zk nodes ={}", nodes);
            return mapInstance(nodes, servicePath);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("进入这里错误了");
            throw new RpcException(e);
        }
    }

    private List<InstanceMeta> mapInstance(List<String> nodes, String servicePath) {
        return nodes.stream().map(node -> {
            String nodePath = servicePath + "/" + node;
            String data = "";
            try {
                data = new String(client.getData().forPath(nodePath));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            String[] strs = node.split("_");
            InstanceMeta instanceMeta = InstanceMeta.http(strs[0], Integer.valueOf(strs[1]));
            log.info("mapInstance -> instanceMeta = {}", instanceMeta.toUrl());
            final HashMap<String, String> parameters = JSON.parseObject(data, HashMap.class);
            parameters.forEach((k, v) -> log.info("{} -> {}", k, v));
            instanceMeta.setParameters(parameters);
            return instanceMeta;
        }).collect(Collectors.toList());
    }
}
