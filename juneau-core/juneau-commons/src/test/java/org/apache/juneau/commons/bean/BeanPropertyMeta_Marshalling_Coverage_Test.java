/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.commons.bean;

import static org.apache.juneau.commons.reflect.ReflectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.bean.BeanTestFakes.*;
import org.apache.juneau.commons.reflect.*;
import org.junit.jupiter.api.*;

/**
 * Coverage tests for the marshalling-only-path branches of {@link BeanPropertyMeta#add}, {@link BeanPropertyMeta#set}
 * (and its private {@code setPropertyValue} helper), and {@code setArray} / {@code createDefaultCollectionForAbstractType}.
 *
 * <p>
 * These methods require a non-null {@link BeanPropertyMeta.Builder#rawMetaType(BeanInfo) rawTypeMeta} and (for
 * type-conversion) a {@link BeanMap#setBeanSession(BeanSession) BeanSession} — both normally supplied by the
 * marshalling layer.  This test class fakes both seams ({@link BeanTestFakes.FakeBeanInfo},
 * {@link BeanTestFakes.FakeBeanSession}) entirely within {@code juneau-commons}, driving
 * {@link BeanPropertyMeta.Builder} directly (package-private constructor/fields — same package as production code)
 * instead of going through the commons-only {@link BeanMeta#of(Class)} discovery path, which never populates
 * {@code rawTypeMeta}.
 */
@SuppressWarnings({
	"unused"  // Test POJO fields are read reflectively through BeanPropertyMeta, not directly.
})
class BeanPropertyMeta_Marshalling_Coverage_Test extends TestBase {

	//====================================================================================================
	// Test POJOs
	//====================================================================================================

	public static class ListBean {
		public List<String> items;
	}

	public static class ArrayBean {
		public String[] items;
	}

	public static class MapBean {
		public Map<String,Integer> props;
	}

	public static class NestedBean {
		public String name;
	}

	public static class BeanPropBean {
		public NestedBean nested;
	}

	public static class SortedSetFieldBean {
		public SortedSet<String> items;
	}

	public static class NavigableSetFieldBean {
		public NavigableSet<String> items;
	}

	public static class SetFieldBean {
		public Set<String> items;
	}

	public static class DequeFieldBean {
		public Deque<String> items;
	}

	public static class QueueFieldBean {
		public Queue<String> items;
	}

	public static class SortedMapFieldBean {
		public SortedMap<String,Integer> props;
	}

	/** No {@code @BeanCtor} - {@code getConstructorArgs()} is empty, so {@code BeanMap} never allocates a propertyCache. */
	public static class ReadOnlyNoCtorBean {
		public final String name;
		public ReadOnlyNoCtorBean() { this.name = null; }
	}

	/** {@code @BeanCtor}-bearing read-only bean - {@code BeanMap} allocates a propertyCache for accumulating properties. */
	public static class ReadOnlyListBean {
		public final List<String> items;
		@BeanCtor(properties = "items")
		public ReadOnlyListBean(List<String> items) { this.items = items; }
	}

	public static class ReadOnlyMapBean {
		public final Map<String,Integer> props;
		@BeanCtor(properties = "props")
		public ReadOnlyMapBean(Map<String,Integer> props) { this.props = props; }
	}

	/** Getter-only (no field) - used to drive the raw-invocation getter branch without a backing field. */
	public static class GetterBean {
		public String getX() { return "hello"; }
	}

	/** Getter throws, declared with a primitive-friendly shape for the ignore+primitive-default path. */
	public static class IntGetterThrowsBean {
		public int getX() { throw new RuntimeException("boom"); }
	}

	/** Dyna property backed by a getter that returns a live (mutable) Map - no setter, no field. */
	public static class DynaGetterOnlyMapBean {
		private final Map<String,Object> store = new LinkedHashMap<>();
		public Map<String,Object> getExtras() { return store; }
	}

	/** Dyna property backed by a getter that returns <jk>null</jk> - no setter, no field. */
	public static class DynaGetterOnlyNullMapBean {
		public Map<String,Object> getExtras() { return null; }
	}

	/** No getter, no field, no setter at all for the <js>"*"</js> dyna property under test - drives the "no
	 *  accessor" throws.  Needs at least one genuine property so {@code BeanMeta.create()} doesn't reject the
	 *  class outright as "not a bean" ({@code beansRequireSomeProperties} defaults to <jk>true</jk>). */
	public static class NoAccessorBean {
		public String dummy;
	}

	/** Getter-only Map property (no setter, no field) whose getter always returns <jk>null</jk>, forcing
	 *  {@code add()}/{@code set()} to allocate a fresh map and then invoke the (nonexistent) setter. */
	public static class MapGetterOnlyNullBean {
		public Map<String,Integer> getProps() { return null; }
	}

	/** Collection-typed property with neither getter nor field - {@code add()} must fail while probing the
	 *  current value via {@code invokeGetter}.  Needs at least one genuine property, same reason as
	 *  {@link NoAccessorBean}. */
	public static class NoAccessorCollectionBean {
		public String dummy;
	}

	/** Concrete (non-abstract) {@link List} implementation with a public no-arg constructor, used to exercise
	 *  the "declared abstract type is directly instantiable via BeanInstantiator" fast path. */
	public static class ConcreteList extends AbstractList<String> {
		private final List<String> data = new ArrayList<>();
		@Override public String get(int index) { return data.get(index); }
		@Override public int size() { return data.size(); }
		@Override public boolean add(String e) { return data.add(e); }
	}

	public static class ConcreteListFieldBean {
		public ConcreteList items;
	}

	/** Map property with a setter method but no field - used to drive the "setter present" branch of the
	 *  abstract-map "no accessor" check. */
	public static class SortedMapSetterBean {
		public SortedMap<String,Integer> props;
		public void setProps(SortedMap<String,Integer> v) { props = v; }
	}

	/** Setter throws, primitive-typed - drives the ignoreInvocationExceptionsOnSetters + primitive-default path. */
	public static class IntSetterThrowsBean {
		public void setX(int v) { throw new RuntimeException("boom"); }
	}

	/** Bean type with no zero-arg constructor - {@link FakeBeanInfo#newInstance()} fails with an
	 *  {@link org.apache.juneau.commons.reflect.ExecutableException}, which is not a {@link BeanRuntimeException}. */
	public static class NoZeroArgCtorNested {
		public final String name;
		public NoZeroArgCtorNested(String name) { this.name = name; }
	}

	public static class BeanPropWithNoZeroArgCtorNestedBean {
		public NoZeroArgCtorNested nested;
	}

	//====================================================================================================
	// Helpers
	//====================================================================================================

	/**
	 * Builds a {@link BeanMeta} whose {@link BeanMeta#getBeanInfo()} is non-null (a {@link FakeBeanInfo} wrapping
	 * {@code beanClass}), by routing through the marshalling-side {@link BeanMeta#create(BeanInfo, ClassInfo)}
	 * factory instead of the commons-only {@link BeanMeta#of(Class)}.  Needed because several error paths in
	 * {@link BeanPropertyMeta#add} / {@code setPropertyValue} read {@code beanMeta.getBeanInfo()} directly (for
	 * diagnostic messages), which is <jk>null</jk> — and thus NPEs — on a plain {@link BeanMeta#of(Class)} instance.
	 * Property discovery still runs in commons mode ({@link BeanMeta#create} passes a <jk>null</jk> marshalling
	 * context internally), so this does not affect any other behavior under test.
	 */
	private static <T> BeanMeta<T> marshallingBeanMeta(Class<T> beanClass) {
		return BeanMeta.create(new FakeBeanInfo<>(beanClass), null).beanMeta();
	}

