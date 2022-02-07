package io.graphoenix.showcase.mysql.annotation;

import jakarta.interceptor.InterceptorBinding;

@InterceptorBinding
public @interface Test {
    String value() default "";
}
