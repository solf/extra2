/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.log4j;

import static site.sonata.extra2.util.NullUtil.fakeNonNull;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.varia.StringMatchFilter;

import lombok.Getter;
import lombok.Setter;

/**
 * This is a version of {@link StringMatchFilter} that additionally matches
 * on exception string (NOT stacktrace).
 * <p>
 * For a match to be established, both string and exception string must match.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class StringAndExceptionMatchFilter extends StringMatchFilter
{
	/**
	 * Exception message must contain this (sub)string to match.
	 */
	@Getter @Setter
	String exceptionStringToMatch = fakeNonNull();

	/* (non-Javadoc)
	 * @see org.apache.log4j.varia.StringMatchFilter#decide(org.apache.log4j.spi.LoggingEvent)
	 */
	@Override
	public int decide(LoggingEvent event)
	{
		int superResult = super.decide(event);
		if (superResult == Filter.NEUTRAL)
			return superResult;
		
		if (event.getThrowableInformation() == null)
			return Filter.NEUTRAL;
		
		if (!event.getThrowableInformation().getThrowable().toString().contains(exceptionStringToMatch))
			return Filter.NEUTRAL;
		
		return superResult;
	}
	
}
