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
package com.github.solf.extra2.nullable;

import static com.github.solf.extra2.util.NullUtil.fakeNonNull;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.DefaultLocation;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.solf.extra2.concurrent.InterruptableConsumer;
import com.github.solf.extra2.concurrent.InterruptableRunnable;
import com.github.solf.extra2.util.TypeUtil;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * A version of {@link Optional} that is allowed to keep null values.
 * <p>
 * It also optionally contains Throwable
 *
 * @author Sergey Olefir
 */
// Exclude TYPE_ARGUMENT
@ParametersAreNonnullByDefault({DefaultLocation.PARAMETER, DefaultLocation.RETURN_TYPE, DefaultLocation.FIELD, DefaultLocation.TYPE_BOUND, DefaultLocation.ARRAY_CONTENTS}) 
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(doNotUseGetters = true) // getters cause exceptions
public class NullableOptional<T>
{
    /**
     * Common instance for {@code empty()}.
     */
    private static final NullableOptional<?> EMPTY = new NullableOptional<>(false, null, null);
	
	/**
	 * Whether value is present.
	 */
	protected final boolean hasValue;
	
	/**
	 * Value (if present), may be null.
	 */
	protected final T value;
	
	/**
	 * Exception if present.
	 */
	@Nullable
	protected final Throwable exception;
	
	/**
	 * Returns an empty {@link NullableOptional}, no value is stored in it.
	 */
	public static <T> NullableOptional<T> empty()
	{
		return TypeUtil.coerce(EMPTY);
	}
	
	/**
	 * Returns an empty {@link NullableOptional} (no value is stored) but with
	 * the specified exception.
	 * 
	 * @param exception may be null in which case no exception is stored
	 */
	public static <T> NullableOptional<T> emptyWithException(Throwable exception)
	{
		return new NullableOptional<T>(false, fakeNonNull(), exception);
	}
	

    /**
     * Returns a {@link NullableOptional} with the specified present value 
     * (possibly null).
     *
     * @param <T> the class of the value
     * @param value the value to be present, possibly null
     * @return an {@code Optional} with the value present
     */
    public static <T> NullableOptional<T> of(T value) 
    {
        return new NullableOptional<>(true, value, null);
    }

    /**
     * Returns a {@link NullableOptional} with the specified present value 
     * (possibly null) and exception
     *
     * @param <T> the class of the value
     * @param value the value to be present, possibly null
	 * @param exception may be null in which case no exception is stored
     * @return an {@code Optional} with the value present
     */
    public static <T> NullableOptional<T> ofWithException(T value, Throwable exception) 
    {
        return new NullableOptional<>(true, value, exception);
    }
    
    /**
     * Method to 'replace' the value of an optional with another value (ONLY if
     * original optional (prototype) HAS the value).
     * <p>
     * Returns original optional (prototype) if it is {@link #isEmpty()} (after
     * properly coercing the type of course).
     * <p>
     * Returns new optional with the given newValue if original optional (prototype)
     * is {@link #isPresent()} AND copies exception information from the original
     * optional (prototype) if exception is present.
     */
    public static <T> NullableOptional<T> fromPrototype(T newValue, NullableOptional<?> prototype) 
    {
    	boolean hasValue = prototype.hasValue;
    	if (!hasValue)
    		return TypeUtil.coerce(prototype);
    	
        return new NullableOptional<>(true, newValue, prototype.exception);
    }
    

    /**
     * Return {@code true} if there is a value present, otherwise {@code false}
     * (regardless of exception presence).
     *
     * @return {@code true} if there is a value present, otherwise {@code false}
     * 		(regardless of exception presence).
     */
    public boolean isPresent() 
    {
        return hasValue;
    }

    /**
     * Return {@code false} if there is a value present, otherwise {@code true} 
     * (regardless of exception presence).
     *
     * @return {@code false} if there is a value present, otherwise {@code true}
     * 		(regardless of exception presence).
     */
    public boolean isEmpty() 
    {
        return !hasValue;
    }

