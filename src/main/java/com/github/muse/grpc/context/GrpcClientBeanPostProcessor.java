package com.github.muse.grpc.context;

import com.alibaba.spring.beans.factory.annotation.AbstractAnnotationBeanPostProcessor;
import com.github.muse.grpc.nacos.NacosNameResolverProvider;
import com.github.muse.grpc.annoation.GrpcClient;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.AbstractAsyncStub;
import io.grpc.stub.AbstractBlockingStub;
import io.grpc.stub.AbstractFutureStub;
import io.grpc.stub.AbstractStub;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.Assert;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.alibaba.spring.util.AnnotationUtils.getAttributes;

public class GrpcClientBeanPostProcessor extends AbstractAnnotationBeanPostProcessor implements
        ApplicationContextAware {

    /**
     * Cache size
     */

    private final ConcurrentMap<String, AbstractStub<?>> beanCache =
            new ConcurrentHashMap<>(32);

    private final ConcurrentMap<InjectionMetadata.InjectedElement, AbstractStub<?>> injectedFieldBeanCache =
            new ConcurrentHashMap<>(32);

    private final ConcurrentMap<InjectionMetadata.InjectedElement, AbstractStub<?>> injectedMethodBeanCache =
            new ConcurrentHashMap<>(32);

    private ApplicationContext applicationContext;

    public GrpcClientBeanPostProcessor() {
        super(GrpcClient.class);
    }

    @Override
    protected Object doGetInjectedBean(AnnotationAttributes attributes, Object bean, String beanName, Class<?> injectedType, InjectionMetadata.InjectedElement injectedElement) throws Exception {
        String name = generateBeanName(attributes, injectedType);
        AbstractStub s = buildGrpcClientBeanIfAbsent(name, attributes, injectedType);
        cacheInjectedBean(s, injectedElement);
        return s;
    }

    @Override
    protected String buildInjectedObjectCacheKey(AnnotationAttributes attributes, Object bean, String beanName, Class<?> injectedType, InjectionMetadata.InjectedElement injectedElement) {
        return generateBeanName(attributes, injectedType) +
                "#source=" + (injectedElement.getMember()) +
                "#attributes=" + getAttributes(attributes, getEnvironment());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


    private void cacheInjectedBean(AbstractStub client,
                                   InjectionMetadata.InjectedElement injectedElement) {
        if (injectedElement.getMember() instanceof Field) {
            injectedFieldBeanCache.put(injectedElement, client);
        } else if (injectedElement.getMember() instanceof Method) {
            injectedMethodBeanCache.put(injectedElement, client);
        }
    }

    private <T extends AbstractStub<T>> T buildGrpcClientBeanIfAbsent(String beanName, AnnotationAttributes attributes, Class<?> injectedType) {
        T s = (T) beanCache.get(beanName);
        if (s == null) {
            s = createStub(injectedType, createChannel(attributes.getString("service")));
            getBeanFactory().registerSingleton(beanName, s);
            beanCache.put(beanName, s);
        }
        return s;
    }

    private String generateBeanName(AnnotationAttributes attributes, Class<?> injectedType) {
        StringBuilder beanNameBuilder = new StringBuilder("@GrpcClient");

        if (!attributes.isEmpty()) {
            beanNameBuilder.append('(');
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                beanNameBuilder.append(entry.getKey())
                        .append('=')
                        .append(entry.getValue())
                        .append(',');
            }
            // replace the latest "," to be ")"
            beanNameBuilder.setCharAt(beanNameBuilder.lastIndexOf(","), ')');
        }

        beanNameBuilder.append(" ").append(injectedType.getName());
        return beanNameBuilder.toString();
    }

    private Channel createChannel(String service) {
        Assert.hasText(service, "service must not be null");
        NacosNameResolverProvider nacosNameResolverProvider = applicationContext.getBean(NacosNameResolverProvider.class);
        return ManagedChannelBuilder.forTarget(service).nameResolverFactory(nacosNameResolverProvider).defaultLoadBalancingPolicy("round_robin").usePlaintext().build();
    }

    protected String deriveStubFactoryMethodName(final Class<?> stubType) {
        if (AbstractAsyncStub.class.isAssignableFrom(stubType)) {
            return "newStub";
        } else if (AbstractBlockingStub.class.isAssignableFrom(stubType)) {
            return "newBlockingStub";
        } else if (AbstractFutureStub.class.isAssignableFrom(stubType)) {
            return "newFutureStub";
        } else {
            throw new IllegalArgumentException(
                    "Unsupported stub type: " + stubType.getName() + " -> Please report this issue.");
        }
    }

    /**
     * Creates a stub of the given type.
     *
     * @param <T>      The type of the instance to be injected.
     * @param stubType The type of the stub to create.
     * @param channel  The channel used to create the stub.
     * @return The newly created stub.
     * @throws BeanInstantiationException If the stub couldn't be created.
     */
    protected <T extends AbstractStub<T>> T createStub(final Class<?> stubType, final Channel channel) {
        try {
            // First try the public static factory method
            final String methodName = deriveStubFactoryMethodName(stubType);
            final Class<?> enclosingClass = stubType.getEnclosingClass();
            final Method factoryMethod = enclosingClass.getMethod(methodName, Channel.class);
            return (T) stubType.cast(factoryMethod.invoke(null, channel));
        } catch (final Exception e) {
            try {
                // Use the private constructor as backup
                final Constructor<T> constructor = (Constructor<T>) stubType.getDeclaredConstructor(Channel.class);
                constructor.setAccessible(true);
                return constructor.newInstance(channel);
            } catch (final Exception ex) {
                e.addSuppressed(ex);
            }
            throw new BeanInstantiationException(stubType, "Failed to create gRPC client", e);
        }
    }
}
