/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.codegenerate.stepbuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Indicates that Step Builder should be generated for this constructor.
 *
 * @author Sergey Olefir
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.CONSTRUCTOR)
@ParametersAreNonnullByDefault
public @interface StepBuilder
{
    /**
     * Suffix used to de-collide constructors
	 * that start with the same-named first argument; suffix may also start
	 * with '^' symbol in which case it comprises the entire initial builder
	 * method name rather than just a suffix ('^' is stripped in this case)
     */
    String value() default "";
}
