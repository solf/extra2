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
package io.github.solf.extra2.collection;

import java.lang.reflect.Field;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.testng.annotations.Test;

import io.github.solf.extra2.util.TypeUtil;

/**
 * Tests for extra Collections extensions.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class ExtraCollectionAndKryoDBTest
{
	
	/**
	 * Test for {@link ItemSampler}
	 */
	@Test
	public void testItemSampler()
	{
		{
			try
			{
				@SuppressWarnings("unused")
				ItemSampler<String> is = new ItemSampler<>(-1);
				assert false;
			} catch (IllegalArgumentException e)
			{
				assert e.toString().contains("Capacity must be 2 or more");
			}
			
		}
		{
			try
			{
				@SuppressWarnings("unused")
				ItemSampler<String> is = new ItemSampler<>(0);
				assert false;
			} catch (IllegalArgumentException e)
			{
				assert e.toString().contains("Capacity must be 2 or more");
			}
			
		}
		{
			try
			{
				@SuppressWarnings("unused")
				ItemSampler<String> is = new ItemSampler<>(1);
				assert false;
			} catch (IllegalArgumentException e)
			{
				assert e.toString().contains("Capacity must be 2 or more");
			}
			
		}
		
		{
			// Special case with size = 2
			final int size = 2;
			testItemSampler(size, new Integer[] {});
			testItemSampler(size, new Integer[] {1}, 1);
			testItemSampler(size, new Integer[] {1, 2}, 1, 2);
			testItemSampler(size, new Integer[] {1, 3}, 1, 2, 3);
			testItemSampler(size, new Integer[] {1, 7}, 1, 2, 3, 4, 5, 6, 7);
			
			// Also test using the same sampler (multiple getSampleList)
			ItemSampler<Integer> is = new ItemSampler<>(size);
			testItemSamplerContents(is, new Integer[] {});
			is.add(1);
			testItemSamplerContents(is, new Integer[] {1});
			is.add(2);
			testItemSamplerContents(is, new Integer[] {1, 2});
			is.add(3);
			testItemSamplerContents(is, new Integer[] {1, 3});
			is.add(4);
			is.add(5);
			is.add(6);
			is.add(7);
			testItemSamplerContents(is, new Integer[] {1, 7});
		}
		
		{
			// Corner case with size = 3
			final int size = 3;
			testItemSampler(size, new Integer[] {});
			testItemSampler(size, new Integer[] {1}, 1);
			testItemSampler(size, new Integer[] {1, 2}, 1, 2);
			testItemSampler(size, new Integer[] {1, 2, 3}, 1, 2, 3);
			testItemSampler(size, new Integer[] {1, 2, 4}, 1, 2, 3, 4);
			testItemSampler(size, new Integer[] {1, 3, 5}, 1, 2, 3, 4, 5);
			testItemSampler(size, new Integer[] {1, 3, 7}, 1, 2, 3, 4, 5, 6, 7);
			testItemSampler(size, new Integer[] {1, 7, 10}, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
			
			// Also test using the same sampler (multiple getSampleList)
			ItemSampler<Integer> is = new ItemSampler<>(size);
			testItemSamplerContents(is, new Integer[] {});
			is.add(1);
			testItemSamplerContents(is, new Integer[] {1});
			is.add(2);
			testItemSamplerContents(is, new Integer[] {1, 2});
			is.add(3);
			testItemSamplerContents(is, new Integer[] {1, 2, 3});
			is.add(4);
			is.add(5);
			is.add(6);
			is.add(7);
			testItemSamplerContents(is, new Integer[] {1, 3, 7});
		}
		
		{
			// Almost corner case with size = 4
			final int size = 4;
			testItemSampler(size, new Integer[] {});
			testItemSampler(size, new Integer[] {1}, 1);
			testItemSampler(size, new Integer[] {1, 2}, 1, 2);
			testItemSampler(size, new Integer[] {1, 2, 3}, 1, 2, 3);
			testItemSampler(size, new Integer[] {1, 2, 3, 4}, 1, 2, 3, 4);
			testItemSampler(size, new Integer[] {1, 2, 3, 5}, 1, 2, 3, 4, 5);
			testItemSampler(size, new Integer[] {1, 2, 4, 7}, 1, 2, 3, 4, 5, 6, 7);
			
			// Also test using the same sampler (multiple getSampleList)
			ItemSampler<Integer> is = new ItemSampler<>(size);
			testItemSamplerContents(is, new Integer[] {});
			is.add(1);
			testItemSamplerContents(is, new Integer[] {1});
			is.add(2);
			testItemSamplerContents(is, new Integer[] {1, 2});
			is.add(3);
			testItemSamplerContents(is, new Integer[] {1, 2, 3});
			is.add(4);
			is.add(5);
			is.add(6);
			is.add(7);
			testItemSamplerContents(is, new Integer[] {1, 2, 4, 7});
		}
		
		{
			// Somewhat realistic test.
			ItemSampler<Integer> is = new ItemSampler<>(10);
			for (int i = 1; i < 100; i++)
				is.add(i);
			System.out.println(is.getSampleList());
			testItemSamplerContents(is, new Integer[] {1, 12, 20, 32, 40, 50, 62, 76, 84, 99});
			
			is = new ItemSampler<>(10);
			for (int i = 1; i < 25000; i++)
				is.add(i);
			System.out.println(is.getSampleList());
			testItemSamplerContents(is, new Integer[] {1, 2276, 4885, 7155, 10478, 12679, 15342, 18565, 22465, 24999});
			
			is = new ItemSampler<>(10);
			for (int i = 1; i < 1500; i++)
				is.add(i);
			System.out.println(is.getSampleList()); // test sideeffect
			testItemSamplerContents(is, new Integer[] {1, 76, 226, 333, 490, 595, 721, 1059, 1282, 1499});
			for (int i = 1500; i < 25000; i++)
				is.add(i);
			System.out.println(is.getSampleList()); // test sideeffect
			testItemSamplerContents(is, new Integer[] {1, 2276, 4885, 7155, 10478, 12679, 15342, 18565, 22465, 24999});
			
			is = new ItemSampler<>(10);
			for (int i = 1; i < 1500; i++)
				is.add(i);
			System.out.println(is.clone().getSampleList()); // test NO side-effect
			testItemSamplerContents(is.clone(), new Integer[] {1, 76, 226, 333, 490, 595, 721, 1059, 1282, 1499});
			for (int i = 1500; i < 25000; i++)
				is.add(i);
			System.out.println(is.getSampleList()); // test NO side-effect
			testItemSamplerContents(is, new Integer[] {1, 2276, 4885, 7155, 10478, 12679, 15342, 18565, 22465, 24999});
			
			// Check sizes of internal arrays.
			is = new ItemSampler<>(10);
			for (int i = 1; i < 1500000; i++)
				is.add(i);
			System.out.println(is.getSampleList());
			testItemSamplerContents(is, new Integer[] {1, 103248, 221327, 324047, 474439, 694629, 1017009, 1230582, 1353641, 1499999});
		}
		
		{
			// Test nulls.
			ItemSampler<@Nullable Integer> is = new ItemSampler<>(10);
			for (int i = 0; i < 100; i++)
			{
				if ((i % 2) == 0)
					is.add(null);
				else
					is.add(i);
			}
			System.out.println(is.getSampleList());
			testItemSamplerContentsNullable(is, new @Nullable Integer[] {null, 11, 19, 31, 39, 49, 61, 75, 83, 99});
		}
		
		{
			// Test nulls 2.
			ItemSampler<@Nullable Integer> is = new ItemSampler<>(10);
			for (int i = 0; i < 1000; i++)
			{
				if ((i % 2) == 0)
					is.add(null);
				else
					is.add(i);
			}
			System.out.println(is.getSampleList());
			testItemSamplerContentsNullable(is, new @Nullable Integer[] {null, 75, 225, null, 403, 489, null, null, 873, 999});
		}
	}
	
	/**
	 * Tests item sampler on Integers.
	 */
	private void testItemSampler(int capacity, Integer[] expectedItems, @Nonnull Integer... itemsToAdd)
	{
		ItemSampler<Integer> is = new ItemSampler<>(capacity);
		for (Integer item : itemsToAdd)
			is.add(item);
		
		testItemSamplerContents(is, expectedItems);
	}

	/**
	 * Tests contents of {@link ItemSampler}
	 */
	@SuppressWarnings("null")
	@NonNullByDefault({})
	private void testItemSamplerContents(@Nonnull ItemSampler<@Nonnull Integer> is, @Nonnull Integer @Nonnull... expectedItems)
	{
		testItemSamplerContentsNullable(is, expectedItems);
	}
	
	/**
	 * Tests contents of {@link ItemSampler}
	 */
	@NonNullByDefault({})
	private void testItemSamplerContentsNullable(@Nonnull ItemSampler<@Nullable Integer> is, @Nullable Integer @Nonnull... expectedItems)
	{
		List<Integer> items = is.getSampleList();
		assert expectedItems.length == items.size() : "Mismatched size: expected " + expectedItems.length + ", got " + items.size();
		
		for (int i = 0; i < expectedItems.length; i++)
		{
			if (items.get(i) == null)
				assert expectedItems[i] == null : "Mismatched value at index [" + i + "] -- expected: " + expectedItems[i] + ", got: " + items.get(i);
			else
			{
				int v = items.get(i);
				assert v == expectedItems[i] : "Mismatched value at index [" + i + "] -- expected: " + expectedItems[i] + ", got: " + items.get(i) + "; samples=" + items;
//				{}System.err.println("Mismatched value at index [" + i + "] -- expected: " + expectedItems[i] + ", got: " + items.get(i) + "; samples=" + items);
			}
		}
	}
	
	/**
	 * Gets field value via reflection.
	 */
	@SuppressWarnings("unused")
	@NonNullByDefault({})
	private <T> T reflectField(@Nonnull Object inst, @Nonnull String fieldName)
	{
		try
		{
			Class<? extends @Nonnull Object> clazz = inst.getClass();
			
			Field field;
			try
			{
				field = clazz.getDeclaredField(fieldName);
			} catch (NoSuchFieldException e)
			{
				field = clazz.getField(fieldName);
			}
			
			field.setAccessible(true);
			
			return TypeUtil.coerceUnknown(field.get(inst));
		} catch (Exception e)
		{
			if (e instanceof RuntimeException)
				throw (RuntimeException)e;
			
			throw new IllegalStateException("Reflection access failed: " + e, e);
		}
	}
}
