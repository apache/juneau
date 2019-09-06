// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau;

import static org.apache.juneau.PropertyType.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ClassUtils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.PropertyStoreBuilder.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.reflect.*;


/**
 * Represents an immutable collection of properties.
 *
 * <p>
 * The general idea behind a property store is to serve as a reusable configuration of an artifact (e.g. a Serializer)
 * such that the artifact can be cached and reused if the property stores are 'equal'.
 *
 * <h5 class='topic'>Concept</h5>
 *
 * <p>
 * For example, two serializers of the same type created with the same configuration will always end up being
 * the same serializer:
 *
 * <p class='bcode w800'>
 * 	WriterSerializer s1 = JsonSerializer.<jsm>create</jsm>().pojoSwaps(MySwap.<jk>class</jk>).simple().build();
 * 	WriterSerializer s2 = JsonSerializer.<jsm>create</jsm>().simple().pojoSwaps(MySwap.<jk>class</jk>).build();
 * 	<jk>assert</jk>(s1 == s2);
 * </p>
 *
 * <p>
 * This has the effect of significantly improving performance, especially if you're creating many Serializers and
 * Parsers.
 *
 * <h5 class='topic'>PropertyStoreBuilder</h5>
 *
 * <p>
 * The {@link PropertyStoreBuilder} class is used to build up and instantiate immutable <c>PropertyStore</c>
 * objects.
 *
 * <p>
 * In the example above, the property store being built looks like the following:
 *
 * <p class='bcode w800'>
 * 	PropertyStore ps = PropertyStore
 * 		.<jsm>create</jsm>()
 * 		.set(<js>"BeanContext.pojoSwaps.lc"</js>, MySwap.<jk>class</jk>)
 * 		.set(<js>"JsonSerializer.simpleMode.b"</js>, <jk>true</jk>)
 * 		.build();
 * </p>
 *
 * <p>
 * Property stores are immutable, comparable, and their hashcodes are calculated exactly one time.
 * That makes them particularly suited for use as hashmap keys, and thus for caching reusable serializers and parsers.
 *
 * <h5 class='topic'>Property naming convention</h5>
 *
 * <p>
 * Property names must have the following format...
 * <p class='bcode w800'>
 * 	<js>"{class}.{name}.{type}"</js>
 * </p>
 * <p>
 * ...where the parts consist of the following...
 * <ul>
 * 	<li><js>"{class}"</js> - The group name of the property (e.g. <js>"JsonSerializer"</js>).
 * 		<br>It's always going to be the simple class name of the class it's associated with.
 * 	<li><js>"{name}"</js> - The property name (e.g. <js>"useWhitespace"</js>).
 * 	<li><js>"{type}"</js> - The property data type.
 * 		<br>A 1 or 2 character string that identifies the data type of the property.
 * 		<br>Valid values are:
 * 		<ul>
 * 			<li><js>"s"</js>:  <c>String</c>
 * 			<li><js>"b"</js>:  <c>Boolean</c>
 * 			<li><js>"i"</js>:  <c>Integer</c>
 * 			<li><js>"c"</js>:  <c>Class</c>
 * 			<li><js>"o"</js>:  <c>Object</c>
 * 			<li><js>"ss"</js>:  <c>TreeSet&lt;String&gt;</c>
 * 			<li><js>"si"</js>:  <c>TreeSet&lt;Integer&gt;</c>
 * 			<li><js>"sc"</js>:  <c>TreeSet&lt;Class&gt;</c>
 * 			<li><js>"ls"</js>:  <c>Linkedlist&lt;String&gt;</c>
 * 			<li><js>"li"</js>:  <c>Linkedlist&lt;Integer&gt;</c>
 * 			<li><js>"lc"</js>:  <c>Linkedlist&lt;Class&gt;</c>
 * 			<li><js>"lo"</js>:  <c>Linkedlist&lt;Object&gt;</c>
 * 			<li><js>"sms"</js>:  <c>TreeMap&lt;String,String&gt;</c>
 * 			<li><js>"smi"</js>:  <c>TreeMap&lt;String,Integer&gt;</c>
 * 			<li><js>"smc"</js>:  <c>TreeMap&lt;String,Class&gt;</c>
 * 			<li><js>"smo"</js>:  <c>TreeMap&lt;String,Object&gt;</c>
 * 			<li><js>"oms"</js>:  <c>LinkedHashMap&lt;String,String&gt;</c>
 * 			<li><js>"omi"</js>:  <c>LinkedHashMap&lt;String,Integer&gt;</c>
 * 			<li><js>"omc"</js>:  <c>LinkedHashMap&lt;String,Class&gt;</c>
 * 			<li><js>"omo"</js>:  <c>LinkedHashMap&lt;String,Object&gt;</c>
 * 		</ul>
 * </ul>
 *
 * <p>
 * For example, <js>"BeanContext.pojoSwaps.lc"</js> refers to a property on the <c>BeanContext</c> class
 * called <c>pojoSwaps</c> that has a data type of <c>List&lt;Class&gt;</c>.
 *
 * <h5 class='topic'>Property value normalization</h5>
 *
 * <p>
 * Property values get 'normalized' when they get set.
 * For example, calling <code>propertyStore.set(<js>"BeanContext.debug.b"</js>, <js>"true"</js>)</code> will cause the property
 * value to be converted to a boolean.
 *
 * <h5 class='topic'>Set types</h5>
 *
 * <p>
 * The <js>"sX"</js> property types are sorted sets.
 * <br>Use these for collections of objects where the order is not important.
 * <br>Internally, a <c>TreeSet</c> is used so that the order in which you add elements does not affect the
 * resulting order of the property.
 *
 * <h5 class='topic'>List types</h5>
 *
 * <p>
 * The <js>"lX"</js> property types are ordered lists.
 * <br>Use these in cases where the order in which entries are added is important.
 *
 * <p>
 * Adding to a list property will cause the new entries to be added to the BEGINNING of the list.
 * <br>This ensures that the resulting order of the list is in most-to-least importance.
 *
 * <p>
 * For example, multiple calls to <c>pojoSwaps()</c> causes new entries to be added to the beginning of the list
 * so that previous values can be 'overridden':
 * <p class='bcode w800'>
 * 	<jc>// Swap order:  [MySwap2.class, MySwap1.class]</jc>
 * 	JsonSerializer.create().pojoSwaps(MySwap1.<jk>class</jk>).pojoSwaps(MySwap2.<jk>class</jk>).build();
 * </p>
 *
 * <p>
 * Note that the order is different when passing multiple values into the <c>pojoSwaps()</c> method, in which
 * case the order should be first-match-wins:
 * <p class='bcode w800'>
 * 	<jc>// Swap order:  [MySwap1.class, MySwap2.class]</jc>
 * 	JsonSerializer.create().pojoSwaps(MySwap1.<jk>class</jk>,MySwap2.<jk>class</jk>).build();
 * </p>
 *
 * <p>
 * Combined, the results look like this:
 * <p class='bcode w800'>
 * 	<jc>// Swap order:  [MySwap4.class, MySwap3.class, MySwap1.class, MySwap2.class]</jc>
 * 	JsonSerializer
 * 		.create()
 * 		.pojoSwaps(MySwap1.<jk>class</jk>,MySwap2.<jk>class</jk>)
 * 		.pojoSwaps(MySwap3.<jk>class</jk>)
 * 		.pojoSwaps(MySwap4.<jk>class</jk>)
 * 		.build();
 * </p>
 *
 * <h5 class='topic'>Map types</h5>
 *
 * <p>
 * The <js>"smX"</js> and <js>"omX"</js> are sorted and order maps respectively.
 *
 * <h5 class='topic'>Command properties</h5>
 *
 * <p>
 * Set and list properties have the additional convenience 'command' names for adding and removing entries:
 * <p class='bcode w800'>
 * 	<js>"{class}.{name}.{type}/add"</js>  <jc>// Add a value to the set/list.</jc>
 * 	<js>"{class}.{name}.{type}/remove"</js>  <jc>// Remove a value from the set/list.</jc>
 * </p>
 *
 * <p>
 * Map properties have the additional convenience property name for adding and removing map entries:
 * <p class='bcode w800'>
 * 	<js>"{class}.{name}.{type}/add.{key}"</js>  <jc>// Add a map entry (or delete if the value is null).</jc>
 * </p>
 */
