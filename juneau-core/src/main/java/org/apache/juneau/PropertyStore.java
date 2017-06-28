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

import static org.apache.juneau.BeanContext.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;

/**
 * A store for instantiating {@link Context} objects.
 *
 * <p>
 * The hierarchy of these objects are...
 * <ul class='spaced-list'>
 * 	<li>
 * 		{@link PropertyStore} - A thread-safe, modifiable context property store.
 * 		<br>Used to create {@link Context} objects.
 * 	<li>
 * 		{@link Context} - A reusable, cacheable, thread-safe, read-only context with configuration properties copied
 * 		from the store.
 * 		<br>Often used to create {@link Session} objects.
 * 	<li>
 * 		{@link Session} - A one-time-use non-thread-safe object.
 * 		<br>Used by serializers and parsers to retrieve context properties and to be used as scratchpads.
 * </ul>
 *
 * <h6 class='topic'>PropertyStore objects</h6>
 *
 * Property stores can be thought of as consisting of the following:
 * <ul>
 * 	<li>A <code>Map&lt;String,Object&gt;</code> of context properties.
 * 	<li>A <code>Map&lt;Class,Context&gt;</code> of context instances.
 * </ul>
 *
 * <p>
 * Property stores are used to create and cache {@link Context} objects using the {@link #getContext(Class)} method.
 *
 * <p>
 * As a general rule, {@link PropertyStore} objects are 'slow'.
 * <br>Setting and retrieving properties on a store can involve relatively slow data conversion and synchronization.
 * <br>However, the {@link #getContext(Class)} method is fast, and will return cached context objects if the context
 * properties have not changed.
 *
 * <p>
 * Property stores can be used to store context properties for a variety of contexts.
 * <br>For example, a single store can store context properties for the JSON serializer, XML serializer, HTML serializer
 * etc... and can thus be used to retrieve context objects for those serializers.
 *
 * <h6 class='topic'>Context properties</h6>
 *
 * Context properties are 'settings' for serializers and parsers.
 * <br>For example, the {@link BeanContext#BEAN_sortProperties} context property defines whether bean properties should be
 * serialized in alphabetical order.
 *
 * <p>
 * Each {@link Context} object should contain the context properties that apply to it as static fields
 * (e.g {@link BeanContext#BEAN_sortProperties}).
 *
 * <p>
 * Context properties can be of the following types:
 * <ul class='spaced-list'>
 * 	<li>
 * 		<l>SIMPLE</l> - A simple property.
 * 		<br>Examples include:  booleans, integers, Strings, Classes, etc...
 * 		<br>An example of this would be the {@link BeanContext#BEAN_sortProperties} property.
 * 		<br>It's name is simply <js>"BeanContext.sortProperties"</js>.
 * 	<li>
 * 		<l>SET</l> - A sorted set of objects.
 * 		<br>These are denoted by appending <js>".set"</js> to the property name.
 * 		<br>Objects can be of any type, even complex types.
 * 		<br>Sorted sets use tree sets to maintain the value in alphabetical order.
 * 		<br>For example, the {@link BeanContext#BEAN_notBeanClasses} property is used to store classes that should not be
 * 		treated like beans.
 * 		<br>It's name is <js>"BeanContext.notBeanClasses.set"</js>.
 * 	<li>
 * 		<l>LIST</l> - A list of unique objects.
 * 		<br>These are denoted by appending <js>".list"</js> to the property name.
 * 		<br>Objects can be of any type, even complex types.
 * 		<br>Use lists if the ordering of the values in the set is important (similar to how the order of entries in a
 * 		classpath is important).
 * 		<br>For example, the {@link BeanContext#BEAN_beanFilters} property is used to store bean filters.
 * 		<br>It's name is <js>"BeanContext.transforms.list"</js>.
 * 	<li>
 * 		<l>MAP</l> - A sorted map of key-value pairs.
 * 		<br>These are denoted by appending <js>".map"</js> to the property name.
 * 		<br>Keys can be any type directly convertible to and from Strings.
 * 		Values can be of any type, even complex types.
 * 		<br>For example, the {@link BeanContext#BEAN_implClasses} property is used to specify the names of implementation
 * 		classes for interfaces.
 * 		<br>It's name is <js>"BeanContext.implClasses.map"</js>.
 * </ul>
 *
 * <p>
 * All context properties are set using the {@link #setProperty(String, Object)} method.
 *
 * <p>
 * Default values for context properties can be specified globally as system properties.
 * <br>Example: <code>System.<jsm>setProperty</jsm>(<jsf>BEAN_sortProperties</jsf>, <jk>true</jk>);</code>
 *
 * <p>
 * SET and LIST properties can be added to using the {@link #addToProperty(String, Object)} method and removed from
 * using the {@link #removeFromProperty(String, Object)} method.
 *
 * <p>
 * SET and LIST properties can also be added to and removed from by appending <js>".add"</js> or <js>".remove"</js> to
 * the property name and using the {@link #setProperty(String, Object)} method.
 *
 * <p>
 * The following shows the two different ways to append to a set or list property:
 * <p class='bcode'>
 * 	PropertyStore ps = <jk>new</jk> PropertyStore().setProperty(<js>"BeanContext.notBeanClasses.set"</js>,
 * 		Collections.<jsm>emptySet</jsm>());
 *
 * 	<jc>// Append to set property using addTo().</jc>
 * 	ps.addToProperty(<js>"BeanContext.notBeanClasses.set"</js>, MyNotBeanClass.<jk>class</jk>);
 *
 * 	<jc>// Append to set property using set().</jc>
 * 	ps.setProperty(<js>"BeanContext.notBeanClasses.set.add"</js>, MyNotBeanClass.<jk>class</jk>);
 * </p>
 *
 * <p>
 * SET and LIST properties can also be set and manipulated using JSON strings.
 * <p class='bcode'>
 * 	PropertyStore ps = PropertyStore.<jsm>create</jsm>();
 *
 * 	<jc>// Set SET value using JSON array.
 * 	ps.setProperty(<js>"BeanContext.notBeanClasses.set"</js>, <js>"['com.my.MyNotBeanClass1']"</js>);
 *
 * 	<jc>// Add to SET using simple string.
 * 	ps.addToProperty(<js>"BeanContext.notBeanClasses.set"</js>, <js>"com.my.MyNotBeanClass2"</js>);
 *
 * 	<jc>// Add an array of values as a JSON array..
 * 	ps.addToProperty(<js>"BeanContext.notBeanClasses.set"</js>, <js>"['com.my.MyNotBeanClass3']"</js>);
 *
 * 	<jc>// Remove an array of values as a JSON array..
 * 	ps.removeFromProperty(<js>"BeanContext.notBeanClasses.set"</js>, <js>"['com.my.MyNotBeanClass3']"</js>);
 * </p>
 *
 * <p>
 * MAP properties can be added to using the {@link #putToProperty(String, Object, Object)} and
 * {@link #putToProperty(String, Object)} methods.
 * <br>MAP property entries can be removed by setting the value to <jk>null</jk>
 * (e.g. <code>putToProperty(<js>"BEAN_implClasses"</js>, MyNotBeanClass.<jk>class</jk>, <jk>null</jk>);</code>.
 * <br>MAP properties can also be added to by appending <js>".put"</js> to the property name and using the
 * {@link #setProperty(String, Object)} method.
 *
 * <p>
 * The following shows the two different ways to append to a set property:
 * <p class='bcode'>
 * 	PropertyStore ps = PropertyStore.<jsm>create</jsm>().setProperty(<js>"BeanContext.implClasses.map"</js>,
 * 		Collections.<jsm>emptyMap</jsm>());
 *
 * 	<jc>// Append to map property using putTo().</jc>
 * 	ps.putToProperty(<js>"BeanContext.implClasses.map"</js>, MyInterface.<jk>class</jk>, MyInterfaceImpl.<jk>class</jk>);
 *
 * 	<jc>// Append to map property using set().</jc>
 * 	Map m = <jk>new</jk> AMap().append(MyInterface.<jk>class</jk>,MyInterfaceImpl.<jk>class</jk>);
 * 	ps.setProperty(<js>"BeanContext.implClasses.map.put"</js>, m);
 * </p>
 *
 * <p>
 * MAP properties can also be set and manipulated using JSON strings.
 * <p class='bcode'>
 * 	PropertyStore ps = PropertyStore.<jsm>create</jsm>();
 *
 * 	<jc>// Set MAP value using JSON object.</jc>
 * 	ps.setProperty(<js>"BeanContext.implClasses.map"</js>,
 * 		<js>"{'com.my.MyInterface1':'com.my.MyInterfaceImpl1'}"</js>);
 *
 * 	<jc>// Add to MAP using JSON object.</jc>
 * 	ps.putToProperty(<js>"BeanContext.implClasses.map"</js>,
 * 		<js>"{'com.my.MyInterface2':'com.my.MyInterfaceImpl2'}"</js>);
 *
 * 	<jc>// Remove from MAP using JSON object.</jc>
 * 	ps.putToProperty(<js>"BeanContext.implClasses.map"</js>, <js>"{'com.my.MyInterface2':null}"</js>);
 * </p>
 *
 * <p>
 * Context properties are retrieved from this store using the following 3 methods:
 * <ul class='spaced-list'>
 * 	<li>
 * 		{@link #getProperty(String, Class, Object)} - Retrieve a SIMPLE or SET property converted to the specified
 * 		class type.
 * 	<li>
 * 		{@link #getMap(String, Class, Class, Map)} - Retrieve a MAP property with keys/values converted to the
 * 		specified class types.
 * 	<li>
 * 		{@link #getPropertyMap(String)} - Retrieve a map of all context properties with the specified prefix
 * 		(e.g. <js>"BeanContext"</js> for {@link BeanContext} properties).
 * </ul>
 *
 * <p>
 * As a general rule, only {@link Context} objects will use these read methods.
 *
 * <h6 class='topic'>Context objects</h6>
 *
 * A Context object can be thought of as unmodifiable snapshot of a store.
 * <br>They should be 'fast' by avoiding synchronization by using final fields whenever possible.
 * <br>However, they MUST be thread safe.
 *
 * <p>
 * Context objects are created using the {@link #getContext(Class)} method.
 * <br>As long as the properties on a store have not been modified, the store will return a cached copy of a context.
 * <p class='bcode'>
 * 	PropertyStore ps = PropertyStore.<jsm>create</jsm>();
 *
 * 	<jc>// Get BeanContext with default store settings.</jc>
 * 	BeanContext bc = ps.getContext(BeanContext.<jk>class</jk>);
 *
 * 	<jc>// Get another one.  This will be the same one.</jc>
 * 	BeanContext bc2 = ps.getContext(BeanContext.<jk>class</jk>);
 * 	<jsm>assertTrue</jsm>(bc1 == bc2);
 *
 * 	<jc>// Set a property.</jc>
 * 	ps.setProperty(<jsf>BEAN_sortProperties</jsf>, <jk>true</jk>);
 *
 * 	<jc>// Get another one.  This will be different!</jc>
 * 	bc2 = f.getContext(BeanContext.<jk>class</jk>);
 * 	<jsm>assertFalse</jsm>(bc1 == bc2);
 * </p>
 *
 * <h6 class='topic'>Session objects</h6>
 *
 * Session objects are created through {@link Context} objects, typically through a <code>createContext()</code> method.
 * <br>Unlike context objects, they are NOT reusable and NOT thread safe.
 * <br>They are meant to be used one time and then thrown away.
 * <br>They should NEVER need to use synchronization.
 *
 * <p>
 * Session objects are also often used as scratchpads for information such as keeping track of call stack information
 * to detect recursive loops when serializing beans.
 */
