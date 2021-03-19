/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.cache.wbrb;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Possible cache message severities.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public enum WBRBCacheMessageSeverity
{
	DEBUG,
	INFO,
	/**
	 * Indicates problem that is probably caused by internal somewhat-known
	 * factors, such as potential concurrency/race conditions (which normally
	 * are not expected to occur).
	 * <p>
	 * These usually should not result in data loss.
	 */
	WARN,
	/**
	 * Indicates an info message probably caused by external factors, such
	 * as read retry executing.
	 * <p>
	 * These messages usually indicate that there was no data loss (yet).
	 */
	EXTERNAL_INFO,
	/**
	 * Indicates an externally-caused warning.
	 * <p>
	 * These messages usually indicate that there was no data loss (yet).
	 */
	EXTERNAL_WARN,
	/**
	 * Indicates an error probably caused by external factors, such
	 * as underlying storage failing.
	 * <p>
	 * These messages usually indicate that there was no data loss (yet).
	 */
	EXTERNAL_ERROR,
	/**
	 * Indicates an error probably caused by external factors, such
	 * as underlying storage failing.
	 * <p>
	 * This is used when data loss is highly likely, e.g. when cache implementation
	 * gives up on writing piece of data to the underlying storage.
	 */
	EXTERNAL_DATA_LOSS,
	/**
	 * Indicates an error which is likely to be caused by the 
	 * problems and/or unexpected behavior in the cache code itself.
	 * <p>
	 * Data loss is likely although this should not be fatal.
	 */
	ERROR,
	/**
	 * Indicates a likely fatal error, meaning cache may well become unusable
	 * after this happens. 
	 */
	FATAL,
	;
}
