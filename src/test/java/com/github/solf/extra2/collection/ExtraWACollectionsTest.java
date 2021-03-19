/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.collection;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.testng.annotations.Test;

import com.github.solf.extra2.collection.WACollections;

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
			Iterator<Integer> iter = WACollections.toIterable(supplier).iterator();
			
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
			
			Iterator<Integer> iterator = WACollections.toIterable(supplier).iterator();
			
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
			
			Iterator<Integer> iterator = WACollections.toIterable(supplier).iterator();
			
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
			
			Iterator<Integer> iterator = WACollections.toIterable(supplier).iterator();
			
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
			
			Iterator<Integer> iterator = WACollections.toIterable(supplier).iterator();
			
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
			
			Iterator<Integer> iterator = WACollections.toIterable(supplier).iterator();
			
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
			
			Iterator<Integer> iterator = WACollections.toIterable(supplier).iterator();
			
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
}
