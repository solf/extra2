/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.options;

import static com.github.solf.extra2.util.NullUtil.nonNull;
import static com.github.solf.extra2.util.NullUtil.nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.javatuples.Pair;
import org.joda.time.DateTime;

import com.github.solf.extra2.config.Configuration;
import com.github.solf.extra2.config.FlatConfiguration;

/**
 * Base class for supporting options functionality, usage example can be found
 * in example source folder: {@link ExampleDbOptions}
 * <p>
 * Suggested usage instructions:
 * - make a subclass specific to your project / options
 * - declare public final variables for your options, e.g. like this:
 * public final String name = getString("name");
 * <p>
 * Another usage recommendation is to always use long (millisecond) values for
 * any time intervals -- those are produced via {@link #getTimeInterval(String)}
 * and if they are used uniformly there's no confusion about time unit used.
 * <p>
 * To actually load configuration files take a look at {@link Configuration}
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class BaseOptions
{
	/**
	 * Options set used by this instance.
	 */
	protected final FlatConfiguration configuration;
	
	/**
	 * Constructor.
	 * NOTE: this constructor doesn't directly throw exceptions, but any
	 * particular subclass most likely will -- so they are declared here for
	 * the case when you do 'generate constructors from superclass'.
	 */
	public BaseOptions(FlatConfiguration configuration)
		throws MissingResourceException, NumberFormatException
	{
		this.configuration = configuration;
	}
	
	/**
	 * Constructor.
	 * NOTE: this constructor doesn't directly throw exceptions, but any
	 * particular subclass most likely will -- so they are declared here for
	 * the case when you do 'generate constructors from superclass'.
	 */
	public BaseOptions(BaseOptions initializeFrom)
		throws MissingResourceException, NumberFormatException
	{
		this(initializeFrom.configuration);
	}
	
	/**
	 * Parses given time interval string to long millis value.
	 * 
	 * This method is smart enough to interpret various measurements, specifically:
	 * 35ms
	 * 35s
	 * 35m
	 * 35h
	 * 35d
	 * 
	 * NOTE: value cannot be negative, but it can be zero
	 * 
	 * @throws IllegalArgumentException if option value doesn't parse
	 */
	public static long parseTimeInterval(String intervalValue) throws IllegalArgumentException
	{
		if (intervalValue.length() < 2)
			throw new IllegalArgumentException("Cannot parse value as time interval: " + intervalValue);
		
		String strCount;
		String unit;
		if (intervalValue.endsWith("ms"))
		{
			unit = "ms";
			strCount = intervalValue.substring(0, intervalValue.length() - 2); 
		}
		else
		{
			unit = intervalValue.substring(intervalValue.length() - 1);
			strCount = intervalValue.substring(0, intervalValue.length() - 1); 
		}
		
		long count;
		try
		{
			count = Long.parseLong(strCount);
		} catch (NumberFormatException e)
		{
			throw new IllegalArgumentException("Cannot parse value as time interval: " + intervalValue, e);
		}
		
		if (count < 0)
			throw new IllegalArgumentException("Cannot parse value as time interval (must not be negative): " + intervalValue);
		
		long multiplier = 1;
		if (unit.equals("ms"))
		{
			return multiplier * count;
		}
		multiplier *= 1000;
		if (unit.equals("s"))
		{
			return multiplier * count;
		}
		multiplier *= 60;
		if (unit.equals("m"))
		{
			return multiplier * count;
		}
		multiplier *= 60;
		if (unit.equals("h"))
		{
			return multiplier * count;
		}
		multiplier *= 24;
		if (unit.equals("d"))
		{
			return multiplier * count;
		}
		
		throw new IllegalArgumentException("Cannot parse value as time interval: " + intervalValue);

	}
	
	/**
	 * Gets option as string or null if not defined.
	 * 
	 * NOTE: this method does not allow for empty option value in configuration --
	 * these are treated the same as missing options.
	 */
	@Nullable
	public String getStringOrNull(String option)
		throws IllegalArgumentException
	{
		try
		{
			return getString(option);
		} catch (MissingResourceException e)
		{
			return null;
		}
	}
	
	/**
	 * Gets option as string; fails if not found.
	 * 
	 * NOTE: this method does not allow for empty option value in configuration --
	 * these are treated the same as missing options.
	 * 
	 * @throws MissingResourceException if option value not found.
	 */
	public String getString(String option)
		throws MissingResourceException, IllegalArgumentException
	{
		String result = getStringPossiblyEmpty(option).trim();
		
		if (result.isEmpty())
			throw new MissingResourceException("Empty option value: " + option, null, option);
		
		return result;
	}
	
	/**
	 * Gets option as string; fails if not found.
	 * Option value is allowed to be empty.
	 * 
	 * @throws MissingResourceException if option value not found.
	 */
	public String getStringPossiblyEmpty(String option)
		throws MissingResourceException, IllegalArgumentException
	{
		if (nullable(option) == null)
			throw new IllegalArgumentException("Option must be non-null!");
		
		try
		{
			return configuration.getString(option);
		} catch (MissingResourceException e)
		{
			throw new MissingResourceException("Missing option: " + option, null, option);
		}
	}
	
	/**
	 * Gets option as string; returns provided 'default' value if not found.
	 * Option value is allowed to be empty.
	 * 
	 * @throws MissingResourceException if option value not found.
	 */
	public String getStringPossiblyEmpty(String option, String defaultValue)
		throws MissingResourceException, IllegalArgumentException
	{
		if (nullable(defaultValue) == null)
			throw new IllegalArgumentException("Default value must be non-null in option: " + option);
		
		try
		{
			return getStringPossiblyEmpty(option);
		} catch (MissingResourceException e)
		{
			return defaultValue;
		}
	}
	
	/**
	 * Gets option as string; returns provided 'default' value if not found.
	 * 
	 * NOTE: this method does not allow for empty option value in configuration --
	 * these are treated the same as missing options.
	 * 
	 * If you want to specify null default value, best use {@link #getStringOrNull(String)}
	 */
	public String getString(String option, String defaultValue)
		throws IllegalArgumentException
	{
		String result = getStringOrNull(option);
		if (result != null)
			return result;
		
		return defaultValue;
	}
	
	/**
	 * Gets options as a list of Strings; fails if not found or value cannot be parsed
	 * as a comma-separated list of Strings.
	 * <p>
	 * Returned list is unmodifiable.
	 * <p>
	 * ATTENTION: by default, resulting list is allowed to be empty
	 * <p>
	 * ATTENTION2: by default, elements of the resulting list are allowed to be empty
	 * 
	 * @param constraints additional constraints that you might want to add
	 * 		on allowed values
	 * 
	 * @throws MissingResourceException if option value not found.
	 * @throws IllegalStateException if incompatible constraints are used
	 */
	public List<String> getStringList(String option, @Nonnull OptionConstraint... constraints)
		throws MissingResourceException, IllegalStateException
	{
		return internalGetStringList(option, null, false, constraints);
	}
	
	/**
	 * Gets options as a list of Strings; fails if not found or value cannot be parsed
	 * as a comma-separated list of Strings.
	 * <p>
	 * Returned list is unmodifiable.
	 * <p>
	 * ATTENTION: by default, resulting list is allowed to be empty
	 * <p>
	 * ATTENTION2: by default, elements of the resulting list are allowed to be empty
	 * 
	 * @param constraints additional constraints that you might want to add
	 * 		on allowed values
	 * 
	 * @throws MissingResourceException if option value not found.
	 * @throws IllegalArgumentException if default value is null
	 * @throws IllegalStateException if incompatible constraints are used
	 */
	public List<String> getStringList(String option, String defaultValue, @Nonnull OptionConstraint... constraints)
		throws MissingResourceException, IllegalArgumentException, IllegalStateException
	{
		if (nullable(defaultValue) == null)
			throw new IllegalArgumentException("Default value must be non-null in option: " + option);
		
		return internalGetStringList(option, defaultValue, false, constraints);
	}
	
	/**
	 * Gets options as a set of Strings (unmodifiable, backed by {@link HashSet}); 
	 * fails if not found or value cannot be parsed as a comma-separated list of Strings.
	 * <p>
	 * Returned list is unmodifiable.
	 * <p>
	 * ATTENTION: by default, resulting set is allowed to be empty
	 * <p>
	 * ATTENTION2: by default, elements of the resulting set are allowed to be empty
	 * 
	 * @param constraints additional constraints that you might want to add
	 * 		on allowed values
	 * 
	 * @throws MissingResourceException if option value not found.
	 * @throws IllegalStateException if options list contains duplicate value(s)
	 * 		or if incompatible constraints are used
	 */
	public Set<String> getStringSet(String option, @Nonnull OptionConstraint... constraints)
		throws MissingResourceException, IllegalStateException
	{
		return Collections.unmodifiableSet(convertListToHashSet(getStringList(option, constraints), option));
	}
	
	/**
	 * Gets options as a set of Strings (unmodifiable, backed by {@link HashSet}); 
	 * fails if not found or value cannot be parsed as a comma-separated list of Strings.
	 * <p>
	 * Returned list is unmodifiable.
	 * <p>
	 * ATTENTION: by default, resulting set is allowed to be empty
	 * <p>
	 * ATTENTION2: by default, elements of the resulting set are allowed to be empty
	 * 
	 * @param constraints additional constraints that you might want to add
	 * 		on allowed values
	 * 
	 * @throws MissingResourceException if option value not found.
	 * @throws IllegalArgumentException if default value is null
	 * @throws IllegalStateException if options list contains duplicate value(s)
	 * 		or if incompatible constraints are used
	 */
	public Set<String> getStringSet(String option, String defaultValue, @Nonnull OptionConstraint... constraints)
		throws MissingResourceException, IllegalArgumentException, IllegalStateException
	{
		return Collections.unmodifiableSet(convertListToHashSet(getStringList(option, defaultValue, constraints), option));
	}
	
	/**
	 * Gets options as a list of Strings; fails if not found or value cannot be parsed
	 * as a comma-separated list of Strings.
	 * <p>
	 * Values are trimmed to avoid unexpected behavior (e.g. leading/trailing spaces).
	 * <p>
	 * Returned list is unmodifiable.
	 * <p>
	 * ATTENTION: by default, resulting list is allowed to be empty
	 * <p>
	 * ATTENTION2: by default, elements of the resulting list are allowed to be empty
	 * 
	 * @param defaultValue if not null, this is used in case option is not found
	 * @param ignoreUnapplicableConstraints if true, then constraints that are
	 * 		not applicable (e.g. number-related) are silently ignored -- useful
	 * 		e.g. when using the resulting list for number processing or something;
	 * 		if false, then unapplicable constraints cause exception
	 * @param constraints additional constraints that you might want to add
	 * 		on allowed values
	 * 
	 * @throws MissingResourceException if option value not found.
	 */
	protected List<String> internalGetStringList(String option, @Nullable String defaultValue,
		boolean ignoreUnapplicableConstraints, @Nonnull OptionConstraint... constraints) 
		throws MissingResourceException
	{
		String strValue;
		if (defaultValue == null)
			strValue = getStringPossiblyEmpty(option);
		else
			strValue = getStringPossiblyEmpty(option, defaultValue);
		
		String[] values;
		if (strValue.isEmpty())
			values = new String[0]; // Special case for empty strings
		else
			values = strValue.split(",");
		
		
		@Nonnull List<String> result = new ArrayList<>(values.length);
		for (String item : values)
			result.add(item.trim());
		
		for (OptionConstraint constraint : constraints)
			checkStringCollection(result, ignoreUnapplicableConstraints, constraint, option);
		
		return Collections.unmodifiableList(result);
	}
	
	/**
	 * Gets option as int; fails if not found.
	 * 
	 * @throws MissingResourceException if option value not found.
	 * @throws NumberFormatException if option doesn't convert to int
	 */
	public int getInt(String option) throws MissingResourceException, NumberFormatException
	{
		return Integer.parseInt(getString(option));
	}
	
	/**
	 * Gets option as int; returns provided 'default' value if not found.
	 * 
	 * NOTE: this method does not allow for empty option value in configuration --
	 * these are treated the same as missing options.
	 * 
	 * @throws NumberFormatException if option doesn't convert to int
	 */
	public int getInt(String option, int defaultValue) throws NumberFormatException
	{
		try
		{
			return getInt(option);
		} catch (MissingResourceException e)
		{
			return defaultValue;
		}
	}
	
	/**
	 * Gets option as a positive int; fails if not found or not a positive integer.
	 * 
	 * @throws MissingResourceException if option value not found.
	 * @throws NumberFormatException if option doesn't convert to int
	 * @throws IllegalStateException if option value is not a positive integer
	 */
	public int getIntPositive(String option) throws MissingResourceException, NumberFormatException, IllegalStateException
	{
		int value = getInt(option);
		if (value < 1)
			throw new IllegalStateException("Property [" + option + "] value must be a positive integer, got: " + value);
		
		return value;
	}
	
	/**
	 * Gets option as a positive int; fails if not a positive integer;
	 * returns provided 'default' value if not found.
	 * 
	 * NOTE: this method does not allow for empty option value in configuration --
	 * these are treated the same as missing options.
	 * 
	 * NOTE2: defaultValue argument is not checked -- it is returned as is (i.e.
	 * method can return non-positive values via this).
	 * 
	 * @throws NumberFormatException if option doesn't convert to int
	 * @throws IllegalStateException if option value is not a positive integer
	 */
	public int getIntPositive(String option, int defaultValue) throws NumberFormatException, IllegalStateException
	{
		try
		{
			int value = getInt(option);
			if (value < 1)
				throw new IllegalStateException("Property [" + option + "] value must be a positive integer, got: " + value);
			
			return value;
		} catch (MissingResourceException e)
		{
			return defaultValue;
		}
	}
	
	/**
	 * Gets option as a positive int or zero; fails if not found or not a positive integer or zero.
	 * 
	 * @throws MissingResourceException if option value not found.
	 * @throws NumberFormatException if option doesn't convert to int
	 * @throws IllegalStateException if option value is not a positive integer
	 */
	public int getIntNonNegative(String option) throws MissingResourceException, NumberFormatException, IllegalStateException
	{
		int value = getInt(option);
		if (value < 0)
			throw new IllegalStateException("Property [" + option + "] value must be a positive integer or zero, got: " + value);
		
		return value;
	}
	
	/**
	 * Gets option as a positive int or zero; fails if not a positive integer or zero;
	 * returns provided 'default' value if not found.
	 * 
	 * NOTE: this method does not allow for empty option value in configuration --
	 * these are treated the same as missing options.
	 * 
	 * NOTE2: defaultValue argument is not checked -- it is returned as is (i.e.
	 * method can return negative values via this).
	 * 
	 * @throws NumberFormatException if option doesn't convert to int
	 * @throws IllegalStateException if option value is not a positive integer
	 */
	public int getIntNonNegative(String option, int defaultValue) throws NumberFormatException, IllegalStateException
	{
		try
		{
			int value = getInt(option);
			if (value < 0)
				throw new IllegalStateException("Property [" + option + "] value must be a positive integer or zero, got: " + value);
			
			return value;
		} catch (MissingResourceException e)
		{
			return defaultValue;
		}
	}
	
	/**
	 * Gets options as a pair of ints; fails if not found or value cannot be parsed
	 * as a pair of ints (two ints should be comma-separated).
	 * 
	 * @throws MissingResourceException if option value not found.
	 * @throws IllegalArgumentException if option value cannot be parsed to pair 
	 * @throws NumberFormatException if either value in pair doesn't convert to int
	 */
	public Pair<@Nonnull Integer, @Nonnull Integer> getIntPair(String option) 
		throws MissingResourceException, IllegalArgumentException, NumberFormatException
	{
		String strValue = getString(option);
		String[] values = strValue.split(",");
		
		if (values.length != 2)
			throw new IllegalArgumentException("Property [" + option + "] value doesn't parse as a pair: " + strValue);
		
		return new Pair<Integer, Integer>(
			Integer.parseInt(values[0]), Integer.parseInt(values[1]) );
	}
	
	/**
	 * Gets options as a pair of ints; fails if not found or value cannot be parsed
	 * as a pair of ints (two ints should be comma-separated).
	 * Also both pair members must be -1, 0, or a positive integer.
	 * 
	 * @throws MissingResourceException if option value not found.
	 * @throws IllegalArgumentException if option value cannot be parsed to pair 
	 * @throws NumberFormatException if either value in pair doesn't convert to int
	 * @throws IllegalStateException if either pair member is less than -1
	 */
	public Pair<@Nonnull Integer, @Nonnull Integer> getIntPairNegOneOrMore(String option) 
		throws MissingResourceException, IllegalArgumentException, NumberFormatException, IllegalStateException
	{
		Pair<Integer, Integer> pair = getIntPair(option);
		
		if ((pair.getValue0().intValue() < -1) || (pair.getValue1().intValue() < -1))
		{
			throw new IllegalStateException("Property [" + option + "] value must have -1 or more for both integers, got: " + pair);
		}
		
		return pair;
	}
	
	/**
	 * Gets options as a list of ints; fails if not found or value cannot be parsed
	 * as a comma-separated list of ints.
	 * <p>
	 * Returned list is unmodifiable.
	 * <p>
	 * ATTENTION: by default, resulting list is allowed to be empty
	 * 
	 * @param constraints additional constraints that you might want to add
	 * 		on allowed values
	 * 
	 * @throws MissingResourceException if option value not found.
	 * @throws NumberFormatException if any value doesn't convert to int
	 * @throws IllegalStateException if incompatible constraints are used
	 */
	public List<Integer> getIntList(String option, @Nonnull OptionConstraint... constraints)
		throws MissingResourceException, NumberFormatException, IllegalStateException
	{
		return internalGetIntList(option, null, constraints);
	}
	
	/**
	 * Gets options as a list of ints; fails if not found or value cannot be parsed
	 * as a comma-separated list of ints.
	 * <p>
	 * Returned list is unmodifiable.
	 * <p>
	 * ATTENTION: by default, resulting list is allowed to be empty
	 * 
	 * @param constraints additional constraints that you might want to add
	 * 		on allowed values
	 * 
	 * @throws MissingResourceException if option value not found.
	 * @throws NumberFormatException if any value doesn't convert to int
	 * @throws IllegalArgumentException if default value is null
	 * @throws IllegalStateException if incompatible constraints are used
	 */
	public List<Integer> getIntList(String option, String defaultValue, @Nonnull OptionConstraint... constraints)
		throws MissingResourceException, NumberFormatException, IllegalArgumentException, IllegalStateException
	{
		if (nullable(defaultValue) == null)
			throw new IllegalArgumentException("Default value must be non-null in option: " + option);
		
		return internalGetIntList(option, defaultValue, constraints);
	}
	
	/**
	 * Gets options as a set of ints (unmodifiable, backed by {@link HashSet}); 
	 * fails if not found or value cannot be parsed as a comma-separated list of ints.
	 * <p>
	 * Returned list is unmodifiable.
	 * <p>
	 * ATTENTION: by default, resulting list is allowed to be empty
	 * 
	 * @param constraints additional constraints that you might want to add
	 * 		on allowed values
	 * 
	 * @throws MissingResourceException if option value not found.
	 * @throws NumberFormatException if any value doesn't convert to int
	 * @throws IllegalStateException if options list contains duplicate value(s)
	 * 		or if incompatible constraints are used
	 */
	public Set<Integer> getIntSet(String option, @Nonnull OptionConstraint... constraints)
		throws MissingResourceException, NumberFormatException, IllegalStateException
	{
		return Collections.unmodifiableSet(convertListToHashSet(getIntList(option, constraints), option));
	}
	
	/**
	 * Gets options as a set of ints (unmodifiable, backed by {@link HashSet}); 
	 * fails if not found or value cannot be parsed as a comma-separated list of ints.
	 * <p>
	 * Returned list is unmodifiable.
	 * <p>
	 * ATTENTION: by default, resulting list is allowed to be empty
	 * 
	 * @param constraints additional constraints that you might want to add
	 * 		on allowed values
	 * 
	 * @throws MissingResourceException if option value not found.
	 * @throws NumberFormatException if any value doesn't convert to int
	 * @throws IllegalArgumentException if default value is null
	 * @throws IllegalStateException if options list contains duplicate value(s)
	 * 		or if incompatible constraints are used
	 */
	public Set<Integer> getIntSet(String option, String defaultValue, @Nonnull OptionConstraint... constraints)
		throws MissingResourceException, NumberFormatException, IllegalArgumentException, IllegalStateException
	{
		return Collections.unmodifiableSet(convertListToHashSet(getIntList(option, defaultValue, constraints), option));
	}
	
	/**
	 * Gets options as a list of ints; fails if not found or value cannot be parsed
	 * as a comma-separated list of ints.
	 * <p>
	 * Returned list is unmodifiable.
	 * <p>
	 * ATTENTION: by default, resulting list is allowed to be empty
	 * 
	 * @param defaultValue if not null, this is used in case option is not found
	 * @param constraints additional constraints that you might want to add
	 * 		on allowed values
	 * 
	 * @throws MissingResourceException if option value not found.
	 * @throws NumberFormatException if any value doesn't convert to int
	 */
	protected List<Integer> internalGetIntList(String option, @Nullable String defaultValue, @Nonnull OptionConstraint... constraints) 
		throws MissingResourceException, NumberFormatException
	{
		@Nonnull List<@Nonnull String> strValues = internalGetStringList(option, defaultValue, true, constraints);
		
		ArrayList<Integer> result = new ArrayList<>(strValues.size());
		for (String v : strValues)
		{
			try
			{
				result.add(Integer.parseInt(v));
			} catch (NumberFormatException e)
			{
				throw new NumberFormatException("Problem with option [" + option + "]: failed to parse [" + v + "] as int in: " + strValues + "; message: " + e);
			}
		}
		
		for (OptionConstraint constraint : constraints)
			checkNumberCollection(result, constraint, option);
		
		return Collections.unmodifiableList(result);
	}
	
	/**
	 * Gets option as long; fails if not found.
	 * 
	 * @throws MissingResourceException if option value not found.
	 * @throws NumberFormatException if option doesn't convert to long
	 */
	public long getLong(String option) throws MissingResourceException, NumberFormatException
	{
		return Long.parseLong(getString(option));
	}
	
	/**
	 * Gets option as long; returns provided 'default' value if not found.
	 * 
	 * NOTE: this method does not allow for empty option value in configuration --
	 * these are treated the same as missing options.
	 * 
	 * @throws NumberFormatException if option doesn't convert to long
	 */
	public long getLong(String option, long defaultValue) throws NumberFormatException
	{
		try
		{
			return getLong(option);
		} catch (MissingResourceException e)
		{
			return defaultValue;
		}
	}
	
	/**
	 * Gets option as a positive long; fails if not found or not a positive long.
	 * 
	 * @throws MissingResourceException if option value not found.
	 * @throws NumberFormatException if option doesn't convert to long
	 * @throws IllegalStateException if option value is not a long integer
	 */
	public long getLongPositive(String option) throws MissingResourceException, NumberFormatException, IllegalStateException
	{
		long value = getLong(option);
		if (value < 1)
			throw new IllegalStateException("Property [" + option + "] value must be a positive long, got: " + value);
		
		return value;
	}
	
	/**
	 * Gets option as a positive long; fails if not a positive long;
	 * returns provided 'default' value if not found.
	 * 
	 * NOTE: this method does not allow for empty option value in configuration --
	 * these are treated the same as missing options.
	 * 
	 * NOTE2: defaultValue argument is not checked -- it is returned as is (i.e.
	 * method can return non-positive values via this).
	 * 
	 * @throws NumberFormatException if option doesn't convert to long
	 * @throws IllegalStateException if option value is not a positive long
	 */
	public long getLongPositive(String option, long defaultValue) throws NumberFormatException, IllegalStateException
	{
		try
		{
			long value = getLong(option);
			if (value < 1)
				throw new IllegalStateException("Property [" + option + "] value must be a positive long, got: " + value);
			
			return value;
		} catch (MissingResourceException e)
		{
			return defaultValue;
		}
	}
	
	/**
	 * Gets option as a positive long or zero; fails if not found or not a positive long or zero.
	 * 
	 * @throws MissingResourceException if option value not found.
	 * @throws NumberFormatException if option doesn't convert to long
	 * @throws IllegalStateException if option value is not a positive long
	 */
	public long getLongNonNegative(String option) throws MissingResourceException, NumberFormatException, IllegalStateException
	{
		long value = getLong(option);
		if (value < 0)
			throw new IllegalStateException("Property [" + option + "] value must be a positive long or zero, got: " + value);
		
		return value;
	}
	
	/**
	 * Gets option as a positive long or zero; fails if not a positive long or zero;
	 * returns provided 'default' value if not found.
	 * 
	 * NOTE: this method does not allow for empty option value in configuration --
	 * these are treated the same as missing options.
	 * 
	 * NOTE2: defaultValue argument is not checked -- it is returned as is (i.e.
	 * method can return negative values via this).
	 * 
	 * @throws NumberFormatException if option doesn't convert to long
	 * @throws IllegalStateException if option value is not a positive long
	 */
	public long getLongNonNegative(String option, long defaultValue) throws NumberFormatException, IllegalStateException
	{
		try
		{
			long value = getLong(option);
			if (value < 0)
				throw new IllegalStateException("Property [" + option + "] value must be a positive long or zero, got: " + value);
			
			return value;
		} catch (MissingResourceException e)
		{
			return defaultValue;
		}
	}
	
	/**
	 * Gets options as a list of longs; fails if not found or value cannot be parsed
	 * as a comma-separated list of longs.
	 * <p>
	 * Returned list is unmodifiable.
	 * <p>
	 * ATTENTION: by default, resulting list is allowed to be empty
	 * 
	 * @param constraints additional constraints that you might want to add
	 * 		on allowed values
	 * 
	 * @throws MissingResourceException if option value not found.
	 * @throws NumberFormatException if any value doesn't convert to long
	 * @throws IllegalStateException if incompatible constraints are used
	 */
	public List<Long> getLongList(String option, @Nonnull OptionConstraint... constraints)
		throws MissingResourceException, NumberFormatException, IllegalStateException
	{
		return internalGetLongList(option, null, constraints);
	}
	
	/**
	 * Gets options as a list of longs; fails if not found or value cannot be parsed
	 * as a comma-separated list of longs.
	 * <p>
	 * Returned list is unmodifiable.
	 * <p>
	 * ATTENTION: by default, resulting list is allowed to be empty
	 * 
	 * @param constraints additional constraints that you might want to add
	 * 		on allowed values
	 * 
	 * @throws MissingResourceException if option value not found.
	 * @throws NumberFormatException if any value doesn't convert to long
	 * @throws IllegalArgumentException if default value is null
	 * @throws IllegalStateException if incompatible constraints are used
	 */
	public List<Long> getLongList(String option, String defaultValue, @Nonnull OptionConstraint... constraints)
		throws MissingResourceException, NumberFormatException, IllegalArgumentException, IllegalStateException
	{
		if (nullable(defaultValue) == null)
			throw new IllegalArgumentException("Default value must be non-null in option: " + option);
		
		return internalGetLongList(option, defaultValue, constraints);
	}
	
	/**
	 * Gets options as a set of longs (unmodifiable, backed by {@link HashSet}); 
	 * fails if not found or value cannot be parsed as a comma-separated list of longs.
	 * <p>
	 * Returned list is unmodifiable.
	 * <p>
	 * ATTENTION: by default, resulting list is allowed to be empty
	 * 
	 * @param constraints additional constraints that you might want to add
	 * 		on allowed values
	 * 
	 * @throws MissingResourceException if option value not found.
	 * @throws NumberFormatException if any value doesn't convert to long
	 * @throws IllegalStateException if options list contains duplicate value(s)
	 */
	public Set<Long> getLongSet(String option, @Nonnull OptionConstraint... constraints)
		throws MissingResourceException, NumberFormatException, IllegalStateException
	{
		return Collections.unmodifiableSet(convertListToHashSet(getLongList(option, constraints), option));
	}
	
	/**
	 * Gets options as a set of longs (unmodifiable, backed by {@link HashSet}); 
	 * fails if not found or value cannot be parsed as a comma-separated list of longs.
	 * <p>
	 * Returned list is unmodifiable.
	 * <p>
	 * ATTENTION: by default, resulting list is allowed to be empty
	 * 
	 * @param constraints additional constraints that you might want to add
	 * 		on allowed values
	 * 
	 * @throws MissingResourceException if option value not found.
	 * @throws NumberFormatException if any value doesn't convert to long
	 * @throws IllegalArgumentException if default value is null
	 * @throws IllegalStateException if options list contains duplicate value(s)
	 */
	public Set<Long> getLongSet(String option, String defaultValue, @Nonnull OptionConstraint... constraints)
		throws MissingResourceException, NumberFormatException, IllegalArgumentException, IllegalStateException
	{
		return Collections.unmodifiableSet(convertListToHashSet(getLongList(option, defaultValue, constraints), option));
	}
	
	/**
	 * Gets options as a list of longs; fails if not found or value cannot be parsed
	 * as a comma-separated list of longs.
	 * <p>
	 * Returned list is unmodifiable.
	 * <p>
	 * ATTENTION: by default, resulting list is allowed to be empty
	 * 
	 * @param defaultValue if not null, this is used in case option is not found
	 * @param constraints additional constraints that you might want to add
	 * 		on allowed values
	 * 
	 * @throws MissingResourceException if option value not found.
	 * @throws NumberFormatException if any value doesn't convert to long
	 */
	protected List<Long> internalGetLongList(String option, @Nullable String defaultValue, @Nonnull OptionConstraint... constraints) 
		throws MissingResourceException, NumberFormatException
	{
		@Nonnull List<@Nonnull String> strValues = internalGetStringList(option, defaultValue, true, constraints);

		ArrayList<Long> result = new ArrayList<>(strValues.size());
		for (String v : strValues)
		{
			try
			{
				result.add(Long.parseLong(v));
			} catch (NumberFormatException e)
			{
				throw new NumberFormatException("Problem with option [" + option + "]: failed to parse [" + v + "] as long in: " + strValues + "; message: " + e);
			}
		}
		
		for (OptionConstraint constraint : constraints)
			checkNumberCollection(result, constraint, option);
		
		return Collections.unmodifiableList(result);
	}
	
	/**
	 * Gets option as boolean; fails if not found.
	 * 
	 * @throws MissingResourceException if option value not found.
	 * @throws IllegalArgumentException if option doesn't convert to boolean
	 */
	public boolean getBoolean(String option) throws MissingResourceException, IllegalArgumentException
	{
		String strVal = getString(option).toLowerCase();
		if ("true".equals(strVal))
			return true;
		if ("false".equals(strVal))
			return false;
		
		throw new IllegalArgumentException("Not a boolean property [" + option + "] value: " + strVal);
	}
	
	/**
	 * Gets option as boolean; fails if not found.
	 * 
	 * @throws IllegalArgumentException if option doesn't convert to boolean
	 */
	public boolean getBoolean(String option, boolean defaultValue) throws IllegalArgumentException
	{
		try
		{
			return getBoolean(option);
		} catch (MissingResourceException e)
		{
			return defaultValue;
		}
	}
	
	/**
	 * Gets options as {@link DateTime}; fails if not found.
	 * The format of the timestamp is as follows:
	 * 2012-04-28T13:54:42.568Z
	 * or
	 * 2012-04-28T13:54:42.568+03:00
	 * 
	 * @throws MissingResourceException if option value not found.
	 * @throws IllegalArgumentException if option doesn't convert to boolean
	 */
	public DateTime getDateTime(String option) throws MissingResourceException, IllegalArgumentException
	{
		return nonNull(DateTime.parse(getString(option)));
	}

	/**
	 * Gets time interval in milliseconds.
	 * This method is smart enough to interpret various measurements, specifically:
	 * 35ms
	 * 35s
	 * 35m
	 * 35h
	 * 35d
	 * 
	 * NOTE: value cannot be negative, but it can be zero
	 * 
	 * @throws MissingResourceException if option value not found.
	 * @throws IllegalArgumentException if option value doesn't parse
	 */
	public long getTimeInterval(String option) throws MissingResourceException, IllegalArgumentException
	{
		String intervalValue = getString(option);
		
		try
		{
			return parseTimeInterval(intervalValue);
		} catch (IllegalArgumentException e)
		{
			throw new IllegalArgumentException("Failed to parse option [" + option +"] value as time interval: " + e, e);
		}
	}
	
	/**
	 * Gets time interval in milliseconds.
	 * This method is smart enough to interpret various measurements, specifically:
	 * 35ms
	 * 35s
	 * 35m
	 * 35h
	 * 35d
	 *
	 * Returns default value if option is not specified in configuration.
	 * 
	 * NOTE: option value cannot be negative, but it can be zero; default
	 * 		value can be anything
	 * 
	 * @throws IllegalArgumentException if option value doesn't parse
	 */
	public long getTimeInterval(String option, long defaultValue) throws IllegalArgumentException
	{
		try
		{
			return getTimeInterval(option);
		} catch (MissingResourceException e)
		{
			return defaultValue;
		}
	}

	/**
	 * Gets time interval in milliseconds.
	 * This method is smart enough to interpret various measurements, specifically:
	 * 35ms
	 * 35s
	 * 35m
	 * 35h
	 * 35d
	 *
	 * Returns default value if option is not specified in configuration.
	 * 
	 * NOTE: option value cannot be negative, but it can be zero; default
	 * 		value must be non-negative parse-able time
	 * 
	 * @throws IllegalArgumentException if option value doesn't parse
	 */
	public long getTimeInterval(String option, String strDefaultValue) throws IllegalArgumentException
	{
		long defaultValue;
		try
		{
			defaultValue = parseTimeInterval(strDefaultValue);
		} catch (IllegalArgumentException e)
		{
			throw new IllegalArgumentException("Failed to parse option [" + option +"] DEFAULT value as time interval: " + e, e);
		}
		
		try
		{
			return getTimeInterval(option);
		} catch (MissingResourceException e)
		{
			return defaultValue;
		}
	}
	

	/**
	 * Gets time interval in milliseconds.
	 * This method is smart enough to interpret various measurements, specifically:
	 * 35ms
	 * 35s
	 * 35m
	 * 35h
	 * 35d
	 * 
	 * Returns null if option is not specified.
	 * 
	 * NOTE: value cannot be negative, but it can be zero
	 * 
	 * @throws IllegalArgumentException if option value doesn't parse
	 */
	@Nullable
	public Long getTimeIntervalOrNull(String option) throws IllegalArgumentException
	{
		try
		{
			return getTimeInterval(option);
		} catch (MissingResourceException e)
		{
			return null;
		}
	}
	
	/**
	 * Gets time interval in milliseconds.
	 * This method is smart enough to interpret various measurements, specifically:
	 * 35ms
	 * 35s
	 * 35m
	 * 35h
	 * 35d
	 * <p>
	 * NOTE: unlike {@link #getTimeInterval(String)} the value must be positive
	 * (cannot be zero)
	 * 
	 * @throws MissingResourceException if option value not found.
	 * @throws IllegalArgumentException if option value doesn't parse
	 * @throws IllegalStateException if option value parses as zero
	 * 
	 */
	public long getTimeIntervalPositive(String option) throws MissingResourceException, IllegalArgumentException, IllegalStateException
	{
		long value = getTimeInterval(option);
		
		if (value < 1)
			throw new IllegalStateException("Property [" + option + "] time value must be positive, got: " + value);
		
		return value;
	}
	
	/**
	 * Gets time interval in milliseconds.
	 * This method is smart enough to interpret various measurements, specifically:
	 * 35ms
	 * 35s
	 * 35m
	 * 35h
	 * 35d
	 *
	 * Returns default value if option is not specified in configuration.
	 * 
	 * NOTE: option value cannot be negative or zero; default
	 * 		value can be anything
	 * 
	 * @throws IllegalArgumentException if option value doesn't parse
	 * @throws IllegalStateException if option value parses as zero
	 */
	public long getTimeIntervalPositive(String option, long defaultValue) throws IllegalArgumentException, IllegalStateException
	{
		try
		{
			return getTimeIntervalPositive(option);
		} catch (MissingResourceException e)
		{
			return defaultValue;
		}
	}

	/**
	 * Gets time interval in milliseconds.
	 * This method is smart enough to interpret various measurements, specifically:
	 * 35ms
	 * 35s
	 * 35m
	 * 35h
	 * 35d
	 *
	 * Returns default value if option is not specified in configuration.
	 * 
	 * NOTE: option value cannot be negative or zero; default
	 * 		value must be non-negative parse-able time
	 * 
	 * @throws IllegalArgumentException if option value doesn't parse
	 * @throws IllegalStateException if option value parses as zero
	 */
	public long getTimeIntervalPositive(String option, String strDefaultValue) throws IllegalArgumentException, IllegalStateException
	{
		long defaultValue;
		try
		{
			defaultValue = parseTimeInterval(strDefaultValue);
		} catch (IllegalArgumentException e)
		{
			throw new IllegalArgumentException("Failed to parse option [" + option +"] DEFAULT value as time interval: " + e, e);
		}
		
		try
		{
			return getTimeIntervalPositive(option);
		} catch (MissingResourceException e)
		{
			return defaultValue;
		}
	}
	

	/**
	 * Gets time interval in milliseconds.
	 * This method is smart enough to interpret various measurements, specifically:
	 * 35ms
	 * 35s
	 * 35m
	 * 35h
	 * 35d
	 * 
	 * Returns null if option is not specified.
	 * 
	 * NOTE: value cannot be negative or zero
	 * 
	 * @throws IllegalArgumentException if option value doesn't parse
	 * @throws IllegalStateException if option value parses as zero
	 */
	@Nullable
	public Long getTimeIntervalPositiveOrNull(String option) throws IllegalArgumentException, IllegalStateException
	{
		try
		{
			return getTimeIntervalPositive(option);
		} catch (MissingResourceException e)
		{
			return null;
		}
	}
	
	
	/**
	 * Gets directory as a {@link File} instance.
	 * If this method returns without exception, then it is guaranteed that
	 * returned File instance specifies an existing (possibly just created) directory.
	 * 
	 * @throws MissingResourceException if option value not found.
	 * @throws IllegalArgumentException if option value doesn't parse
	 */
	public File getDirectory(String option) throws MissingResourceException, IllegalArgumentException
	{
		String strVal = getString(option);
		
		File dir = new File(strVal);
		
		if (!dir.exists())
		{
			if (!dir.mkdirs())
				throw new IllegalArgumentException("Unable to create directory for property [" + option + "] value: " + dir);
		}
		
		if (!dir.isDirectory())
			throw new IllegalArgumentException("Not a directory -- property [" + option + "] value: " + dir);
		
		return dir;
	}
	
	/**
	 * Gets key/value pairs where keys and values are Strings.
	 * Option format is:
	 * key1:value1;key2:value2;...
	 * 
	 * Returned map is unmodifiable.
	 * 
	 * @return empty map if option value is empty, map with values otherwise
	 * 
	 * @throws MissingResourceException if option value not found.
	 * @throws IllegalArgumentException if option value doesn't parse
	 */
	public Map<String, String> getKeyValueAsString(String option) throws MissingResourceException, IllegalArgumentException
	{
		// Non-null because argument value cannot be null
		return nonNull(parseKeyValueAsString(getStringPossiblyEmpty(option), option));
	}
	
	/**
	 * Gets key/value pairs where keys and values are Strings.
	 * Option format is:
	 * key1:value1;key2:value2;...
	 * 
	 * Returned map is unmodifiable.
	 * 
	 * If you want to use null default value, best use {@link #getKeyValueAsStringOrNull(String)}
	 * 
	 * @return empty map if option value is empty, null if option is not specified
	 * 		and default value is null, result of default value parse if option
	 * 		is not specified, map with option values otherwise
	 * 
	 * @throws IllegalArgumentException if option value doesn't parse
	 */
	public Map<String, String> getKeyValueAsString(String option, String defaultValue) throws MissingResourceException, IllegalArgumentException
	{
		String value;
		try
		{
			value = getStringPossiblyEmpty(option);
		} catch (MissingResourceException e)
		{
			value = defaultValue;
		}
		
		// Non-null because argument value cannot be null
		return nonNull(parseKeyValueAsString(value, option));
	}
	
	/**
	 * Gets key/value pairs where keys and values are Strings.
	 * Option format is:
	 * key1:value1;key2:value2;...
	 * 
	 * Returned map is unmodifiable.
	 * 
	 * @return empty map if option value is empty, null if option is not specified, 
	 * 			map with option values otherwise
	 * 
	 * @throws IllegalArgumentException if option value doesn't parse
	 */
	@Nullable
	public Map<String, String> getKeyValueAsStringOrNull(String option) throws MissingResourceException, IllegalArgumentException
	{
		String value;
		try
		{
			value = getStringPossiblyEmpty(option);
		} catch (MissingResourceException e)
		{
			return null;
		}
		
		// Non-null because argument value cannot be null
		return nonNull(parseKeyValueAsString(value, option));
	}
	
	/**
	 * Parses string as key-value pairs.
	 * String format is:
	 * key1:value1;key2:value2;...
	 * 
	 * Returned map is unmodifiable.
	 * 
	 * @return null if argument string is null, empty map if argument string is empty, map with values otherwise
	 * @throws IllegalArgumentException if option value doesn't parse
	 */
	@Nullable
	protected Map<String, String> parseKeyValueAsString(@Nullable String stringToParse, String optionNameForMessages) throws IllegalArgumentException
	{
		if (stringToParse == null)
			return null;
		
		Map<String, String> result = new HashMap<String, String>();
		if (stringToParse.trim().isEmpty())
			return result;
		
		String[] pairs = stringToParse.split(";");
		for (String pair : pairs)
		{
			if (pair.trim().isEmpty())
				continue; // Ignore empty pairs.
			
			String[] parts = pair.split(":");
			
			final String key;
			final String value;
			if ((parts.length == 1) && (pair.endsWith(":")))
			{
				// Special case for when value is empty.
				key = parts[0];
				value = "";
			}
			else
			{
				if (parts.length != 2)
					throw new IllegalArgumentException("String doesn't parse as key=value: " + pair + "; in option: " + optionNameForMessages);
				key = parts[0].trim();
				value = parts[1].trim();
			}
			if (key.isEmpty())
				throw new IllegalArgumentException("String doesn't parse as key=value: " + pair + "; in option: " + optionNameForMessages);
			
			result.put(key, value);
		}
		
		return Collections.unmodifiableMap(result);
	}
	
	/**
	 * Checks whether given collection is NON-empty.
	 * 
	 * @throws IllegalStateException if collection is empty
	 */
	protected <C extends Collection<E>, E> void checkNonEmpty(C collection, String optionNameForMessages)
		throws IllegalStateException
	{
		if (collection.size() == 0)
			throw new IllegalStateException("Collection is empty in option: " + optionNameForMessages);
	}
	
	/**
	 * Checks specific constraint for a given {@link Number} collection.
	 * 
	 * @throws IllegalStateException if collection fails constraint check or
	 * 		if given constraints are not applicable to the collection type
	 */
	protected <C extends Collection<E>, E extends Number> void checkNumberCollection(
		C collection, OptionConstraint constraint, String optionNameForMessages) 
			throws IllegalStateException
	{
		long minValue = Long.MIN_VALUE;
		
		switch (constraint)
		{
			case NON_EMPTY_COLLECTION:
				checkNonEmpty(collection, optionNameForMessages);
				break;
			case POSITIVE:
				minValue = 1;
				break;
			case NON_NEGATIVE:
				minValue = 0;
				break;
			case NEGATIVE_ONE_OR_MORE:
				minValue = -1;
				break;
			case NON_EMPTY_ELEMENT:
				throw new IllegalStateException("Option [" + optionNameForMessages + "] constraint type not applicable to number collection: " + constraint);
		}
		
		if (minValue > Long.MIN_VALUE)
		{
			for (E v : collection)
			{
				if (v.longValue() < minValue)
					throw new IllegalStateException("Option [" + optionNameForMessages + "] value [" + v + "] violates constraint: " + constraint);
			}
		}
	}
	
	/**
	 * Checks specific constraint for a given String collection.
	 * 
	 * @param ignoreUnapplicableConstraints if true, then constraints that are
	 * 		not applicable (e.g. number-related) are silently ignored -- useful
	 * 		e.g. when using the resulting list for number processing or something;
	 * 		if false, then unapplicable constraints cause exception
	 * 
	 * @throws IllegalStateException if collection fails constraint check or
	 * 		if given constraints are not applicable to the collection type
	 */
	protected void checkStringCollection(
		Collection<String> collection, boolean ignoreUnapplicableConstraints, 
		OptionConstraint constraint, String optionNameForMessages) 
			throws IllegalStateException
	{
		switch (constraint)
		{
			case NON_EMPTY_COLLECTION:
				checkNonEmpty(collection, optionNameForMessages);
				break;
			case NON_EMPTY_ELEMENT:
				for (String item : collection)
				{
					if (item.isEmpty())
						throw new IllegalStateException("Option [" + optionNameForMessages + "] contains empty value: " + collection);
				}
				break;
			case POSITIVE:
			case NON_NEGATIVE:
			case NEGATIVE_ONE_OR_MORE:
				if (!ignoreUnapplicableConstraints)
					throw new IllegalStateException("Option [" + optionNameForMessages + "] constraint type not applicable to string collection: " + constraint);
				break;
		}
	}
	
	
	/**
	 * Converts list to {@link HashSet}.
	 * 
	 * @throws IllegalStateException if there is a duplicate value
	 */
	protected <T> HashSet<T> convertListToHashSet(List<T> list, String optionNameForMessages) 
		throws IllegalStateException
	{
		HashSet<T> result = new HashSet<>((list.size() + 1) * 4 / 3); // Adjust size for default load factor
		
		for (T item : list)
		{
			if (!result.add(item))
				throw new IllegalStateException("Duplicate value [" + item + "]: in option: " + optionNameForMessages);
		}
		
		return result;
	}
}
