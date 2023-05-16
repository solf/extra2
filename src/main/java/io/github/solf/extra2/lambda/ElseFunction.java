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
package io.github.solf.extra2.lambda;

import static io.github.solf.extra2.util.NullUtil.fakeNonNull;

import javax.annotation.Nonnull;

import io.github.solf.extra2.concurrent.FunctionWithExceptionType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * The implementation of 'orElse' logic approach, such as used in
 * {@link ValueOrProblem#ifValue(FunctionWithExceptionType)}
 *
 * @author Sergey Olefir
 * 
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 * @param <E> the type of exception that {@link #orElse(FunctionWithExceptionType)}
 * 		is allowed to throw
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ElseFunction<T, R, E extends Exception>
{
	/**
	 * Result to be returned in case {@link #hasResult} is true.
	 */
	private final R result;
	/**
	 * Whether this instance contains {@link #result} or if it needs to be
	 * calculated via function in {@link #orElse(FunctionWithExceptionType)}
	 * using {@link #elseArgument}.
	 */
	private final boolean hasResult;
	/**
	 * Argument used for function in {@link #orElse(FunctionWithExceptionType)} --
	 * used if {@link #hasResult} is false.
	 */
	private final T elseArgument;
	
	/**
	 * Builds instance using already-known result (i.e. 'if' part is true, so
	 * {@link #orElse(FunctionWithExceptionType)} method will do nothing other
	 * than return the value passed here).
	 */
	public static <T, R, E extends Exception> ElseFunction<T, R, E> ofKnownResult(R result)
	{
		return new ElseFunction<>(result, true, fakeNonNull());
	}
	
	/**
	 * Builds instance for the case when result isn't yet known (i.e. the 'if'
	 * part is false, so result value will have to be calculated in {@link #orElse(FunctionWithExceptionType)}
	 * by passing the {@link #elseArgument} to the processing function).
	 */
	public static <T, R, E extends Exception> ElseFunction<T, R, E> ofResultToBeCalculated(T elseArgument)
	{
		return new ElseFunction<>(fakeNonNull(), false, elseArgument);
	}
	
	/**
	 * Finishes the if()-orElse() method invocation chain -- depending on how
	 * instance was created it either returns an already-calculated value (if the
	 * 'if' part was true) or uses provided function and stored {@link #elseArgument}
	 * to calculate the result (if the 'if' part was false).
	 * 
	 * @param e (stands for 'else', 'exception', 'error') -- function to be
	 * 		invoked to obtain the result in case 'if' part wasn't true
	 * @return if-else chain return value
	 */
	public R orElse(FunctionWithExceptionType<T, R, @Nonnull E> e) throws E
	{
		if (hasResult)
			return result;
		
		return e.apply(elseArgument);
	}
}
