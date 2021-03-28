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
package com.github.solf.extra2.util;

import static com.github.solf.extra2.util.NullUtil.nullable;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * ASCII utilities.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class ASCIIUtil
{
	/**
	 * Checks whether the supplied character is a letter or number.
	 */
	public static boolean isLetterOrNumber(int c)
	{
		return isLetter(c) || isNumber(c);
	}

	/**
	 * Checks whether the supplied character is a letter.
	 */
	public static boolean isLetter(int c)
	{
		return isUpperCaseLetter(c) || isLowerCaseLetter(c);
	}

	/**
	 * Checks whether the supplied character is an upper-case letter.
	 */
	public static boolean isUpperCaseLetter(int c)
	{
		return (c >= 65 && c <= 90); // A - Z
	}

	/**
	 * Checks whether the supplied character is an lower-case letter.
	 */
	public static boolean isLowerCaseLetter(int c)
	{
		return (c >= 97 && c <= 122); // a - z
	}

	/**
	 * Checks whether the supplied character is a number
	 */
	public static boolean isNumber(int c)
	{
		return (c >= 48 && c <= 57); // 0 - 9
	}
	
	/**
	 * Encodes string into exclusively ASCII letters, digits, and _ symbols.
	 * Furthermore string is guaranteed to start with a letter -- this is important
	 * in e.g. using these strings as identifiers.
	 * It also attempts to preserve readability (so that at least original line
	 * can be more or less read). It does so by replacing all of the non-letter/non-digit
	 * characters with _ and then at the end of the string recording data about
	 * these special characters, so you get something like this:
	 * This_is_a_text_[encoded data about special characters]
	 * 
	 * The data recorded about special characters at the end is as follows:
	 * - Items are delimited by _
	 * - Space is recorded as empty string (i.e. the result would be something like __)
	 * - Other special characters are encoded as their int code, e.g. _123_
	 * - Letters are used to record special conditions -- e.g. a_ denotes that
	 *   string was prefixed with 'a'. _e denotes end of the string.
	 * The resulting data about special characters is inverted and glued to the
	 * end (so that decoding can be done by reading the string from the start and
	 * the end simultaneously).
	 */
	public static String encodeUnderscore(String source) throws IllegalArgumentException
	{
		if (nullable(source) == null)
			throw new IllegalArgumentException("Source string may not be null.");
		
		StringBuilder head = new StringBuilder(source.length() * 150 / 100); // Guess size
		StringBuilder tail = new StringBuilder(source.length() * 50 / 100); // Guess size
		
		boolean firstChar = true;
		for (char c : source.toCharArray())
		{
			if (firstChar)
			{
				// First character must be letter.
				if (!isLetter(c))
				{
					head.append('a');
					tail.append("a_");
				}
			}
			firstChar = false;
			
			if (isLetterOrNumber(c))
			{
				head.append(c);
				continue;
			}
			
			head.append('_');
			switch (c)
			{
				case ' ':
					tail.append('_');
					break;
				default:
					tail.append(Integer.toString(c));
					tail.append('_');
			}
		}

		// Special-case for empty string.
		if (firstChar)
		{
			// First character must be letter.
			head.append('a');
			tail.append("a_");
		}
		
		// Write out 'end of the string'
		head.append('_');
		tail.append('e');
		
		// Combine result.
		head.append(tail.reverse());
		
		return head.toString();
	}
	
	/**
	 * Decode from {@link #encodeUnderscore(String)}
	 * 
	 * @throws IllegalArgumentException if argument is null or string cannot be decoded
	 */
	public static String decodeUnderscore(String source) throws IllegalArgumentException
	{
		if (nullable(source) == null)
			throw new IllegalArgumentException("Source string may not be null.");
		
		StringBuilder result = new StringBuilder(source.length());
		String reverseSource = new StringBuilder(source).reverse().toString();
		int idx = 0;
		String[] tokens = reverseSource.split("_");
		
		boolean skipChar = false; // Whether to skip next character.
		if ("a".equals(tokens[idx]))
		{
			idx++;
			skipChar = true;
		}
		
		for (char c : source.toCharArray())
		{
			if (skipChar)
			{
				skipChar = false;
				continue;
			}
			
			if (isLetterOrNumber(c))
			{
				result.append(c);
				continue;
			}
			
			if (c != '_')
			{
				// Only letters, numbers and underscores are allowed.
				throw new IllegalArgumentException("Illegal character [" + c + "] encountered while decoding: " + source);
			}
			
			// We have special character.
			String token = tokens[idx++];
			
			if (token.isEmpty())
			{
				// Empty tokens are used for spaces.
				result.append(' ');
				continue;
			}
			
			if ("e".equals(token))
			{
				// End of the line -- decoding seems fine.
				break;
			}
			
			// Otherwise token must represent character code.
			try
			{
				result.append( (char)Integer.parseInt(token) );
			} catch (NumberFormatException e)
			{
				throw new IllegalArgumentException("Failed to decode string [" + source + "] at attempting to convert token [" + token + "] to character code: " + e, e);
			}
		}
		
		return result.toString();
	}
}
