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

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;

/**
 * A reusable stateless thread-safe read-only configuration, typically used for creating one-time use {@link Session}
 * objects.
 *
 * <p>
 * Contexts are created through the {@link ContextBuilder#build()} method.
 *
 * <p>
 * Subclasses MUST implement a constructor method that takes in a {@link PropertyStore} parameter.
 * Besides that restriction, a context object can do anything you desire.
 * However, it MUST be thread-safe and all fields should be declared final to prevent modification.
 * It should NOT be used for storing temporary or state information.
 *
 * @see PropertyStore
 */
public abstract class Context {

	private final PropertyStore propertyStore;
	private final int hashCode;

	/**
	 * Constructor for this class.
	 *
	 * <p>
	 * Subclasses MUST implement the same public constructor.
	 *
	 * @param ps The read-only configuration for this context object.
	 */
	public Context(PropertyStore ps) {
		this.propertyStore = ps == null ? PropertyStore.DEFAULT : ps;
		this.hashCode = new HashCode().add(getClass().getName()).add(ps).get();
	}

	/**
	 * Returns the raw property value with the specified name.
	 * 
	 * @param key The property name.
	 * @return The property value, or <jk>null</jk> if it doesn't exist.
	 */
	public Object getProperty(String key) {
		return propertyStore.getProperty(key);
	}

	/**
	 * Returns the property value with the specified name.
	 * 
	 * @param key The property name.
	 * @param c The class to cast or convert the value to.
	 * @param def The default value.
	 * @return The property value, or the default value if it doesn't exist.
	 */
	public final <T> T getProperty(String key, Class<T> c, T def) {
		return propertyStore.getProperty(key, c, def);
	}

	/**
	 * Returns the class property with the specified name.
	 * 
	 * @param key The property name.
	 * @param type The class type of the property.
	 * @param def The default value.
	 * @return The property value, or the default value if it doesn't exist.
	 */
	public final <T> Class<? extends T> getClassProperty(String key, Class<T> type, Class<? extends T> def) {
		return propertyStore.getClassProperty(key, type, def);
	}
	
	/**
	 * Returns the array property value with the specified name.
	 * 
	 * @param key The property name.
	 * @param eType The class type of the elements in the property.
	 * @return The property value, or the default value if it doesn't exist.
	 */
	public final <T> T[] getArrayProperty(String key, Class<T> eType) {
		return propertyStore.getArrayProperty(key, eType);
	}
	
	/**
	 * Returns the array property value with the specified name.
	 * 
	 * @param key The property name.
	 * @param eType The class type of the elements in the property.
	 * @param def The default value.
	 * @return The property value, or the default value if it doesn't exist.
	 */
	public final <T> T[] getArrayProperty(String key, Class<T> eType, T[] def) {
		return propertyStore.getArrayProperty(key, eType, def);
	}
	
	/**
	 * Returns the class array property with the specified name.
	 * 
	 * @param key The property name.
	 * @return The property value, or an empty array if it doesn't exist.
	 */
	public final Class<?>[] getClassArrayProperty(String key) {
		return propertyStore.getClassArrayProperty(key);
	}

	/**
	 * Returns the class array property with the specified name.
	 * 
	 * @param key The property name.
	 * @param def The default value.
	 * @return The property value, or an empty array if it doesn't exist.
	 */
	public final Class<?>[] getClassArrayProperty(String key, Class<?>[] def) {
		return propertyStore.getClassArrayProperty(key, def);
	}

	/**
	 * Returns the class array property with the specified name.
	 * 
	 * @param key The property name.
	 * @param eType The class type of the elements in the property.
	 * @return The property value, or an empty array if it doesn't exist.
	 */
	public final <T> Class<T>[] getClassArrayProperty(String key, Class<T> eType) {
		return propertyStore.getClassArrayProperty(key, eType);
	}

	/**
	 * Returns the set property with the specified name.
	 * 
	 * @param key The property name.
	 * @param eType The class type of the elements in the property.
	 * @return The property value as an unmodifiable <code>LinkedHashSet</code>, or an empty set if it doesn't exist.
	 */
	public final <T> Set<T> getSetProperty(String key, Class<T> eType) {
		return propertyStore.getSetProperty(key, eType);
	}
	
	/**
	 * Returns the class set property with the specified name.
	 * 
	 * @param key The property name.
	 * @return The property value as an unmodifiable <code>LinkedHashSet</code>, or an empty set if it doesn't exist.
	 */
	public final Set<Class<?>> getClassSetProperty(String key) {
		return propertyStore.getClassSetProperty(key);
	}