	/** Overload allowing a custom {@link BeanConfigContext} (e.g. to flip ignore-* flags) to flow through {@link FakeBeanInfo#getBeanConfigContext()} into {@link BeanMeta#getConfig()}. */
	private static <T> BeanMeta<T> marshallingBeanMeta(Class<T> beanClass, BeanConfigContext config) {
		return BeanMeta.create(new FakeBeanInfo<>(beanClass, config), null).beanMeta();
	}

	/** Builds a field-only property (no getter/setter method — exercises the field-based invoke paths). */
	private static BeanPropertyMeta.Builder fieldProperty(Class<?> beanClass, String fieldName) throws Exception {
		var beanMeta = marshallingBeanMeta(beanClass);
		var f = info(beanClass.getField(fieldName));
		return BeanPropertyMeta.builder(beanMeta, fieldName).setField(f).canRead().canWrite();
	}

	@SuppressWarnings("unchecked")
	private static <T> BeanMap<T> mapOf(T bean) {
		var bm = BeanMap.of(bean, BeanMeta.of((Class<T>) bean.getClass()));
		bm.setBeanSession(new FakeBeanSession());
		return bm;
	}


	//====================================================================================================
	// add(BeanMap, String, Object) — Collection
	//====================================================================================================

	@Test
	void a01_add_collection_existingMutableCollection_addsDirectly() throws Exception {
		var bean = new ListBean();
		bean.items = new ArrayList<>(List.of("a"));
		var pm = fieldProperty(ListBean.class, "items").rawMetaType(new FakeBeanInfo<>(List.class)).build();
		var bMap = mapOf(bean);
		pm.add(bMap, null, "b");
		assertEquals(List.of("a", "b"), bean.items);
	}

	@Test
	void a02_add_collection_existingImmutableCollection_copiesIntoNewInstance() throws Exception {
		var bean = new ListBean();
		bean.items = List.of("a", "b");  // Immutable - canAddTo() returns false.
		var pm = fieldProperty(ListBean.class, "items").rawMetaType(new FakeBeanInfo<>(ArrayList.class)).build();
		var bMap = mapOf(bean);
		pm.add(bMap, null, "c");
		assertEquals(List.of("a", "b", "c"), bean.items);
		assertNotSame(List.of("a", "b"), bean.items);
	}

	@Test
	void a03_add_collection_noExisting_canCreateNewInstance_createsConcreteType() throws Exception {
		var bean = new ListBean();
		var pm = fieldProperty(ListBean.class, "items").rawMetaType(new FakeBeanInfo<>(ArrayList.class)).build();
		var bMap = mapOf(bean);
		pm.add(bMap, null, "x");
		assertEquals(List.of("x"), bean.items);
		assertInstanceOf(ArrayList.class, bean.items);
	}

	@Test
	void a04_add_collection_noExisting_cannotCreateNewInstance_fallsBackToList() throws Exception {
		var bean = new ListBean();
		var pm = fieldProperty(ListBean.class, "items")
			.rawMetaType(new FakeBeanInfo<>(List.class).canCreateNewInstance(false))
			.build();
		var bMap = mapOf(bean);
		pm.add(bMap, null, "x");
		assertEquals(List.of("x"), bean.items);
	}

	@Test
	void a05_add_notCollectionOrArray_throwsBeanRuntimeException() throws Exception {
		var bean = new NestedBean();
		var pm = fieldProperty(NestedBean.class, "name").rawMetaType(new FakeBeanInfo<>(String.class)).build();
		var bMap = mapOf(bean);
		assertThrowsWithMessage(BeanRuntimeException.class, "not a collection or array", () -> pm.add(bMap, null, "x"));
	}

	@Test
	void a06_add_readOnlyBean_cachesValue() throws Exception {
		var beanMeta = BeanMeta.of(ReadOnlyListBean.class);
		var f = info(ReadOnlyListBean.class.getField("items"));
		var pm = BeanPropertyMeta.builder(beanMeta, "items").setField(f).canRead().canWrite()
			.rawMetaType(new FakeBeanInfo<>(ArrayList.class)).build();
		var bMap = BeanMap.of(null, beanMeta);
		bMap.setBeanSession(new FakeBeanSession());
		pm.add(bMap, null, "x");
		pm.add(bMap, null, "y");
		assertEquals(List.of("x", "y"), bMap.propertyCache.get("items"));
	}

	//====================================================================================================
	// add(BeanMap, String, Object) — Array
	//====================================================================================================

	@Test
	void a07_add_array_noExistingArray_startsFromEmpty() throws Exception {
		var bean = new ArrayBean();
		var pm = fieldProperty(ArrayBean.class, "items")
			.rawMetaType(new FakeBeanInfo<>(String[].class).elementType(new FakeBeanInfo<>(String.class)))
			.build();
		var bMap = mapOf(bean);
		pm.add(bMap, null, "a");
		pm.add(bMap, null, "b");
		// add() on an array property stages entries in the map's arrayPropertyCache; the property that eventually
		// flushes them (via BeanMap#getBean) is looked up from the owning BeanMeta's own registered property map,
		// not this standalone `pm` instance, so verify the staged cache directly and exercise setArray() separately
		// (see f01) rather than going through BeanMap#getBean() here.
		assertEquals(List.of("a", "b"), bMap.arrayPropertyCache.get("items"));
	}

	@Test
	void a08_add_array_existingArray_copiesThenAppends() throws Exception {
		var bean = new ArrayBean();
		bean.items = new String[] {"a", "b"};
		var pm = fieldProperty(ArrayBean.class, "items")
			.rawMetaType(new FakeBeanInfo<>(String[].class).elementType(new FakeBeanInfo<>(String.class)))
			.build();
		var bMap = mapOf(bean);
		pm.add(bMap, null, "c");
		assertEquals(List.of("a", "b", "c"), bMap.arrayPropertyCache.get("items"));
	}

	//====================================================================================================
	// add(BeanMap, String, String, Object) — Map
	//====================================================================================================

	@Test
	void b01_addKeyValue_map_existingMap_putsDirectly() throws Exception {
		var bean = new MapBean();
		bean.props = new LinkedHashMap<>();
		bean.props.put("a", 1);
		var pm = fieldProperty(MapBean.class, "props").rawMetaType(new FakeBeanInfo<>(Map.class)).build();
		var bMap = mapOf(bean);
		pm.add(bMap, null, "b", 2);
		assertEquals(Map.of("a", 1, "b", 2), bean.props);
	}

	@Test
	void b02_addKeyValue_map_noExisting_canCreateNewInstance_createsConcreteType() throws Exception {
		var bean = new MapBean();
		var pm = fieldProperty(MapBean.class, "props").rawMetaType(new FakeBeanInfo<>(LinkedHashMap.class)).build();
		var bMap = mapOf(bean);
		pm.add(bMap, null, "a", 1);
		assertEquals(Map.of("a", 1), bean.props);
		assertInstanceOf(LinkedHashMap.class, bean.props);
	}

