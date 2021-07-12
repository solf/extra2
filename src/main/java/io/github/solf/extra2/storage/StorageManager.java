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

import static io.github.solf.extra2.util.NullUtil.nnChecked;
import static io.github.solf.extra2.util.NullUtil.nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.solf.extra2.storage.exception.IOStorageException;
import io.github.solf.extra2.storage.exception.MismatchedExistModeException;
import io.github.solf.extra2.storage.exception.MissingDataStorageException;
import io.github.solf.extra2.util.SplitUtil;

/**
 * Storage manager for storing 'stuff' and its metadata.
 * 
 * Metadata is flexible, may contain 'any' number of different metadata types,
 * and each 'type' is represented by a list of strings. Interpretation of
 * metadata is up to the caller.
 * 
 * Thread-safe as much as possible, but since data is actually read/written
 * externally -- these parts must be made thread-safe externally (if needed).
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class StorageManager 
{
	/**
	 * Directory for storing data.
	 */
	private final File storageDirectory;
	
	/**
	 * Cache + ensure singleton: group+name identifier -> {@link StorageItem}
	 */
	private final Map<String, StorageItem> storageItems = new HashMap<>();
	
	/**
	 * Constructor.
	 * Will try to create storage directory if it doesn't exist.
	 */
	public StorageManager(File storageDirectory) throws IOStorageException
	{
		try
		{
			File canonicalDirectory = new File(storageDirectory.getCanonicalPath()); // This is more convenient for error reporting and such.
			
			if (!canonicalDirectory.exists())
			{
				canonicalDirectory.mkdirs();
			}
			
			if (!canonicalDirectory.exists())
				throw new IOStorageException("Failed to create storage directory: " + canonicalDirectory);
			if (!canonicalDirectory.isDirectory())
				throw new IOStorageException("Storage path exists, but is not a directory: " + canonicalDirectory);
			
			this.storageDirectory = canonicalDirectory;
		} catch (IOException e)
		{
			throw new IOStorageException(e);
		}
	}
	
	/**
	 * Constructor.
	 * Will try to create storage directory if it doesn't exist.
	 */
	public StorageManager(String storageDirectory) throws IOStorageException
	{
		this(new File(storageDirectory));
	}
	
	/**
	 * Gets storage item for specific path.
	 * Storage is initialized lazily -- i.e. nothing is actually written until
	 * an actual write operation is issued.
	 * 
	 * @param mode defines whether item must already exist
	 * @param path item path; elements may not be null or empty EXCEPT for one
	 * 		special case of path containing a single empty element to denote
	 * 		'root' path (i.e. 'new StorageItem(file, "")') which is treated as
	 * 		equivalent to new 'StorageItem(file, new String[0])' for convenience 
	 */
	public synchronized StorageItem getStorageItem(ExistMode mode, @Nonnull String... path)
		throws IllegalArgumentException, IOStorageException, MismatchedExistModeException
	{
		return getStorageItem(mode, StorageItem.convertArrayPathToList(path));
	}
	
	/**
	 * Gets storage item for specific path.
	 * Storage is initialized lazily -- i.e. nothing is actually written until
	 * an actual write operation is issued.
	 * 
	 * @param mode defines whether item must already exist
	 * @param path elements may not be null or empty; list itself may be empty
	 * 		to indicate 'root'
	 */
	public synchronized StorageItem getStorageItem(ExistMode mode, List<String> path)
		throws IllegalArgumentException, IOStorageException, MismatchedExistModeException
	{
		if (nullable(path) == null)
			throw new IllegalArgumentException("List argument may not be null.");
		if (nullable(mode) == null)
			throw new IllegalArgumentException("ExistMode argument may not be null.");
		
		String key;
		if (path.isEmpty())
			key = "";
		else if (path.size() == 1)
		{
			key = path.get(0);
			if (nullable(key) == null)
				throw new IllegalArgumentException("List items may not be null: " + path);
			if (key.isEmpty())
				throw new IllegalArgumentException("List items may not be empty: " + path);
		}
		else
			key = SplitUtil.mergeStrings(':', path);
		
		
		StorageItem item = storageItems.get(key);
		
		if (item == null)
		{
			item = new StorageItem(storageDirectory, path);
			
			@Nullable
			StorageItem concurrent = storageItems.putIfAbsent(key, item);
			if (concurrent != null)
				item = concurrent; // If added concurrently, use that value. 
		}
		
		switch(mode)
		{
			case ANY:
				break;
			case MUST_NOT_EXIST:
				if (item.exists())
					throw new MismatchedExistModeException("Item exist when it should not, requested mode [" + mode + "] on path: " + path);
				break;
			case MUST_EXIST:
				if (!item.exists())
					throw new MismatchedExistModeException("Item does not exist when it should, requested mode [" + mode + "] on path: " + path);
				break;
			case MUST_BE_EMPTY_OR_NOT_EXIST:
				if (item.exists())
					if (!item.isEmpty())
						throw new MismatchedExistModeException("Non-empty item when it should be empty or new, requested mode [" + mode + "] on path: " + path);
				break;
			case MUST_BE_NON_EMPTY:
				if (!item.exists())
					throw new MismatchedExistModeException("Item does not exist when it should, requested mode [" + mode + "] on path: " + path);
				if (item.isEmpty())
					throw new MismatchedExistModeException("Item is empty when it should be non-empty, requested mode [" + mode + "] on path: " + path);
				break;
		}
		
		return item;
	}
	
	/**
	 * Gets storage item for specific path.
	 * This method uses {@link ExistMode#ANY} for checking existence.
	 * Storage is initialized lazily -- i.e. nothing is actually written until
	 * an actual write operation is issued.
	 * 
	 * @param mode defines whether item must already exist
	 * @param path item path; elements may not be null or empty EXCEPT for one
	 * 		special case of path containing a single empty element to denote
	 * 		'root' path (i.e. 'new StorageItem(file, "")') which is treated as
	 * 		equivalent to new 'StorageItem(file, new String[0])' for convenience 
	 */
	public synchronized StorageItem getStorageItem(@Nonnull String... path)
		throws IllegalArgumentException, IOStorageException, MismatchedExistModeException
	{
		return getStorageItem(ExistMode.ANY, path);
	}
	
	/**
	 * Gets storage item for specific path.
	 * This method uses {@link ExistMode#ANY} for checking existence.
	 * Storage is initialized lazily -- i.e. nothing is actually written until
	 * an actual write operation is issued.
	 * 
	 * @param mode defines whether item must already exist
	 * @param path elements may not be null or empty; list itself may be empty
	 * 		to indicate 'root'
	 */
	public synchronized StorageItem getStorageItem(List<String> path)
		throws IllegalArgumentException, IOStorageException, MismatchedExistModeException
	{
		return getStorageItem(ExistMode.ANY, path);
	}
	
	/**
	 * Recursively finds all paths within given path that contain storage items
	 * (e.g. like 'ls -R'). The returned items are guaranteed to exist 
	 * ({@link StorageItem#exists()} == true)
	 * Ignores paths that don't parse properly (i.e. not encoded properly).
	 * 
	 * @param path elements may not be null or empty except for a special case
	 * 		with one empty element (it addresses 'root' element in a more convenient
	 * 		form than 'recursivelyFindStorageItems(baseDirectory, new String[0])')
	 * 
	 * @return list of found paths; does NOT include the given path itself ('root') --
	 * 		similarly how you expect file listing in a directory to work
	 * 
	 * @throws MissingDataStorageException if given path doesn't exist
	 * 
	 * @see #listStorageItems(File, String...)
	 */
	public synchronized Set<StorageItem> recursivelyFindExistingStorageItems(@Nonnull String... path)
		throws IOStorageException, MissingDataStorageException
	{
		return convertPathsToStorageItems(ExistMode.MUST_EXIST, StorageItem.recursivelyFindExistingStorageItems(storageDirectory, path));
	}
		
	/**
	 * Recursively finds all paths within given path that contain storage items
	 * (e.g. like 'ls -R'). The returned items are guaranteed to exist 
	 * ({@link StorageItem#exists()} == true)
	 * Ignores paths that don't parse properly (i.e. not encoded properly).
	 * 
	 * @param path elements may not be null or empty; list itself may be empty
	 * 		to start search from the 'root'
	 * 
	 * @return list of found paths; does NOT include the given path itself ('root') --
	 * 		similarly how you expect file listing in a directory to work
	 * 
	 * @throws MissingDataStorageException if given path doesn't exist
	 * 
	 * @see #listStorageItems(File, List)
	 */
	public synchronized Set<StorageItem> recursivelyFindExistingStorageItems(List<String> path)
		throws IOStorageException, MissingDataStorageException
	{
		return convertPathsToStorageItems(ExistMode.MUST_EXIST, StorageItem.recursivelyFindExistingStorageItems(storageDirectory, path));
	}
	
	/**
	 * Recursively finds all paths within given path that contain storage items
	 * (e.g. like 'ls -R'). The returned items are guaranteed to exist 
	 * ({@link StorageItem#exists()} == true)
	 * Ignores paths that don't parse properly (i.e. not encoded properly).
	 * 
	 * @param path {@link StorageItem} that acts as path
	 *  
	 * @return list of found paths; does NOT include the given path itself ('root') --
	 * 		similarly how you expect file listing in a directory to work
	 * 
	 * @throws MissingDataStorageException if given path doesn't exist
	 * 
	 * @see #listStorageItems(File, List)
	 */
	public synchronized Set<StorageItem> recursivelyFindExistingStorageItems(StorageItem path)
		throws IOStorageException, MissingDataStorageException
	{
		return convertPathsToStorageItems(ExistMode.MUST_EXIST, StorageItem.recursivelyFindExistingStorageItems(storageDirectory, path.getPath()));
	}
	
	/**
	 * Non-recursively (a la 'ls') finds all paths within given path that contain 
	 * storage items.
	 * Result may include items that do not exist but may have existing children.
	 * Ignores paths that don't parse properly (i.e. not encoded properly).
	 * 
	 * @param path elements may not be null or empty except for a special case
	 * 		with one empty element (it addresses 'root' element in a more convenient
	 * 		form than 'recursivelyFindStorageItems(baseDirectory, new String[0])')
	 * 
	 * @return set of found paths; does NOT include the given path itself ('root') --
	 * 		similarly how you expect file listing in a directory to work
	 * 
	 * @throws MissingDataStorageException if given path doesn't exist
	 * 
	 * @see #recursivelyFindExistingStorageItems(File, String...)
	 */
	public synchronized Set<StorageItem> listStorageItems(@Nonnull String... path)
		throws IOStorageException, MissingDataStorageException
	{
		return convertPathsToStorageItems(ExistMode.ANY, StorageItem.listStorageItems(storageDirectory, path));
	}
		
	/**
	 * Non-recursively (a la 'ls') finds all paths within given path that contain 
	 * storage items.
	 * Result may include items that do not exist but may have existing children.
	 * Ignores paths that don't parse properly (i.e. not encoded properly).
	 * 
	 * @param path elements may not be null or empty; list itself may be empty
	 * 		to start search from the 'root'
	 * 
	 * @return set of found paths; does NOT include the given path itself ('root') --
	 * 		similarly how you expect file listing in a directory to work
	 * 
	 * @throws MissingDataStorageException if given path doesn't exist
	 * 
	 * @see #recursivelyFindExistingStorageItems(File, List)
	 */
	public synchronized Set<StorageItem> listStorageItems(List<String> path)
		throws IOStorageException, MissingDataStorageException
	{
		return convertPathsToStorageItems(ExistMode.ANY, StorageItem.listStorageItems(storageDirectory, path));
	}
	
	/**
	 * Non-recursively (a la 'ls') finds all paths within given path that contain 
	 * storage items.
	 * Result may include items that do not exist but may have existing children.
	 * Ignores paths that don't parse properly (i.e. not encoded properly).
	 * 
	 * @param path {@link StorageItem} that defines the path
	 * 
	 * @return set of found paths; does NOT include the given path itself ('root') --
	 * 		similarly how you expect file listing in a directory to work
	 * 
	 * @throws MissingDataStorageException if given path doesn't exist
	 * 
	 * @see #recursivelyFindExistingStorageItems(File, List)
	 */
	public synchronized Set<StorageItem> listStorageItems(StorageItem path)
		throws IOStorageException, MissingDataStorageException
	{
		return convertPathsToStorageItems(ExistMode.ANY, StorageItem.listStorageItems(storageDirectory, path.getPath()));
	}
	
	/**
	 * Converts a set of paths to a set of {@link StorageItem}
	 */
	private Set<StorageItem> convertPathsToStorageItems(ExistMode existMode, Set<List<String>> paths)
	{
		Set<StorageItem> result = new HashSet<>(paths.size());
		
		for (List<@Nonnull String> path : paths)
		{
			result.add(getStorageItem(existMode, path));
		}
		
		return result;
	}
	

	/**
	 * Removes this {@link StorageItem} -- specifically removes every file
	 * in directory and directory itself (if possible).
	 * Also scans for any directories up to base directory that can be subsequently
	 * removed (so as not to retain empty directories indefinitely).
	 * 
	 * This is done at manager level (rather than item level) because of 
	 * potential changes to directory structure -- which is outside item's
	 * purview.
	 */
	public synchronized void delete(StorageItem item) throws IOStorageException
	{
		item.removeAllFiles();
		
		// Now try to clean up as many unused directories as we can.
		List<String> remainingPath = new ArrayList<>(item.getPath());
		while (true)
		{
			if (remainingPath.size() < 1)
				break; // Don't delete root directory.
			
			if (!removeDirectoryIfEmpty(storageDirectory, remainingPath))
				break; // Can't delete this, no point in trying higher up
			
			remainingPath.remove(remainingPath.size() - 1);
		}
	}
	
	/**
	 * Cleans up (removes) directory if it is empty.
	 * 
	 * @return true if directory doesn't exist anymore (either removed or didn't
	 * 		exist in the first place); false if directory is still present (not
	 * 		empty)
	 */
	private static boolean removeDirectoryIfEmpty(File baseDirectory, List<String> path)
		throws IOStorageException
	{
		File dir = StorageItem.buildStoragePath(baseDirectory, path);
		if (!dir.exists())
			return true;
		
		if (!dir.isDirectory())
			throw new IOStorageException("Path for removal is not a directory: " + dir);
		
		if (nnChecked(dir.listFiles()).length > 0)
		{
			return false; // Can't remove directory that has something in it.
		}
		
		dir.delete();
		if (dir.exists())
			throw new IOStorageException("Failed to remove directory: " + dir);
		
		return true;
	}
	
}