	/**
	 * Returns the class set property with the specified name.
	 * 
	 * @param key The property name.
	 * @param eType The class type of the elements in the property.
	 * @return The property value as an unmodifiable <code>LinkedHashSet</code>, or an empty set if it doesn't exist.
	 */
	public final <T> Set<Class<T>> getClassSetProperty(String key, Class<T> eType) {
		return propertyStore.getClassSetProperty(key, eType);
	}

	/**
	 * Returns the list property with the specified name.
	 * 
	 * @param key The property name.
	 * @param eType The class type of the elements in the property.
	 * @return The property value as an unmodifiable <code>ArrayList</code>, or an empty list if it doesn't exist.
	 */
	public final <T> List<T> getListProperty(String key, Class<T> eType) {
		return propertyStore.getListProperty(key, eType);
	}
	
	/**
	 * Returns the class list property with the specified name.
	 * 
	 * @param key The property name.
	 * @return The property value as an unmodifiable <code>ArrayList</code>, or an empty list if it doesn't exist.
	 */
	public final List<Class<?>> getClassListProperty(String key) {
		return propertyStore.getClassListProperty(key);
	}

	/**
	 * Returns the class list property with the specified name.
	 * 
	 * @param key The property name.
	 * @param eType The class type of the elements in the property.
	 * @return The property value as an unmodifiable <code>ArrayList</code>, or an empty list if it doesn't exist.
	 */
	public final <T> List<Class<T>> getClassListProperty(String key, Class<T> eType) {
		return propertyStore.getClassListProperty(key, eType);
	}

	/**
	 * Returns the map property with the specified name.
	 * 
	 * @param key The property name.
	 * @param eType The class type of the elements in the property.
	 * @return The property value as an unmodifiable <code>LinkedHashMap</code>, or an empty map if it doesn't exist.
	 */
	public final <T> Map<String,T> getMapProperty(String key, Class<T> eType) {
		return propertyStore.getMapProperty(key, eType);
	}
	
	/**
	 * Returns the class map property with the specified name.
	 * 
	 * @param key The property name.
	 * @return The property value as an unmodifiable <code>LinkedHashMap</code>, or an empty map if it doesn't exist.
	 */
	public final Map<String,Class<?>> getClassMapProperty(String key) {
		return propertyStore.getClassMapProperty(key);
	}

