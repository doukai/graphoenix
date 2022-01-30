package io.graphoenix.showcase.mysql.annotation;

import io.graphoenix.spi.aop.InterceptorAnnotation;

@InterceptorAnnotation
public @interface Aspect {
    String value();
}