	@Test
	void b03_addKeyValue_map_noExisting_cannotCreateNewInstance_fallsBackToMap() throws Exception {
		var bean = new MapBean();
		var pm = fieldProperty(MapBean.class, "props")
			.rawMetaType(new FakeBeanInfo<>(Map.class).canCreateNewInstance(false))
			.build();
		var bMap = mapOf(bean);
		pm.add(bMap, null, "a", 1);
		assertEquals(Map.of("a", 1), bean.props);
	}

	@Test
	void b04_addKeyValue_readOnlyBean_cachesValue() throws Exception {
		var beanMeta = BeanMeta.of(ReadOnlyMapBean.class);
		var f = info(ReadOnlyMapBean.class.getField("props"));
		var pm = BeanPropertyMeta.builder(beanMeta, "props").setField(f).canRead().canWrite()
			.rawMetaType(new FakeBeanInfo<>(LinkedHashMap.class)).build();
		var bMap = BeanMap.of(null, beanMeta);
		bMap.setBeanSession(new FakeBeanSession());
		pm.add(bMap, null, "a", 1);
		// Second add() for the SAME property name exercises the `propertyCache.containsKey(name)` true branch
		// (first call takes the false branch and allocates the cache entry).
		pm.add(bMap, null, "b", 2);
		assertEquals(Map.of("a", 1, "b", 2), bMap.propertyCache.get("props"));
	}

	//====================================================================================================
	// add(BeanMap, String, String, Object) — Bean
	//====================================================================================================

	@Test
	void b05_addKeyValue_bean_existingBean_putsThroughBeanMap() throws Exception {
		var bean = new BeanPropBean();
		bean.nested = new NestedBean();
		var pm = fieldProperty(BeanPropBean.class, "nested").rawMetaType(new FakeBeanInfo<>(NestedBean.class).bean(true)).build();
		var bMap = mapOf(bean);
		pm.add(bMap, null, "name", "x");
		assertEquals("x", bean.nested.name);
	}

	@Test
	void b06_addKeyValue_bean_noExisting_canCreateNewInstance_createsAndPuts() throws Exception {
		var bean = new BeanPropBean();
		var pm = fieldProperty(BeanPropBean.class, "nested").rawMetaType(new FakeBeanInfo<>(NestedBean.class).bean(true)).build();
		var bMap = mapOf(bean);
		pm.add(bMap, null, "name", "y");
		assertNotNull(bean.nested);
		assertEquals("y", bean.nested.name);
	}

	@Test
	void b07_addKeyValue_bean_noExisting_cannotCreateNewInstance_setsNull() throws Exception {
		var bean = new BeanPropBean();
		var pm = fieldProperty(BeanPropBean.class, "nested")
			.rawMetaType(new FakeBeanInfo<>(NestedBean.class).bean(true).canCreateNewInstance(false))
			.build();
		var bMap = mapOf(bean);
		pm.add(bMap, null, "name", "z");
		assertNull(bean.nested);
	}

	@Test
	void b08_addKeyValue_notMapOrBean_throwsBeanRuntimeException() throws Exception {
		var bean = new NestedBean();
		var pm = fieldProperty(NestedBean.class, "name").rawMetaType(new FakeBeanInfo<>(String.class)).build();
		var bMap = mapOf(bean);
		assertThrowsWithMessage(BeanRuntimeException.class, "not a map or bean", () -> pm.add(bMap, null, "k", "v"));
	}

	//====================================================================================================
	// set() / setPropertyValue() — non-collection, non-map property
	//====================================================================================================

	@Test
	void c01_set_plainProperty_convertsAndInvokesSetter() throws Exception {
		var bean = new NestedBean();
		var pm = fieldProperty(NestedBean.class, "name").rawMetaType(new FakeBeanInfo<>(String.class)).build();
		var bMap = mapOf(bean);
		pm.set(bMap, null, "hello");
		assertEquals("hello", bean.name);
	}

	@Test
	void c02_set_nullValue_collectionOrMap_setsFieldToNull() throws Exception {
		var bean = new ListBean();
		bean.items = new ArrayList<>(List.of("a"));
		var pm = fieldProperty(ListBean.class, "items").rawMetaType(new FakeBeanInfo<>(List.class)).build();
		var bMap = mapOf(bean);
		pm.set(bMap, null, null);
		assertNull(bean.items);
	}

	@Test
	void c03_set_readOnlyBean_noPropertyCache_throws() throws Exception {
		var beanMeta = BeanMeta.of(ReadOnlyNoCtorBean.class);
		var f = info(ReadOnlyNoCtorBean.class.getField("name"));
		var pm = BeanPropertyMeta.builder(beanMeta, "name").setField(f).canRead().canWrite()
			.rawMetaType(new FakeBeanInfo<>(String.class)).build();
		// A BeanMap constructed with a null bean AND no @BeanCtor constructor args never allocates propertyCache.
		var bMap = BeanMap.of(null, beanMeta);
		bMap.setBeanSession(new FakeBeanSession());
		assertThrowsWithMessage(BeanRuntimeException.class, "Non-existent bean instance", () -> pm.set(bMap, null, "x"));
	}

	//====================================================================================================
	// set() / setPropertyValue() — Map property
	//====================================================================================================

	@Test
	void d01_set_map_fromCharSequence_parsesViaSession() throws Exception {
		var bean = new MapBean();
		var pm = fieldProperty(MapBean.class, "props").rawMetaType(new FakeBeanInfo<>(LinkedHashMap.class)).build();
		var bMap = mapOf(bean);
		pm.set(bMap, null, "{}");  // FakeBeanSession.parseToMap() returns an empty map.
		assertNotNull(bean.props);
		assertTrue(bean.props.isEmpty());
	}

	@Test
	void d02_set_map_fromNonMapNonCharSequence_throws() throws Exception {
		var bean = new MapBean();
		var pm = fieldProperty(MapBean.class, "props").rawMetaType(new FakeBeanInfo<>(LinkedHashMap.class)).build();
		var bMap = mapOf(bean);
		assertThrowsWithMessage(BeanRuntimeException.class, "Cannot set property", () -> pm.set(bMap, null, 123));
	}

	@Test
	void d03_set_map_canCreateNewInstance_existingMapCleared() throws Exception {
		var bean = new MapBean();
		bean.props = new LinkedHashMap<>(Map.of("stale", 0));
		var pm = fieldProperty(MapBean.class, "props")
			.rawMetaType(new FakeBeanInfo<>(LinkedHashMap.class).keyType(new FakeBeanInfo<>(Object.class)).valueType(new FakeBeanInfo<>(Object.class)))
			.build();
		var bMap = mapOf(bean);
		pm.set(bMap, null, Map.of("a", 1));
		assertEquals(Map.of("a", 1), bean.props);
	}

	@Test
	void d04_set_map_canCreateNewInstance_noExisting_createsConcreteType() throws Exception {
		var bean = new MapBean();
		var pm = fieldProperty(MapBean.class, "props")
			.rawMetaType(new FakeBeanInfo<>(LinkedHashMap.class).keyType(new FakeBeanInfo<>(Object.class)).valueType(new FakeBeanInfo<>(Object.class)))
			.build();
		var bMap = mapOf(bean);
		pm.set(bMap, null, Map.of("a", 1));
		assertEquals(Map.of("a", 1), bean.props);
		assertInstanceOf(LinkedHashMap.class, bean.props);
	}

