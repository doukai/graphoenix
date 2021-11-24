package io.graphoenix.spi.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface MutationOperation {
    String value();

    int layers() default 0;
}
