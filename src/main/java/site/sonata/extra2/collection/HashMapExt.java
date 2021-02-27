/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.collection;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * An extension to {@link HashMap}
 *
 * Currently provides {@link #getEntry(Object)} method.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class HashMapExt<K, V> extends HashMap<K, V> implements MapExt<K, V>
{
	/**
	 * Factory used for creating entries in {@link #getOrCreate(Object)}
	 */
	@Nullable
	private Function<K, @Nonnull V> defaultFactory; 
	
	/**
	 * 
	 */
	public HashMapExt()
	{
		super();
	}
	
	/**
	 * Creates an instance and initializes factory to be used in {@link #getOrCreate(Object)} 
	 */
	public HashMapExt(Function<K, @Nonnull V> factory)
	{
		super();
		setFactory(factory);
	}

	/**
	 * @param initialCapacity
	 * @param loadFactor
	 */
	public HashMapExt(int initialCapacity, float loadFactor)
	{
		super(initialCapacity, loadFactor);
	}

	/**
	 * @param initialCapacity
	 */
	public HashMapExt(int initialCapacity)
	{
		super(initialCapacity);
	}

	/**
	 * @param m
	 */
	public HashMapExt(Map<? extends K, ? extends V> m)
	{
		super(m);
	}
	
	/* (non-Javadoc)
	 * @see site.sonata.extra2.collection.ExtendedMap#getEntry(java.lang.Object)
	 */
	@Override
	@Nullable
	public Entry<K, V> getEntry(K key)
		throws IllegalStateException
	{
		return WACollections.getEntry(this, key);
	}

	/* (non-Javadoc)
	 * @see site.sonata.extra2.collection.MapExt#get(java.lang.Object, java.util.function.Supplier)
	 */
	@Override
	public V get(K key, @Nonnull Supplier<@Nonnull V> factory)
	{
		@Nullable V result = get(key);
		if (result == null)
		{
			result = factory.get();
			put(key, result);
		}
		
		return result;
	}

	/* (non-Javadoc)
	 * @see site.sonata.extra2.collection.MapExt#setFactory(java.util.function.Function)
	 */
	@Override
	public MapExt<K, V> setFactory(Function<K, @Nonnull V> factory)
	{
		this.defaultFactory = factory;
		
		return this;
	}

	/* (non-Javadoc)
	 * @see site.sonata.extra2.collection.MapExt#getOrCreate(java.lang.Object)
	 */
	@Override
	public V getOrCreate(K key)
		throws IllegalStateException
	{
		Function<K, @Nonnull V> f = defaultFactory;
		
		if (f == null)
			throw new IllegalStateException("getOrCreate called when factory is not set!");
		
		@Nullable V result = get(key);
		if (result == null)
		{
			result = f.apply(key);
			put(key, result);
		}
		
		return result;
	}
	
	
}
