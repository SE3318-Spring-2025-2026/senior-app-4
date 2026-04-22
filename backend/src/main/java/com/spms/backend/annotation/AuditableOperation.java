package com.spms.backend.annotation;

import com.spms.backend.model.ActionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that should be intercepted for Audit Logging.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditableOperation {
    /**
     * The type of action being logged.
     */
    ActionType actionType();

    /**
     * Optional explicit event details template. 
     */
    String eventDetails() default "";
}
