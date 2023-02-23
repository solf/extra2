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
import static io.github.solf.extra2.util.NullUtil.nullable;
import static org.testng.Assert.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.testng.annotations.Test;

import io.github.solf.extra2.collection.IdentityHashSet;
import io.github.solf.extra2.objectgraph.ObjectGraphCollectionStep;
import io.github.solf.extra2.objectgraph.ObjectGraphCollectionType;
import io.github.solf.extra2.objectgraph.ObjectGraphCompoundNodeVisitor;
import io.github.solf.extra2.objectgraph.ObjectGraphConfig;
import io.github.solf.extra2.objectgraph.ObjectGraphHandleMode;
import io.github.solf.extra2.objectgraph.ObjectGraphRelationType;
import io.github.solf.extra2.objectgraph.ObjectGraphUnhandledTypeException;
import io.github.solf.extra2.objectgraph.ObjectGraphUtil;
import io.github.solf.extra2.objectgraph.ObjectGraphUtil.ObjectGraphVisiteeHandleMode;
import io.github.solf.extra2.objectgraph2.OGTestObject2;

/**
 * Tests for {@link ObjectGraphUtil}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class OGTest
{
	@Test
	public void test()
	{
		List<OGRelation> tmp = new ArrayList<OGRelation>();
		
		OGTestObject root = new OGTestObject(0);
		OGTestObject i1 = new OGTestObject(1);
		OGTestObject i2 = new OGTestObject(2);
		OGTestObject i3 = new OGTestObject(3);
		OGTestObject i4 = new OGTestObject(4);
		OGTestObject i5 = new OGTestObject(5);
		OGTestObject i6 = new OGTestObject(6);
		OGTestObject i7 = new OGTestObject(7);
		
		root.map = new HashMap<String, OGTestObject>();
		root.map.put("1", i1);
		root.map.put("2", i2);
		root.sampleEnum = OGTestEnum.VAL1;
		
		registerDirectFields(tmp, root);
		tmp.add( new OGRelation(root, OGTestObject.class, "map", ObjectGraphRelationType.MAP_KEY, null, "1") );
		tmp.add( new OGRelation(root, OGTestObject.class, "map", ObjectGraphRelationType.MAP_KEY, null, "2") );
		tmp.add( new OGRelation(root, OGTestObject.class, "map", ObjectGraphRelationType.ITEM_IN_COLLECTION, path(ObjectGraphCollectionType.MAP, "1"), i1) );
		tmp.add( new OGRelation(root, OGTestObject.class, "map", ObjectGraphRelationType.ITEM_IN_COLLECTION, path(ObjectGraphCollectionType.MAP, "2"), i2) );
		
		tmp.add( new OGRelation(OGTestEnum.VAL1, OGTestEnum.class, "lowerCaseName", ObjectGraphRelationType.FIELD, null, "val1") );
		tmp.add( new OGRelation(OGTestEnum.VAL1, Enum.class, "name", ObjectGraphRelationType.FIELD, null, "VAL1") );
		tmp.add( new OGRelation(OGTestEnum.VAL1, Enum.class, "ordinal", ObjectGraphRelationType.FIELD, null, 0) );
		tmp.add( new OGRelation(OGTestEnum.VAL2, OGTestEnum.class, "lowerCaseName", ObjectGraphRelationType.FIELD, null, "val2") );
		tmp.add( new OGRelation(OGTestEnum.VAL2, Enum.class, "name", ObjectGraphRelationType.FIELD, null, "VAL2") );
		tmp.add( new OGRelation(OGTestEnum.VAL2, Enum.class, "ordinal", ObjectGraphRelationType.FIELD, null, 1) );
		
		i2.list = new ArrayList<OGTestObject>();
		i2.list.add(i3);
		i2.object = i7;
		OGTestObject2 object2 = new OGTestObject2("object2");
		i2.object2 = object2;
		
		registerDirectFields(tmp, i2);
		tmp.add( new OGRelation(i2, OGTestObject.class, "list", ObjectGraphRelationType.ITEM_IN_COLLECTION, path(ObjectGraphCollectionType.LIST, 0), i3) );
		tmp.add( new OGRelation(object2, OGTestObject2.class, "stringId", ObjectGraphRelationType.FIELD, null, object2.stringId) );
		
		i3.collection = new ArrayList<OGTestObject>(); // Use list to preserve order, later convert to collection.
		i3.collection.add(i2);
		i3.collection.add(i1);
		i3.collection.add(root);
		i3.collection.add(null);
		i3.collection = Collections.unmodifiableCollection(i3.collection); // convert to collection
		i4.collection = i3.collection; // share a collection between two instances
		
		tmp.add( new OGRelation(i3, OGTestObject.class, "collection", ObjectGraphRelationType.ITEM_IN_COLLECTION, path(ObjectGraphCollectionType.COLLECTION, 0), i2) );
		tmp.add( new OGRelation(i3, OGTestObject.class, "collection", ObjectGraphRelationType.ITEM_IN_COLLECTION, path(ObjectGraphCollectionType.COLLECTION, 1), i1) );
		tmp.add( new OGRelation(i3, OGTestObject.class, "collection", ObjectGraphRelationType.ITEM_IN_COLLECTION, path(ObjectGraphCollectionType.COLLECTION, 2), root) );
		tmp.add( new OGRelation(i3, OGTestObject.class, "collection", ObjectGraphRelationType.ITEM_IN_COLLECTION, path(ObjectGraphCollectionType.COLLECTION, 3), null) );
		tmp.add( new OGRelation(i4, OGTestObject.class, "collection", ObjectGraphRelationType.ITEM_IN_COLLECTION, path(ObjectGraphCollectionType.COLLECTION, 0), i2) );
		tmp.add( new OGRelation(i4, OGTestObject.class, "collection", ObjectGraphRelationType.ITEM_IN_COLLECTION, path(ObjectGraphCollectionType.COLLECTION, 1), i1) );
		tmp.add( new OGRelation(i4, OGTestObject.class, "collection", ObjectGraphRelationType.ITEM_IN_COLLECTION, path(ObjectGraphCollectionType.COLLECTION, 2), root) );
		tmp.add( new OGRelation(i4, OGTestObject.class, "collection", ObjectGraphRelationType.ITEM_IN_COLLECTION, path(ObjectGraphCollectionType.COLLECTION, 3), null) );
		
		i3.setArray(new @Nullable OGTestObject[] {i1, i2, null, i3, i4});
		
		tmp.add( new OGRelation(i3, OGTestObject.class, "array", ObjectGraphRelationType.ITEM_IN_COLLECTION, path(ObjectGraphCollectionType.ARRAY, 0), i1) );
		tmp.add( new OGRelation(i3, OGTestObject.class, "array", ObjectGraphRelationType.ITEM_IN_COLLECTION, path(ObjectGraphCollectionType.ARRAY, 1), i2) );
		tmp.add( new OGRelation(i3, OGTestObject.class, "array", ObjectGraphRelationType.ITEM_IN_COLLECTION, path(ObjectGraphCollectionType.ARRAY, 2), null) );
		tmp.add( new OGRelation(i3, OGTestObject.class, "array", ObjectGraphRelationType.ITEM_IN_COLLECTION, path(ObjectGraphCollectionType.ARRAY, 3), i3) );
		tmp.add( new OGRelation(i3, OGTestObject.class, "array", ObjectGraphRelationType.ITEM_IN_COLLECTION, path(ObjectGraphCollectionType.ARRAY, 4), i4) );
		
		i3.setParentArray(new @Nullable OGTestObject[] {null, i1});
		i4.setParentArray(i3.getParentArray()); // Share an array between two instances.
		
		tmp.add( new OGRelation(i3, OGTOParent.class, "array", ObjectGraphRelationType.ITEM_IN_COLLECTION, path(ObjectGraphCollectionType.ARRAY, 0), null) );
		tmp.add( new OGRelation(i3, OGTOParent.class, "array", ObjectGraphRelationType.ITEM_IN_COLLECTION, path(ObjectGraphCollectionType.ARRAY, 1), i1) );
		tmp.add( new OGRelation(i4, OGTOParent.class, "array", ObjectGraphRelationType.ITEM_IN_COLLECTION, path(ObjectGraphCollectionType.ARRAY, 0), null) );
		tmp.add( new OGRelation(i4, OGTOParent.class, "array", ObjectGraphRelationType.ITEM_IN_COLLECTION, path(ObjectGraphCollectionType.ARRAY, 1), i1) );
		
		i3.transientName = "child name";
		((OGTOParent)i3).transientName = "parent name";
		i3.sampleEnum = OGTestEnum.VAL2;
		i3.complexKeyMap = new HashMap<OGTestObject, OGTestObject>();
		i3.complexKeyMap.put(i5, i3);
		i3.complexKeyMap.put(null, i6);
		i3.complexKeyMap.put(i6, i6);
		i3.complexKeyMap.put(i2, null);
		
		tmp.add( new OGRelation(i3, OGTestObject.class, "complexKeyMap", ObjectGraphRelationType.MAP_KEY, null, i5) );
		tmp.add( new OGRelation(i3, OGTestObject.class, "complexKeyMap", ObjectGraphRelationType.MAP_KEY, null, null) );
		tmp.add( new OGRelation(i3, OGTestObject.class, "complexKeyMap", ObjectGraphRelationType.MAP_KEY, null, i6) );
		tmp.add( new OGRelation(i3, OGTestObject.class, "complexKeyMap", ObjectGraphRelationType.MAP_KEY, null, i2) );
		tmp.add( new OGRelation(i3, OGTestObject.class, "complexKeyMap", ObjectGraphRelationType.ITEM_IN_COLLECTION, path(ObjectGraphCollectionType.MAP, i5), i3) );
		tmp.add( new OGRelation(i3, OGTestObject.class, "complexKeyMap", ObjectGraphRelationType.ITEM_IN_COLLECTION, path(ObjectGraphCollectionType.MAP, null), i6) );
		tmp.add( new OGRelation(i3, OGTestObject.class, "complexKeyMap", ObjectGraphRelationType.ITEM_IN_COLLECTION, path(ObjectGraphCollectionType.MAP, i6), i6) );
		tmp.add( new OGRelation(i3, OGTestObject.class, "complexKeyMap", ObjectGraphRelationType.ITEM_IN_COLLECTION, path(ObjectGraphCollectionType.MAP, i2), null) );
		
		registerDirectFields(tmp, i3);
		registerDirectFields(tmp, i4);
		registerDirectFields(tmp, i5);
		registerDirectFields(tmp, i6);
		
		
		i1.nestedMap = new HashMap<String, List<Collection<Map<String,OGTestObject>>>>();
		List<Collection<Map<@Nullable String,@Nullable OGTestObject>>> nestedList0 = new ArrayList<>();
		List<Collection<Map<String,OGTestObject>>> nestedList1 = new ArrayList<Collection<Map<String,OGTestObject>>>();
		i1.nestedMap.put("a", nestedList0);
		i1.nestedMap.put("b", nestedList1);
		Collection<Map<@Nullable String, @Nullable OGTestObject>> nestedCollection0 = new HashSet<>();
		Collection<Map<@Nullable String, @Nullable OGTestObject>> nestedCollection1 = new ArrayDeque<>(); // Use dequeue to preserve ordering for test
		Collection<Map<@Nullable String, @Nullable OGTestObject>> nestedCollection2 = new HashSet<>(); 
		nestedList0.add(nestedCollection0);
		nestedList0.add(nestedCollection1);
		nestedList0.add(nestedCollection2);
		HashMap<@Nullable String, @Nullable OGTestObject> doubleNestedMap0 = new HashMap<>();
		HashMap<@Nullable String, @Nullable OGTestObject> doubleNestedMap1 = new HashMap<>();
		HashMap<@Nullable String, @Nullable OGTestObject> doubleNestedMap2 = new HashMap<>();
		HashMap<@Nullable String, @Nullable OGTestObject> doubleNestedMap3 = new HashMap<>();
		nestedCollection1.add(doubleNestedMap0);
		nestedCollection1.add(doubleNestedMap1);
		nestedCollection1.add(doubleNestedMap2);
		nestedCollection1.add(doubleNestedMap3);
		doubleNestedMap2.put("x", null);
		doubleNestedMap2.put("y", root);
		doubleNestedMap2.put("z", i4);
		doubleNestedMap2.put(null, i7);
		
		registerDirectFields(tmp, i1);
		registerDirectFields(tmp, i7);
		tmp.add( new OGRelation(i1, OGTestObject.class, "nestedMap", ObjectGraphRelationType.MAP_KEY, null, "a") );
		tmp.add( new OGRelation(i1, OGTestObject.class, "nestedMap", ObjectGraphRelationType.MAP_KEY, null, "b") );
		
		tmp.add( new OGRelation(i1, OGTestObject.class, "nestedMap", ObjectGraphRelationType.COLLECTION_INSTANCE, path(ObjectGraphCollectionType.MAP, "a"), nestedList0) );
		tmp.add( new OGRelation(i1, OGTestObject.class, "nestedMap", ObjectGraphRelationType.COLLECTION_INSTANCE, path(ObjectGraphCollectionType.MAP, "b"), nestedList1) );
		
		tmp.add( new OGRelation(i1, OGTestObject.class, "nestedMap", ObjectGraphRelationType.COLLECTION_INSTANCE, path(ObjectGraphCollectionType.MAP, "a", ObjectGraphCollectionType.LIST, 0), nestedCollection0) );
		tmp.add( new OGRelation(i1, OGTestObject.class, "nestedMap", ObjectGraphRelationType.COLLECTION_INSTANCE, path(ObjectGraphCollectionType.MAP, "a", ObjectGraphCollectionType.LIST, 1), nestedCollection1) );
		tmp.add( new OGRelation(i1, OGTestObject.class, "nestedMap", ObjectGraphRelationType.COLLECTION_INSTANCE, path(ObjectGraphCollectionType.MAP, "a", ObjectGraphCollectionType.LIST, 2), nestedCollection2) );
		
		tmp.add( new OGRelation(i1, OGTestObject.class, "nestedMap", ObjectGraphRelationType.COLLECTION_INSTANCE, path(ObjectGraphCollectionType.MAP, "a", ObjectGraphCollectionType.LIST, 1, ObjectGraphCollectionType.COLLECTION, 0), doubleNestedMap0) );
		tmp.add( new OGRelation(i1, OGTestObject.class, "nestedMap", ObjectGraphRelationType.COLLECTION_INSTANCE, path(ObjectGraphCollectionType.MAP, "a", ObjectGraphCollectionType.LIST, 1, ObjectGraphCollectionType.COLLECTION, 1), doubleNestedMap1) );
		tmp.add( new OGRelation(i1, OGTestObject.class, "nestedMap", ObjectGraphRelationType.COLLECTION_INSTANCE, path(ObjectGraphCollectionType.MAP, "a", ObjectGraphCollectionType.LIST, 1, ObjectGraphCollectionType.COLLECTION, 2), doubleNestedMap2) );
		tmp.add( new OGRelation(i1, OGTestObject.class, "nestedMap", ObjectGraphRelationType.COLLECTION_INSTANCE, path(ObjectGraphCollectionType.MAP, "a", ObjectGraphCollectionType.LIST, 1, ObjectGraphCollectionType.COLLECTION, 3), doubleNestedMap3) );
		
		tmp.add( new OGRelation(i1, OGTestObject.class, "nestedMap", ObjectGraphRelationType.MAP_KEY, path(ObjectGraphCollectionType.MAP, "a", ObjectGraphCollectionType.LIST, 1, ObjectGraphCollectionType.COLLECTION, 2), "x") );
		tmp.add( new OGRelation(i1, OGTestObject.class, "nestedMap", ObjectGraphRelationType.MAP_KEY, path(ObjectGraphCollectionType.MAP, "a", ObjectGraphCollectionType.LIST, 1, ObjectGraphCollectionType.COLLECTION, 2), "y") );
		tmp.add( new OGRelation(i1, OGTestObject.class, "nestedMap", ObjectGraphRelationType.MAP_KEY, path(ObjectGraphCollectionType.MAP, "a", ObjectGraphCollectionType.LIST, 1, ObjectGraphCollectionType.COLLECTION, 2), "z") );
		tmp.add( new OGRelation(i1, OGTestObject.class, "nestedMap", ObjectGraphRelationType.MAP_KEY, path(ObjectGraphCollectionType.MAP, "a", ObjectGraphCollectionType.LIST, 1, ObjectGraphCollectionType.COLLECTION, 2), null) );
		tmp.add( new OGRelation(i1, OGTestObject.class, "nestedMap", ObjectGraphRelationType.ITEM_IN_COLLECTION, path(ObjectGraphCollectionType.MAP, "a", ObjectGraphCollectionType.LIST, 1, ObjectGraphCollectionType.COLLECTION, 2, ObjectGraphCollectionType.MAP, "x"), null) );
		tmp.add( new OGRelation(i1, OGTestObject.class, "nestedMap", ObjectGraphRelationType.ITEM_IN_COLLECTION, path(ObjectGraphCollectionType.MAP, "a", ObjectGraphCollectionType.LIST, 1, ObjectGraphCollectionType.COLLECTION, 2, ObjectGraphCollectionType.MAP, "y"), root) );
		tmp.add( new OGRelation(i1, OGTestObject.class, "nestedMap", ObjectGraphRelationType.ITEM_IN_COLLECTION, path(ObjectGraphCollectionType.MAP, "a", ObjectGraphCollectionType.LIST, 1, ObjectGraphCollectionType.COLLECTION, 2, ObjectGraphCollectionType.MAP, "z"), i4) );
		tmp.add( new OGRelation(i1, OGTestObject.class, "nestedMap", ObjectGraphRelationType.ITEM_IN_COLLECTION, path(ObjectGraphCollectionType.MAP, "a", ObjectGraphCollectionType.LIST, 1, ObjectGraphCollectionType.COLLECTION, 2, ObjectGraphCollectionType.MAP, null), i7) );
		
		final List<OGRelation> completeRelationList = nn(Collections.unmodifiableList(tmp));
		
		// Base all-inclusive test
		{
			OGDataCollector collector = new OGDataCollector();
			ObjectGraphUtil.visitGraphExcludingRoot(root, createAllInclusiveOptions(), collector);
			compareData(completeRelationList, collector.data);
		}
		
		// Test declaring something as primitive via prefix
		{
			ObjectGraphConfig cfg = createAllInclusiveOptions();
			cfg.setPrimitiveClassPrefixes(new HashSet<String>(cfg.getPrimitiveClassPrefixes()));
			cfg.getPrimitiveClassPrefixes().add(nn(OGTestEnum.class.getCanonicalName()));
			
			OGDataCollector collector = new OGDataCollector();
			ObjectGraphUtil.visitGraphExcludingRoot(root, cfg, collector);
			compareData( 
				removeEnumDetails(completeRelationList)
				, collector.data);
			
			// Test declaring something as compound works and overrides primitive prefixes
			{
				cfg.setCompoundClassPrefixes(new HashSet<String>(cfg.getCompoundClassPrefixes()));
				cfg.getCompoundClassPrefixes().add(nn(OGTestEnum.class.getCanonicalName()));
				
				collector = new OGDataCollector();
				ObjectGraphUtil.visitGraphExcludingRoot(root, cfg, collector);
				compareData( 
					completeRelationList
					, collector.data);
			}
			
			// Test declaring something as primitive class works and overrides compound prefixes
			{
				cfg.setPrimitiveTypes(new HashSet<Class<?>>(cfg.getPrimitiveTypes()));
				cfg.getPrimitiveTypes().add(OGTestEnum.class);
				
				collector = new OGDataCollector();
				ObjectGraphUtil.visitGraphExcludingRoot(root, cfg, collector);
				compareData( 
					removeEnumDetails(completeRelationList)
					, collector.data);
			}
		}
		
		// Test that enum handle mode works.
		{
			ObjectGraphConfig cfg = createAllInclusiveOptions();
			cfg.setEnumHandleMode(ObjectGraphHandleMode.PRIMITIVE);
			
			OGDataCollector collector = new OGDataCollector();
			ObjectGraphUtil.visitGraphExcludingRoot(root, cfg, collector);
			compareData( 
				removeEnumDetails(completeRelationList)
				, collector.data);
		}
		
		// Test that enum handle mode setting to null works.
		{
			ObjectGraphConfig cfg = createAllInclusiveOptions();
			cfg.setEnumHandleMode(null);
			
			OGDataCollector collector = new OGDataCollector();
			ObjectGraphUtil.visitGraphExcludingRoot(root, cfg, collector);
			compareData( 
				completeRelationList
				, collector.data);
			
			cfg.setUserClassHandleMode(ObjectGraphHandleMode.PRIMITIVE);
			cfg.setCompoundClassPrefixes(new HashSet<String>(cfg.getCompoundClassPrefixes()));
			cfg.getCompoundClassPrefixes().add(nn(OGTestObject.class.getCanonicalName())); // This makes sure we don't treat base test class as primitive.
			cfg.getCompoundClassPrefixes().add(nn(OGTestObject2.class.getPackage().getName())); // This makes sure we don't treat base test classes as primitive.
			
			collector = new OGDataCollector();
			ObjectGraphUtil.visitGraphExcludingRoot(root, cfg, collector);
			compareData( 
				removeEnumDetails(completeRelationList)
				, collector.data);
		}
		
		// Test that system classes handle mode / prefixes work -- to this end re-declare 2nd test class as system.
		{
			ObjectGraphConfig cfg = createAllInclusiveOptions();
			cfg.setSystemClassPrefixes(new HashSet<String>(cfg.getSystemClassPrefixes()));
			cfg.getSystemClassPrefixes().add(nn(OGTestObject2.class.getPackage().getName()));
			
			// Test unknown system class handling set to 'exception' (default)
			OGDataCollector collector = new OGDataCollector();
			try
			{
				ObjectGraphUtil.visitGraphExcludingRoot(root, cfg, collector);
				compareData( 
					completeRelationList
					, collector.data);
				assert false;
			} catch (ObjectGraphUnhandledTypeException e)
			{
				// This exception is expected.
				assert e.toString().contains("Instance of class [io.github.solf.extra2.objectgraph.OGTestObject], field [object2] defined in [io.github.solf.extra2.objectgraph.OGTestObject], relation type [FIELD] has child of type [io.github.solf.extra2.objectgraph2.OGTestObject2] that parses as unknown 'SYSTEM' class and these are not handled as per configuration. Relation path is: null, parent object is: [OGTestObject[2]], child object is [OGTestObject2:object2]") : e.toString();
			}
			
			// Test unknown system class handling set to 'compound'
			cfg.setSystemClassHandleMode(ObjectGraphHandleMode.COMPOUND);
			collector = new OGDataCollector();
			ObjectGraphUtil.visitGraphExcludingRoot(root, cfg, collector);
			compareData( 
				completeRelationList
				, collector.data);
			
			// Test unknown system class handling set to 'primitive'
			cfg.setSystemClassHandleMode(ObjectGraphHandleMode.PRIMITIVE);
			collector = new OGDataCollector();
			ObjectGraphUtil.visitGraphExcludingRoot(root, cfg, collector);
			compareData( 
				removeObject2Details(completeRelationList)
				, collector.data);
		}
		
		// Test that user classes handle mode / prefixes / default to user/system work.
		{
			ObjectGraphConfig cfg = createAllInclusiveOptions();
			cfg.setCompoundClassPrefixes(new HashSet<String>(cfg.getCompoundClassPrefixes()));
			cfg.getCompoundClassPrefixes().add(OGTestObject.class.getPackage().getName() + "."); // primary test package is 'compound'
			// Secondary test class should now be user, test handling.
			cfg.setUnknownClassesDefaultToUser(false); // Default unknown classes to 'system' in order to test user classes prefixes 
			
			// Test that stuff fails because of unknown 'system' class Object2 
			OGDataCollector collector = new OGDataCollector();
			try
			{
				ObjectGraphUtil.visitGraphExcludingRoot(root, cfg, collector);
				compareData( 
					completeRelationList
					, collector.data);
				assert false;
			} catch (ObjectGraphUnhandledTypeException e)
			{
				// This exception is expected.
				assert e.toString().contains("Instance of class [io.github.solf.extra2.objectgraph.OGTestObject], field [object2] defined in [io.github.solf.extra2.objectgraph.OGTestObject], relation type [FIELD] has child of type [io.github.solf.extra2.objectgraph2.OGTestObject2] that parses as unknown 'SYSTEM' class and these are not handled as per configuration. Relation path is: null, parent object is: [OGTestObject[2]], child object is [OGTestObject2:object2]") : e.toString();
			}
			
			// Set user class prefixes and test it with default 'compound' option
			cfg.setUserClassPrefixes(new HashSet<String>(cfg.getUserClassPrefixes()));
			cfg.getUserClassPrefixes().add(OGTestObject2.class.getPackage().getName());
			// Test that 'compound' option works.
			collector = new OGDataCollector();
			ObjectGraphUtil.visitGraphExcludingRoot(root, cfg, collector);
			compareData( 
				completeRelationList
				, collector.data);
			
			// Test that 'primitive' option works.
			cfg.setUserClassHandleMode(ObjectGraphHandleMode.PRIMITIVE);
			collector = new OGDataCollector();
			ObjectGraphUtil.visitGraphExcludingRoot(root, cfg, collector);
			compareData( 
				removeObject2Details(completeRelationList)
				, collector.data);
			
			// Test that 'exception' option works.
			cfg.setUserClassHandleMode(ObjectGraphHandleMode.EXCEPTION);
			collector = new OGDataCollector();
			try
			{
				ObjectGraphUtil.visitGraphExcludingRoot(root, cfg, collector);
				compareData( 
					completeRelationList
					, collector.data);
				assert false;
			} catch (ObjectGraphUnhandledTypeException e)
			{
				// This exception is expected.
				assert e.toString().contains("Instance of class [io.github.solf.extra2.objectgraph.OGTestObject], field [object2] defined in [io.github.solf.extra2.objectgraph.OGTestObject], relation type [FIELD] has child of type [io.github.solf.extra2.objectgraph2.OGTestObject2] that parses as unknown 'USER' class and these are not handled as per configuration. Relation path is: null, parent object is: [OGTestObject[2]], child object is [OGTestObject2:object2]") : e.toString();
			}
		}
		
		// Test 'handle collections' option.
		{
			ObjectGraphConfig cfg = createAllInclusiveOptions();
			cfg.setHandleCollections(false);
			
			OGDataCollector collector = new OGDataCollector();
			try
			{
				ObjectGraphUtil.visitGraphExcludingRoot(root, cfg, collector);
				compareData( 
					completeRelationList
					, collector.data);
				assert false;
			} catch (ObjectGraphUnhandledTypeException e)
			{
				// This exception is expected.
				assert e.toString().contains("Instance of class [io.github.solf.extra2.objectgraph.OGTestObject], field [map] defined in [io.github.solf.extra2.objectgraph.OGTestObject], relation type [FIELD] has child of type [java.util.HashMap] that parses as unknown 'SYSTEM' class and these are not handled as per configuration. Relation path is: null, parent object is: [OGTestObject[0]], child object is [{") : e.toString();
				assert e.toString().contains("1=OGTestObject[1]") : e.toString();
				assert e.toString().contains("2=OGTestObject[2]") : e.toString();
			}
		}
		
		// Test 'include collection holders' option
		{
			ObjectGraphConfig cfg = createAllInclusiveOptions();
			cfg.setIncludeCollectionHolders(false);
			
			OGDataCollector collector = new OGDataCollector();
			ObjectGraphUtil.visitGraphExcludingRoot(root, cfg, collector);
			compareData( 
				removeCollectionInstances(completeRelationList)
				, collector.data);
		}
		
		// Test 'include map keys' option
		{
			ObjectGraphConfig cfg = createAllInclusiveOptions();
			cfg.setIncludeMapKeys(false);
			
			OGDataCollector collector = new OGDataCollector();
			ObjectGraphUtil.visitGraphExcludingRoot(root, cfg, collector);
			compareData( 
				removeObjectsDetails( removeMapKeys(completeRelationList), i5) 
				, collector.data);
		}
		
		// Test 'include nulls' option
		{
			ObjectGraphConfig cfg = createAllInclusiveOptions();
			cfg.setIncludeNullFields(false);
			
			OGDataCollector collector = new OGDataCollector();
			ObjectGraphUtil.visitGraphExcludingRoot(root, cfg, collector);
			compareData( 
				removeNullFields(completeRelationList) 
				, collector.data);
		}
		
		// Test 'include primitive fields' option
		{
			ObjectGraphConfig cfg = createAllInclusiveOptions();
			cfg.setIncludePrimitiveTypes(false);
			
			OGDataCollector collector = new OGDataCollector();
			ObjectGraphUtil.visitGraphExcludingRoot(root, cfg, collector);
			compareData( 
				removePrimitiveFields(completeRelationList) 
				, collector.data);
		}
		
		// Test 'include transient fields' option
		{
			ObjectGraphConfig cfg = createAllInclusiveOptions();
			cfg.setIncludeTransientFields(false);
			
			OGDataCollector collector = new OGDataCollector();
			ObjectGraphUtil.visitGraphExcludingRoot(root, cfg, collector);
			compareData( 
				removeFields(completeRelationList, "transientName") 
				, collector.data);
		}
		
		// Test 'visit for each parent' option
		{
			ObjectGraphConfig cfg = createAllInclusiveOptions();
			cfg.setVisitForEachParent(false);
			
			OGDataCollector collector = new OGDataCollector();
			ObjectGraphUtil.visitGraphExcludingRoot(root, cfg, collector);
			
			List<OGRelation> expected = new ArrayList<OGRelation>(completeRelationList);
			// Under this config root is never visited, so remove it.
			expected = removeInstanceVisits(expected, root, ObjectGraphRelationType.FIELD);
			expected = removeInstanceVisits(expected, root, ObjectGraphRelationType.ITEM_IN_COLLECTION);
			for (OGRelation item : collector.data)
			{
				assert expected.remove(item) : item;
				Object obj = item.visitee;
				if (obj != null)
				{
					if (!ObjectGraphUtil.isPrimitiveType(cfg, obj)) // Only for non-primitive types.
					{
						// Keep COLLECTION_INSTANCE below because those are not de-duped 
						expected = removeInstanceVisits(expected, obj, ObjectGraphRelationType.FIELD);
						expected = removeInstanceVisits(expected, obj, ObjectGraphRelationType.ITEM_IN_COLLECTION);
						expected = removeInstanceVisits(expected, obj, ObjectGraphRelationType.MAP_KEY);
					}
				}
			}
			
			assert expected.size() == 0 : "Expected has EXTRA items: " + expected;
		}
		
		// Test simply visiting compound nodes.
		{
			ObjectGraphConfig cfg = createAllInclusiveOptions();
			
			final IdentityHashSet<Object> expected = new IdentityHashSet<Object>();
			expected.add(root);
			expected.add(i1);
			expected.add(i2);
			expected.add(i3);
			expected.add(i4);
			expected.add(i5);
			expected.add(i6);
			expected.add(i7);
			expected.add(OGTestEnum.VAL1);
			expected.add(OGTestEnum.VAL2);
			expected.add(object2);
			
			ObjectGraphUtil.visitCompoundNodesIncludingRoot(root, cfg,
				new ObjectGraphCompoundNodeVisitor()
				{
					@Override
					public void visit(Object compoundNode)
					{
						assert expected.remove(compoundNode) : compoundNode;
					}
				}
			);
			
			assert expected.size() == 0 : expected;
		}
		
		// Test visiting compound nodes when root object is collection. 
		{
			ObjectGraphConfig cfg = createAllInclusiveOptions();
			
			OGTestObject otherItem = new OGTestObject(1001);
			ArrayList<@Nullable OGTestObject> rootList = new ArrayList<>();
			rootList.add(null);
			rootList.add(root);
			rootList.add(otherItem);
			
			final IdentityHashSet<Object> expected = new IdentityHashSet<Object>();
			expected.add(root);
			expected.add(i1);
			expected.add(i2);
			expected.add(i3);
			expected.add(i4);
			expected.add(i5);
			expected.add(i6);
			expected.add(i7);
			expected.add(OGTestEnum.VAL1);
			expected.add(OGTestEnum.VAL2);
			expected.add(object2);
			expected.add(otherItem);
			
			ObjectGraphUtil.visitCompoundNodesIncludingRoot(rootList, cfg,
				new ObjectGraphCompoundNodeVisitor()
				{
					@Override
					public void visit(Object compoundNode)
					{
						assert expected.remove(compoundNode) : compoundNode;
					}
				}
			);
			
			assert expected.size() == 0 : expected;
		}
		
		{
			// Skip test for collection and field
			root.skipCollection = new ArrayList<>();
			final OGTestSkip skip = new OGTestSkip();
			root.skipCollection.add(skip);
			final OGTestSkip2 skip2 = new OGTestSkip2();
			skip.skip2 = skip2;
			
			ArrayList<OGRelation> expectedRelations = new ArrayList<>(completeRelationList);
			assertTrue( expectedRelations.remove(new OGRelation(root, OGTestObject.class, "skipCollection", ObjectGraphRelationType.FIELD, null, null)) );

			expectedRelations.add( new OGRelation(root, OGTestObject.class, "skipCollection", ObjectGraphRelationType.COLLECTION_INSTANCE, null, root.skipCollection) );
			
			{
				// test skipping OGTestSkip (in collection)
				ObjectGraphConfig config = createAllInclusiveOptions().setCustomHandlingResolver(new ObjectGraphHandlingResolver()
				{
					@Override
					public @Nullable ObjectGraphVisiteeHandleMode determineHandling(ObjectGraphConfig cfg,
						@Nullable Object parent,
						@Nullable Class<?> fieldContainer,
						boolean isKnownToBePrimitive, @Nullable String fieldName,
						@Nullable ObjectGraphRelationType relationType,
						ObjectGraphCollectionStep @Nullable [] path, Object visitee)
						throws ObjectGraphUnhandledTypeException
					{
						if (visitee instanceof OGTestSkip)
							return ObjectGraphVisiteeHandleMode.SKIP;
						
						return null;
					}
				});
				OGDataCollector collector = new OGDataCollector();
				ObjectGraphUtil.visitGraphExcludingRoot(root, config, collector);
				compareData(expectedRelations, collector.data);
			}

			expectedRelations.add( new OGRelation(root, OGTestObject.class, "skipCollection", ObjectGraphRelationType.ITEM_IN_COLLECTION, path(ObjectGraphCollectionType.LIST, 0), skip) );
			
			{
				// test skipping OGTestSkip2 (as a field)
				ObjectGraphConfig config = createAllInclusiveOptions().setCustomHandlingResolver(new ObjectGraphHandlingResolver()
				{
					@Override
					public @Nullable ObjectGraphVisiteeHandleMode determineHandling(ObjectGraphConfig cfg,
						@Nullable Object parent,
						@Nullable Class<?> fieldContainer,
						boolean isKnownToBePrimitive, @Nullable String fieldName,
						@Nullable ObjectGraphRelationType relationType,
						ObjectGraphCollectionStep @Nullable [] path, Object visitee)
						throws ObjectGraphUnhandledTypeException
					{
						if (visitee instanceof OGTestSkip2)
							return ObjectGraphVisiteeHandleMode.SKIP;
						
						return null;
					}
				});
				OGDataCollector collector = new OGDataCollector();
				ObjectGraphUtil.visitGraphExcludingRoot(root, config, collector);
				compareData(expectedRelations, collector.data);
			}
			
			expectedRelations.add( new OGRelation(skip, OGTestSkip.class, "skip2", ObjectGraphRelationType.FIELD, null, skip2) );
			
			{
				// test no-skipping too
				ObjectGraphConfig config = createAllInclusiveOptions();
				OGDataCollector collector = new OGDataCollector();
				ObjectGraphUtil.visitGraphExcludingRoot(root, config, collector);
				compareData(expectedRelations, collector.data);
			}
			
			// clean up
			root.skipCollection = null;
		}
		
		
		{
			// Skip test for map
			final Map<OGTestSkip, OGTestSkip2> skipMap = new HashMap<>();
			root.setSkipMap(skipMap);
			final OGTestSkip key = new OGTestSkip();
			final OGTestSkip2 value = new OGTestSkip2();
			skipMap.put(key, value);
			
			
			ArrayList<OGRelation> expectedRelations = new ArrayList<>(completeRelationList);
			assertTrue( expectedRelations.remove(new OGRelation(root, OGTestObject.class, "skipMap", ObjectGraphRelationType.FIELD, null, null)) );

			expectedRelations.add( new OGRelation(root, OGTestObject.class, "skipMap", ObjectGraphRelationType.COLLECTION_INSTANCE, null, skipMap) );
			expectedRelations.add( new OGRelation(root, OGTestObject.class, "skipMap", ObjectGraphRelationType.ITEM_IN_COLLECTION, path(ObjectGraphCollectionType.MAP, key), value) );
			
			{
				// test skipping OGTestSkip (in collection)
				ObjectGraphConfig config = createAllInclusiveOptions().setCustomHandlingResolver(new ObjectGraphHandlingResolver()
				{
					@Override
					public @Nullable ObjectGraphVisiteeHandleMode determineHandling(ObjectGraphConfig cfg,
						@Nullable Object parent,
						@Nullable Class<?> fieldContainer,
						boolean isKnownToBePrimitive, @Nullable String fieldName,
						@Nullable ObjectGraphRelationType relationType,
						ObjectGraphCollectionStep @Nullable [] path, Object visitee)
						throws ObjectGraphUnhandledTypeException
					{
						if (visitee instanceof OGTestSkip)
							return ObjectGraphVisiteeHandleMode.SKIP;
						
						return null;
					}
				});
				OGDataCollector collector = new OGDataCollector();
				ObjectGraphUtil.visitGraphExcludingRoot(root, config, collector);
				compareData(expectedRelations, collector.data);
			}

			expectedRelations.add( new OGRelation(key, OGTestSkip.class, "skip2", ObjectGraphRelationType.FIELD, null, null));
			expectedRelations.add( new OGRelation(root, OGTestObject.class, "skipMap", ObjectGraphRelationType.MAP_KEY, null, key) );
			
			assertTrue( expectedRelations.remove( new OGRelation(root, OGTestObject.class, "skipMap", ObjectGraphRelationType.ITEM_IN_COLLECTION, path(ObjectGraphCollectionType.MAP, key), value) ) );
			
			{
				// test skipping OGTestSkip2 (as a field)
				ObjectGraphConfig config = createAllInclusiveOptions().setCustomHandlingResolver(new ObjectGraphHandlingResolver()
				{
					@Override
					public @Nullable ObjectGraphVisiteeHandleMode determineHandling(ObjectGraphConfig cfg,
						@Nullable Object parent,
						@Nullable Class<?> fieldContainer,
						boolean isKnownToBePrimitive, @Nullable String fieldName,
						@Nullable ObjectGraphRelationType relationType,
						ObjectGraphCollectionStep @Nullable [] path, Object visitee)
						throws ObjectGraphUnhandledTypeException
					{
						if (visitee instanceof OGTestSkip2)
							return ObjectGraphVisiteeHandleMode.SKIP;
						
						return null;
					}
				});
				OGDataCollector collector = new OGDataCollector();
				ObjectGraphUtil.visitGraphExcludingRoot(root, config, collector);
				compareData(expectedRelations, collector.data);
			}
			
			// clean up
			root.setSkipMap(null);
		}
		
		// WARNING!! DESTRUCTIVE TEST -- MUST BE LAST.
		{
			OGDataCollector collector = new OGDataCollectorWithNulling(); // Nulls every non-primitive field as it is processed.
			ObjectGraphUtil.visitGraphExcludingRoot(root, createAllInclusiveOptions(), collector);
			compareData(completeRelationList, collector.data);
		}
	}
	
	/**
	 * Create {@link ObjectGraphUtil} configuration that is maximally inclusive
	 * or everything.
	 */
	private ObjectGraphConfig createAllInclusiveOptions()
	{
		return new ObjectGraphConfig()
			.setEnumHandleMode(ObjectGraphHandleMode.COMPOUND)
			.setIncludeCollectionHolders(true)
			.setIncludeMapKeys(true)
			.setIncludeNullFields(true)
			.setIncludePrimitiveTypes(true)
			;
	}
	
