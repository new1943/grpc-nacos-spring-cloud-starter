package com.github.demo.grpc;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.github.demo.grpc.annoation.GrpcGlobalInterceptor;
import com.github.demo.grpc.annoation.GrpcService;
import com.github.demo.grpc.context.GrpcServerInitializedEvent;
import com.github.demo.grpc.config.GrpcServerProperties;
import com.github.demo.grpc.utils.NetUtils;
import io.grpc.*;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.services.HealthStatusManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Hosts embedded gRPC server.
 */

public class GrpcServer implements CommandLineRunner, DisposableBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private HealthStatusManager healthStatusManager;

    @Autowired
    private AbstractApplicationContext applicationContext;

    @Autowired
    private GrpcServerProperties grpcServerProperties;

    private Server server;


    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting gRPC Server ...");

        final ServerBuilder<?> serverBuilder = ServerBuilder.forPort(grpcServerProperties.getPort());

        // find all interceptor
        Collection<ServerInterceptor> globalInterceptors = getBeanNamesByTypeWithAnnotation(GrpcGlobalInterceptor.class, ServerInterceptor.class)
                .map(name -> applicationContext.getBeanFactory().getBean(name, ServerInterceptor.class))
                .collect(Collectors.toList());

        // Adding health service
        serverBuilder.addService(healthStatusManager.getHealthService());

        // find and register all GRpcService-enabled beans
        getBeanNamesByTypeWithAnnotation(GrpcService.class, BindableService.class)
                .forEach(name -> {
                    BindableService srv = applicationContext.getBeanFactory().getBean(name, BindableService.class);
                    ServerServiceDefinition serviceDefinition = srv.bindService();
                    GrpcService annotation = applicationContext.findAnnotationOnBean(name, GrpcService.class);
                    // bind interceptors
                    serviceDefinition = bindInterceptors(serviceDefinition, annotation, globalInterceptors);
                    serverBuilder.addService(serviceDefinition);
                    String serviceName = serviceDefinition.getServiceDescriptor().getName();
                    healthStatusManager.setStatus(serviceName, HealthCheckResponse.ServingStatus.SERVING);

                    logger.info("'{}' service has been registered.", srv.getClass().getName());

                });

        server = serverBuilder.build().start();
        applicationContext.publishEvent(new GrpcServerInitializedEvent(applicationContext, server));

        logger.info("gRPC Server started, listening on port {}.", server.getPort());
        startDaemonAwaitThread();

    }

    private Instance createInstance() {
        Instance instance = new Instance();
        instance.setIp(NetUtils.getLocalHost());
        instance.setPort(grpcServerProperties.getPort());
        return instance;
    }

    private ServerServiceDefinition bindInterceptors(ServerServiceDefinition serviceDefinition, GrpcService grpcService, Collection<ServerInterceptor> globalInterceptors) {
        Stream<? extends ServerInterceptor> privateInterceptors = Stream.of(grpcService.interceptors())
                .map(interceptorClass -> {
                    try {
                        return 0 < applicationContext.getBeanNamesForType(interceptorClass).length ?
                                applicationContext.getBean(interceptorClass) :
                                interceptorClass.newInstance();
                    } catch (Exception e) {
                        throw new BeanCreationException("Failed to create interceptor instance.", e);
                    }
                });

        List<ServerInterceptor> interceptors = Stream.concat(
                grpcService.applyGlobalInterceptors() ? globalInterceptors.stream() : Stream.empty(),
                privateInterceptors)
                .distinct()
                .sorted(serverInterceptorOrderComparator())
                .collect(Collectors.toList());
        return ServerInterceptors.intercept(serviceDefinition, interceptors);
    }

    private Comparator<Object> serverInterceptorOrderComparator() {
        Function<Object, Boolean> isOrderAnnotated = obj -> {
            Order ann = obj instanceof Method ? AnnotationUtils.findAnnotation((Method) obj, Order.class) :
                    AnnotationUtils.findAnnotation(obj.getClass(), Order.class);
            return ann != null;
        };
        return AnnotationAwareOrderComparator.INSTANCE.thenComparing((o1, o2) -> {
            boolean p1 = isOrderAnnotated.apply(o1);
            boolean p2 = isOrderAnnotated.apply(o2);
            return p1 && !p2 ? -1 : p2 && !p1 ? 1 : 0;
        }).reversed();
    }

    private void startDaemonAwaitThread() {
        Thread awaitThread = new Thread(() -> {
            try {
                this.server.awaitTermination();
            } catch (InterruptedException e) {
                logger.error("gRPC server awaiter interrupted.", e);
            }
        });
        awaitThread.setName("grpc-server-awaiter");
        awaitThread.setDaemon(false);
        awaitThread.start();
    }

    @Override
    public void destroy() throws Exception {
        Optional.ofNullable(server).ifPresent(s -> {
            logger.info("Shutting down gRPC server ...");
            s.getServices().forEach(def -> healthStatusManager.clearStatus(def.getServiceDescriptor().getName()));
            s.shutdown();
            logger.info("gRPC server stopped.");
        });

    }

    private <T> Stream<String> getBeanNamesByTypeWithAnnotation(Class<? extends Annotation> annotationType, Class<T> beanType) throws Exception {

        return Stream.of(applicationContext.getBeanNamesForType(beanType))
                .filter(name -> {
                    final BeanDefinition beanDefinition = applicationContext.getBeanFactory().getBeanDefinition(name);
                    final Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(annotationType);

                    if (beansWithAnnotation.containsKey(name)) {
                        return true;
                    } else if (beanDefinition.getSource() instanceof AnnotatedTypeMetadata) {
                        return AnnotatedTypeMetadata.class.cast(beanDefinition.getSource()).isAnnotated(annotationType.getName());

                    }
                    return false;
                });
    }


}