	@Test
	void d05_set_sortedMap_abstractType_matchingInstance_setsDirectly() throws Exception {
		var bean = new SortedMapFieldBean();
		var pm = fieldProperty(SortedMapFieldBean.class, "props")
			.rawMetaType(new FakeBeanInfo<>(SortedMap.class).canCreateNewInstance(false)
				.keyType(new FakeBeanInfo<>(Object.class)).valueType(new FakeBeanInfo<>(Object.class)))
			.build();
		var bMap = mapOf(bean);
		var value = new TreeMap<>(Map.of("a", 1));
		pm.set(bMap, null, value);
		assertEquals(Map.of("a", 1), bean.props);
	}

	@Test
	void d06_set_sortedMap_abstractType_needsKeyValueConversion() throws Exception {
		var bean = new SortedMapFieldBean();
		var pm = fieldProperty(SortedMapFieldBean.class, "props")
			.rawMetaType(new FakeBeanInfo<>(SortedMap.class).canCreateNewInstance(false)
				.keyType(new FakeBeanInfo<>(Integer.class)).valueType(new FakeBeanInfo<>(Integer.class)))
			.build();
		var bMap = mapOf(bean);
		// Keys are Strings, not Integers -> keyType.isInstance(k) is false -> needsConversion path exercised
		// (FakeBeanSession.convertToType() is identity, so the map content passes through unchanged).
		var value = new TreeMap<>(Map.of("a", 1));
		pm.set(bMap, null, value);
		assertEquals(Map.of("a", 1), bean.props);
	}

	@Test
	void d07_set_sortedMap_abstractType_noExistingNoAccessor_throws() throws Exception {
		var beanMeta = marshallingBeanMeta(SortedMapFieldBean.class);
		var pm = BeanPropertyMeta.builder(beanMeta, "props")
			.canRead().canWrite()
			.rawMetaType(new FakeBeanInfo<>(SortedMap.class).canCreateNewInstance(false)
				.keyType(new FakeBeanInfo<>(Object.class)).valueType(new FakeBeanInfo<>(Object.class)))
			.build();
		var bean = new SortedMapFieldBean();
		var bMap = mapOf(bean);
		assertThrowsWithMessage(BeanRuntimeException.class, "no setter or public field is defined", () -> pm.set(bMap, null, new TreeMap<>(Map.of("a", 1))));
	}

	@Test
	void d08_set_sortedMap_abstractType_incompatibleValue_throws() throws Exception {
		var bean = new SortedMapFieldBean();
		var pm = fieldProperty(SortedMapFieldBean.class, "props")
			.rawMetaType(new FakeBeanInfo<>(SortedMap.class).canCreateNewInstance(false)
				.keyType(new FakeBeanInfo<>(Object.class)).valueType(new FakeBeanInfo<>(Object.class)))
			.build();
		var bMap = mapOf(bean);
		// A plain LinkedHashMap is not a SortedMap - the declared (abstract) type cannot be satisfied.
		assertThrowsWithMessage(BeanRuntimeException.class, "property type is abstract", () -> pm.set(bMap, null, new LinkedHashMap<>(Map.of("a", 1))));
	}

	//====================================================================================================
	// set() / setPropertyValue() — Collection property
	//====================================================================================================

	@Test
	void e01_set_collection_fromCharSequence_parsesViaSession() throws Exception {
		var bean = new ListBean();
		var pm = fieldProperty(ListBean.class, "items")
			.rawMetaType(new FakeBeanInfo<>(ArrayList.class).elementType(new FakeBeanInfo<>(Object.class)))
			.build();
		var bMap = mapOf(bean);
		pm.set(bMap, null, "[]");  // FakeBeanSession.parseToList() returns an empty list.
		assertNotNull(bean.items);
		assertTrue(bean.items.isEmpty());
	}

	@Test
	void e02_set_collection_fromNonCollectionNonCharSequence_throws() throws Exception {
		var bean = new ListBean();
		var pm = fieldProperty(ListBean.class, "items")
			.rawMetaType(new FakeBeanInfo<>(ArrayList.class).elementType(new FakeBeanInfo<>(Object.class)))
			.build();
		var bMap = mapOf(bean);
		assertThrowsWithMessage(BeanRuntimeException.class, "Cannot set property", () -> pm.set(bMap, null, 123));
	}

	@Test
	void e03_set_collection_canCreateNewInstance_existingCollectionCleared() throws Exception {
		var bean = new ListBean();
		bean.items = new ArrayList<>(List.of("stale"));
		var pm = fieldProperty(ListBean.class, "items")
			.rawMetaType(new FakeBeanInfo<>(ArrayList.class).elementType(new FakeBeanInfo<>(Object.class)))
			.build();
		var bMap = mapOf(bean);
		pm.set(bMap, null, List.of("a", "b"));
		assertEquals(List.of("a", "b"), bean.items);
	}

	@Test
	void e04_set_collection_canCreateNewInstance_noExisting_createsConcreteType() throws Exception {
		var bean = new ListBean();
		var pm = fieldProperty(ListBean.class, "items")
			.rawMetaType(new FakeBeanInfo<>(ArrayList.class).elementType(new FakeBeanInfo<>(Object.class)))
			.build();
		var bMap = mapOf(bean);
		pm.set(bMap, null, List.of("a"));
		assertEquals(List.of("a"), bean.items);
		assertInstanceOf(ArrayList.class, bean.items);
	}

	@Test
	void e05_set_collection_abstractType_matchingInstance_setsDirectly() throws Exception {
		var bean = new SetFieldBean();
		var pm = fieldProperty(SetFieldBean.class, "items")
			.rawMetaType(new FakeBeanInfo<>(Set.class).canCreateNewInstance(false).elementType(new FakeBeanInfo<>(Object.class)))
			.build();
		var bMap = mapOf(bean);
		pm.set(bMap, null, new LinkedHashSet<>(List.of("a", "b")));
		assertEquals(Set.of("a", "b"), bean.items);
	}

	@Test
	void e06_set_collection_abstractType_needsElementConversion() throws Exception {
		var bean = new SetFieldBean();
		var pm = fieldProperty(SetFieldBean.class, "items")
			.rawMetaType(new FakeBeanInfo<>(Set.class).canCreateNewInstance(false).elementType(new FakeBeanInfo<>(Integer.class)))
			.build();
		var bMap = mapOf(bean);
		// Elements are Strings, not Integers -> elementType.isInstance(v) is false -> per-element conversion path.
		pm.set(bMap, null, new LinkedHashSet<>(List.of("a", "b")));
		assertEquals(Set.of("a", "b"), bean.items);
	}

	@Test
	void e07_set_collection_abstractType_noExistingNoAccessor_throws() throws Exception {
		var beanMeta = marshallingBeanMeta(SetFieldBean.class);
		var pm = BeanPropertyMeta.builder(beanMeta, "items")
			.canRead().canWrite()
			.rawMetaType(new FakeBeanInfo<>(Set.class).canCreateNewInstance(false).elementType(new FakeBeanInfo<>(Object.class)))
			.build();
		var bean = new SetFieldBean();
		var bMap = mapOf(bean);
		assertThrowsWithMessage(BeanRuntimeException.class, "no setter or public field is defined", () -> pm.set(bMap, null, List.of("a")));
	}

