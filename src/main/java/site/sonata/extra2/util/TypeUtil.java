/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Utilities for working with classes / types.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class TypeUtil
{
	/**
	 * Coerces any object to the requested type -- both argument and result
	 * must be non-null.
	 * 
	 * E.g.:
	 * T result = TypeUtil.coerce(new SomethingThatIsActuallyT());
	 */
	@SuppressWarnings("unchecked")
	public static <K> @Nonnull K coerce(@Nonnull Object obj)
	{
		return (K)obj;
	}
	
	/**
	 * Coerces any object to the requested type -- argument may be nullable or
	 * unknown, but result is coerced to be non-null -- USE WITH CAUTION.
	 * 
	 * E.g.:
	 * T result = TypeUtil.coerce(new SomethingThatIsActuallyT());
	 */
	@SuppressWarnings("unchecked")
	@ParametersAreNonnullByDefault({})
	public static <@Nonnull K> @Nonnull K coerceForceNonnull(Object obj)
	{
		return (K)obj;
	}
	
	/**
	 * Coerces any object to the requested type -- both argument and result
	 * are nullable.
	 * 
	 * E.g.:
	 * T result = TypeUtil.coerce(new SomethingThatIsActuallyT());
	 */
	@SuppressWarnings("unchecked")
	@Nullable 
	public static <K> K coerceNullable(@Nullable Object obj)
	{
		return (K)obj;
	}
	
	/**
	 * Coerces any object to the requested type (with unknown nullability) --
	 * useful with e.g. type variables of unknown nullability.
	 * 
	 * E.g.:
	 * T result = TypeUtil.coerce(new SomethingThatIsActuallyT());
	 */
	@SuppressWarnings("unchecked")
	@ParametersAreNonnullByDefault({})
	public static <K> K coerceUnknown(Object obj)
	{
		return (K)obj;
	}
}
