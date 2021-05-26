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

import static io.github.solf.extra2.lambda.LambdaUtil.and;
import static io.github.solf.extra2.lambda.LambdaUtil.equalsAll;
import static io.github.solf.extra2.lambda.LambdaUtil.equalsAllIn;
import static io.github.solf.extra2.lambda.LambdaUtil.equalsAny;
import static io.github.solf.extra2.lambda.LambdaUtil.equalsAnyIn;
import static io.github.solf.extra2.lambda.LambdaUtil.not;
import static io.github.solf.extra2.lambda.LambdaUtil.or;
import static io.github.solf.extra2.lambda.LambdaUtil.p;
import static io.github.solf.extra2.lambda.LambdaUtil.trueForAll;
import static io.github.solf.extra2.lambda.LambdaUtil.trueForAllIn;
import static io.github.solf.extra2.lambda.LambdaUtil.trueForAny;
import static io.github.solf.extra2.lambda.LambdaUtil.trueForAnyIn;
import static io.github.solf.extra2.lambda.LambdaUtil.valueEqualsAll;
import static io.github.solf.extra2.lambda.LambdaUtil.valueEqualsAllIn;
import static io.github.solf.extra2.lambda.LambdaUtil.valueEqualsAny;
import static io.github.solf.extra2.lambda.LambdaUtil.valueEqualsAnyIn;
import static io.github.solf.extra2.lambda.LambdaUtil.valueTrueForAll;
import static io.github.solf.extra2.lambda.LambdaUtil.valueTrueForAllIn;
import static io.github.solf.extra2.lambda.LambdaUtil.valueTrueForAny;
import static io.github.solf.extra2.lambda.LambdaUtil.valueTrueForAnyIn;
import static io.github.solf.extra2.util.NullUtil.nn;
import static io.github.solf.extra2.util.NullUtil.nncn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.NonNullByDefault;

import org.testng.annotations.Test;

import io.github.solf.extra2.lambda.LambdaUtil;
import io.github.solf.extra2.options.OptionConstraint;



