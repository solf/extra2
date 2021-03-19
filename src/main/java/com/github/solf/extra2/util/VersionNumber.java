package com.github.solf.extra2.util;

import static java.util.regex.Pattern.compile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.maven.artifact.versioning.ComparableVersion;

/**
 * Compares artifact versions as per Maven rules.
 * <p>
 * Usage: Version.compare("1.0.0").gt("0.9.9"))
 * <p>
 * Or: new Version("1.0.0").gt("0.9.0")
 * <p>
 * Source: https://gist.github.com/yclian/2627608
 *
 * @author yclian
 * 
 * @see ComparableVersion
 */
@ParametersAreNonnullByDefault
public class VersionNumber
{

	private static final Pattern PATTERN_APPROXIMATION = compile("^([\\d\\.]+\\.)*(\\d+)$");

	private final ComparableVersion mVersion;

	public VersionNumber(String v)
	{
		mVersion = new ComparableVersion(v);
	}

	public static VersionNumber compare(String v)
	{
		return new VersionNumber(v);
	}

	/**
	 * <p>
	 * Return the result of {@link Comparable#compareTo(Object)}. Very limited
	 * as it doesn't support {@code &gt;=}, {@code &lt;=} and {@code ~&gt;}.
	 * </p>
	 *
	 * @param v
	 * @return
	 */
	public int with(String v)
	{
		return mVersion.compareTo(new ComparableVersion(v));
	}

	public boolean eq(String v)
	{
		return with(v) == 0;
	}

	public boolean le(String v)
	{
		int c = with(v);
		return c == 0 || c == -1;
	}

	public boolean lt(String v)
	{
		return with(v) == -1;
	}

	public boolean ge(String v)
	{
		int c = with(v);
		return c == 0 || c == 1;
	}

	public boolean gt(String v)
	{
		return with(v) == 1;
	}

	/**
	 * <p>
	 * Approximately greater than, inspired by (and works exactly like)
	 * RubyGems.
	 * </p>
	 *
	 * @see <a href="http://docs.rubygems.org/read/chapter/16">RubyGems Manuals
	 *      - Specifying Versions</a>
	 * @param v
	 * @return
	 */
	public boolean agt(String v)
	{
		return ge(v) && lt(approximateUpper(v));
	}

	private String approximateUpper(String v)
	{
		Matcher m = PATTERN_APPROXIMATION.matcher(v.split("\\.\\d+$")[0]);
		if( m.find() )
		{
			int i = Integer.parseInt(m.group(2));
			return (null != m.group(1) ? m.group(1) : "") + ++i;
		}
		else
		{
			return v;
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return mVersion.toString();
	}
	
	
}
