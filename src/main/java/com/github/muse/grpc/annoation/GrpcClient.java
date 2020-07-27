package com.github.muse.grpc.annoation;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface GrpcClient {
    String service() default "";
    boolean broadcast() default false;
}
