/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.objectgraph;

/**
 * Parent for test object.
 *
 * @author Sergey Olefir
 */
//@ParametersAreNonnullByDefault // No annotations because they make writing test code annoying.
/*package*/ abstract class OGTOParent
{
	public transient String transientName = null; // For testing field hiding & transients
	
	private OGTestObject[] array = null; // For testing field 'hiding'
	
	/**
	 * Gets array.
	 */
	public OGTestObject[] getParentArray()
	{
		return array;
	}
	
	/**
	 * Sets array.
	 */
	public OGTOParent setParentArray(OGTestObject[] array)
	{
		this.array = array;
		
		return this;
	}

}
