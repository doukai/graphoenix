package io.graphoenix.spi.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface QueryOperation {
    String value();

    String selectionSet() default "";

    int layers() default 0;
}
