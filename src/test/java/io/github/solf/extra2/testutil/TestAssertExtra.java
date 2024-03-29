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

import static io.github.solf.extra2.testutil.AssertExtra.assertAnyOk;
import static io.github.solf.extra2.testutil.AssertExtra.assertAnyOkWithEval;
import static io.github.solf.extra2.testutil.AssertExtra.assertAnyOkWithValue;
import static io.github.solf.extra2.testutil.AssertExtra.assertAnyTrue;
import static io.github.solf.extra2.testutil.AssertExtra.assertAnyTrueWithEval;
import static io.github.solf.extra2.testutil.AssertExtra.assertAnyTrueWithValue;
import static io.github.solf.extra2.testutil.AssertExtra.assertAnyTrueWithValueAndMessage;
import static io.github.solf.extra2.testutil.AssertExtra.assertBetweenExclusive;
import static io.github.solf.extra2.testutil.AssertExtra.assertBetweenInclusive;
import static io.github.solf.extra2.testutil.AssertExtra.assertContains;
import static io.github.solf.extra2.testutil.AssertExtra.assertContainsIgnoreCase;
import static io.github.solf.extra2.testutil.AssertExtra.assertContainsWithFlag;
import static io.github.solf.extra2.testutil.AssertExtra.assertContainsWithFlagIgnoreCase;
import static io.github.solf.extra2.testutil.AssertExtra.assertFails;
import static io.github.solf.extra2.testutil.AssertExtra.assertFailsWithSubstring;
import static io.github.solf.extra2.testutil.AssertExtra.assertFailsWithSubstringIgnoreCase;
import static io.github.solf.extra2.testutil.AssertExtra.assertGreater;
import static io.github.solf.extra2.testutil.AssertExtra.assertGreaterOrEqual;
import static io.github.solf.extra2.testutil.AssertExtra.assertLess;
import static io.github.solf.extra2.testutil.AssertExtra.assertLessOrEqual;
import static io.github.solf.extra2.testutil.AssertExtra.assertNotBetweenExclusive;
import static io.github.solf.extra2.testutil.AssertExtra.assertNotBetweenInclusive;
import static io.github.solf.extra2.testutil.AssertExtra.assertNotContains;
import static io.github.solf.extra2.testutil.AssertExtra.assertNotContainsIgnoreCase;
import static io.github.solf.extra2.testutil.AssertExtra.assertPasses;
import static io.github.solf.extra2.util.NullUtil.fakeNonNull;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.testng.annotations.Test;

import io.github.solf.extra2.concurrent.RunnableWithException;

