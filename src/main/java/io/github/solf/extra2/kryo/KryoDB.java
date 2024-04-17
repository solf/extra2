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
package io.github.solf.extra2.kryo;

import static io.github.solf.extra2.util.NullUtil.nn;
import static io.github.solf.extra2.util.NullUtil.nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.joda.time.LocalDate;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;

import io.github.solf.extra2.objectgraph.ObjectGraphCompoundNodeVisitor;
import io.github.solf.extra2.objectgraph.ObjectGraphConfig;
import io.github.solf.extra2.objectgraph.ObjectGraphUtil;
import io.github.solf.extra2.util.ReflectionUtil;

/**
 * 'Database' based on Kryo serialization mechanism.
 * 
 * Might want to cache saves in-memory until shutdown -- but performance gain from that is questionable.
 * 
 * NOTE: this uses slightly modified version of {@link CompatibleFieldSerializer}
 * for Kryo -- specifically it has better support for obsolete fields. If you
 * have obsolete fields and you want to remove them, you should instead move them
 * to an inner class like this:
	public static class TestKryoData
	{
		public String aaaa = "aaaa";
//		public String bbbb = "bbbb";
		public String cccc = "cccc";
//		public String dddd = bbbb;
		public String dddd = "dddd";
		
		private static class ObsoleteKryoFields
		{
			public String bbb;
		}
	}
 * 
 * If you need access to obsolete fields (e.g. for data migration), declare field:
 * private transient ObsoleteKryoFields obsoleteKryoFields;
 * (this also works across superclasses -- that's the reason for using 'private' modifier).
 * 
 * After you re-save your data with obsolete fields migrated, you can then
 * remove them entirely. Removing obsolete fields WITHOUT moving them to
 * inner class is very-very RISKY -- it is possible that deserialization will
 * crash -- or worse yet it might not crash but deserialize incorrectly -- this
 * has to do with lack of field information in serialized data. 
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class KryoDB<T>
{
	/**
	 * File object for the actual data file to be used.
	 */
	private final File file;
	
	/**
	 * File object for the backup data file to be used.
	 */
	private final File fileBak;
	
	/**
	 * File object for the second backup to be used.
	 */
	private final File fileBak2;
	
	/**
	 * Whether to use compression.
	 */
	private final boolean useCompression;
	
	/**
	 * Kryo serializer -- NOT thread safe.
	 */
	private final Kryo kryo;
	
	/**
	 * File system lock (if used).
	 * Only here so it is not garbage-collected.
	 */
	@SuppressWarnings("unused")
	@Nullable
	private final FileLock dbFileLock;
	
	/**
	 * Object graph config used to walk data graph in case of the need to upgrade
	 * stored data.
	 */
	private ObjectGraphConfig objectGraphConfig = new ObjectGraphConfig();

	/**
	 * Constructor.
	 * 
	 * Does not use compression.
	 * 
	 * Will NOT create missing dirs in the pass (fails instead).
	 * WILL NOT LOCK THE DIRECTORY.
	 * 
	 * @param fileName name of the file to store data in; this will also make
	 * 			use of .bak file (with .bak appended to the given file name) and
	 * 			also will use .bak2 for brief periods
	 * 
	 * @throws IllegalStateException if file system state doesn't appear to be valid
	 * 		(e.g. no access, or previous saves don't appear to have completed successfully)
	 */
	public KryoDB(String fileName) throws IllegalStateException
	{
		this(fileName, false);
	}
	
	/**
	 * Constructor.
	 * 
	 * Does not use compression.
	 * 
	 * WILL NOT LOCK THE DIRECTORY.
	 * 
	 * @param fileName name of the file to store data in; this will also make
	 * 			use of .bak file (with .bak appended to the given file name) and
	 * 			also will use .bak2 for brief periods
	 * @param createDirsIfMissing if true, will attempt to create missing
	 * 			directories (if any)
	 * 
	 * @throws IllegalStateException if file system state doesn't appear to be valid
	 * 		(e.g. no access, or previous saves don't appear to have completed successfully)
	 */
	public KryoDB(String fileName, boolean createDirsIfMissing) throws IllegalStateException
	{
		this(fileName, createDirsIfMissing, false);
	}
	
	/**
	 * Constructor.
	 * 
	 * Does not use compression.
	 * 
	 * @param fileName name of the file to store data in; this will also make
	 * 			use of .bak file (with .bak appended to the given file name) and
	 * 			also will use .bak2 for brief periods
	 * @param createDirsIfMissing if true, will attempt to create missing
	 * 			directories (if any)
	 * @param lockDirectory if true, the directory containing data files will
	 * 			be locked (intended to prevent concurrent access by multiple
	 * 			instances); if directory is already locked (by e.g. another
	 * 			instance) -- will fail
	 * 
	 * @throws IllegalStateException if file system state doesn't appear to be valid
	 * 		(e.g. no access, or previous saves don't appear to have completed successfully)
	 */
	public KryoDB(String fileName, boolean createDirsIfMissing, boolean lockDirectory) throws IllegalStateException
	{
		this(false, fileName, createDirsIfMissing, lockDirectory);
	}

	/**
	 * Constructor.
	 * 
	 * Does not use compression.
	 * 
	 * Will NOT create missing dirs in the pass (fails instead).
	 * WILL NOT LOCK THE DIRECTORY.
	 * 
	 * @param fileName name of the file to store data in; this will also make
	 * 			use of .bak file (with .bak appended to the given file name) and
	 * 			also will use .bak2 for brief periods
	 * 
	 * @throws IllegalStateException if file system state doesn't appear to be valid
	 * 		(e.g. no access, or previous saves don't appear to have completed successfully)
	 */
	public KryoDB(boolean useCompression, String fileName) throws IllegalStateException
	{
		this(useCompression, fileName, false);
	}
	
	/**
	 * Constructor.
	 * 
	 * Does not use compression.
	 * 
	 * WILL NOT LOCK THE DIRECTORY.
	 * 
	 * @param fileName name of the file to store data in; this will also make
	 * 			use of .bak file (with .bak appended to the given file name) and
	 * 			also will use .bak2 for brief periods
	 * @param createDirsIfMissing if true, will attempt to create missing
	 * 			directories (if any)
	 * 
	 * @throws IllegalStateException if file system state doesn't appear to be valid
	 * 		(e.g. no access, or previous saves don't appear to have completed successfully)
	 */
	public KryoDB(boolean useCompression, String fileName, boolean createDirsIfMissing) throws IllegalStateException
	{
		this(useCompression, fileName, createDirsIfMissing, false);
	}
	
	/**
	 * Constructor.
	 * 
	 * @param fileName name of the file to store data in; this will also make
	 * 			use of .bak file (with .bak appended to the given file name) and
	 * 			also will use .bak2 for brief periods
	 * @param useCompression whether or not to use gzip compression on the data
	 * @param createDirsIfMissing if true, will attempt to create missing
	 * 			directories (if any)
	 * @param lockDirectory if true, the directory containing data files will
	 * 			be locked (intended to prevent concurrent access by multiple
	 * 			instances); if directory is already locked (by e.g. another
	 * 			instance) -- will fail
	 * 
	 * @throws IllegalStateException if file system state doesn't appear to be valid
	 * 		(e.g. no access, or previous saves don't appear to have completed successfully)
	 */
	public KryoDB(boolean useCompression, String fileName, boolean createDirsIfMissing, boolean lockDirectory) throws IllegalStateException
	{
		file = new File(fileName);
		fileBak = new File(fileName + ".bak");
		fileBak2 = new File(fileName + ".bak2");
		this.useCompression = useCompression;
		
		if (fileBak2.exists())
			throw new IllegalStateException("Second backup file exists -- probably indicates unclean shutdown -- please verify manually. File name is: " + fileBak2);
		
		if (file.exists())
		{
			if (!file.isFile())
				throw new IllegalStateException("Data file is not a file: " + file);
			if (!file.canWrite())
				throw new IllegalStateException("Data file is not writable: " + file);
			if (!file.canRead())
				throw new IllegalStateException("Data file is not readable: " + file);
		}
		else
		{
			if (fileBak.exists())
				throw new IllegalStateException("Data file doesn't exist, but backup file exists -- probably indicates unclean shutdown -- please verify manually. File name is: " + fileBak);
			
			if (createDirsIfMissing)
			{
				File parent = file.getParentFile();
				if (parent != null) // Guard against e.g. simple file name (w/o any path)
					parent.mkdirs();
			}
			
			try
			{
				if (!file.createNewFile())
					throw new IllegalStateException("Failed to create data file: " + file);
			} catch (IOException e)
			{
				throw new IllegalStateException("Failed to create data file: " + file + "; with exception: " + e, e);
			}
			if (!file.delete())
				throw new IllegalStateException("Failed to delete newly create data file: " + file);
		}
		
		if (lockDirectory)
		{
			File lockFile = new File(file.getParentFile(), ".lock");
			try
			{
				FileLock lock = tryLockFile(lockFile);
				if (lock == null)
					throw new IllegalStateException("Unable to acquire lock on database lock file -- probably lock held by another process.");
				else
					dbFileLock = lock;
			} catch (IOException e)
			{
				throw new IllegalStateException("Unable to lock database lock file [" + lockFile + "]: " + e, e);
			}
		}
		else
		{
			dbFileLock = null;
		}
		
		kryo = createDefaultKryoInstance();
	}

	/**
	 * Attempts to lock a given file.
	 * <p>
	 * One reason to extract this to a method is to make warnings suppression
	 * work consistently across old/new Eclipse versions.
	 * 
	 * @param lockFile file to try locking
	 * @return {@link FileLock} on a successful lock or null if locking has
	 * 		failed (because of an overlapping lock)
	 */
	@SuppressWarnings("resource")
	@Nullable
	private FileLock tryLockFile(File lockFile) throws FileNotFoundException, IOException
	{
		FileOutputStream dbLockStream = new FileOutputStream(lockFile);
		FileLock lock = dbLockStream.getChannel().tryLock();
		
		if (lock == null)
		{
			dbLockStream.close(); // close output stream in case of failure
		}
		
		return lock;
	}
	
	/**
	 * Creates Kryo instance with default serialization settings.
	 * kryo.setDefaultSerializer(CompatibleFieldSerializer.class);
	 * kryo.addDefaultSerializer(LocalDate.class, KryoLocalDateSerializer.class);
	 */
	public static Kryo createDefaultKryoInstance()
	{
		Kryo kryo = new Kryo();
		
		// Set serializer that can handle added & removed fields (but can't handle type change).
		kryo.setDefaultSerializer(CompatibleFieldSerializer.class);
		
		// Register specific serializer(s).
		kryo.addDefaultSerializer(LocalDate.class, KryoLocalDateSerializer.class);
//		kryo.addDefaultSerializer(com.gargoylesoftware.htmlunit.util.Cookie.class, KryoHtmlUnitCookieSerializer.class);
//		kryo.addDefaultSerializer(org.openqa.selenium.Cookie.class, KryoSeleniumCookieSerializer.class);
		
		return kryo;
	}
	
	/**
	 * Sets default serializer to be used by the underlying Kryo instance.
	 * <p>
	 * Should be called before any persistence operations.
	 * <p>
	 * For example, you can use {@link CompatibleFieldSerializerWithSameNameFieldSupport} 
	 * to enable limited support for classes that have multiple fields with the same
	 * name in the class hierarchy.
	 */
	public synchronized void setDefaultSerializer(@SuppressWarnings("rawtypes") Class<? extends Serializer> serializer)
	{
		kryo.setDefaultSerializer(serializer);
	}
	
	/**
	 * Adds additional serializer to Kryo so that you can save/load custom classes.
	 * (e.g. Cookies in HtmlUnit/Selenium or w/e).
	 * Implementation simply invokes {@link Kryo#addDefaultSerializer(Class, com.esotericsoftware.kryo.Serializer)}
	 * 
	 * WARNING: must add all required serializers prior to invoking any other methods
	 */
	@NonNullByDefault({}) // To avoid constraints on K which causes annoyanced in invoking code.
	public synchronized <K> void addSerializer(@Nonnull Class<K> type, @Nonnull Class<? extends Serializer<K>> serializerClass)
	{
		kryo.addDefaultSerializer(type, serializerClass);
	}
	
	/**
	 * Saves (serializes) data into database.
	 * Argument may not be null.
	 * 
	 * @param version saved version of the objects; this is a way to declare for
	 * 		later loading the version of stored data so it might be upgraded if
	 * 		necessary, see {@link #loadData(int)}; must be positive
	 * 
	 * @throws IllegalArgumentException if data is null or version is not positive
	 * @throws IllegalStateException if there's an issue with the filesystem state
	 */
	public synchronized void saveData(final T data, int version) throws IllegalArgumentException, IllegalStateException, KryoException
	{
//		System.out.print(">");
		if (nullable(data) == null) // Don't know if calling code tracks for nulls
			throw new IllegalArgumentException("Data may not be null.");
		if (version < 1)
			throw new IllegalArgumentException("Version must be positive.");

		{
			// First backup existing data (if any).
			if (fileBak.exists())
				renameFileTo(fileBak, fileBak2);
			
			if (file.exists())
				renameFileTo(file, fileBak);
			
			if (fileBak2.exists())
				if (!fileBak2.delete())
					throw new IllegalStateException("Failed to delete [" + fileBak2 + "]");
		}
		
		// Now save data together with version.
		KryoDataWrapper<T> dataWrapper = new KryoDataWrapper<T>();
		dataWrapper.version = version;
		dataWrapper.data = data;
		
		try (OutputStream fos = new FileOutputStream(file))
		{
			@SuppressWarnings("resource")
			OutputStream os = fos;
			if (useCompression)
				os = new GZIPOutputStream(os);
			@SuppressWarnings("resource")
			Output output = new Output(os);
			kryo.writeObject(output, dataWrapper);
			output.close();
		} catch (FileNotFoundException e)
		{
			throw new IllegalStateException("Failed to write to data file [" + file + "]: " + e, e);
		} catch (IOException e)
		{
			throw new IllegalStateException("IOException writing to data file [" + file + "]: " + e, e);
		}
//		System.out.print(".");
	}
	
	/**
	 * Renames given file to another name. Makes multiple attempts as as least
	 * on my machine it doesn't always succeed //Solf
	 * 
	 * @throws IllegalStateException if rename fails
	 */
	private void renameFileTo(File src, File dest) throws IllegalStateException
	{
		try
		{
			Files.move(src.toPath(), dest.toPath());
		} catch (IOException e)
		{
			throw new IllegalStateException("Failed to rename [" + src + "] to [" + dest + "]: " + e, e);
		}
	}
	
	/**
	 * Loads (deserializes) data from database.
	 * Never returns null.
	 * 
	 * @throws IllegalStateException if there's an issue with the filesystem state
	 * @throws KryoNoDataException if there appears to be no stored Kryo data
	 * @throws KryoException if other deserialization errors occur
	 */
	@SuppressWarnings("resource")
	private synchronized KryoDataWrapper<T> loadWrapper() throws KryoNoDataException, IllegalStateException, KryoException
	{
//		System.out.print("<");
		if (!file.exists())
			throw new KryoNoDataException();
		if (!file.isFile())
			throw new IllegalStateException("Data file is not a file: " + file);
		if (file.length() == 0)
			throw new KryoNoDataException();
		
		KryoDataWrapper<T> dataWrapper;
		try (InputStream fis = new FileInputStream(file))
		{
			InputStream is = fis;
			if (useCompression)
				is = new GZIPInputStream(is);
			Input input = new Input(is);
			dataWrapper = nn(kryo.readObject(input, KryoDataWrapper.class));
			input.close();
		} catch( FileNotFoundException e )
		{
			throw new IllegalStateException("Failed to read data from file [" + file + "]: " + e, e);
		} catch( IOException e )
		{
			throw new IllegalStateException("IOException reading data from file [" + file + "]: " + e, e);
		}
		
//		System.out.print(".");
		return dataWrapper;
	}
	
	/**
	 * Loads (deserializes) data from database.
	 * Never returns null.
	 * 
	 * @param version expected version of the objects; this is a way to declare
	 * 		that loaded objects need upgrade (if stored data has lower version) 
	 * 		or to quickly fail if old code is run against data with newer structure;
	 * 		if required, data upgrade is carried out in-memory via {@link ObjectGraphUtil},
	 * 		see {@link #setObjectGraphConfig(ObjectGraphConfig)}
	 * 		see {@link KryoUpgradeListener}
	 * 
	 * @throws IllegalArgumentException if requested version is less than the one
	 * 		stored in database (i.e. doesn't allow to load newer data with older code
	 * 		to prevent corruption)
	 * @throws IllegalStateException if there's an issue with the filesystem state
	 * 		or if there's problem during data upgrade (e.g. interface is implemented
	 * 		but the corresponding upgrade method is not found or method throws
	 * 		exception)
	 * @throws KryoNoDataException if there appears to be no stored Kryo data
	 * @throws KryoException if other deserialization errors occur
	 */
	public synchronized T loadData(int version) throws KryoNoDataException, IllegalStateException, IllegalArgumentException, KryoException
	{
		KryoDataWrapper<T> dataWrapper = loadWrapper();
		
		if (dataWrapper.version > version)
			throw new IllegalArgumentException("Stored data has version [" + dataWrapper.version + "] which is higher than version supported by the calling code [" + version + "].");
		
		@Nullable T data = dataWrapper.data;
		if (data == null)
			throw new KryoNoDataException();
		
		if (dataWrapper.version < version)
		{
			// Data needs update.
			final Integer upgradeFromVersion = dataWrapper.version;
			ObjectGraphUtil.visitCompoundNodesIncludingRoot(data, objectGraphConfig,
				new ObjectGraphCompoundNodeVisitor()
				{
					@Override
					public void visit(Object compoundNode)
					{
						if (compoundNode instanceof KryoUpgradeListener)
						{
							List<Method> methods = findMethods(compoundNode, "kryoUpgrade", Integer.TYPE);
							if (methods.size() == 0)
								throw new IllegalStateException("Class [" + compoundNode.getClass().getCanonicalName() + "] implements " + KryoUpgradeListener.class.getSimpleName() + ", but doesn't define the required method kryoUpgrade(int).");
							
							// Invoke methods in reverse order -- from the parent to the child.
							for (int i = methods.size() - 1; i >= 0; i--)
							{
								try
								{
									methods.get(i).invoke(compoundNode, upgradeFromVersion);
								} catch (Exception e)
								{
									String compoundNodeString;
									try
									{
										compoundNodeString = compoundNode.toString();
									} catch (Exception e1)
									{
										compoundNodeString = "[exception in toString(): " + e1 + "]";
									}
									throw new IllegalStateException("Failure trying to upgrade class [" + compoundNode.getClass().getCanonicalName() + "], instance [" + compoundNodeString + "]: " + e, e);
								}
							}
						}
					}
				}
			);
			
			// Post-upgrade (if required).
			if (data instanceof KryoPostUpgradeListener)
			{
				List<Method> methods = findMethods(data, "kryoPostUpgrade", Integer.TYPE);
				if (methods.size() == 0)
					throw new IllegalStateException("Class [" + data.getClass().getCanonicalName() + "] implements " + KryoPostUpgradeListener.class.getSimpleName() + ", but doesn't define the required method kryoPostUpgrade(int).");
				
				// Invoke methods in reverse order -- from the parent to the child.
				for (int i = methods.size() - 1; i >= 0; i--)
				{
					try
					{
						methods.get(i).invoke(data, upgradeFromVersion);
					} catch (Exception e)
					{
						throw new IllegalStateException("Failure trying to post-upgrade class [" + data.getClass().getCanonicalName() + "], instance [" + data + "]: " + e, e);
					}
				}
			}
		}
		
		return data;
	}
	
	/**
	 * Gets stored data version.
	 * 
	 * It can also be used to check whether there's stored data (via exception)
	 * 
	 * @throws IllegalStateException if there's an issue with the filesystem state
	 * @throws KryoNoDataException if there appears to be no stored Kryo data
	 * @throws KryoException if other deserialization errors occur
	 */
	public synchronized int getStoredDataVersion() throws KryoNoDataException, IllegalStateException, KryoException
	{
		KryoDataWrapper<T> dataWrapper = loadWrapper();
		
		if (dataWrapper.data == null)
			throw new KryoNoDataException();
		
		return dataWrapper.version;
	}
	
	
	/**
	 * Shutdown database.
	 * Highly desirable to call this method -- this ensures a clean shutdown.
	 * Although no irrecoverable error must occur if this method is not called.
	 * After invocation of this method, no other methods related to this database
	 * instance should be called.
	 */
	public void shutdown()
	{
		// Currently actually does nothing.
	}
	
	/**
	 * Sets object graph config used to walk data graph in case of the need to upgrade
	 * stored data.
	 * If not explicitly set, then unconfigured {@link ObjectGraphConfig} instance is used
	 * (i.e. defaults to whatever defaults {@link ObjectGraphConfig} is using).
	 */
	public synchronized void setObjectGraphConfig(ObjectGraphConfig objectGraphConfig)
	{
		this.objectGraphConfig = objectGraphConfig;
	}
	
	/**
	 * Finds specific method(s) in the given object instance.
	 * Will find all matches -- in case method is declared in multiple places (in parent and child and etc.).
	 * Will find non-public stuff -- which will be made accessible.
	 * 
	 * @return list of found methods -- ordered from the most specific class to
	 * 		java.lang.Object -- or empty list if none found
	 */
	private List<Method> findMethods(Object obj, String methodName, Class<?>... args)
	{
		Class<?> clazz = obj.getClass();
		if (clazz.isArray())
			throw new UnsupportedOperationException("Searching in arrays not supported, got: " + clazz);
		
		List<Method> result = new ArrayList<Method>();
		
		for (;clazz != null; clazz = clazz.getSuperclass())
		{
			try
			{
				Method method = clazz.getDeclaredMethod(methodName, args);
				if (!ReflectionUtil.isAccessible(method))
					method.setAccessible(true);
					
				result.add(method);
			} catch (NoSuchMethodException e)
			{
				// Okay, try next class.
			}		
		}
		
		return result;
	}
}