@SuppressWarnings("unchecked")
public final class PropertyStore {

	/**
	 * A default empty property store.
	 */
	public static PropertyStore DEFAULT = PropertyStore.create().build();

	final SortedMap<String,PropertyGroup> groups;
	private final int hashCode;

	// Created by PropertyStoreBuilder.build()
	PropertyStore(Map<String,PropertyGroupBuilder> propertyMaps) {
		TreeMap<String,PropertyGroup> m = new TreeMap<>();
		for (Map.Entry<String,PropertyGroupBuilder> p : propertyMaps.entrySet())
			m.put(p.getKey(), p.getValue().build());
		this.groups = Collections.unmodifiableSortedMap(m);
		this.hashCode = groups.hashCode();
	}

	/**
	 * Creates a new empty builder for a property store.
	 *
	 * @return A new empty builder for a property store.
	 */
	public static PropertyStoreBuilder create() {
		return new PropertyStoreBuilder();
	}

	/**
	 * Creates a new property store builder initialized with the values in this property store.
	 *
	 * @return A new property store builder.
	 */
	public PropertyStoreBuilder builder() {
		return new PropertyStoreBuilder(this);
	}

	private Property findProperty(String key) {
		String g = group(key);
		String k = key.substring(g.length()+1);
		PropertyGroup pm = groups.get(g);

		if (pm != null) {
			Property p = pm.get(k);
			if (p != null)
				return p;
		}

		String s = System.getProperty(key);
		if (s != null)
			return PropertyStoreBuilder.MutableProperty.create(k, s).build();

		return null;
	}

