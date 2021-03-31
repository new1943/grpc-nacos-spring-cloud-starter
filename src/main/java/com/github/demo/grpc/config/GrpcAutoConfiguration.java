package com.github.demo.grpc.config;

import com.github.demo.grpc.GrpcServer;
import com.github.demo.grpc.annoation.GrpcService;
import com.github.demo.grpc.context.GrpcClientBeanPostProcessor;
import io.grpc.services.HealthStatusManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;


@AutoConfigureOrder
@ConditionalOnBean(annotation = GrpcService.class)
@EnableConfigurationProperties({GrpcServerProperties.class})
public class GrpcAutoConfiguration {

    @Autowired
    private GrpcServerProperties grpcServerProperties;

    @Bean
    @ConditionalOnProperty(value = "grpc.enabled", havingValue = "true", matchIfMissing = true)
    public GrpcServer grpcServer() {
        return new GrpcServer();
    }

    @Bean
    static GrpcClientBeanPostProcessor grpcClientBeanPostProcessor() {
        return new GrpcClientBeanPostProcessor();
    }

    @Bean
    public HealthStatusManager healthStatusManager() {
        return new HealthStatusManager();
    }


}