public final class PropertyStore {

	// All configuration properties in this object.
	// Keys are property prefixes (e.g. 'BeanContext').
	// Values are maps containing properties for that specific prefix.
	private Map<String,PropertyMap> properties = new ConcurrentSkipListMap<String,PropertyMap>();

	// Context cache.
	// This gets cleared every time any properties change on this object.
	private final Map<Class<? extends Context>,Context> contexts = new ConcurrentHashMap<Class<? extends Context>,Context>();

	// Global Context cache.
	// Property stores that are the 'same' will use the same maps from this cache.
	// 'same' means the context properties are all the same when converted to strings.
	private static final ConcurrentHashMap<Integer, ConcurrentHashMap<Class<? extends Context>,Context>>
		globalContextCache = new ConcurrentHashMap<Integer, ConcurrentHashMap<Class<? extends Context>,Context>>();

	private ReadWriteLock lock = new ReentrantReadWriteLock();
	private Lock rl = lock.readLock(), wl = lock.writeLock();

	// Classloader used to instantiate Class instances.
	ClassLoader classLoader = ClassLoader.getSystemClassLoader();

	// Parser to use to convert JSON strings to POJOs
	ReaderParser defaultParser;

	// Bean session for converting strings to POJOs.
	private static BeanSession beanSession;

