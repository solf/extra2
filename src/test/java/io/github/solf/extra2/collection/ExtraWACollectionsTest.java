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
		public <S extends SerializableBSet<E>, E> S createFromReadOnly(@Nonnull ReadOnlySet<? extends E> c)
		{
			return TestUtil.invokeInaccessibleMethod(factoryClass, "createFromReadOnly", null, 
				ReadOnlySet.class, c);
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
}
