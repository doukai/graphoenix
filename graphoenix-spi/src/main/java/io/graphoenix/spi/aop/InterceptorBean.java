package io.graphoenix.spi.aop;

import java.lang.annotation.*;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Retention(RUNTIME)
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD})
public @interface InterceptorBean {
    Class<? extends Annotation>[] value();
}
