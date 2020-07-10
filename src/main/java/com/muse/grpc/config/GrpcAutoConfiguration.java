package com.muse.grpc.config;


import com.muse.grpc.GrpcServer;
import com.muse.grpc.annoation.GrpcService;
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
    public HealthStatusManager healthStatusManager() {
        return new HealthStatusManager();
    }


}