	@Test
	void e08_set_sortedSet_abstractType_fallsBackToTreeSet() throws Exception {
		var bean = new SortedSetFieldBean();
		var pm = fieldProperty(SortedSetFieldBean.class, "items")
			.rawMetaType(new FakeBeanInfo<>(SortedSet.class).canCreateNewInstance(false).elementType(new FakeBeanInfo<>(Object.class)))
			.build();
		var bMap = mapOf(bean);
		// An ArrayList is not a SortedSet, so createDefaultCollectionForAbstractType() shape-falls-back to TreeSet.
		pm.set(bMap, null, new ArrayList<>(List.of("b", "a")));
		assertInstanceOf(SortedSet.class, bean.items);
		assertEquals(new TreeSet<>(List.of("a", "b")), bean.items);
	}

	@Test
	void e09_set_navigableSet_abstractType_fallsBackToTreeSet() throws Exception {
		var bean = new NavigableSetFieldBean();
		var pm = fieldProperty(NavigableSetFieldBean.class, "items")
			.rawMetaType(new FakeBeanInfo<>(NavigableSet.class).canCreateNewInstance(false).elementType(new FakeBeanInfo<>(Object.class)))
			.build();
		var bMap = mapOf(bean);
		pm.set(bMap, null, new ArrayList<>(List.of("a")));
		assertInstanceOf(NavigableSet.class, bean.items);
	}

	@Test
	void e10_set_set_abstractType_fallsBackToLinkedHashSet() throws Exception {
		var bean = new SetFieldBean();
		var pm = fieldProperty(SetFieldBean.class, "items")
			.rawMetaType(new FakeBeanInfo<>(Set.class).canCreateNewInstance(false).elementType(new FakeBeanInfo<>(Object.class)))
			.build();
		var bMap = mapOf(bean);
		pm.set(bMap, null, new ArrayList<>(List.of("a")));
		assertInstanceOf(LinkedHashSet.class, bean.items);
	}

	@Test
	void e11_set_deque_abstractType_fallsBackToArrayDeque() throws Exception {
		var bean = new DequeFieldBean();
		var pm = fieldProperty(DequeFieldBean.class, "items")
			.rawMetaType(new FakeBeanInfo<>(Deque.class).canCreateNewInstance(false).elementType(new FakeBeanInfo<>(Object.class)))
			.build();
		var bMap = mapOf(bean);
		pm.set(bMap, null, new ArrayList<>(List.of("a")));
		assertInstanceOf(ArrayDeque.class, bean.items);
	}

	@Test
	void e12_set_queue_abstractType_fallsBackToArrayDeque() throws Exception {
		var bean = new QueueFieldBean();
		var pm = fieldProperty(QueueFieldBean.class, "items")
			.rawMetaType(new FakeBeanInfo<>(Queue.class).canCreateNewInstance(false).elementType(new FakeBeanInfo<>(Object.class)))
			.build();
		var bMap = mapOf(bean);
		pm.set(bMap, null, new ArrayList<>(List.of("a")));
		assertInstanceOf(ArrayDeque.class, bean.items);
	}

	@Test
	void e13_set_list_abstractType_fallsBackToArrayList() throws Exception {
		var bean = new ListBean();
		var pm = fieldProperty(ListBean.class, "items")
			.rawMetaType(new FakeBeanInfo<>(List.class).canCreateNewInstance(false).elementType(new FakeBeanInfo<>(Object.class)))
			.build();
		var bMap = mapOf(bean);
		// A LinkedHashSet is not a List, so createDefaultCollectionForAbstractType() shape-falls-back to ArrayList
		// (the generic "List or raw Collection" default) rather than taking the "matching instance" fast path.
		pm.set(bMap, null, new LinkedHashSet<>(List.of("a")));
		assertInstanceOf(ArrayList.class, bean.items);
		assertEquals(List.of("a"), bean.items);
	}

	//====================================================================================================
	// setArray()
	//====================================================================================================

	@Test
	void f01_setArray_buildsArrayFromList() throws Exception {
		var bean = new ArrayBean();
		var pm = fieldProperty(ArrayBean.class, "items")
			.rawMetaType(new FakeBeanInfo<>(String[].class).elementType(new FakeBeanInfo<>(String.class)))
			.build();
		pm.setArray(bean, new ArrayList<>(List.of("a", "b")));
		assertArrayEquals(new String[] {"a", "b"}, bean.items);
	}

	//====================================================================================================
	// getRaw() / getInner() — ignoreInvocationExceptionsOnGetters, marshalling path
	//====================================================================================================

	@Test
	void g01_getRaw_throwingGetter_primitiveRawType_returnsPrimitiveDefaultWhenIgnoring() throws Exception {
		var cfg = BeanConfigContext.create().ignoreInvocationExceptionsOnGetters(true).build();
		var beanMeta = marshallingBeanMeta(IntGetterThrowsBean.class, cfg);
		var pm = BeanPropertyMeta.builder(beanMeta, "x")
			.setGetter(info(IntGetterThrowsBean.class.getMethod("getX")))
			.canRead().canWrite()
			.rawMetaType(new FakeBeanInfo<>(int.class))
			.build();
		var bMap = mapOf(new IntGetterThrowsBean());
		assertEquals(0, pm.getRaw(bMap, null));
	}

	@Test
	void g02_get_readTransformThrows_propagatesByDefault() throws Exception {
		var beanMeta = marshallingBeanMeta(GetterBean.class);
		var pm = BeanPropertyMeta.builder(beanMeta, "x")
			.setGetter(info(GetterBean.class.getMethod("getX")))
			.canRead().canWrite()
			.readTransform((session, val) -> { throw new RuntimeException("boom"); })
			.build();
		var bMap = mapOf(new GetterBean());
		assertThrows(BeanRuntimeException.class, () -> pm.get(bMap, null));
	}

	@Test
	void g03_get_readTransformThrows_returnsNullWhenIgnoring() throws Exception {
		var cfg = BeanConfigContext.create().ignoreInvocationExceptionsOnGetters(true).build();
		var beanMeta = marshallingBeanMeta(GetterBean.class, cfg);
		var pm = BeanPropertyMeta.builder(beanMeta, "x")
			.setGetter(info(GetterBean.class.getMethod("getX")))
			.canRead().canWrite()
			.readTransform((session, val) -> { throw new RuntimeException("boom"); })
			.build();
		var bMap = mapOf(new GetterBean());
		assertNull(pm.get(bMap, null));
	}

	@Test
	void g05_get_readTransformThrows_nonPrimitiveRawType_returnsNullWhenIgnoring() throws Exception {
		var cfg = BeanConfigContext.create().ignoreInvocationExceptionsOnGetters(true).build();
		var beanMeta = marshallingBeanMeta(GetterBean.class, cfg);
		var pm = BeanPropertyMeta.builder(beanMeta, "x")
			.setGetter(info(GetterBean.class.getMethod("getX")))
			.canRead().canWrite()
			.rawMetaType(new FakeBeanInfo<>(String.class))
			.readTransform((session, val) -> { throw new RuntimeException("boom"); })
			.build();
		var bMap = mapOf(new GetterBean());
		// rawTypeMeta is non-null here but NOT primitive - distinct from g04 (primitive) and g03 (rawTypeMeta
		// null) - exercises the `nn(rawTypeMeta) && rawTypeMeta.isPrimitive()` "true but not primitive" combo.
		assertNull(pm.get(bMap, null));
	}

