/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.codegenerate.stepbuilder;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * What mode is used for suppressing warnings.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
@RequiredArgsConstructor
public enum SuppressWarningsMode
{
	/**
	 * No warnings should be suppressed.
	 */
	NONE(null),
	/**
	 * 'unused' warnings should be suppressed.
	 */
	UNUSED("unused"),
	/**
	 * 'all' warnings should be suppressed.
	 */
	ALL("all"),
	;
	
	/**
	 * Token that can be used to suppress this kind of warnings. 
	 */
	@Getter
	@Nullable
	private final String warningsSuppressString;
}
