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
package io.github.solf.extra2.objectgraph;

import static io.github.solf.extra2.util.NullUtil.nn;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Configuration options for object graph walking.
 * 
 * NOTE: order of checking {@link ObjectGraphUtil#determineHandling(ObjectGraphConfig, Object, Class, String, ObjectGraphRelationType, ObjectGraphCollectionStep[], Object)}:
 * - arrays are processed as arrays
 * - if collection handling is enabled, then check if type is any kind of collection (map included) 
 * - whether class matches any of the 'primitive' types
 * - whether class matches any of the 'compound' prefixes (this way the above primitive classes can serve as exclusions for this while this can serve as exclusions for primitive prefixes below) 
 * - whether class matches any of the 'primitive' prefixes
 * - check if enum default handling is set and if class is enum and if both true, then handle with default enum mode
 * - check whether class matches any of the 'user' prefixes and if so, handle per user handling mode setting
 * - check whether class matches any of the 'system' prefixes and if so, handle per system handling mode setting
 * - check how unknown classes must be treated ({@link #isUnknownClassesDefaultToUser()}) and handle accordingly
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class ObjectGraphConfig implements Cloneable
{
	/**
	 * Default known primitive types.
	 */
	public static final Set<Class<?>> DEFAULT_PRIMITIVE_TYPES;
	
	/**
	 * Default prefixes for classes that are considered 'system'. 
	 */
	public static final Set<String> DEFAULT_SYSTEM_CLASS_PREFIXES;
	
	/**
	 * Default prefixes for classes that are considered 'user' (defaults to empty set).
	 */
	public static final Set<String> DEFAULT_USER_CLASS_PREFIXES;
	
	/**
	 * Default prefixes for classes that are considered 'primitive' (these are not deduplicated [always reported even if the same] and are not 'walked in').
	 */
	public static final Set<String> DEFAULT_PRIMITIVE_CLASS_PREFIXES;
	
	/**
	 * Default prefixes for classes that are considered 'compound'.
	 */
	public static final Set<String> DEFAULT_COMPOUND_CLASS_PREFIXES;
	
	
	
	static
	{
		{
			Set<String> proto = new HashSet<String>();
			proto.add("java.");
			proto.add("javax.");
			proto.add("com.sun.");
			DEFAULT_SYSTEM_CLASS_PREFIXES = nn(Collections.unmodifiableSet(proto));
		}
		
		{
			DEFAULT_USER_CLASS_PREFIXES = nn(Collections.unmodifiableSet(new HashSet<String>()));
		}
		
		{
			Set<String> proto = new HashSet<String>();
			proto.add("java.sql.");
			proto.add("org.joda.time.");
			proto.add("java.util.concurrent.atomic.");
			DEFAULT_PRIMITIVE_CLASS_PREFIXES = nn(Collections.unmodifiableSet(proto));
		}
		
		{
			Set<String> proto = new HashSet<String>();
			DEFAULT_COMPOUND_CLASS_PREFIXES = nn(Collections.unmodifiableSet(proto));
		}
		
		{
			Set<Class<?>> proto = new HashSet<Class<?>>();
			proto.add(Boolean.class);
			proto.add(Byte.class);
			proto.add(Character.class);
			proto.add(Class.class);
			proto.add(Double.class);
			proto.add(Float.class);
			proto.add(Integer.class);
			proto.add(Long.class);
			proto.add(Object.class);
			proto.add(Short.class);
			proto.add(String.class);
			proto.add(StringBuffer.class);
			proto.add(StringBuilder.class);
			proto.add(Void.class);
			proto.add(BigDecimal.class);
			proto.add(BigInteger.class);
			proto.add(Date.class);
			DEFAULT_PRIMITIVE_TYPES = nn(Collections.unmodifiableSet(proto));
		}
	}
	
	/**
	 * Default: false. Whether null fields are included in the walk.
	 */
	private boolean includeNullFields = false;
	
	/**
	 * Default: true. Whether to visit once for each parent (false means that compound object is visited only once even if it has multiple parents -- effectively 'random' parent is chosen for visit).
	 * NOTE: this option DOES NOT affect primitive types and collection instances (if collection instances are requested)
	 */
	private boolean visitForEachParent = true;
	
	/**
	 * Default: true. Whether to handle collections as collections (false means that collections aren't specifically handled and thus collection-related relation types are not used).
	 */
	private boolean handleCollections = true;
	
	/**
	 * Default: false. 
	 * Whether to include collection holders (this also includes Maps).
	 * If true, then relation type {@link ObjectGraphRelationType#COLLECTION_INSTANCE}
	 * is used to visit actual collection objects.
	 * If false, then actual collection objects are not visited (only elements
	 * in the collection itself are visited) and relation {@link ObjectGraphRelationType#COLLECTION_INSTANCE}
	 * is not used.
	 */
	private boolean includeCollectionHolders = false;
	
	/**
	 * Default: true. Whether unknown classes default to 'user' type (if false -- then to 'system' type).
	 */
	private boolean unknownClassesDefaultToUser = true;
	
	/**
	 * Default: umodifiable set {@link #DEFAULT_PRIMITIVE_TYPES}. Set of types that are considered primitive (no dedup, no 'walk in').
	 */
	private Set<Class<?>> primitiveTypes = DEFAULT_PRIMITIVE_TYPES;
	
	/**
	 * Default: umodifiable set {@link #DEFAULT_SYSTEM_CLASS_PREFIXES}. Set of prefixes that identify what is considered a system class.
	 */
	private Set<String> systemClassPrefixes = DEFAULT_SYSTEM_CLASS_PREFIXES;

	/**
	 * Default: unmodifiable empty set. Set of prefixes that identify what is considered a user class.
	 */
	private Set<String> userClassPrefixes = DEFAULT_USER_CLASS_PREFIXES;
	
	/**
	 * Default: umodifiable set {@link #DEFAULT_PRIMITIVE_CLASS_PREFIXES}. Set of prefixes that identify what is considered a primitive class (no dedup, no 'walk in').
	 */
	private Set<String> primitiveClassPrefixes = DEFAULT_PRIMITIVE_CLASS_PREFIXES;
	
	/**
	 * Default: umodifiable set {@link #DEFAULT_COMPOUND_CLASS_PREFIXES}. Set of prefixes that identify what is considered a compound class (require 'walk in' and possible dedup).
	 */
	private Set<String> compoundClassPrefixes = DEFAULT_COMPOUND_CLASS_PREFIXES;
	
	/**
	 * Default: {@link ObjectGraphHandleMode#EXCEPTION}. Default mode for handling classes considered 'system' ({@link #getSystemClassPrefixes()}) that aren't otherwise marked as primitive/compound.
	 */
	private ObjectGraphHandleMode systemClassHandleMode = ObjectGraphHandleMode.EXCEPTION;
	
	/**
	 * Default: {@link ObjectGraphHandleMode#COMPOUND}. Default mode for handling classes considered 'user' (those that are not in {@link #getSystemClassPrefixes()}) that aren't otherwise marked as primitive/compound.
	 */
	private ObjectGraphHandleMode userClassHandleMode = ObjectGraphHandleMode.COMPOUND;
	
	/**
	 * Default: {@link ObjectGraphHandleMode#PRIMITIVE}. Default mode for handling enum classes that aren't otherwise marked as primitive/compound.
	 * 'null' means no special handling for enums, handle as per other rules
	 */
	@Nullable
	private ObjectGraphHandleMode enumHandleMode = ObjectGraphHandleMode.PRIMITIVE;
	
	/**
	 * Default: false. Whether to include (visit) primitive types (those that are considered primitive according to configuration).
	 */
	private boolean includePrimitiveTypes = false;

	/**
	 * Default: false. Whether to include (visit) primitive types (those that are considered primitive according to configuration).
	 * @return Default: false. Whether to include (visit) primitive types (those that are considered primitive according to configuration)
	 */
	public boolean isIncludePrimitiveTypes()
	{
		return includePrimitiveTypes;
	}

	/**
	 * Default: false. Whether to include (visit) primitive types (those that are considered primitive according to configuration).
	 * @param newIncludePrimitiveTypes new value of Default: false. Whether to include (visit) primitive types (those that are considered primitive according to configuration)
	 */
	public ObjectGraphConfig setIncludePrimitiveTypes(boolean newIncludePrimitiveTypes)
	{
		includePrimitiveTypes = newIncludePrimitiveTypes;
		return this;
	}
	
	/**
	 * Default: true. Whether to include/navigate transient fields.
	 */
	private boolean includeTransientFields = true;
	
	/**
	 * Default: false. Whether to visit map keys.
	 */
	private boolean includeMapKeys = false;

	/**
	 * Default: false. Whether to visit map keys.
	 * @return Default: false. Whether to visit map keys
	 */
	public boolean isIncludeMapKeys()
	{
		return includeMapKeys;
	}

	/**
	 * Default: false. Whether to visit map keys.
	 * @param newIncludeMapKeys new value of Default: false. Whether to visit map keys
	 */
	public ObjectGraphConfig setIncludeMapKeys(boolean newIncludeMapKeys)
	{
		includeMapKeys = newIncludeMapKeys;
		return this;
	}

	/**
	 * Default: true. Whether to include/navigate transient fields.
	 * @return Default:true. Whether to include/navigate transient fields
	 */
	public boolean isIncludeTransientFields()
	{
		return includeTransientFields;
	}

	/**
	 * Default: true. Whether to include/navigate transient fields.
	 * @param newIncludeTransientFields new value of Default:true. Whether to include/navigate transient fields
	 */
	public ObjectGraphConfig setIncludeTransientFields(boolean newIncludeTransientFields)
	{
		includeTransientFields = newIncludeTransientFields;
		return this;
	}

	/**
	 * Default: unmodifiable empty set. Set of prefixes that identify what is considered a user class.
	 * @return Default: unmodifiable empty set. Set of prefixes that identify what is considered a user class
	 */
	public Set<String> getUserClassPrefixes()
	{
		return userClassPrefixes;
	}

	/**
	 * Default: unmodifiable empty set. Set of prefixes that identify what is considered a user class.
	 * @param newUserClassPrefixes new value of Default: unmodifiable empty set. Set of prefixes that identify what is considered a user class
	 */
	public ObjectGraphConfig setUserClassPrefixes(Set<String> newUserClassPrefixes)
	{
		userClassPrefixes = newUserClassPrefixes;
		return this;
	}
	
	/**
	 * Default: true. Whether unknown classes default to 'user' type (if false -- then to 'system' type).
	 * @return Default: true. Whether unknown classes default to 'user' type (if false -- then to 'system' type)
	 */
	public boolean isUnknownClassesDefaultToUser()
	{
		return unknownClassesDefaultToUser;
	}

	/**
	 * Default: true. Whether unknown classes default to 'user' type (if false -- then to 'system' type).
	 * @param newUnknownClassesDefaultToUser new value of Default: true. Whether unknown classes default to 'user' type (if false -- then to 'system' type)
	 */
	public ObjectGraphConfig setUnknownClassesDefaultToUser(boolean newUnknownClassesDefaultToUser)
	{
		unknownClassesDefaultToUser = newUnknownClassesDefaultToUser;
		return this;
	}

	/**
	 * Gets Default: {@link ObjectGraphHandleMode#PRIMITIVE}. Default mode for handling enum classes that aren't otherwise marked as primitive/compound.
	 * 'null' means no special handling for enums, handle as per other rules
	 * @return Default: {@link ObjectGraphHandleMode#PRIMITIVE}. Default mode for handling enum classes that aren't otherwise marked as primitive/compound
	 */
	@Nullable
	public ObjectGraphHandleMode getEnumHandleMode()
	{
		return enumHandleMode;
	}

	/**
	 * Sets Default: {@link ObjectGraphHandleMode#PRIMITIVE}. Default mode for handling enum classes that aren't otherwise marked as primitive/compound.
	 * 'null' means no special handling for enums, handle as per other rules
	 * @param newEnumHandleMode new value of Default: {@link ObjectGraphHandleMode#PRIMITIVE}. Default mode for handling enum classes that aren't otherwise marked as primitive/compound
	 */
	public ObjectGraphConfig setEnumHandleMode(@Nullable ObjectGraphHandleMode newEnumHandleMode)
	{
		enumHandleMode = newEnumHandleMode;
		return this;
	}

	/**
	 * Default: {@link ObjectGraphHandleMode#COMPOUND}. Default mode for handling classes considered 'user' (those that are not in {@link #getSystemClassPrefixes()}) that aren't otherwise marked as primitive/compound.
	 * @return Default: {@link ObjectGraphHandleMode#COMPOUND}. Default mode for handling classes considered 'user' (those that are not in {@link #getSystemClassPrefixes()}) that aren't otherwise marked as primitive/compound
	 */
	public ObjectGraphHandleMode getUserClassHandleMode()
	{
		return userClassHandleMode;
	}

	/**
	 * Default: {@link ObjectGraphHandleMode#COMPOUND}. Default mode for handling classes considered 'user' (those that are not in {@link #getSystemClassPrefixes()}) that aren't otherwise marked as primitive/compound.
	 * @param newUserClassHandleMode new value of Default: {@link ObjectGraphHandleMode#COMPOUND}. Default mode for handling classes considered 'user' (those that are not in {@link #getSystemClassPrefixes()}) that aren't otherwise marked as primitive/compound
	 */
	public ObjectGraphConfig setUserClassHandleMode(ObjectGraphHandleMode newUserClassHandleMode)
	{
		userClassHandleMode = newUserClassHandleMode;
		return this;
	}

	/**
	 * Default: {@link ObjectGraphHandleMode#EXCEPTION}. Default mode for handling classes considered 'system' ({@link #getSystemClassPrefixes()}) that aren't otherwise marked as primitive/compound.
	 * @return Default: {@link ObjectGraphHandleMode#EXCEPTION}. Default mode for handling classes considered 'system' ({@link #getSystemClassPrefixes()}) that aren't otherwise marked as primitive/compound
	 */
	public ObjectGraphHandleMode getSystemClassHandleMode()
	{
		return systemClassHandleMode;
	}

	/**
	 * Default: {@link ObjectGraphHandleMode#EXCEPTION}. Default mode for handling classes considered 'system' ({@link #getSystemClassPrefixes()}) that aren't otherwise marked as primitive/compound.
	 * @param newSystemClassHandleMode new value of Default: {@link ObjectGraphHandleMode#EXCEPTION}. Default mode for handling classes considered 'system' ({@link #getSystemClassPrefixes()}) that aren't otherwise marked as primitive/compound
	 */
	public ObjectGraphConfig setSystemClassHandleMode(ObjectGraphHandleMode newSystemClassHandleMode)
	{
		systemClassHandleMode = newSystemClassHandleMode;
		return this;
	}

	/**
	 * Gets Default: umodifiable set {@link #DEFAULT_COMPOUND_CLASS_PREFIXES}. Set of prefixes that identify what is considered a compound class (require 'walk in' and possible dedup).
	 * @return Default: umodifiable set {@link #DEFAULT_COMPOUND_CLASS_PREFIXES}. Set of prefixes that identify what is considered a compound class (require 'walk in' and possible dedup)
	 */
	public Set<String> getCompoundClassPrefixes()
	{
		return compoundClassPrefixes;
	}

	/**
	 * Sets Default: umodifiable set {@link #DEFAULT_COMPOUND_CLASS_PREFIXES}. Set of prefixes that identify what is considered a compound class (require 'walk in' and possible dedup).
	 * @param newCompoundClassPrefixes new value of Default: umodifiable set {@link #DEFAULT_COMPOUND_CLASS_PREFIXES}. Set of prefixes that identify what is considered a compound class (require 'walk in' and possible dedup)
	 */
	public ObjectGraphConfig setCompoundClassPrefixes(Set<String> newCompoundClassPrefixes)
	{
		compoundClassPrefixes = newCompoundClassPrefixes;
		return this;
	}

	/**
	 * Default: umodifiable set {@link #DEFAULT_PRIMITIVE_CLASS_PREFIXES}. Set of prefixes that identify what is considered a primitive class (no dedup, no 'walk in').
	 * @return Default: umodifiable set {@link #DEFAULT_PRIMITIVE_CLASS_PREFIXES}. Set of prefixes that identify what is considered a primitive class (no dedup, no 'walk in')
	 */
	public Set<String> getPrimitiveClassPrefixes()
	{
		return primitiveClassPrefixes;
	}

	/**
	 * Default: umodifiable set {@link #DEFAULT_PRIMITIVE_CLASS_PREFIXES}. Set of prefixes that identify what is considered a primitive class (no dedup, no 'walk in').
	 * @param newPrimitiveClassPrefixes new value of Default: umodifiable set {@link #DEFAULT_PRIMITIVE_CLASS_PREFIXES}. Set of prefixes that identify what is considered a primitive class (no dedup, no 'walk in')
	 */
	public ObjectGraphConfig setPrimitiveClassPrefixes(Set<String> newPrimitiveClassPrefixes)
	{
		primitiveClassPrefixes = newPrimitiveClassPrefixes;
		return this;
	}

	/**
	 * Default: umodifiable set {@link #DEFAULT_SYSTEM_CLASS_PREFIXES}. Set of prefixes that identify what is considered a system class.
	 * @return Default: umodifiable set {@link #DEFAULT_SYSTEM_CLASS_PREFIXES}. Set of prefixes that identify what is considered a system class
	 */
	public Set<String> getSystemClassPrefixes()
	{
		return systemClassPrefixes;
	}

	/**
	 * Default: umodifiable set {@link #DEFAULT_SYSTEM_CLASS_PREFIXES}. Set of prefixes that identify what is considered a system class.
	 * @param newSystemClassPrefixes new value of Default: umodifiable set {@link #DEFAULT_SYSTEM_CLASS_PREFIXES}. Set of prefixes that identify what is considered a system class
	 */
	public ObjectGraphConfig setSystemClassPrefixes(Set<String> newSystemClassPrefixes)
	{
		systemClassPrefixes = newSystemClassPrefixes;
		return this;
	}

	/**
	 * Default: umodifiable set {@link #DEFAULT_PRIMITIVE_TYPES}. Set of types that are considered primitive (no dedup, no 'walk in').
	 * @return Default: umodifiable set {@link #DEFAULT_PRIMITIVE_TYPES}. Set of types that are considered primitive (no dedup, no 'walk in')
	 */
	public Set<Class<?>> getPrimitiveTypes()
	{
		return primitiveTypes;
	}

	/**
	 * Default: umodifiable set {@link #DEFAULT_PRIMITIVE_TYPES}. Set of types that are considered primitive (no dedup, no 'walk in').
	 * @param newPrimitiveTypes new value of Default: umodifiable set {@link #DEFAULT_PRIMITIVE_TYPES}. Set of types that are considered primitive (no dedup, no 'walk in')
	 */
	public ObjectGraphConfig setPrimitiveTypes(Set<Class<?>> newPrimitiveTypes)
	{
		primitiveTypes = newPrimitiveTypes;
		return this;
	}

	/**
	 * Default: false. 
	 * Whether to include collection holders (this also includes Maps).
	 * If true, then relation type {@link ObjectGraphRelationType#COLLECTION_INSTANCE}
	 * is used to visit actual collection objects.
	 * If false, then actual collection objects are not visited (only elements
	 * in the collection itself are visited) and relation {@link ObjectGraphRelationType#COLLECTION_INSTANCE}
	 * is not used.
	 * @return Default: false. Whether to include collection holders
	 */
	public boolean isIncludeCollectionHolders()
	{
		return includeCollectionHolders;
	}

	/**
	 * Default: false. 
	 * Whether to include collection holders (this also includes Maps).
	 * If true, then relation type {@link ObjectGraphRelationType#COLLECTION_INSTANCE}
	 * is used to visit actual collection objects.
	 * If false, then actual collection objects are not visited (only elements
	 * in the collection itself are visited) and relation {@link ObjectGraphRelationType#COLLECTION_INSTANCE}
	 * is not used.
	 * @param newIncludeCollectionHolders new value of Default: false. Whether to include collection holders
	 */
	public ObjectGraphConfig setIncludeCollectionHolders(boolean newIncludeCollectionHolders)
	{
		includeCollectionHolders = newIncludeCollectionHolders;
		return this;
	}

	/**
	 * Default: true. Whether to handle collections as collections (false means that collections aren't specifically handled and thus collection-related relation types are not used).
	 * @return Default: true. Whether to handle collections as collections (false means that collections aren't specifically handled and thus collection-related relation types are not used)
	 */
	public boolean isHandleCollections()
	{
		return handleCollections;
	}

	/**
	 * Default: true. Whether to handle collections as collections (false means that collections aren't specifically handled and thus collection-related relation types are not used).
	 * @param newHandleCollections new value of Default: true. Whether to handle collections as collections (false means that collections aren't specifically handled and thus collection-related relation types are not used)
	 */
	public ObjectGraphConfig setHandleCollections(boolean newHandleCollections)
	{
		handleCollections = newHandleCollections;
		return this;
	}

	/**
	 * Default: true. Whether to visit once for each parent (false means that compound object is visited only once even if it has multiple parents -- effectively 'random' parent is chosen for visit).
	 * NOTE: this option DOES NOT affect primitive types and collection instances (if collection instances are requested)
	 * @return Default: true. Whether to visit once for each parent (false means that compound object is visited only once even if it has multiple parents -- effectively 'random' parent is chosen for visit)
	 */
	public boolean isVisitForEachParent()
	{
		return visitForEachParent;
	}

	/**
	 * Default: true. Whether to visit once for each parent (false means that compound object is visited only once even if it has multiple parents -- effectively 'random' parent is chosen for visit).
	 * NOTE: this option DOES NOT affect primitive types and collection instances (if collection instances are requested)
	 * @param newVisitForEachParent new value of Default: true. Whether to visit once for each parent (false means that compound object is visited only once even if it has multiple parents -- effectively 'random' parent is chosen for visit)
	 */
	public ObjectGraphConfig setVisitForEachParent(boolean newVisitForEachParent)
	{
		visitForEachParent = newVisitForEachParent;
		return this;
	}

	/**
	 * Default: false. Whether null fields are included in the walk.
	 * @return Default: false. Whether null fields are included in the walk
	 */
	public boolean isIncludeNullFields()
	{
		return includeNullFields;
	}

	/**
	 * Default: false. Whether null fields are included in the walk.
	 * @param newIncludeNullFields new value of Default: false. Whether null fields are included in the walk
	 */
	public ObjectGraphConfig setIncludeNullFields(boolean newIncludeNullFields)
	{
		includeNullFields = newIncludeNullFields;
		return this;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public ObjectGraphConfig clone()
	{
		try
		{
			return (ObjectGraphConfig)nn(super.clone());
		} catch (CloneNotSupportedException e)
		{
			// WTF?
			throw new IllegalStateException("ASSERTION FAILED: clone not supported: " + e, e);
		}
	}
}