/**
 * Tests for {@link LambdaUtil}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class ExtraLambaUtilTest
{
	/**
	 * Test data set.
	 */
	private static final List<String> cData = Collections.unmodifiableList(Arrays.asList("one", "two", "three", "four", "five"));

	/**
	 * Test data set with null.
	 */
	private static final List<@Nullable String> cDataWithNull = Collections.unmodifiableList(Arrays.asList("one", "two", "three", "four", "five", null));

	/**
	 * Test data set.
	 */
	private static final List<SillyNumber> sillyData = Collections.unmodifiableList(Arrays.asList(
		new SillyNumber(123), new SillyNumber(456), new SillyNumber(789)
		, new SillyNumber(0), new SillyNumber(1), new SillyNumber(2), new SillyNumber(3)
		, new SillyNumber(111), new SillyNumber(222), new SillyNumber(333)
		, new SillyNumber(112), new SillyNumber(134), new SillyNumber(156)
		, new SillyNumber(212), new SillyNumber(234), new SillyNumber(256)
		, new SillyNumber(55555), new SillyNumber(12345), new SillyNumber(222222)
		));

	/**
	 * Test data set with null.
	 */
	private static final List<@Nullable SillyNumber> sillyDataWithNull;
	static
	{
		ArrayList<@Nullable SillyNumber> tmp = new ArrayList<>(sillyData);
		tmp.add(null);
		sillyDataWithNull = Collections.unmodifiableList(tmp);
	}
	
	/**
	 * Silly class for testing 'equals all'.
	 * <p>
	 * It takes integer 0-999999 and its equals method accepts Strings that are
	 * equal to that number or any of its digits (as strings).
	 */
	private static class SillyNumber
	{
		/**
		 * Actual number as string.
		 */
		private final String name;
		
		/**
		 * Possible string matches.
		 */
		private final Set<String> matches;
		
		/**
		 * Silly length for this.
		 */
		private final SillyLength sillyLength;
		
		/**
		 * Silly length of the number, matches both string length & number of 
		 * items in #matches
		 */
		private class SillyLength
		{

			/* (non-Javadoc)
			 * @see java.lang.Object#equals(java.lang.Object)
			 */
			@Override
			public boolean equals(@Nullable Object obj)
			{
				if (this == obj)
					return true;
				
				if (obj instanceof Number)
				{
					int n = Integer.parseInt("" + obj);
					if (n == name.length())
						return true;
					
					return n == matches.size();
				}
				else
					return false;
			}

			/* (non-Javadoc)
			 * @see java.lang.Object#hashCode()
			 */
			@Override
			public int hashCode()
			{
				return name.hashCode();
			}
			
			
		}
		
		/**
		 * Constructor.
		 */
		public SillyNumber(int src)
		{
			assert src >= 0 : src;
			assert src < 1000000: src;
			
			Set<String> tMatches = new HashSet<>();
			tMatches.add("" + src);
			
			for (int tmp = src; tmp > 0; tmp = tmp / 10)
			{
				int digit = tmp % 10;
				tMatches.add("" + digit);
			}
			
			this.name = "" + src;
			this.matches = tMatches;
			this.sillyLength = new SillyLength();
		}
		
		/**
		 * Gets name (number as string).
		 */
		public String name()
		{
			return name;
		}
		
		/**
		 * Gets silly length.
		 */
		public SillyLength sillyLength()
		{
			return sillyLength;
		}
		
		/**
		 * Whether silly length is equal.
		 */
		@SuppressWarnings("unlikely-arg-type")
		public boolean isSillyLengthEqual(@Nullable Number number)
		{
			return sillyLength().equals(number);
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(@Nullable Object obj)
		{
			if (this == obj)
				return true;
			
			if (obj instanceof String)
				return matches.contains((String)obj);
			else if (obj instanceof Number)
				return matches.contains(obj.toString());
			else
				return false;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode()
		{
			return name.hashCode();
		}
		
		
	}
	
	/**
	 * Asserts that given collection contains given items and no other elements.
	 */
	private void assertContents(Collection<@Nullable String> collection, @Nullable String... items)
	{
		ArrayList<@Nullable String> remaining = new ArrayList<>(collection);
		
		for (String item : items)
		{
			assert remaining.remove(item) : "Collection has not enough of element [" + item + "]: " + collection;
		}
		
		assert remaining.isEmpty() : "Collection has extra elements [" + remaining + "]: " + collection;
	}
	
	/**
	 * Test some combined functionality.
	 */
	@Test
	public void testCombined()
	{
		assertContents(
			cData.stream()
				.filter(
					and(
						equalsAny(String::length, 3, 4),
						trueForAny(String::endsWith, "r", "e")
					)
				).collect(Collectors.toList())
			, "one", "four", "five");
		
		assertContents(
			Arrays.stream(OptionConstraint.values())
				.filter(
					and(
						equalsAny(OptionConstraint.NEGATIVE_ONE_OR_MORE, OptionConstraint.NON_NEGATIVE, OptionConstraint.POSITIVE),
						trueForAny((e, o) -> e.name().startsWith(o), "NEGATIVE", "NON")
					)
				)
				.map(OptionConstraint::name)
				.collect(Collectors.toList())
			, "NEGATIVE_ONE_OR_MORE", "NON_NEGATIVE");
	}
	
	/**
	 * Tests p() and logical predicates and/or/not. 
	 */
	@Test
	public void testPLogic()
	{
		assertContents(
			cDataWithNull.stream()
				.filter(
					and(
						e -> e != null,
						e -> nn(e).contains("e")
					)
				).collect(Collectors.toList())
			, "one", "three", "five");
		
		assertContents(
			cDataWithNull.stream()
				.filter(
					not( and(
						e -> e != null,
						e -> nn(e).contains("e")
					))
				).collect(Collectors.toList())
			, null, "two", "four");
		
		assertContents(
			cData.stream()
				.filter(
					not( and(
						e -> e.contains("e"),
						e -> e.startsWith("t")
					))
				).collect(Collectors.toList())
			, "one", "two", "four", "five");
		
		assertContents(
			cDataWithNull.stream()
				.filter(
					or(
						e -> e == null,
						e -> nn(e).contains("e")
					)
				).collect(Collectors.toList())
			, "one", "three", "five", null);
		
		assertContents(
			cDataWithNull.stream()
				.filter(
					or(
						e -> e == null,
						e -> nn(e).contains("e"),
						e -> nn(e).startsWith("t")
					)
				).collect(Collectors.toList())
			, "one", "two", "three", "five", null);
		
		assertContents(
			cData.stream()
				.filter(
					p((String e) -> e.contains("o"))
					.and(
						or(
							e -> nn(e).contains("e"),
							e -> nn(e).startsWith("t")
						))
				).collect(Collectors.toList())
			, "one", "two");
		
		
		assertContents(
			cData.stream()
				.filter(
					p((String e) -> e.contains("o"))
					.and(
						not ( or(
							e -> nn(e).contains("e"),
							e -> nn(e).startsWith("t")
						)))
				).collect(Collectors.toList())
			, "four");
	}
	
	/**
	 * Tests {@link LambdaUtil#equalsAny(java.util.function.Function, Object...)}
	 */
	@Test
	public void testEqualsAnyFunction()
	{
		
		assertContents(
			cData.stream()
				.filter(
					equalsAny(e -> e.length(), 3, null)
				).collect(Collectors.toList())
			, "one", "two");
		
		assertContents(
			cData.stream()
				.filter(
					equalsAny(String::length, 3, null)
				).collect(Collectors.toList())
			, "one", "two");

		
		assertContents(
			cData.stream()
				.filter(
					equalsAny(String::length, -7)
				).collect(Collectors.toList())
			);
		
		assertContents(
			cData.stream()
				.filter(
					equalsAny(String::length)
				).collect(Collectors.toList())
			);
		
	}
	
	/**
	 * Tests {@link LambdaUtil#equalsAnyIn(java.util.function.Function, Collection)}
	 */
	@Test
	public void testEqualsAnyInFunction()
	{
		
		assertContents(
			cData.stream()
				.filter(
					equalsAnyIn(e -> e.length(), Arrays.asList(3, null))
				).collect(Collectors.toList())
			, "one", "two");
		
		assertContents(
			cData.stream()
				.filter(
					equalsAnyIn(String::length, Arrays.asList(3, null))
				).collect(Collectors.toList())
			, "one", "two");

		
		assertContents(
			cData.stream()
				.filter(
					equalsAnyIn(String::length, Arrays.asList(-7))
				).collect(Collectors.toList())
			);
		
		assertContents(
			cData.stream()
				.filter(
					equalsAnyIn(String::length, new ArrayList<Integer>())
				).collect(Collectors.toList())
			);
		
	}
	
	/**
	 * Tests {@link LambdaUtil#equalsAny(Object...)}
	 */
	@Test
	public void testEqualsAnyNoArg()
	{
		
		assertContents(
			cData.stream()
				.filter(
					equalsAny("one", "two", null)
				).collect(Collectors.toList())
			, "one", "two");
		
		assertContents(
			cData.stream()
				.filter(
					equalsAny("nomatch")
				).collect(Collectors.toList())
			);

		
		assertContents(
			cData.stream()
				.filter(
					equalsAny()
				).collect(Collectors.toList())
			);
		
		
		assertContents(
			cDataWithNull.stream()
				.filter(
					equalsAny("two", null)
				).collect(Collectors.toList())
			, "two", null);
	}
	
	/**
	 * Tests {@link LambdaUtil#equalsAnyIn(Collection)}
	 */
	@Test
	public void testEqualsAnyInNoArg()
	{
		
		assertContents(
			cData.stream()
				.filter(
					equalsAnyIn(Arrays.asList("one", "two", null))
				).collect(Collectors.toList())
			, "one", "two");
		
		assertContents(
			cData.stream()
				.filter(
					equalsAnyIn(Arrays.asList("nomatch"))
				).collect(Collectors.toList())
			);

		
		assertContents(
			cData.stream()
				.filter(
					equalsAnyIn(new ArrayList<String>())
				).collect(Collectors.toList())
			);
		
		
		assertContents(
			cDataWithNull.stream()
				.filter(
					equalsAnyIn(Arrays.asList("two", null))
				).collect(Collectors.toList())
			, "two", null);
	}
	
	/**
	 * Tests {@link LambdaUtil#trueForAny(java.util.function.BiPredicate, Object...)}
	 */
	@Test
	public void testTrueForAnyBiPredicate()
	{
		assertContents(
			cData.stream()
				.filter(
					trueForAny((e, o) -> e.startsWith(o), "o", "f")
				).collect(Collectors.toList())
			, "one", "four", "five");
		
		assertContents(
			cData.stream()
				.filter(
					trueForAny(String::startsWith, "o", "f")
				).collect(Collectors.toList())
			, "one", "four", "five");
		
		assertContents(
			cData.stream()
				.filter(
					trueForAny(String::equals, "one", null)
				).collect(Collectors.toList())
			, "one");
		
		assertContents(
			cData.stream()
				.filter(
					trueForAny(String::startsWith, "nomatch")
				).collect(Collectors.toList())
			);
		
		assertContents(
			cData.stream()
				.filter(
					trueForAny((String e, String o) -> e.startsWith(o))
				).collect(Collectors.toList())
			);
		
	}
	
	/**
	 * Tests {@link LambdaUtil#trueForAnyIn(java.util.function.BiPredicate, Collection)}
	 */
	@Test
	public void testTrueForAnyInBiPredicate()
	{
		assertContents(
			cData.stream()
				.filter(
					trueForAnyIn((e, o) -> e.startsWith(o), Arrays.asList("o", "f"))
				).collect(Collectors.toList())
			, "one", "four", "five");
		
		assertContents(
			cData.stream()
				.filter(
					trueForAnyIn(String::startsWith, Arrays.asList("o", "f"))
				).collect(Collectors.toList())
			, "one", "four", "five");
		
		assertContents(
			cData.stream()
				.filter(
					trueForAnyIn(String::equals, Arrays.asList("one", null))
				).collect(Collectors.toList())
			, "one");
		
		assertContents(
			cData.stream()
				.filter(
					trueForAnyIn(String::startsWith, Arrays.asList("nomatch"))
				).collect(Collectors.toList())
			);
		
		assertContents(
			cData.stream()
				.filter(
					trueForAnyIn((String e, String o) -> e.startsWith(o), new ArrayList<String>())
				).collect(Collectors.toList())
			);
		
	}
	
	/**
	 * Tests {@link LambdaUtil#equalsAll(java.util.function.Function, Object...)}
	 */
	@Test
	public void testEqualsAllFunction()
	{
		
		assertContents(
			sillyData.stream()
				.filter(
					equalsAll(e -> e.sillyLength(), 3, 2)
				).map(SillyNumber::name).collect(Collectors.toList())
			, "111", "222", "333");
		
		assertContents(
			sillyData.stream()
				.filter(
					equalsAll(SillyNumber::sillyLength, 3, 2)
				).map(SillyNumber::name).collect(Collectors.toList())
			, "111", "222", "333");

		
		assertContents(
			sillyData.stream()
				.filter(
					equalsAll(SillyNumber::sillyLength, -7)
				).map(SillyNumber::name).collect(Collectors.toList())
			);
		
		assertContents(
			sillyData.stream()
				.filter(
					equalsAll(SillyNumber::sillyLength)
				).map(SillyNumber::name).collect(Collectors.toList())
			);
		
	}
	
	/**
	 * Tests {@link LambdaUtil#equalsAllIn(java.util.function.Function, Collection)}
	 */
	@Test
	public void testEqualsAllInFunction()
	{
		
		assertContents(
			sillyData.stream()
				.filter(
					equalsAllIn(e -> e.sillyLength(), nncn(Arrays.asList(3, 2)))
				).map(SillyNumber::name).collect(Collectors.toList())
			, "111", "222", "333");
		
		assertContents(
			sillyData.stream()
				.filter(
					equalsAllIn(SillyNumber::sillyLength, nncn(Arrays.asList(3, 2)))
				).map(SillyNumber::name).collect(Collectors.toList())
			, "111", "222", "333");

		
		assertContents(
			sillyData.stream()
				.filter(
					equalsAllIn(SillyNumber::sillyLength, nncn(Arrays.asList(-7)))
				).map(SillyNumber::name).collect(Collectors.toList())
			);
		
		assertContents(
			sillyData.stream()
				.filter(
					equalsAllIn(SillyNumber::sillyLength, new ArrayList<SillyNumber.SillyLength>())
				).map(SillyNumber::name).collect(Collectors.toList())
			);
		
	}
	
	/**
	 * Tests {@link LambdaUtil#equalsAll(Object...)}
	 */
	@Test
	public void testEqualsAllNoArg()
	{
		
		assertContents(
			sillyData.stream()
				.filter(
					equalsAll("2", "1")
				).map(SillyNumber::name).collect(Collectors.toList())
			, "123", "112", "212", "12345");
		
		assertContents(
			sillyData.stream()
				.filter(
					equalsAll("nomatch")
				).map(SillyNumber::name).collect(Collectors.toList())
			);

		
		assertContents(
			sillyData.stream()
				.filter(
					equalsAll()
				).map(SillyNumber::name).collect(Collectors.toList())
			);
		
		
		
		{
			List<@Nullable SillyNumber> result = sillyDataWithNull.stream()
				.filter(
					equalsAll(new @Nullable SillyNumber[] {null})
				).collect(Collectors.toList());
			
			assert result.size() == 1 : result;
			assert result.get(0) == null : result;
		}
		
	}
	
	/**
	 * Tests {@link LambdaUtil#equalsAllIn(Collection)}
	 */
	@Test
	public void testEqualsAllInNoArg()
	{
		
		assertContents(
			sillyData.stream()
				.filter(
					equalsAllIn(nncn(Arrays.asList("2", "1")))
				).map(SillyNumber::name).collect(Collectors.toList())
			, "123", "112", "212", "12345");
		
		assertContents(
			sillyData.stream()
				.filter(
					equalsAllIn(nncn(Arrays.asList("nomatch")))
				).map(SillyNumber::name).collect(Collectors.toList())
			);

		
		assertContents(
			sillyData.stream()
				.filter(
					equalsAllIn(new ArrayList<SillyNumber>())
				).map(SillyNumber::name).collect(Collectors.toList())
			);
		
		
		{
			List<@Nullable SillyNumber> result = sillyDataWithNull.stream()
				.filter(
					equalsAllIn(Arrays.asList((SillyNumber)null))
				).collect(Collectors.toList());
			
			assert result.size() == 1 : result;
			assert result.get(0) == null : result;
		}
	}
	
	/**
	 * Tests {@link LambdaUtil#trueForAll(java.util.function.BiPredicate, Object...)}
	 */
	@SuppressWarnings("unlikely-arg-type")
	@Test
	public void testTrueForAllBiPredicate()
	{
		assertContents(
			sillyData.stream()
				.filter(
					trueForAll((e, o) -> e.isSillyLengthEqual(o), 3, 4)
				).map(SillyNumber::name).collect(Collectors.toList())
			, "123", "456", "789", "134", "156", "234", "256");
		
		assertContents(
			sillyData.stream()
				.filter(
					trueForAll(SillyNumber::isSillyLengthEqual, 3, 4)
				).map(SillyNumber::name).collect(Collectors.toList())
			, "123", "456", "789", "134", "156", "234", "256");
		
		assertContents(
			sillyData.stream()
				.filter(
					trueForAll(SillyNumber::equals, 3, 4)
				).map(SillyNumber::name).collect(Collectors.toList())
			, "134", "234", "12345");
		
		assertContents(
			sillyData.stream()
				.filter(
					trueForAll(SillyNumber::isSillyLengthEqual, -7)
				).map(SillyNumber::name).collect(Collectors.toList())
			);
		
		assertContents(
			sillyData.stream()
				.filter(
					trueForAll((SillyNumber e, Number o) -> e.isSillyLengthEqual(o))
				).map(SillyNumber::name).collect(Collectors.toList())
			);
		
	}
	
	/**
	 * Tests {@link LambdaUtil#trueForAllIn(java.util.function.BiPredicate, Collection)}
	 */
	@SuppressWarnings("unlikely-arg-type")
	@Test
	public void testTrueForAllInBiPredicate()
	{
		assertContents(
			sillyData.stream()
				.filter(
					trueForAllIn((e, o) -> e.isSillyLengthEqual(o), Arrays.asList(5, 2))
				).map(SillyNumber::name).collect(Collectors.toList())
			, "55555");
		
		assertContents(
			sillyData.stream()
				.filter(
					trueForAllIn(SillyNumber::isSillyLengthEqual, Arrays.asList(5, 2))
				).map(SillyNumber::name).collect(Collectors.toList())
			, "55555");
		
		assertContents(
			sillyData.stream()
				.filter(
					trueForAllIn(SillyNumber::equals, Arrays.asList(5, 2))
				).map(SillyNumber::name).collect(Collectors.toList())
			, "256", "12345");
		
		assertContents(
			sillyData.stream()
				.filter(
					trueForAllIn(SillyNumber::isSillyLengthEqual, Arrays.asList(-7))
				).map(SillyNumber::name).collect(Collectors.toList())
			);
		
		assertContents(
			sillyData.stream()
				.filter(
					trueForAllIn((SillyNumber e, Number o) -> e.isSillyLengthEqual(o), new ArrayList<Number>())
				).map(SillyNumber::name).collect(Collectors.toList())
			);
		
	}
	
	/**
	 * Tests all value... methods
	 */
	@SuppressWarnings("unlikely-arg-type")
	public void testValueMethods()
	{
		assert valueEqualsAny("string", "s", "a", "d");
		assert valueEqualsAny("string", null, "s", "a", "d");
		assert !valueEqualsAny("string", "a", "d");
		assert !valueEqualsAny("string", new String[] {});
		
		assert valueEqualsAnyIn("string", Arrays.asList("s", "a", "d"));
		assert valueEqualsAnyIn("string", Arrays.asList(null, "s", "a", "d"));
		assert !valueEqualsAnyIn("string", Arrays.asList("a", "d"));
		assert !valueEqualsAnyIn("string", Arrays.asList(new String[] {}));
		
		assert valueTrueForAny(o -> "string".startsWith(o), "s", "a", "d");
		assert valueTrueForAny(o -> "string".startsWith(o), null, "s", "a", "d");
		assert !valueTrueForAny(o -> "string".startsWith(o), "a", "d");
		assert !valueTrueForAny(o -> "string".startsWith(o), new String[] {});
		
		assert valueTrueForAnyIn(o -> "string".startsWith(o), Arrays.asList("s", "a", "d"));
		assert valueTrueForAnyIn(o -> "string".startsWith(o), Arrays.asList(null, "s", "a", "d"));
		assert !valueTrueForAnyIn(o -> "string".startsWith(o), Arrays.asList("a", "d"));
		assert !valueTrueForAnyIn(o -> "string".startsWith(o), Arrays.asList(new String[] {}));
		
		
		assert valueEqualsAll(new SillyNumber(12345), 1, 2, 3, 4, 5);
		assert !valueEqualsAll(new SillyNumber(12345), 1, 2, 3, null, 4, 5);
		assert !valueEqualsAll(new SillyNumber(12345), 1, 2, 5, 6);
		assert !valueEqualsAll(new SillyNumber(12345), new SillyNumber[] {});
		
		assert valueEqualsAllIn(new SillyNumber(12345), Arrays.asList(1, 2, 3, 4, 5));
		assert !valueEqualsAllIn(new SillyNumber(12345), Arrays.asList(1, 2, 3, null, 4, 5));
		assert !valueEqualsAllIn(new SillyNumber(12345), Arrays.asList(1, 2, 5, 6));
		assert !valueEqualsAllIn(new SillyNumber(12345), Arrays.asList(new SillyNumber[] {}));
		
		assert valueTrueForAll(o -> new SillyNumber(12345).sillyLength().equals(o), 5, 6);
		assert !valueTrueForAll(o -> new SillyNumber(12345).sillyLength().equals(o), 5, null, 6);
		assert valueTrueForAll(o -> new SillyNumber(12345).sillyLength().equals(o), 5, 4);
		assert valueTrueForAll(o -> new SillyNumber(12345).sillyLength().equals(o), new Integer[] {});
		
		assert valueTrueForAllIn(o -> new SillyNumber(12345).sillyLength().equals(o), Arrays.asList(5, 6));
		assert !valueTrueForAllIn(o -> new SillyNumber(12345).sillyLength().equals(o), Arrays.asList(5, null, 6));
		assert valueTrueForAllIn(o -> new SillyNumber(12345).sillyLength().equals(o), Arrays.asList(5, 4));
		assert valueTrueForAllIn(o -> new SillyNumber(12345).sillyLength().equals(o), Arrays.asList(new Integer[] {}));
	}
}