	/**
	 * Returns the raw property value with the specified name.
	 *
	 * @param key The property name.
	 * @return The property value, or <jk>null</jk> if it doesn't exist.
	 */
	public Object getProperty(String key) {
		Property p = findProperty(key);
		return p == null ? null : p.value;
	}

	/**
	 * Returns the property value with the specified name.
	 *
	 * @param key The property name.
	 * @param c The class to cast or convert the value to.
	 * @param def The default value.
	 * @return The property value, or the default value if it doesn't exist.
	 */
	public <T> T getProperty(String key, Class<T> c, T def) {
		Property p = findProperty(key);
		return p == null ? def : p.as(c);
	}

	/**
	 * Returns the class property with the specified name.
	 *
	 * @param key The property name.
	 * @param type The class type of the property.
	 * @param def The default value.
	 * @return The property value, or the default value if it doesn't exist.
	 */
	public <T> Class<? extends T> getClassProperty(String key, Class<T> type, Class<? extends T> def) {
		Property p = findProperty(key);
		return p == null ? def : (Class<T>)p.as(Class.class);
	}

	/**
	 * Returns the array property value with the specified name.
	 *
	 * @param key The property name.
	 * @param eType The class type of the elements in the property.
	 * @return The property value, or an empty array if it doesn't exist.
	 */
	public <T> T[] getArrayProperty(String key, Class<T> eType) {
		Property p = findProperty(key);
		return (T[]) (p == null ? Array.newInstance(eType, 0) : p.asArray(eType));
	}

	/**
	 * Returns the array property value with the specified name.
	 *
	 * @param key The property name.
	 * @param eType The class type of the elements in the property.
	 * @param def The default value.
	 * @return The property value, or an empty array if it doesn't exist.
	 */
	public <T> T[] getArrayProperty(String key, Class<T> eType, T[] def) {
		Property p = findProperty(key);
		return p == null ? def : p.asArray(eType);
	}

	/**
	 * Returns the class array property with the specified name.
	 *
	 * @param key The property name.
	 * @return The property value, or an empty array if it doesn't exist.
	 */
	public Class<?>[] getClassArrayProperty(String key) {
		Property p = findProperty(key);
		return p == null ? new Class[0] : p.as(Class[].class);
	}

	/**
	 * Returns the class array property with the specified name.
	 *
	 * @param key The property name.
	 * @param def The default value.
	 * @return The property value, or an empty array if it doesn't exist.
	 */
	public Class<?>[] getClassArrayProperty(String key, Class<?>[] def) {
		Property p = findProperty(key);
		return p == null ? def : p.as(Class[].class);
	}

	/**
	 * Returns the class array property with the specified name.
	 *
	 * @param key The property name.
	 * @param eType The class type of the elements in the property.
	 * @return The property value, or an empty array if it doesn't exist.
	 */
	public <T> Class<T>[] getClassArrayProperty(String key, Class<T> eType) {
		Property p = findProperty(key);
		return p == null ? new Class[0] : p.as(Class[].class);
	}

	/**
	 * Returns the set property with the specified name.
	 *
	 * @param key The property name.
	 * @param eType The class type of the elements in the property.
	 * @return The property value as an unmodifiable <c>LinkedHashSet</c>, or an empty set if it doesn't exist.
	 */
	public <T> Set<T> getSetProperty(String key, Class<T> eType) {
		Property p = findProperty(key);
		return p == null ? Collections.EMPTY_SET : p.asSet(eType);
	}

	/**
	 * Returns the set property with the specified name.
	 *
	 * @param key The property name.
	 * @param eType The class type of the elements in the property.
	 * @param def The default value if the property doesn't exist or is empty.
	 * @return The property value as an unmodifiable <c>LinkedHashSet</c>, or the default value if it doesn't exist or is empty.
	 */
	public <T> Set<T> getSetProperty(String key, Class<T> eType, Set<T> def) {
		Set<T> l = getSetProperty(key, eType);
		return (l.isEmpty() ? def : l);
	}

