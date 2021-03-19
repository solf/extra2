/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.cache.exception;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Thrown to indicate that cache operation cannot be performed because cache
 * [control] state is invalid, e.g. it is shutdown, not started, or similar.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class CacheControlStateException extends CacheIllegalStateException
{
	/**
	 * Constructor.
	 */
	public CacheControlStateException(String cacheName, String message)
	{
		super("Cache [" + cacheName + "]: " + message);
	}
}
