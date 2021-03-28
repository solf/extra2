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
package com.github.solf.extra2.collection;

import static com.github.solf.extra2.util.NullUtil.nn;
import static com.github.solf.extra2.util.NullUtil.nonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.solf.extra2.util.TypeUtil;

/**
 * Helpful stuff for collections.
 * 
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class WACollections
{
	/**
	 * Method for accessing HashMap's getEntry in 'old' JDKs.
	 */
	@Nullable
	private static final Method hashMapGetEntryMethod;
	
	/**
	 * Method for accessing HashMap's getNode in 'new' JDKs.
	 */
	@Nullable
	private static final Method hashMapGetNodeMethod;
	
	/**
	 * Method for accessing HashMap's hash in 'new' JDKs.
	 */
	@Nullable
	private static final Method hashMapHashMethod;
	
	/**
	 * Method for accessing TreeMap's getEntry method
	 */
	private static final Method treeMapGetEntryMethod;
	
	/**
	 * Field for accessing {@link HashSet} backing map field.
	 */
	private static final Field hashSetMapField;
	
	/**
	 * Field for accessing {@link TreeSet} backing map field.
	 */
	private static final Field treeSetMapField;
	
	static
	{
		Method tmpHashMapGetEntryMethod = null;
		Method tmpHashMapGetNodeMethod = null;
		Method tmpHashMapHashMethod = null;
		try
		{
			Method method = HashMap.class.getDeclaredMethod("getEntry", Object.class);
			method.setAccessible(true);
			tmpHashMapGetEntryMethod = method;
		} catch (Exception e)
		{
			// Try 'new' JDK format.
			try
			{
				tmpHashMapGetEntryMethod = null;
				{
					Method method = HashMap.class.getDeclaredMethod("getNode", int.class, Object.class);
					method.setAccessible(true);
					tmpHashMapGetNodeMethod = method;
				}
				{
					Method method = HashMap.class.getDeclaredMethod("hash", Object.class);
					method.setAccessible(true);
					tmpHashMapHashMethod = method;
				}
			} catch (Exception e1)
			{
				throw new IllegalStateException("Failed to obtain / make accessible HashMap.getNode or HashMap.hash method (after HashMap.getEntry already failed): " + e1, e1);
			}
		}
		
		hashMapGetEntryMethod = tmpHashMapGetEntryMethod;
		hashMapGetNodeMethod = tmpHashMapGetNodeMethod;
		hashMapHashMethod = tmpHashMapHashMethod;
		
		try
		{
			Method method;
			Field field;
			
			method = TreeMap.class.getDeclaredMethod("getEntry", Object.class);
			method.setAccessible(true);
			treeMapGetEntryMethod = method;
			
			field = HashSet.class.getDeclaredField("map");
			field.setAccessible(true);
			hashSetMapField = field;
			
			field = TreeSet.class.getDeclaredField("m");
			field.setAccessible(true);
			treeSetMapField = field;
		} catch (Exception e)
		{
			throw new IllegalStateException("Failed to obtain required accessor methods: " + e, e);
		}
	}
	
	
	/**
	 * Implementation of Iterable over Enumeration.
	 * http://www.javaspecialists.eu/archive/Issue107.html 
	 */
	public static class IterableEnumeration<T> implements Iterable<T>
	{
		/**
		 * Underlying enumeration.
		 */
		private final Enumeration<T> en;

		/**
		 * Constructor.
		 */
		public IterableEnumeration(Enumeration<T> en)
		{
			this.en = en;
		}

		/**
		 * Create & return adapter.
		 */
		@Override
		public Iterator<T> iterator()
		{
			return new Iterator<T>()
			{
				@Override
				public boolean hasNext()
				{
					return en.hasMoreElements();
				}

				@Override
				public T next()
				{
					return nonNull(en.nextElement());
				}

				@Override
				public void remove()
				{
					throw new UnsupportedOperationException();
				}
			};
		}
	}
	
	/**
	 * Implementation of {@link Iterator} for {@link ResultSet}
	 * <p>
	 * Each element is the same {@link ResultSet} but it is positioned to a new
	 * row. 
	 */
	public static class ResultSetIterator implements Iterator<ResultSet>
	{
		/**
		 * Underlying result set.
		 */
		private final ResultSet rs;
		
		/**
		 * Whether iteration has finished.
		 */
		private final AtomicBoolean finished = new AtomicBoolean(false);
		
		/**
		 * Whether next element is 'loaded' (that is next() has already been
		 * called and returned positive value).
		 */
		private final AtomicBoolean nextLoaded = new AtomicBoolean(false);

		/**
		 * Constructor.
		 */
		public ResultSetIterator(ResultSet rs)
		{
			this.rs = rs;
		}

		@Override
		public boolean hasNext()
		{
			try
			{
				if (finished.get())
					return false;
				
				if (nextLoaded.get())
					return true;
				
				if (rs.next())
				{
					nextLoaded.set(true);
					return true;
				}
				else
				{
					finished.set(true);
					return false;
				}
			} catch (SQLException e)
			{
				throw new IllegalStateException("SQL exception during iteration: " + e);
			}
		}

		@Override
		public ResultSet next() throws NoSuchElementException, IllegalStateException
		{
			try
			{
				if (finished.get())
					throw new NoSuchElementException();
				
				if (nextLoaded.get())
				{
					nextLoaded.set(false);
					return rs;
				}
				
				if (rs.next())
					return rs;
				else
				{
					finished.set(true);
					throw new NoSuchElementException();
				}
			} catch (SQLException e)
			{
				throw new IllegalStateException("SQL exception during iteration: " + e);
			}
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException();
		}
	}
	
	
	/**
	 * Iterator for {@link Supplier} -- returns results as long as {@link Supplier}
	 * provides non-null results; terminates upon {@link Supplier} returning null.
	 * <p>
	 * NOT thread-safe.
	 *
	 * @author Sergey Olefir
	 */
	@ParametersAreNonnullByDefault
	public static class SupplierIterator<T> implements Iterator<@Nonnull T> 
	{
		/**
		 * Underlying supplier
		 */
		private final Supplier<@Nullable T> supplier;
		
		/**
		 * Set to true when there are no more elements.
		 */
		private boolean noMoreElements = false;
		
		/**
		 * Previously-read next element (must be returned next) or null if there 
		 * is no previously-read element.
		 */
		@Nullable
		private T next;
		
		/**
		 * Constructor.
		 */
		public SupplierIterator(Supplier<@Nullable T> supplier)
		{
			this.supplier = supplier;
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#hasNext()
		 */
		@Override
		public boolean hasNext()
		{
			if (noMoreElements)
				return false;
			
			if (next != null)
				return true;
			
			next = supplier.get();
			if (next == null)
			{
				noMoreElements = true;
				return false;
			}
			
			return true;
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#next()
		 */
		@Override
		public @Nonnull T next() throws NoSuchElementException
		{
			if (noMoreElements)
				throw new NoSuchElementException();
			
			@Nullable T result = next;
			next = null;
			
			if (result == null)
			{
				result = supplier.get();
				if (result == null)
				{
					noMoreElements = true;
					throw new NoSuchElementException();
				}
			}
			
			return result;
		}
	}

	/**
	 * Convenient way to treat {@link Enumeration} as {@link Iterable}
	 * Note that it only allows a single use.
	 */
	public static <T> Iterable<T> toIterable(Enumeration<T> en)
	{
		return new IterableEnumeration<T>(en);
	}
	
	/**
	 * Convenient way to treat {@link Iterator} as {@link Iterable}
	 * Note that it only allows a single use.
	 * 
	 * https://github.com/google/guava/issues/796
	 */
	public static <T> Iterable<T> toIterable(Iterator<T> it)
	{
		return new Iterable<T>()
		{
			private AtomicBoolean exhausted = new AtomicBoolean(false);

			@Override
			public Iterator<T> iterator()
			{
				if( exhausted.getAndSet(true) )
					throw new IllegalStateException(
						"Underlying iterator has been already consumed once!");
				return it;
			}
		};
	}
	
	/**
	 * Convenient way to treat {@link ResultSet} as {@link Iterable}.
	 * <p>
	 * Note that it only allows a single use.
	 */
	public static Iterable<ResultSet> toIterable(ResultSet rs)
	{
		return toIterable(new ResultSetIterator(rs));
	}
	
	/**
	 * Convenient way to treat {@link Supplier} as {@link Iterable} -- it'll
	 * produce next elements as long as {@link Supplier} returns non-null values;
	 * it stops (no more elements) once {@link Supplier} returns null even once
	 * <p>
	 * Note that it only allows a single use.
	 */
	@ParametersAreNonnullByDefault({})
	public static <T> @Nonnull Iterable<@Nonnull T> toIterable(@Nonnull Supplier<@Nullable T> supplier)
	{
		return toIterable(new SupplierIterator<T>(supplier));
	}

	/**
	 * A way to get entry (so you can access the key) from the HashMap -- specifically
	 * HashMap as it uses internal HashMap method.
	 * 
	 * USES REFLECTION!!!
	 * 
	 * @throws IllegalStateException if reflection fails for some reasons (shouldn't really happen)
	 */
	@Nullable
	public static <K, V> Entry<K, V> getEntry(HashMap<K, V> map, K key) throws IllegalStateException
	{
		try
		{
			if (hashMapGetEntryMethod != null)
			{
				return TypeUtil.coerceNullable(hashMapGetEntryMethod.invoke(map, key));
			}
			else
			{
				int hash = TypeUtil.coerce(nn(nn(hashMapHashMethod).invoke(map, key)));
				return TypeUtil.coerceNullable(nn(hashMapGetNodeMethod).invoke(map, hash, key));
			}
		} catch (Exception e)
		{
			throw new IllegalStateException("Failed to retrieve entry from HashMap: " + e, e);
		}
	}

	/**
	 * A way to get entry (so you can access the key) from the TreeMap -- specifically
	 * TreeMap as it uses internal TreeMap method.
	 * 
	 * USES REFLECTION!!!
	 * 
	 * @throws IllegalStateException if reflection fails for some reasons (shouldn't really happen)
	 */
	@Nullable
	public static <K, V> Entry<K, V> getEntry(TreeMap<K, V> map, K key) throws IllegalStateException
	{
		try
		{
			return TypeUtil.coerceNullable(treeMapGetEntryMethod.invoke(map, key));
		} catch (Exception e)
		{
			throw new IllegalStateException("Failed to retrieve entry from TreeMap: " + e, e);
		}
	}
	
	/**
	 * Gets set item from the set by 'itself'. Relevant in case stuff in Set has
	 * interesting fields that are not part of equals/hashCode.
	 * 
	 * This works specifically with {@link HashSet}
	 * 
	 * USES REFLECTION!!!
	 * 
	 * @throws IllegalStateException if reflection fails for some reasons (shouldn't really happen)
	 */
	@Nullable
	public static <E> E getSetItem(HashSet<E> set, E item) throws IllegalStateException
	{
		try
		{
			HashMap<E, Object> backingMap = TypeUtil.coerceForceNonnull(hashSetMapField.get(set));
			Entry<E, @Nonnull Object> entry = getEntry(backingMap, item);
			if (entry == null)
				return null;
			
			return entry.getKey();
		} catch (Exception e)
		{
			throw new IllegalStateException("Failed to invoke HashMap.getEntry method: " + e, e);
		}
	}
	
	/**
	 * Gets set item from the set by 'itself'. Relevant in case stuff in Set has
	 * interesting fields that are not part of equals/hashCode.
	 * 
	 * This works specifically with {@link TreeSet}
	 * 
	 * USES REFLECTION!!!
	 * 
	 * @throws IllegalStateException if reflection fails for some reasons (shouldn't really happen)
	 */
	@Nullable
	public static <E> E getSetItem(TreeSet<E> set, E item) throws IllegalStateException
	{
		try
		{
			Object obj = nn(treeSetMapField.get(set));
			if (!(obj instanceof TreeMap))
				throw new IllegalStateException("TreeSet backing map isn't TreeMap -- can't retrieve set item: " + obj.getClass() + ": " + set);
			@Nonnull TreeMap<E, Object> backingMap = TypeUtil.coerceForceNonnull(obj);
			Entry<E, @Nonnull Object> entry = getEntry(backingMap, item);
			if (entry == null)
				return null;
			
			return entry.getKey();
		} catch (Exception e)
		{
			throw new IllegalStateException("Failed to invoke TreeMap.getEntry method: " + e, e);
		}
	}
}
