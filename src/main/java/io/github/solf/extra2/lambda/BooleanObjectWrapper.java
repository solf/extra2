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

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Convenience/optimization version of {@link ObjectWrapper}
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class BooleanObjectWrapper extends ObjectWrapper<Boolean>
{

	/**
	 * @param value
	 */
	protected BooleanObjectWrapper(Boolean value)
	{
		super(value);
	}

	/**
	 * Constructs wrapper for the given argument.
	 */
	public static BooleanObjectWrapper of(boolean value)
	{
		if (value)
			return new BooleanObjectWrapper(Boolean.TRUE);
		else
			return new BooleanObjectWrapper(Boolean.FALSE);
	}
	
	/**
	 * Constructs wrapper for the given argument.
	 */
	public static BooleanObjectWrapper of(Boolean value)
	{
		return new BooleanObjectWrapper(value);
	}
	
	/**
	 * Sets value to true.
	 * 
	 * @return this for method chaining
	 */
	public BooleanObjectWrapper setTrue()
	{
		set(Boolean.TRUE);
		
		return this;
	}
	
	/**
	 * Sets value to false.
	 * 
	 * @return this for method chaining
	 */
	public BooleanObjectWrapper setFalse()
	{
		set(Boolean.FALSE);
		
		return this;
	}
	
	/**
	 * Checks if this is true.
	 */
	public boolean isTrue()
	{
		return get().booleanValue();
	}
}
