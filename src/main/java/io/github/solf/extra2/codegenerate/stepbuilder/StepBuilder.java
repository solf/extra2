/**
 * Copyright Sergey Olefir
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.solf.extra2.codegenerate.stepbuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.NonNullByDefault;

/**
 * Indicates that Step Builder should be generated for this constructor.
 *
 * @author Sergey Olefir
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.CONSTRUCTOR)
@NonNullByDefault
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