	/**
	 * Returns the class map property with the specified name.
	 * 
	 * @param key The property name.
	 * @param eType The class type of the elements in the property.
	 * @return The property value as an unmodifiable <code>LinkedHashMap</code>, or an empty map if it doesn't exist.
	 */
	public final <T> Map<String,Class<T>> getClassMapProperty(String key, Class<T> eType) {
		return propertyStore.getClassMapProperty(key, eType);
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
	 * 	<br>Can either be an instance of <code>T</code>, or a <code>Class&lt;? <jk>extends</jk> T&gt;</code>, or <jk>null</jk>.
	 * @return A new property instance.
	 */
	public <T> T getInstanceProperty(String key, Class<T> type, Object def) {
		return propertyStore.getInstanceProperty(key, type, def);
	}

	/**
	 * Returns an instance of the specified class, string, or object property.
	 * 
	 * @param key The property name.
	 * @param type The class type of the property.
	 * @param def 
	 * 	The default value if the property doesn't exist.
	 * 	<br>Can either be an instance of <code>T</code>, or a <code>Class&lt;? <jk>extends</jk> T&gt;</code>.
	 * @param allowNoArgs 
	 * 	Look for no-arg constructors when instantiating a class.
	 * @param args 
	 * 	Arguments to pass to the constructor.
	 * 	Constructors matching the arguments are always used before no-arg constructors.
	 * @return A new property instance.
	 */
	public <T> T getInstanceProperty(String key, Class<T> type, Object def, boolean allowNoArgs, Object...args) {
		return propertyStore.getInstanceProperty(key, type, def, allowNoArgs, args);
	}

	/**
	 * Returns an instance of the specified class, string, or object property.
	 * 
	 * @param key The property name.
	 * @param outer The outer object if the class we're instantiating is an inner class.
	 * @param type The class type of the property.
	 * @param def 
	 * 	The default value if the property doesn't exist.
	 * 	<br>Can either be an instance of <code>T</code>, or a <code>Class&lt;? <jk>extends</jk> T&gt;</code>.
	 * @param allowNoArgs 
	 * 	Look for no-arg constructors when instantiating a class.
	 * @param args 
	 * 	Arguments to pass to the constructor.
	 * 	Constructors matching the arguments are always used before no-arg constructors.
	 * @return A new property instance.
	 */
	public <T> T getInstanceProperty(String key, Object outer, Class<T> type, Object def, boolean allowNoArgs, Object...args) {
		return propertyStore.getInstanceProperty(key, outer, type, def, allowNoArgs, args);
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
		return propertyStore.getInstanceArrayProperty(key, type, def);
	}

	/**
	 * Returns the specified property as an array of instantiated objects.
	 * 
	 * @param key The property name.
	 * @param type The class type of the property.
	 * @param def The default object to return if the property doesn't exist.
	 * @param allowNoArgs 
	 * 	Look for no-arg constructors when instantiating a class.
	 * @param args 
	 * 	Arguments to pass to the constructor.
	 * 	Constructors matching the arguments are always used before no-arg constructors.
	 * @return A new property instance.
	 */
	public <T> T[] getInstanceArrayProperty(String key, Class<T> type, T[] def, boolean allowNoArgs, Object...args) {
		return propertyStore.getInstanceArrayProperty(key, type, def, allowNoArgs, args);
	}

	/**
	 * Returns the specified property as an array of instantiated objects.
	 * 
	 * @param key The property name.
	 * @param outer The outer object if the class we're instantiating is an inner class.
	 * @param type The class type of the property.
	 * @param def The default object to return if the property doesn't exist.
	 * @param allowNoArgs 
	 * 	Look for no-arg constructors when instantiating a class.
	 * @param args 
	 * 	Arguments to pass to the constructor.
	 * 	Constructors matching the arguments are always used before no-arg constructors.
	 * @return A new property instance.
	 */
	public <T> T[] getInstanceArrayProperty(String key, Object outer, Class<T> type, T[] def, boolean allowNoArgs, Object...args) {
		return propertyStore.getInstanceArrayProperty(key, outer, type, def, allowNoArgs, args);
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
		return propertyStore.getPropertyKeys(group);
	}

	/**
	 * Returns the property store associated with this context.
	 *
	 * @return The property store associated with this context.
	 */
	@BeanIgnore
	public final PropertyStore getPropertyStore() {
		return propertyStore;
	}
	
	/**
	 * Creates a builder from this context object.
	 * 
	 * <p>
	 * Builders are used to define new contexts (e.g. serializers, parsers) based on existing configurations.
	 * 
	 * @return A new ContextBuilder object.
	 */
	public ContextBuilder builder() {
		return null;
	}

	/**
	 * Create a new bean session based on the properties defined on this context.
	 *
	 * <p>
	 * Use this method for creating sessions if you don't need to override any
	 * properties or locale/timezone currently set on this context.
	 *
	 * @return A new session object.
	 */
	public Session createSession() {
		return createSession(createDefaultSessionArgs());
	}
	
	/**
	 * Create a new session based on the properties defined on this context combined with the specified
	 * runtime args.
	 *
	 * <p>
	 * Use this method for creating sessions if you don't need to override any
	 * properties or locale/timezone currently set on this context.
	 * 
	 * @param args 
	 * 	The session arguments.
	 * @return A new session object.
	 */
	public abstract Session createSession(SessionArgs args);

	/**
	 * Defines default session arguments used when calling the {@link #createSession()} method.
	 * 
	 * @return A SessionArgs object, possibly a read-only reusable instance.
	 */
	public abstract SessionArgs createDefaultSessionArgs();

	/**
	 * Returns the properties defined on this bean context as a simple map for debugging purposes.
	 *
	 * @return A new map containing the properties defined on this context.
	 */
	@Overrideable
	public ObjectMap asMap() {
		return new ObjectMap();
	}
	
	@Override /* Object */
	public final int hashCode() {
		return hashCode;
	}
	
	@Override /* Object */
	public final boolean equals(Object o) {
		// Context objects are considered equal if they're the same class and have the same set of properties.
		if (o == null)
			return false;
		if (o.getClass() != this.getClass()) 
			return false;
		Context c = (Context)o;
		return (c.propertyStore.equals(propertyStore));
	}

	@Override /* Object */
	public final String toString() {
		try {
			return asMap().toString(JsonSerializer.DEFAULT_LAX_READABLE);
		} catch (SerializeException e) {
			return e.getLocalizedMessage();
		}
	}
}
