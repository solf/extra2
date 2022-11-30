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
import static io.github.solf.extra2.testutil.AssertExtra.assertLessOrEqual;
import static io.github.solf.extra2.util.NullUtil.nn;
import static io.github.solf.extra2.util.NullUtil.nnChecked;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.testng.annotations.Test;

import io.github.solf.extra2.exception.AssertionException;
import io.github.solf.extra2.io.BAOSInputStream;
import io.github.solf.extra2.testutil.TestUtil;
import io.github.solf.extra2.util.TypeUtil;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.With;

/**
 * Tests for {@link WACollections}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class ExtraWACollectionsTest
{
	/**
	 * Converts {@link Iterable} to {@link Supplier}.
	 * <p>
	 * Returns nulls when there are no more elements.
	 */
	@NonNullByDefault({})
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
			
			@NonNullByDefault({})
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
	 * <p>
	 * NB: equals/hashCode do NOT include all fields on purpose!
	 */
	@AllArgsConstructor
	@EqualsAndHashCode(onlyExplicitlyIncluded = true)
	@ToString
	private static class TKeyValue implements Cloneable, Serializable
	{
		@EqualsAndHashCode.Include 
		private final String key;
		
		@With
		private int value;

		/**
		 * This compares ALL fields in the object, not just the equals/hashCode stuff
		 */
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
	 * Factory class for creating instances of various subclasses of {@link BSet}
	 * via static *.create* methods.
	 */
	@RequiredArgsConstructor
	private static class BSetFactory
	{
		/**
		 * Factory class that contains create* methods.
		 */
		private final Class<?> factoryClass;
		
		/**
		 * Factory create method.
		 */
		public <S extends SerializableBSet<E>, E> S create()
		{
			return TestUtil.invokeInaccessibleMethod(factoryClass, "create", null);
		}
		
		/**
		 * Factory create method.
		 */
		public <S extends SerializableBSet<E>, E> S create(int initialCapacity)
		{
			return TestUtil.invokeInaccessibleMethod(factoryClass, "create", null, 
				int.class, initialCapacity);
		}
		
		/**
		 * Factory create method.
		 */
		public <S extends SerializableBSet<E>, E> S create(@Nonnull Collection<? extends E> c)
		{
			return TestUtil.invokeInaccessibleMethod(factoryClass, "create", null, 
				Collection.class, c);
		}
		
		/**
		 * Factory create method.
		 */
		public <S extends SerializableBSet<E>, E> S createFromReadOnly(@Nonnull ReadOnlyCollection<? extends E> c)
		{
			return TestUtil.invokeInaccessibleMethod(factoryClass, "createFromReadOnly", null, 
				ReadOnlyCollection.class, c);
		}
		
		/**
		 * Factory create method.
		 */
		public <S extends SerializableBSet<E>, E> S create(int initialCapacity, float loadFactor)
		{
			return TestUtil.invokeInaccessibleMethod(factoryClass, "create", null, 
				int.class, initialCapacity, float.class, loadFactor);
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
			assertTrue(tset.addIfAbsent(key1));
			assertFalse(tset.addIfAbsent(key1.withValue(876)));
			assertEquals(set, tset);
			assertTrue(tset.addIfAbsent(nkey1));
			assertFalse(tset.addIfAbsent(nkey1.withValue(876)));
			assertNotEquals(set, tset);
			assertEquals(tset.size(), 2);
			assertTrue(tset.has(key1));
			assertTrue(tset.has(nkey1));
			
			tset.clear();
			assertNotEquals(set, tset);
			tset.addOrReplace(key1.clone());
			assertEquals(set, tset);
			
			tset.clear();
			assertNotEquals(set, tset);
			tset.addOrReplace(key1.clone().withValue(456));
		}
		
		TKeyValue key1_2 = new TKeyValue("key1", 2);
		
		{
			RHashSet<@Nullable TKeyValue> tset = new RHashSet<>(5, 0.75f);
			assertNotEquals(set, tset);
			assertNull(tset.addIfAbsentAndGetIfPresent(key1_2)); 
			assertTrue(tset.addIfAbsentAndGetIfPresent(key1_2) == key1_2); 
			assertTrue(tset.addIfAbsentAndGetIfPresent(key1) == key1_2);
			compareItems(tset, key1_2);
			assertEquals(set, tset.toUnmodifiableJavaSet());
			
			assertTrue(tset.removeAndGet(key1) == key1_2);
			assertNotEquals(set, tset);
			assertTrue(tset.addIfAbsentAndGet(key1_2) == key1_2); 
			assertTrue(tset.addIfAbsentAndGet(key1_2) == key1_2); 
			assertTrue(tset.addIfAbsentAndGet(key1) == key1_2);
			compareItems(tset, key1_2);
			assertEquals(set, tset.toUnmodifiableJavaSet());
			
			assertNull(tset.addIfAbsentAndGetIfPresent(null));
			compareItems(tset, key1_2, null);
			assertNull(tset.addIfAbsentAndGetIfPresent(null));
			compareItems(tset, key1_2, null);
			assertNull(tset.removeAndGet(null));
			assertNull(tset.addIfAbsentAndGetIfPresent(null));
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
				
				tset.clear(); // test decoupling
				
				assertNotEquals(aset, tset.toUnmodifiableJavaSet());
				assertNotEquals(aset.hashCode(), tset.hashCode());
				
				assertEquals(tset.size(), 0);
				assertEquals(aset.size(), 1);
				
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
	 * Gets an element from set (via scanning an entire set).
	 */
	@Nullable
	private <E> E getSetElement(Set<E> set, @Nonnull E element)
	{
		for (E item : set)
		{
			if (element.equals(item))
				return item;
		}
		
		return null;
	}
	
	/**
	 * Tests for various flavors of {@link BSet}
	 */
	@Test
	public void testBSet()
	{
		internalTestBSet(BHashSet.create(), new BSetFactory(BHashSet.class));
		internalTestBSet(BHashSet.create(3), new BSetFactory(BHashSet.class));
		internalTestBSet(BHashSet.create(3, 0.6f), new BSetFactory(BHashSet.class));
		internalTestBSet(BHashSet.create(Collections.emptySet()), new BSetFactory(BHashSet.class));

		internalTestBSet(SerializableBHashSet.create(), new BSetFactory(SerializableBHashSet.class));
		internalTestBSet(SerializableBHashSet.create(3), new BSetFactory(SerializableBHashSet.class));
		internalTestBSet(SerializableBHashSet.create(3, 0.6f), new BSetFactory(SerializableBHashSet.class));
		internalTestBSet(SerializableBHashSet.create(Collections.emptySet()), new BSetFactory(SerializableBHashSet.class));
		
		internalTestBSet(EHashSet.create(), new BSetFactory(EHashSet.class));
		internalTestBSet(EHashSet.create(3), new BSetFactory(EHashSet.class));
		internalTestBSet(EHashSet.create(3, 0.6f), new BSetFactory(EHashSet.class));
		internalTestBSet(EHashSet.create(Collections.emptySet()), new BSetFactory(EHashSet.class));
		
		internalTestBSet(SerializableEHashSet.create(), new BSetFactory(SerializableEHashSet.class));
		internalTestBSet(SerializableEHashSet.create(3), new BSetFactory(SerializableEHashSet.class));
		internalTestBSet(SerializableEHashSet.create(3, 0.6f), new BSetFactory(SerializableEHashSet.class));
		internalTestBSet(SerializableEHashSet.create(Collections.emptySet()), new BSetFactory(SerializableEHashSet.class));
		
		internalTestBSet(RHashSet.create(), new BSetFactory(RHashSet.class));
		internalTestBSet(RHashSet.create(3), new BSetFactory(RHashSet.class));
		internalTestBSet(RHashSet.create(3, 0.6f), new BSetFactory(RHashSet.class));
		internalTestBSet(RHashSet.create(Collections.emptySet()), new BSetFactory(RHashSet.class));
		
		internalTestBSet(new RHashSet<>(), new BSetFactory(RHashSet.class));
		internalTestBSet(new RHashSet<>(3), new BSetFactory(RHashSet.class));
		internalTestBSet(new RHashSet<>(3, 0.6f), new BSetFactory(RHashSet.class));
		internalTestBSet(new RHashSet<>(Collections.emptySet()), new BSetFactory(RHashSet.class));
		
		
		internalTestBSet(BSet.of(new HashSet<>()), new BSetFactory(BHashSet.class));
		internalTestBSet(BSet.of(new HashSet<>(3)), new BSetFactory(BHashSet.class));
		internalTestBSet(BSet.of(new HashSet<>(3, 0.6f)), new BSetFactory(BHashSet.class));
		internalTestBSet(BSet.of(new HashSet<>(Collections.emptySet())), new BSetFactory(BHashSet.class));
		
		internalTestBSet(SerializableBSet.of(new HashSet<>()), new BSetFactory(BHashSet.class));
		internalTestBSet(SerializableBSet.of(new HashSet<>(3)), new BSetFactory(BHashSet.class));
		internalTestBSet(SerializableBSet.of(new HashSet<>(3, 0.6f)), new BSetFactory(BHashSet.class));
		internalTestBSet(SerializableBSet.of(new HashSet<>(Collections.emptySet())), new BSetFactory(BHashSet.class));
		
		// Cheat since we know the underlying impl
		internalTestBSet(TypeUtil.coerce(ReadOnlySet.of(new HashSet<>())), new BSetFactory(BHashSet.class));
		internalTestBSet(TypeUtil.coerce(ReadOnlySet.of(new HashSet<>(3))), new BSetFactory(BHashSet.class));
		internalTestBSet(TypeUtil.coerce(ReadOnlySet.of(new HashSet<>(3, 0.6f))), new BSetFactory(BHashSet.class));
		internalTestBSet(TypeUtil.coerce(ReadOnlySet.of(new HashSet<>(Collections.emptySet()))), new BSetFactory(BHashSet.class));
		
		assertNull(ReadOnlySet.toNullableUnmodifiableJavaSet(null));
		{
			Set<Integer> rs = nnChecked(ReadOnlySet.toNullableUnmodifiableJavaSet(
				ReadOnlySet.of(Set.of(1, 2))
			));
			assertEquals(rs.size(), 2);
			assertTrue(rs.contains(1));
			assertTrue(rs.contains(2));
			assertFails(() -> rs.add(3));
		}
	}
	
	/**
	 * Tests {@link ReadOnlySet#emptyReadOnlySet()}
	 */
	@SuppressWarnings("unlikely-arg-type")
	@Test
	public void testEmptySet()
	{
		assertTrue(ReadOnlySet.emptyReadOnlySet().equals(Collections.emptySet()));
		assertTrue(Collections.emptySet().equals(ReadOnlySet.emptyReadOnlySet()));
		assertEquals(ReadOnlySet.emptyReadOnlySet().hashCode(), Collections.emptySet().hashCode());
		
		assertTrue(ReadOnlySet.emptyReadOnlySet().equals(Set.of()));
		assertTrue(Set.of().equals(ReadOnlySet.emptyReadOnlySet()));
		assertEquals(ReadOnlySet.emptyReadOnlySet().hashCode(), Set.of().hashCode());
		
		assertFalse(ReadOnlySet.emptyReadOnlySet().equals(Set.of("1", "2")));
		assertFalse(Set.of("1", "2").equals(ReadOnlySet.emptyReadOnlySet()));
		assertNotEquals(ReadOnlySet.emptyReadOnlySet().hashCode(), Set.of("1", "2").hashCode());
	}
	
	/**
	 * Tests {@link ReadOnlyList#emptyReadOnlyList()}
	 */
	@SuppressWarnings("unlikely-arg-type")
	@Test
	public void testEmptyList()
	{
		assertTrue(ReadOnlyList.emptyReadOnlyList().equals(Collections.emptyList()));
		assertTrue(Collections.emptyList().equals(ReadOnlyList.emptyReadOnlyList()));
		assertEquals(ReadOnlyList.emptyReadOnlyList().hashCode(), Collections.emptyList().hashCode());
		
		assertTrue(ReadOnlyList.emptyReadOnlyList().equals(List.of()));
		assertTrue(List.of().equals(ReadOnlyList.emptyReadOnlyList()));
		assertEquals(ReadOnlyList.emptyReadOnlyList().hashCode(), List.of().hashCode());
		
		assertFalse(ReadOnlyList.emptyReadOnlyList().equals(List.of("1", "2")));
		assertFalse(List.of("1", "2").equals(ReadOnlyList.emptyReadOnlyList()));
		assertNotEquals(ReadOnlyList.emptyReadOnlyList().hashCode(), List.of("1", "2").hashCode());
		
		assertNull(ReadOnlyList.emptyReadOnlyList().first());
		assertNull(ReadOnlyList.emptyReadOnlyList().last());
	}

	/**
	 * Not all interfaces are declared as serializable, so this takes care of
	 * casting.
	 */
	private <S extends BSet<@Nullable TKeyValue>> void internalTestBSet(S set, BSetFactory factory)
	{
		internalTestBSet0(TypeUtil.coerce(set), factory);
	}
	
	/**
	 * Tests {@link RHashSet} & {@link ForIterable}
	 */
	@SuppressWarnings("deprecation")
	private <S extends SerializableBSet<@Nullable TKeyValue>> void internalTestBSet0(S set, BSetFactory factory)
	{
		TKeyValue nkey1 = new TKeyValue("nkey1", 1);
		
		TKeyValue key1 = new TKeyValue("key1", 1);
		
		assertEquals(set.size(), 0);
		assertTrue(set.isEmpty());
		assertFalse(set.has(key1));
		assertFalse(set.has(nkey1));
		assertFalse(set.has(null));
		compareItems(set);
		set.stream().map(e -> Integer.parseInt(nn(e).toString())).collect(Collectors.toList()); // should fail if there are any elements
		
		assertTrue(set.add(key1));
		assertEquals(set.size(), 1);
		assertFalse(set.isEmpty());
		assertFalse(set.has(nkey1));
		assertFalse(set.has(null));
		compareItems(set, key1);
		assertTrue(set.has(key1));
		assertTrue(set.has(key1.clone()));
		assertTrue(set.has(key1.clone().withValue(321)));
		assertTrue(nn(getSetElement(set, key1)).fullyEquals(key1));
		assertTrue(nn(getSetElement(set, key1.clone())).fullyEquals(key1));
		assertTrue(nn(getSetElement(set, key1.clone().withValue(321))).fullyEquals(key1));
		assertFails( () ->
			{set.stream().map(e -> Integer.parseInt(nn(e).toString())).collect(Collectors.toList());}); // should fail if there are any elements
		compareItems(set, new ArrayList<@Nullable TKeyValue>(set.stream().map(e -> e).collect(Collectors.toList())).toArray(new @Nullable TKeyValue[0]));
		
		{
			BSet<TKeyValue> tset = factory.create(3);
			
			assertNotEquals(set, tset);
			assertTrue(tset.add(key1));
			assertFalse(tset.add(key1.withValue(876)));
			assertEquals(set, tset);
			
			tset.clear();
			assertNotEquals(set, tset);
			assertTrue(tset.add(key1.clone()));
			assertEquals(set, tset);
			
			tset.clear();
			assertNotEquals(set, tset);
			assertTrue(tset.add(key1.clone().withValue(456)));
			assertEquals(set, tset);
		}
		
		TKeyValue key1_2 = new TKeyValue("key1", 2);
		
		{
			SerializableBSet<@Nullable TKeyValue> tset = factory.create(5, 0.75f);
			
			assertNotEquals(set, tset);
			assertTrue(tset.add(key1_2)); 
			assertFalse(tset.add(key1_2)); 
			assertFalse(tset.add(key1));
			compareItems(tset, key1_2);
			assertEquals(set, tset.toUnmodifiableJavaSet());
			
			assertTrue(tset.removeElement(key1));
			compareItems(tset);
			assertEquals(tset.size(), 0);
			assertTrue(tset.isEmpty());
			assertTrue(tset.add(key1_2)); 
			assertTrue(tset.remove(key1));
			compareItems(tset);
			assertEquals(tset.size(), 0);
			assertTrue(tset.isEmpty());
			assertTrue(tset.add(key1_2)); 
			assertTrue(tset.add(nkey1)); 
			compareItems(tset, key1_2, nkey1);
			assertEquals(tset.size(), 2);
			assertFalse(tset.isEmpty());
			assertTrue(tset.removeElement(nkey1));
			compareItems(tset, key1_2);
			assertEquals(tset.size(), 1);
			assertFalse(tset.isEmpty());
			assertTrue(tset.add(nkey1)); 
			compareItems(tset, key1_2, nkey1);
			assertEquals(tset.size(), 2);
			assertFalse(tset.isEmpty());
			assertTrue(tset.remove(nkey1));
			compareItems(tset, key1_2);
			assertEquals(tset.size(), 1);
			assertFalse(tset.isEmpty());
			
			assertTrue(tset.add(null));
			compareItems(tset, key1_2, null);
			assertFalse(tset.add(null));
			compareItems(tset, key1_2, null);
			assertTrue(tset.remove(null));
			compareItems(tset, key1_2);
			assertTrue(tset.add(null));
			compareItems(tset, key1_2, null);
			assertTrue(tset.remove(null));
			assertFalse(tset.remove(null));
			compareItems(tset, key1_2);
			
			assertTrue(tset.add(null));
			compareItems(tset, key1_2, null);
			assertFalse(tset.add(null));
			compareItems(tset, key1_2, null);
			assertTrue(tset.removeElement(null));
			compareItems(tset, key1_2);
			assertTrue(tset.add(null));
			compareItems(tset, key1_2, null);
			assertTrue(tset.removeElement(null));
			assertFalse(tset.removeElement(null));
			compareItems(tset, key1_2);
			
			tset.clear();
			assertNotEquals(set, tset);
			assertTrue(tset.add(key1_2.clone()));
			assertEquals(set, tset.toUnmodifiableJavaSet());
			
			{
				SerializableBSet<@Nullable TKeyValue> aset = cloneViaSerialization(tset);
				
				assert aset != tset;
				assertEquals(aset, tset.toUnmodifiableJavaSet());
				assertEquals(aset.hashCode(), tset.hashCode());
				
				tset.clear(); // test decoupling
				
				assertNotEquals(aset, tset.toUnmodifiableJavaSet());
				assertNotEquals(aset.hashCode(), tset.hashCode());
				
				assertEquals(tset.size(), 0);
				assertEquals(aset.size(), 1);
				
				tset = aset;
			}
			
			tset.clear();
			assertNotEquals(set, tset.toUnmodifiableJavaSet());
			assertTrue(tset.add(key1_2.clone().withValue(456)));
			assertEquals(set, tset.toUnmodifiableJavaSet());
		}
		
		assertTrue(key1.fullyEquals(getSetElement(set, key1)));
		assertTrue(set.removeElement(key1_2));
		assertTrue(set.add(key1_2));
		{
			BSet<TKeyValue> tset = factory.create();
			assertNotEquals(set, tset.toUnmodifiableJavaSet());
			assertTrue(tset.add(key1));
			assertEquals(set, tset.toUnmodifiableJavaSet()); 
			
			tset.clear();
			assertNotEquals(set, tset);
			assertTrue(tset.add(key1.clone()));
			assertEquals(set, tset);
			
			tset.clear();
			assertNotEquals(set, tset);
			assertTrue(tset.add(key1.clone().withValue(456)));
			assertTrue(tset.remove(key1.clone()));
			assertTrue(tset.add(key1.clone().withValue(456)));
			assertEquals(set, tset);
		}
		assertEquals(set.size(), 1);
		assertFalse(set.has(nkey1));
		assertFalse(set.has(null));
		compareItems(set, key1_2);
		assertTrue(set.has(key1_2));
		assertTrue(set.has(key1_2.clone()));
		assertTrue(set.has(key1_2.clone().withValue(321)));
		assertTrue(nn(getSetElement(set, key1_2)).fullyEquals(key1_2));
		assertTrue(nn(getSetElement(set, key1_2.clone())).fullyEquals(key1_2));
		assertTrue(nn(getSetElement(set, key1_2.clone().withValue(321))).fullyEquals(key1_2));
		assertTrue(set.has(key1));
		assertTrue(set.has(key1.clone()));
		assertTrue(set.has(key1.clone().withValue(321)));
		assertTrue(nn(getSetElement(set, key1)).fullyEquals(key1_2));
		assertTrue(nn(getSetElement(set, key1.clone())).fullyEquals(key1_2));
		assertTrue(nn(getSetElement(set, key1.clone().withValue(321))).fullyEquals(key1_2));
		
		TKeyValue key2 = new TKeyValue("key2", 22);
		assertTrue(set.add(key2));
		assertEquals(set.size(), 2);
		assertFalse(set.has(nkey1));
		assertFalse(set.has(null));
		compareItems(set, key1_2, key2);
		
		for (int i = 0; i < 2; i++)
		{
			BSet<TKeyValue> tset = (i == 0) ? 
				factory.create(Arrays.asList(key1)) :
				factory.createFromReadOnly(ReadOnlySet.of(Set.of(key1)));
			
			assertNotEquals(set, tset);
			assertTrue(tset.add(key2));
			assertEquals(set, tset);
			
			tset.clear();
			assertNotEquals(set, tset);
			assertTrue(tset.add(key1_2));
			assertNotEquals(set, tset);
			assertTrue(tset.add(key2.clone()));
			assertEquals(set, tset);
			
			tset.clear();
			assertNotEquals(set, tset);
			assertTrue(tset.add(key1));
			assertNotEquals(set, tset);
			assertTrue(tset.add(key2.clone().withValue(678)));
			assertEquals(set, tset);
		}

		assertTrue(set.has(key1_2));
		assertTrue(set.has(key1_2.clone()));
		assertTrue(set.has(key1_2.clone().withValue(321)));
		assertTrue(nn(getSetElement(set, key1_2)).fullyEquals(key1_2));
		assertTrue(nn(getSetElement(set, key1_2.clone())).fullyEquals(key1_2));
		assertTrue(nn(getSetElement(set, key1_2.clone().withValue(321))).fullyEquals(key1_2));
		assertTrue(set.has(key1));
		assertTrue(set.has(key1.clone()));
		assertTrue(set.has(key1.clone().withValue(321)));
		assertTrue(nn(getSetElement(set, key1)).fullyEquals(key1_2));
		assertTrue(nn(getSetElement(set, key1.clone())).fullyEquals(key1_2));
		assertTrue(nn(getSetElement(set, key1.clone().withValue(321))).fullyEquals(key1_2));
		assertTrue(set.has(key2));
		assertTrue(set.has(key2.clone()));
		assertTrue(set.has(key2.clone().withValue(321)));
		assertTrue(nn(getSetElement(set, key2)).fullyEquals(key2));
		assertTrue(nn(getSetElement(set, key2.clone())).fullyEquals(key2));
		assertTrue(nn(getSetElement(set, key2.clone().withValue(321))).fullyEquals(key2));
		
		
		// check with nulls too
		assertTrue(set.add(null));
		assertEquals(set.size(), 3);
		assertFalse(set.has(nkey1));
		assertTrue(set.has(null));
		compareItems(set, key1_2, key2, null);
		compareItems(set, new ArrayList<@Nullable TKeyValue>(set.stream().map(e -> e).collect(Collectors.toList())).toArray(new @Nullable TKeyValue[0]));

		assertFalse(set.add(null));
		assertEquals(set.size(), 3);
		assertFalse(set.has(nkey1));
		assertTrue(set.has(null));
		compareItems(set, key1_2, key2, null);
		
		assertTrue(set.contains(null));
		assertTrue(set.has(null));
		assertTrue(set.has(key1_2));
		assertTrue(set.has(key1_2.clone()));
		assertTrue(set.has(key1_2.clone().withValue(321)));
		assertTrue(nn(getSetElement(set, key1_2)).fullyEquals(key1_2));
		assertTrue(nn(getSetElement(set, key1_2.clone())).fullyEquals(key1_2));
		assertTrue(nn(getSetElement(set, key1_2.clone().withValue(321))).fullyEquals(key1_2));
		assertTrue(set.has(key1));
		assertTrue(set.has(key1.clone()));
		assertTrue(set.has(key1.clone().withValue(321)));
		assertTrue(nn(getSetElement(set, key1)).fullyEquals(key1_2));
		assertTrue(nn(getSetElement(set, key1.clone())).fullyEquals(key1_2));
		assertTrue(nn(getSetElement(set, key1.clone().withValue(321))).fullyEquals(key1_2));
		assertTrue(set.has(key2));
		assertTrue(set.has(key2.clone()));
		assertTrue(set.has(key2.clone().withValue(321)));
		assertTrue(nn(getSetElement(set, key2)).fullyEquals(key2));
		assertTrue(nn(getSetElement(set, key2.clone())).fullyEquals(key2));
		assertTrue(nn(getSetElement(set, key2.clone().withValue(321))).fullyEquals(key2));
		
	}
	
	
	/**
	 * Clones given serializable object via serialization-deserialization pair.
	 */
	private static <O extends Serializable> O cloneViaSerialization(O src)
	{
		try
		{
			try (
				ByteArrayOutputStream os = new ByteArrayOutputStream(2048);
				
				ObjectOutputStream oos = new ObjectOutputStream(os);
			) {
			
				oos.writeObject(src);
				oos.flush();
				
				try (
					BAOSInputStream is = new BAOSInputStream(os);
					ObjectInputStream ois = new ObjectInputStream(is);
				) {
					@SuppressWarnings("unchecked") O result = (O)ois.readObject();
					
					return result;
				}
			}
		} catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Verifies that given set contains exactly the elements (via fullyEquals())
	 */
	private void compareItems(SerializableBSet<@Nullable TKeyValue> set, @Nullable TKeyValue... elements)
	{
		compareItems(set, false, elements);
		compareItems(set, true, elements);
	}
	
	/**
	 * Verifies that given set contains exactly the elements (via fullyEquals())
	 */
	@SuppressWarnings("deprecation")
	private void compareItems(SerializableBSet<@Nullable TKeyValue> set, boolean useRemove, @Nullable TKeyValue... elements)
	{
		assertTrue(set.containsAll(Arrays.asList(elements)));
		assertTrue(set.hasAll(Arrays.asList(elements)));
		assertTrue(Arrays.asList(elements).containsAll(set));
		
		{
			ArrayList<@Nullable TKeyValue> badList = new ArrayList<>(Arrays.asList(elements));
			badList.add(new TKeyValue("a123456", 456789));
			assertFalse(set.containsAll(badList));
			assertFalse(set.hasAll(badList));
		}
		
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
		
		RHashSet<@Nullable TKeyValue> remaining = RHashSet.create(set);
		
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
		
		// Compare stuff using standard equals/hashCode
//		System.out.println("" + set.getClass() + ": " + set);
		{
			Set<@Nullable TKeyValue> targetSet = new HashSet<>(Arrays.asList(elements));
			
			assertTrue(set.equals(targetSet), "" + set + " : " + targetSet);
			assertTrue(targetSet.equals(set), "" + set + " : " + targetSet);
			assertEquals(set.hashCode(), targetSet.hashCode(), "" + set + " : " + targetSet);
		}
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

	/**
	 * Factory class for creating instances of various subclasses of {@link BList}
	 * via static *.create* methods.
	 */
	@RequiredArgsConstructor
	private static class BListFactory
	{
		/**
		 * Factory class that contains create* methods.
		 */
		private final Class<?> factoryClass;
		
		/**
		 * Factory create method.
		 */
		public <S extends SerializableBList<E>, E> S create()
		{
			return TestUtil.invokeInaccessibleMethod(factoryClass, "create", null);
		}
		
		/**
		 * Factory create method.
		 */
		public <S extends SerializableBList<E>, E> S create(int initialCapacity)
		{
			return TestUtil.invokeInaccessibleMethod(factoryClass, "create", null, 
				int.class, initialCapacity);
		}
		
		/**
		 * Factory create method.
		 */
		public <S extends SerializableBList<E>, E> S create(@Nonnull Collection<? extends E> c)
		{
			return TestUtil.invokeInaccessibleMethod(factoryClass, "create", null, 
				Collection.class, c);
		}
		
		/**
		 * Factory create method.
		 */
		public <S extends SerializableBList<E>, E> S createFromReadOnly(@Nonnull ReadOnlyCollection<? extends E> c)
		{
			return TestUtil.invokeInaccessibleMethod(factoryClass, "createFromReadOnly", null, 
				ReadOnlyCollection.class, c);
		}
	}
	
	/**
	 * Verifies that given list contains exactly the elements (via fullyEquals())
	 */
	private void compareItems(SerializableBList<@Nullable TKeyValue> list, @Nullable TKeyValue... elements)
	{
		compareItems(list, false, elements);
		compareItems(list, true, elements);
	}
	
	/**
	 * Verifies that given list contains exactly the elements (via fullyEquals())
	 */
	@SuppressWarnings("deprecation")
	private void compareItems(SerializableBList<@Nullable TKeyValue> list, boolean useRemove, @Nullable TKeyValue... elements)
	{
		assertTrue(list.containsAll(Arrays.asList(elements)));
		assertTrue(list.hasAll(Arrays.asList(elements)));
		assertTrue(Arrays.asList(elements).containsAll(list));
		
		{
			ArrayList<@Nullable TKeyValue> badList = new ArrayList<>(Arrays.asList(elements));
			badList.add(new TKeyValue("a123456", 456789));
			assertFalse(list.containsAll(badList));
			assertFalse(list.hasAll(badList));
		}
		
		if (list.size() == 1)
		{
			TKeyValue actual = list.only();
			TKeyValue expected = elements[0];
			assertEquals(actual, expected);
			if (actual != null)
				assertTrue(actual.fullyEquals(expected));
		}
		else
			assertFails(() -> list.only(), "IllegalStateException");
		
		if (list.size() > 0)
		{
			TKeyValue actual = list.any();
			assertTrue(Arrays.asList(elements).contains(actual));
		}
		else
			assertFails(() -> list.any(), "IllegalStateException");
		
		{
			Iterator<@Nullable TKeyValue> iter = list.liveIterator();
			for (TKeyValue item : elements)
			{
				if (item == null)
					assertNull(iter.next());
				else
					assertTrue(item.fullyEquals(iter.next()));
			}
			assertFalse(iter.hasNext());
		}
		
		int count = 0;
		for (@SuppressWarnings("unused") TKeyValue item : list)
			count++;
		assertEquals(elements.length, count);
		
		BArrayList<@Nullable TKeyValue> remaining = BArrayList.create(list);
		
		for (int i = 0; i < elements.length; i++)
		{
			TKeyValue item = elements[i];
			TKeyValue listItem = remaining.get(i);
			
			assertTrue(remaining.has(item));
			assertTrue(remaining.contains(item));
			
			if (item != null)
				assertTrue(item.fullyEquals(listItem));
			else
				assertNull(listItem);
		}
		
		{
			int remCount = remaining.size();
			for (@Nullable TKeyValue item : elements)
			{
				remCount--;
				
				if (useRemove)
					assertTrue(remaining.remove(item));
				else
					assertTrue(remaining.removeElement(item));
				
				assertEquals(remaining.size(), remCount);
			}
		}
		
		assertEquals(0, remaining.size());
		
		// Compare stuff using standard equals/hashCode
//		System.out.println("" + list.getClass() + ": " + list);
		{
			List<@Nullable TKeyValue> targetList = Arrays.asList(elements);
			
			assertTrue(list.equals(targetList), "" + list + " : " + targetList);
			assertTrue(targetList.equals(list), "" + list + " : " + targetList);
			assertEquals(list.hashCode(), targetList.hashCode(), "" + list + " : " + targetList);
		}
	}

	/**
	 * Not all interfaces are declared as serializable, so this takes care of
	 * casting.
	 */
	private <S extends BList<@Nullable TKeyValue>> void internalTestBList(S list, BListFactory factory)
	{
		internalTestBList0(TypeUtil.coerce(list), factory);
	}

	/**
	 * Gets an element from list (via scanning an entire list).
	 * 
	 * @param startFrom if specified -- zero-based index where to start from
	 */
	@Nullable
	private <E> E getListElement(List<E> list, @Nonnull E element, Integer... startFrom)
	{
		assertLessOrEqual(startFrom.length, 1);
		int skip = 0;
		if (startFrom.length == 1)
			skip = startFrom[0];
		
		for (E item : list)
		{
			if (skip > 0)
			{
				skip--;
				continue;
			}
			
			if (element.equals(item))
				return item;
		}
		
		return null;
	}
	
	/**
	 * Tests {@link BList}
	 */
	@SuppressWarnings("deprecation")
	private <S extends SerializableBList<@Nullable TKeyValue>> void internalTestBList0(S list, BListFactory factory)
	{
		// NB: this test was initially copy-pasted from BSet test, so some stuff may look weird (and then it was extended/fixed for BList)
		TKeyValue nkey1 = new TKeyValue("nkey1", 1);
		
		TKeyValue key1 = new TKeyValue("key1", 1);
		
		assertEquals(list.size(), 0);
		assertTrue(list.isEmpty());
		assertNull(list.first());
		assertNull(list.last());
		assertFalse(list.has(key1));
		assertFalse(list.has(nkey1));
		assertFalse(list.has(null));
		compareItems(list);
		list.stream().map(e -> Integer.parseInt(nn(e).toString())).collect(Collectors.toList()); // should fail if there are any elements
		
		assertTrue(list.add(key1));
		assertEquals(list.size(), 1);
		assertFalse(list.isEmpty());
		assertEquals(list.first(), key1);
		assertEquals(list.last(), key1);
		assertFalse(list.has(nkey1));
		assertFalse(list.has(null));
		compareItems(list, key1);
		assertTrue(list.has(key1));
		assertTrue(list.has(key1.clone()));
		assertTrue(list.has(key1.clone().withValue(321)));
		assertTrue(nn(getListElement(list, key1)).fullyEquals(key1));
		assertTrue(nn(getListElement(list, key1.clone())).fullyEquals(key1));
		assertTrue(nn(getListElement(list, key1.clone().withValue(321))).fullyEquals(key1));
		assertFails( () ->
			{list.stream().map(e -> Integer.parseInt(nn(e).toString())).collect(Collectors.toList());}); // should fail if there are any elements
		compareItems(list, new ArrayList<@Nullable TKeyValue>(list.stream().map(e -> e).collect(Collectors.toList())).toArray(new @Nullable TKeyValue[0]));
		
		{
			BList<TKeyValue> tlist = factory.create(3);
			
			assertNotEquals(list, tlist);
			assertTrue(tlist.add(key1));
			assertEquals(list, tlist);
			assertTrue(tlist.add(key1.withValue(876)));
			assertEquals(tlist.first(), key1);
			assertEquals(tlist.last(), key1.withValue(876));
			assertNotEquals(list, tlist);
			
			tlist.clear();
			assertNull(tlist.first());
			assertNull(tlist.last());
			assertNotEquals(list, tlist);
			assertTrue(tlist.add(key1.clone()));
			assertEquals(list, tlist);
			
			tlist.clear();
			assertNotEquals(list, tlist);
			assertTrue(tlist.add(key1.clone().withValue(456)));
			assertEquals(list, tlist);
		}
		
		TKeyValue key1_2 = new TKeyValue("key1", 2);
		
		{
			SerializableBList<@Nullable TKeyValue> tlist = factory.create(5);
			
			assertNotEquals(list, tlist);
			assertTrue(tlist.add(key1_2)); 
			assertTrue(tlist.add(key1_2)); 
			assertTrue(tlist.add(key1));
			assertEquals(tlist.first(), key1_2);
			assertEquals(tlist.last(), key1);
			compareItems(tlist, key1_2, key1_2, key1);
			nnChecked(tlist.remove(1)).fullyEquals(key1_2);
			nnChecked(tlist.remove(1)).fullyEquals(key1);
			assertEquals(list, tlist.toUnmodifiableJavaList());
			
			assertTrue(tlist.removeElement(key1));
			compareItems(tlist);
			assertEquals(tlist.size(), 0);
			assertTrue(tlist.isEmpty());
			assertTrue(tlist.add(key1_2)); 
			assertTrue(tlist.remove(key1));
			compareItems(tlist);
			assertEquals(tlist.size(), 0);
			assertTrue(tlist.isEmpty());
			assertTrue(tlist.add(key1_2)); 
			assertTrue(tlist.add(nkey1)); 
			compareItems(tlist, key1_2, nkey1);
			assertEquals(tlist.size(), 2);
			assertFalse(tlist.isEmpty());
			assertTrue(tlist.removeElement(nkey1));
			compareItems(tlist, key1_2);
			assertEquals(tlist.size(), 1);
			assertFalse(tlist.isEmpty());
			assertTrue(tlist.add(nkey1)); 
			compareItems(tlist, key1_2, nkey1);
			assertEquals(tlist.size(), 2);
			assertFalse(tlist.isEmpty());
			assertTrue(tlist.remove(nkey1));
			compareItems(tlist, key1_2);
			assertEquals(tlist.size(), 1);
			assertFalse(tlist.isEmpty());
			
			assertTrue(tlist.add(null));
			compareItems(tlist, key1_2, null);
			assertTrue(tlist.add(null));
			compareItems(tlist, key1_2, null, null);
			assertTrue(tlist.remove(null));
			compareItems(tlist, key1_2, null);
			assertTrue(tlist.remove(null));
			compareItems(tlist, key1_2);
			assertTrue(tlist.add(null));
			compareItems(tlist, key1_2, null);
			assertTrue(tlist.remove(null));
			assertFalse(tlist.remove(null));
			compareItems(tlist, key1_2);
			
			assertTrue(tlist.add(null));
			compareItems(tlist, key1_2, null);
			assertTrue(tlist.add(null));
			compareItems(tlist, key1_2, null, null);
			assertTrue(tlist.removeElement(null));
			compareItems(tlist, key1_2, null);
			assertTrue(tlist.removeElement(null));
			compareItems(tlist, key1_2);
			assertTrue(tlist.add(null));
			compareItems(tlist, key1_2, null);
			assertTrue(tlist.removeElement(null));
			assertFalse(tlist.removeElement(null));
			compareItems(tlist, key1_2);
			
			tlist.clear();
			assertNotEquals(list, tlist);
			assertTrue(tlist.add(key1_2.clone()));
			assertEquals(list, tlist.toUnmodifiableJavaList());
			
			{
				SerializableBList<@Nullable TKeyValue> alist = cloneViaSerialization(tlist);
				
				assert alist != tlist;
				assertEquals(alist, tlist.toUnmodifiableJavaList());
				assertEquals(alist.hashCode(), tlist.hashCode());
				
				tlist.clear(); // test decoupling
				
				assertNotEquals(alist, tlist.toUnmodifiableJavaList());
				assertNotEquals(alist.hashCode(), tlist.hashCode());
				
				assertEquals(tlist.size(), 0);
				assertEquals(alist.size(), 1);
				
				tlist = alist;
			}
			
			tlist.clear();
			assertNotEquals(list, tlist.toUnmodifiableJavaList());
			assertTrue(tlist.add(key1_2.clone().withValue(456)));
			assertEquals(list, tlist.toUnmodifiableJavaList());
		}
		
		assertTrue(key1.fullyEquals(getListElement(list, key1)));
		assertTrue(list.removeElement(key1_2));
		assertTrue(list.add(key1_2));
		{
			BList<TKeyValue> tlist = factory.create();
			assertNotEquals(list, tlist.toUnmodifiableJavaList());
			assertTrue(tlist.add(key1));
			assertEquals(list, tlist.toUnmodifiableJavaList()); 
			
			tlist.clear();
			assertNotEquals(list, tlist);
			assertTrue(tlist.add(key1.clone()));
			assertEquals(list, tlist);
			
			tlist.clear();
			assertNotEquals(list, tlist);
			assertTrue(tlist.add(key1.clone().withValue(456)));
			assertTrue(tlist.remove(key1.clone()));
			assertTrue(tlist.add(key1.clone().withValue(456)));
			assertEquals(list, tlist);
		}
		assertEquals(list.size(), 1);
		assertFalse(list.has(nkey1));
		assertFalse(list.has(null));
		compareItems(list, key1_2);
		assertTrue(list.has(key1_2));
		assertTrue(list.has(key1_2.clone()));
		assertTrue(list.has(key1_2.clone().withValue(321)));
		assertTrue(nn(getListElement(list, key1_2)).fullyEquals(key1_2));
		assertTrue(nn(getListElement(list, key1_2.clone())).fullyEquals(key1_2));
		assertTrue(nn(getListElement(list, key1_2.clone().withValue(321))).fullyEquals(key1_2));
		assertTrue(list.has(key1));
		assertTrue(list.has(key1.clone()));
		assertTrue(list.has(key1.clone().withValue(321)));
		assertTrue(nn(getListElement(list, key1)).fullyEquals(key1_2));
		assertTrue(nn(getListElement(list, key1.clone())).fullyEquals(key1_2));
		assertTrue(nn(getListElement(list, key1.clone().withValue(321))).fullyEquals(key1_2));
		
		TKeyValue key2 = new TKeyValue("key2", 22);
		assertTrue(list.add(key2));
		assertEquals(list.size(), 2);
		assertFalse(list.has(nkey1));
		assertFalse(list.has(null));
		compareItems(list, key1_2, key2);
		
		for (int i = 0; i < 2; i++)
		{
			BList<TKeyValue> tlist = (i == 0) ? 
				factory.create(Arrays.asList(key1)) :
				factory.createFromReadOnly(ReadOnlyList.of(List.of(key1)));
			
			assertNotEquals(list, tlist);
			assertTrue(tlist.add(key2));
			assertEquals(list, tlist);
			
			tlist.clear();
			assertNotEquals(list, tlist);
			assertTrue(tlist.add(key1_2));
			assertNotEquals(list, tlist);
			assertTrue(tlist.add(key2.clone()));
			assertEquals(list, tlist);
			
			tlist.clear();
			assertNotEquals(list, tlist);
			assertTrue(tlist.add(key1));
			assertNotEquals(list, tlist);
			assertTrue(tlist.add(key2.clone().withValue(678)));
			assertEquals(list, tlist);
		}

		assertTrue(list.has(key1_2));
		assertTrue(list.has(key1_2.clone()));
		assertTrue(list.has(key1_2.clone().withValue(321)));
		assertTrue(nn(getListElement(list, key1_2)).fullyEquals(key1_2));
		assertTrue(nn(getListElement(list, key1_2.clone())).fullyEquals(key1_2));
		assertTrue(nn(getListElement(list, key1_2.clone().withValue(321))).fullyEquals(key1_2));
		assertTrue(list.has(key1));
		assertTrue(list.has(key1.clone()));
		assertTrue(list.has(key1.clone().withValue(321)));
		assertTrue(nn(getListElement(list, key1)).fullyEquals(key1_2));
		assertTrue(nn(getListElement(list, key1.clone())).fullyEquals(key1_2));
		assertTrue(nn(getListElement(list, key1.clone().withValue(321))).fullyEquals(key1_2));
		assertTrue(list.has(key2));
		assertTrue(list.has(key2.clone()));
		assertTrue(list.has(key2.clone().withValue(321)));
		assertTrue(nn(getListElement(list, key2)).fullyEquals(key2));
		assertTrue(nn(getListElement(list, key2.clone())).fullyEquals(key2));
		assertTrue(nn(getListElement(list, key2.clone().withValue(321))).fullyEquals(key2));
		
		// list contains: key1_2, key2
		compareItems(list, key1_2, key2);
		
		// check with nulls too
		assertTrue(list.add(null));
		assertEquals(list.size(), 3);
		assertFalse(list.has(nkey1));
		assertTrue(list.has(null));
		compareItems(list, key1_2, key2, null);
		compareItems(list, new ArrayList<@Nullable TKeyValue>(list.stream().map(e -> e).collect(Collectors.toList())).toArray(new @Nullable TKeyValue[0]));

		assertTrue(list.add(null));
		assertEquals(list.size(), 4);
		compareItems(list, key1_2, key2, null, null);
		assertNull(list.remove(3));
		assertEquals(list.size(), 3);
		assertFalse(list.has(nkey1));
		assertTrue(list.has(null));
		compareItems(list, key1_2, key2, null);
		
		assertTrue(list.contains(null));
		assertTrue(list.has(null));
		assertTrue(list.has(key1_2));
		assertTrue(list.has(key1_2.clone()));
		assertTrue(list.has(key1_2.clone().withValue(321)));
		assertTrue(nn(getListElement(list, key1_2)).fullyEquals(key1_2));
		assertTrue(nn(getListElement(list, key1_2.clone())).fullyEquals(key1_2));
		assertTrue(nn(getListElement(list, key1_2.clone().withValue(321))).fullyEquals(key1_2));
		assertTrue(list.has(key1));
		assertTrue(list.has(key1.clone()));
		assertTrue(list.has(key1.clone().withValue(321)));
		assertTrue(nn(getListElement(list, key1)).fullyEquals(key1_2));
		assertTrue(nn(getListElement(list, key1.clone())).fullyEquals(key1_2));
		assertTrue(nn(getListElement(list, key1.clone().withValue(321))).fullyEquals(key1_2));
		assertTrue(list.has(key2));
		assertTrue(list.has(key2.clone()));
		assertTrue(list.has(key2.clone().withValue(321)));
		assertTrue(nn(getListElement(list, key2)).fullyEquals(key2));
		assertTrue(nn(getListElement(list, key2.clone())).fullyEquals(key2));
		assertTrue(nn(getListElement(list, key2.clone().withValue(321))).fullyEquals(key2));
		
		// list contains: key1_2, key2, null
		compareItems(list, key1_2, key2, null);
		
		///////////////////////////////////////////////////////////////////////
		//   CHECK ACTUAL LIST OPERATIONS
		///////////////////////////////////////////////////////////////////////
		// Add more duplicate elements
		assertTrue(list.add(key1_2));
		list.add(3, key2); 
		list.add(3, null);
		compareItems(list, key1_2, key2, null, null, key2, key1_2);
		list.add(0, nkey1);
		compareItems(list, nkey1, key1_2, key2, null, null, key2, key1_2);
		assertEquals(list.size(), 7);
		list.add(7, nkey1);
		compareItems(list, nkey1, key1_2, key2, null, null, key2, key1_2, nkey1);
		assertEquals(list.size(), 8);
		assertTrue(nkey1.fullyEquals(list.remove(7)));
		compareItems(list, nkey1, key1_2, key2, null, null, key2, key1_2);
		assertEquals(list.size(), 7);
		assertTrue(nkey1.fullyEquals(list.remove(0)));
		compareItems(list, key1_2, key2, null, null, key2, key1_2);
		assertEquals(list.size(), 6);
		
		// addAll
		assertTrue(list.addAll(Arrays.asList(nkey1, nkey1)));
		compareItems(list, key1_2, key2, null, null, key2, key1_2, nkey1, nkey1);
		assertEquals(list.size(), 8);
		assertTrue(nkey1.fullyEquals(list.remove(6)));
		assertTrue(nkey1.fullyEquals(list.remove(6)));
		compareItems(list, key1_2, key2, null, null, key2, key1_2);
		assertEquals(list.size(), 6);
		
		assertTrue(list.addAll(0, Arrays.asList(nkey1, nkey1)));
		compareItems(list, nkey1, nkey1, key1_2, key2, null, null, key2, key1_2);
		assertEquals(list.size(), 8);
		assertTrue(nkey1.fullyEquals(list.remove(0)));
		assertTrue(nkey1.fullyEquals(list.remove(0)));
		compareItems(list, key1_2, key2, null, null, key2, key1_2);
		assertEquals(list.size(), 6);
		
		// get by index
		assertEquals(list.get(0), key1_2);
		assertEquals(list.get(1), key2);
		assertEquals(list.get(2), null);
		assertEquals(list.get(3), null);
		assertEquals(list.get(4), key2);
		assertEquals(list.get(5), key1_2);
		
		// indexof
		assertEquals(list.indexOf(nkey1), -1);
		assertEquals(list.indexOf(key1_2), 0);
		assertEquals(list.indexOf(key2), 1);
		assertEquals(list.indexOf(null), 2);
		assertEquals(list.lastIndexOf(nkey1), -1);
		assertEquals(list.lastIndexOf(key1_2), 5);
		assertEquals(list.lastIndexOf(key2), 4);
		assertEquals(list.lastIndexOf(null), 3);
		assertEquals(list.indexOfElement(nkey1), -1);
		assertEquals(list.indexOfElement(key1_2), 0);
		assertEquals(list.indexOfElement(key2), 1);
		assertEquals(list.indexOfElement(null), 2);
		assertEquals(list.lastIndexOfElement(nkey1), -1);
		assertEquals(list.lastIndexOfElement(key1_2), 5);
		assertEquals(list.lastIndexOfElement(key2), 4);
		assertEquals(list.lastIndexOfElement(null), 3);
		
		// sublists
		assertEquals(list.subList(0, 0).size(), 0);
		assertFalse(list.subList(0, 0).has(key2));
		assertEquals(list.subList(6, 6).size(), 0);
		assertFalse(list.subList(6, 6).has(null));
		{
			SerializableBList<@Nullable TKeyValue> sublist = TypeUtil.coerce(list.subList(1, 5)); // cheat since we know the type
			compareItems(sublist, key2, null, null, key2);
			assertEquals(sublist.size(), 4);
			
			assertEquals(sublist.indexOf(nkey1), -1);
			assertEquals(sublist.indexOf(key1_2), -1);
			assertEquals(sublist.indexOf(key2), 0);
			assertEquals(sublist.indexOf(null), 1);
			assertEquals(sublist.lastIndexOf(nkey1), -1);
			assertEquals(sublist.lastIndexOf(key1_2), -1);
			assertEquals(sublist.lastIndexOf(key2), 3);
			assertEquals(sublist.lastIndexOf(null), 2);
			assertEquals(sublist.indexOfElement(nkey1), -1);
			assertEquals(sublist.indexOfElement(key1_2), -1);
			assertEquals(sublist.indexOfElement(key2), 0);
			assertEquals(sublist.indexOfElement(null), 1);
			assertEquals(sublist.lastIndexOfElement(nkey1), -1);
			assertEquals(sublist.lastIndexOfElement(key1_2), -1);
			assertEquals(sublist.lastIndexOfElement(key2), 3);
			assertEquals(sublist.lastIndexOfElement(null), 2);
			
			sublist.add(nkey1);
			compareItems(sublist, key2, null, null, key2, nkey1);
			assertEquals(sublist.size(), 5);
			compareItems(list, key1_2, key2, null, null, key2, nkey1, key1_2);
			assertEquals(list.size(), 7);
			
			sublist.add(0, nkey1);
			compareItems(sublist, nkey1, key2, null, null, key2, nkey1);
			assertEquals(sublist.size(), 6);
			compareItems(list, key1_2, nkey1, key2, null, null, key2, nkey1, key1_2);
			assertEquals(list.size(), 8);
			
			sublist.remove(0);
			assertEquals(sublist.size(), 5);
			compareItems(list, key1_2, key2, null, null, key2, nkey1, key1_2);
			assertEquals(list.size(), 7);
			
			sublist.remove(4);
			assertEquals(sublist.size(), 4);
			compareItems(list, key1_2, key2, null, null, key2, key1_2);
			assertEquals(list.size(), 6);
		}
	}
	
	/**
	 * Tests for various flavors of {@link BList}
	 */
	@Test
	public void testBList()
	{
		// NB: this test was initially copy-pasted from BSet test, so some stuff may look weird (and then it was extended/fixed for BList)
		
		internalTestBList(BArrayList.create(), new BListFactory(BArrayList.class));
		internalTestBList(BArrayList.create(3), new BListFactory(BArrayList.class));
		internalTestBList(BArrayList.create(Collections.emptyList()), new BListFactory(BArrayList.class));
		
		internalTestBList(new BArrayList<>(), new BListFactory(BArrayList.class));
		internalTestBList(new BArrayList<>(3), new BListFactory(BArrayList.class));
		internalTestBList(new BArrayList<>(Collections.emptyList()), new BListFactory(BArrayList.class));
		
		
		internalTestBList(BList.of(new ArrayList<>()), new BListFactory(BArrayList.class));
		internalTestBList(BList.of(new ArrayList<>(3)), new BListFactory(BArrayList.class));
		internalTestBList(BList.of(new ArrayList<>(Collections.emptyList())), new BListFactory(BArrayList.class));
		
		internalTestBList(SerializableBList.of(new ArrayList<>()), new BListFactory(BArrayList.class));
		internalTestBList(SerializableBList.of(new ArrayList<>(3)), new BListFactory(BArrayList.class));
		internalTestBList(SerializableBList.of(new ArrayList<>(Collections.emptyList())), new BListFactory(BArrayList.class));
		
		internalTestBList(BList.of(new LinkedList<>()), new BListFactory(BArrayList.class));
		internalTestBList(BList.of(new LinkedList<>(Collections.emptyList())), new BListFactory(BArrayList.class));
		
		
		// Cheat since we know the underlying impl
		internalTestBList(TypeUtil.coerce(ReadOnlyList.of(new ArrayList<>())), new BListFactory(BArrayList.class));
		internalTestBList(TypeUtil.coerce(ReadOnlyList.of(new ArrayList<>(3))), new BListFactory(BArrayList.class));
		internalTestBList(TypeUtil.coerce(ReadOnlyList.of(new ArrayList<>(Collections.emptyList()))), new BListFactory(BArrayList.class));
		
		assertNull(ReadOnlyList.toNullableUnmodifiableJavaList(null));
		{
			List<Integer> rs = nnChecked(ReadOnlyList.toNullableUnmodifiableJavaList(
				ReadOnlyList.of(List.of(1, 2, 1))
			));
			assertEquals(rs.size(), 3);
			assertTrue(rs.contains(1));
			assertTrue(rs.contains(2));
			assertFails(() -> rs.add(3));
		}
	}
}
