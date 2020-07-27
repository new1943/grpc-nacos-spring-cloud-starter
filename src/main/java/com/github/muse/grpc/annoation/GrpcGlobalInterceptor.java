package com.github.muse.grpc.annoation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;


/**
 * @author zhenglingbing
 */
@Target({ElementType.TYPE,ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface GrpcGlobalInterceptor {
}
