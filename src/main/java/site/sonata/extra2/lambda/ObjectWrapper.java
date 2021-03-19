/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.lambda;

import javax.annotation.DefaultLocation;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.ToString;

/**
 * Simple wrapper for an object.
 * <p>
 * Useful e.g. when lambda blocks / nested methods need to change something
 * available from the enclosing method/class.
 *
 * @author Sergey Olefir
 */
//Exclude TYPE_ARGUMENT as we will allow nullable type values.
@ParametersAreNonnullByDefault({DefaultLocation.PARAMETER, DefaultLocation.RETURN_TYPE, DefaultLocation.FIELD, DefaultLocation.TYPE_BOUND, DefaultLocation.ARRAY_CONTENTS})
@ToString
public class ObjectWrapper<T>
{
	/**
	 * Stored object.
	 */
	private T value;
	
	/**
	 * Constructor.
	 */
	protected ObjectWrapper(T value)
	{
		this.value = value;
	}
	
	/**
	 * Gets value.
	 */
	public T get()
	{
		return value;
	}
	
	/**
	 * Sets new value.
	 * 
	 * @return this for method chaining
	 */
	public ObjectWrapper<T> set(T newValue)
	{
		value = newValue;
		
		return this;
	}
	
	/**
	 * Constructs wrapper for the given argument.
	 */
	public static <T> ObjectWrapper<T> of(T value)
	{
		return new ObjectWrapper<T>(value);
	}
}
