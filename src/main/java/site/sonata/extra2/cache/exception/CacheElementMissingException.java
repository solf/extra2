/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.cache.exception;

import static site.sonata.extra2.util.NullUtil.nullable;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import site.sonata.extra2.util.TypeUtil;

/**
 * Thrown to indicate that cache element with the particular key is not present.
 * FIXME not used? remove?
 *
 * @param K key type
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class CacheElementMissingException extends CacheIllegalStateException
{
	/**
	 * Key for which exception is generated.
	 */
	protected final Object key;
	
	/**
	 * Constructor.
	 * 
	 * @param key must be non-null
	 */
	public <@Nonnull K> CacheElementMissingException(String cacheName, K key)
	{
		super("Cache [" + cacheName + "] element missing: {{==}}"); // do not toString() key unnecessarily, might be expensive, instead do this in getMessage() 
		
		this.key = key;
		if (nullable(key) == null)
			throw new IllegalArgumentException("Key may not be null");
	}
	
	/**
	 * Gets key for this exception.
	 */
	public <@Nonnull K> K getKey()
	{
		return TypeUtil.coerce(key);
	}

	@Override
	public String getMessage()
	{
		return super.getMessage().replace("{{==}}", key.toString());
	}
	
	
}
