package io.graphoenix.spi.annotation.dagger;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GPXModule {

    Class<?>[] includes() default {};

    Class<?>[] subcomponents() default {};
}