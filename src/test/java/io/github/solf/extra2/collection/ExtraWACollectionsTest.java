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

import static io.github.solf.extra2.testutil.AssertExtra.assertFails;
import static io.github.solf.extra2.util.NullUtil.nn;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.testng.annotations.Test;

import io.github.solf.extra2.exception.AssertionException;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.With;

/**
 * Tests for {@link WACollections}
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class ExtraWACollectionsTest
{
	/**
	 * Converts {@link Iterable} to {@link Supplier}.
	 * <p>
	 * Returns nulls when there are no more elements.
	 */
	@ParametersAreNonnullByDefault({})
	private static class SupplierFromIterable<T> implements Supplier<@Nullable T>
	{
		/**
		 * Iterator for underlying iterable
		 */
		private final Iterator<T> iterator;
		
		/**
		 * Constructor.
		 */
		public SupplierFromIterable(Iterable<T> iterable)
		{
			iterator = iterable.iterator();
		}

		/* (non-Javadoc)
		 * @see java.util.function.Supplier#get()
		 */
		@Override
		public @Nullable T get()
		{
			if (iterator.hasNext())
				return iterator.next();
			
			return null;
		}
		
	}
	
	/**
	 * Tests {@link WACollections#toIterable(java.util.function.Supplier)}
	 */
	@Test
	public void testSupplierIterable()
	{
		{
			// Simple success path test
			SupplierFromIterable<Integer> supplier = new SupplierFromIterable<>(Arrays.asList(0, 1, 2, 3));
			int count = -1;
			for (Integer item : WACollections.toIterable(supplier))
			{
				count++;
				assertEquals((int)item, count);
			}
			assertEquals(count, 3);
		}
		
		{
			// Empty collection test
			SupplierFromIterable<Integer> supplier = new SupplierFromIterable<>(Arrays.asList());
			int count = -1;
			for (Integer item : WACollections.toIterable(supplier))
			{
				count++;
				assertEquals((int)item, count);
			}
			assertEquals(count, -1);
		}
		
		{
			// Only null test
			ArrayList<@Nullable Integer> list = new ArrayList<>();
			list.add(null);
			SupplierFromIterable<@Nullable Integer> supplier = new SupplierFromIterable<>(list);
			int count = -1;
			for (Integer item : WACollections.toIterable(supplier))
			{
				count++;
				assertEquals((int)item, count);
			}
			assertEquals(count, -1);
		}
		
		{
			// Null in the middle test
			SupplierFromIterable<@Nullable Integer> supplier = new SupplierFromIterable<>(Arrays.asList(0, 1, null, 3));
			int count = -1;
			for (Integer item : WACollections.toIterable(supplier))
			{
				count++;
				assertEquals((int)item, count);
			}
			assertEquals(count, 1);
		}
		
		{
			// Exception test
			SupplierFromIterable<@Nullable Integer> supplier = new SupplierFromIterable<>(Arrays.asList(0, 1, null, 3));
			
			@ParametersAreNonnullByDefault({})
			@SuppressWarnings("deprecation") Iterator<Integer> iter = WACollections.toIterable(supplier).iterator();
			
			assertEquals((int)iter.next(), 0);
			assertEquals((int)iter.next(), 1);
			
			assertFalse(iter.hasNext());
			try
			{
				iter.next();
				assert false;
			} catch (NoSuchElementException e)
			{
				// expected
			}
			
			// subsequent attempts must have the same result even though there are non-null elements following
			assertFalse(iter.hasNext());
			try
			{
				iter.next();
				assert false;
			} catch (NoSuchElementException e)
			{
				// expected
			}
		}
		
		{
			// First 'hasNext' test
			SupplierFromIterable<Integer> supplier = new SupplierFromIterable<>(Arrays.asList(0, 1, 2));
			
			@SuppressWarnings("deprecation") Iterator<Integer> iterator = WACollections.toIterable(supplier).iterator();
			
			assert iterator.hasNext();
			
			assert iterator.next() == 0;
			assert iterator.next() == 1;
			assert iterator.next() == 2;
			
			assert !iterator.hasNext();
			
			try
			{
				iterator.next();
				assert false;
			} catch (NoSuchElementException e)
			{
				// expected
			}
		}
		
		{
			// First double 'hasNext' test
			SupplierFromIterable<Integer> supplier = new SupplierFromIterable<>(Arrays.asList(0, 1, 2));
			
			@SuppressWarnings("deprecation") Iterator<Integer> iterator = WACollections.toIterable(supplier).iterator();
			
			assert iterator.hasNext();
			assert iterator.hasNext();
			
			assert iterator.next() == 0;
			assert iterator.next() == 1;
			assert iterator.next() == 2;
			
			try
			{
				iterator.next();
				assert false;
			} catch (NoSuchElementException e)
			{
				// expected
			}
			
			assert !iterator.hasNext();
		}
		
		{
			// Last 'hasNext' test
			SupplierFromIterable<Integer> supplier = new SupplierFromIterable<>(Arrays.asList(0, 1, 2));
			
			@SuppressWarnings("deprecation") Iterator<Integer> iterator = WACollections.toIterable(supplier).iterator();
			
			assert iterator.next() == 0;
			assert iterator.next() == 1;
			
			assert iterator.hasNext();
			
			assert iterator.next() == 2;
			
			assert !iterator.hasNext();
			assert !iterator.hasNext();
			
			try
			{
				iterator.next();
				assert false;
			} catch (NoSuchElementException e)
			{
				// expected
			}
		}
		
		{
			// Double last 'hasNext' test
			SupplierFromIterable<Integer> supplier = new SupplierFromIterable<>(Arrays.asList(0, 1, 2));
			
			@SuppressWarnings("deprecation") Iterator<Integer> iterator = WACollections.toIterable(supplier).iterator();
			
			assert iterator.next() == 0;
			assert iterator.next() == 1;
			
			assert iterator.hasNext();
			assert iterator.hasNext();
			
			assert iterator.next() == 2;
			
			assert !iterator.hasNext();
			assert !iterator.hasNext();
			
			try
			{
				iterator.next();
				assert false;
			} catch (NoSuchElementException e)
			{
				// expected
			}
			try
			{
				iterator.next();
				assert false;
			} catch (NoSuchElementException e)
			{
				// expected
			}
		}
		
		{
			// Every 'hasNext' test
			SupplierFromIterable<Integer> supplier = new SupplierFromIterable<>(Arrays.asList(0, 1, 2));
			
			@SuppressWarnings("deprecation") Iterator<Integer> iterator = WACollections.toIterable(supplier).iterator();
			
			assert iterator.hasNext();
			assert iterator.next() == 0;
			
			assert iterator.hasNext();
			assert iterator.next() == 1;
			
			assert iterator.hasNext();
			assert iterator.next() == 2;
			
			assert !iterator.hasNext();
			assert !iterator.hasNext();
			
			try
			{
				iterator.next();
				assert false;
			} catch (NoSuchElementException e)
			{
				// expected
			}
		}
		
		{
			// Double every 'hasNext' test
			SupplierFromIterable<Integer> supplier = new SupplierFromIterable<>(Arrays.asList(0, 1, 2));
			
			@SuppressWarnings("deprecation") Iterator<Integer> iterator = WACollections.toIterable(supplier).iterator();
			
			assert iterator.hasNext();
			assert iterator.hasNext();
			assert iterator.next() == 0;
			
			assert iterator.hasNext();
			assert iterator.hasNext();
			assert iterator.next() == 1;
			
			assert iterator.hasNext();
			assert iterator.hasNext();
			assert iterator.next() == 2;
			
			
			try
			{
				iterator.next();
				assert false;
			} catch (NoSuchElementException e)
			{
				// expected
			}
			try
			{
				iterator.next();
				assert false;
			} catch (NoSuchElementException e)
			{
				// expected
			}
			
			assert !iterator.hasNext();
			assert !iterator.hasNext();
		}
	}
	
	/**
	 * Class for testing behavior of {@link RHashSet}
	 */
	@AllArgsConstructor
	@EqualsAndHashCode(onlyExplicitlyIncluded = true)
	@ToString
	private static class TKeyValue implements Cloneable
	{
		@EqualsAndHashCode.Include 
		private final String key;
		
		@With
		private int value;

		@SuppressWarnings("all")
		public boolean fullyEquals(@Nullable Object obj) // generated by Eclipse
		{
			if( this == obj )
				return true;
			if( obj == null )
				return false;
			if( getClass() != obj.getClass() )
				return false;
			TKeyValue other = (TKeyValue)obj;
			if( key == null )
			{
				if( other.key != null )
					return false;
			}
			else if( !key.equals(other.key) )
				return false;
			if( value != other.value )
				return false;
			return true;
		}

		@Override
		public TKeyValue clone()
		{
			try
			{
				return (TKeyValue)super.clone();
			} catch( CloneNotSupportedException e )
			{
				throw new AssertionException();
			}
		}
	}
	
	/**
	 * Tests {@link RHashSet} & {@link ForIterable}
	 */
	@SuppressWarnings("deprecation")
	@Test
	public void testRHashSet()
	{
		assertFails( () ->
			{@SuppressWarnings("unused") RHashSet<Object> cloneConstructorTest = new RHashSet<>((HashMap<Object, Object>)null);},
			"map is marked non-null but is null");
		
		TKeyValue nkey1 = new TKeyValue("nkey1", 1);
		
		TKeyValue key1 = new TKeyValue("key1", 1);
		RHashSet<@Nullable TKeyValue> set = new RHashSet<>();
		
		assertEquals(set.size(), 0);
		assertTrue(set.isEmpty());
		assertFalse(set.has(key1));
		assertFalse(set.has(nkey1));
		assertFalse(set.has(null));
		compareItems(set);
		set.stream().map(e -> Integer.parseInt(nn(e).toString())).collect(Collectors.toList()); // should fail if there are any elements
		
		assertNull(set.addOrReplace(key1));
		assertEquals(set.size(), 1);
		assertFalse(set.isEmpty());
		assertFalse(set.has(nkey1));
		assertFalse(set.has(null));
		compareItems(set, key1);
		assertTrue(set.has(key1));
		assertTrue(set.has(key1.clone()));
		assertTrue(set.has(key1.clone().withValue(321)));
		assertTrue(nn(set.get(key1)).fullyEquals(key1));
		assertTrue(nn(set.get(key1.clone())).fullyEquals(key1));
		assertTrue(nn(set.get(key1.clone().withValue(321))).fullyEquals(key1));
		assertFails( () ->
			{set.stream().map(e -> Integer.parseInt(nn(e).toString())).collect(Collectors.toList());}); // should fail if there are any elements
		compareItems(set, new ArrayList<@Nullable TKeyValue>(set.stream().map(e -> e).collect(Collectors.toList())).toArray(new @Nullable TKeyValue[0]));
		
		{
			RHashSet<TKeyValue> tset = new RHashSet<>(3);
			assertNotEquals(set, tset);
			assertTrue(tset.add(key1));
			assertFalse(tset.add(key1.withValue(876)));
			assertEquals(set, tset);
			
			tset.clear();
			assertNotEquals(set, tset);
			tset.addOrReplace(key1.clone());
			assertEquals(set, tset);
			
			tset.clear();
			assertNotEquals(set, tset);
			tset.addOrReplace(key1.clone().withValue(456));
			assertEquals(set, tset);
		}
		
		TKeyValue key1_2 = new TKeyValue("key1", 2);
		
		{
			RHashSet<@Nullable TKeyValue> tset = new RHashSet<>(5, 0.75f);
			assertNotEquals(set, tset);
			assertNull(tset.addIfAbsent(key1_2)); 
			assertTrue(tset.addIfAbsent(key1_2) == key1_2); 
			assertTrue(tset.addIfAbsent(key1) == key1_2);
			compareItems(tset, key1_2);
			assertEquals(set, tset.toUnmodifiableJavaSet());
			
			assertTrue(tset.removeAndGet(key1) == key1_2);
			assertNotEquals(set, tset);
			assertTrue(tset.addIfAbsentAndGet(key1_2) == key1_2); 
			assertTrue(tset.addIfAbsentAndGet(key1_2) == key1_2); 
			assertTrue(tset.addIfAbsentAndGet(key1) == key1_2);
			compareItems(tset, key1_2);
			assertEquals(set, tset.toUnmodifiableJavaSet());
			
			assertNull(tset.addIfAbsent(null));
			compareItems(tset, key1_2, null);
			assertNull(tset.addIfAbsent(null));
			compareItems(tset, key1_2, null);
			assertNull(tset.removeAndGet(null));
			assertNull(tset.addIfAbsent(null));
			assertTrue(tset.remove(null));
			assertFalse(tset.remove(null));
			
			assertNull(tset.addIfAbsentAndGet(null));
			compareItems(tset, key1_2, null);
			assertNull(tset.addIfAbsentAndGet(null));
			compareItems(tset, key1_2, null);
			assertNull(tset.removeAndGet(null));
			assertNull(tset.addIfAbsentAndGet(null));
			assertTrue(tset.remove(null));
			assertFalse(tset.remove(null));
			
			tset.clear();
			assertNotEquals(set, tset);
			tset.addOrReplace(key1_2.clone());
			assertEquals(set, tset.toUnmodifiableJavaSet());
			
			{
				RHashSet<@Nullable TKeyValue> aset = tset.clone();
				assert aset != tset;
				assertEquals(aset, tset.toUnmodifiableJavaSet());
				assertEquals(aset.hashCode(), tset.hashCode());
				
				tset = aset;
			}
			
			tset.clear();
			assertNotEquals(set, tset.toUnmodifiableJavaSet());
			tset.addOrReplace(key1_2.clone().withValue(456));
			assertEquals(set, tset.toUnmodifiableJavaSet());
		}
		
		TKeyValue prevValue = set.addOrReplace(key1_2);
		assertTrue(key1.fullyEquals(prevValue));
		{
			RHashSet<TKeyValue> tset = new RHashSet<>();
			assertNotEquals(set, tset.toUnmodifiableJavaSet());
			tset.addOrReplace(key1);
			assertEquals(set, tset.toUnmodifiableJavaSet()); 
			
			tset.clear();
			assertNotEquals(set, tset);
			tset.addOrReplace(key1.clone());
			assertEquals(set, tset);
			
			tset.clear();
			assertNotEquals(set, tset);
			tset.addOrReplace(key1.clone().withValue(456));
			assertEquals(set, tset);
		}
		assertEquals(set.size(), 1);
		assertFalse(set.has(nkey1));
		assertFalse(set.has(null));
		compareItems(set, key1_2);
		assertTrue(set.has(key1_2));
		assertTrue(set.has(key1_2.clone()));
		assertTrue(set.has(key1_2.clone().withValue(321)));
		assertTrue(nn(set.get(key1_2)).fullyEquals(key1_2));
		assertTrue(nn(set.get(key1_2.clone())).fullyEquals(key1_2));
		assertTrue(nn(set.get(key1_2.clone().withValue(321))).fullyEquals(key1_2));
		assertTrue(set.has(key1));
		assertTrue(set.has(key1.clone()));
		assertTrue(set.has(key1.clone().withValue(321)));
		assertTrue(nn(set.get(key1)).fullyEquals(key1_2));
		assertTrue(nn(set.get(key1.clone())).fullyEquals(key1_2));
		assertTrue(nn(set.get(key1.clone().withValue(321))).fullyEquals(key1_2));
		
		TKeyValue key2 = new TKeyValue("key2", 22);
		assertNull(set.addOrReplace(key2));
		assertEquals(set.size(), 2);
		assertFalse(set.has(nkey1));
		assertFalse(set.has(null));
		compareItems(set, key1_2, key2);
		
		{
			RHashSet<TKeyValue> tset = new RHashSet<>(Arrays.asList(key1));
			assertNotEquals(set, tset);
			tset.addOrReplace(key2);
			assertEquals(set, tset);
			
			tset.clear();
			assertNotEquals(set, tset);
			tset.addOrReplace(key1_2);
			assertNotEquals(set, tset);
			tset.addOrReplace(key2.clone());
			assertEquals(set, tset);
			
			tset.clear();
			assertNotEquals(set, tset);
			tset.addOrReplace(key1);
			assertNotEquals(set, tset);
			tset.addOrReplace(key2.clone().withValue(678));
			assertEquals(set, tset);
		}

		assertTrue(set.has(key1_2));
		assertTrue(set.has(key1_2.clone()));
		assertTrue(set.has(key1_2.clone().withValue(321)));
		assertTrue(nn(set.get(key1_2)).fullyEquals(key1_2));
		assertTrue(nn(set.get(key1_2.clone())).fullyEquals(key1_2));
		assertTrue(nn(set.get(key1_2.clone().withValue(321))).fullyEquals(key1_2));
		assertTrue(set.has(key1));
		assertTrue(set.has(key1.clone()));
		assertTrue(set.has(key1.clone().withValue(321)));
		assertTrue(nn(set.get(key1)).fullyEquals(key1_2));
		assertTrue(nn(set.get(key1.clone())).fullyEquals(key1_2));
		assertTrue(nn(set.get(key1.clone().withValue(321))).fullyEquals(key1_2));
		assertTrue(set.has(key2));
		assertTrue(set.has(key2.clone()));
		assertTrue(set.has(key2.clone().withValue(321)));
		assertTrue(nn(set.get(key2)).fullyEquals(key2));
		assertTrue(nn(set.get(key2.clone())).fullyEquals(key2));
		assertTrue(nn(set.get(key2.clone().withValue(321))).fullyEquals(key2));
		
		
		// check with nulls too
		assertNull(set.addOrReplace(null));
		assertEquals(set.size(), 3);
		assertFalse(set.has(nkey1));
		assertTrue(set.has(null));
		compareItems(set, key1_2, key2, null);
		compareItems(set, new ArrayList<@Nullable TKeyValue>(set.stream().map(e -> e).collect(Collectors.toList())).toArray(new @Nullable TKeyValue[0]));

		assertNull(set.addOrReplace(null));
		assertEquals(set.size(), 3);
		assertFalse(set.has(nkey1));
		assertTrue(set.has(null));
		compareItems(set, key1_2, key2, null);
		
		assertNull(set.get(null));
		assertTrue(set.has(key1_2));
		assertTrue(set.has(key1_2.clone()));
		assertTrue(set.has(key1_2.clone().withValue(321)));
		assertTrue(nn(set.get(key1_2)).fullyEquals(key1_2));
		assertTrue(nn(set.get(key1_2.clone())).fullyEquals(key1_2));
		assertTrue(nn(set.get(key1_2.clone().withValue(321))).fullyEquals(key1_2));
		assertTrue(set.has(key1));
		assertTrue(set.has(key1.clone()));
		assertTrue(set.has(key1.clone().withValue(321)));
		assertTrue(nn(set.get(key1)).fullyEquals(key1_2));
		assertTrue(nn(set.get(key1.clone())).fullyEquals(key1_2));
		assertTrue(nn(set.get(key1.clone().withValue(321))).fullyEquals(key1_2));
		assertTrue(set.has(key2));
		assertTrue(set.has(key2.clone()));
		assertTrue(set.has(key2.clone().withValue(321)));
		assertTrue(nn(set.get(key2)).fullyEquals(key2));
		assertTrue(nn(set.get(key2.clone())).fullyEquals(key2));
		assertTrue(nn(set.get(key2.clone().withValue(321))).fullyEquals(key2));
		
	}
	
	/**
	 * Verifies that given set contains exactly the elements (via fullyEquals())
	 */
	private void compareItems(RHashSet<@Nullable TKeyValue> set, @Nullable TKeyValue... elements)
	{
		compareItems(set, false, elements);
		compareItems(set, true, elements);
	}
	
	/**
	 * Verifies that given set contains exactly the elements (via fullyEquals())
	 */
	@SuppressWarnings("deprecation")
	private void compareItems(RHashSet<@Nullable TKeyValue> set, boolean useRemove, @Nullable TKeyValue... elements)
	{
		if (set.size() == 1)
		{
			TKeyValue actual = set.only();
			TKeyValue expected = elements[0];
			assertEquals(actual, expected);
			if (actual != null)
				assertTrue(actual.fullyEquals(expected));
		}
		else
			assertFails(() -> set.only(), "IllegalStateException");
		
		if (set.size() > 0)
		{
			TKeyValue actual = set.any();
			assertTrue(Arrays.asList(elements).contains(actual));
		}
		else
			assertFails(() -> set.any(), "IllegalStateException");
		
		int count = 0;
		for (@SuppressWarnings("unused") TKeyValue item : set)
			count++;
		assertEquals(elements.length, count);
		
		RHashSet<@Nullable TKeyValue> remaining = set.clone();
		
		for (@Nullable TKeyValue item : elements)
		{
			assertTrue(remaining.contains(item));
			assertTrue(remaining.has(item));
			if (item != null)
			{
				assertTrue(item.fullyEquals(remaining.get(item)));
				if (useRemove)
					assertTrue(remaining.remove(item));
				else
					assertTrue(item.fullyEquals(remaining.removeAndGet(item)));
			}
			else
			{
				assertNull(remaining.get(null));
				if (useRemove)
					assertTrue(remaining.remove(null));
				else
					assertNull(remaining.removeAndGet(null));
			}
			assertFalse(remaining.contains(item));
			assertFalse(remaining.has(item));
		}
		
		assertEquals(0, remaining.size());
	}
	
	/**
	 * Verifies that given iterable contains all the provided elements. Handles
	 * nulls properly.
	 */
	private <@Nullable T> void compareItemsViaIterable(Iterable<T> iterable, @SuppressWarnings("unchecked") T... items)
	{
		ArrayList<T> remaining = new ArrayList<>();
		for (T item : iterable)
			remaining.add(item);
		
		for (@Nullable T expected : items)
			assertTrue(remaining.remove(expected), "" + expected + ": " + remaining);
		
		assertTrue(remaining.isEmpty(), remaining.toString());
	}
	
	/**
	 * Verifies that given {@link Enumeration} contains all the provided elements. 
	 * Handles nulls properly.
	 */
	private <@Nullable T> void compareItemsViaEnumeration(Enumeration<T> enm, @SuppressWarnings("unchecked") T... items)
	{
		ArrayList<T> remaining = new ArrayList<>();
		while (enm.hasMoreElements())
			remaining.add(enm.nextElement());
		
		for (@Nullable T expected : items)
			assertTrue(remaining.remove(expected), "" + expected + ": " + remaining);
		
		assertTrue(remaining.isEmpty(), remaining.toString());
	}
	
	/**
	 * Verifies contents of the given {@link ForIterable} via both {@link Iterable}
	 * and {@link Enumeration}. Handles nulls properly. 
	 */
	private <@Nullable T> void compareItemsViaForIterable(Supplier<ForIterable<T>> iterableSupplier, 
		@SuppressWarnings("unchecked") T... items)
	{
		compareItemsViaIterable(iterableSupplier.get(), items);
		compareItemsViaEnumeration(iterableSupplier.get().enumeration(), items);
	}
	
	
	/**
	 * Tests {@link ForIterableOfIterable}
	 */
	@Test
	public void testIterableOfIterable()
	{
		HashMap<@Nullable String, @Nullable List<@Nullable Integer>> map1 = new HashMap<>();
	
		compareItemsViaForIterable(() -> WACollections.toIterableValuesFromMapWithCollectionElements(map1));
		
		map1.put(null, Arrays.asList(1));
		compareItemsViaForIterable(() -> WACollections.toIterableValuesFromMapWithCollectionElements(map1), 1);

		map1.put("a", Arrays.asList(1, 2));
		compareItemsViaForIterable(() -> WACollections.toIterableValuesFromMapWithCollectionElements(map1), 1, 1, 2);
		
		map1.put("b", null);
		compareItemsViaForIterable(() -> WACollections.toIterableValuesFromMapWithCollectionElements(map1), 1, 1, 2);
		
		map1.put(null, null);
		compareItemsViaForIterable(() -> WACollections.toIterableValuesFromMapWithCollectionElements(map1), 1, 2);
		
		map1.put("c", Collections.emptyList());
		compareItemsViaForIterable(() -> WACollections.toIterableValuesFromMapWithCollectionElements(map1), 1, 2);
		
		map1.put("d", Arrays.asList(10));
		map1.put("e", Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));
		compareItemsViaForIterable(() -> WACollections.toIterableValuesFromMapWithCollectionElements(map1), 1, 2, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

		compareItemsViaForIterable(() -> ForIterableOfIterable.of(map1.values(), e -> e, () -> Collections.emptyIterator())
			, 1, 2, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
		
		compareItemsViaForIterable(() -> ForIterableOfIterable.of(map1.values(), e -> e, () -> Arrays.asList(-1, -2).iterator())
			, 1, 2, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, -1, -2, -1, -2);
	}
}