	@Test
	void g04_get_readTransformThrows_returnsPrimitiveDefaultWhenIgnoring() throws Exception {
		var cfg = BeanConfigContext.create().ignoreInvocationExceptionsOnGetters(true).build();
		var beanMeta = marshallingBeanMeta(GetterBean.class, cfg);
		var pm = BeanPropertyMeta.builder(beanMeta, "x")
			.setGetter(info(GetterBean.class.getMethod("getX")))
			.canRead().canWrite()
			.rawMetaType(new FakeBeanInfo<>(int.class))
			.readTransform((session, val) -> { throw new RuntimeException("boom"); })
			.build();
		var bMap = mapOf(new GetterBean());
		assertEquals(0, pm.get(bMap, null));
	}

	//====================================================================================================
	// invokeGetter() / invokeSetter() / classNameForError() — dyna "no accessor" and getter-as-map paths
	//====================================================================================================

	@Test
	void h01_dynaSet_noSetterNoField_getterReturnsMap_putsIntoBackingMap() throws Exception {
		var bean = new DynaGetterOnlyMapBean();
		var beanMeta = marshallingBeanMeta(DynaGetterOnlyMapBean.class);
		var builder = BeanPropertyMeta.builder(beanMeta, "*")
			.setGetter(info(DynaGetterOnlyMapBean.class.getMethod("getExtras")))
			.canRead().canWrite();
		builder.isDyna = true;  // Package-private for direct test construction of dyna "no setter/field" scenarios.
		builder.isDynaGetterMap = true;  // Same precedent - the getter returns the whole Map, not a per-key value.
		var pm = builder.build();
		var bMap = mapOf(bean);
		pm.set(bMap, "k", "v");
		assertEquals("v", bean.getExtras().get("k"));
	}

	@Test
	void h02_dynaSet_noSetterNoField_getterReturnsNullMap_returnsNullWithoutThrowing() throws Exception {
		var bean = new DynaGetterOnlyNullMapBean();
		var beanMeta = marshallingBeanMeta(DynaGetterOnlyNullMapBean.class);
		var builder = BeanPropertyMeta.builder(beanMeta, "*")
			.setGetter(info(DynaGetterOnlyNullMapBean.class.getMethod("getExtras")))
			.canRead().canWrite();
		builder.isDyna = true;
		builder.isDynaGetterMap = true;
		var pm = builder.build();
		var bMap = mapOf(bean);
		assertNull(pm.set(bMap, "k", "v"));
	}

	@Test
	void h03_dynaSet_noAccessorAtAll_throwsWithMarshallingClassName() throws Exception {
		var bean = new NoAccessorBean();
		var beanMeta = marshallingBeanMeta(NoAccessorBean.class);
		var builder = BeanPropertyMeta.builder(beanMeta, "*").canRead().canWrite();
		builder.isDyna = true;
		var pm = builder.build();
		var bMap = mapOf(bean);
		// beanMeta.getBeanInfo() is non-null here (marshalling path), so classNameForError() takes the
		// cm.getName() branch rather than falling back to beanMeta.getClassInfo().getName().
		assertThrowsWithMessage(BeanRuntimeException.class, "no setter is defined", () -> pm.set(bMap, "k", "v"));
	}

	//====================================================================================================
	// add() — invokeGetter/invokeSetter "no accessor" BeanRuntimeException passthrough
	//====================================================================================================

	@Test
	void i01_add_collection_noAccessor_propagatesBeanRuntimeException() throws Exception {
		var bean = new NoAccessorCollectionBean();
		var beanMeta = marshallingBeanMeta(NoAccessorCollectionBean.class);
		var pm = BeanPropertyMeta.builder(beanMeta, "items").canRead().canWrite()
			.rawMetaType(new FakeBeanInfo<>(ArrayList.class).elementType(new FakeBeanInfo<>(Object.class)))
			.build();
		var bMap = mapOf(bean);
		// invokeGetter() throws (no getter, no field) inside the try block; add()'s
		// `catch (BeanRuntimeException e) { throw e; }` must rethrow it unwrapped rather than
		// re-wrapping it via the generic `catch (Exception e1)` clause.
		assertThrowsWithMessage(BeanRuntimeException.class, "Getter or public field not defined", () -> pm.add(bMap, null, "x"));
	}

	@Test
	void i02_addKeyValue_map_noAccessor_propagatesBeanRuntimeException() throws Exception {
		var bean = new NoAccessorCollectionBean();
		var beanMeta = marshallingBeanMeta(NoAccessorCollectionBean.class);
		var pm = BeanPropertyMeta.builder(beanMeta, "props").canRead().canWrite()
			.rawMetaType(new FakeBeanInfo<>(LinkedHashMap.class).elementType(new FakeBeanInfo<>(Object.class)))
			.build();
		var bMap = mapOf(bean);
		assertThrowsWithMessage(BeanRuntimeException.class, "Getter or public field not defined", () -> pm.add(bMap, null, "k", 1));
	}

	@Test
	void i03_addKeyValue_map_getterOnlyReturnsNull_invokeSetterHasNoAccessor_throws() throws Exception {
		var bean = new MapGetterOnlyNullBean();
		var beanMeta = marshallingBeanMeta(MapGetterOnlyNullBean.class);
		var pm = BeanPropertyMeta.builder(beanMeta, "props")
			.setGetter(info(MapGetterOnlyNullBean.class.getMethod("getProps")))
			.canRead().canWrite()
			.rawMetaType(new FakeBeanInfo<>(LinkedHashMap.class).elementType(new FakeBeanInfo<>(Integer.class)))
			.build();
		var bMap = mapOf(bean);
		// getProps() returns null, so add() allocates a fresh LinkedHashMap and then calls invokeSetter() to
		// write it back - but there's no setter and no field, so invokeSetter()'s own "no setter defined"
		// check fires (a different code path than the getter-side check exercised by i01/i02).
		assertThrowsWithMessage(BeanRuntimeException.class, "no setter is defined", () -> pm.add(bMap, null, "k", 1));
	}

	//====================================================================================================
	// createDefaultCollectionForAbstractType() — declared type directly instantiable
	//====================================================================================================

	@Test
	void j01_set_collection_abstractType_declaredTypeDirectlyInstantiable_usesIt() throws Exception {
		var bean = new ConcreteListFieldBean();
		var pm = fieldProperty(ConcreteListFieldBean.class, "items")
			.rawMetaType(new FakeBeanInfo<>(ConcreteList.class).canCreateNewInstance(false).elementType(new FakeBeanInfo<>(Object.class)))
			.build();
		var bMap = mapOf(bean);
		// ConcreteList isn't the incoming value's type (ArrayList) and isn't shape-recognized (Set/Deque/etc.),
		// but it IS directly instantiable via BeanInstantiator - createDefaultCollectionForAbstractType() should
		// use that best-effort path rather than falling back to the generic ArrayList default.
		pm.set(bMap, null, new ArrayList<>(List.of("a", "b")));
		assertInstanceOf(ConcreteList.class, bean.items);
		assertEquals(List.of("a", "b"), bean.items);
	}