	// Used to keep properties in alphabetical order regardless of whether
	// they're not strings.
	private static Comparator<Object> PROPERTY_COMPARATOR = new Comparator<Object>() {
		@Override
		public int compare(Object o1, Object o2) {
			return unswap(o1).toString().compareTo(unswap(o2).toString());
		}
	};

	/**
	 * Create a new property store with default settings.
	 *
	 * @return A new property store with default settings.
	 */
	public static PropertyStore create() {
		PropertyStore f = new PropertyStore();
		BeanContext.loadDefaults(f);
		return f;
	}

	/**
	 * Create a new property store with settings copied from the specified store.
	 *
	 * @param copyFrom The existing store to copy properties from.
	 * @return A new property store with default settings.
	 */
	public static PropertyStore create(PropertyStore copyFrom) {
		return new PropertyStore().copyFrom(copyFrom);
	}


	PropertyStore() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The store to copy properties from.
	 */
	private PropertyStore(PropertyStore copyFrom) {
		copyFrom(copyFrom);
	}

	/**
	 * Copies the properties from the specified store into this store.
	 *
	 * <p>
	 * Properties of type set/list/collection will be appended to the existing properties if they already exist.
	 *
	 * @param ps The store to copy from.
	 * @return This object (for method chaining).
	 */
	public PropertyStore copyFrom(PropertyStore ps) {
		if (ps != null) {
			// TODO - Needs more testing.
			for (Map.Entry<String,PropertyMap> e : ps.properties.entrySet())
				this.properties.put(e.getKey(), new PropertyMap(this.properties.get(e.getKey()), e.getValue()));
			this.classLoader = ps.classLoader;
			this.defaultParser = ps.defaultParser;
		}
		return this;
	}

	/**
	 * Creates a new store with the specified override properties applied to this store.
	 *
	 * @param overrideProperties The properties to apply to the copy of this store.
	 * @return Either this unmodified store, or a new store with override properties applied.
	 */
	public PropertyStore create(Map<String,Object> overrideProperties) {
		return new PropertyStore(this).setProperties(overrideProperties);
	}

	/**
	 * Sets a configuration property value on this object.
	 *
	 * <p>
	 * A typical usage is to set or overwrite configuration values like so...
	 * <p class='bcode'>
	 * 	PropertyStore ps = PropertyStore.<jsm>create</jsm>();
	 * 	ps.setProperty(<jsf>BEAN_sortProperties</jsf>, <jk>true</jk>);
	 * </p>
	 *
	 * <p>
	 * The possible class types of the value depend on the property type:
	 * <table class='styled'>
	 * 	<tr>
	 * 		<th>Property type</th>
	 * 		<th>Example</th>
	 * 		<th>Allowed value type</th>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>Set <l>SIMPLE</l></td>
	 * 		<td><js>"Foo.x"</js></td>
	 * 		<td>Any object type.</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>Set <l>SET/LIST</l></td>
	 * 		<td><js>"Foo.x.set"</js></td>
	 * 		<td>Any collection or array of any objects, or a String containing a JSON array.</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>Add/Remove <l>SET/LIST</l></td>
	 * 		<td><js>"Foo.x.set.add"</js></td>
	 * 		<td>If a collection, adds or removes the entries in the collection.  Otherwise, adds/removes a single
	 * 			entry.</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>Set <l>MAP</l></td>
	 * 		<td><js>"Foo.x.map"</js></td>
	 * 		<td>A map, or a String containing a JSON object.  Entries overwrite existing map.</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>Put <l>MAP</l></td>
	 * 		<td><js>"Foo.x.map.put"</js></td>
	 * 		<td>A map, or a String containing a JSON object.  Entries are added to existing map.</td>
	 * 	</tr>
	 * </table>
	 *
	 * @param name
	 * 	The configuration property name.
	 * 	<br>If name ends with <l>.add</l>, then the specified value is added to the existing property value as an entry
	 * 	in a SET or LIST property.
	 * 	<br>If name ends with <l>.put</l>, then the specified value is added to the existing property value as a
	 * 	key/value pair in a MAP property.
	 * 	<br>If name ends with <l>.remove</l>, then the specified value is removed from the existing property property
	 * 	value in a SET or LIST property.
	 * @param value
	 * 	The new value.
	 * 	If <jk>null</jk>, the property value is deleted.
	 * 	In general, the value type can be anything.
	 * @return This object (for method chaining).
	 */
	public PropertyStore setProperty(String name, Object value) {
		String prefix = prefix(name);

		if (name.endsWith(".add"))
			return addToProperty(name.substring(0, name.lastIndexOf('.')), value);

		if (name.endsWith(".put"))
			return putToProperty(name.substring(0, name.lastIndexOf('.')), value);

		if (name.endsWith(".remove"))
			return removeFromProperty(name.substring(0, name.lastIndexOf('.')), value);

		wl.lock();
		try {
			contexts.clear();
			if (! properties.containsKey(prefix))
				properties.put(prefix, new PropertyMap(prefix));
			properties.get(prefix).set(name, value);
		} finally {
			wl.unlock();
		}
		return this;
	}

	/**
	 * Convenience method for setting multiple properties in one call.
	 *
	 * <p>
	 * This appends to any previous configuration properties set on this store.
	 *
	 * @param newProperties The new properties to set.
	 * @return This object (for method chaining).
	 */
	public PropertyStore setProperties(Map<String,Object> newProperties) {
		if (newProperties == null || newProperties.isEmpty())
			return this;
		wl.lock();
		try {
			contexts.clear();
			for (Map.Entry<String,Object> e : newProperties.entrySet()) {
				String name = e.getKey().toString();
				Object value = e.getValue();
				String prefix = prefix(name);
				if (name.endsWith(".add"))
					addToProperty(name.substring(0, name.lastIndexOf('.')), value);
				else if (name.endsWith(".remove"))
					removeFromProperty(name.substring(0, name.lastIndexOf('.')), value);
				else {
					if (! properties.containsKey(prefix))
						properties.put(prefix, new PropertyMap(prefix));
					properties.get(prefix).set(name, value);
				}
			}

		} finally {
			wl.unlock();
		}
		return this;
	}

