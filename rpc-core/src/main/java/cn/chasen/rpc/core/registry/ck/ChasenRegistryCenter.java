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

    private static final String REG_PATH = "/reg";
    private static final String UNREG_PATH = "/unreg";
    private static final String FIND_ALL_PATH = "/findAll";
    private static final String VERSION_PATH = "/version";

    private static final String RENWS_PATH = "/renews";

    Map<String, Long> VERSIONS = new HashMap<>();
    MultiValueMap<InstanceMeta, ServiceMeta> renews = new LinkedMultiValueMap<>();

    ChasenHealthChecker healthChecker = new ChasenHealthChecker();

    @Override
    public void start() {
        log.info("====>>> [CkRegistry] : start with server : {}", servers);
        healthChecker.start();
        providerCheck();
    }

    @Override
    public void stop() {
        log.info("====>>> [CkRegistry] : stop with server : {}", servers);
        healthChecker.stop();
    }

    @Override
    public void register(ServiceMeta service, InstanceMeta instance) {
        log.info("====>>> [CkRegistry] : register instance {} for {}", instance, service);
        log.info("====>>> [CkRegistry] : register path {}", regPath(service));

        HttpInvoker.httpPost(JSON.toJSONString(instance), regPath(service), InstanceMeta.class);
        log.info("====>>> [CkRegistry] : registered  {}", instance);
        renews.add(instance, service);

    }


    @Override
    public void unregister(ServiceMeta service, InstanceMeta instance) {
        log.info("====>>> [CkRegistry] : unregister instance {} for {}", instance, service);
        HttpInvoker.httpPost(JSON.toJSONString(instance), unregPath(service), InstanceMeta.class);
        log.info("====>>> [CkRegistry] : unregistered  {}", instance);
        renews.remove(instance, service);
    }


    @Override
    public List<InstanceMeta> fetchAll(ServiceMeta service) {
        log.info("====>>> [CkRegistry] : find all instance for {}", service);
        List<InstanceMeta> instanceMetas = HttpInvoker.httpGet(findAllPath(service), new TypeReference<List<InstanceMeta>>() {
        });
        log.info("====>>> [CkRegistry] : find all =  {}", instanceMetas);
        return instanceMetas;
    }


    @Override
    public void subscribe(ServiceMeta service, ChangedListener listener) {
        healthChecker.consumerCheck( () -> {
            List<InstanceMeta> instanceMetas = fetchAll(service);
            if (instanceMetas == null) {
                return;
            }
            Long version = VERSIONS.getOrDefault(service.toPath(), -1L);
            Long newVersion = HttpInvoker.httpGet(versionPath(service), Long.class);
            log.info(" ====>>>> [CkRegistry] : version = {}, newVersion = {}", version, newVersion);
            if(newVersion > version) {
                List<InstanceMeta> instances = fetchAll(service);
                listener.fire(new Event(instances));
                VERSIONS.put(service.toPath(), newVersion);
            }
        });
    }

    private String versionPath(ServiceMeta service) {
        return path(VERSION_PATH, service);
    }

    private String findAllPath(ServiceMeta service) {
        return path(FIND_ALL_PATH, service);
    }

    private String unregPath(ServiceMeta service) {
        return path(UNREG_PATH, service);
    }

    private String regPath(ServiceMeta service) {
        return path(REG_PATH, service);
    }

    private String renewsPath(List<ServiceMeta> serviceMetaList) {
        String services = String.join(",", serviceMetaList.stream().map(ServiceMeta::toPath).toList());
        return path(RENWS_PATH, serviceMetaList);
    }

    private String path(String context, List<ServiceMeta> serviceMetaList) {
        String services = String.join(",", serviceMetaList.stream().map(ServiceMeta::toPath).toList());
        return servers + context + "?services=" + services;
    }

    private String path(String context, ServiceMeta service) {
        return servers + context + "?service=" + service.toPath();
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


    public void providerCheck() {
        healthChecker.providerCheck(() -> {
            renews.keySet().stream().forEach(
                    instance -> {
                        Long timestamp = HttpInvoker.httpPost(JSON.toJSONString(instance),
                                renewsPath(renews.get(instance)), Long.class);
                        log.info(" ====>>>> [CkRegistry] : renew instance {} at {}", instance, timestamp);
                    }
            );
        });
    }
}