/**
 * Tests for {@link AssertExtra}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class TestAssertExtra
{
	/**
	 * Tests.
	 */
	@Test
	public void test()
	{
		final @Nonnull Integer nullInt = fakeNonNull();		
		assertGreater(1, 0);
		assertGreater(4, 3, "msg");
		assertGreaterOrEqual(2, 1);
		assertGreaterOrEqual(2, 2);
		assertGreaterOrEqual(6, 5, "msg2");
		assertGreaterOrEqual(6, 6, "msg2");
		
		assertLess(0, 1);
		assertLess(2, 3, "msg3");
		assertLessOrEqual(4, 5);
		assertLessOrEqual(5, 5);
		assertLessOrEqual(6, 7, "msg4");
		assertLessOrEqual(7, 7, "msg4");

		mustFailWithException(() -> assertGreater(nullInt, 0));
		mustFailWithException(() -> assertGreater(1, nullInt));
		mustFailWithException(() -> assertGreater(nullInt, 3, "msg"));
		mustFailWithException(() -> assertGreater(4, nullInt, "msg"));
		mustFailWithException(() -> assertGreaterOrEqual(nullInt, 1));
		mustFailWithException(() -> assertGreaterOrEqual(2, nullInt));
		mustFailWithException(() -> assertGreaterOrEqual(nullInt, 5, "msg2"));
		mustFailWithException(() -> assertGreaterOrEqual(6, nullInt, "msg2"));
		
		mustFailWithException(() -> assertLess(nullInt, 1));
		mustFailWithException(() -> assertLess(0, nullInt));
		mustFailWithException(() -> assertLess(nullInt, 3, "msg3"));
		mustFailWithException(() -> assertLess(2, nullInt, "msg3"));
		mustFailWithException(() -> assertLessOrEqual(nullInt, 5));
		mustFailWithException(() -> assertLessOrEqual(5, nullInt));
		mustFailWithException(() -> assertLessOrEqual(nullInt, 7, "msg4"));
		mustFailWithException(() -> assertLessOrEqual(7, nullInt, "msg4"));
		
		assertFails(() -> {throw new Exception("asd");});
		assertFails(() -> {throw new Exception("asd");}, "msgF");
		assertFailsWithSubstring(() -> {throw new Exception("asd");}, "sd");
		assertFailsWithSubstring(() -> {throw new Exception("asd");}, "asd", "msgF");
		assertFailsWithSubstringIgnoreCase(() -> {throw new Exception("asd");}, "sd");
		assertFailsWithSubstringIgnoreCase(() -> {throw new Exception("asd");}, "SD", "msgF");
		mustFailWithException(() -> assertFailsWithSubstring(() -> {throw new Exception("asd");}, "aSd"));
		mustFailWithException(() -> assertFailsWithSubstring(() -> {throw new Exception("asd");}, "qasd", "msgF"));
		mustFailWithException(() -> assertFailsWithSubstringIgnoreCase(() -> {throw new Exception("asd");}, "substring"));
		mustFailWithException(() -> assertFailsWithSubstringIgnoreCase(() -> {throw new Exception("asd");}, "substring", "msgF"));
		
		assertEquals((int)assertPasses(() -> 1), 1);
		assertEquals((int)assertPasses(() -> 2, "msgF"), 2);
		assertPasses(() -> {System.currentTimeMillis();});
		assertPasses(() -> {System.currentTimeMillis();}, "msgF");
		assertFailsWithSubstring(() -> assertPasses(() -> {if (System.currentTimeMillis() > 0) throw new Exception("qwe"); return 1;}), "code block failed: java.lang.Exception: qwe");
		assertFailsWithSubstring(() -> assertPasses(() -> {if (System.currentTimeMillis() > 0) throw new Exception("qwe"); return 1;}, "msgF"), "code block failed: : [msgF]: java.lang.Exception: qwe");
		assertFailsWithSubstring(() -> assertPasses(() -> {if (System.currentTimeMillis() > 0) throw new Exception("zxc");}), "code block failed: java.lang.Exception: zxc");
		assertFailsWithSubstring(() -> assertPasses(() -> {if (System.currentTimeMillis() > 0) throw new Exception("zxc");}, "msgF"), "code block failed: : [msgF]: java.lang.Exception: zxc");
		assertFailsWithSubstring(() -> assertPasses((RunnableWithException)fakeNonNull()), "Parameter code block must not be null");
		assertFailsWithSubstring(() -> assertPasses((Callable<?>)fakeNonNull()), "Parameter code block must not be null");
		
		mustFailWithException(() -> assertGreater(0, 0));
		mustFailWithException(() -> assertGreater(0, 0, "msg"));
		mustFailWithException(() -> assertGreater(0, 1));
		mustFailWithException(() -> assertGreater(0, 1, "msg"));
		mustFailWithException(() -> assertGreaterOrEqual(1, 2));
		mustFailWithException(() -> assertGreaterOrEqual(5, 6, "msg2"));
		
		mustFailWithException(() -> assertLess(1, 0));
		mustFailWithException(() -> assertLess(1, 0, "msg3"));
		mustFailWithException(() -> assertLess(0, 0));
		mustFailWithException(() -> assertLess(0, 0, "msg3"));
		mustFailWithException(() -> assertLessOrEqual(5, 4));
		mustFailWithException(() -> assertLessOrEqual(5, 4, "msg4"));
		
		mustFailWithException(() -> assertFails(() -> {/*nothing*/}));
		mustFailWithException(() -> assertFails(() -> {/*nothing*/}, "msgF"));
		mustFailWithException(() -> assertFailsWithSubstring(() -> {/*nothing*/}, "a"));
		mustFailWithException(() -> assertFailsWithSubstring(() -> {/*nothing*/}, "b", "msgF"));
		mustFailWithException(() -> assertFailsWithSubstringIgnoreCase(() -> {/*nothing*/}, "c"));
		mustFailWithException(() -> assertFailsWithSubstringIgnoreCase(() -> {/*nothing*/}, "d", "msgF"));
		
		
		
		mustFailWithException(() -> assertContains(null, null));
		mustFailWithException(() -> assertContains(null, null, "msgC"));
		mustFailWithException(() -> assertContainsIgnoreCase(null, null));
		mustFailWithException(() -> assertContainsIgnoreCase(null, null, "msgC"));
		mustFailWithException(() -> assertContainsWithFlag(true, null, null, "msgC"));
		mustFailWithException(() -> assertContainsWithFlag(false, null, null, "msgC"));
		mustFailWithException(() -> assertContainsWithFlagIgnoreCase(true, null, null, "msgC"));
		mustFailWithException(() -> assertContainsWithFlagIgnoreCase(false, null, null, "msgC"));
		mustFailWithException(() -> assertContains(null, "a"));
		mustFailWithException(() -> assertContains(null, "a", "msgC"));
		mustFailWithException(() -> assertContainsIgnoreCase(null, "a"));
		mustFailWithException(() -> assertContainsIgnoreCase(null, "a", "msgC"));
		mustFailWithException(() -> assertContainsWithFlag(true, null, "a", "msgC"));
		mustFailWithException(() -> assertContainsWithFlag(false, null, "a", "msgC"));
		mustFailWithException(() -> assertContainsWithFlagIgnoreCase(true, null, "a", "msgC"));
		mustFailWithException(() -> assertContainsWithFlagIgnoreCase(false, null, "a", "msgC"));
		mustFailWithException(() -> assertContains("b", null));
		mustFailWithException(() -> assertContains("b", null, "msgC"));
		mustFailWithException(() -> assertContainsIgnoreCase("b", null));
		mustFailWithException(() -> assertContainsIgnoreCase("b", null, "msgC"));
		mustFailWithException(() -> assertContainsWithFlag(true, "b", null, "msgC"));
		mustFailWithException(() -> assertContainsWithFlag(false, "b", null, "msgC"));
		mustFailWithException(() -> assertContainsWithFlagIgnoreCase(true, "b", null, "msgC"));
		mustFailWithException(() -> assertContainsWithFlagIgnoreCase(false, "b", null, "msgC"));
		
		assertContains("haystack", "hay");
		assertContains("haystack", "hay", "msgH");
		assertContainsWithFlag(true, "haystack", "hay", "msgH");
		assertContains("haystack", "stack");
		assertContains("haystack", "stack", "msgH");
		assertContainsWithFlag(true, "haystack", "stack", "msgH");
		assertContainsWithFlag(true, new Object(), "Object", "msgH");
		assertContainsWithFlag(false, new Object(), "needle", "msgH");
		mustFailWithException(() -> assertContains("haystack", "Hay"));
		mustFailWithException(() -> assertContains("haystack", "Hay", "msgH"));
		mustFailWithException(() -> assertContainsWithFlag(true, "haystack", "Hay", "msgH"));
		mustFailWithException(() -> assertContains("haystack", "needle"));
		mustFailWithException(() -> assertContains("haystack", "needle", "msgH"));
		mustFailWithException(() -> assertContainsWithFlag(true, "haystack", "needle", "msgH"));
		mustFailWithException(() -> assertContainsWithFlag(true, new Object(), "needle", "msgH"));
		mustFailWithException(() -> assertContainsWithFlag(false, new Object(), "Object", "msgH"));
		
		
		mustFailWithException(() -> assertContainsIgnoreCase(null, null));
		mustFailWithException(() -> assertContainsIgnoreCase(null, null, "msgC"));
		mustFailWithException(() -> assertContainsWithFlagIgnoreCase(true, null, null, "msgC"));
		mustFailWithException(() -> assertContainsIgnoreCase(null, "a"));
		mustFailWithException(() -> assertContainsIgnoreCase(null, "a", "msgC"));
		mustFailWithException(() -> assertContainsWithFlagIgnoreCase(true, null, "a", "msgC"));
		mustFailWithException(() -> assertContainsIgnoreCase("b", null));
		mustFailWithException(() -> assertContainsIgnoreCase("b", null, "msgC"));
		mustFailWithException(() -> assertContainsWithFlagIgnoreCase(true, "b", null, "msgC"));
		
		assertContainsIgnoreCase("haystack", "hay");
		assertContainsIgnoreCase("haystack", "hay", "msgH");
		assertContainsWithFlagIgnoreCase(true, "haystack", "hay", "msgH");
		assertContainsIgnoreCase("haystack", "stack");
		assertContainsIgnoreCase("haystack", "stack", "msgH");
		assertContainsWithFlagIgnoreCase(true, "haystack", "stack", "msgH");
		assertContainsIgnoreCase("haystack", "Hay");
		assertContainsIgnoreCase("haystack", "Hay", "msgH");
		assertContainsWithFlagIgnoreCase(true, "haystack", "Hay", "msgH");
		assertContainsWithFlagIgnoreCase(true, new Object(), "Object", "msgH");
		assertContainsWithFlagIgnoreCase(false, new Object(), "needle", "msgH");
		mustFailWithException(() -> assertContainsIgnoreCase("haystack", "needle"));
		mustFailWithException(() -> assertContainsIgnoreCase("haystack", "needle", "msgH"));
		mustFailWithException(() -> assertContainsWithFlagIgnoreCase(true, "haystack", "needle", "msgH"));
		mustFailWithException(() -> assertContainsWithFlagIgnoreCase(true, new Object(), "needle", "msgH"));
		mustFailWithException(() -> assertContainsWithFlagIgnoreCase(false, new Object(), "Object", "msgH"));
		
		
		
		
		mustFailWithException(() -> assertNotContains("haystack", "hay"));
		mustFailWithException(() -> assertNotContains("haystack", "hay", "msgH"));
		mustFailWithException(() -> assertContainsWithFlag(false, "haystack", "hay", "msgH"));
		mustFailWithException(() -> assertNotContains("haystack", "stack"));
		mustFailWithException(() -> assertNotContains("haystack", "stack", "msgH"));
		mustFailWithException(() -> assertNotContains(new Object(), "Object", "msgH"));
		mustFailWithException(() -> assertContainsWithFlag(false, "haystack", "stack", "msgH"));
		assertNotContains("haystack", "Hay");
		assertNotContains("haystack", "Hay", "msgH");
		assertContainsWithFlag(false, "haystack", "Hay", "msgH");
		assertNotContains("haystack", "needle");
		assertNotContains("haystack", "needle", "msgH");
		assertNotContains(new Object(), "needle", "msgH");
		assertContainsWithFlag(false, "haystack", "needle", "msgH");
		
		
		mustFailWithException(() -> assertNotContainsIgnoreCase(null, null));
		mustFailWithException(() -> assertNotContainsIgnoreCase(null, null, "msgC"));
		mustFailWithException(() -> assertContainsWithFlagIgnoreCase(false, null, null, "msgC"));
		mustFailWithException(() -> assertNotContainsIgnoreCase(null, "a"));
		mustFailWithException(() -> assertNotContainsIgnoreCase(null, "a", "msgC"));
		mustFailWithException(() -> assertContainsWithFlagIgnoreCase(false, null, "a", "msgC"));
		mustFailWithException(() -> assertNotContainsIgnoreCase("b", null));
		mustFailWithException(() -> assertNotContainsIgnoreCase("b", null, "msgC"));
		mustFailWithException(() -> assertContainsWithFlagIgnoreCase(false, "b", null, "msgC"));
		
		mustFailWithException(() -> assertNotContainsIgnoreCase("haystack", "hay"));
		mustFailWithException(() -> assertNotContainsIgnoreCase("haystack", "hay", "msgH"));
		mustFailWithException(() -> assertContainsWithFlagIgnoreCase(false, "haystack", "hay", "msgH"));
		mustFailWithException(() -> assertNotContainsIgnoreCase("haystack", "stack"));
		mustFailWithException(() -> assertNotContainsIgnoreCase("haystack", "stack", "msgH"));
		mustFailWithException(() -> assertContainsWithFlagIgnoreCase(false, "haystack", "stack", "msgH"));
		mustFailWithException(() -> assertNotContainsIgnoreCase("haystack", "Hay"));
		mustFailWithException(() -> assertNotContainsIgnoreCase("haystack", "Hay", "msgH"));
		mustFailWithException(() -> assertNotContainsIgnoreCase(new Object(), "Object", "msgH"));
		mustFailWithException(() -> assertContainsWithFlagIgnoreCase(false, "haystack", "Hay", "msgH"));
		assertNotContainsIgnoreCase("haystack", "needle");
		assertNotContainsIgnoreCase("haystack", "needle", "msgH");
		assertNotContainsIgnoreCase(new Object(), "needle", "msgH");
		assertContainsWithFlagIgnoreCase(false, "haystack", "needle", "msgH");
		
		
		assertBetweenExclusive(5, 4, 6);
		assertBetweenExclusive(-5, -6, -4, "msgb");
		assertBetweenInclusive(5, 5, 6);
		assertBetweenInclusive(-5, -6, -5, "msgbi");
		
		assertNotBetweenExclusive(6, 4, 6);
		assertNotBetweenExclusive(4, 4, 6);
		assertNotBetweenExclusive(-6, -6, -4, "msgb");
		assertNotBetweenExclusive(-4, -6, -4, "msgb");
		assertNotBetweenInclusive(4, 5, 6);
		assertNotBetweenInclusive(7, 5, 6);
		assertNotBetweenInclusive(-7, -6, -5, "msgbi");
		assertNotBetweenInclusive(-4, -6, -5, "msgbi");

	
		mustFailWithException(() -> assertBetweenExclusive(4, 4, 6));
		mustFailWithException(() -> assertBetweenExclusive(-4, -6, -4, "msgb"));
		mustFailWithException(() -> assertBetweenInclusive(4, 5, 6));
		mustFailWithException(() -> assertBetweenInclusive(-4, -6, -5, "msgbi"));
		
		mustFailWithException(() -> assertNotBetweenExclusive(5, 4, 6));
		mustFailWithException(() -> assertNotBetweenExclusive(-5, -6, -4, "msgb"));
		mustFailWithException(() -> assertNotBetweenInclusive(5, 5, 6));
		mustFailWithException(() -> assertNotBetweenInclusive(6, 5, 6));
		mustFailWithException(() -> assertNotBetweenInclusive(-6, -6, -5, "msgbi"));
		mustFailWithException(() -> assertNotBetweenInclusive(-5, -6, -5, "msgbi"));
		
		
		mustFailWithException(() -> assertBetweenExclusive(nullInt, 4, 6));
		mustFailWithException(() -> assertBetweenExclusive(5, nullInt, 6));
		mustFailWithException(() -> assertBetweenExclusive(5, 4, nullInt));
		mustFailWithException(() -> assertBetweenExclusive(nullInt, -6, -4, "msgb"));
		mustFailWithException(() -> assertBetweenExclusive(-5, nullInt, -4, "msgb"));
		mustFailWithException(() -> assertBetweenExclusive(-5, -6, nullInt, "msgb"));
		mustFailWithException(() -> assertBetweenInclusive(nullInt, 5, 6));
		mustFailWithException(() -> assertBetweenInclusive(5, nullInt, 6));
		mustFailWithException(() -> assertBetweenInclusive(5, 5, nullInt));
		mustFailWithException(() -> assertBetweenInclusive(nullInt, -6, -5, "msgbi"));
		mustFailWithException(() -> assertBetweenInclusive(-5, nullInt, -5, "msgbi"));
		mustFailWithException(() -> assertBetweenInclusive(-5, -6, nullInt, "msgbi"));
	}
	
	/**
	 * Tests for {@link AssertExtra#assertAnyOk(io.github.solf.extra2.concurrent.RunnableWithException...)}
	 * and similar methods.
	 */
	@Test
	public void testAnyOk()
	{
		assertFailsWithSubstring(() -> assertAnyOk(), "Must provide at least one path");
		
		assertAnyOk(() -> {/**/});
		assertAnyOk(() -> {/**/}, () -> {/**/});
		assertAnyOk(() -> {/**/}, () -> {/**/}, () -> {/**/});

		assertFailsWithSubstring(() -> assertAnyOk(() -> fail("fail")), "All paths failed, last exception: java.lang.AssertionError: fail");
		assertAnyOk(() -> fail("fail"), () -> {/**/});
		assertAnyOk(() -> fail("fail"), () -> {/**/}, () -> {/**/});
		
		assertFailsWithSubstring(() -> assertAnyOk(() -> fail("fail"), () -> fail("fail")), "All paths failed, last exception: java.lang.AssertionError: fail");
		assertAnyOk(() -> fail("fail"), () -> fail("fail"), () -> {/**/});
		
		assertFailsWithSubstring(() -> assertAnyOk(() -> fail("fail"), () -> fail("fail"), () -> fail("fail")), "All paths failed, last exception: java.lang.AssertionError: fail");
		
		assertEquals(assertAnyOk(() -> {/**/}), 1);
		assertEquals(assertAnyOk(() -> fail("fail"), () -> {/**/}), 2);
		assertEquals(assertAnyOk(() -> fail("fail"), () -> fail("fail"), () -> {/**/}), 3);
		
		
		
		
		assertFailsWithSubstring(() -> assertAnyOkWithEval(() -> "asd"), "Must provide at least one path");
		assertFailsWithSubstring(() -> assertAnyOkWithEval(() -> {fail("eval"); return "qwe";}, v -> {/**/}), "Expression evaluation failed: java.lang.AssertionError: eval");

		assertAnyOkWithEval(() -> "asd", v -> {/**/});
		assertAnyOkWithEval(() -> "asd", v -> {/**/}, v -> {/**/});
		assertAnyOkWithEval(() -> "asd", v -> {/**/}, v -> {/**/}, v -> {/**/});

		assertFailsWithSubstring(() -> assertAnyOkWithEval(() -> "asd", v -> fail("fail")), "assertAnyOkWithEval failed for value [asd]: java.lang.IllegalStateException: All paths failed, last exception: java.lang.AssertionError: fail");
		assertAnyOkWithEval(() -> "asd", v -> fail("fail"), v -> {/**/});
		assertAnyOkWithEval(() -> "asd", v -> fail("fail"), v -> {/**/}, v -> {/**/});
		
		assertFailsWithSubstring(() -> assertAnyOkWithEval(() -> "asd", v -> fail("fail"), v -> fail("fail")), "assertAnyOkWithEval failed for value [asd]: java.lang.IllegalStateException: All paths failed, last exception: java.lang.AssertionError: fail");
		assertAnyOkWithEval(() -> "asd", v -> fail("fail"), v -> fail("fail"), v -> {/**/});
		
		assertFailsWithSubstring(() -> assertAnyOkWithEval(() -> "asd", v -> fail("fail"), v -> fail("fail"), v -> fail("fail")), "assertAnyOkWithEval failed for value [asd]: java.lang.IllegalStateException: All paths failed, last exception: java.lang.AssertionError: fail");
		
		assertEquals(assertAnyOkWithEval(() -> "asd", v -> {/**/}), 1);
		assertEquals(assertAnyOkWithEval(() -> "asd", v -> fail("fail"), v -> {/**/}), 2);
		assertEquals(assertAnyOkWithEval(() -> "asd", v -> fail("fail"), v -> fail("fail"), v -> {/**/}), 3);
		
		
		assertFailsWithSubstring(() -> assertAnyOkWithValue("asd"), "Must provide at least one path");

		assertAnyOkWithValue("asd", v -> {/**/});
		assertAnyOkWithValue("asd", v -> {/**/}, v -> {/**/});
		assertAnyOkWithValue("asd", v -> {/**/}, v -> {/**/}, v -> {/**/});

		assertFailsWithSubstring(() -> assertAnyOkWithValue("asd", v -> fail("fail")), "assertAnyOkWithEval failed for value [asd]: java.lang.IllegalStateException: All paths failed, last exception: java.lang.AssertionError: fail");
		assertAnyOkWithValue("asd", v -> fail("fail"), v -> {/**/});
		assertAnyOkWithValue("asd", v -> fail("fail"), v -> {/**/}, v -> {/**/});
		
		assertFailsWithSubstring(() -> assertAnyOkWithValue("asd", v -> fail("fail"), v -> fail("fail")), "assertAnyOkWithEval failed for value [asd]: java.lang.IllegalStateException: All paths failed, last exception: java.lang.AssertionError: fail");
		assertAnyOkWithValue("asd", v -> fail("fail"), v -> fail("fail"), v -> {/**/});
		
		assertFailsWithSubstring(() -> assertAnyOkWithValue("asd", v -> fail("fail"), v -> fail("fail"), v -> fail("fail")), "assertAnyOkWithEval failed for value [asd]: java.lang.IllegalStateException: All paths failed, last exception: java.lang.AssertionError: fail");
		
		assertEquals(assertAnyOkWithValue("asd", v -> {/**/}), 1);
		assertEquals(assertAnyOkWithValue("asd", v -> fail("fail"), v -> {/**/}), 2);
		assertEquals(assertAnyOkWithValue("asd", v -> fail("fail"), v -> fail("fail"), v -> {/**/}), 3);
		
		
		
		assertFailsWithSubstring(() -> assertAnyTrue(), "Must provide at least one path");
		assertFailsWithSubstring(() -> assertAnyTrue(() -> {fail("fail"); return true;}), "All paths failed, last exception: java.lang.AssertionError: fail");
		assertFailsWithSubstring(() -> assertAnyTrue(() -> false, () -> {fail("fail"); return true;}), "All paths failed, last exception: java.lang.AssertionError: fail");
		assertFailsWithSubstring(() -> assertAnyTrue("AMSG", () -> {fail("fail"); return true;}), "AMSG: java.lang.IllegalStateException: All paths failed, last exception: java.lang.AssertionError: fail");
		
		assertAnyTrue(() -> true);
		assertAnyTrue(() -> true, () -> true);
		assertAnyTrue(() -> true, () -> true, () -> true);

		assertFailsWithSubstring(() -> assertAnyTrue(() -> false), "All paths failed, last exception: java.lang.IllegalStateException: got false");
		assertAnyTrue(() -> false, () -> true);
		assertAnyTrue(() -> false, () -> true, () -> true);
		
		assertFailsWithSubstring(() -> assertAnyTrue(() -> false, () -> false), "All paths failed, last exception: java.lang.IllegalStateException: got false");
		assertAnyTrue(() -> false, () -> false, () -> true);
		
		assertFailsWithSubstring(() -> assertAnyTrue(() -> false, () -> false, () -> false), "All paths failed, last exception: java.lang.IllegalStateException: got false");
		
		assertEquals(assertAnyTrue(() -> true), 1);
		assertEquals(assertAnyTrue(() -> false, () -> true), 2);
		assertEquals(assertAnyTrue(() -> false, () -> fakeNonNull(), () -> true), 3);
		
		
		
		assertFailsWithSubstring(() -> assertAnyTrueWithEval(() -> "asd"), "Must provide at least one path");
		assertFailsWithSubstring(() -> assertAnyTrueWithEval(() -> {fail("eval"); return "qwe";}, v -> true), "Expression evaluation failed: java.lang.AssertionError: eval");
		
		assertFailsWithSubstring(() -> assertAnyTrueWithEval(() -> "asd", v -> {fail("fail"); return true;}), "All paths failed, last exception: java.lang.AssertionError: fail");
		assertFailsWithSubstring(() -> assertAnyTrueWithEval(() -> "asd", v -> false, v -> {fail("fail"); return true;}), "All paths failed, last exception: java.lang.AssertionError: fail");
		assertFailsWithSubstring(() -> assertAnyTrueWithEval("AMSG", () -> "asd", v -> {fail("fail"); return true;}), "AMSG: java.lang.IllegalStateException: assertAnyTrueWithEval failed for value [asd]: java.lang.IllegalStateException: All paths failed, last exception: java.lang.AssertionError: fail");
		
		assertAnyTrueWithEval(() -> "asd", v -> true);
		assertAnyTrueWithEval(() -> "asd", v -> true, v -> true);
		assertAnyTrueWithEval(() -> "asd", v -> true, v -> true, v -> true);

		assertFailsWithSubstring(() -> assertAnyTrueWithEval(() -> "asd", v -> false), "All paths failed, last exception: java.lang.IllegalStateException: got false");
		assertAnyTrueWithEval(() -> "asd", v -> false, v -> true);
		assertAnyTrueWithEval(() -> "asd", v -> false, v -> true, v -> true);
		
		assertFailsWithSubstring(() -> assertAnyTrueWithEval(() -> "asd", v -> false, v -> false), "All paths failed, last exception: java.lang.IllegalStateException: got false");
		assertAnyTrueWithEval(() -> "asd", v -> false, v -> false, v -> true);
		
		assertFailsWithSubstring(() -> assertAnyTrueWithEval(() -> "asd", v -> false, v -> false, v -> false), "All paths failed, last exception: java.lang.IllegalStateException: got false");
		
		assertEquals(assertAnyTrueWithEval(() -> "asd", v -> true), 1);
		assertEquals(assertAnyTrueWithEval(() -> "asd", v -> false, v -> true), 2);
		assertEquals(assertAnyTrueWithEval(() -> "asd", v -> false, v -> fakeNonNull(), v -> true), 3);
		
		
		assertFailsWithSubstring(() -> assertAnyTrueWithValue("asd"), "Must provide at least one path");
		
		assertFailsWithSubstring(() -> assertAnyTrueWithValue("asd", v -> {fail("fail"); return true;}), "All paths failed, last exception: java.lang.AssertionError: fail");
		assertFailsWithSubstring(() -> assertAnyTrueWithValue("asd", v -> false, v -> {fail("fail"); return true;}), "All paths failed, last exception: java.lang.AssertionError: fail");
		assertFailsWithSubstring(() -> assertAnyTrueWithValueAndMessage("AMSG", "asd", v -> {fail("fail"); return true;}), "AMSG: java.lang.IllegalStateException: assertAnyTrueWithEval failed for value [asd]: java.lang.IllegalStateException: All paths failed, last exception: java.lang.AssertionError: fail");
		
		assertAnyTrueWithValue("asd", v -> true);
		assertAnyTrueWithValue("asd", v -> true, v -> true);
		assertAnyTrueWithValue("asd", v -> true, v -> true, v -> true);

		assertFailsWithSubstring(() -> assertAnyTrueWithValue("asd", v -> false), "All paths failed, last exception: java.lang.IllegalStateException: got false");
		assertAnyTrueWithValue("asd", v -> false, v -> true);
		assertAnyTrueWithValue("asd", v -> false, v -> true, v -> true);
		
		assertFailsWithSubstring(() -> assertAnyTrueWithValue("asd", v -> false, v -> false), "All paths failed, last exception: java.lang.IllegalStateException: got false");
		assertAnyTrueWithValue("asd", v -> false, v -> false, v -> true);
		
		assertFailsWithSubstring(() -> assertAnyTrueWithValue("asd", v -> false, v -> false, v -> false), "All paths failed, last exception: java.lang.IllegalStateException: got false");
		
		assertEquals(assertAnyTrueWithValue("asd", v -> true), 1);
		assertEquals(assertAnyTrueWithValue("asd", v -> false, v -> true), 2);
		assertEquals(assertAnyTrueWithValue("asd", v -> false, v -> fakeNonNull(), v -> true), 3);
	}
	
	/**
	 * Verifies that whatever runnable is given fails with an exception
	 */
	private void mustFailWithException(Runnable r)
	{
		try
		{
			r.run();
		} catch (Exception e)
		{
			// expected
			return;
		}
		fail("must not be reached");
	}
}