	//====================================================================================================
	// set() — no setter/no field, non-collection/non-map property, ignore-* config combos
	//====================================================================================================

	@Test
	void k01_set_noSetterNoField_ignoreUnknownNullBeanProperties_nullValue_returnsNullWithoutThrowing() throws Exception {
		var cfg = BeanConfigContext.create().ignoreMissingSetters(false).ignoreUnknownNullBeanProperties(true).build();
		var beanMeta = marshallingBeanMeta(GetterBean.class, cfg);
		var pm = BeanPropertyMeta.builder(beanMeta, "x")
			.setGetter(info(GetterBean.class.getMethod("getX")))
			.canRead().canWrite()
			.rawMetaType(new FakeBeanInfo<>(String.class))
			.build();
		var bMap = mapOf(new GetterBean());
		assertNull(pm.set(bMap, null, null));
	}

	@Test
	void k02_set_noSetterNoField_ignoreMissingSetters_nonNullValue_returnsNullWithoutThrowing() throws Exception {
		var cfg = BeanConfigContext.create().ignoreMissingSetters(true).ignoreUnknownNullBeanProperties(false).build();
		var beanMeta = marshallingBeanMeta(GetterBean.class, cfg);
		var pm = BeanPropertyMeta.builder(beanMeta, "x")
			.setGetter(info(GetterBean.class.getMethod("getX")))
			.canRead().canWrite()
			.rawMetaType(new FakeBeanInfo<>(String.class))
			.build();
		var bMap = mapOf(new GetterBean());
		assertNull(pm.set(bMap, null, "ignored"));
	}

	@Test
	void k03_set_noSetterNoField_strictConfig_nonNullValue_throws() throws Exception {
		var cfg = BeanConfigContext.create().ignoreMissingSetters(false).ignoreUnknownNullBeanProperties(false).build();
		var beanMeta = marshallingBeanMeta(GetterBean.class, cfg);
		var pm = BeanPropertyMeta.builder(beanMeta, "x")
			.setGetter(info(GetterBean.class.getMethod("getX")))
			.canRead().canWrite()
			.rawMetaType(new FakeBeanInfo<>(String.class))
			.build();
		var bMap = mapOf(new GetterBean());
		assertThrowsWithMessage(BeanRuntimeException.class, "Setter or public field not defined", () -> pm.set(bMap, null, "v"));
	}

	//====================================================================================================
	// setPropertyValue() — old-value probe ternary (config.isBeanMapPutReturnsOldValue() / getter-based Map)
	//====================================================================================================

	@Test
	void k04_set_map_getterBased_noField_probesOldValueViaGetter() throws Exception {
		var bean = new MapGetterOnlyNullBean();
		var beanMeta = marshallingBeanMeta(MapGetterOnlyNullBean.class);
		var pm = BeanPropertyMeta.builder(beanMeta, "props")
			.setGetter(info(MapGetterOnlyNullBean.class.getMethod("getProps")))
			.canRead().canWrite()
			.rawMetaType(new FakeBeanInfo<>(LinkedHashMap.class).keyType(new FakeBeanInfo<>(Object.class)).valueType(new FakeBeanInfo<>(Object.class)))
			.build();
		var bMap = mapOf(bean);
		// getProps() (a getter, not a field) backs the property - setPropertyValue()'s old-value probe must
		// go through the getter (nn(getter) branch) since there's no field to fall back to.
		//
		// SUSPECTED BUG (not fixed here): when the map property is concrete
		// (canCreateNewInstance()==true) and has neither a setter nor a field, setPropertyValue() builds and
		// populates a fresh map (lines ~1223-1240) but then silently skips invokeSetter() (guarded by
		// `nn(setter) || nn(field)` at line 1241) - so the call succeeds without exception yet has no
		// observable effect on the bean.  Contrast with the abstract-type branch (canCreateNewInstance()==false),
		// which explicitly throws "no setter or public field is defined" for the same no-accessor condition
		// (lines ~1198-1201).  This test pins the current (silent no-op) behavior.
		assertNull(pm.set(bMap, null, Map.of("a", 1)));
	}

	@Test
	void k05_set_plainProperty_beanMapPutReturnsOldValue_probesOldValueEvenWithoutMapOrCollection() throws Exception {
		var cfg = BeanConfigContext.create().beanMapPutReturnsOldValue(true).build();
		var bean = new NestedBean();
		bean.name = "old";
		var beanMeta = marshallingBeanMeta(NestedBean.class, cfg);
		var f = info(NestedBean.class.getField("name"));
		var pm = BeanPropertyMeta.builder(beanMeta, "name").setField(f).canRead().canWrite()
			.rawMetaType(new FakeBeanInfo<>(String.class))
			.build();
		var bMap = mapOf(bean);
		// isBeanMapPutReturnsOldValue()=true forces the old-value probe even for a plain (non-map/collection)
		// property, where it would otherwise be skipped entirely.
		var old = pm.set(bMap, null, "new");
		assertEquals("old", old);
		assertEquals("new", bean.name);
	}

	//====================================================================================================
	// add(BeanMap, String, String, Object) — generic Exception catch (not a BeanRuntimeException)
	//====================================================================================================

	@Test
	void b09_addKeyValue_bean_newInstanceFails_wrapsNonBeanRuntimeExceptionInGenericCatch() throws Exception {
		var bean = new BeanPropWithNoZeroArgCtorNestedBean();
		var pm = fieldProperty(BeanPropWithNoZeroArgCtorNestedBean.class, "nested")
			.rawMetaType(new FakeBeanInfo<>(NoZeroArgCtorNested.class).bean(true))
			.build();
		var bMap = mapOf(bean);
		// FakeBeanInfo.newInstance() throws ExecutableException (not a BeanRuntimeException) when the target
		// type has no no-arg constructor - add()'s `catch (Exception e) { throw brex(e); }` clause (distinct
		// from the `catch (BeanRuntimeException e) { throw e; }` clause exercised elsewhere) must still surface
		// it as a BeanRuntimeException.
		assertThrows(BeanRuntimeException.class, () -> pm.add(bMap, null, "name", "x"));
	}

	//====================================================================================================
	// set() — abstract Map path: setter-present variant of the "no accessor" / mixed key-value-type checks
	//====================================================================================================

	@Test
	void d09_set_sortedMap_abstractType_setterPresent_incompatibleValue_throws() throws Exception {
		var bean = new SortedMapSetterBean();
		var beanMeta = marshallingBeanMeta(SortedMapSetterBean.class);
		var pm = BeanPropertyMeta.builder(beanMeta, "props")
			.setSetter(info(SortedMapSetterBean.class.getMethod("setProps", SortedMap.class)))
			.canRead().canWrite()
			.rawMetaType(new FakeBeanInfo<>(SortedMap.class).canCreateNewInstance(false)
				.keyType(new FakeBeanInfo<>(Object.class)).valueType(new FakeBeanInfo<>(Object.class)))
			.build();
		var bMap = mapOf(bean);
		// setter present (so the `setter == null && field == null` "no accessor" throw does NOT fire) but the
		// incoming value isn't assignable to the declared abstract type and there's no existing map to reuse -
		// falls through to the "property type is abstract" throw instead.
		assertThrowsWithMessage(BeanRuntimeException.class, "property type is abstract",
			() -> pm.set(bMap, null, new LinkedHashMap<>(Map.of("a", 1))));
	}

