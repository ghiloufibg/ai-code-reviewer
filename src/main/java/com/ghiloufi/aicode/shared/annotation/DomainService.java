package com.ghiloufi.aicode.shared.annotation;

import org.springframework.stereotype.Service;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark domain services.
 *
 * <p>This annotation helps identify classes that contain
 * domain business logic and should be managed by Spring.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Service
public @interface DomainService {
}