package com.sidutti.charlie.agent;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Component
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE) // Apply to classes
public @interface Worker {
    String goal();
    String background() default "";
    boolean disabled() default false;
}
