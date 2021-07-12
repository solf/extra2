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
package io.github.solf.extra2.storage;

import static io.github.solf.extra2.util.NullUtil.fakeNonNull;
import static io.github.solf.extra2.util.NullUtil.nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.testng.annotations.Test;

import io.github.solf.extra2.storage.ExistMode;
import io.github.solf.extra2.storage.StorageItem;
import io.github.solf.extra2.storage.StorageManager;
import io.github.solf.extra2.storage.exception.IOStorageException;
import io.github.solf.extra2.storage.exception.MismatchedExistModeException;
import io.github.solf.extra2.storage.exception.MissingDataStorageException;
import io.github.solf.extra2.util.ASCIIUtil;

/**
 * Test for {@link StorageManager}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class ExtraStorageTest
{
	/**
	 * Directory used for testing.
	 */
	private static final String TEST_DIRECTORY = "target/storage-test";
	
	/**
	 * General entry point.
	 */
	@Test
	public void test() throws IOException
	{
		try
		{
			@SuppressWarnings("unused")
			StorageManager sm = new StorageManager("pom.xml");
			assert false;
		} catch (IOStorageException e)
		{
			// expected
		}
		
		testBasicSingletons();
		testEmptyAndNullPathElements();
		
		{
			// Test some non-existent paths.
			cleanTestDirectory();
			StorageManager sm = new StorageManager(TEST_DIRECTORY);
			testNonExistentPath(sm, true, ""); // root directory exists
			testNonExistentPath(sm, false, "%(!#^", "||{}?<>ZCNU", "///\\\\\\///+-");
			testNonExistentPath(sm, false, "just one level");
		}
		
		{
			// Build structure:
			// root ("") --> dataLevel1 --> nodataLevel2 --> dataLevel3 --> dataLevel4_1
			//                                                          \-> dataLevel4_2
			cleanTestDirectory();
			StorageManager sm = new StorageManager(TEST_DIRECTORY);
			
			StorageItem root = retrieveItem(sm, ExistMode.MUST_NOT_EXIST, "");
			playWithItem(root, true, false, false);
			StorageItem dataLevel1 = retrieveItem(sm, ExistMode.MUST_NOT_EXIST, "dataLevel1");
			playWithItem(dataLevel1, false, false, false);
			StorageItem nodataLevel2 = retrieveItem(sm, ExistMode.MUST_NOT_EXIST, "dataLevel1", "nodataLevel2");
			testNonExistentPath(sm, false, nodataLevel2.getPath());
			// First create deeper element to check how creation of higher-level element works afterwards
			StorageItem dataLevel4_1 = retrieveItem(sm, ExistMode.MUST_NOT_EXIST, "dataLevel1", "nodataLevel2", "dataLevel3", "dataLevel4_1");
			playWithItem(dataLevel4_1, false, false, false);
			StorageItem dataLevel3 = retrieveItem(sm, ExistMode.MUST_NOT_EXIST, "dataLevel1", "nodataLevel2", "dataLevel3");
			playWithItem(dataLevel3, true, false, false);
			StorageItem dataLevel4_2 = retrieveItem(sm, ExistMode.MUST_NOT_EXIST, "dataLevel1", "nodataLevel2", "dataLevel3", "dataLevel4_2");
			playWithItem(dataLevel4_2, false, false, false);
			
			{
				// Check that list works as expected.
				verifyCollection(sm.listStorageItems(""), dataLevel1);
				verifyCollection(sm.listStorageItems(dataLevel1.getPath()), nodataLevel2);
				verifyCollection(sm.listStorageItems(nodataLevel2.getPath()), dataLevel3);
				verifyCollection(sm.listStorageItems(dataLevel3.getPath()), dataLevel4_1, dataLevel4_2);
			}
			
			verifyCollection( sm.recursivelyFindExistingStorageItems(""), dataLevel1, dataLevel3, dataLevel4_1, dataLevel4_2 );
			verifyCollection( sm.recursivelyFindExistingStorageItems(dataLevel1.getPath()), dataLevel3, dataLevel4_1, dataLevel4_2 );
			verifyCollection( sm.recursivelyFindExistingStorageItems(dataLevel3.getPath()), dataLevel4_1, dataLevel4_2 );
			verifyCollection( sm.recursivelyFindExistingStorageItems(dataLevel4_1.getPath()) );
			
			// Load another storage manager instance and check stuff.
			sm = new StorageManager(TEST_DIRECTORY);
			
			// Specify 'check preexisting data' so that playing with item also checks what it is.
			root = retrieveItem(sm, ExistMode.MUST_EXIST, "");
			playWithItem(root, true, true, true);
			dataLevel1 = retrieveItem(sm, ExistMode.MUST_EXIST, "dataLevel1");
			playWithItem(dataLevel1, true, true, true);
			nodataLevel2 = retrieveItem(sm, ExistMode.MUST_NOT_EXIST, "dataLevel1", "nodataLevel2");
			testNonExistentPath(sm, true, nodataLevel2.getPath());
			// First create deeper element to check how creation of higher-level element works afterwards
			dataLevel4_1 = retrieveItem(sm, ExistMode.MUST_EXIST, "dataLevel1", "nodataLevel2", "dataLevel3", "dataLevel4_1");
			playWithItem(dataLevel4_1, true, true, true);
			dataLevel3 = retrieveItem(sm, ExistMode.MUST_EXIST, "dataLevel1", "nodataLevel2", "dataLevel3");
			playWithItem(dataLevel3, true, true, true);
			dataLevel4_2 = retrieveItem(sm, ExistMode.MUST_EXIST, "dataLevel1", "nodataLevel2", "dataLevel3", "dataLevel4_2");
			playWithItem(dataLevel4_2, true, true, true);
			
			verifyCollection( sm.recursivelyFindExistingStorageItems(""), dataLevel1, dataLevel3, dataLevel4_1, dataLevel4_2 );
			verifyCollection( sm.recursivelyFindExistingStorageItems(dataLevel1.getPath()), dataLevel3, dataLevel4_1, dataLevel4_2 );
			verifyCollection( sm.recursivelyFindExistingStorageItems(dataLevel3.getPath()), dataLevel4_1, dataLevel4_2 );
			verifyCollection( sm.recursivelyFindExistingStorageItems(dataLevel4_1.getPath()) );
			
			// Test exists modes with new storage manager instances.
			{
				// Tests exists modes on non-existent item.
				@Nonnull String[] path = new @Nonnull String[] {"abrakadabra"};
				testExistMode(true, ExistMode.ANY, path);
				testExistMode(true, ExistMode.MUST_BE_EMPTY_OR_NOT_EXIST, path);
				testExistMode(false, ExistMode.MUST_BE_NON_EMPTY, path);
				testExistMode(false, ExistMode.MUST_EXIST, path);
				testExistMode(true, ExistMode.MUST_NOT_EXIST, path);
			}
			{
				// Tests exists modes on existing item.
				@Nonnull String[] path = dataLevel3.getPath().toArray(new @Nonnull String[0]);
				testExistMode(true, ExistMode.ANY, path);
				testExistMode(false, ExistMode.MUST_BE_EMPTY_OR_NOT_EXIST, path);
				testExistMode(true, ExistMode.MUST_BE_NON_EMPTY, path);
				testExistMode(true, ExistMode.MUST_EXIST, path);
				testExistMode(false, ExistMode.MUST_NOT_EXIST, path);
			}
			{
				// Tests exists modes on empty item.
				// First we need to create empty item.
				assert !nodataLevel2.exists();
				nodataLevel2.writeMetadataAsSingleLine("a", "metadata");
				assert nodataLevel2.exists();
				assert !nodataLevel2.isEmpty();
				nodataLevel2.removeMetadata("a");
				assert nodataLevel2.exists();
				assert nodataLevel2.isEmpty();
				
				// Now test.
				@Nonnull String[] path = nodataLevel2.getPath().toArray(new @Nonnull String[0]);
				testExistMode(true, ExistMode.ANY, path);
				testExistMode(true, ExistMode.MUST_BE_EMPTY_OR_NOT_EXIST, path);
				testExistMode(false, ExistMode.MUST_BE_NON_EMPTY, path);
				testExistMode(true, ExistMode.MUST_EXIST, path);
				testExistMode(false, ExistMode.MUST_NOT_EXIST, path);
				
				// Now remove item that we've created.
				sm.delete(nodataLevel2);
			}
			
			// Test removing stuff.
			// First remove non-existent item -- it shouldn't have any effect on the following tests.
			sm.delete(nodataLevel2);
			
			removeItem(sm, dataLevel3, true);
			playWithItem(root, true, true, true);
			playWithItem(dataLevel1, true, true, true);
			testNonExistentPath(sm, true, nodataLevel2.getPath());
			playWithItem(dataLevel4_1, true, true, true);
			testNonExistentPath(sm, true, dataLevel3.getPath());
			playWithItem(dataLevel4_2, true, true, true);
			verifyCollection( sm.recursivelyFindExistingStorageItems(""), dataLevel1, dataLevel4_1, dataLevel4_2 );
			verifyCollection( sm.recursivelyFindExistingStorageItems(dataLevel1.getPath()), dataLevel4_1, dataLevel4_2 );
			verifyCollection( sm.recursivelyFindExistingStorageItems(dataLevel3.getPath()), dataLevel4_1, dataLevel4_2 );
			verifyCollection( sm.recursivelyFindExistingStorageItems(dataLevel4_1.getPath()) );
			
			// dataLevel3 directory must remain after this deletion
			removeItem(sm, dataLevel4_1, false);
			playWithItem(root, true, true, true);
			playWithItem(dataLevel1, true, true, true);
			testNonExistentPath(sm, true, nodataLevel2.getPath());
			testNonExistentPath(sm, false, dataLevel4_1.getPath());
			testNonExistentPath(sm, true, dataLevel3.getPath());
			playWithItem(dataLevel4_2, true, true, true);
			verifyCollection( sm.recursivelyFindExistingStorageItems(""), dataLevel1, dataLevel4_2 );
			verifyCollection( sm.recursivelyFindExistingStorageItems(dataLevel1.getPath()), dataLevel4_2 );
			verifyCollection( sm.recursivelyFindExistingStorageItems(dataLevel3.getPath()), dataLevel4_2 );
			try
			{
				verifyCollection( sm.recursivelyFindExistingStorageItems(dataLevel4_1.getPath()) );
				assert false;
			} catch(MissingDataStorageException e)
			{
				// expected
			}
			
			// Now dataLevel2 and dataLevel3 must be cleaned too.
			removeItem(sm, dataLevel4_2, false);
			playWithItem(root, true, true, true);
			playWithItem(dataLevel1, true, true, true);
			testNonExistentPath(sm, false, nodataLevel2.getPath());
			testNonExistentPath(sm, false, dataLevel4_1.getPath());
			testNonExistentPath(sm, false, dataLevel3.getPath());
			testNonExistentPath(sm, false, dataLevel4_2.getPath());
			verifyCollection( sm.recursivelyFindExistingStorageItems(""), dataLevel1 );
			verifyCollection( sm.recursivelyFindExistingStorageItems(dataLevel1.getPath()) );
			try
			{
				verifyCollection( sm.recursivelyFindExistingStorageItems(nodataLevel2.getPath()) );
				assert false;
			} catch(MissingDataStorageException e)
			{
				// expected
			}
			
			// Now only root remains.
			removeItem(sm, dataLevel1, false);
			playWithItem(root, true, true, true);
			testNonExistentPath(sm, false, dataLevel1.getPath());
			testNonExistentPath(sm, false, nodataLevel2.getPath());
			testNonExistentPath(sm, false, dataLevel4_1.getPath());
			testNonExistentPath(sm, false, dataLevel3.getPath());
			testNonExistentPath(sm, false, dataLevel4_2.getPath());
			verifyCollection( sm.recursivelyFindExistingStorageItems("") );
			try
			{
				verifyCollection( sm.recursivelyFindExistingStorageItems(dataLevel1.getPath()) );
				assert false;
			} catch(MissingDataStorageException e)
			{
				// expected
			}
			
			// Delete root now.
			removeItem(sm, root, true);
			testNonExistentPath(sm, true, "");
			testNonExistentPath(sm, false, dataLevel1.getPath());
			testNonExistentPath(sm, false, nodataLevel2.getPath());
			testNonExistentPath(sm, false, dataLevel4_1.getPath());
			testNonExistentPath(sm, false, dataLevel3.getPath());
			testNonExistentPath(sm, false, dataLevel4_2.getPath());
			verifyCollection( sm.recursivelyFindExistingStorageItems("") );
			try
			{
				verifyCollection( sm.recursivelyFindExistingStorageItems(dataLevel1.getPath()) );
				assert false;
			} catch(MissingDataStorageException e)
			{
				// expected
			}
		}
		
		// Clean at the end.
		cleanTestDirectory();
	}

	/**
	 * Tests that given items comprise the given collection (i.e. all
	 * present in collection and no extra elements).
	 * Assertion fail if not.
	 */
	@SafeVarargs
	private static <T> void verifyCollection(Collection<T> collection, T... items)
	{
		ArrayList<T> tmp = new ArrayList<>(collection);
		
		for (T item : items)
		{
			assert tmp.remove(item) : "Item [" + item + "] not found in collection: " + collection;
		}
		
		assert tmp.isEmpty() : "Collection [" + collection + "] contains extra elements: " + tmp;
	}
	
	/**
	 * @throws IOException
	 */
	private void testEmptyAndNullPathElements()
		throws IOException
	{
		// Test basic empty- and null-correctess.
		cleanTestDirectory();
		StorageManager sm = new StorageManager(TEST_DIRECTORY);
		sm.getStorageItem(""); // ok
		try
		{
			sm.getStorageItem("", "asd");
			assert false;
		} catch (IllegalArgumentException e)
		{
			// expected
		}
		try
		{
			sm.getStorageItem("asd", "");
			assert false;
		} catch (IllegalArgumentException e)
		{
			// expected
		}
		try
		{
			sm.getStorageItem("asd", fakeNonNull());
			assert false;
		} catch (IllegalArgumentException e)
		{
			// expected
		}
		try
		{
			sm.getStorageItem((String)fakeNonNull(), "qwe");
			assert false;
		} catch (IllegalArgumentException e)
		{
			// expected
		}
		try
		{
			sm.getStorageItem((String)fakeNonNull());
			assert false;
		} catch (IllegalArgumentException e)
		{
			// expected
		}
		try
		{
			List<String> lst = fakeNonNull();
			sm.getStorageItem(lst);
			assert false;
		} catch (IllegalArgumentException e)
		{
			// expected
		}
		try
		{
			List<String> lst = new ArrayList<>();
			lst.add("");
			sm.getStorageItem(lst);
			assert false;
		} catch (IllegalArgumentException e)
		{
			// expected
		}
		try
		{
			List<String> lst = new ArrayList<>();
			lst.add("");
			lst.add("qwe");
			sm.getStorageItem(lst);
			assert false;
		} catch (IllegalArgumentException e)
		{
			// expected
		}
		try
		{
			List<String> lst = new ArrayList<>();
			lst.add("qwe");
			lst.add("");
			sm.getStorageItem(lst);
			assert false;
		} catch (IllegalArgumentException e)
		{
			// expected
		}
		try
		{
			List<String> lst = new ArrayList<>();
			lst.add(fakeNonNull());
			sm.getStorageItem(lst);
			assert false;
		} catch (IllegalArgumentException e)
		{
			// expected
		}
		try
		{
			List<String> lst = new ArrayList<>();
			lst.add(fakeNonNull());
			lst.add("qwe");
			sm.getStorageItem(lst);
			assert false;
		} catch (IllegalArgumentException e)
		{
			// expected
		}
		try
		{
			List<String> lst = new ArrayList<>();
			lst.add("qwe");
			lst.add(fakeNonNull());
			sm.getStorageItem(lst);
			assert false;
		} catch (IllegalArgumentException e)
		{
			// expected
		}
	}

	/**
	 * @throws IOException
	 */
	private void testBasicSingletons()
		throws IOException
	{
		// Test basic singletons.
		cleanTestDirectory();
		StorageManager sm = new StorageManager(TEST_DIRECTORY);
		
		{
			// Test different ways to reference root item.
			StorageItem el1 = sm.getStorageItem("");
			StorageItem el2 = sm.getStorageItem(new @Nonnull String[0]);
			StorageItem el3 = sm.getStorageItem(new ArrayList<>(0));
			StorageItem el4 = sm.getStorageItem("");
			
			assert el1 == el2;
			assert el2 == el3;
			assert el3 == el4;
		}
		
		{
			// Test level1 item.
			StorageItem el1 = sm.getStorageItem("level1");
			StorageItem el2 = sm.getStorageItem(new @Nonnull String[] {new StringBuffer("level1").toString()});
			StorageItem el3 = sm.getStorageItem(Arrays.asList("level1"));
			StorageItem el4 = sm.getStorageItem("level1");
			
			assert el1 == el2;
			assert el2 == el3;
			assert el3 == el4;
		}
		
		{
			// Test level2 item.
			StorageItem el1 = sm.getStorageItem("level1", "level2");
			StorageItem el2 = sm.getStorageItem(new @Nonnull String[] {new StringBuffer("level1").toString(), new StringBuffer("level2").toString()});
			StorageItem el3 = sm.getStorageItem(Arrays.asList("level1", "level2"));
			StorageItem el4 = sm.getStorageItem("level1", "level2");
			
			assert el1 == el2;
			assert el2 == el3;
			assert el3 == el4;
		}
	}
	
	/**
	 * Cleans test directory.
	 */
	private void cleanTestDirectory() throws IOException
	{
		File dir = new File(TEST_DIRECTORY);
		if (dir.exists())
		{
			assert dir.isDirectory();
			FileUtils.deleteDirectory(dir);
		}
		
		dir.mkdirs();
		assert dir.exists();
		assert dir.isDirectory();
	}

	/**
	 * Retrieves specific path from storage manager and also tests various
	 * ways to reference it to ensure it is actually singleton.
	 */
	private StorageItem retrieveItem(StorageManager sm, ExistMode mode, @Nonnull String... path)
	{
		@Nonnull String[] alt = new @Nonnull String[path.length];
		System.arraycopy(path, 0, alt, 0, path.length);
		
		if (alt[0].length() > 1)
		{
			String item = alt[0];
			alt[0] = "" + item.charAt(0) + item.substring(1);
		}
		
		StorageItem el1 = sm.getStorageItem(mode, path);
		StorageItem el2 = sm.getStorageItem(mode, alt);
		StorageItem el3 = sm.getStorageItem(mode, convertArrayPathToList(path));
		StorageItem el4 = sm.getStorageItem(mode, path);
		
		assert el1 == el2;
		assert el2 == el3;
		assert el3 == el4;
		
		assert el1.getPath().equals(convertArrayPathToList(path));
		
		return el1;
	}
	
	/**
	 * Tests operations on non-existent path.
	 */
	private void testNonExistentPath(StorageManager sm, boolean preExistingDirectory, List<@Nonnull String> path)
	{
		testNonExistentPath(sm, preExistingDirectory, path.toArray(new @Nonnull String[0]));
	}
		
	/**
	 * Tests operations on non-existent path.
	 */
	private void testNonExistentPath(StorageManager sm, boolean preExistingDirectory, @Nonnull String... path)
	{
		File dir = findStorageDirectory(path);
		assert dir.exists() == preExistingDirectory;

		StorageItem item = retrieveItem(sm, ExistMode.MUST_NOT_EXIST, path);
		
		assert !item.exists();
		assert item.findAvailableMetadata().isEmpty();
		try
		{
			item.getInputStream();
			assert false;
		} catch (MissingDataStorageException e)
		{
			// expected.
		}
		try
		{
			item.readMetadata("some");
			assert false;
		} catch (MissingDataStorageException e)
		{
			// expected.
		}
		assert !item.hasAnyMetadata();
		assert !item.hasData();
		assert item.isEmpty();
		assert !item.hasMetadata("some");
		assert item.hasAllocatedStorage() == preExistingDirectory;
		item.removeData();
		item.removeMetadata("some");
		sm.delete(item);
		
		assert dir.exists() == preExistingDirectory;
	}
	
	/**
	 * Finds storage directory for the given path.
	 */
	private File findStorageDirectory(@Nonnull String... path)
	{
		return findStorageDirectory(convertArrayPathToList(path));
	}
	
	/**
	 * Finds storage directory for the given path.
	 */
	private File findStorageDirectory(List<String> path)
	{
		File dir = new File(TEST_DIRECTORY);
		for (@Nonnull String item : path)
		{
			dir = new File(dir, ASCIIUtil.encodeUnderscore(item));
		}
		
		return dir;
	}
	
	/**
	 * Converts path array to path list taking care of special casing.
	 */
	private static List<String> convertArrayPathToList(@Nonnull String[] path)
		throws IllegalArgumentException
	{
		if (nullable(path) == null)
			throw new IllegalArgumentException("Path may not be null.");
		
		// Special case for 'root' path.
		if (path.length == 1)
		{
			String item = path[0];
			if (nullable(item) == null)
				throw new IllegalArgumentException("Path elements may not be null: " + Arrays.toString(path));
			if (item.isEmpty())
				return Collections.EMPTY_LIST;
		}
		
		return Arrays.asList(path);
	}
	
	/**
	 * Counter to determine which path to take in {@link #playWithItem(StorageItem, boolean, boolean, boolean)}
	 */
	private AtomicInteger playPathCounter = new AtomicInteger(0);
	/**
	 * 'Plays' with an item -- writes some data, metadata, checks it out...
	 */
	private void playWithItem(StorageItem item, boolean preExistingDirectory, boolean preExistingItem, boolean verifyPreExistingItem)
		throws IOException
	{
		File dir = findStorageDirectory(item.getPath());
		assert dir.exists() == preExistingDirectory;
		assert item.exists() == preExistingItem;
		assert item.hasAllocatedStorage() == preExistingDirectory;
		
		if (!preExistingItem)
		{
			assert item.isEmpty();
			assert !item.hasAnyMetadata();
			assert !item.hasData();
		}
		
		if (verifyPreExistingItem)
		{
			assert preExistingItem;
			
			assert item.exists();
			assert item.hasAnyMetadata();
			assert item.hasData();
			assert item.hasAllocatedStorage();
			
			try (BufferedReader br = new BufferedReader(new InputStreamReader(item.getInputStream())))
			{
				assert "final_dataline1".equals(br.readLine());
				assert "final_dataline2".equals(br.readLine());
				assert br.readLine() == null;
			}
			{
				Set<@Nonnull String> mds = item.findAvailableMetadata();
				assert mds.remove("metadata2");
				assert mds.isEmpty() : mds;
			}
			assert item.readMetadataAsSingleLine("metadata2").equals("final_value1");
		}
		
		// Clean up (in case there's something already)
		item.removeData();
		for (String md : item.findAvailableMetadata())
		{
			item.removeMetadata(md);
		}
		assert item.isEmpty();
		assert !item.hasAnyMetadata();
		assert !item.hasData();
		
		
		try
		{
			item.getInputStream();
			assert false;
		} catch (MissingDataStorageException e)
		{
			// expected
		}
		assert item.findAvailableMetadata().isEmpty();
		
		// Write something and test.
		// Start with either metadata or data on different passes
		{
			int data = playPathCounter.incrementAndGet() % 2; // get 0 or 1
			for (int i = 0; i < 2; i++)
			{
				if (data == i)
				{
					// Write data.
					try (PrintWriter pw = new PrintWriter(item.getOutputStream()))
					{
						pw.println("dataline1");
						pw.println("dataline2");
					}
					
					assert item.hasData();
				}
				else
				{
					// Write metadata.
					item.writeMetadataAsSingleLine("metadata1", "value1");
					
					assert item.hasMetadata("metadata1");
					assert item.hasAnyMetadata();
				}
				assert item.exists();
				assert !item.isEmpty();
				assert dir.exists();
			}
		}
		
		// More write/overwrite of data.
		try (BufferedReader br = new BufferedReader(new InputStreamReader(item.getInputStream())))
		{
			assert "dataline1".equals(br.readLine());
			assert "dataline2".equals(br.readLine());
			assert br.readLine() == null;
		}
		try (PrintWriter pw = new PrintWriter(item.getOutputStream()))
		{
			pw.println("updated_dataline1");
			pw.println("updated_dataline2");
		}
		try (BufferedReader br = new BufferedReader(new InputStreamReader(item.getInputStream())))
		{
			assert "updated_dataline1".equals(br.readLine());
			assert "updated_dataline2".equals(br.readLine());
			assert br.readLine() == null;
		}
		item.removeData();
		assert !item.hasData();
		try
		{
			item.getInputStream();
			assert false;
		} catch (MissingDataStorageException e)
		{
			// expected
		}
		try (PrintWriter pw = new PrintWriter(item.getOutputStream()))
		{
			pw.println("final_dataline1");
			pw.println("final_dataline2");
		}
		try (BufferedReader br = new BufferedReader(new InputStreamReader(item.getInputStream())))
		{
			assert "final_dataline1".equals(br.readLine());
			assert "final_dataline2".equals(br.readLine());
			assert br.readLine() == null;
		}
		
		
		// Do some metadata stuff.
		assert item.readMetadataAsSingleLine("metadata1").equals("value1");
		{
			Set<@Nonnull String> mds = item.findAvailableMetadata();
			assert mds.remove("metadata1");
			assert mds.isEmpty();
		}
		item.writeMetadataAsSingleLine("metadata1", "");
		assert item.readMetadataAsSingleLine("metadata1").isEmpty();
		{
			List<String> list = Arrays.asList("other_value1", "other_value2");
			item.writeMetadata("metadata2", list);
			assert item.hasMetadata("metadata2");
			List<@Nonnull String> result = item.readMetadata("metadata2");
			assert list.equals(result);
		}
		{
			Set<@Nonnull String> mds = item.findAvailableMetadata();
			assert mds.remove("metadata1");
			assert mds.remove("metadata2");
			assert mds.isEmpty();
		}
		{
			item.writeMetadataAsSingleLine("metadata2", "final_value1");
			assert item.hasMetadata("metadata2");
			assert item.readMetadataAsSingleLine("metadata2").equals("final_value1");
		}
		{
			Set<@Nonnull String> mds = item.findAvailableMetadata();
			assert mds.remove("metadata1");
			assert mds.remove("metadata2");
			assert mds.isEmpty();
		}
		{
			String metadataBody = "\na some stuff\t\nhere is";
			item.writeMetadataBody("metadata1", metadataBody);
			assert item.readMetadataBody("metadata1").equals(metadataBody);
		}
		try
		{
			item.writeMetadataBody("metadata1", fakeNonNull());
			assert false;
		} catch (IllegalArgumentException e)
		{
			// expected
		}
		{
			item.removeMetadata("metadata1");
			assert !item.hasMetadata("metadata1");
			try
			{
				item.readMetadata("metadata1");
				assert false;
			} catch (MissingDataStorageException e)
			{
				// expected
			}
			try
			{
				item.readMetadataBody("metadata1");
				assert false;
			} catch (MissingDataStorageException e)
			{
				// expected
			}
		}
		{
			Set<@Nonnull String> mds = item.findAvailableMetadata();
			assert mds.remove("metadata2");
			assert mds.isEmpty();
		}
		try
		{
			item.writeMetadataAsSingleLine("metadata3", (String)fakeNonNull());
			assert false;
		} catch (IllegalArgumentException e)
		{
			// expected
		}
		try
		{
			List<@Nonnull String> list = fakeNonNull();
			item.writeMetadata("metadata4", list);
			assert false;
		} catch (IllegalArgumentException e)
		{
			// expected
		}
		try
		{
			List<@Nonnull String> list = new ArrayList<>();
			list.add("item1");
			list.add(fakeNonNull());
			list.add("item2");
			item.writeMetadata("metadata5", list);
			assert false;
		} catch (IllegalArgumentException e)
		{
			// expected
		}
		
		assert item.exists();
		assert item.hasData();
		assert item.hasAnyMetadata();
		assert item.hasAllocatedStorage();
		assert dir.exists();
	}
	
	/**
	 * Counter to determine which path to take in {@link #removeItem(StorageItem, boolean)}
	 */
	private AtomicInteger removeCounter = new AtomicInteger(0);
	/**
	 * Tests removal of an item. Item must have both data and metadata.
	 */
	private void removeItem(StorageManager sm, StorageItem item, boolean directoryMustRemain)
	{
		File dir = findStorageDirectory(item.getPath());
		assert dir.exists();
		assert item.exists();
		assert item.hasAnyMetadata();
		assert item.hasData();
		assert item.hasAllocatedStorage();
		
		int data = removeCounter.incrementAndGet() % 2; // get 0 or 1 or 2
		if (data < 2) // if data == 2 go directly to removing item
		{
			for (int i = 0; i < 2; i++)
			{
				if (data == i)
				{
					// Remove data.
					item.removeData();
					
					assert !item.hasData();
				}
				else
				{
					// Remove metadata.
					for (@Nonnull String md : item.findAvailableMetadata())
					{
						item.removeMetadata(md);
					}
					
					assert !item.hasAnyMetadata();
				}
				assert item.exists();
				assert item.hasAllocatedStorage();
				assert dir.exists();
			}
		}
		
		sm.delete(item); // TA-DA!
		assert !item.exists();
		assert item.isEmpty();
		assert !item.hasAnyMetadata();
		assert !item.hasData();
		assert item.hasAllocatedStorage() == directoryMustRemain;
		assert dir.exists() == directoryMustRemain;
	}
	
	/**
	 * Test exist mode for the specific item using a new {@link StorageManager}
	 * twice -- so uncached + cached variants.
	 */
	private void testExistMode(boolean success, ExistMode mode, @Nonnull String... path)
	{
		StorageManager sm = new StorageManager(TEST_DIRECTORY);
		
		try
		{
			sm.getStorageItem(mode, path);
			assert success;
		} catch (MismatchedExistModeException e)
		{
			assert !success;
		}
	}
}