	/**
	 * Adds several properties to this store.
	 *
	 * @param properties The properties to add to this store.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("hiding")
	public PropertyStore addProperties(Map<String,Object> properties) {
		if (properties != null)
			for (Map.Entry<String,Object> e : properties.entrySet())
				setProperty(e.getKey(), e.getValue());
		return this;
	}

	/**
	 * Adds a value to a SET property.
	 *
	 * @param name The property name.
	 * @param value The new value to add to the SET property.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a SET property.
	 */
	public PropertyStore addToProperty(String name, Object value) {
		String prefix = prefix(name);
		wl.lock();
		try {
			contexts.clear();
			if (! properties.containsKey(prefix))
				properties.put(prefix, new PropertyMap(prefix));
			properties.get(prefix).addTo(name, value);
		} finally {
			wl.unlock();
		}
		return this;
	}

	/**
	 * Adds or overwrites a value to a MAP property.
	 *
	 * @param name The property name.
	 * @param key The property value map key.
	 * @param value The property value map value.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a MAP property.
	 */
	public PropertyStore putToProperty(String name, Object key, Object value) {
		String prefix = prefix(name);
		wl.lock();
		try {
			contexts.clear();
			if (! properties.containsKey(prefix))
				properties.put(prefix, new PropertyMap(prefix));
			properties.get(prefix).putTo(name, key, value);
		} finally {
			wl.unlock();
		}
		return this;
	}

	/**
	 * Adds or overwrites a value to a MAP property.
	 *
	 * @param name The property value.
	 * @param value The property value map value.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a MAP property.
	 */
	public PropertyStore putToProperty(String name, Object value) {
		String prefix = prefix(name);
		wl.lock();
		try {
			contexts.clear();
			if (! properties.containsKey(prefix))
				properties.put(prefix, new PropertyMap(prefix));
			properties.get(prefix).putTo(name, value);
		} finally {
			wl.unlock();
		}
		return this;
	}

	/**
	 * Removes a value from a SET property.
	 *
	 * @param name The property name.
	 * @param value The property value in the SET property.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a SET property.
	 */
	public PropertyStore removeFromProperty(String name, Object value) {
		String prefix = prefix(name);
		wl.lock();
		try {
			contexts.clear();
			if (properties.containsKey(prefix))
				properties.get(prefix).removeFrom(name, value);
		} finally {
			wl.unlock();
		}
		return this;
	}

	/**
	 * Returns an instance of the specified context initialized with the properties in this store.
	 *
	 * <p>
	 * Multiple calls to this method for the same store class will return the same cached value as long as the
	 * properties on this store are not touched.
	 *
	 * <p>
	 * As soon as any properties are modified on this store, all cached entries are discarded and recreated as needed.
	 *
	 * @param c The context class to instantiate.
	 * @return The context instance.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Context> T getContext(Class<T> c) {
		rl.lock();
		try {
			try {
				if (! contexts.containsKey(c)) {

					// Try to get it from the global cache.
					Integer key = hashCode();
					if (! globalContextCache.containsKey(key))
						globalContextCache.putIfAbsent(key, new ConcurrentHashMap<Class<? extends Context>,Context>());
					ConcurrentHashMap<Class<? extends Context>, Context> cacheForThisConfig = globalContextCache.get(key);

					if (! cacheForThisConfig.containsKey(c))
						cacheForThisConfig.putIfAbsent(c, newInstance(c, c, this));

					contexts.put(c, cacheForThisConfig.get(c));
				}
				return (T)contexts.get(c);
			} catch (Exception e) {
				throw new ConfigException("Could not instantiate context class ''{0}''", className(c)).initCause(e);
			}
		} finally {
			rl.unlock();
		}
	}

	/**
	 * Returns the configuration properties with the specified prefix.
	 *
	 * <p>
	 * For example, if <l>prefix</l> is <js>"BeanContext"</js>, then retrieves all configuration properties that are
	 * prefixed with <js>"BeanContext."</js>.
	 *
	 * @param prefix The prefix of properties to retrieve.
	 * @return The configuration properties with the specified prefix, never <jk>null</jk>.
	 */
	public PropertyMap getPropertyMap(String prefix) {
		rl.lock();
		try {
			PropertyMap m = properties.get(prefix);
			return m == null ? new PropertyMap(prefix) : m;
		} finally {
			rl.unlock();
		}
	}

	/**
	 * Specifies the classloader to use when resolving classes from strings.
	 *
	 * <p>
	 * Can be used for resolving class names when the classes being created are in a different classloader from the
	 * Juneau code.
	 *
	 * <p>
	 * If <jk>null</jk>, the system classloader will be used to resolve classes.
	 *
	 * @param classLoader The new classloader.
	 * @return This object (for method chaining).
	 */
	public PropertyStore setClassLoader(ClassLoader classLoader) {
		this.classLoader = (classLoader == null ? ClassLoader.getSystemClassLoader() : classLoader);
		return this;
	}

	/**
	 * Specifies the parser to use to convert Strings to POJOs.
	 *
	 * <p>
	 * If <jk>null</jk>, {@link JsonParser#DEFAULT} will be used.
	 *
	 * @param defaultParser The new defaultParser.
	 * @return This object (for method chaining).
	 */
	public PropertyStore setDefaultParser(ReaderParser defaultParser) {
		this.defaultParser = defaultParser == null ? JsonParser.DEFAULT : defaultParser;
		return this;
	}

