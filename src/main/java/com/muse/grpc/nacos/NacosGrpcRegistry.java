package com.muse.grpc.nacos;


import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.registry.NacosRegistration;
import com.alibaba.cloud.nacos.registry.NacosServiceRegistry;
import com.muse.grpc.config.GrpcServerProperties;
import com.muse.grpc.context.GrpcServerInitializedEvent;
import org.springframework.beans.BeanUtils;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.EventListener;

public class NacosGrpcRegistry implements SmartLifecycle {

    private final NacosServiceRegistry nacosServiceRegistry;

    private final GrpcServerProperties grpcServerProperties;

    private final NacosDiscoveryProperties nacosDiscoveryProperties;

    private NacosRegistration registration;

    public NacosGrpcRegistry(NacosServiceRegistry nacosServiceRegistry, NacosDiscoveryProperties nacosDiscoveryProperties, GrpcServerProperties grpcServerProperties) {
        this.nacosServiceRegistry = nacosServiceRegistry;
        this.grpcServerProperties = grpcServerProperties;
        this.nacosDiscoveryProperties = nacosDiscoveryProperties;
    }

    @EventListener
    public void onGrpcServerStarted(GrpcServerInitializedEvent initializedEvent) {
        registration = getRegistration(initializedEvent);
        nacosServiceRegistry.register(registration);
    }

    public NacosRegistration getRegistration(GrpcServerInitializedEvent event) {
        NacosDiscoveryProperties properties = new NacosDiscoveryProperties();
        BeanUtils.copyProperties(this.nacosDiscoveryProperties, properties);
        properties.setPort(grpcServerProperties.getPort());
        properties.setService("grpc-" + nacosDiscoveryProperties.getService());
        return new NacosRegistration(properties, event.getApplicationContext());
    }

    @Override
    public boolean isAutoStartup() {
        return false;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public void start() {

    }

    @Override
    public synchronized void stop() {
        nacosServiceRegistry.deregister(registration);
        nacosServiceRegistry.close();
        registration = null;

    }

    @Override
    public synchronized boolean isRunning() {
        return null != registration;
    }

    @Override
    public int getPhase() {
        return 0;
    }
}
