package com.muse.grpc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * @author muse
 */
@ConfigurationProperties("grpc")
public class GrpcServerProperties {
    /**
     * gRPC server port
     */
    private int port = 20880;

    /**
     * Enables the embedded grpc server.
     */
    private boolean enabled = true;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }




}
