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

import java.util.NoSuchElementException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.ToString;

/**
 * Convenient wraper class for the cases where processing must return a value OR
 * information about a processing problem (and where throwing exception is not
 * appropriate).
 * <p>
 * Instances of this class always contain EITHER value OR problem, never both, 
 * never none.
 * <p>
 * Problem type can often be an (some subclass of) exception but it is not
 * required. 
 *
 * @author Sergey Olefir
 */
//@NonNullByDefault must not restrict types to non-nullable ones
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(doNotUseGetters = true)
public class ValueOrProblem<V, P>
{
	/**
	 * Creates {@link ValueOrProblem} instance containing specific value (and
	 * no problem).
	 */
	public static <V, P> ValueOrProblem<V, P> ofValue(V value)
	{
		return new ValueOrProblem<V, P>(true, value, fakeNonNull());
	}
	
	/**
	 * Create {@link ValueOrProblem} instance containing specific problem (and
	 * no value).
	 */
	public static <V, P> ValueOrProblem<V, P> ofProblem(P problem)
	{
		return new ValueOrProblem<V, P>(false, fakeNonNull(), problem);
	}
	
	/**
	 * Whether instance contains value (false means it contains problem). 
	 */
	private final boolean hasValue;
	/**
	 * True if this instance contains value (and no problem) -- {@link #getValue()}
	 * method can be called without exception and {@link #getProblem()} will 
	 * fail.
	 */
	public boolean hasValue()
	{
		return hasValue;
	}
	/**
	 * True if this instance contains problem (and no value) -- {@link #getProblem()}
	 * method can be called without exception and {@link #getValue()} will fail.
	 */
	public boolean hasProblem()
	{
		return !hasValue();
	}
	
	/**
	 * Value (if any) stored in this instance.
	 */
	private final V value;
	/**
	 * Gets value from this instance.
	 * 
	 * @throws NoSuchElementException if called on instance that has no value
	 */
	public V getValue() throws NoSuchElementException
	{
		if (!hasValue)
    		throw new NoSuchElementException("No value present");
		
		return value;
	}
	
	/**
	 * Problem (if any) stored in this instance.
	 */
	private final P problem;
	/**
	 * Gets problem from this instance.
	 * 
	 * @throws NoSuchElementException if called on instance that has no problem
	 */
	public P getProblem() throws NoSuchElementException
	{
		if (hasValue)
    		throw new NoSuchElementException("No problem present");
		
		return problem;
	}
	
}