    /**
     * If a value is present in this {@link NullableOptional}, returns the value,
     * otherwise throws {@code NoSuchElementException}.
     *
     * @return the value held by this {@link NullableOptional}
     * @throws NoSuchElementException if there is no value present
     *
     * @see #isPresent()
     */
    public T get() 
    {
        if (!hasValue)
        {
        	Throwable cause = exception;
        	if (cause == null)
        		throw new NoSuchElementException("No value present");
        	else
        	{
        		NoSuchElementException toThrow = new NoSuchElementException("No value present, has exception: " + cause);
        		toThrow.initCause(cause);
        		throw toThrow;
        	}
        }
        
        return value;
    }
    
    /**
     * Returns exception if present otherwise throws {@link NoSuchElementException}
     * 
     * @return stored exception if present
     * @throws NoSuchElementException if there's no exception present
     */
    public Throwable getException()
    {
    	Throwable result = exception;
    	
    	if (result == null)
            throw new NoSuchElementException("No exception present");
    	
    	return result;
    }
    
    /**
     * Whether this {@link NullableOptional} has stored exception.
     */
    public boolean hasException()
    {
    	return exception != null;
    }
    

    /**
     * If a value is present, invoke the specified consumer with the value,
     * otherwise do nothing.
     *
     * @param consumer block to be executed if a value is present
     * @throws NullPointerException if value is present and {@code consumer} is
     * null
     * 
     * @return this for method chaining
     */
    public NullableOptional<T> ifPresent(Consumer<? super T> consumer) 
    {
        if (hasValue)
            consumer.accept(value);
        
        return this;
    }

    /**
     * If a value is present, invoke the specified consumer with the value,
     * otherwise do nothing.
     * <p>
     * Same as {@link #ifPresent(Consumer)} except allows {@link InterruptedException}
     *
     * @param consumer block to be executed if a value is present
     * @throws NullPointerException if value is present and {@code consumer} is
     * null
     * 
     * @return this for method chaining
     */
    public NullableOptional<T> ifPresentInterruptibly(InterruptableConsumer<? super T> consumer)
    	throws InterruptedException
    {
        if (hasValue)
            consumer.accept(value);
        
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
    public NullableOptional<T> ifNotPresent(Runnable runnable) 
    {
        if (!hasValue)
            runnable.run();
        
        return this;
    }

    /**
     * If a value is NOT present, invoke the specified runnable,
     * otherwise do nothing.
     * <p>
     * Same as {@link #ifNotPresent(Runnable)} except allows {@link InterruptedException}
     *
     * @param runnable block to be executed if a value is present
     * @throws NullPointerException if value is present and {@code runnable} is
     * null
     * 
     * @return this for method chaining
     */
    public NullableOptional<T> ifNotPresentInterruptibly(InterruptableRunnable runnable)
    	throws InterruptedException
    {
        if (!hasValue)
            runnable.run();
        
        return this;
    }
    

    /**
     * If an exception is present, invoke the specified consumer with the exception,
     * otherwise do nothing.
     *
     * @param consumer block to be executed if an exception is present
     * @throws NullPointerException if value is present and {@code consumer} is
     * null
     * 
     * @return this for method chaining
     */
    public NullableOptional<T> ifException(Consumer<@Nonnull Throwable> consumer) 
    {
    	Throwable e = exception;
    	
        if (e != null)
            consumer.accept(e);
        
        return this;
    }

    /**
     * If an exception is present, invoke the specified consumer with the exception,
     * otherwise do nothing.
     * <p>
     * Same as {@link #ifException(Consumer)} except allows {@link InterruptedException}
     *
     * @param consumer block to be executed if an exception is present
     * @throws NullPointerException if value is present and {@code consumer} is
     * null
     * 
     * @return this for method chaining
     */
    public NullableOptional<T> ifExceptionInterruptibly(InterruptableConsumer<@Nonnull Throwable> consumer)
    	throws InterruptedException
    {
    	Throwable e = exception;
    	
        if (e != null)
            consumer.accept(e);
        
        return this;
    }
}
