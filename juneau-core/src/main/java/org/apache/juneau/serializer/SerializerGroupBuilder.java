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
package org.apache.juneau.serializer;

import static org.apache.juneau.BeanContext.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.serializer.SerializerContext.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;

/**
 * Builder class for creating instances of {@link SerializerGroup}.
 */
public class SerializerGroupBuilder {

	private final List<Object> serializers;
	private final PropertyStore propertyStore;

	/**
	 * Create an empty serializer group builder.
	 */
	public SerializerGroupBuilder() {
		this.serializers = new ArrayList<Object>();
		this.propertyStore = PropertyStore.create();
	}

	/**
	 * Create an empty serializer group using the specified property store for settings.
	 * <p>
	 * Note:  Modifying the specified property store externally will also modify it here.
	 *
	 * @param propertyStore The property store containing all settings common to all serializers in this group.
	 */
	public SerializerGroupBuilder(PropertyStore propertyStore) {
		this.serializers = new ArrayList<Object>();
		this.propertyStore = propertyStore;
	}

	/**
	 * Clone an existing serializer group builder.
	 * @param copyFrom The serializer group that we're copying settings and serializers from.
	 */
	public SerializerGroupBuilder(SerializerGroup copyFrom) {
		this.serializers = new ArrayList<Object>();
		addReverse(serializers, copyFrom.getSerializers());
		this.propertyStore = copyFrom.createPropertyStore();
	}

	/**
	 * Registers the specified serializers with this group.
	 *
	 * @param s The serializers to append to this group.
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder append(Class<?>...s) {
		addReverse(serializers, s);
		return this;
	}

	/**
	 * Registers the specified serializers with this group.
	 *
	 * @param s The serializers to append to this group.
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder append(Serializer...s) {
		addReverse(serializers, s);
		return this;
	}

	/**
	 * Registers the specified serializers with this group.
	 *
	 * @param s The serializers to append to this group.
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder append(List<Serializer> s) {
		addReverse(serializers, s);
		return this;
	}

	/**
	 * Creates a new {@link SerializerGroup} object using a snapshot of the settings defined in this builder.
	 * <p>
	 * This method can be called multiple times to produce multiple serializer groups.
	 *
	 * @return A new {@link SerializerGroup} object.
	 */
	public SerializerGroup build() {
		List<Serializer> l = new ArrayList<Serializer>();
		for (Object s : serializers) {
			Class<?> c = null;
			PropertyStore ps = propertyStore;
			if (s instanceof Class) {
				c = (Class<?>)s;
			} else {
				// Note that if we added a serializer instance, we want a new instance with this builder's properties
				// on top of the previous serializer's properties.
				Serializer s2 = (Serializer)s;
				ps = s2.createPropertyStore().copyFrom(propertyStore);
				c = s2.getClass();
			}
			l.add(newInstance(Serializer.class, c, ps));
		}
		Collections.reverse(l);
		return new SerializerGroup(propertyStore, l.toArray(new Serializer[l.size()]));
	}


	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	/**
	 * Sets a property on all serializers in this group.
	 *
	 * @param name The property name.
	 * @param value The property value.
	 * @return This object (for method chaining).
	 * @see PropertyStore#setProperty(String, Object)
	 */
	public SerializerGroupBuilder property(String name, Object value) {
		propertyStore.setProperty(name, value);
		return this;
	}

	/**
	 * Sets a set of properties on all serializers in this group.
	 *
	 * @param properties The properties to set on this class.
	 * @return This object (for method chaining).
	 * @see PropertyStore#setProperties(java.util.Map)
	 */
	public SerializerGroupBuilder properties(ObjectMap properties) {
		propertyStore.setProperties(properties);
		return this;
	}

	/**
	 * Adds a value to a SET property on all serializers in this group.
	 *
	 * @param name The property name.
	 * @param value The new value to add to the SET property.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a SET property.
	 */
	public SerializerGroupBuilder addToProperty(String name, Object value) {
		propertyStore.addToProperty(name, value);
		return this;
	}

