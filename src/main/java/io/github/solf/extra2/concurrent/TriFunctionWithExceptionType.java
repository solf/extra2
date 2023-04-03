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
package io.github.solf.extra2.concurrent;

import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Represents a function that accepts three arguments and produces a result.
 * This is the three-arity specialization of {@link Function}.
 * <p>
 * It is allowed to throw specific {@link Exception} type
 * (checked or unchecked).
 *
 * <p>This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #apply(Object, Object)}.
 *
 * @param <A1> the type of the first argument to the function
 * @param <A2> the type of the second argument to the function
 * @param <A3> the type of the first argument to the function
 * @param <R> the type of the result of the function
 *
 * @author Sergey Olefir
 */
@FunctionalInterface
@NonNullByDefault({}) // this is to solve the problem of Eclipse warning 'interface doesn't appear to be designed with nullability in mind'
public interface TriFunctionWithExceptionType<A1, A2, A3, R, E extends Exception>
{
    /**
     * Applies this function to the given arguments.
     *
     * @param a1 the first function argument
     * @param a2 the second function argument
     * @param a2 the third function argument
     * @return the function result
     */
    R apply(A1 a1, A2 a2, A3 a3) throws E;
}
