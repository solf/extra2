/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.log4j;

import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Log4j extension that also logs {@link LoggingInheritableContext} at the end of the string.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class PatternLayoutWithInheritableContext extends PatternLayout
{
	/**
	 * Whether to append new line at the end of string when formatting.
	 * <p>
	 * Depends on whether original pattern ends with %n
	 */
	private boolean appendNewLine;
	
	/**
	 * Constructs with default pattern, {@link PatternLayout#DEFAULT_CONVERSION_PATTERN} 
	 * 
	 */
	public PatternLayoutWithInheritableContext()
	{
		this(DEFAULT_CONVERSION_PATTERN);
	}

	/**
	 * @param pattern
	 */
	public PatternLayoutWithInheritableContext(String pattern)
	{
		super(); // We'll set pattern later on
		setConversionPattern(pattern);
	}

	/* (non-Javadoc)
	 * @see org.apache.log4j.PatternLayout#format(org.apache.log4j.spi.LoggingEvent)
	 */
	@Override
	public String format(LoggingEvent event)
	{
		String msg = super.format(event);
		
		Supplier<String> ctxSupplier = LoggingInheritableContext.getCurrentContext();
		String ctx;
		if (ctxSupplier == null)
			ctx = "null";
		else
			ctx = ctxSupplier.get();
		
		if (appendNewLine)
			return msg + " [ihc:" + ctx + "]" + System.lineSeparator();
		else
			return msg + " [ihc:" + ctx + "]";
	}

	/* (non-Javadoc)
	 * @see org.apache.log4j.PatternLayout#setConversionPattern(java.lang.String)
	 */
	@Override
	public void setConversionPattern(String conversionPattern)
	{
		if (conversionPattern.endsWith("%n") && conversionPattern.length() > 2)
		{
			String subPattern = conversionPattern.substring(0, conversionPattern.length() - 2);
			appendNewLine = true;
			super.setConversionPattern(subPattern);
		}
		else
		{
			appendNewLine = false;
			super.setConversionPattern(conversionPattern);
		}
	}
}
