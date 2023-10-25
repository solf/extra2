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
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.solf.extra2.concurrent.FunctionWithExceptionType;
import io.github.solf.extra2.concurrent.InterruptableConsumer;
import io.github.solf.extra2.concurrent.InterruptableRunnable;
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
	
    /**
     * If you need to return value from the handler, see {@link #ifValue(FunctionWithExceptionType)}
     * <p>
     * If a value is present, invoke the specified consumer with the value,
     * otherwise do nothing.
     *
     * @param consumer block to be executed if a value is present
     * @throws NullPointerException if value is present and {@code consumer} is
     * null
     * 
     * @return this for method chaining
     */
    public ValueOrProblem<V, P> ifValue(Consumer<? super V> consumer) 
    {
        if (hasValue())
            consumer.accept(getValue());
        
        return this;
    }

    /**
     * If you need to return value from the handler, see {@link #ifValueInterruptibly(FunctionWithExceptionType)}
     * <p>
     * If a value is present, invoke the specified consumer with the value,
     * otherwise do nothing.
     * <p>
     * Same as {@link #ifValue(Consumer)} except allows {@link InterruptedException}
     *
     * @param consumer block to be executed if a value is present
     * @throws NullPointerException if value is present and {@code consumer} is
     * null
     * 
     * @return this for method chaining
     */
    public ValueOrProblem<V, P> ifValueInterruptibly(InterruptableConsumer<? super V> consumer)
    	throws InterruptedException
    {
        if (hasValue())
            consumer.accept(getValue());
        
        return this;
    }


    /**
     * If a value is NOT present, invoke the specified runnable,
     * otherwise do nothing.
     *
     * @param runnable block to be executed if a value is present
     * @throws NullPointerException if value is present and {@code runnable} is
     * null
     * 
     * @return this for method chaining
     */
    public ValueOrProblem<V, P> ifNoValue(Runnable runnable) 
    {
        if (!hasValue())
            runnable.run();
        
        return this;
    }

    /**
     * If a value is NOT present, invoke the specified runnable,
     * otherwise do nothing.
     * <p>
     * Same as {@link #ifNoValue(Runnable)} except allows {@link InterruptedException}
     *
     * @param runnable block to be executed if a value is present
     * @throws NullPointerException if value is present and {@code runnable} is
     * null
     * 
     * @return this for method chaining
     */
    public ValueOrProblem<V, P> ifNoValueInterruptibly(InterruptableRunnable runnable)
    	throws InterruptedException
    {
        if (!hasValue())
            runnable.run();
        
        return this;
    }
    

    /**
     * If a problem is present, invoke the specified consumer with the problem,
     * otherwise do nothing.
     *
     * @param consumer block to be executed if an exception is present
     * @throws NullPointerException if value is present and {@code consumer} is
     * null
     * 
     * @return this for method chaining
     */
    public ValueOrProblem<V, P> ifProblem(Consumer<? super P> consumer) 
    {
    	if (hasProblem())
    		consumer.accept(getProblem());
        
        return this;
    }

    /**
     * If a problem is present, invoke the specified consumer with the problem,
     * otherwise do nothing.
     * <p>
     * Same as {@link #ifProblem(Consumer)} except allows {@link InterruptedException}
     *
     * @param consumer block to be executed if an exception is present
     * @throws NullPointerException if value is present and {@code consumer} is
     * null
     * 
     * @return this for method chaining
     */
    public ValueOrProblem<V, P> ifProblemInterruptibly(InterruptableConsumer<? super P> consumer)
    	throws InterruptedException
    {
    	if (hasProblem())
    		consumer.accept(getProblem());
        
        return this;
    }
    
    /**
     * If a value is present, invokes the specified value processor and returns
     * its result; otherwise invokes the specified problem processor and returns
     * its result.
     * <p> 
     * This is intended to be used in code like this:
			ValueOrProblem<String, Exception> valueOrProblem = ...
			String msg = valueOrProblem.ifValue(v -> {
				return v;
			}).orElse(e -> {
				return e.toString();
			});
     *
     * @param v function to be executed if a value is present
     * @throws NullPointerException can be thrown if v is null
     * 
     * @return {@link ElseFunction} that should be used to chain .orElse(..) call
     */
    public <R> ElseFunction<P, R, @Nonnull RuntimeException> ifValue(
    	FunctionWithExceptionType<? super V, R, @Nonnull RuntimeException> v) 
    {
    	return ifValueWithExceptionType(v);
    }

    /**
     * If a value is present, invokes the specified value processor and returns
     * its result; otherwise invokes the specified problem processor and returns
     * its result.
     * <p> 
     * This is intended to be used in code like this:
			ValueOrProblem<String, Exception> valueOrProblem = ...
			String msg = valueOrProblem.ifValue(v -> {
				return v;
			}).orElse(e -> {
				return e.toString();
			});
	 * <p>
	 * This processors (and the method) are allowed to throw {@link InterruptedException}
	 * in this case.
     *
     * @param v function to be executed if a value is present
     * @throws NullPointerException can be thrown if v is null
     * 
     * @return {@link ElseFunction} that should be used to chain .orElse(..) call
     */
    public <R> ElseFunction<P, R, @Nonnull InterruptedException> ifValueInterruptibly(
    	FunctionWithExceptionType<? super V, R, @Nonnull InterruptedException> v)
    		throws InterruptedException
    {
    	return ifValueWithExceptionType(v);
    }
    
    /**
     * If a value is present, invokes the specified value processor and returns
     * its result; otherwise invokes the specified problem processor and returns
     * its result.
     * <p> 
     * This is intended to be used in code like this:
			ValueOrProblem<String, Exception> valueOrProblem = ...
			String msg = valueOrProblem.ifValue(v -> {
				return v;
			}).orElse(e -> {
				return e.toString();
			});
	 * <p>
	 * This processors (and the method) are allowed to throw any specific
	 * desired Exception subclass (invocation may require weird casting
	 * constructs depending on the usecase; for simpler cases you should consider
	 * {@link #ifValue(FunctionWithExceptionType)} and {@link #ifValueInterruptibly(FunctionWithExceptionType)}
	 * methods instead.
     *
     * @param v function to be executed if a value is present
     * @throws NullPointerException can be thrown if v is null
     * 
     * @return {@link ElseFunction} that should be used to chain .orElse(..) call
     */
    public <R, @Nonnull E extends Exception> ElseFunction<P, R, E> ifValueWithExceptionType(
    	FunctionWithExceptionType<? super V, R, E> v)
    		throws E
    {
        if (hasValue())
        {
            R result = v.apply(getValue());
        	return ElseFunction.ofKnownResult(result);
        }
        else
        {
        	return ElseFunction.ofResultToBeCalculated(getProblem());
        }
    }
}
