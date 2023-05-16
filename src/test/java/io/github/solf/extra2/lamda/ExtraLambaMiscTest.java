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
package io.github.solf.extra2.lamda;

import static io.github.solf.extra2.testutil.AssertExtra.assertFailsWithSubstring;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.IOException;

import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.testng.annotations.Test;

import io.github.solf.extra2.lambda.ObjectWrapper;
import io.github.solf.extra2.lambda.ValueOrProblem;

/**
 * Misc tests for lambda package.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class ExtraLambaMiscTest
{
	/**
	 * Tests {@link ValueOrProblem}
	 * @throws InterruptedException 
	 */
	@Test
	public void testValueOrProblem() throws InterruptedException
	{
		{
			ValueOrProblem<Integer, String> testValue = ValueOrProblem.ofValue(123);
			
			assertTrue(testValue.hasValue());
			assertFalse(testValue.hasProblem());
			assertEquals((int)testValue.getValue(), 123);
			assertFailsWithSubstring(() -> testValue.getProblem(), "java.util.NoSuchElementException: No problem present");
			testValue.toString(); // test @ToString doesn't fail
			assertEquals(testValue.getValueOrNull(), (Object)123);
			assertNull(testValue.getProblemOrNull());
			
			{
				ObjectWrapper<Boolean> invocationTracker = ObjectWrapper.of(false);
				testValue.ifValue(v -> {
					invocationTracker.set(true);
					assertEquals(v, (Object)123);
				});
				assertTrue(invocationTracker.get());
			}
			{
				ObjectWrapper<Boolean> invocationTracker = ObjectWrapper.of(false);
				testValue.ifValueInterruptibly(v -> {
					invocationTracker.set(true);
					assertEquals(v, (Object)123);
				});
				assertTrue(invocationTracker.get());
			}
			{
				ObjectWrapper<Boolean> invocationTracker = ObjectWrapper.of(false);
				testValue.ifNoValue(() -> {
					invocationTracker.set(true);
					fail("should not be invoked");
				});
				assertFalse(invocationTracker.get());
			}
			{
				ObjectWrapper<Boolean> invocationTracker = ObjectWrapper.of(false);
				testValue.ifNoValueInterruptibly(() -> {
					invocationTracker.set(true);
					fail("should not be invoked");
				});
				assertFalse(invocationTracker.get());
			}
			{
				ObjectWrapper<Boolean> invocationTracker = ObjectWrapper.of(false);
				testValue.ifProblem(p -> {
					invocationTracker.set(true);
					fail("should not be invoked");
				});
				assertFalse(invocationTracker.get());
			}
			{
				ObjectWrapper<Boolean> invocationTracker = ObjectWrapper.of(false);
				testValue.ifProblemInterruptibly(p -> {
					invocationTracker.set(true);
					fail("should not be invoked");
				});
				assertFalse(invocationTracker.get());
			}
			{
				ObjectWrapper<Boolean> ifTracker = ObjectWrapper.of(false);
				ObjectWrapper<Boolean> elseTracker = ObjectWrapper.of(false);
				assertEquals(testValue.ifValue(v -> {
						ifTracker.set(true);
						return v + 1;
					}).orElse(e -> {
						elseTracker.set(true);
						return -1;
					}),
				(Object)124);
				assertTrue(ifTracker.get());
				assertFalse(elseTracker.get());
			}
			{
				ObjectWrapper<Boolean> ifTracker = ObjectWrapper.of(false);
				ObjectWrapper<Boolean> elseTracker = ObjectWrapper.of(false);
				assertFailsWithSubstring( () -> {
					testValue.ifValue(v -> {
						ifTracker.set(true);
						if (System.currentTimeMillis() > 0) throw new RuntimeException("fail1");
						return null;
					}).orElse(e -> {
						elseTracker.set(true);
						throw new RuntimeException("fail2");
					});},
				"fail1");
				assertTrue(ifTracker.get());
				assertFalse(elseTracker.get());
			}
			{
				ObjectWrapper<Boolean> ifTracker = ObjectWrapper.of(false);
				ObjectWrapper<Boolean> elseTracker = ObjectWrapper.of(false);
				assertFailsWithSubstring( () -> {
					testValue.ifValueInterruptibly(v -> {
						ifTracker.set(true);
						if (System.currentTimeMillis() > 0) throw new InterruptedException("fail1");
						return null;
					}).orElse(e -> {
						elseTracker.set(true);
						throw new InterruptedException("fail2");
					});},
				"fail1");
				assertTrue(ifTracker.get());
				assertFalse(elseTracker.get());
			}
			{
				ObjectWrapper<Boolean> ifTracker = ObjectWrapper.of(false);
				ObjectWrapper<Boolean> elseTracker = ObjectWrapper.of(false);
				assertFailsWithSubstring( () -> {
					testValue.ifValueWithExceptionType(v -> {
						ifTracker.set(true);
						if (System.currentTimeMillis() > 0) throw new Exception("fail1");
						return null;
					}).orElse(e -> {
						elseTracker.set(true);
						throw new Exception("fail2");
					});},
				"fail1");
				assertTrue(ifTracker.get());
				assertFalse(elseTracker.get());
			}
		}
		
		{
			ValueOrProblem<Integer, String> testValue = ValueOrProblem.ofProblem("problem");
			
			assertFalse(testValue.hasValue());
			assertTrue(testValue.hasProblem());
			assertFailsWithSubstring(() -> testValue.getValue(), "java.util.NoSuchElementException: No value present");
			assertEquals(testValue.getProblem(), "problem");
			testValue.toString(); // test @ToString doesn't fail
			assertNull(testValue.getValueOrNull());
			assertEquals(testValue.getProblemOrNull(), "problem");
			
			{
				ObjectWrapper<Boolean> invocationTracker = ObjectWrapper.of(false);
				testValue.ifValue(v -> {
					invocationTracker.set(true);
					fail("should not be invoked");
				});
				assertFalse(invocationTracker.get());
			}
			{
				ObjectWrapper<Boolean> invocationTracker = ObjectWrapper.of(false);
				testValue.ifValueInterruptibly(v -> {
					invocationTracker.set(true);
					fail("should not be invoked");
				});
				assertFalse(invocationTracker.get());
			}
			{
				ObjectWrapper<Boolean> invocationTracker = ObjectWrapper.of(false);
				testValue.ifNoValue(() -> {
					invocationTracker.set(true);
				});
				assertTrue(invocationTracker.get());
			}
			{
				ObjectWrapper<Boolean> invocationTracker = ObjectWrapper.of(false);
				testValue.ifNoValueInterruptibly(() -> {
					invocationTracker.set(true);
				});
				assertTrue(invocationTracker.get());
			}
			{
				ObjectWrapper<Boolean> invocationTracker = ObjectWrapper.of(false);
				testValue.ifProblem(p -> {
					invocationTracker.set(true);
					assertEquals(p, "problem");
				});
				assertTrue(invocationTracker.get());
			}
			{
				ObjectWrapper<Boolean> invocationTracker = ObjectWrapper.of(false);
				testValue.ifProblemInterruptibly(p -> {
					invocationTracker.set(true);
					assertEquals(p, "problem");
				});
				assertTrue(invocationTracker.get());
			}
			{
				ObjectWrapper<Boolean> ifTracker = ObjectWrapper.of(false);
				ObjectWrapper<Boolean> elseTracker = ObjectWrapper.of(false);
				assertEquals(testValue.ifValue(v -> {
						ifTracker.set(true);
						return "" + (v + 1);
					}).orElse(e -> {
						elseTracker.set(true);
						return "r:" + e;
					}),
				"r:problem");
				assertFalse(ifTracker.get());
				assertTrue(elseTracker.get());
			}
			{
				ObjectWrapper<Boolean> ifTracker = ObjectWrapper.of(false);
				ObjectWrapper<Boolean> elseTracker = ObjectWrapper.of(false);
				assertFailsWithSubstring( () -> {
					testValue.ifValue(v -> {
						ifTracker.set(true);
						if (System.currentTimeMillis() > 0) throw new RuntimeException("fail1");
						return null;
					}).orElse(e -> {
						elseTracker.set(true);
						throw new RuntimeException("fail2");
					});},
				"fail2");
				assertFalse(ifTracker.get());
				assertTrue(elseTracker.get());
			}
			{
				ObjectWrapper<Boolean> ifTracker = ObjectWrapper.of(false);
				ObjectWrapper<Boolean> elseTracker = ObjectWrapper.of(false);
				assertFailsWithSubstring( () -> {
					testValue.ifValueInterruptibly(v -> {
						ifTracker.set(true);
						if (System.currentTimeMillis() > 0) throw new InterruptedException("fail1");
						return null;
					}).orElse(e -> {
						elseTracker.set(true);
						throw new InterruptedException("fail2");
					});},
				"fail2");
				assertFalse(ifTracker.get());
				assertTrue(elseTracker.get());
			}
			{
				ObjectWrapper<Boolean> ifTracker = ObjectWrapper.of(false);
				ObjectWrapper<Boolean> elseTracker = ObjectWrapper.of(false);
				assertFailsWithSubstring( () -> {
					testValue.ifValueWithExceptionType(v -> {
						ifTracker.set(true);
						if (System.currentTimeMillis() > 0) throw new Exception("fail1");
						return null;
					}).orElse(e -> {
						elseTracker.set(true);
						throw new Exception("fail2");
					});},
				"fail2");
				assertFalse(ifTracker.get());
				assertTrue(elseTracker.get());
			}
		}
		
		{
			ValueOrProblem<@Nullable Integer, String> testValue = ValueOrProblem.ofValue(null);
			
			assertTrue(testValue.hasValue());
			assertFalse(testValue.hasProblem());
			assertNull(testValue.getValue());
			assertFailsWithSubstring(() -> testValue.getProblem(), "java.util.NoSuchElementException: No problem present");
			testValue.toString(); // test @ToString doesn't fail
			assertNull(testValue.getValueOrNull());
			assertNull(testValue.getProblemOrNull());
		}
		
		{
			ValueOrProblem<Integer, @Nullable String> testValue = ValueOrProblem.ofProblem(null);
			
			assertFalse(testValue.hasValue());
			assertTrue(testValue.hasProblem());
			assertFailsWithSubstring(() -> testValue.getValue(), "java.util.NoSuchElementException: No value present");
			assertNull(testValue.getProblem());
			testValue.toString(); // test @ToString doesn't fail
			assertNull(testValue.getValueOrNull());
			assertNull(testValue.getProblemOrNull());
		}
		
		{
			ValueOrProblem<Integer, IOException> testValue = ValueOrProblem.ofValue(123);
			try
			{
				assertEquals((int)ValueOrProblem.getValueOrThrowProblem(testValue), 123);
			} catch (IOException e)
			{
				fail("" + e);
			}
		}
		
		{
			ValueOrProblem<Integer, IOException> testValue = ValueOrProblem.ofProblem(new IOException("SOLF"));
			assertFailsWithSubstring(() -> {ValueOrProblem.getValueOrThrowProblem(testValue);}, "java.io.IOException: SOLF");
		}
	}
}
