# Nacos + Spring Cloud + Grpc.

Grpc Server and Client integration with Nacos

## Usage

**Server**:
```
@GrpcService
public class GreeterServiceImpl extends GreeterServiceGrpc.GreeterServiceImplBase {
    
    @Override
    public void greet() {
        // your logic
    }
}
```

**Client** `Direct`:
```
@GrpcClient
private GreeterServiceGrpc.GreeterServiceFutureStub stub

// some code

stub.greet();
```

**Client** `Broadcast`:
```

NacosDiscoveryProperties nacosDiscoveryProperties = ApplicationContextHelper.getBean(NacosDiscoveryProperties.class);

final Instance instance = nacosDiscoveryProperties.namingServiceInstance().selectOneHealthyInstance("xxxx", true);

final ManagedChannel channel = ManagedChannelBuilder.forAddress(instance.getIPAddr(), instance.getPort())
                .usePlaintext()
                .build();
        final GreeterServiceGrpc.GreeterServiceFutureStub stub = GreeterServiceGrpc.newFutureStub(channel);
        stub.greet(name);

```

> **NOTE**: Javaassist and CGLIB not support final class.
JDK Proxy support interface only.
I want @GrpcClient inject and provider different call type, direct or broadcast. But currently only automatic inject and robin call available. if your have any idea, please contact me.