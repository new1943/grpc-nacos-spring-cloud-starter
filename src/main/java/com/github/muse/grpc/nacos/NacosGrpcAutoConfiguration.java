package com.github.muse.grpc.nacos;

import com.alibaba.cloud.nacos.ConditionalOnNacosDiscoveryEnabled;
import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.registry.NacosServiceRegistry;
import com.alibaba.cloud.nacos.registry.NacosServiceRegistryAutoConfiguration;
import com.github.muse.grpc.GrpcServer;
import com.github.muse.grpc.config.GrpcAutoConfiguration;
import com.github.muse.grpc.config.GrpcServerProperties;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnNacosDiscoveryEnabled
@AutoConfigureAfter({NacosServiceRegistryAutoConfiguration.class, GrpcAutoConfiguration.class})
@ConditionalOnBean({NacosServiceRegistry.class, GrpcServer.class})
public class NacosGrpcAutoConfiguration {


    @Bean
    @ConditionalOnMissingBean
    public NacosGrpcRegistry nacosGrpcRegistry(NacosServiceRegistry nacosServiceRegistry, NacosDiscoveryProperties nacosDiscoveryProperties , GrpcServerProperties grpcServerProperties) {
        return new NacosGrpcRegistry(nacosServiceRegistry, nacosDiscoveryProperties, grpcServerProperties);
    }
}
