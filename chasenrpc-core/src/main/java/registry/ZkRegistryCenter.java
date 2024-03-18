package registry;

import api.RegistryCenter;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import javax.swing.event.ChangeListener;
import java.util.List;

public class ZkRegistryCenter implements RegistryCenter {

    private CuratorFramework client = null;

    @Override
    public void start() {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        client = CuratorFrameworkFactory.builder()
                .connectString("127.0.0.1:2181")
                .namespace("chasenrpc")
                .retryPolicy(retryPolicy)
                .build();
        client.start();
        System.out.println(" ===> zk client starting.");
    }

    @Override
    public void stop() {
        System.out.println(" ===> zk client stopped.");
        client.close();
    }

    @Override
    public void register(String service, String instance) {
        String servicePath = "/" + service;
        try {
            // 创建服务的持久化节点
            if (client.checkExists().forPath(servicePath) == null) {
                client.create().withMode(CreateMode.PERSISTENT).forPath(servicePath, "service".getBytes());
            }
            // 创建实例的临时节点
            String instancePath = servicePath + "/" + instance;
            System.out.println(" ===> register to zk: " + instancePath);
            client.create().withMode(CreateMode.EPHEMERAL).forPath(instancePath, "provider".getBytes());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void unregister(String service, String instance) {
        String servicePath = "/" + service;
        try {
            // 创建服务的持久化节点
            if (client.checkExists().forPath(servicePath) == null) {
                return;
            }
            // 删除实例的临时节点
            String instancePath = servicePath + "/" + instance;
            System.out.println(" ===> register to zk: " + instancePath);
            client.delete().quietly().forPath(instancePath);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void subscribe(String service, ChangedListener listener) {

        try {
            final TreeCache cache = TreeCache.newBuilder(client, "/"+service)
                    .setCacheData(true).setMaxDepth(2).build();
            cache.getListenable().addListener(
                    (curator, event) -> {
                        // 有任何节点变动这里会执行
                        System.out.println("zk subscribe event: " + event);
                        List<String> nodes = fetchAll(service);
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
    public List<String> fetchAll(String service) {
        String servicePath = "/" + service;
        try {
            System.out.println(" ===> fetchAll from zk: " + servicePath);
            final List<String> nodes = client.getChildren().forPath(servicePath);
            nodes.forEach(System.out::println);
            return nodes;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
