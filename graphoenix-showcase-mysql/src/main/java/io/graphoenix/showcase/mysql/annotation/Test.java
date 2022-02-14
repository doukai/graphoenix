package io.graphoenix.showcase.mysql.annotation;

import jakarta.interceptor.InterceptorBinding;
import org.eclipse.microprofile.graphql.Name;

@InterceptorBinding
@Name("test")
public @interface Test {
    String value() default "";
}
