package io.graphoenix.spi.annotation.dagger;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface GPXComponent {
    String value() default "";

    String getMethodName() default "get";

    String packageName() default "";
}
