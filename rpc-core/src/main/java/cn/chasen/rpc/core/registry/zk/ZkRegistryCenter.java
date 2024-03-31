package cn.chasen.rpc.core.registry.zk;

import cn.chasen.rpc.core.api.RegistryCenter;
import cn.chasen.rpc.core.registry.ChangedListener;
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
            client.create().withMode(CreateMode.EPHEMERAL).forPath(instancePath, "provider".getBytes());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
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
            throw new RuntimeException(e);
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
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<InstanceMeta> fetchAll(ServiceMeta service) {
        String servicePath = "/" + service.toPath();
        try {
            final List<String> strings = client.getChildren().forPath("/");
            log.info(" ===> fetchAll from zk: " + servicePath);
            final List<String> nodes = client.getChildren().forPath(servicePath);
            log.info("===> fetchAll from zk nodes ={}", nodes);
            return mapInstance(nodes);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("进入这里错误了");
            throw new RuntimeException(e);
        }
    }

    private List<InstanceMeta> mapInstance(List<String> nodes) {
        return nodes.stream().map(x -> {
            String[] strs = x.split("_");
            return InstanceMeta.http(strs[0], Integer.valueOf(strs[1]));
        }).collect(Collectors.toList());
    }
}
