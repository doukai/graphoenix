package io.graphoenix.spi.aop;

import java.lang.annotation.*;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Retention(RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
public @interface InterceptorAnnotation {
    Class<? extends Annotation>[] value() default {};
}
