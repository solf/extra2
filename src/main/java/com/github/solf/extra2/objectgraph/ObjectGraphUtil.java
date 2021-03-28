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
package com.github.solf.extra2.objectgraph;

import static com.github.solf.extra2.util.NullUtil.nn;
import static com.github.solf.extra2.util.NullUtil.nnc;
import static com.github.solf.extra2.util.NullUtil.nullable;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.solf.extra2.collection.IdentityHashSet;
import com.github.solf.extra2.objectgraph.ObjectGraphUnhandledTypeException.VisiteeClassClassification;
import com.github.solf.extra2.util.ReflectionUtil;

/**
 * Utility methods for working with (navigating) in-memory object graphs.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class ObjectGraphUtil
{
	/**
	 * Context for object graph processing.
	 */
	/*package*/ static class Context
	{
		/**
		 * Root.
		 */
		final Object root;
		
		/**
		 * Config.
		 */
		final ObjectGraphConfig cfg;
		
		/**
		 * Visitor.
		 */
		final ObjectGraphVisitor visitor;
		
		/**
		 * Stack of compound objects to process.
		 */
		final Deque<Object> compoundObjectsToProcess = new ArrayDeque<Object>();
		
		/**
		 * Map of compound objects that have already been seen.
		 */
		final IdentityHashSet<Object> seenCompoundObjects = new IdentityHashSet<Object>();
		
		/**
		 * Constructor.
		 */
		public Context(Object root, ObjectGraphConfig cfg, ObjectGraphVisitor visitor)
		{
			this.root = root;
			this.cfg = cfg;
			this.visitor = visitor;
		}
	}
	
	/**
	 * Simple holder for an object.
	 */
	private static class Holder
	{
		/**
		 * Held object.
		 */
		@SuppressWarnings("unused")
		final Object heldObject;
		
		/**
		 * Constructor.
		 */
		public Holder(Object heldObject)
		{
			this.heldObject = heldObject;
		}
	}
	
	/**
	 * Possible handling modes.
	 */
	private static enum HandleMode
	{
		PRIMITIVE,
		COMPOUND,
		/** This includes maps, lists, and other collections (but not arrays) */
		COLLECTION,
		ARRAY,
		;
	}
	
	/**
	 * 'Visits' all objects in the given graph (excluding initial root visitation
	 * because it has no parent -- but root can still be visited if it is 
	 * referenced from one of the other objects in the graph and visitation is
	 * required for each parent).
	 * The actual visitation is carried out in terms of 'relations' -- i.e.
	 * we don't simply visit an object, we visit object with information how
	 * it is included in its parent -- this is particularly useful for primitive
	 * types.
	 * 
	 * NOTE: root object must be a compound object and not a collection/map/array;
	 * if you want to handle such cases, consider creating a simple holder object
	 * (in which case the actual root object will also be visited). The reason
	 * for this restriction is that collection-type objects require parent
	 * for proper walk.
	 * 
	 * 'Visitation'/'walk' is performed via reflection and it examines all fields --
	 * including private fields and those that are overloaded.
	 * 
	 * The implementation handles cycles correctly (no infinite loop). 
	 * 
	 * This method makes two important distinctions: primitive vs compound classes
	 * and system vs user classes.
	 * 
	 * If a class is considered 'primitive', then it will not be de-duplicated
	 * (i.e. it will appear for each time it is included in some parent class
	 * even if it is the same) and it will not be 'walked into' (i.e. its fields
	 * will not be inspected).
	 * For 'compound' classes this is reversed -- they are optionally de-duplicated
	 * ({@link ObjectGraphConfig#isVisitForEachParent()}) and are 'walked into' --
	 * i.e. their fields are inspected and visited.
	 * 
	 * The distinction between 'user' and 'system' classes allows to select different
	 * default handling mode for them.
	 * 
	 * Please see {@link ObjectGraphConfig} for available configuration options.
	 * 
	 * NOTE ON MODIFICATIONS: by the time relation involving particular field
	 * is passed to the visitor the value in that field has already bean read
	 * and processed -- and will no longer be read or used. So any changes to
	 * this field will not affect such processing. However any other changes
	 * to the structure may have an impact (because processing order is not
	 * guaranteed).
	 * 
	 * DO be aware that single field may be visited multiple times (in case of
	 * collections/arrays) -- if you modify its value, it'll not affect {@link ObjectGraphUtil},
	 * but do be prepared in your own code.
	 * 
	 * ATTENTION: maps that have keys that are themselves collections (or maps, or arrays)
	 * are not supported -- primarily because of difficulties with fitting them
	 * into the data model.
	 * 
	 * @param visitor this can also implement {@link ObjectGraphExtendedVisitor}
	 * 		to access more events
	 * 
	 * @throws ObjectGraphUnhandledTypeException if handling for some field
	 * 		cannot be determined
	 * @throws IllegalArgumentException if something's wrong with arguments
	 * @throws IllegalStateException if something goes wrong -- e.g. reflection failure
	 */
	public static void visitGraphExcludingRoot(Object root, ObjectGraphConfig cfg,
		ObjectGraphVisitor visitor) throws ObjectGraphUnhandledTypeException, IllegalStateException, IllegalArgumentException
	{
		// Sanity check because client code wouldn't necessarily respect annotations
		if (nullable(root) == null)
			throw new IllegalArgumentException("root object may not be null.");
		if (nullable(cfg) == null)
			throw new IllegalArgumentException("config may not be null.");
		if (nullable(visitor) == null)
			throw new IllegalArgumentException("visitor may not be null.");
		
		Context ctx = new Context(root, cfg, visitor);
		
		// Verify that root node is valid
		{
			HandleMode rootHandling = determineHandling(ctx.cfg, null, null, null, null, null, root);
			switch(rootHandling)
			{
				case COMPOUND:
					break; // This is valid option.
				case ARRAY:
				case COLLECTION:
				case PRIMITIVE:
					throw new IllegalStateException("root object must be a compound object and not a collection or an array, got: " + rootHandling);
			}
		}
		
		ctx.seenCompoundObjects.add(root); // root is considered 'visited'
		ctx.compoundObjectsToProcess.push(root); // queue root for handling
		
		while(true)
		{
			Object obj = ctx.compoundObjectsToProcess.poll();
			if (obj == null)
				break; // no more objects to process
			
			// Process single compound object.
			{
				for (Class<?> clazz = obj.getClass(); clazz != null; clazz = clazz.getSuperclass())
				{
					Field[] fields = clazz.getDeclaredFields();
					for (Field field : fields)
					{
						if (Modifier.isStatic(field.getModifiers()))
							continue; // Skip static fields.
						if (!ctx.cfg.isIncludeTransientFields() && (Modifier.isTransient(field.getModifiers())))
							continue; // Skip transient fields if required.
						
						if (!ReflectionUtil.isAccessible(field))
							field.setAccessible(true);
						final String fieldName = nn(field.getName());
						final Object value;
						try
						{
							value = field.get(obj);
						} catch (IllegalAccessException e)
						{
							throw new IllegalStateException("Reflection failed: " + e, e);
						}
						
						handleItem(ctx, obj, clazz, field, fieldName, ObjectGraphRelationType.FIELD, null, value, field.getType().isPrimitive());
					}
					
				}
			}
			
			// Done processing compound object -- notify if required.
			if (obj != root) // Do not notify for root.
			{
				if (visitor instanceof ObjectGraphExtendedVisitor)
				{
					((ObjectGraphExtendedVisitor)visitor).finishedCompoundObject(obj);
				}
			}
		}
	}
	
	/**
	 * Handles an item -- determines its type and handles accordingly.
	 */
	private static void handleItem(Context ctx, Object parent, Class<?> fieldContainer, Field field,
		String fieldName, ObjectGraphRelationType relationType, ObjectGraphCollectionStep @Nullable[] path, 
		@Nullable Object object, boolean isPrimitive)
	{
		if (object == null) // handle nulls -- visit if requested
		{
			if (ctx.cfg.isIncludeNullFields())
				ctx.visitor.visit(new ObjectGraphRelation(parent, fieldContainer, field, fieldName, relationType, path, object));
			return;
		}
		
		HandleMode handleMode;
		if (isPrimitive)
			handleMode = HandleMode.PRIMITIVE;
		else
			handleMode = determineHandling(ctx.cfg, parent, fieldContainer, fieldName, relationType, path, object);
		
		switch (handleMode)
		{
			case PRIMITIVE:
				if (ctx.cfg.isIncludePrimitiveTypes())
					ctx.visitor.visit(new ObjectGraphRelation(parent, fieldContainer, field, fieldName, relationType, path, object));
				break;
			case COMPOUND:
				boolean alreadySeen = ctx.seenCompoundObjects.contains(object);
				
				// Compound objects are visited if it is the first time or if configured to visit for each parent
				if (!alreadySeen || ctx.cfg.isVisitForEachParent())
					ctx.visitor.visit(new ObjectGraphRelation(parent, fieldContainer, field, fieldName, relationType, path, object));
				
				if (!alreadySeen)
				{
					ctx.seenCompoundObjects.add(object);
					ctx.compoundObjectsToProcess.push(object); // push for fields processing
				}
				break;
			case ARRAY:
			case COLLECTION:
				handleCollection(ctx, parent, fieldContainer, field, fieldName, path, object);
				break;
		}
		
	}
	
	/**
	 * Handles collection.
	 */
	private static void handleCollection(Context ctx, Object parent,
		Class<?> fieldContainer, Field field, String fieldName,
		ObjectGraphCollectionStep @Nullable[] pathToCollection, Object collection)
	{
		if (ctx.cfg.isIncludeCollectionHolders())
			ctx.visitor.visit(new ObjectGraphRelation(parent, fieldContainer, field, fieldName, ObjectGraphRelationType.COLLECTION_INSTANCE, pathToCollection, collection));
		
		// Handle arrays.
		if (collection.getClass().isArray())
		{
			boolean isPrimitiveArray = collection.getClass().getComponentType().isPrimitive();
			
			int length = Array.getLength(collection);
			for (int i = 0; i < length; i++)
			{
				ObjectGraphCollectionStep[] path = appendStep(pathToCollection, ObjectGraphCollectionType.ARRAY, i);
				Object item = Array.get(collection, i);
				
				handleItem(ctx, parent, fieldContainer, field, fieldName, ObjectGraphRelationType.ITEM_IN_COLLECTION, path, item, isPrimitiveArray);
			}
			return;
		}
		
		// Handle lists.
		if (collection instanceof List<?>)
		{
			List<?> list = (List<?>)collection;
			int length = list.size();
			for (int i = 0; i < length; i++)
			{
				ObjectGraphCollectionStep[] path = appendStep(pathToCollection, ObjectGraphCollectionType.LIST, i);
				Object item = list.get(i);
				
				handleItem(ctx, parent, fieldContainer, field, fieldName, ObjectGraphRelationType.ITEM_IN_COLLECTION, path, item, false);
			}
			
			return;
		}
		
		// Handle Maps.
		if (collection instanceof Map<?, ?>)
		{
			Map<?, ?> map = (Map<?, ?>)collection;
			
			if (ctx.cfg.isIncludeMapKeys()) // Visit map keys if required
			{
				for (Object key : map.keySet())
				{
					if (key != null)
					{
						HandleMode keyType = determineHandling(ctx.cfg, parent, fieldContainer, fieldName, ObjectGraphRelationType.MAP_KEY, pathToCollection, key);
						switch (keyType)
						{
							case ARRAY:
							case COLLECTION:
								throw new IllegalStateException("Collections/maps/arrays as map keys are not supported, found one in instance of class [" + parent.getClass().getName() + "], field [" + fieldName + "] defined in [" + fieldContainer.getName() + "], relation path is: " + Arrays.toString(pathToCollection) + ", key of type [" + key.getClass().getName() + "]. Parent object is: [" + parent + "], key is [" + key + "]");
							case COMPOUND:
							case PRIMITIVE:
								break; // These are ok
						}
					}	
					handleItem(ctx, parent, fieldContainer, field, fieldName, ObjectGraphRelationType.MAP_KEY, pathToCollection, key, false);
				}
			}
			
			for (Entry<?, ?> entry : map.entrySet())
			{
				ObjectGraphCollectionStep[] path = appendStep(pathToCollection, ObjectGraphCollectionType.MAP, entry.getKey());
				
				handleItem(ctx, parent, fieldContainer, field, fieldName, ObjectGraphRelationType.ITEM_IN_COLLECTION, path, entry.getValue(), false);
			}
			
			return;
		}

		// Handle collections
		if (collection instanceof Collection<?>)
		{
			Iterator<?> iter = ((Collection<?>)collection).iterator();
			int i = -1; // surrogate index.
			
			while(iter.hasNext())
			{
				i++;
				ObjectGraphCollectionStep[] path = appendStep(pathToCollection, ObjectGraphCollectionType.COLLECTION, i);
				Object item = iter.next();
				
				handleItem(ctx, parent, fieldContainer, field, fieldName, ObjectGraphRelationType.ITEM_IN_COLLECTION, path, item, false);
			}
			return;
		}
		
		throw new IllegalStateException("Non-collection type given to collection handling code: instance of class [" + parent.getClass().getName() + "], field [" + fieldName + "] defined in [" + fieldContainer.getName() + "], relation path is: " + Arrays.toString(pathToCollection) + ". Parent object is: [" + parent + "].");
	}

	/**
	 * Determines how given visitee should be handled.
	 * Most arguments are for error reporting.
	 * 
	 * @throws ObjectGraphUnhandledTypeException if specific type is determined
	 * 		to require throwing exception (which is then thrown).
	 */
	private static HandleMode determineHandling(ObjectGraphConfig cfg,
		@Nullable Object parent, @Nullable Class<?> fieldContainer, 
		@Nullable String fieldName, @Nullable ObjectGraphRelationType relationType, ObjectGraphCollectionStep @Nullable[] path, 
		Object visitee)
			throws ObjectGraphUnhandledTypeException
	{
		final Class<?> clazz = visitee.getClass(); 
		
		if (clazz.isArray())
			return HandleMode.ARRAY;
		
		if (cfg.isHandleCollections())
		{
			if (visitee instanceof Map<?, ?>)
				return HandleMode.COLLECTION;
			if (visitee instanceof Collection<?>)
				return HandleMode.COLLECTION;
		}
		
		if (cfg.getPrimitiveTypes().contains(clazz))
			return HandleMode.PRIMITIVE;
		
		String className = clazz.getName();
		
		for (String prefix : cfg.getCompoundClassPrefixes())
		{
			if (className.startsWith(prefix))
				return HandleMode.COMPOUND;
		}
		
		for (String prefix : cfg.getPrimitiveClassPrefixes())
		{
			if (className.startsWith(prefix))
				return HandleMode.PRIMITIVE;
		}
		
		if (cfg.getEnumHandleMode() != null)
		{
			if (visitee instanceof Enum<?>)
				return convertHandleMode(nn(cfg.getEnumHandleMode()), parent, fieldContainer, fieldName, relationType, path, visitee, VisiteeClassClassification.ENUM);
		}
		
		for (String prefix : cfg.getUserClassPrefixes())
		{
			if (className.startsWith(prefix))
				return convertHandleMode(cfg.getUserClassHandleMode(), parent, fieldContainer, fieldName, relationType, path, visitee, VisiteeClassClassification.USER);
		}
		
		for (String prefix : cfg.getSystemClassPrefixes())
		{
			if (className.startsWith(prefix))
				return convertHandleMode(cfg.getSystemClassHandleMode(), parent, fieldContainer, fieldName, relationType, path, visitee, VisiteeClassClassification.SYSTEM);
		}
		
		// Unknown class...
		if (cfg.isUnknownClassesDefaultToUser())
			return convertHandleMode(cfg.getUserClassHandleMode(), parent, fieldContainer, fieldName, relationType, path, visitee, VisiteeClassClassification.USER);
		else
			return convertHandleMode(cfg.getSystemClassHandleMode(), parent, fieldContainer, fieldName, relationType, path, visitee, VisiteeClassClassification.SYSTEM);
	}
	
	/**
	 * Converts {@link ObjectGraphHandleMode} to {@link HandleMode} or exception.
	 * Most arguments are here in order to be able to construct exception.
	 */
	private static HandleMode convertHandleMode(ObjectGraphHandleMode handleMode,
		@Nullable Object parent, @Nullable Class<?> fieldContainer, 
		@Nullable String fieldName, @Nullable ObjectGraphRelationType relationType, 
		ObjectGraphCollectionStep @Nullable[] path, 
		Object visitee, VisiteeClassClassification visiteeClassClassification)
		throws ObjectGraphUnhandledTypeException
	{
		switch(handleMode)
		{
			case COMPOUND:
				return HandleMode.COMPOUND;
			case PRIMITIVE:
				return HandleMode.PRIMITIVE;
			case EXCEPTION:
				throw new ObjectGraphUnhandledTypeException(parent, fieldContainer, fieldName, relationType, path, visitee, visiteeClassClassification);
		}
		
		throw new IllegalStateException("Assertion failed!");
	}
	
	/**
	 * Appends a new entry at the end of {@link ObjectGraphCollectionStep} array
	 * (creates a new array that is 1 bigger, copies existing data, sets last element)
	 */
	private static ObjectGraphCollectionStep[] appendStep(ObjectGraphCollectionStep @Nullable[] base,
		ObjectGraphCollectionType type, @Nullable Object key)
	{
		ObjectGraphCollectionStep[] path; // need to append our index to existing path (or create a new one)
		if (base == null)
		{
			path = nnc(new ObjectGraphCollectionStep[1]);
		}
		else
		{
			path = nnc(new ObjectGraphCollectionStep[base.length + 1]);
			System.arraycopy(base, 0, path, 0, base.length);
		}
		path[path.length - 1] = new ObjectGraphCollectionStep(type, key);
		
		return path;
	}
	
	/**
	 * Determines whether given instance is primitive according to the given configuration.
	 * 
	 * @throws ObjectGraphUnhandledTypeException if handling cannot be determined
	 */
	public static boolean isPrimitiveType(ObjectGraphConfig cfg, Object object)
		throws ObjectGraphUnhandledTypeException
	{
		if (determineHandling(cfg, null, null, null, null, null, object) == HandleMode.PRIMITIVE)
			return true;
		
		return false;
	}
	
	/**
	 * Visits all compound (not primitive and not collections) nodes on the graph
	 * including the root node.
	 * Root node must itself be a compound object or an array/collection -- otherwise exception.
	 * 
	 * Please see {@link ObjectGraphConfig} for available configuration options
	 * and {@link #visitGraphExcludingRoot(Object, ObjectGraphConfig, ObjectGraphVisitor)}
	 * for more details.
	 * 
	 * @param cfg configuration; NOTE: several options will be ignored and act as if set thus (actual values in given config are NOT changed):
	 * 		setIncludePrimitiveTypes -> false
	 * 		setVisitForEachParent -> false
	 * 		setIncludeCollectionHolders -> false
	 * 
	 * @throws ObjectGraphUnhandledTypeException if handling for some field
	 * 		cannot be determined
	 * @throws IllegalArgumentException if something's wrong with arguments
	 * @throws IllegalStateException if something goes wrong -- e.g. reflection failure
	 */
	public static void visitCompoundNodesIncludingRoot(Object root, ObjectGraphConfig argCfg, 
		final ObjectGraphCompoundNodeVisitor visitor)
		throws ObjectGraphUnhandledTypeException, IllegalArgumentException, 
			IllegalStateException
	{
		if (nullable(root) == null)
			throw new IllegalArgumentException("root object may not be null.");
		if (nullable(argCfg) == null)
			throw new IllegalArgumentException("config may not be null.");
		if (nullable(visitor) == null)
			throw new IllegalArgumentException("visitor may not be null.");
		
		// Verify that root node is valid
		{
			HandleMode rootHandling = determineHandling(argCfg, null, null, null, null, null, root);
			switch(rootHandling)
			{
				case COMPOUND:
				case ARRAY:
				case COLLECTION:
					break; // These are valid options.
				case PRIMITIVE:
					throw new IllegalStateException("root object must be a compound object and not a collection or an array, got: " + rootHandling);
			}
		}
		
		ObjectGraphConfig cfg = argCfg.clone();
		
		cfg.setIncludePrimitiveTypes(false);
		cfg.setVisitForEachParent(false);
		cfg.setIncludeCollectionHolders(false);
		
		visitGraphExcludingRoot(new Holder(root), cfg,
			new ObjectGraphExtendedVisitor()
			{
				
				@Override
				public void visit(ObjectGraphRelation relation)
				{
					// Do nothing here as we react to the other event.
				}
				
				@Override
				public void finishedCompoundObject(Object compoundObject)
				{
					visitor.visit(compoundObject);
				}
			}
		);
	}
}