	@Test
	void d10_set_sortedMap_abstractType_mixedKeyValueTypes_needsPartialConversion() throws Exception {
		var bean = new SortedMapFieldBean();
		var pm = fieldProperty(SortedMapFieldBean.class, "props")
			.rawMetaType(new FakeBeanInfo<>(SortedMap.class).canCreateNewInstance(false)
				.keyType(new FakeBeanInfo<>(Object.class)).valueType(new FakeBeanInfo<>(Integer.class)))
			.build();
		var bMap = mapOf(bean);
		// keyType is Object (no conversion needed) but valueType is Integer (specific) - unlike d05 (both
		// Object) and d06 (both Integer), this exercises the "mixed" combination of the
		// `! (keyType.isObject() && valueType.isObject())` check.
		var value = new TreeMap<>(Map.of("a", 1));
		pm.set(bMap, null, value);
		assertEquals(Map.of("a", 1), bean.props);
	}

	//====================================================================================================
	// setPropertyValue() — ignoreInvocationExceptionsOnSetters, primitive rawTypeMeta
	//====================================================================================================

	@Test
	void d11_set_primitiveSetterThrows_returnsPrimitiveDefaultWhenIgnoring() throws Exception {
		var cfg = BeanConfigContext.create().ignoreInvocationExceptionsOnSetters(true).build();
		var beanMeta = marshallingBeanMeta(IntSetterThrowsBean.class, cfg);
		var pm = BeanPropertyMeta.builder(beanMeta, "x")
			.setSetter(info(IntSetterThrowsBean.class.getMethod("setX", int.class)))
			.canRead().canWrite()
			.rawMetaType(new FakeBeanInfo<>(int.class))
			.build();
		var bMap = mapOf(new IntSetterThrowsBean());
		assertEquals(0, pm.set(bMap, null, 5));
	}

	//====================================================================================================
	// getInner() — overrideValue / read-only-bean propertyCache probes
	//====================================================================================================

	@Test
	void d12_get_overrideValue_returnsOverrideWithoutInvokingGetter() throws Exception {
		var beanMeta = marshallingBeanMeta(GetterBean.class);
		var pm = BeanPropertyMeta.builder(beanMeta, "x")
			.setGetter(info(GetterBean.class.getMethod("getX")))
			.canRead().canWrite()
			.overrideValue("overridden")
			.build();
		var bMap = mapOf(new GetterBean());
		assertEquals("overridden", pm.get(bMap, null));
	}

	@Test
	void d13_get_readOnlyBean_returnsFromPropertyCache() throws Exception {
		var beanMeta = BeanMeta.of(ReadOnlyListBean.class);
		var f = info(ReadOnlyListBean.class.getField("items"));
		var pm = BeanPropertyMeta.builder(beanMeta, "items").setField(f).canRead().canWrite()
			.rawMetaType(new FakeBeanInfo<>(ArrayList.class))
			.build();
		var bMap = BeanMap.of(null, beanMeta);
		bMap.setBeanSession(new FakeBeanSession());
		bMap.propertyCache.put("items", List.of("cached"));
		assertEquals(List.of("cached"), pm.get(bMap, null));
	}

	//====================================================================================================
	// set() — no setter/no field, remaining ignore-* config combos (value==null, ignoreUnknownNull==false)
	//====================================================================================================

	@Test
	void k06_set_noSetterNoField_nullValue_ignoreUnknownNullFalse_ignoreMissingSettersTrue_returnsNull() throws Exception {
		var cfg = BeanConfigContext.create().ignoreMissingSetters(true).ignoreUnknownNullBeanProperties(false).build();
		var beanMeta = marshallingBeanMeta(GetterBean.class, cfg);
		var pm = BeanPropertyMeta.builder(beanMeta, "x")
			.setGetter(info(GetterBean.class.getMethod("getX")))
			.canRead().canWrite()
			.rawMetaType(new FakeBeanInfo<>(String.class))
			.build();
		var bMap = mapOf(new GetterBean());
		assertNull(pm.set(bMap, null, null));
	}

	@Test
	void k07_set_noSetterNoField_nullValue_strictConfig_throws() throws Exception {
		var cfg = BeanConfigContext.create().ignoreMissingSetters(false).ignoreUnknownNullBeanProperties(false).build();
		var beanMeta = marshallingBeanMeta(GetterBean.class, cfg);
		var pm = BeanPropertyMeta.builder(beanMeta, "x")
			.setGetter(info(GetterBean.class.getMethod("getX")))
			.canRead().canWrite()
			.rawMetaType(new FakeBeanInfo<>(String.class))
			.build();
		var bMap = mapOf(new GetterBean());
		assertThrowsWithMessage(BeanRuntimeException.class, "Setter or public field not defined", () -> pm.set(bMap, null, null));
	}

	//====================================================================================================
	// invokeGetter() — dyna "no accessor" throw
	//====================================================================================================

	@Test
	void h04_dynaGet_noAccessorAtAll_throwsWithMarshallingClassName() throws Exception {
		var bean = new NoAccessorBean();
		var beanMeta = marshallingBeanMeta(NoAccessorBean.class);
		var builder = BeanPropertyMeta.builder(beanMeta, "*").canRead().canWrite();
		builder.isDyna = true;
		var pm = builder.build();
		var bMap = mapOf(bean);
		assertThrowsWithMessage(BeanRuntimeException.class, "Getter or public field not defined", () -> pm.getRaw(bMap, "key"));
	}

	//====================================================================================================
	// set() — BasicRuntimeException wrapping (session.parseToMap/parseToList raising a marshalling ParseException)
	//====================================================================================================

	@Test
	void d14_set_writeTransformThrowsBasicRuntimeException_wrapsInBeanRuntimeException() throws Exception {
		var bean = new NestedBean();
		var beanMeta = marshallingBeanMeta(NestedBean.class);
		var f = info(NestedBean.class.getField("name"));
		var pm = BeanPropertyMeta.builder(beanMeta, "name").setField(f).canRead().canWrite()
			.rawMetaType(new FakeBeanInfo<>(String.class))
			.writeTransform((session, val) -> { throw new org.apache.juneau.commons.BasicRuntimeException("bad value: %s", val); })
			.build();
		var bMap = mapOf(bean);
		// writeTransform.apply() runs directly inside set()'s own try block (unlike the setPropertyValue()
		// calls, whose exceptions are all caught and re-wrapped internally by setPropertyValue()'s own
		// try/catch before ever reaching this frame) - it's the only realistic way to have a raw
		// BasicRuntimeException reach set()'s own `catch (BasicRuntimeException e2) { throw brex(e2); }` clause,
		// which must translate it to a BeanRuntimeException (BeanRuntimeException itself does NOT extend
		// BasicRuntimeException, per the comment on that catch clause, so it would otherwise propagate unchanged).
		assertThrows(BeanRuntimeException.class, () -> pm.set(bMap, null, "x"));
	}
}