	/**
	 * Returns the class set property with the specified name.
	 *
	 * @param key The property name.
	 * @return The property value as an unmodifiable <c>LinkedHashSet</c>, or an empty set if it doesn't exist.
	 */
	@SuppressWarnings("rawtypes")
	public Set<Class<?>> getClassSetProperty(String key) {
		Property p = findProperty(key);
		return p == null ? Collections.EMPTY_SET : (Set)p.asSet(Class.class);
	}

	/**
	 * Returns the class set property with the specified name.
	 *
	 * @param key The property name.
	 * @param eType The class type of the elements in the property.
	 * @return The property value as an unmodifiable <c>LinkedHashSet</c>, or an empty set if it doesn't exist.
	 */
	@SuppressWarnings("rawtypes")
	public <T> Set<Class<T>> getClassSetProperty(String key, Class<T> eType) {
		Property p = findProperty(key);
		return p == null ? Collections.EMPTY_SET : (Set)p.asSet(Class.class);
	}

	/**
	 * Returns the list property with the specified name.
	 *
	 * @param key The property name.
	 * @param eType The class type of the elements in the property.
	 * @return The property value as an unmodifiable <c>ArrayList</c>, or an empty list if it doesn't exist.
	 */
	public <T> List<T> getListProperty(String key, Class<T> eType) {
		Property p = findProperty(key);
		return p == null ? Collections.EMPTY_LIST : p.asList(eType);
	}

	/**
	 * Returns the list property with the specified name.
	 *
	 * @param key The property name.
	 * @param eType The class type of the elements in the property.
	 * @param def The default value if the property doesn't exist or is empty.
	 * @return The property value as an unmodifiable <c>ArrayList</c>, or the default value if it doesn't exist or is empty.
	 */
	public <T> List<T> getListProperty(String key, Class<T> eType, List<T> def) {
		List<T> l = getListProperty(key, eType);
		return (l.isEmpty() ? def : l);
	}

	/**
	 * Returns the class list property with the specified name.
	 *
	 * @param key The property name.
	 * @return The property value as an unmodifiable <c>ArrayList</c>, or an empty list if it doesn't exist.
	 */
	@SuppressWarnings("rawtypes")
	public List<Class<?>> getClassListProperty(String key) {
		Property p = findProperty(key);
		return p == null ? Collections.EMPTY_LIST : (List)p.asList(Class.class);
	}

	/**
	 * Returns the class list property with the specified name.
	 *
	 * @param key The property name.
	 * @param eType The class type of the elements in the property.
	 * @return The property value as an unmodifiable <c>ArrayList</c>, or an empty list if it doesn't exist.
	 */
	@SuppressWarnings("rawtypes")
	public <T> List<Class<T>> getClassListProperty(String key, Class<T> eType) {
		Property p = findProperty(key);
		return p == null ? Collections.EMPTY_LIST : (List)p.asList(Class.class);
	}

	/**
	 * Returns the map property with the specified name.
	 *
	 * @param key The property name.
	 * @param eType The class type of the elements in the property.
	 * @return The property value as an unmodifiable <c>LinkedHashMap</c>, or an empty map if it doesn't exist.
	 */
	public <T> Map<String,T> getMapProperty(String key, Class<T> eType) {
		Property p = findProperty(key);
		return p == null ? Collections.EMPTY_MAP : p.asMap(eType);
	}

	/**
	 * Returns the class map property with the specified name.
	 *
	 * @param key The property name.
	 * @return The property value as an unmodifiable <c>LinkedHashMap</c>, or an empty map if it doesn't exist.
	 */
	@SuppressWarnings("rawtypes")
	public Map<String,Class<?>> getClassMapProperty(String key) {
		Property p = findProperty(key);
		return p == null ? Collections.EMPTY_MAP : (Map)p.asMap(Class.class);
	}

	/**
	 * Returns the class map property with the specified name.
	 *
	 * @param key The property name.
	 * @param eType The class type of the elements in the property.
	 * @return The property value as an unmodifiable <c>LinkedHashMap</c>, or an empty map if it doesn't exist.
	 */
	@SuppressWarnings("rawtypes")
	public <T> Map<String,Class<T>> getClassMapProperty(String key, Class<T> eType) {
		Property p = findProperty(key);
		return p == null ? Collections.EMPTY_MAP : (Map)p.asMap(Class.class);
	}

