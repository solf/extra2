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
package io.github.solf.extra2.testutil;

import org.eclipse.jdt.annotation.NonNullByDefault;

import lombok.Getter;

/**
 * Test class for testing accessing inaccessible stuff.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
/*package*/ class TestInaccessibleValue
{
	@Getter
	private int value;
	
	/**
	 * No-arg private constructor -- sets value to -1.
	 */
	@SuppressWarnings("unused")
	private TestInaccessibleValue()
	{
		value = -1;
	}
	
	/**
	 * Constructor.
	 */
	public TestInaccessibleValue(int value)
	{
		this.value = value;
	}
	
	/**
	 * Two arg private constructor -- sets value to the sum of values.
	 */
	@SuppressWarnings("unused")
	private TestInaccessibleValue(int v1, int v2)
	{
		value = v1 + v2;
	}
}
