/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.storage;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Used to define 'already exist' modes.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public enum ExistMode
{
	/** Item must already exist */
	MUST_EXIST,
	/** Item must NOT already exist (descriptor doesn't immediately create it) */
	MUST_NOT_EXIST,
	/** Item either must NOT already exist (it is created) or it must be empty */
	MUST_BE_EMPTY_OR_NOT_EXIST,
	/** Item must exists and be non-empty */
	MUST_BE_NON_EMPTY,
	/** Any is fine */
	ANY,
	;
}