//	@Test
//	public void test2()
//	{
//		io.github.solf.extra2.objectgraph.other.Child child = new io.github.solf.extra2.objectgraph.other.Child();
//		System.out.println(child.getCPub());
//		System.out.println(child.getCPro());
//		System.out.println(child.getCPac());
//		System.out.println(child.getCPri());
//		
//		System.out.println();
//		
//		System.out.println(((Parent)child).pub);
//		System.out.println(((Parent)child).pro);
//		System.out.println(((Parent)child).pac);
//		System.out.println(((Parent)child).getPPri());
//		
//		System.out.println();
//		
//		System.out.println(child.getPPub());
//		System.out.println(child.getPPro());
//		System.out.println(child.getPPac());
//		System.out.println(child.getPPri());
//		System.out.println(child.getCPac());
//		System.out.println(child.getCPri());
//	}
	
	/**
	 * Registers values for all direct object fields 
	 */
	private void registerDirectFields(List<OGRelation> rels, OGTestObject obj)
	{
		rels.add(new OGRelation(obj, OGTestObject.class, "id", ObjectGraphRelationType.FIELD, null, obj.id));
		rels.add(new OGRelation(obj, OGTestObject.class, "object", ObjectGraphRelationType.FIELD, null, obj.object));
		rels.add(new OGRelation(obj, OGTestObject.class, "object2", ObjectGraphRelationType.FIELD, null, obj.object2));
		rels.add(new OGRelation(obj, OGTestObject.class, "transientName", ObjectGraphRelationType.FIELD, null, obj.transientName));
		if (obj.map == null)
			rels.add(new OGRelation(obj, OGTestObject.class, "map", ObjectGraphRelationType.FIELD, null, null));
		else
			rels.add(new OGRelation(obj, OGTestObject.class, "map", ObjectGraphRelationType.COLLECTION_INSTANCE, null, obj.map));
		if (obj.list == null)
			rels.add(new OGRelation(obj, OGTestObject.class, "list", ObjectGraphRelationType.FIELD, null, null));
		else
			rels.add(new OGRelation(obj, OGTestObject.class, "list", ObjectGraphRelationType.COLLECTION_INSTANCE, null, obj.list));
		if (obj.collection == null)
			rels.add(new OGRelation(obj, OGTestObject.class, "collection", ObjectGraphRelationType.FIELD, null, null));
		else
			rels.add(new OGRelation(obj, OGTestObject.class, "collection", ObjectGraphRelationType.COLLECTION_INSTANCE, null, obj.collection));
		if (obj.skipCollection == null)
			rels.add(new OGRelation(obj, OGTestObject.class, "skipCollection", ObjectGraphRelationType.FIELD, null, null));
		else
			rels.add(new OGRelation(obj, OGTestObject.class, "skipCollection", ObjectGraphRelationType.COLLECTION_INSTANCE, null, obj.skipCollection));
		if (obj.getSkipMap() == null)
			rels.add(new OGRelation(obj, OGTestObject.class, "skipMap", ObjectGraphRelationType.FIELD, null, null));
		else
			rels.add(new OGRelation(obj, OGTestObject.class, "skipMap", ObjectGraphRelationType.COLLECTION_INSTANCE, null, obj.getSkipMap()));
		if (obj.getArray() == null)
			rels.add(new OGRelation(obj, OGTestObject.class, "array", ObjectGraphRelationType.FIELD, null, null));
		else
			rels.add(new OGRelation(obj, OGTestObject.class, "array", ObjectGraphRelationType.COLLECTION_INSTANCE, null, obj.getArray()));
		if (obj.nestedMap == null)
			rels.add(new OGRelation(obj, OGTestObject.class, "nestedMap", ObjectGraphRelationType.FIELD, null, null));
		else
			rels.add(new OGRelation(obj, OGTestObject.class, "nestedMap", ObjectGraphRelationType.COLLECTION_INSTANCE, null, obj.nestedMap));
		rels.add(new OGRelation(obj, OGTestObject.class, "sampleEnum", ObjectGraphRelationType.FIELD, null, obj.sampleEnum));
		if (obj.complexKeyMap == null)
			rels.add(new OGRelation(obj, OGTestObject.class, "complexKeyMap", ObjectGraphRelationType.FIELD, null, null));
		else
			rels.add(new OGRelation(obj, OGTestObject.class, "complexKeyMap", ObjectGraphRelationType.COLLECTION_INSTANCE, null, obj.complexKeyMap));
		rels.add(new OGRelation(obj, OGTOParent.class, "transientName", ObjectGraphRelationType.FIELD, null, ((OGTOParent)obj).transientName));
		if (obj.getParentArray() == null)
			rels.add(new OGRelation(obj, OGTOParent.class, "array", ObjectGraphRelationType.FIELD, null, null));
		else
			rels.add(new OGRelation(obj, OGTOParent.class, "array", ObjectGraphRelationType.COLLECTION_INSTANCE, null, obj.getParentArray()));
	}
	
	/**
	 * Builds relation path array out of pairs 'type', key, ...
	 */
	private ObjectGraphCollectionStep[] path(@Nullable Object... objs)
	{
		if (objs.length < 2)
			throw new IllegalArgumentException("Size must be 2+");
		if (objs.length % 2 != 0)
			throw new IllegalArgumentException("Size must be divisible by 2");
		
		ArrayList<ObjectGraphCollectionStep> result = new ArrayList<ObjectGraphCollectionStep>();
		ObjectGraphCollectionType type = null;
		for (Object obj : objs)
		{
			if (type == null)
				type = (ObjectGraphCollectionType)obj;
			else
			{
				result.add(new ObjectGraphCollectionStep(type, obj));
				type = null;
			}
		}
		
		return result.toArray(new @Nonnull ObjectGraphCollectionStep[0]);
	}
	
	/**
	 * Compares actual data with expected data and dumps & fails (assert) if
	 * they don't match.
	 */
	private void compareData(List<OGRelation> expectedData, List<@Nonnull OGRelation> actualData)
	{
		ArrayList<OGRelation> missing = new ArrayList<OGRelation>(expectedData);
		ArrayList<OGRelation> extra = new ArrayList<OGRelation>();
		
		for (OGRelation item : actualData)
		{
			if (!missing.remove(item))
				extra.add(item);
		}
		
		if (missing.size() != 0)
		{
			System.err.println("--------------------------------------------------");
			System.err.println("MISSING in ACTUAL data:");
			for (OGRelation item : missing)
			{
				System.err.println(item);
			}
			System.err.println("--------------------------------------------------");
		}
		
		if (extra.size() != 0)
		{
			System.err.println("--------------------------------------------------");
			System.err.println("EXTRA in ACTUAL data:");
			for (OGRelation item : extra)
			{
				System.err.println(item);
			}
			System.err.println("--------------------------------------------------");
		}
		
		assert (missing.size() == 0) && (extra.size() == 0) : "Expected doesn't match actual -- see above.";
	}
	
	/**
	 * Removes enum details from the relations.
	 */
	private List<OGRelation> removeEnumDetails(List<OGRelation> src)
	{
		ArrayList<OGRelation> result = new ArrayList<OGRelation>(src.size());
		for (OGRelation item : src)
		{
			if (item.parent instanceof Enum<?>)
				continue;
			
			result.add(item);
		}
		
		return result;
	}
	
	/**
	 * Removes Object2 details from the relations.
	 */
	private List<OGRelation> removeObject2Details(List<OGRelation> src)
	{
		ArrayList<OGRelation> result = new ArrayList<OGRelation>(src.size());
		for (OGRelation item : src)
		{
			if (item.parent instanceof OGTestObject2)
				continue;
			
			result.add(item);
		}
		
		return result;
	}
	
	/**
	 * Removes collection instances from the relations.
	 */
	private List<OGRelation> removeCollectionInstances(List<OGRelation> src)
	{
		ArrayList<OGRelation> result = new ArrayList<OGRelation>(src.size());
		for (OGRelation item : src)
		{
			if (item.relationType == ObjectGraphRelationType.COLLECTION_INSTANCE)
				continue;
			
			result.add(item);
		}
		
		return result;
	}
	
	/**
	 * Removes map keys from the relations.
	 */
	private List<OGRelation> removeMapKeys(List<OGRelation> src)
	{
		ArrayList<OGRelation> result = new ArrayList<OGRelation>(src.size());
		for (OGRelation item : src)
		{
			if (item.relationType == ObjectGraphRelationType.MAP_KEY)
				continue;
			
			result.add(item);
		}
		
		return result;
	}
	
	/**
	 * Remove details of specific objects from the relations.
	 */
	private List<OGRelation> removeObjectsDetails(List<OGRelation> src, Object... toRemove)
	{
		HashSet<Object> targets = new HashSet<Object>();
		for (Object obj : toRemove)
		{
			if (nullable(obj) == null)
				throw new NullPointerException("toRemove values must not contain null");
			targets.add(obj);
		}
		
		ArrayList<OGRelation> result = new ArrayList<OGRelation>(src.size());
		for (OGRelation item : src)
		{
			if (targets.contains(item.parent))
				continue;
			
			result.add(item);
		}
		
		return result;
	}
	
	/**
	 * Removes null fields from the relations.
	 */
	private List<OGRelation> removeNullFields(List<OGRelation> src)
	{
		ArrayList<OGRelation> result = new ArrayList<OGRelation>(src.size());
		for (OGRelation item : src)
		{
			if (item.visitee == null)
				continue;
			
			result.add(item);
		}
		
		return result;
	}
	
	/**
	 * Removes primitive fields from the relations.
	 */
	private List<OGRelation> removePrimitiveFields(List<OGRelation> src)
	{
		ObjectGraphConfig cfg = new ObjectGraphConfig();
		ArrayList<OGRelation> result = new ArrayList<OGRelation>(src.size());
		for (OGRelation item : src)
		{
			Object visitee = item.visitee;
			if (visitee != null)
				if (cfg.getPrimitiveTypes().contains(visitee.getClass()))
					continue;
			
			result.add(item);
		}
		
		return result;
	}
	
	/**
	 * Remove specific fields from the relations.
	 */
	private List<OGRelation> removeFields(List<OGRelation> src, String... toRemove)
	{
		HashSet<String> targets = new HashSet<String>();
		for (String obj : toRemove)
		{
			if (nullable(obj) == null)
				throw new NullPointerException("toRemove values must not contain null");
			targets.add(obj);
		}
		
		ArrayList<OGRelation> result = new ArrayList<OGRelation>(src.size());
		for (OGRelation item : src)
		{
			if (targets.contains(item.fieldName))
				continue;
			
			result.add(item);
		}
		
		return result;
	}
	
	/**
	 * Removes all instances of visiting to the given object with the given relation type.
	 */
	private List<OGRelation> removeInstanceVisits(List<OGRelation> src, Object instance, ObjectGraphRelationType relType)
	{
		ArrayList<OGRelation> result = new ArrayList<OGRelation>(src.size());
		for (OGRelation item : src)
		{
			if (relType == item.relationType)
			{
				if (instance == item.visitee)
					continue;
			}
			
			result.add(item);
		}
		
		return result;
	}
}