	/**
	 * Returns a property value converted to the specified type.
	 *
	 * @param name The full name of the property (e.g. <js>"BeanContext.sortProperties"</js>)
	 * @param type The class type to convert the property value to.
	 * @param def The default value if the property is not set.
	 * @return The property value.
	 * @throws ConfigException If property has a value that cannot be converted to a boolean.
	 */
	public <T> T getProperty(String name, Class<T> type, T def) {
		rl.lock();
		try {
			PropertyMap pm = getPropertyMap(prefix(name));
			if (pm != null)
				return pm.get(name, type, def);
			String s = System.getProperty(name);
			if ((! isEmpty(s)) && isBeanSessionAvailable())
				return getBeanSession().convertToType(s, type);
			return def;
		} finally {
			rl.unlock();
		}
	}

	/**
	 * Returns a property value either cast to the specified type, or a new instance of the specified type.
	 *
	 * <p>
	 * It's assumed that the current property value is either an instance of that type, or a <code>Class</code> that's
	 * a subclass of the type to be instantiated.
	 *
	 * @param name The full name of the property (e.g. <js>"BeanContext.sortProperties"</js>)
	 * @param type The class type to convert the property value to.
	 * @param def The type to instantiate if the property is not set.
	 * @param args The arguments to pass to the default type constructor.
	 * @return The property either cast to the specified type, or instantiated from a <code>Class</code> object.
	 * @throws ConfigException If property has a value that cannot be converted to a boolean.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getTypedProperty(String name, Class<T> type, Class<? extends T> def, Object...args) {
		rl.lock();
		try {
			Object o = null;
			PropertyMap pm = getPropertyMap(prefix(name));
			if (pm != null)
				o = pm.get(name, type, null);
			if (o == null && def != null)
				o = ClassUtils.newInstance(type, def, args);
			if (o == null)
				return null;
			if (ClassUtils.isParentClass(type, o.getClass()))
				return (T)o;
			throw new FormattedRuntimeException(
				"Invalid object of type {0} found in call to PropertyStore.getTypeProperty({1},{2},{3},...)",
				o.getClass(), name, type, def);
		} finally {
			rl.unlock();
		}
	}

	/**
	 * Returns a property value converted to a {@link LinkedHashMap} with the specified key and value types.
	 *
	 * @param name The full name of the property (e.g. <js>"BeanContext.sortProperties"</js>)
	 * @param keyType The class type of the keys in the map.
	 * @param valType The class type of the values in the map.
	 * @param def The default value if the property is not set.
	 * @return The property value.
	 * @throws ConfigException If property has a value that cannot be converted to a boolean.
	 */
	public <K,V> Map<K,V> getMap(String name, Class<K> keyType, Class<V> valType, Map<K,V> def) {
		rl.lock();
		try {
			PropertyMap pm = getPropertyMap(prefix(name));
			if (pm != null)
				return pm.getMap(name, keyType, valType, def);
			return def;
		} finally {
			rl.unlock();
		}
	}


	//-------------------------------------------------------------------------------------
	// Convenience methods.
	//-------------------------------------------------------------------------------------

	/**
	 * Shortcut for calling <code>getContext(BeanContext.<jk>class</jk>);</code>.
	 *
	 * @return The bean context instance.
	 */
	public BeanContext getBeanContext() {
		return getContext(BeanContext.class);
	}

	/**
	 * Shortcut for calling <code>setProperty(<jsf>BEAN_notBeanClasses</jsf>, <jf>classes</jf>)</code>.
	 *
	 * @param classes The new setting value for the bean context.
	 * @return This object (for method chaining).
	 * @see PropertyStore#setProperty(String, Object)
	 * @see BeanContext#BEAN_notBeanClasses
	 */
	public PropertyStore setNotBeanClasses(Class<?>...classes) {
		setProperty(BEAN_notBeanClasses, classes);
		return this;
	}

	/**
	 * Shortcut for calling <code>addToProperty(<jsf>BEAN_notBeanClasses</jsf>, <jf>classes</jf>)</code>.
	 *
	 * @see PropertyStore#addToProperty(String,Object)
	 * @param classes The new setting value for the bean context.
	 * @return This object (for method chaining).
	 * @see PropertyStore#addToProperty(String, Object)
	 * @see BeanContext#BEAN_notBeanClasses
	 */
	public PropertyStore addNotBeanClasses(Class<?>...classes) {
		addToProperty(BEAN_notBeanClasses, classes);
		return this;
	}

	/**
	 * Shortcut for calling <code>setProperty(<jsf>BEAN_beanFilters</jsf>, <jf>classes</jf>)</code>.
	 *
	 * @param classes The new setting value for the bean context.
	 * @return This object (for method chaining).
	 * @see PropertyStore#setProperty(String, Object)
	 * @see BeanContext#BEAN_beanFilters
	 */
	public PropertyStore setBeanFilters(Class<?>...classes) {
		setProperty(BEAN_beanFilters, classes);
		return this;
	}

	/**
	 * Shortcut for calling <code>addToProperty(<jsf>BEAN_beanFilters</jsf>, <jf>classes</jf>)</code>.
	 *
	 * @param classes The new setting value for the bean context.
	 * @return This object (for method chaining).
	 * @see PropertyStore#addToProperty(String, Object)
	 * @see BeanContext#BEAN_beanFilters
	 */
	public PropertyStore addBeanFilters(Class<?>...classes) {
		addToProperty(BEAN_beanFilters, classes);
		return this;
	}

	/**
	 * Shortcut for calling <code>setProperty(<jsf>BEAN_pojoSwaps</jsf>, <jf>classes</jf>)</code>.
	 *
	 * @param classes The new setting value for the bean context.
	 * @return This object (for method chaining).
	 * @see PropertyStore#setProperty(String, Object)
	 * @see BeanContext#BEAN_pojoSwaps
	 */
	public PropertyStore setPojoSwaps(Class<?>...classes) {
		setProperty(BEAN_pojoSwaps, classes);
		return this;
	}

	/**
	 * Shortcut for calling <code>addToProperty(<jsf>BEAN_pojoSwaps</jsf>, <jf>classes</jf>)</code>.
	 *
	 * @param classes The new setting value for the bean context.
	 * @return This object (for method chaining).
	 * @see PropertyStore#addToProperty(String, Object)
	 * @see BeanContext#BEAN_pojoSwaps
	 */
	public PropertyStore addPojoSwaps(Class<?>...classes) {
		addToProperty(BEAN_pojoSwaps, classes);
		return this;
	}

