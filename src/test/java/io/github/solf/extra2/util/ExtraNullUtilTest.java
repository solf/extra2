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
package io.github.solf.extra2.util;

import static io.github.solf.extra2.testutil.AssertExtra.assertFailsWithSubstring;
import static io.github.solf.extra2.util.NullUtil.evalIfNotNull;
import static io.github.solf.extra2.util.NullUtil.fakeNonNull;
import static io.github.solf.extra2.util.NullUtil.isNull;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.testng.annotations.Test;

/**
 * Some tests for {@link NullUtil}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class ExtraNullUtilTest
{
	/**
	 * Some tests for evalIfNotNull(..) methods.
	 */
	@Test
	public void testEvalIfNotNull()
	{
		final String defaultValue = "defaultValue";
		
		assertEquals(evalIfNotNull(defaultValue, nonNull -> nonNull.hashCode()), (Integer)defaultValue.hashCode());
		assertEquals(evalIfNotNull(defaultValue, nonNull -> nonNull.toString()), defaultValue.toString());
		assertEquals(evalIfNotNull(defaultValue, nonNull -> nonNull.hashCode(), ifNull -> null), (Integer)defaultValue.hashCode());
		assertEquals(evalIfNotNull(defaultValue, nonNull -> nonNull.toString(), ifNull -> fakeNonNull()), defaultValue.toString());
		
		assertNull(evalIfNotNull(null, nonNull -> nonNull.toString()));
		assertNull(evalIfNotNull(null, nonNull -> nonNull.toString(), ifNull -> fakeNonNull()));
		assertEquals(evalIfNotNull(null, nonNull -> nonNull.toString(), ifNull -> "123"), "123");
		
		final Function<@Nonnull String, String> nullFunc = fakeNonNull();
		assertFailsWithSubstring(() -> evalIfNotNull(defaultValue, nullFunc), "java.lang.NullPointerException");
		assertFailsWithSubstring(() -> evalIfNotNull(defaultValue, nullFunc, ifNull -> "123"), "java.lang.NullPointerException");
		
		final Function<@Nullable Void, String> voidNullFunc = fakeNonNull();
		assertFailsWithSubstring(() -> evalIfNotNull(null, nonNull -> nonNull.toString(), voidNullFunc), "java.lang.NullPointerException");
	}
	
	/**
	 * Some tests for generic {@link NullUtil} functionality.
	 */
	@Test
	public void testNullUtil()
	{
		assertTrue(isNull(null));
		assertFalse(isNull("asd"));
		assertFalse(isNull(""));
		assertFalse(isNull(new Object()));
		
		{
			final @Nonnull String v = fakeNonNull();
			assertTrue(isNull(v));
		}
		{
			final @Nonnull String v = "asd";
			assertFalse(isNull(v));
		}
		{
			final @Nullable String v = null;
			assertTrue(isNull(v));
		}
		{
			final @Nullable String v = "asd";
			assertFalse(isNull(v));
		}
	}
}
