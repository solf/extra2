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
package io.github.solf.extra2.log;

import static io.github.solf.extra2.util.NullUtil.nnChecked;

import javax.annotation.Nonnull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.helpers.MessageFormatter;

import lombok.NonNull;

/**
 * Helper methods for SLF4J.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class Slf4jHelper
{
	/**
	 * Wrapper around {@link MessageFormatter#arrayFormat(String, Object[])}
	 * that has compatible behavior across SLF4J versions.
	 */
	@NonNullByDefault({})
	@Nonnull
	public static String messageArrayFormat(final @Nonnull @NonNull String messagePattern,
	      final Object @Nonnull [] args)
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
			
		return nnChecked(MessageFormatter.arrayFormat(messagePattern, finalArgs).getMessage());
	}
}
