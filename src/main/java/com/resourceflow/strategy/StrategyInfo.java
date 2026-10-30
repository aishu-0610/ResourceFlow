package com.resourceflow.strategy;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation that attaches display metadata to an allocation strategy class.
 *
 * RUNTIME retention keeps the annotation in the compiled class file so that
 * AbstractAllocationStrategy can read it with reflection (getClass().getAnnotation(...)).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface StrategyInfo {

    /** Human-readable name shown in the UI and reports. */
    String name();

    /** One-paragraph explanation of how the algorithm decides. */
    String description();

    /** Time complexity of the algorithm, for the comparison screen. */
    String complexity() default "O(n)";
}