	/**
	 * Shortcut for calling <code>setProperty(<jsf>BEAN_beanDictionary</jsf>, <jf>classes</jf>)</code>.
	 *
	 * @param classes The new setting value for the bean context.
	 * @return This object (for method chaining).
	 * @see PropertyStore#setProperty(String, Object)
	 * @see BeanContext#BEAN_beanDictionary
	 */
	public PropertyStore setBeanDictionary(Class<?>...classes) {
		addToProperty(BEAN_beanDictionary, classes);
		return this;
	}

	/**
	 * Shortcut for calling <code>addToProperty(<jsf>BEAN_beanDictionary</jsf>, <jf>classes</jf>)</code>.
	 *
	 * @param classes The new setting value for the bean context.
	 * @return This object (for method chaining).
	 * @see PropertyStore#addToProperty(String, Object)
	 * @see BeanContext#BEAN_beanDictionary
	 */
	public PropertyStore addToBeanDictionary(Class<?>...classes) {
		addToProperty(BEAN_beanDictionary, classes);
		return this;
	}

	/**
	 * Shortcut for calling <code>putTo(<jsf>BEAN_implCLasses</jsf>, <jf>interfaceClass</jf>, <jf>implClass</jf>)</code>.
	 *
	 * @param interfaceClass The interface class.
	 * @param implClass The implementation class.
	 * @param <T> The class type of the interface.
	 * @return This object (for method chaining).
	 * @see PropertyStore#putToProperty(String, Object, Object)
	 * @see BeanContext#BEAN_implClasses
	 */
	public <T> PropertyStore addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) {
		putToProperty(BEAN_implClasses, interfaceClass, implClass);
		return this;
	}


	//-------------------------------------------------------------------------------------
	// Object methods.
	//-------------------------------------------------------------------------------------

	@Override /* Object */
	public int hashCode() {
		HashCode c = new HashCode();
		for (PropertyMap m : properties.values())
			c.add(m);
		return c.get();
	}


	//--------------------------------------------------------------------------------
	// Utility classes and methods.
	//--------------------------------------------------------------------------------

	/**
	 * Hashcode generator that treats strings and primitive values the same.
	 * (e.g. <code>123</code> and <js>"123"</js> result in the same hashcode.)
	 */
	private static class NormalizingHashCode extends HashCode {
		@Override /* HashCode */
		protected Object unswap(Object o) {
			return PropertyStore.unswap(o);
		}
	}

	/**
	 * Contains all the properties for a particular property prefix (e.g. <js>'BeanContext'</js>)
	 *
	 * <p>
	 * Instances of this map are immutable from outside this class.
	 *
	 * <p>
	 * The {@link PropertyMap#hashCode()} and {@link PropertyMap#equals(Object)} methods can be used to compare with
	 * other property maps.
	 */
	@SuppressWarnings("hiding")
	public class PropertyMap {

		private final Map<String,Property> map = new ConcurrentSkipListMap<String,Property>();
		private volatile int hashCode = 0;
		private final ReadWriteLock lock = new ReentrantReadWriteLock();
		private final Lock rl = lock.readLock(), wl = lock.writeLock();
		private final String prefix;

		private PropertyMap(String prefix) {
			this.prefix = prefix;
			prefix = prefix + '.';
			Properties p = System.getProperties();
			for (Map.Entry<Object,Object> e : p.entrySet())
				if (e.getKey().toString().startsWith(prefix))
					set(e.getKey().toString(), e.getValue());
		}

		/**
		 * Copy constructor.
		 */
		private PropertyMap(PropertyMap orig, PropertyMap apply) {
			this.prefix = apply.prefix;
			if (orig != null)
				for (Map.Entry<String,Property> e : orig.map.entrySet())
					this.map.put(e.getKey(), Property.create(e.getValue().name, e.getValue().value()));
			for (Map.Entry<String,Property> e : apply.map.entrySet()) {
				Property prev = this.map.get(e.getKey());
				if (prev == null || "SIMPLE".equals(prev.type))
					this.map.put(e.getKey(), Property.create(e.getValue().name, e.getValue().value()));
				else {
					if ("SET".equals(prev.type) || "LIST".equals(prev.type))
						prev.add(e.getValue().value());
					else if ("MAP".equals(prev.type))
						prev.put(e.getValue().value());
				}
			}
		}

		/**
		 * Returns the specified property as the specified class type.
		 *
		 * @param name The property name.
		 * @param type The type of object to convert the value to.
		 * @param def The default value if the specified property is not set.
		 * @return The property value.
		 */
		public <T> T get(String name, Class<T> type, T def) {
			rl.lock();
			try {
				Property p = map.get(name);
				if (p == null || type == null)
					return def;
				try {
					if (! isBeanSessionAvailable())
						return def;
					return getBeanSession().convertToType(p.value, type);
				} catch (InvalidDataConversionException e) {
					throw new ConfigException("Could not retrieve property store property ''{0}''.  {1}", p.name,
						e.getMessage());
				}
			} finally {
				rl.unlock();
			}
		}

		/**
		 * Returns the specified property as a map with the specified key and value types.
		 *
		 * <p>
		 * The map returned is an instance of {@link LinkedHashMap}.
		 *
		 * @param name The property name.
		 * @param keyType The class type of the keys of the map.
		 * @param valueType The class type of the values of the map.
		 * @param def The default value if the specified property is not set.
		 * @return The property value.
		 */
		@SuppressWarnings("unchecked")
		public <K,V> Map<K,V> getMap(String name, Class<K> keyType, Class<V> valueType, Map<K,V> def) {
			rl.lock();
			try {
				Property p = map.get(name);
				if (p == null || keyType == null || valueType == null)
					return def;
				try {
					if (isBeanSessionAvailable()) {
						BeanSession session = getBeanSession();
						return (Map<K,V>)session.convertToType(p.value,
							session.getClassMeta(LinkedHashMap.class, keyType, valueType));
					}
					return def;
				} catch (InvalidDataConversionException e) {
					throw new ConfigException("Could not retrieve property store property ''{0}''.  {1}", p.name,
						e.getMessage());
				}
			} finally {
				rl.unlock();
			}
		}

		/**
		 * Convenience method for returning all values in this property map as a simple map.
		 *
		 * <p>
		 * Primarily useful for debugging.
		 *
		 * @return A new {@link LinkedHashMap} with all values in this property map.
		 */
		public Map<String,Object> asMap() {
			rl.lock();
			try {
				Map<String,Object> m = new LinkedHashMap<String,Object>();
				for (Property p : map.values())
					m.put(p.name, p.value);
				return m;
			} finally {
				rl.unlock();
			}
		}

		private void set(String name, Object value) {
			wl.lock();
			hashCode = 0;
			try {
				if (value == null)
					map.remove(name);
				else
					map.put(name, Property.create(name, value));
			} finally {
				wl.unlock();
			}
		}

		private void addTo(String name, Object value) {
			wl.lock();
			hashCode = 0;
			try {
				if (! map.containsKey(name))
					map.put(name, Property.create(name, Collections.emptyList()));
				map.get(name).add(value);
			} finally {
				wl.unlock();
			}
		}

		private void putTo(String name, Object key, Object value) {
			wl.lock();
			hashCode = 0;
			try {
				if (! map.containsKey(name))
					map.put(name, Property.create(name, Collections.emptyMap()));
				map.get(name).put(key, value);
			} finally {
				wl.unlock();
			}
		}

		private void putTo(String name, Object value) {
			wl.lock();
			hashCode = 0;
			try {
				if (! map.containsKey(name))
					map.put(name, Property.create(name, Collections.emptyMap()));
				map.get(name).put(value);
			} finally {
				wl.unlock();
			}
		}

		private void removeFrom(String name, Object value) {
			wl.lock();
			hashCode = 0;
			try {
				if (map.containsKey(name))
					map.get(name).remove(value);
			} finally {
				wl.unlock();
			}
		}

		@Override
		public int hashCode() {
			rl.lock();
			try {
				if (hashCode == 0) {
					HashCode c = new HashCode().add(prefix);
					for (Property p : map.values())
						c.add(p);
					this.hashCode = c.get();
				}
				return hashCode;
			} finally {
				rl.unlock();
			}
		}

		@Override
		public boolean equals(Object o) {
			rl.lock();
			try {
				if (o instanceof PropertyMap) {
					PropertyMap m = (PropertyMap)o;
					if (m.hashCode() != hashCode())
						return false;
					return this.map.equals(m.map);
				}
				return false;
			} finally {
				rl.unlock();
			}
		}

		@Override
		public String toString() {
			return "PropertyMap(id="+System.identityHashCode(this)+")";
		}
	}

	private abstract static class Property {
		private final String name, type;
		private final Object value;

		private static Property create(String name, Object value) {
			if (name.endsWith(".set"))
				return new SetProperty(name, value);
			else if (name.endsWith(".list"))
				return new ListProperty(name, value);
			else if (name.endsWith(".map"))
				return new MapProperty(name, value);
			return new SimpleProperty(name, value);
		}

		Property(String name, String type, Object value) {
			this.name = name;
			this.type = type;
			this.value = value;
		}

		void add(Object val) {
			throw new ConfigException("Cannot add value {0} ({1}) to property ''{2}'' ({3}).",
				JsonSerializer.DEFAULT_LAX.toString(val), getReadableClassNameForObject(val), name, type);
		}

		void remove(Object val) {
			throw new ConfigException("Cannot remove value {0} ({1}) from property ''{2}'' ({3}).",
				JsonSerializer.DEFAULT_LAX.toString(val), getReadableClassNameForObject(val), name, type);
		}

		void put(Object val) {
			throw new ConfigException("Cannot put value {0} ({1}) to property ''{2}'' ({3}).",
				JsonSerializer.DEFAULT_LAX.toString(val), getReadableClassNameForObject(val), name, type);
		}

		void put(Object key, Object val) {
			throw new ConfigException("Cannot put value {0}({1})->{2}({3}) to property ''{4}'' ({5}).",
				JsonSerializer.DEFAULT_LAX.toString(key), getReadableClassNameForObject(key),
				JsonSerializer.DEFAULT_LAX.toString(val), getReadableClassNameForObject(val), name, type);
		}

		protected Object value() {
			return value;
		}

		@Override /* Object */
		public int hashCode() {
			HashCode c = new NormalizingHashCode().add(name);
			if (value instanceof Map) {
				for (Map.Entry<?,?> e : ((Map<?,?>)value).entrySet())
					c.add(e.getKey()).add(e.getValue());
			} else if (value instanceof Collection) {
				for (Object o : (Collection<?>)value)
					c.add(o);
			} else {
				c.add(value);
			}
			return c.get();
		}

		@Override
		public String toString() {
			return "Property(name="+name+",type="+type+")";
		}
	}

	private static class SimpleProperty extends Property {

		SimpleProperty(String name, Object value) {
			super(name, "SIMPLE", value);
		}
	}

	@SuppressWarnings({"unchecked"})
	private static class SetProperty extends Property {
		private final Set<Object> value;

		private SetProperty(String name, Object value) {
			super(name, "SET", new ConcurrentSkipListSet<Object>(PROPERTY_COMPARATOR));
			this.value = (Set<Object>)value();
			add(value);
		}

		@Override
		void add(Object val) {
			if (val.getClass().isArray())
				for (int i = 0; i < Array.getLength(val); i++)
					add(Array.get(val, i));
			else if (val instanceof Collection)
				for (Object o : (Collection<Object>)val)
					add(o);
			else {
				if (val instanceof String) {
					String s = val.toString();
					if (s.startsWith("[") && s.endsWith("]")) {
						try {
							add(new ObjectList(s));
							return;
						} catch (Exception e) {}
					}
				}
				for (Object o : value)
					if (same(val, o))
						return;
				value.add(val);
			}
		}

		@Override
		void remove(Object val) {
			if (val.getClass().isArray())
				for (int i = 0; i < Array.getLength(val); i++)
					remove(Array.get(val, i));
			else if (val instanceof Collection)
				for (Object o : (Collection<Object>)val)
					remove(o);
			else {
				if (val instanceof String) {
					String s = val.toString();
					if (s.startsWith("[") && s.endsWith("]")) {
						try {
							remove(new ObjectList(s));
							return;
						} catch (Exception e) {}
					}
				}
				for (Iterator<Object> i = value.iterator(); i.hasNext();)
					if (same(i.next(), val))
						i.remove();
			}
		}
	}

	@SuppressWarnings({"unchecked"})
	private static class ListProperty extends Property {
		private final LinkedList<Object> value;

		private ListProperty(String name, Object value) {
			super(name, "LIST", new LinkedList<Object>());
			this.value = (LinkedList<Object>)value();
			add(value);
		}

		@Override
		void add(Object val) {
			if (val.getClass().isArray()) {
				for (int i = Array.getLength(val) - 1; i >= 0; i--)
					add(Array.get(val, i));
			} else if (val instanceof List) {
				List<Object> l = (List<Object>)val;
				for (ListIterator<Object> i = l.listIterator(l.size()); i.hasPrevious();)
					add(i.previous());
			} else if (val instanceof Collection) {
				List<Object> l = new ArrayList<Object>((Collection<Object>)val);
				for (ListIterator<Object> i = l.listIterator(l.size()); i.hasPrevious();)
					add(i.previous());
			} else {
				String s = val.toString();
				if (s.startsWith("[") && s.endsWith("]")) {
					try {
						add(new ObjectList(s));
						return;
					} catch (Exception e) {}
				}
				for (Iterator<Object> i = value.iterator(); i.hasNext(); )
					if (same(val, i.next()))
						i.remove();
				value.addFirst(val);
			}
		}

		@Override
		void remove(Object val) {
			if (val.getClass().isArray())
				for (int i = 0; i < Array.getLength(val); i++)
					remove(Array.get(val, i));
			else if (val instanceof Collection)
				for (Object o : (Collection<Object>)val)
					remove(o);
			else {
				String s = val.toString();
				if (s.startsWith("[") && s.endsWith("]")) {
					try {
						remove(new ObjectList(s));
						return;
					} catch (Exception e) {}
				}
				for (Iterator<Object> i = value.iterator(); i.hasNext();)
					if (same(i.next(), val))
						i.remove();
			}
		}
	}

	@SuppressWarnings({"unchecked","rawtypes"})
	private static class MapProperty extends Property {
		final Map<Object,Object> value;

		MapProperty(String name, Object value) {
			// ConcurrentSkipListMap doesn't support Map.Entry.remove(), so use TreeMap instead.
			super(name, "MAP", Collections.synchronizedMap(new TreeMap<Object,Object>(PROPERTY_COMPARATOR)));
			this.value = (Map<Object,Object>)value();
			put(value);
		}

		@Override
		void put(Object val) {
			try {
				if (isBeanSessionAvailable() && ! (val instanceof Map))
					val = getBeanSession().convertToType(val, Map.class);
				if (val instanceof Map) {
					Map m = (Map)val;
					for (Map.Entry e : (Set<Map.Entry>)m.entrySet())
						put(e.getKey(), e.getValue());
					return;
				}
			} catch (Exception e) {}
			super.put(val);
		}

		@Override
		void put(Object key, Object val) {
			// ConcurrentSkipListMap doesn't support Map.Entry.remove().
			for (Map.Entry<Object,Object> e : value.entrySet()) {
				if (same(e.getKey(), key)) {
					e.setValue(val);
					return;
				}
			}
			value.put(key, val);
		}
	}

	/**
	 * Converts an object to a normalized form for comparison purposes.
	 *
	 * @param o The object to normalize.
	 * @return The normalized object.
	 */
	private static final Object unswap(Object o) {
		if (o instanceof Class)
			return ((Class<?>)o).getName();
		if (o instanceof Number || o instanceof Boolean)
			return o.toString();
		return o;
	}

	private static BeanSession getBeanSession() {
		if (beanSession == null && BeanContext.DEFAULT != null)
			beanSession = BeanContext.DEFAULT.createSession();
		return beanSession;
	}

	/**
	 * Returns true if a bean session is available.
	 *
	 * <p>
	 * Note that a bean session will not be available when constructing the BeanContext.DEFAULT context.
	 * (it's a chicken-and-egg thing).
	 */
	private static boolean isBeanSessionAvailable() {
		return getBeanSession() != null;
	}

	/*
	 * Compares two objects for "string"-equality.
	 * Basically mean both objects are equal if they're the same when converted to strings.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static boolean same(Object o1, Object o2) {
		if (o1 == o2)
			return true;
		if (o1 instanceof Map) {
			if (o2 instanceof Map) {
				Map m1 = (Map)o1, m2 = (Map)o2;
				if (m1.size() == m2.size()) {
					Set<Map.Entry> s1 = m1.entrySet(), s2 = m2.entrySet();
					for (Iterator<Map.Entry> i1 = s1.iterator(), i2 = s2.iterator(); i1.hasNext();) {
						Map.Entry e1 = i1.next(), e2 = i2.next();
						if (! same(e1.getKey(), e2.getKey()))
							return false;
						if (! same(e1.getValue(), e2.getValue()))
							return false;
					}
					return true;
				}
			}
			return false;
		} else if (o1 instanceof Collection) {
			if (o2 instanceof Collection) {
				Collection c1 = (Collection)o1, c2 = (Collection)o2;
				if (c1.size() == c2.size()) {
					for (Iterator i1 = c1.iterator(), i2 = c2.iterator(); i1.hasNext();) {
						if (! same(i1.next(), i2.next()))
							return false;
					}
					return true;
				}
			}
			return false;
		} else {
			return unswap(o1).equals(unswap(o2));
		}
	}

	private static String prefix(String name) {
		if (name == null)
			throw new ConfigException("Invalid property name specified: 'null'");
		if (name.indexOf('.') == -1)
			return "";
		return name.substring(0, name.indexOf('.'));
	}

	private static String className(Object o) {
		if (o == null)
			return null;
		if (o instanceof Class)
			return getReadableClassName((Class<?>)o);
		return getReadableClassName(o.getClass());
	}

	@Override /* Object */
	public String toString() {
		rl.lock();
		try {
			ObjectMap m = new ObjectMap();
			m.put("id", System.identityHashCode(this));
			m.put("hashCode", hashCode());
			m.put("properties.id", System.identityHashCode(properties));
			m.put("contexts.id", System.identityHashCode(contexts));
			m.put("properties", properties);
			m.put("contexts", contexts);
			return m.toString();
		} finally {
			rl.unlock();
		}
	}
}
