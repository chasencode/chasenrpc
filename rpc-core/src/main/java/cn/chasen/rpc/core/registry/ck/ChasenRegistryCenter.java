package cn.chasen.rpc.core.registry.ck;

import cn.chasen.rpc.core.api.RegistryCenter;
import cn.chasen.rpc.core.consuemer.http.HttpInvoker;
import cn.chasen.rpc.core.meta.InstanceMeta;
import cn.chasen.rpc.core.meta.ServiceMeta;
import cn.chasen.rpc.core.registry.ChangedListener;
import cn.chasen.rpc.core.registry.Event;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @Program: chasenrpc
 * @Description: 注册中心
 * @Author: Chasen
 * @Create: 2024-04-24 20:21
 **/
@Slf4j
public class ChasenRegistryCenter implements RegistryCenter {

    @Value("${ck.servers}")
    private String servers;


    Map<String, Long> VERSIONS = new HashMap<>();
    ScheduledExecutorService consumerExecutor;
    ScheduledExecutorService providerExecutor;
    MultiValueMap<InstanceMeta, ServiceMeta> renews = new LinkedMultiValueMap<>();

    @Override
    public void start() {
        log.info("====>>> [CkRegistry] : start with server : {}", servers);
        consumerExecutor = Executors.newScheduledThreadPool(1);
        providerExecutor = Executors.newScheduledThreadPool(1);
        providerExecutor.scheduleAtFixedRate(() -> {
            renews.keySet().forEach(instanceMeta -> {
                String services = String.join( ",", renews.get(instanceMeta).stream().map(ServiceMeta::toPath).toList());
                Long timeStamp = HttpInvoker.httpPost(JSON.toJSONString(instanceMeta), servers + "/renews?services=" + services, Long.class);
                log.debug("====>>> [CkRegistry] : renews instance {} for services {} with timestamp {}", instanceMeta, services, timeStamp);
            });
        }, 5, 5, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        log.info("====>>> [CkRegistry] : stop with server : {}", servers);
        gracefulShutdown(consumerExecutor);
        gracefulShutdown(providerExecutor);
    }

    @Override
    public void register(ServiceMeta service, InstanceMeta instance) {
        log.info("====>>> [CkRegistry] : register instance {} for {}", instance, service);
        HttpInvoker.httpPost(JSON.toJSONString(instance), servers + "/reg?service=" + service.toPath(), InstanceMeta.class);
        log.info("====>>> [CkRegistry] : registered  {}", instance);
        renews.add(instance, service);

    }

    @Override
    public void unregister(ServiceMeta service, InstanceMeta instance) {
        log.info("====>>> [CkRegistry] : unregister instance {} for {}", instance, service);
        HttpInvoker.httpPost(JSON.toJSONString(instance), servers + "/unreg?service=" + service.toPath(), InstanceMeta.class);
        log.info("====>>> [CkRegistry] : unregistered  {}", instance);
        renews.remove(instance, service);
    }

    @Override
    public List<InstanceMeta> fetchAll(ServiceMeta service) {
        log.info("====>>> [CkRegistry] : find all instance for {}", service);
        List<InstanceMeta> instanceMetas = HttpInvoker.httpGet(servers + "/findAll?service=" + service.toPath(), new TypeReference<List<InstanceMeta>>() {
        });
        log.info("====>>> [CkRegistry] : find all =  {}", instanceMetas);
        return instanceMetas;
    }


    @Override
    public void subscribe(ServiceMeta service, ChangedListener listener) {
        consumerExecutor.scheduleWithFixedDelay(() -> {
            try {
                List<InstanceMeta> instanceMetas = fetchAll(service);
                if (instanceMetas == null) {
                    return;
                }
                long version = VERSIONS.getOrDefault(service.toPath(), -1L);
                Long newVersion = HttpInvoker.httpGet(servers + "/version?service=" + service.toPath(), Long.class);
                log.info("====>>> [CkRegistry] :  version {}, newVersion {}", version, newVersion);
                if (newVersion > version) {
                    List<InstanceMeta> instanceMetaList = fetchAll(service);
                    listener.fire(new Event(instanceMetaList));
                    VERSIONS.put(service.toPath(), newVersion);
                }
            } catch (Exception e) {
                log.error("====>>> [CkRegistry] : subscribe error", e);
            }
        }, 1000, 5000, TimeUnit.MILLISECONDS);
    }


    private void gracefulShutdown(ScheduledExecutorService executor) {
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.SECONDS);
            if (!executor.isTerminated()) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("====>>> [CkRegistry] : stop error", e);
        }
    }
}
