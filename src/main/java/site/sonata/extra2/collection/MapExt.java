/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.collection;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Extension interface for {@link Map}
 * 
 * Currently provides #get
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public interface MapExt<K, V> extends Map<K, V>
{
	/**
	 * Gets entry by key.
	 * 
	 * TYPICAL IMPLEMENTATION USES REFLECTION!!!
	 * 
	 * @throws IllegalStateException if reflection fails for some reasons (shouldn't really happen)
	 */
	@Nullable
	public Entry<K, V> getEntry(K key) throws IllegalStateException;
	
	/**
	 * Gets entry by key -- if entry is not present or is null, then creates
	 * the value using provided factory and stores it in the map (and then returns
	 * the created value).
	 */
	public V get(K key, Supplier<@Nonnull V> factory);
	
	/**
	 * Sets factory used for automatically creating new entries via {@link #getOrCreate(Object)}
	 * <p>
	 * If factory is used to create an entry, it gets requested key value as argument 
	 * 
	 * @return this, for convenient chaining
	 */
	public MapExt<K, V> setFactory(Function<K, @Nonnull V> factory);
	
	/**
	 * Gets entry by key -- if entry is not present or is null, then creates
	 * the value using previously set factory (either in contructor or in 
	 * {@link #setFactory(Supplier)} method) and stores it in the map (and then returns
	 * the created value).
	 * 
	 * @throws IllegalStateException if factory is not previously set
	 */
	public V getOrCreate(K key) throws IllegalStateException;
}
