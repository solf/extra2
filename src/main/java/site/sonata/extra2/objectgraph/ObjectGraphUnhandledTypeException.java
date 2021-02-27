/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.objectgraph;

import static site.sonata.extra2.util.NullUtil.nn;

import java.util.Arrays;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Thrown to indicate failure to determine proper handling for the field.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class ObjectGraphUnhandledTypeException extends RuntimeException
{
	/** Parent object (null if problem is with root object). */
	public final @Nullable Object parent;
	/** Class that actually contains this field declaration (e.g. private fields may appear multiple times in hierarchy */
	public final @Nullable Class<?> fieldContainer;
	/** Field name in parent (null if problem is with root object). */
	public final @Nullable String fieldName;
	/** Relation type (null if problem is with root object). */
	public final @Nullable ObjectGraphRelationType relationType;
	/** Relation path (if any is available) */
	public final ObjectGraphCollectionStep @Nullable[] path; 
	/** Object that actually is a problem (visitee). */
	public final Object visitee;
	/** Visitee class classification (user class/system class/enum...) */
	public final VisiteeClassClassification visiteeClassClassification;
	
	/**
	 * Visitee class classification (user class/system class/enum...)
	 */
	public static enum VisiteeClassClassification
	{
		USER,
		SYSTEM,
		ENUM,
		;
	}
	
	/**
	 * Constructor.
	 */
	public ObjectGraphUnhandledTypeException(@Nullable Object parent, @Nullable Class<?> fieldContainer, 
		@Nullable String fieldName, @Nullable ObjectGraphRelationType relationType, ObjectGraphCollectionStep @Nullable[] path, 
		Object visitee, VisiteeClassClassification visiteeClassClassification)
	{
		super(formatErrorMessage(parent, fieldContainer, fieldName, relationType, path, visitee, visiteeClassClassification));
		this.parent = parent;
		this.fieldContainer = fieldContainer;
		this.fieldName = fieldName;
		this.relationType = relationType;
		this.path = path;
		this.visitee = visitee;
		this.visiteeClassClassification = visiteeClassClassification;
	}
	
	/**
	 * Formats error message.
	 */
	private static String formatErrorMessage(@Nullable Object parent,  @Nullable Class<?> fieldContainer, 
		@Nullable String fieldName, @Nullable ObjectGraphRelationType relationType,
		ObjectGraphCollectionStep @Nullable[] path, Object visitee, VisiteeClassClassification visiteeClassClassification)
	{
		if (parent == null)
			return "Root object class [" + visitee.getClass().getName() + "] parses as unknown '" + visiteeClassClassification + "' class and these are not handled as per configuration. Root object is: " + visitee;
		
		return "Instance of class [" + parent.getClass().getName() + "], field [" + fieldName + "] defined in [" + nn(fieldContainer).getName() + "], relation type [" + relationType + "] has child of type [" + visitee.getClass().getName() + "] that parses as unknown '" + visiteeClassClassification + "' class and these are not handled as per configuration. Relation path is: " + Arrays.toString(path) + ", parent object is: [" + parent + "], child object is [" + visitee + "]";
	}
}
