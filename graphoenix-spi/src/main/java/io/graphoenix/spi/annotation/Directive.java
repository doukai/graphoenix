package io.graphoenix.spi.annotation;

import java.lang.annotation.*;

@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Directive {
    String value();
}
