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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;

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
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(doNotUseGetters = true)
@NonNullByDefault({}) // must not restrict types to non-nullable ones 
public class ValueOrProblem<V, P>
{
	/**
	 * Creates {@link ValueOrProblem} instance containing specific value (and
	 * no problem).
	 */
	@Nonnull
	public static <V, P> ValueOrProblem<V, P> ofValue(V value)
	{
		return new ValueOrProblem<V, P>(true, value, fakeNonNull());
	}
	
	/**
	 * Create {@link ValueOrProblem} instance containing specific problem (and
	 * no value).
	 */
	@Nonnull
	public static <V, P> ValueOrProblem<V, P> ofProblem(P problem)
	{
		return new ValueOrProblem<V, P>(false, fakeNonNull(), problem);
	}
	
	/**
	 * Helper method: for {@link ValueOrProblem} instances that declare problem
	 * as some {@link Throwable} subclass this method will obtain value from the
	 * given instance if available ({@link ValueOrProblem#getValue()}) or else 
	 * throw the corresponding exception from the problem ({@link ValueOrProblem#getProblem()}). 
	 */
	public static <V, @Nonnull E extends Throwable> V getValueOrThrowProblem(ValueOrProblem<V, E> valueOrProblem)
		throws E
	{
		if (valueOrProblem.hasValue())
			return valueOrProblem.getValue();
		
		throw valueOrProblem.getProblem();
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
	 * Gets value from this instance if present; returns null if no value is present.
	 */
	@Nullable
	public V getValueOrNull() throws NoSuchElementException
	{
		if (!hasValue)
    		return null;
		
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
	/**
	 * Gets problem from this instance if present; returns null if no problem is present.
	 */
	@Nullable
	public P getProblemOrNull() throws NoSuchElementException
	{
		if (hasValue)
    		return null;
		
		return problem;
	}
	
}
