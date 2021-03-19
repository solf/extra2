/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.log;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.slf4j.helpers.MessageFormatter;

/**
 * Helper methods for SLF4J.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class Slf4jHelper
{
	/**
	 * Wrapper around {@link MessageFormatter#arrayFormat(String, Object[])}
	 * that has compatible behavior across SLF4J versions.
	 */
	@Nullable
	public static String messageArrayFormat(final String messagePattern,
	      final Object[] args)
	{
		// Format message.
		Object[] finalArgs = args;
		if ((finalArgs.length > 0) && (finalArgs[finalArgs.length - 1] instanceof Throwable))
		{
			// SLF4J 1.8.0-beta4 has a 'feature' where it'll remove last item
			// from args array if it is a Throwable.
			// So work around that by adding String "{}" which substitutes to {}
			// -- same as if it wasn't present at all.
			finalArgs = new @Nonnull Object[args.length + 1];
			System.arraycopy(args, 0, finalArgs, 0, args.length);
			finalArgs[args.length] = "{}";
		}
			
		return MessageFormatter.arrayFormat(messagePattern, finalArgs).getMessage();
	}
}
