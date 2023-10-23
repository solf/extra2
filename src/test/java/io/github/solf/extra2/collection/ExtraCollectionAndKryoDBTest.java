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

import static io.github.solf.extra2.util.NullUtil.nnChecked;
import static org.testng.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.testng.annotations.Test;

import io.github.solf.extra2.kryo.KryoDB;
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
	 * Basic tests for extra Collections extensions for Map.
	 */
	@Test
	public void testCollections()
		throws IOException, ClassNotFoundException
	{
		{
			HashMapExt<String, String> m = new HashMapExt<String, String>();
			m.setFactory(e -> new String(e));
		}
		
		testExtendedMap(new HashMapExt<String, String>(), new HashMapExt<String, String>(e -> new String(e)), m -> m.clone(), true);
		testExtendedMap(new TreeMapExt<String, String>(), new TreeMapExt<String, String>(e -> new String(e)), m -> m.clone(), false);
		
		testExtendedSet(new HashSetExt<String>(), (m) -> {return m.clone();}, true);
		testExtendedSet(new TreeSetExt<String>(), (m) -> {return m.clone();}, false);
	}
	
	/**
	 * Basic tests for extra Collections extensions for Maps.
	 * 
	 * @param compressed whether Kryo (de)serialization should compress data
	 */
	private <T extends MapExt<String, String>> void testExtendedMap(T map, T mapWithFactory, Function<T, Object> cloneFunction, boolean compressed)
		throws IOException, ClassNotFoundException
	{
		final String key1 = "12345";
		final String key2;
		{
			StringBuilder sb = new StringBuilder();
			for (int i = 1; i <=5; i++)
				sb.append(i);
			key2 = sb.toString();
		}
		
		assert key1 != key2;
		
		final String value = "whatever";
		map.put(key1, value);
		assert map.containsKey(key2);
		
		for (@Nonnull String key : map.keySet())
		{
			assert key == key1;
			assert key != key2;
		}
		
		final String otherKey = "otherKey";
		final String otherValue = "otherValue";
		
		// Now actually test WACollections
		{
			Entry<@Nonnull String, @Nonnull String> entry = map.getEntry(key2);
			assert entry != null;
			assert entry.getKey() == key1;
			assert entry.getKey() != key2;
		}
		{
			// Test get with factory
			assert map.get(otherKey) == null;
			try
			{
				map.getOrCreate(otherKey);
				assert false;
			} catch (IllegalStateException e)
			{
				// expected
			}
			assert map.get(otherKey, () -> otherValue) == otherValue;
			assert map.get(otherKey) == otherValue;
			assert map.remove(otherKey) == otherValue;
		}
		{
			// Test getOrCreate
			assert mapWithFactory.get(otherKey) == null;
			assert mapWithFactory.getOrCreate(otherKey).equals(otherKey);
			assertEquals(mapWithFactory.get(otherKey), otherKey);
			assertEquals(mapWithFactory.remove(otherKey), otherKey);
		}
		
		// Test clone
		{
			T map2 = TypeUtil.coerce(cloneFunction.apply(map));
			assert map2.size() == 1;
			assert map2.containsKey(key1);
			assert map2.containsKey(key2);
			assert value.equals(map2.get(key1));
			Entry<@Nonnull String, @Nonnull String> entry = map2.getEntry(key2);
			assert entry != null;
			assert key1.equals(entry.getKey());
			assert entry.getKey() == key1;
			assert entry.getKey() != key2;
		}
		
		// Test (de)serialization
		{
			T map2 = reserialize(map);
			assert map2.size() == 1;
			assert map2.containsKey(key1);
			assert map2.containsKey(key2);
			assert value.equals(map2.get(key1));
			Entry<@Nonnull String, @Nonnull String> entry = map2.getEntry(key2);
			assert entry != null;
			assert key1.equals(entry.getKey());
			assert entry.getKey() != key1;
			assert entry.getKey() != key2;
		}
		
		// Test Kryo (de)serialization
		{
			T map2 = kryoReserialize(map, compressed);
			assert map2.size() == 1;
			assert map2.containsKey(key1);
			assert map2.containsKey(key2);
			assert value.equals(map2.get(key1));
			Entry<@Nonnull String, @Nonnull String> entry = map2.getEntry(key2);
			assert entry != null;
			assert key1.equals(entry.getKey());
			assert entry.getKey() != key1;
			assert entry.getKey() != key2;
		}
	}
	
	/**
	 * Basic tests for extra Collections extensions for Sets.
	 * 
	 * @param compressed whether Kryo (de)serialization should compress data
	 */
	private <T extends SetExt<String>> void testExtendedSet(T set, Function<T, Object> cloneFunction, boolean compressed)
		throws IOException, ClassNotFoundException
	{
		final String key1 = "12345";
		final String key2;
		{
			StringBuilder sb = new StringBuilder();
			for (int i = 1; i <=5; i++)
				sb.append(i);
			key2 = sb.toString();
		}
		
		assert key1 != key2;
		
		assert set.add(key1) == true;
		assert set.add(key2) == false;
		assert set.contains(key1);
		assert set.contains(key2);
		
		for (@Nonnull String item : set)
		{
			assert item == key1;
			assert item != key2;
		}
		
		// Now actually test WACollections
		{
			String item = set.get(key2);
			assert item != null;
			assert item == key1;
			assert item != key2;
		}
		
		// Test clone
		{
			T set2 = TypeUtil.coerce(cloneFunction.apply(set));
			assert set2.size() == 1;
			assert set2.contains(key1);
			assert set2.contains(key2);
			String item = set2.get(key2);
			assert item != null;
			assert key1.equals(item);
			assert item == key1;
			assert item != key2;
		}
		
		// Test (de)serialization
		{
			T set2 = reserialize(set);
			assert set2.size() == 1;
			assert set2.contains(key1);
			assert set2.contains(key2);
			String item = set2.get(key2);
			assert item != null;
			assert key1.equals(item);
			assert item != key1;
			assert item != key2;
		}
		
		// Test Kryo (de)serialization
		{
			T set2 = kryoReserialize(set, compressed);
			assert set2.size() == 1;
			assert set2.contains(key1);
			assert set2.contains(key2);
			String item = set2.get(key2);
			assert item != null;
			assert key1.equals(item);
			assert item != key1;
			assert item != key2;
		}
	}
	
	/**
	 * Serialize object and deserialize it back.
	 */
	private <T> T reserialize(T src) throws IOException, ClassNotFoundException
	{
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(baos);
		)
		{
			out.writeObject(src);
			out.flush();
			byte[] bytes = baos.toByteArray();
			try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
				ObjectInputStream in = new ObjectInputStream(bais);
			)
			{
				return TypeUtil.coerce(nnChecked(in.readObject()));
			}
		}
	}
	
	/**
	 * Serialize object and deserialize it back.
	 */
	private <T> T kryoReserialize(T src, boolean compressed)
	{
		final int kryoDataVersion = 1;
		final String kryoDataFile = "colKryoData";
		
		// Save with wrong compression flag.
		{
			KryoDB<T> db = new KryoDB<T>(!compressed, kryoDataFile);
			db.saveData(src, kryoDataVersion);
		}
		
		// Loading with mismatched compression flag expected to fail.
		{
			KryoDB<T> db = new KryoDB<T>(compressed, kryoDataFile);
			try
			{
				@SuppressWarnings("unused")
				T data = db.loadData(kryoDataVersion);
				assert false : "May not reach this line!";
			} catch (Exception e)
			{
				// that's expected.
			}
		}
		
		// Save with correct compression flag.
		{
			KryoDB<T> db = new KryoDB<T>(compressed, kryoDataFile);
			db.saveData(src, kryoDataVersion);
		}
		
		// Loading with mismatched compression flag expected to fail.
		{
			KryoDB<T> db = new KryoDB<T>(!compressed, kryoDataFile);
			try
			{
				@SuppressWarnings("unused")
				T data = db.loadData(kryoDataVersion);
				assert false : "May not reach this line!";
			} catch (Exception e)
			{
				// that's expected.
			}
		}
		
		// Load data correctly and return.
		KryoDB<T> db = new KryoDB<T>(compressed, kryoDataFile);
		return db.loadData(kryoDataVersion);
	}
	
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
			testItemSamplerContents(is, new @Nullable Integer[] {null, 11, 19, 31, 39, 49, 61, 75, 83, 99});
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
			testItemSamplerContents(is, new @Nullable Integer[] {null, 75, 225, null, 403, 489, null, null, 873, 999});
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
	@NonNullByDefault({})
	private void testItemSamplerContents(@Nonnull ItemSampler<Integer> is, Integer @Nonnull... expectedItems)
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
