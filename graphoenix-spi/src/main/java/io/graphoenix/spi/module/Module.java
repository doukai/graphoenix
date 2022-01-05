package io.graphoenix.spi.module;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Module {
    /**
     * Additional {@code @Module}-annotated classes from which this module is composed. The
     * de-duplicated contributions of the modules in {@code includes}, and of their inclusions
     * recursively, are all contributed to the object graph.
     */
    Class<?>[] includes() default {};
}
