package com.github.demo.grpc.nacos;

import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;
import io.grpc.internal.GrpcUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.cloud.client.discovery.event.HeartbeatMonitor;
import org.springframework.context.event.EventListener;

import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

public class NacosNameResolverProvider extends NameResolverProvider {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * The constant containing the scheme that will be used by this factory.
     */
    public static final String NACOS = "nacos";

    private final Set<NacosNameResolver> discoveryClientNameResolvers = ConcurrentHashMap.newKeySet();
    private final HeartbeatMonitor monitor = new HeartbeatMonitor();

    private final DiscoveryClient client;

    /**
     * Creates a new discovery client based name resolver factory.
     *
     * @param client The client to use for the address discovery.
     */
    public NacosNameResolverProvider(final DiscoveryClient client) {
        this.client = requireNonNull(client, "client");
    }

    @Nullable
    @Override
    public NameResolver newNameResolver(final URI targetUri, final NameResolver.Args args) {
        if (NACOS.equals(targetUri.getScheme())) {
            final String serviceName = targetUri.getPath();
            if (serviceName == null || serviceName.length() <= 1 || !serviceName.startsWith("/")) {
                throw new IllegalArgumentException("Incorrectly formatted target uri; "
                        + "expected: '" + NACOS + ":[//]/<service-name>'; "
                        + "but was '" + targetUri.toString() + "'");
            }
            final AtomicReference<NacosNameResolver> reference = new AtomicReference<>();
            final NacosNameResolver discoveryClientNameResolver =
                    new NacosNameResolver(serviceName.substring(1), this.client, args,
                            GrpcUtil.SHARED_CHANNEL_EXECUTOR,
                            () -> this.discoveryClientNameResolvers.remove(reference.get()));
            reference.set(discoveryClientNameResolver);
            this.discoveryClientNameResolvers.add(discoveryClientNameResolver);
            return discoveryClientNameResolver;
        }
        return null;
    }

    @Override
    public String getDefaultScheme() {
        return NACOS;
    }

    @Override
    protected boolean isAvailable() {
        return true;
    }

    @Override
    protected int priority() {
        return 6; // More important than DNS
    }

    /**
     * Triggers a refresh of the registered name resolvers.
     *
     * @param event The event that triggered the update.
     */
    @EventListener(HeartbeatEvent.class)
    public void heartbeat(final HeartbeatEvent event) {
        if (this.monitor.update(event.getValue())) {
            for (final NacosNameResolver nacosNameResolver : this.discoveryClientNameResolvers) {
                nacosNameResolver.refreshFromExternal();
            }
        }
    }

    /**
     * Cleans up the name resolvers.
     */
    @PreDestroy
    public void destroy() {
        this.discoveryClientNameResolvers.clear();
    }

    @Override
    public String toString() {
        return "DiscoveryClientResolverFactory [scheme=" + getDefaultScheme() +
                ", discoveryClient=" + this.client + "]";
    }

}