	/**
	 * Returns an instance of the specified class, string, or object property.
	 *
	 * <p>
	 * If instantiating a class, assumes the class has a no-arg constructor.
	 * Otherwise, throws a runtime exception.
	 *
	 * @param key The property name.
	 * @param type The class type of the property.
	 * @param def
	 * 	The default value if the property doesn't exist.
	 * 	<br>Can either be an instance of <c>T</c>, or a <code>Class&lt;? <jk>extends</jk> T&gt;</code>, or <jk>null</jk>.
	 * @return A new property instance.
	 */
	public <T> T getInstanceProperty(String key, Class<T> type, Object def) {
		return getInstanceProperty(key, type, def, ResourceResolver.BASIC);
	}

	/**
	 * Returns an instance of the specified class, string, or object property.
	 *
	 * @param key The property name.
	 * @param type The class type of the property.
	 * @param def
	 * 	The default value if the property doesn't exist.
	 * 	<br>Can either be an instance of <c>T</c>, or a <code>Class&lt;? <jk>extends</jk> T&gt;</code>.
	 * @param resolver
	 * 	The resolver to use for instantiating objects.
	 * @param args
	 * 	Arguments to pass to the constructor.
	 * 	Constructors matching the arguments are always used before no-arg constructors.
	 * @return A new property instance.
	 */
	public <T> T getInstanceProperty(String key, Class<T> type, Object def, ResourceResolver resolver, Object...args) {
		return getInstanceProperty(key, null, type, def, resolver, args);
	}

	/**
	 * Returns an instance of the specified class, string, or object property.
	 *
	 * @param key The property name.
	 * @param outer The outer object if the class we're instantiating is an inner class.
	 * @param type The class type of the property.
	 * @param def
	 * 	The default value if the property doesn't exist.
	 * 	<br>Can either be an instance of <c>T</c>, or a <code>Class&lt;? <jk>extends</jk> T&gt;</code>.
	 * @param resolver
	 * 	The resolver to use for instantiating objects.
	 * @param args
	 * 	Arguments to pass to the constructor.
	 * 	Constructors matching the arguments are always used before no-arg constructors.
	 * @return A new property instance.
	 */
	public <T> T getInstanceProperty(String key, Object outer, Class<T> type, Object def, ResourceResolver resolver, Object...args) {
		Property p = findProperty(key);
		if (p != null)
			return p.asInstance(outer, type, resolver, args);
		if (def == null)
			return null;
		if (def instanceof Class)
			return resolver.resolve(outer, (Class<T>)def, args);
		if (type.isInstance(def))
			return (T)def;
		throw new ConfigException("Could not instantiate property ''{0}'' as type ''{1}'' with default value ''{2}''", key, type, def);
	}

	/**
	 * Returns the specified property as an array of instantiated objects.
	 *
	 * @param key The property name.
	 * @param type The class type of the property.
	 * @param def The default object to return if the property doesn't exist.
	 * @return A new property instance.
	 */
	public <T> T[] getInstanceArrayProperty(String key, Class<T> type, T[] def) {
		return getInstanceArrayProperty(key, type, def, ResourceResolver.BASIC);
	}

	/**
	 * Returns the specified property as an array of instantiated objects.
	 *
	 * @param key The property name.
	 * @param type The class type of the property.
	 * @param def The default object to return if the property doesn't exist.
	 * @param resolver
	 * 	The resolver to use for instantiating objects.
	 * @param args
	 * 	Arguments to pass to the constructor.
	 * 	Constructors matching the arguments are always used before no-arg constructors.
	 * @return A new property instance.
	 */
	public <T> T[] getInstanceArrayProperty(String key, Class<T> type, T[] def, ResourceResolver resolver, Object...args) {
		return getInstanceArrayProperty(key, null, type, def, resolver, args);
	}

	/**
	 * Returns the specified property as an array of instantiated objects.
	 *
	 * @param key The property name.
	 * @param outer The outer object if the class we're instantiating is an inner class.
	 * @param type The class type of the property.
	 * @param def The default object to return if the property doesn't exist.
	 * @param resolver
	 * 	The resolver to use for instantiating objects.
	 * @param args
	 * 	Arguments to pass to the constructor.
	 * 	Constructors matching the arguments are always used before no-arg constructors.
	 * @return A new property instance.
	 */
	public <T> T[] getInstanceArrayProperty(String key, Object outer, Class<T> type, T[] def, ResourceResolver resolver, Object...args) {
		Property p = findProperty(key);
		return p == null ? def : p.asInstanceArray(outer, type, resolver, args);
	}

