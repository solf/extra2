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
package io.github.solf.extra2.util;

import static io.github.solf.extra2.util.NullUtil.nn;
import static io.github.solf.extra2.util.NullUtil.nnc;
import static io.github.solf.extra2.util.NullUtil.nullable;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.NonNullByDefault;

/**
 * Utilities for splitting / merging strings.
 * The main feature of this class is that values are merged in such a way that
 * they are guaranteed unique (that is there's no different combination of strings
 * that after merge result in the same merged value).
 * This is achieved by prepending string length for each string being merged.
 * 
 * These properties make this class useful for producing unique single keys based on
 * multiple strings.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class SplitUtil
{
	/**
	 * Empty String array for typecasting.
	 */
	private static final @Nonnull String[] EMPTY_STRING_ARRAY = new @Nonnull String[] {};
	
	/**
	 * 'Encodes' string by prepending it with its length separated by '.', i.e
	 * "asd" --> "3.asd"
	 * This method is used by append/merge methods in this class.
	 * 
	 * @throws IllegalArgumentException if string is null
	 */
	public static String encodeString(String string) throws IllegalArgumentException
	{
		if (nullable(string) == null)
			throw new IllegalArgumentException("Argument may not be null.");
		
		return string.length() + "." + string;
	}
	
	/**
	 * Decodes string encoded via {@link #encodeString(String)}
	 * 
	 * @throws IllegalArgumentException if string cannot be parsed
	 */
	public static String decodeString(String encodedString) throws IllegalArgumentException
	{
		@Nullable String split[] = internalSplitHead(encodedString);
		if (split[1] == null)
			return nn(split[0]);
		
		throw new IllegalArgumentException("Unable to decode string: " + encodedString);
	}
	
	/**
	 * Appends head to a given string (which is expected to be already encoded).
	 * Format: <head length>.<head><delimiter character><tail>
	 * 
	 * @throws IllegalArgumentException if either string is null
	 */
	public static String appendHead(String head, char delimiter, String encodedTail)
		throws IllegalArgumentException
	{
		if (nullable(encodedTail) == null)
			throw new IllegalArgumentException("Tail may not be null.");
		return encodeString(head) + delimiter + encodedTail;
	}
	
	/**
	 * Splits head from the given string that was encoded via {@link #appendHead(String, char, String)}
	 * or {@link #mergeStrings(char, String...)}
	 * 
	 * @return array, [0]=head, [1]=tail
	 * 
	 * @throws IllegalArgumentException if string cannot be parsed or is null
	 */
	public static String[] splitHead(String encodedString)
		throws IllegalArgumentException
	{
		@Nullable String[] result = internalSplitHead(encodedString);
		if (result[1] == null) // This special case is not allowed in case of actually splitting head.
			throw new IllegalArgumentException("String doesn't parse: " + encodedString);
		
		return nnc(result);
	}
	
	/**
	 * (INTERNAL) Splits head from the given string that was encoded via {@link #appendHead(String, char, String)}
	 * As a special case will return [0]=head, [1]= null if given string is simply
	 * a string encoded via {@link #encodeString(String)}
	 * 
	 * @return array, [0]=head, [1]=tail
	 * 
	 * @throws IllegalArgumentException if string cannot be parsed or is null
	 */
	private static @Nullable String[] internalSplitHead(String encodedString)
		throws IllegalArgumentException
	{
		if (nullable(encodedString) == null)
			throw new IllegalArgumentException("Argument may not be null.");
		
		int length = 0;
		int i;
		boolean hadDigits = false;
		for (i = 0; i < encodedString.length(); i++)
		{
			char c = encodedString.charAt(i);
			if (ASCIIUtil.isNumber(c))
			{
				length = length * 10 + (c - '0');
				hadDigits = true;
				continue;
			}
			else
			{
				if (c != '.')
					throw new IllegalArgumentException("String doesn't parse: " + encodedString);
			}
			
			// Here is actual parsing.
			if (!hadDigits)
				throw new IllegalArgumentException("String doesn't parse: " + encodedString);
			int startHead = i + 1;
			if (startHead > encodedString.length())
				throw new IllegalArgumentException("String doesn't parse: " + encodedString);
			if (startHead == encodedString.length())
			{
				// Special case, what if head is empty and there's no tail?
				if (length == 0)
					return new @Nullable String[] {"", null};
				else
					throw new IllegalArgumentException("String doesn't parse: " + encodedString);
			}
			
			final int startTail;
			String head;
			if (length == 0)
			{
				head = "";
				startTail = startHead + 1;
			}
			else
			{
				int endHead = startHead + length;
				if (endHead > encodedString.length())
					throw new IllegalArgumentException("String doesn't parse: " + encodedString);
				head = encodedString.substring(startHead, endHead);
				startTail = endHead + 1;
				
				// Special case for no tail.
				if (endHead == encodedString.length())
					return new @Nullable String[] {head, null};
			}
			
			if (startTail > encodedString.length())
				throw new IllegalArgumentException("String doesn't parse: " + encodedString);
			
			if (startTail == encodedString.length())
			{
				// Special case when tail is empty.
				return new @Nullable String[] {head, ""};
			}
			else
			{
				return new @Nullable String[] {head, encodedString.substring(startTail)};
			}
		}
		
		// If we exit cycle above -- it means we didn't find '.' delimiter.
		throw new IllegalArgumentException("String doesn't parse: " + encodedString);
	}
	
	/**
	 * Merges a number of strings with given delimiter.
	 * Format: <str1.length>.<str1><delim><str2.length>.<str2>...
	 * 
	 * Fails if there's less than two strings.
	 * 
	 * @throws IllegalArgumentException if either string argument is null or there's
	 * 		less than two string arguments
	 */
	public static String mergeStrings(char delimiter, @Nonnull String... strings)
		throws IllegalArgumentException
	{
		return mergeStrings(delimiter, true, strings);
	}
	
	
	/**
	 * Merges a number of strings with given delimiter.
	 * Format: <str1.length>.<str1><delim><str2.length>.<str2>...
	 * 
	 * @param failIfOnlyOneString whether to fail if argument contains only
	 * 		one string (if false, then it is the same as {@link #encodeString(String)}
	 * 		when only one argument supplied)
	 * 
	 * @throws IllegalArgumentException if either string argument is null or there's
	 * 		less than two string arguments
	 */
	public static String mergeStrings(char delimiter, boolean failIfOnlyOneString, @Nonnull String... strings)
		throws IllegalArgumentException
	{
		if (nullable(strings) == null)
			throw new IllegalArgumentException("Strings argument may not be null.");
		
		if (strings.length < 1)
			throw new IllegalArgumentException("Zero-length strings array given.");
		
		if ((strings.length < 2) && failIfOnlyOneString)
			throw new IllegalArgumentException("At least two argument strings must be specified.");
		
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String str : strings)
		{
			if (nullable(str) == null)
				throw new IllegalArgumentException("No string argument may be null.");
			
			if (first)
				first = false;
			else
				sb.append(delimiter);
			
			sb.append(encodeString(str));
		}
		
		return nn(sb.toString());
	}
	
	/**
	 * Merges a number of strings with given delimiter.
	 * Format: <str1.length>.<str1><delim><str2.length>.<str2>...
	 * 
	 * @param failIfOnlyOneString whether to fail if argument contains only
	 * 		one string (if false, then it is the same as {@link #encodeString(String)}
	 * 		when only one argument supplied)
	 * 
	 * @throws IllegalArgumentException if list is null; if any string in the list is 
	 * 		null; if there's less than two string arguments
	 */
	public static String mergeStrings(char delimiter, boolean failIfOnlyOneString, List<@Nonnull String> strings)
		throws IllegalArgumentException
	{
		if (nullable(strings) == null)
			throw new IllegalArgumentException("List argument may not be null.");
			
		return mergeStrings(delimiter, failIfOnlyOneString, strings.toArray(EMPTY_STRING_ARRAY));
	}	
	
	/**
	 * Merges a number of strings with given delimiter.
	 * Format: <str1.length>.<str1><delim><str2.length>.<str2>...
	 * 
	 * Fails if there's less than two strings.
	 * 
	 * @param failIfOnlyOneString whether to fail if argument contains only
	 * 		one string (if false, then it is the same as {@link #encodeString(String)}
	 * 		when only one argument supplied)
	 * 
	 * @throws IllegalArgumentException if list is null; if any string in the list is 
	 * 		null; if there's less than two string arguments
	 */
	public static String mergeStrings(char delimiter, List<@Nonnull String> strings)
		throws IllegalArgumentException
	{
		return mergeStrings(delimiter, true, strings);
	}	
	
	/**
	 * Splits a number of strings that were encoded via {@link #mergeStrings(char, String...)}
	 * 
	 * Fails if result is only one string (e.g. if it was obtained by {@link #encodeString(String)})
	 * 
	 * @throws IllegalArgumentException if string cannot be parsed or is null
	 */
	public static @Nonnull String[] splitStrings(String encodedString)
	{
		return splitStrings(encodedString, true);
	}
	
	/**
	 * Splits a number of strings that were encoded via {@link #mergeStrings(char, String...)}
	 * 
	 * @param failIfOnlyOneString whether to fail if the result contains only
	 * 		one string (e.g. it was obtained with {@link #encodeString(String)})
	 * 
	 * @throws IllegalArgumentException if string cannot be parsed or is null
	 */
	public static @Nonnull String[] splitStrings(String encodedString, boolean failIfOnlyOneString)
	{
		if (nullable(encodedString) == null)
			throw new IllegalArgumentException("Argument may not be null.");
		
		ArrayList<String> result = new ArrayList<String>();
		
		String remaining = encodedString;
		while(remaining != null)
		{
			@Nullable String[] split = internalSplitHead(remaining);
			result.add(nn(split[0]));
			remaining = split[1];
		}
		
		if ((result.size() < 1) || ((result.size() < 2) && failIfOnlyOneString))
			throw new IllegalArgumentException("Cannot split string: " + encodedString);
		
		return nn(result.toArray(EMPTY_STRING_ARRAY));
	}
}
