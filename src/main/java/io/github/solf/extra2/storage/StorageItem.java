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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import io.github.solf.extra2.storage.exception.IOStorageException;
import io.github.solf.extra2.storage.exception.MissingDataStorageException;
import io.github.solf.extra2.util.ASCIIUtil;

/**
 * Storage item together with its metadata.
 * {@link StorageItem} is synchronized on all methods, but thread-safety is not
 * guaranteed because operations on Input/Output streams are handled outside
 * this class.
 * 
 * NOTE: creating {@link StorageItem} instance in most cases does not write 
 * anything to the file system (exception: if storage directory exists but there's
 * no marker file, then every file in that directory is deleted to avoid 
 * inconsistencies in the future) -- particularly creating {@link StorageItem}
 * for a non-existent directory will not immediately create directory and marker
 * file -- these are created only when an actual write operation occurs.
 * 
 * @see StorageManager
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class StorageItem
{
	/**
	 * Whether to encode path (for storage on filesystem).
	 */
	private static final boolean ENCODE_PATH = true;
	
	/**
	 * Name of the marker file to indicate that {@link StorageItem} exists in a given directory.
	 */
	private static final String MARKER_FILE_NAME = "itemMarker";
	
	/**
	 * Name of the file for storing data.
	 */
	private static final String DATA_FILE_NAME = "data.data";
	
	/**
	 * Extension for metadata files.
	 */
	private static final String METADATA_FILE_EXT = "metadata";
	
	/**
	 * Metadata filename parse regexp.
	 */
	private static final Pattern REGEXP_METADATA_FILE_NAME_PARSE = Pattern.compile("^(.*)\\." + METADATA_FILE_EXT + "$");
	
	/**
	 * Base directory.
	 */
	@SuppressWarnings("unused")
	private final File baseDirectory;

	/**
	 * Directory where all data for this {@link StorageItem} is stored.
	 * May not exist (created lazily).
	 */
	private final File storageDirectory;
	
	/**
	 * Handle (File) for marker file.
	 */
	private final File markerHandle;
	
	/**
	 * Handle (File) for data file.
	 */
	private final File dataHandle;
	
	/**
	 * Name for this {@link StorageItem} -- last element in the {@link #getPath()}
	 * May be empty string when path denotes 'root' path. May never be null.
	 */
	private final String name;
	
	/**
	 * Path for this {@link StorageItem}
	 * May be empty (size=0) to denote 'root' path. Elements may never be
	 * null or empty.
	 */
	private final List<String> path;

	/**
	 * Creates {@link StorageItem} for the given path.
	 * 
	 * @param path item path; elements may not be null or empty EXCEPT for one
	 * 		special case of path containing a single empty element to denote
	 * 		'root' path (i.e. 'new StorageItem(file, "")') which is treated as
	 * 		equivalent to new 'StorageItem(file, new String[0])' for convenience 
	 */
	/* package */ StorageItem(File baseDirectory, @Nonnull String ... path)
		throws IllegalArgumentException, IllegalStateException
	{
		this(baseDirectory, convertArrayPathToList(path)); // conversion method takes care of special-casing
	}

	/**
	 * Creates {@link StorageItem} for the given path.
	 * 
	 * @param path elements may not be null or empty; list itself may be empty
	 * 		to indicate 'root'
	 */
	/* package */ StorageItem(File baseDirectory, List<String> path)
		throws IllegalArgumentException, IOStorageException
	{
		this.baseDirectory = baseDirectory;
		this.path = Collections.unmodifiableList(path);
		if (path.isEmpty())
			name = "";
		else
			name = path.get(path.size() - 1);
		this.storageDirectory = buildStoragePath(baseDirectory, path);
		
		this.markerHandle = createMarkerHandle(storageDirectory);
		this.dataHandle = new File(storageDirectory, DATA_FILE_NAME);
		
		// Clean up storage directory if no marker (to avoid inconsistencies later).
		if (storageDirectory.exists() && !markerHandle.exists())
		{
			for (File file : nnChecked(storageDirectory.listFiles()))
			{
				if (file.isFile())
				{
					file.delete();
					if (file.exists())
						throw new IOStorageException("Failed to clean up (remove) file: " + file);
				}
			}
		}
	}

	/**
	 * Creates {@link StorageItem} for the given storage directory. Fails if
	 * given path not exists, if given path is not a directory, if given path
	 * is not inside base directory, or directory names cannot be reverse-parsed 
	 * into path.
	 * 
	 * @param baseDirectory required so we know when to stop reverse-parsing
	 */
	/*package*/ StorageItem(File baseDirectory, File storagePath)
		throws IllegalArgumentException, IOStorageException
	{
		this(baseDirectory, recoverStoragePath(baseDirectory, storagePath));
	}
	
	/**
	 * Recursively finds all paths within given 'base directory & path' that contain 
	 * storage items (e.g. like 'ls -R'). The returned paths are guaranteed to
	 * point to an existing item ({@link StorageItem#exists()} == true)
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
	/*package*/ static Set<List<String>> recursivelyFindExistingStorageItems(File baseDirectory, @Nonnull String... path)
		throws IOStorageException, MissingDataStorageException
	{
		return recursivelyFindExistingStorageItems(baseDirectory, convertArrayPathToList(path));
	}
		
	/**
	 * Recursively finds all paths within given 'base directory & path' that contain 
	 * storage items (e.g. like 'ls -R'). The returned paths are guaranteed to
	 * point to an existing item ({@link StorageItem#exists()} == true)
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
	/*package*/ static Set<List<String>> recursivelyFindExistingStorageItems(File baseDirectory, List<String> path)
		throws IOStorageException, MissingDataStorageException
	{
		Set<List<String>> result = new HashSet<>();
		
		File startDirectory = buildStoragePath(baseDirectory, path);
		
		if (!startDirectory.exists())
			throw new MissingDataStorageException("Directory for path " + path + " doesn't exist: " + startDirectory);
		if (!startDirectory.isDirectory())
			throw new MissingDataStorageException("Path for " + path + " isn't directory: " + startDirectory);
		
		// Limit recursion to prevent potential infinite loops
		boolean bottommedOut = internalRecursivelyFindStorageItems(true, result, new ArrayList<>(path), startDirectory, 1024);
		
		if (bottommedOut)
			throw new IOStorageException("Recursive storage items search exceeded recursion limit of 1024. Check directories for loops.");
		
		return result;
	}
	
	/**
	 * Non-recursively (a la 'ls') finds all paths within given 
	 * 'base directory & path' that contain storage items.
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
	/*package*/ static Set<List<String>> listStorageItems(File baseDirectory, @Nonnull String... path)
		throws IOStorageException, MissingDataStorageException
	{
		return listStorageItems(baseDirectory, convertArrayPathToList(path));
	}
		
	/**
	 * Non-recursively (a la 'ls') finds all paths within given 
	 * 'base directory & path' that contain storage items.
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
	/*package*/ static Set<List<String>> listStorageItems(File baseDirectory, List<String> path)
		throws IOStorageException, MissingDataStorageException
	{
		Set<List<String>> result = new HashSet<>();
		
		File startDirectory = buildStoragePath(baseDirectory, path);
		
		if (!startDirectory.exists())
			throw new MissingDataStorageException("Directory for path " + path + " doesn't exist: " + startDirectory);
		if (!startDirectory.isDirectory())
			throw new MissingDataStorageException("Path for " + path + " isn't directory: " + startDirectory);
		
		// limit = 0 as we only want the current directory
		internalRecursivelyFindStorageItems(false, result, new ArrayList<>(path), startDirectory, 0);
		
		return result;
	}
	
	/**
	 * Internal method to recursively search for {@link StorageItem}s starting
	 * with a given path (path itself is not included in result).
	 * 
	 * NOTE: current directory is not included in the result even if it contains
	 * 		marker
	 * 
	 * @param pathSoFar calculated path so far -- used to optimize path calculation
	 * 		for results; path elements are added before recursing into function
	 * 		and removed after returning from function; these path
	 * 		elements are already decoded (if needed)
	 * @param recursionLimit limits how many levels down we can go; 0 means current
	 * 		directory only, negative value means that we've reached the bottom
	 * 		and should return immediately
	 * 
	 * @return true if limit was reached on any branch; false otherwise
	 */
	private static boolean internalRecursivelyFindStorageItems(boolean onlyIncludeExistingItems,
		Set<List<String>> result, ArrayList<String> pathSoFar, File currentDirectory, int recursionLimit)
	{
		if (recursionLimit < 0)
			return true;
		
		boolean reachedBottom = false;
		for (File item : nnChecked(currentDirectory.listFiles()))
		{
			if (!item.isDirectory())
				continue; // skip non-directories
			
			// Check if directory is valid for path element / decode actual path element. 
			String pathElement = item.getName();
			if (ENCODE_PATH)
			{
				try
				{
					pathElement = ASCIIUtil.decodeUnderscore(pathElement);
				} catch (IllegalArgumentException e)
				{
					continue; // Skip invalid directories.
				}
			}
			
			pathSoFar.add(pathElement);
			
			// If only include existing items -- check if directory contains StorageItem marker and add path if so.
			if ((!onlyIncludeExistingItems) || (createMarkerHandle(item).exists()))
				result.add(new ArrayList<>(pathSoFar));
			
			// Recurse into directory
			if (internalRecursivelyFindStorageItems(onlyIncludeExistingItems, result, pathSoFar, item, recursionLimit - 1)) // Lower limit by 1 when recursing
				reachedBottom = true; // Track whether we've reached bottom anywhere.
			
			pathSoFar.remove(pathSoFar.size() - 1); // Remove added path element
		}
		
		return reachedBottom;
	}
	
	
	
	/**
	 * Converts path array to path list taking care of special casing.
	 */
	/*package*/ static List<String> convertArrayPathToList(@Nonnull String[] path)
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
	 * Builds storage path for the given path.
	 * 
	 * @param path path elements may not be null or empty; empty path is allowed
	 * 		(denotes 'root' element)
	 * 
	 * @throws IOStorageException if resulting path exists and is not a directory
	 */
	/*package*/ static File buildStoragePath(File baseDirectory, List<String> path)
			throws IllegalArgumentException, IOStorageException
	{
		File result = baseDirectory;
		for (String item : path)
		{
			if (item.isEmpty())
				throw new IllegalArgumentException("Path elements may not be empty: " + path);
			
			if (ENCODE_PATH)
				item = ASCIIUtil.encodeUnderscore(item);
			
			result = new File(result, item);
		}

		if (result.exists() && (!result.isDirectory()))
			throw new IOStorageException("Storage path exists, but is not a directory: " + result);

		return result;
	}
	
	/**
	 * 'Recovers' storage path from the directory structure.
	 * Walks directories backwards until baseDirectory
	 * 
	 * @param baseDirectory required so we know when to stop reverse-parsing
	 */
	private static List<String> recoverStoragePath(File baseDirectory, File storagePath)
		throws IllegalArgumentException, IOStorageException
	{
		try
		{
			Path stopPath = baseDirectory.toPath();
			File current = storagePath;
			
			List<String> reversedResult = new ArrayList<>();
			while (true)
			{
				if (Files.isSameFile(stopPath, current.toPath()))
					break;
				
				String item = current.getName();
				if (ENCODE_PATH)
				{
					try
					{
						item = ASCIIUtil.decodeUnderscore(item);
					} catch (Exception e)
					{
						throw new IllegalArgumentException("Storage path [" + storagePath + "] contains elements [" + item + "] that cannot be decoded: " + e, e);
					}
				}
				
				reversedResult.add(item);
				
				current = current.getParentFile();
				if (current == null)
					throw new IllegalArgumentException("Storage path [" + storagePath + "] doesn't appear to be contained in base directory: " + baseDirectory);
			}
			
			Collections.reverse(reversedResult);
			
			return reversedResult;
		} catch (IOException e)
		{
			throw new IOStorageException(e);
		}
	}
	
	/**
	 * Checks whether storage has been allocated (directory created in this
	 * particular implementation).
	 * Storage is allocated lazily, so if it has been allocated, that means
	 * that some operation took place (successfully or not).
	 */
	public synchronized boolean hasAllocatedStorage()
	{
		return storageDirectory.exists();
	}
	
	/**
	 * Ensures that storage directory exists (creates if necessary).
	 */
	private void ensureStorageDirectoryExists() throws IOStorageException
	{
		try
		{
			if (!storageDirectory.exists())
				storageDirectory.mkdirs();
			
			if (!storageDirectory.exists())
				throw new IOStorageException("Failed to create storage directory: " + storageDirectory);
			
			if (!markerHandle.exists())
				markerHandle.createNewFile();
			
			if (!markerHandle.exists())
				throw new IOStorageException("Failed to create marker file: " + markerHandle);
		} catch (IOException e)
		{
			throw new IOStorageException(e);
		}
	}
	
	/**
	 * Checks whether this {@link StorageItem} exists -- specifically checks
	 * if marker is present (marker is created whenever any write occurs and
	 * is only removed if remove() is called). 
	 */
	public synchronized boolean exists()
	{
		return markerHandle.exists();
	}
	
	/**
	 * Removes all files in storage directory (including marker file) -- i.e.
	 * effectively removes the item.
	 * Directory cleanup is not done here because {@link StorageItem} conceptually
	 * doesn't touch anything outside its own directory.
	 */
	/*package*/ synchronized void removeAllFiles() throws IOStorageException
	{
		if (!storageDirectory.exists())
			return;
		
		// First remove marker if exists.
		if (markerHandle.exists())
			markerHandle.delete();
		
		if (markerHandle.exists())
			throw new IOStorageException("Failed to remove marker file: " + markerHandle);
		
		for (File item : nnChecked(storageDirectory.listFiles()))
		{
			if (!item.isFile())
				continue; // Skip non-files.
			
			item.delete();
			if (item.exists())
				throw new IOStorageException("Failed to remove file: " + item);
		}
	}
	
	/**
	 * Checks whether storage has any data in it.
	 * 
	 * @return true if data present (even if empty); false if it was not created
	 * 		or was removed
	 */
	public synchronized boolean hasData()
	{
		return dataHandle.exists();
	}
	
	/**
	 * Retrieves list of metadata available for this {@link StorageItem}
	 * 
	 * @return never null, set is empty in case there's no metadata
	 */
	public synchronized Set<String> findAvailableMetadata()
	{
		if (!hasAllocatedStorage())
			return Collections.EMPTY_SET;
		
		Set<String> result = new HashSet<>();
		for (String file : nnChecked(storageDirectory.list()))
		{
			Matcher matcher = REGEXP_METADATA_FILE_NAME_PARSE.matcher(file);
			if (matcher.matches())
				result.add(ASCIIUtil.decodeUnderscore(nnChecked(matcher.group(1))));
		}
		
		return result;
	}
	
	/**
	 * Creates a handle (File) for marker file in the specific directory.
	 * Doesn't guarantee that file actually exists.
	 */
	private static File createMarkerHandle(File dir)
	{
		return new File(dir, MARKER_FILE_NAME);
	}
	
	/**
	 * Creates a handle (File) for specific metadata file.
	 * Doesn't guarantee that file actually exists.
	 */
	private File createMetadataFileHandle(String metadata) throws IllegalArgumentException
	{
		if (nullable(metadata) == null)
			throw new IllegalArgumentException("Metadata name may not be null.");
		if (metadata.isEmpty())
			throw new IllegalArgumentException("Metadata name may not be empty.");
		
		return new File(storageDirectory, ASCIIUtil.encodeUnderscore(metadata) + '.' + METADATA_FILE_EXT);
	}
	
	/**
	 * Checks whether storage has any metadata in it.
	 * 
	 * @return true if any metadata is present (even if empty); false otherwise
	 */
	public synchronized boolean hasAnyMetadata()
	{
		return !findAvailableMetadata().isEmpty();
	}
	
	/**
	 * Checks whether storage has specific metadata in it.
	 * 
	 * @return true if specified metadata is present (even if empty); false otherwise
	 */
	public synchronized boolean hasMetadata(String metadata)
	{
		return findAvailableMetadata().contains(metadata);
	}
	
	/**
	 * Returns true if neither data, nor metadata is present.
	 * False otherwise. 
	 */
	public synchronized boolean isEmpty()
	{
		if (hasData() || hasAnyMetadata())
			return false;
		
		return true;
	}
	
	/**
	 * Gets input stream for reading stored data.
	 * 
	 * @throws MissingDataStorageException if data is not present
	 */
	public synchronized InputStream getInputStream() throws MissingDataStorageException
	{
		try
		{
			return new FileInputStream(dataHandle);
		} catch( FileNotFoundException e )
		{
			throw new MissingDataStorageException("Missing data file: " + e, e);
		}
	}
	
	/**
	 * Gets output stream for writing stored data.
	 * WARNING: calling this method will immediately and irrevocably destroy
	 * any previously stored data.
	 * 
	 * You should flush/close stream at the end. New calls to this method will
	 * attempt to produce new streams and if old one is not closed -- that may
	 * break things.
	 */
	public synchronized OutputStream getOutputStream() throws IOStorageException
	{
		ensureStorageDirectoryExists();
		try
		{
			return new FileOutputStream(dataHandle);
		} catch (IOException e)
		{
			throw new IOStorageException(e);
		}
	}
	
	/**
	 * Removes stored data.
	 * Does nothing if data is not present.
	 * 
	 * @return this instance (for easy chaining)
	 */
	public synchronized StorageItem removeData() throws IOStorageException
	{
		dataHandle.delete();
		
		if (dataHandle.exists())
			throw new IOStorageException("Failed to delete data file: " + dataHandle);
		
		return this;
	}
	
	/**
	 * Gets specific metadata.
	 * 
	 * @throws MissingDataStorageException if given metadata does not exist
	 */
	public synchronized List<String> readMetadata(String metadata) 
		throws MissingDataStorageException, IOStorageException, IllegalArgumentException
	{
		try
		{
			return Files.readAllLines(createMetadataFileHandle(metadata).toPath(), StandardCharsets.UTF_8);
		} catch (NoSuchFileException e)
		{
			throw new MissingDataStorageException("Metadata file not found: " + metadata, e);
		} catch(IOException e)
		{
			throw new IOStorageException(e);
		}
	}
	
	/**
	 * Helper method to read metadata that consists of exactly one line.
	 * 
	 * @throws IllegalStateException if metadata contains more than one line or
	 * 		no lines
	 */
	public synchronized String readMetadataAsSingleLine(String metadata)
		throws MissingDataStorageException, IOStorageException, IllegalArgumentException, IllegalStateException
	{
		List<@Nonnull String> list = readMetadata(metadata);
		
		if (list.size() != 1)
			throw new IllegalStateException("Metadata [" + metadata + "] doesn't consist of one line only: " + list);
		
		return list.get(0);
	}

	
	/**
	 * Sets specific metadata body (as is, no additional line delimiters or anything).
	 * Item may not be null.
	 * 
	 * @param body metadata body to be set 'as is'; read will return exactly the
	 * 		same value
	 * 
	 * @return this instance (for easy chaining)
	 */
	public synchronized StorageItem writeMetadataBody(String metadata, String body) throws IOStorageException, IllegalArgumentException
	{
		if (nullable(body) == null)
			throw new IllegalArgumentException("Metadata body may not be null.");
		
		ensureStorageDirectoryExists();

		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		
		try
		{
			Files.write(createMetadataFileHandle(metadata).toPath(), bytes);
		} catch (IOException e)
		{
			throw new IOStorageException(e);
		}
		
		return this;
	}
	
	/**
	 * Reads specific metadata body (as is, no additional line delimiters or anything).
	 * Item may not be null.
	 *
	 * @return metadata body exactly as was set by {@link #writeMetadataBody(String, String)}
	 */
	public synchronized String readMetadataBody(String metadata) throws MissingDataStorageException, IOStorageException, IllegalArgumentException
	{
		ensureStorageDirectoryExists();
		
		try
		{
			byte[] bytes = Files.readAllBytes(createMetadataFileHandle(metadata).toPath());
			return new String(bytes, StandardCharsets.UTF_8);
		} catch (NoSuchFileException e)
		{
			throw new MissingDataStorageException("Metadata file not found: " + metadata, e);
		} catch (IOException e)
		{
			throw new IOStorageException(e);
		}
	}
	
	/**
	 * Sets specific metadata.
	 * Item may not be null.
	 * 
	 * @param value converted into list with exactly one item for convenience
	 * 
	 * @return this instance (for easy chaining)
	 */
	public synchronized StorageItem writeMetadataAsSingleLine(String metadata, String value) throws IOStorageException, IllegalArgumentException
	{
		List<String> list = new ArrayList<>(1);
		list.add(value);
		return writeMetadata(metadata, list);
	}
	
	/**
	 * Sets specific metadata.
	 * Items may not be null.
	 * 
	 * @return this instance (for easy chaining)
	 */
	public synchronized StorageItem writeMetadata(String metadata, List<String> value) throws IOStorageException, IllegalArgumentException
	{
		if (nullable(value) == null)
			throw new IllegalArgumentException("Metadata value may not be null.");
		
		// Check before writing anything.
		for (@Nonnull String line : value)
		{
			if (nullable(line) == null)
				throw new IllegalArgumentException("Metadata may not contain null strings, got: " + value);
		}
		
		ensureStorageDirectoryExists();
		
		try
		{
			try (PrintWriter pw = new PrintWriter( new OutputStreamWriter( 
				new FileOutputStream( createMetadataFileHandle(metadata) ), 
				StandardCharsets.UTF_8)))
			{
				for (@Nonnull String line : value)
				{
					pw.println(line);
				}
				
				return this;
			}
		} catch (IOException e)
		{
			throw new IOStorageException(e);
		}
	}
	
	/**
	 * Removes specific metadata.
	 * Does nothing if specific metadata is not present.
	 * 
	 * @return this instance (for easy chaining)
	 */
	public synchronized StorageItem removeMetadata(String metadata) throws IOStorageException
	{
		File handle = createMetadataFileHandle(metadata);
		handle.delete();
		
		if (handle.exists())
			throw new IOStorageException("Failed to delete metadata [" + metadata + "] file: " + handle);
		
		return this;
	}
	
	/**
	 * Gets path of this {@link StorageItem}
	 */
	public List<String> getPath()
	{
		return path;
	}
	
	/**
	 * Gets name for this {@link StorageItem} -- last element in the {@link #getPath()}
	 * May be empty string when path denotes 'root' path. May never be null.
	 */
	public String getName()
	{
		return name;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "StorageItem" + path;
	}
}