	/**
	 * Returns the keys found in the specified property group.
	 *
	 * <p>
	 * The keys are NOT prefixed with group names.
	 *
	 * @param group The group name.
	 * @return The set of property keys, or an empty set if the group was not found.
	 */
	public Set<String> getPropertyKeys(String group) {
		if (group == null)
			return Collections.EMPTY_SET;
		PropertyGroup g = groups.get(group);
		return g == null ? Collections.EMPTY_SET : g.keySet();
	}

	@Override /* Object */
	public int hashCode() {
		return hashCode;
	}

	/**
	 * Returns a hashcode of this property store using only the specified group names.
	 *
	 * @param groups The names of the property groups to use in the calculation.
	 * @return The hashcode.
	 */
	public Integer hashCode(String...groups) {
		HashCode c = new HashCode();
		for (String p : groups)
			if (p != null)
				c.add(p).add(this.groups.get(p));
		return c.get();
	}

	@Override /* Object */
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o instanceof PropertyStore)
			return (this.groups.equals(((PropertyStore)o).groups));
		return false;
	}

	/**
	 * Compares two property stores, but only based on the specified group names.
	 *
	 * @param ps The property store to compare to.
	 * @param groups The groups to compare.
	 * @return <jk>true</jk> if the two property stores are equal in the specified groups.
	 */
	public boolean equals(PropertyStore ps, String...groups) {
		if (this == ps)
			return true;
		for (String p : groups) {
			if (p != null) {
				PropertyGroup pg1 = this.groups.get(p), pg2 = ps.groups.get(p);
				if (pg1 == null && pg2 == null)
					continue;
				if (pg1 == null || pg2 == null)
					return false;
				if (! pg1.equals(pg2))
					return false;
			}
		}
		return true;
	}

	/**
	 * Used for debugging.
	 *
	 * <p>
	 * Allows property stores to be serialized to easy-to-read JSON objects.
	 *
	 * @param beanSession The bean session.
	 * @return The property groups.
	 */
	public Map<String,PropertyGroup> swap(BeanSession beanSession) {
		return groups;
	}

	//-------------------------------------------------------------------------------------------------------------------
	// PropertyGroup
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * A group of properties with the same prefixes.
	 */
	public static class PropertyGroup {
		final SortedMap<String,Property> properties;
		private final int hashCode;

		PropertyGroup(Map<String,MutableProperty> properties) {
			TreeMap<String,Property> m = new TreeMap<>();
			for (Map.Entry<String,MutableProperty> p : properties.entrySet())
				m.put(p.getKey(), p.getValue().build());
			this.properties = Collections.unmodifiableSortedMap(m);
			this.hashCode = this.properties.hashCode();
		}

		PropertyGroupBuilder builder() {
			return new PropertyGroupBuilder(properties);
		}

		Property get(String key) {
			return properties.get(key);
		}

		@Override /* Object */
		public int hashCode() {
			return hashCode;
		}

		@Override /* Object */
		public boolean equals(Object o) {
			if (o instanceof PropertyGroup)
				return properties.equals(((PropertyGroup)o).properties);
			return false;
		}

		Set<String> keySet() {
			return properties.keySet();
		}

		/**
		 * Converts this object to serializable form.
		 *
		 * @return The serializable form of this object.
		 */
		public Map<String,Property> swap() {
			return properties;
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Property
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * A property in a property store group.
	 */
	public static class Property {
		private final String name;
		final Object value;
		private final int hashCode;
		private final PropertyType type;

		Property(String name, Object value, PropertyType type) {
			this.name = name;
			this.value = value;
			this.type = type;
			this.hashCode = value.hashCode();
		}

		MutableProperty mutable() {
			switch(type) {
				case STRING:
				case BOOLEAN:
				case INTEGER:
				case CLASS:
				case OBJECT: return new MutableSimpleProperty(name, type, value);
				case SET_STRING:
				case SET_INTEGER:
				case SET_CLASS: return new MutableSetProperty(name, type, value);
				case LIST_STRING:
				case LIST_INTEGER:
				case LIST_CLASS:
				case LIST_OBJECT: return new MutableListProperty(name, type, value);
				case SORTED_MAP_STRING:
				case SORTED_MAP_INTEGER:
				case SORTED_MAP_CLASS:
				case SORTED_MAP_OBJECT: return new MutableMapProperty(name, type, value);
				case ORDERED_MAP_STRING:
				case ORDERED_MAP_INTEGER:
				case ORDERED_MAP_CLASS:
				case ORDERED_MAP_OBJECT: return new MutableLinkedMapProperty(name, type, value);
			}
			throw new ConfigException("Invalid type specified: ''{0}''", type);
		}

		/**
		 * Converts this property to the specified type.
		 *
		 * @param <T> The type to convert the property to.
		 * @param c The type to convert the property to.
		 * @return The converted type.
		 * @throws ConfigException If value could not be converted.
		 */
		public <T> T as(Class<T> c) {
			Class<?> c2 = ClassInfo.of(c).getPrimitiveWrapper();
			if (c2 != null)
				c = (Class<T>)c2;
			if (c.isInstance(value))
				return (T)value;
			if (c.isArray() && value instanceof Collection)
				return (T)asArray(c.getComponentType());
			if (type == STRING) {
				T t = fromString(c, value.toString());
				if (t != null)
					return t;
			}
			throw new ConfigException("Invalid property conversion ''{0}'' to ''{1}'' on property ''{2}''", type, c, name);
		}

		/**
		 * Converts this property to the specified array type.
		 *
		 * @param <T> The element type to convert the property to.
		 * @param eType The element type to convert the property to.
		 * @return The converted type.
		 * @throws ConfigException If value could not be converted.
		 */
		public <T> T[] asArray(Class<T> eType) {
			if (value instanceof Collection) {
				Collection<?> l = (Collection<?>)value;
				Object t = Array.newInstance(eType, l.size());
				int i = 0;
				for (Object o : l) {
					Object o2 = null;
					if (eType.isInstance(o))
						o2 = o;
					else if (type == SET_STRING || type == LIST_STRING) {
						o2 = fromString(eType, o.toString());
						if (o2 == null)
							throw new ConfigException("Invalid property conversion ''{0}'' to ''{1}[]'' on property ''{2}''", type, eType, name);
					} else {
						throw new ConfigException("Invalid property conversion ''{0}'' to ''{1}[]'' on property ''{2}''", type, eType, name);
					}
					Array.set(t, i++, o2);
				}
				return (T[])t;
			}
			throw new ConfigException("Invalid property conversion ''{0}'' to ''{1}[]'' on property ''{2}''", type, eType, name);
		}

		/**
		 * Converts this property to the specified set type.
		 *
		 * @param <T> The element type to convert the property to.
		 * @param eType The element type to convert the property to.
		 * @return The converted type.
		 * @throws ConfigException If value could not be converted.
		 */
		public <T> Set<T> asSet(Class<T> eType) {
			if (type == SET_STRING && eType == String.class || type == SET_INTEGER && eType == Integer.class || type == SET_CLASS && eType == Class.class) {
				return (Set<T>)value;
			} else if (type == SET_STRING) {
				Set<T> s = new LinkedHashSet<>();
				for (Object o : (Set<?>)value) {
					T t = fromString(eType, o.toString());
					if (t == null)
						throw new ConfigException("Invalid property conversion ''{0}'' to ''Set<{1}>'' on property ''{2}''", type, eType, name);
					s.add(t);
				}
				return Collections.unmodifiableSet(s);
			} else {
				throw new ConfigException("Invalid property conversion ''{0}'' to ''Set<{1}>'' on property ''{2}''", type, eType, name);
			}
		}

		/**
		 * Converts this property to the specified list type.
		 *
		 * @param <T> The element type to convert the property to.
		 * @param eType The element type to convert the property to.
		 * @return The converted type.
		 * @throws ConfigException If value could not be converted.
		 */
		public <T> List<T> asList(Class<T> eType) {
			if (type == LIST_STRING && eType == String.class || type == LIST_INTEGER && eType == Integer.class || type == LIST_CLASS && eType == Class.class || type == LIST_OBJECT) {
				return (List<T>)value;
			} else if (type == PropertyType.LIST_STRING) {
				List<T> l = new ArrayList<>();
				for (Object o : (List<?>)value) {
					T t = fromString(eType, o.toString());
					if (t == null)
						throw new ConfigException("Invalid property conversion ''{0}'' to ''List<{1}>'' on property ''{2}''", type, eType, name);
					l.add(t);
				}
				return unmodifiableList(l);
			} else {
				throw new ConfigException("Invalid property conversion ''{0}'' to ''List<{1}>'' on property ''{2}''", type, eType, name);
			}
		}

		/**
		 * Converts this property to the specified map type.
		 *
		 * @param <T> The element type to convert the property to.
		 * @param eType The element type to convert the property to.
		 * @return The converted type.
		 * @throws ConfigException If value could not be converted.
		 */
		public <T> Map<String,T> asMap(Class<T> eType) {
			if (
				eType == String.class && (type == SORTED_MAP_STRING || type == ORDERED_MAP_STRING)
				|| eType == Integer.class && (type == SORTED_MAP_INTEGER || type == ORDERED_MAP_INTEGER)
				|| eType == Class.class && (type == SORTED_MAP_CLASS || type == ORDERED_MAP_CLASS)
				|| (type == SORTED_MAP_OBJECT || type == ORDERED_MAP_OBJECT)) {
				return (Map<String,T>)value;
			} else if (type == SORTED_MAP_STRING || type == ORDERED_MAP_STRING) {
				Map<String,T> m = new LinkedHashMap<>();
				for (Map.Entry<String,String> e : ((Map<String,String>)value).entrySet()) {
					T t = fromString(eType, e.getValue());
					if (t == null)
						throw new ConfigException("Invalid property conversion ''{0}'' to ''Map<String,{1}>'' on property ''{2}''", type, eType, name);
					m.put(e.getKey(), t);
				}
				return unmodifiableMap(m);
			} else {
				throw new ConfigException("Invalid property conversion ''{0}'' to ''Map<String,{1}>'' on property ''{2}''", type, eType, name);
			}
		}

		/**
		 * Converts this property to the specified instance type.
		 *
		 * @param outer The outer class if this is a member type.
		 * @param iType The type to instantiate.
		 * @param resolver The resource resolver.
		 * @param args The arguments to pass to the constructor.
		 * @param <T> The type to instantiate.
		 * @return The instantiated object.
		 * @throws ConfigException If value could not be instantiated.
		 */
		public <T> T asInstance(Object outer, Class<T> iType, ResourceResolver resolver, Object...args) {
			if (value == null)
				return null;
			if (type == STRING)
				return fromString(iType, value.toString());
			else if (type == OBJECT || type == CLASS) {
				T t = instantiate(resolver, outer, iType, value, args);
				if (t != null)
					return t;
			}
			throw new ConfigException("Invalid property instantiation ''{0}'' to ''{1}'' on property ''{2}''", type, iType, name);
		}

		/**
		 * Converts this property to an array of specified instance type.
		 *
		 * @param outer The outer class if this is a member type.
		 * @param eType The entry type to instantiate.
		 * @param resolver The resource resolver.
		 * @param args The arguments to pass to the constructor.
		 * @param <T> The type to instantiate.
		 * @return The instantiated object.
		 * @throws ConfigException If value could not be instantiated.
		 */
		public <T> T[] asInstanceArray(Object outer, Class<T> eType, ResourceResolver resolver, Object...args) {
			if (value instanceof Collection) {
				Collection<?> l = (Collection<?>)value;
				Object t = Array.newInstance(eType, l.size());
				int i = 0;
				for (Object o : l) {
					Object o2 = null;
					if (eType.isInstance(o))
						o2 = o;
					else if (type == SET_STRING || type == LIST_STRING)
						o2 = fromString(eType, o.toString());
					else if (type == SET_CLASS || type == LIST_CLASS || type == LIST_OBJECT)
						o2 = instantiate(resolver, outer, eType, o, args);
					if (o2 == null)
						throw new ConfigException("Invalid property conversion ''{0}'' to ''{1}[]'' on property ''{2}''", type, eType, name);
					Array.set(t, i++, o2);
				}
				return (T[])t;
			}
			throw new ConfigException("Invalid property conversion ''{0}'' to ''{1}[]'' on property ''{2}''", type, eType, name);
		}

		@Override /* Object */
		public int hashCode() {
			return hashCode;
		}

		@Override /* Object */
		public boolean equals(Object o) {
			if (o instanceof Property)
				return ((Property)o).value.equals(value);
			return false;
		}

		/**
		 * Converts this object to serializable form.
		 *
		 * @return The serializable form of this object.
		 */
		public Object swap() {
			return value;
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-------------------------------------------------------------------------------------------------------------------

	static <T> T instantiate(ResourceResolver resolver, Object outer, Class<T> c, Object value, Object...args) {
		if (ClassInfo.of(c).isParentOf(value.getClass()))
			return (T)value;
		if (ClassInfo.of(value.getClass()).isChildOf(Class.class))
			return resolver.resolve(outer, (Class<T>)value, args);
		return null;
	}

	private static String group(String key) {
		if (key == null || key.indexOf('.') == -1 || key.charAt(key.length()-1) == '.')
			throw new ConfigException("Invalid property name specified: ''{0}''", key);
		String g = key.substring(0, key.indexOf('.'));
		if (g.isEmpty())
			throw new ConfigException("Invalid property name specified: ''{0}''", key);
		return g;
	}

	@Override /* Object */
	public String toString() {
		return SimpleJsonSerializer.DEFAULT.toString(this);
	}
}
