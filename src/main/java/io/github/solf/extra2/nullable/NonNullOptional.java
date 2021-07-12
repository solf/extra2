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
package io.github.solf.extra2.nullable;

import static io.github.solf.extra2.util.NullUtil.fakeNonNull;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.solf.extra2.util.TypeUtil;
import lombok.NonNull;
import lombok.ToString;

/**
 * A version of {@link NullableOptional} that can only contain non-null values.
 * <p>
 * It is similar to {@link Optional} but can e.g. additionally contain an exception.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
@ToString(callSuper = true)
public class NonNullOptional<@Nonnull T> extends NullableOptional<T>
{
    /**
     * Common instance for {@code empty()}.
     */
    private static final NonNullOptional<?> EMPTY = new NonNullOptional<>(false, fakeNonNull(), null);

	
	/**
	 * Returns an empty {@link NonNullOptional}, no value is stored in it.
	 */
	public static <@Nonnull T> NonNullOptional<T> empty()
	{
		return TypeUtil.coerce(EMPTY);
	}
	
	/**
	 * Returns an empty {@link NonNullOptional} (no value is stored) but with
	 * the specified exception.
	 * 
	 * @param exception may be null in which case no exception is stored
	 */
	public static <@Nonnull T> NonNullOptional<T> emptyWithException(@Nullable Throwable exception)
	{
		return new NonNullOptional<T>(false, fakeNonNull(), exception);
	}
	

    /**
     * Returns a {@link NonNullOptional} with the specified present value 
     * (never null).
     *
     * @param <T> the class of the value
     * @param value the value to be present, never null
     * @return an {@code Optional} with the value present
     */
    public static <@Nonnull T> NonNullOptional<T> of(@NonNull T value) 
    {
        return new NonNullOptional<>(true, value, null);
    }

    /**
     * Returns a {@link NonNullOptional} with the specified present value 
     * (never null) and exception
     *
     * @param <T> the class of the value
     * @param value the value to be present, possibly null
	 * @param exception may be null in which case no exception is stored
     * @return an {@code Optional} with the value present
     */
    public static <@Nonnull T> NonNullOptional<T> ofWithException(@NonNull T value, Throwable exception) 
    {
        return new NonNullOptional<>(true, value, exception);
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
    public static <@Nonnull T> NonNullOptional<T> fromPrototype(@NonNull T newValue, NonNullOptional<?> prototype) 
    {
    	boolean hasValue = prototype.hasValue;
    	if (!hasValue)
    		return TypeUtil.coerce(prototype);
    	
        return new NonNullOptional<>(true, newValue, prototype.exception);
    }
    
    /**
     * Builds {@link NonNullOptional} from the given {@link NullableOptional}
     * IF the held value (if any) is not-null.
     *
     * @throws IllegalArgumentException if argument {@link NullableOptional}
     * 		contains null value
     */
    @NonNullByDefault({})
    @Nonnull
    public static <T> NonNullOptional<@Nonnull T> fromNullableOptionalIfNonNull(@Nonnull NullableOptional<T> prototype)
    	throws IllegalArgumentException
    {
    	boolean hasValue = prototype.hasValue;
    	if (!hasValue)
    		return new NonNullOptional<>(false, fakeNonNull(prototype.value), prototype.exception);
    	
    	T v = prototype.value;
    	if (v == null)
    		throw new IllegalArgumentException("Unable to create NonNullOptional from NullableOptional with null value: " + prototype);
    	
    	return new NonNullOptional<>(true, v, prototype.exception);
    }
    
    /**
     * Builds {@link NonNullOptional} from the given {@link NullableOptional}
     * ({@link NullableOptional} is supposed to be type-ized as holding non-null
     * values; use {@link #fromNullableOptionalIfNonNull(NullableOptional)} if
     * {@link NullableOptional} it typeized to potentially hold null values).
     *
     * @throws IllegalArgumentException if argument {@link NullableOptional}
     * 		contains null value
     */
    @Nonnull
    public static <@Nonnull T> NonNullOptional<T> fromNullableOptional(NullableOptional<T> prototype)
    	throws IllegalArgumentException
    {
    	return fromNullableOptionalIfNonNull(prototype);
    }

	/**
	 * Internal constructor.
	 */
	protected NonNullOptional(boolean hasValue, T value,
		@Nullable Throwable exception)
	{
		super(hasValue, value, exception);
	}

}