	/**
	 * Adds or overwrites a value to a MAP property on all serializers in this group.
	 *
	 * @param name The property name.
	 * @param key The property value map key.
	 * @param value The property value map value.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a MAP property.
	 */
	public SerializerGroupBuilder putToProperty(String name, Object key, Object value) {
		propertyStore.putToProperty(name, key, value);
		return this;
	}

	/**
	 * Adds or overwrites a value to a MAP property on all serializers in this group.
	 *
	 * @param name The property value.
	 * @param value The property value map value.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a MAP property.
	 */
	public SerializerGroupBuilder putToProperty(String name, Object value) {
		propertyStore.putToProperty(name, value);
		return this;
	}

	/**
	 * Removes a value from a SET property on all serializers in this group.
	 *
	 * @param name The property name.
	 * @param value The property value in the SET property.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a SET property.
	 */
	public SerializerGroupBuilder removeFromProperty(String name, Object value) {
		propertyStore.removeFromProperty(name, value);
		return this;
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_maxDepth} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_maxDepth
	 */
	public SerializerGroupBuilder maxDepth(int value) {
		return property(SERIALIZER_maxDepth, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_initialDepth} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_initialDepth
	 */
	public SerializerGroupBuilder initialDepth(int value) {
		return property(SERIALIZER_initialDepth, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_detectRecursions} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_detectRecursions
	 */
	public SerializerGroupBuilder detectRecursions(boolean value) {
		return property(SERIALIZER_detectRecursions, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_ignoreRecursions} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_ignoreRecursions
	 */
	public SerializerGroupBuilder ignoreRecursions(boolean value) {
		return property(SERIALIZER_ignoreRecursions, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_useWhitespace} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_useWhitespace
	 */
	public SerializerGroupBuilder useWhitespace(boolean value) {
		return property(SERIALIZER_useWhitespace, value);
	}

	/**
	 * Shortcut for calling <code>useWhitespace(<jk>true</jk>)</code>.
	 *
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder ws() {
		return useWhitespace(true);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_addBeanTypeProperties} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_addBeanTypeProperties
	 */
	public SerializerGroupBuilder addBeanTypeProperties(boolean value) {
		return property(SERIALIZER_addBeanTypeProperties, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_quoteChar} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_quoteChar
	 */
	public SerializerGroupBuilder quoteChar(char value) {
		return property(SERIALIZER_quoteChar, value);
	}

	/**
	 * Shortcut for calling <code>quoteChar(<js>'\''</js>)</code>.
	 *
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder sq() {
		return quoteChar('\'');
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_trimNullProperties} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_trimNullProperties
	 */
	public SerializerGroupBuilder trimNullProperties(boolean value) {
		return property(SERIALIZER_trimNullProperties, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_trimEmptyCollections} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_trimEmptyCollections
	 */
	public SerializerGroupBuilder trimEmptyCollections(boolean value) {
		return property(SERIALIZER_trimEmptyCollections, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_trimEmptyMaps} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_trimEmptyMaps
	 */
	public SerializerGroupBuilder trimEmptyMaps(boolean value) {
		return property(SERIALIZER_trimEmptyMaps, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_trimStrings} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_trimStrings
	 */
	public SerializerGroupBuilder trimStrings(boolean value) {
		return property(SERIALIZER_trimStrings, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_uriContext} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_uriContext
	 */
	public SerializerGroupBuilder uriContext(UriContext value) {
		return property(SERIALIZER_uriContext, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_uriResolution} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_uriResolution
	 */
	public SerializerGroupBuilder uriResolution(UriResolution value) {
		return property(SERIALIZER_uriResolution, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_uriRelativity} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_uriRelativity
	 */
	public SerializerGroupBuilder uriRelativity(UriRelativity value) {
		return property(SERIALIZER_uriRelativity, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_sortCollections} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_sortCollections
	 */
	public SerializerGroupBuilder sortCollections(boolean value) {
		return property(SERIALIZER_sortCollections, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_sortMaps} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_sortMaps
	 */
	public SerializerGroupBuilder sortMaps(boolean value) {
		return property(SERIALIZER_sortMaps, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_abridged} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_abridged
	 */
	public SerializerGroupBuilder abridged(boolean value) {
		return property(SERIALIZER_abridged, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_listener} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_listener
	 */
	public SerializerGroupBuilder listener(Class<? extends SerializerListener> value) {
		return property(SERIALIZER_listener, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beansRequireDefaultConstructor} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beansRequireDefaultConstructor
	 */
	public SerializerGroupBuilder beansRequireDefaultConstructor(boolean value) {
		return property(BEAN_beansRequireDefaultConstructor, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beansRequireSerializable} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beansRequireSerializable
	 */
	public SerializerGroupBuilder beansRequireSerializable(boolean value) {
		return property(BEAN_beansRequireSerializable, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beansRequireSettersForGetters} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beansRequireSettersForGetters
	 */
	public SerializerGroupBuilder beansRequireSettersForGetters(boolean value) {
		return property(BEAN_beansRequireSettersForGetters, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beansRequireSomeProperties} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beansRequireSomeProperties
	 */
	public SerializerGroupBuilder beansRequireSomeProperties(boolean value) {
		return property(BEAN_beansRequireSomeProperties, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanMapPutReturnsOldValue} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanMapPutReturnsOldValue
	 */
	public SerializerGroupBuilder beanMapPutReturnsOldValue(boolean value) {
		return property(BEAN_beanMapPutReturnsOldValue, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanConstructorVisibility} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanConstructorVisibility
	 */
	public SerializerGroupBuilder beanConstructorVisibility(Visibility value) {
		return property(BEAN_beanConstructorVisibility, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanClassVisibility} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanClassVisibility
	 */
	public SerializerGroupBuilder beanClassVisibility(Visibility value) {
		return property(BEAN_beanClassVisibility, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanFieldVisibility} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanFieldVisibility
	 */
	public SerializerGroupBuilder beanFieldVisibility(Visibility value) {
		return property(BEAN_beanFieldVisibility, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_methodVisibility} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_methodVisibility
	 */
	public SerializerGroupBuilder methodVisibility(Visibility value) {
		return property(BEAN_methodVisibility, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_useJavaBeanIntrospector} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_useJavaBeanIntrospector
	 */
	public SerializerGroupBuilder useJavaBeanIntrospector(boolean value) {
		return property(BEAN_useJavaBeanIntrospector, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_useInterfaceProxies} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_useInterfaceProxies
	 */
	public SerializerGroupBuilder useInterfaceProxies(boolean value) {
		return property(BEAN_useInterfaceProxies, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_ignoreUnknownBeanProperties} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_ignoreUnknownBeanProperties
	 */
	public SerializerGroupBuilder ignoreUnknownBeanProperties(boolean value) {
		return property(BEAN_ignoreUnknownBeanProperties, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_ignoreUnknownNullBeanProperties} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_ignoreUnknownNullBeanProperties
	 */
	public SerializerGroupBuilder ignoreUnknownNullBeanProperties(boolean value) {
		return property(BEAN_ignoreUnknownNullBeanProperties, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_ignorePropertiesWithoutSetters} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_ignorePropertiesWithoutSetters
	 */
	public SerializerGroupBuilder ignorePropertiesWithoutSetters(boolean value) {
		return property(BEAN_ignorePropertiesWithoutSetters, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_ignoreInvocationExceptionsOnGetters} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_ignoreInvocationExceptionsOnGetters
	 */
	public SerializerGroupBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
		return property(BEAN_ignoreInvocationExceptionsOnGetters, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_ignoreInvocationExceptionsOnSetters} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_ignoreInvocationExceptionsOnSetters
	 */
	public SerializerGroupBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
		return property(BEAN_ignoreInvocationExceptionsOnSetters, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_sortProperties} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_sortProperties
	 */
	public SerializerGroupBuilder sortProperties(boolean value) {
		return property(BEAN_sortProperties, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanPackages_add} property on all serializers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages_add
	 */
	public SerializerGroupBuilder notBeanPackages(String...values) {
		return property(BEAN_notBeanPackages_add, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanPackages_add} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages_add
	 */
	public SerializerGroupBuilder notBeanPackages(Collection<String> value) {
		return property(BEAN_notBeanPackages_add, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanPackages} property on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages
	 */
	public SerializerGroupBuilder setNotBeanPackages(String...values) {
		return property(BEAN_notBeanPackages, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanPackages} property on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages
	 */
	public SerializerGroupBuilder setNotBeanPackages(Collection<String> values) {
		return property(BEAN_notBeanPackages, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanPackages_remove} property on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages_remove
	 */
	public SerializerGroupBuilder removeNotBeanPackages(String...values) {
		return property(BEAN_notBeanPackages_remove, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanPackages_remove} property on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages_remove
	 */
	public SerializerGroupBuilder removeNotBeanPackages(Collection<String> values) {
		return property(BEAN_notBeanPackages_remove, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanClasses_add} property on all serializers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanClasses_add
	 */
	public SerializerGroupBuilder notBeanClasses(Class<?>...values) {
		return property(BEAN_notBeanClasses_add, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanClasses_add} property on all serializers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages_add
	 */
	public SerializerGroupBuilder notBeanClasses(Collection<Class<?>> values) {
		return property(BEAN_notBeanClasses_add, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanClasses} property on all serializers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanClasses
	 */
	public SerializerGroupBuilder setNotBeanClasses(Class<?>...values) {
		return property(BEAN_notBeanClasses, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanClasses} property on all serializers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanClasses
	 */
	public SerializerGroupBuilder setNotBeanClasses(Collection<Class<?>> values) {
		return property(BEAN_notBeanClasses, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanClasses_remove} property on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanClasses_remove
	 */
	public SerializerGroupBuilder removeNotBeanClasses(Class<?>...values) {
		return property(BEAN_notBeanClasses_remove, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanClasses_remove} property on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanClasses_remove
	 */
	public SerializerGroupBuilder removeNotBeanClasses(Collection<Class<?>> values) {
		return property(BEAN_notBeanClasses_remove, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanFilters_add} property on all serializers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanFilters_add
	 */
	public SerializerGroupBuilder beanFilters(Class<?>...values) {
		return property(BEAN_beanFilters_add, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanFilters_add} property on all serializers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanFilters_add
	 */
	public SerializerGroupBuilder beanFilters(Collection<Class<?>> values) {
		return property(BEAN_beanFilters_add, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanFilters} property on all serializers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanFilters
	 */
	public SerializerGroupBuilder setBeanFilters(Class<?>...values) {
		return property(BEAN_beanFilters, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanFilters} property on all serializers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanFilters
	 */
	public SerializerGroupBuilder setBeanFilters(Collection<Class<?>> values) {
		return property(BEAN_beanFilters, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanFilters_remove} property on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanFilters_remove
	 */
	public SerializerGroupBuilder removeBeanFilters(Class<?>...values) {
		return property(BEAN_beanFilters_remove, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanFilters_remove} property on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanFilters_remove
	 */
	public SerializerGroupBuilder removeBeanFilters(Collection<Class<?>> values) {
		return property(BEAN_beanFilters_remove, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_pojoSwaps_add} property on all serializers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_pojoSwaps_add
	 */
	public SerializerGroupBuilder pojoSwaps(Class<?>...values) {
		return property(BEAN_pojoSwaps_add, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_pojoSwaps_add} property on all serializers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_pojoSwaps_add
	 */
	public SerializerGroupBuilder pojoSwaps(Collection<Class<?>> values) {
		return property(BEAN_pojoSwaps_add, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_pojoSwaps} property on all serializers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_pojoSwaps
	 */
	public SerializerGroupBuilder setPojoSwaps(Class<?>...values) {
		return property(BEAN_pojoSwaps, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_pojoSwaps} property on all serializers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_pojoSwaps
	 */
	public SerializerGroupBuilder setPojoSwaps(Collection<Class<?>> values) {
		return property(BEAN_pojoSwaps, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_pojoSwaps_remove} property on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_pojoSwaps_remove
	 */
	public SerializerGroupBuilder removePojoSwaps(Class<?>...values) {
		return property(BEAN_pojoSwaps_remove, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_pojoSwaps_remove} property on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_pojoSwaps_remove
	 */
	public SerializerGroupBuilder removePojoSwaps(Collection<Class<?>> values) {
		return property(BEAN_pojoSwaps_remove, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_implClasses} property on all serializers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_implClasses
	 */
	public SerializerGroupBuilder implClasses(Map<Class<?>,Class<?>> values) {
		return property(BEAN_implClasses, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_implClasses_put} property on all serializers in this group.
	 *
	 * @param interfaceClass The interface class.
	 * @param implClass The implementation class.
	 * @param <T> The class type of the interface.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_implClasses
	 * @see BeanContext#BEAN_implClasses_put
	 */
	public <T> SerializerGroupBuilder implClass(Class<T> interfaceClass, Class<? extends T> implClass) {
		return putToProperty(BEAN_implClasses, interfaceClass, implClass);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanDictionary_add} property on all serializers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanDictionary_add
	 */
	public SerializerGroupBuilder beanDictionary(Class<?>...values) {
		return property(BEAN_beanDictionary_add, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanDictionary_add} property on all serializers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanDictionary_add
	 */
	public SerializerGroupBuilder beanDictionary(Collection<Class<?>> values) {
		return property(BEAN_beanDictionary_add, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanDictionary} property on all serializers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanDictionary
	 */
	public SerializerGroupBuilder setBeanDictionary(Class<?>...values) {
		return property(BEAN_beanDictionary, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanDictionary} property on all serializers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanDictionary
	 */
	public SerializerGroupBuilder setBeanDictionary(Collection<Class<?>> values) {
		return property(BEAN_beanDictionary, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanDictionary_remove} property on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanDictionary_remove
	 */
	public SerializerGroupBuilder removeFromBeanDictionary(Class<?>...values) {
		return property(BEAN_beanDictionary_remove, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanDictionary_remove} property on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanDictionary_remove
	 */
	public SerializerGroupBuilder removeFromBeanDictionary(Collection<Class<?>> values) {
		return property(BEAN_beanDictionary_remove, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanTypePropertyName} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanTypePropertyName
	 */
	public SerializerGroupBuilder beanTypePropertyName(String value) {
		return property(BEAN_beanTypePropertyName, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_defaultParser} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_defaultParser
	 */
	public SerializerGroupBuilder defaultParser(Class<?> value) {
		return property(BEAN_defaultParser, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_locale} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_locale
	 */
	public SerializerGroupBuilder locale(Locale value) {
		return property(BEAN_locale, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_timeZone} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_timeZone
	 */
	public SerializerGroupBuilder timeZone(TimeZone value) {
		return property(BEAN_timeZone, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_mediaType} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_mediaType
	 */
	public SerializerGroupBuilder mediaType(MediaType value) {
		return property(BEAN_mediaType, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_debug} property to <jk>true</jk> on all serializers in this group.
	 *
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_debug
	 */
	public SerializerGroupBuilder debug() {
		return property(BEAN_debug, true);
	}

	/**
	 * Specifies the classloader to use when resolving classes from strings for all serializers in this group.
	 * <p>
	 * Can be used for resolving class names when the classes being created are in a different
	 * 	classloader from the Juneau code.
	 * <p>
	 * If <jk>null</jk>, the system classloader will be used to resolve classes.
	 *
	 * @param classLoader The new classloader.
	 * @return This object (for method chaining).
	 * @see PropertyStore#setClassLoader(ClassLoader)
	 */
	public SerializerGroupBuilder classLoader(ClassLoader classLoader) {
		propertyStore.setClassLoader(classLoader);
		return this;
	}
}
