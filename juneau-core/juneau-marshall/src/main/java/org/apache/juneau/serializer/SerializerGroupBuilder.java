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
import static org.apache.juneau.serializer.Serializer.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;

/**
 * Builder class for creating instances of {@link SerializerGroup}.
 */
public class SerializerGroupBuilder {

	private final List<Object> serializers;
	private final PropertyStoreBuilder propertyStore;

	/**
	 * Create an empty serializer group builder.
	 */
	public SerializerGroupBuilder() {
		this.serializers = new ArrayList<>();
		this.propertyStore = PropertyStore2.create();
	}

	/**
	 * Create an empty serializer group using the specified property store for settings.
	 *
	 * <p>
	 * Note:  Modifying the specified property store externally will also modify it here.
	 *
	 * @param propertyStore The property store containing all settings common to all serializers in this group.
	 */
	public SerializerGroupBuilder(PropertyStoreBuilder propertyStore) {
		this.serializers = new ArrayList<>();
		this.propertyStore = propertyStore;
	}

	/**
	 * Clone an existing serializer group builder.
	 *
	 * @param copyFrom The serializer group that we're copying settings and serializers from.
	 */
	public SerializerGroupBuilder(SerializerGroup copyFrom) {
		this.serializers = new ArrayList<>();
		addReverse(serializers, copyFrom.getSerializers());
		this.propertyStore = copyFrom.getPropertyStore().builder();
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
	 *
	 * <p>
	 * This method can be called multiple times to produce multiple serializer groups.
	 *
	 * @return A new {@link SerializerGroup} object.
	 */
	@SuppressWarnings("unchecked")
	public SerializerGroup build() {
		List<Serializer> l = new ArrayList<>();
		for (Object s : serializers) {
			Class<? extends Serializer> c = null;
			PropertyStore2 ps = propertyStore.build();
			if (s instanceof Class) {
				c = (Class<? extends Serializer>)s;
			} else {
				// Note that if we added a serializer instance, we want a new instance with this builder's properties
				// on top of the previous serializer's properties.
				Serializer s2 = (Serializer)s;
				ps = s2.getPropertyStore().builder().apply(ps).build();
				c = s2.getClass();
			}
			l.add(ContextCache.INSTANCE.create(c, ps));
		}
		return new SerializerGroup(propertyStore.build(), ArrayUtils.toReverseArray(Serializer.class, l));
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
	 * @see PropertyStoreBuilder#set(String, Object)
	 */
	public SerializerGroupBuilder set(String name, Object value) {
		propertyStore.set(name, value);
		return this;
	}

	/**
	 * Sets a set of properties on all serializers in this group.
	 *
	 * @param properties The properties to set on this class.
	 * @return This object (for method chaining).
	 * @see PropertyStoreBuilder#set(java.util.Map)
	 */
	public SerializerGroupBuilder set(ObjectMap properties) {
		propertyStore.set(properties);
		return this;
	}

	/**
	 * Appends a set of properties on all serializers in this group.
	 *
	 * @param properties The properties to append on this class.
	 * @return This object (for method chaining).
	 * @see PropertyStoreBuilder#add(java.util.Map)
	 */
	public SerializerGroupBuilder add(ObjectMap properties) {
		propertyStore.add(properties);
		return this;
	}

	/**
	 * Adds a value to a SET/LIST property on all serializers in this group.
	 *
	 * @param name The property name.
	 * @param value The new value to add to the SET property.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a SET property.
	 */
	public SerializerGroupBuilder addTo(String name, Object value) {
		propertyStore.addTo(name, value);
		return this;
	}

	/**
	 * Adds or overwrites a value to a SET/LIST/MAP property on all serializers in this group.
	 *
	 * @param name The property name.
	 * @param key The property value map key.
	 * @param value The property value map value.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a MAP property.
	 */
	public SerializerGroupBuilder addTo(String name, String key, Object value) {
		propertyStore.addTo(name, key, value);
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
	public SerializerGroupBuilder removeFrom(String name, Object value) {
		propertyStore.removeFrom(name, value);
		return this;
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_maxDepth} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_maxDepth
	 */
	public SerializerGroupBuilder maxDepth(int value) {
		return set(SERIALIZER_maxDepth, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_initialDepth} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_initialDepth
	 */
	public SerializerGroupBuilder initialDepth(int value) {
		return set(SERIALIZER_initialDepth, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_detectRecursions} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_detectRecursions
	 */
	public SerializerGroupBuilder detectRecursions(boolean value) {
		return set(SERIALIZER_detectRecursions, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_ignoreRecursions} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_ignoreRecursions
	 */
	public SerializerGroupBuilder ignoreRecursions(boolean value) {
		return set(SERIALIZER_ignoreRecursions, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_useWhitespace} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_useWhitespace
	 */
	public SerializerGroupBuilder useWhitespace(boolean value) {
		return set(SERIALIZER_useWhitespace, value);
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
	 * Sets the {@link Serializer#SERIALIZER_maxIndent} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_maxIndent
	 */
	public SerializerGroupBuilder maxIndent(boolean value) {
		return set(SERIALIZER_maxIndent, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_addBeanTypeProperties} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_addBeanTypeProperties
	 */
	public SerializerGroupBuilder addBeanTypeProperties(boolean value) {
		return set(SERIALIZER_addBeanTypeProperties, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_quoteChar} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_quoteChar
	 */
	public SerializerGroupBuilder quoteChar(char value) {
		return set(SERIALIZER_quoteChar, value);
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
	 * Sets the {@link Serializer#SERIALIZER_trimNullProperties} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_trimNullProperties
	 */
	public SerializerGroupBuilder trimNullProperties(boolean value) {
		return set(SERIALIZER_trimNullProperties, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_trimEmptyCollections} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_trimEmptyCollections
	 */
	public SerializerGroupBuilder trimEmptyCollections(boolean value) {
		return set(SERIALIZER_trimEmptyCollections, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_trimEmptyMaps} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_trimEmptyMaps
	 */
	public SerializerGroupBuilder trimEmptyMaps(boolean value) {
		return set(SERIALIZER_trimEmptyMaps, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_trimStrings} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_trimStrings
	 */
	public SerializerGroupBuilder trimStrings(boolean value) {
		return set(SERIALIZER_trimStrings, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_uriContext} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_uriContext
	 */
	public SerializerGroupBuilder uriContext(UriContext value) {
		return set(SERIALIZER_uriContext, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_uriResolution} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_uriResolution
	 */
	public SerializerGroupBuilder uriResolution(UriResolution value) {
		return set(SERIALIZER_uriResolution, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_uriRelativity} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_uriRelativity
	 */
	public SerializerGroupBuilder uriRelativity(UriRelativity value) {
		return set(SERIALIZER_uriRelativity, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_sortCollections} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_sortCollections
	 */
	public SerializerGroupBuilder sortCollections(boolean value) {
		return set(SERIALIZER_sortCollections, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_sortMaps} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_sortMaps
	 */
	public SerializerGroupBuilder sortMaps(boolean value) {
		return set(SERIALIZER_sortMaps, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_abridged} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_abridged
	 */
	public SerializerGroupBuilder abridged(boolean value) {
		return set(SERIALIZER_abridged, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_listener} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_listener
	 */
	public SerializerGroupBuilder listener(Class<? extends SerializerListener> value) {
		return set(SERIALIZER_listener, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beansRequireDefaultConstructor} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beansRequireDefaultConstructor
	 */
	public SerializerGroupBuilder beansRequireDefaultConstructor(boolean value) {
		return set(BEAN_beansRequireDefaultConstructor, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beansRequireSerializable} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beansRequireSerializable
	 */
	public SerializerGroupBuilder beansRequireSerializable(boolean value) {
		return set(BEAN_beansRequireSerializable, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beansRequireSettersForGetters} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beansRequireSettersForGetters
	 */
	public SerializerGroupBuilder beansRequireSettersForGetters(boolean value) {
		return set(BEAN_beansRequireSettersForGetters, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beansRequireSomeProperties} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beansRequireSomeProperties
	 */
	public SerializerGroupBuilder beansRequireSomeProperties(boolean value) {
		return set(BEAN_beansRequireSomeProperties, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanMapPutReturnsOldValue} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanMapPutReturnsOldValue
	 */
	public SerializerGroupBuilder beanMapPutReturnsOldValue(boolean value) {
		return set(BEAN_beanMapPutReturnsOldValue, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanConstructorVisibility} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanConstructorVisibility
	 */
	public SerializerGroupBuilder beanConstructorVisibility(Visibility value) {
		return set(BEAN_beanConstructorVisibility, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanClassVisibility} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanClassVisibility
	 */
	public SerializerGroupBuilder beanClassVisibility(Visibility value) {
		return set(BEAN_beanClassVisibility, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanFieldVisibility} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanFieldVisibility
	 */
	public SerializerGroupBuilder beanFieldVisibility(Visibility value) {
		return set(BEAN_beanFieldVisibility, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_methodVisibility} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_methodVisibility
	 */
	public SerializerGroupBuilder methodVisibility(Visibility value) {
		return set(BEAN_methodVisibility, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_useJavaBeanIntrospector} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_useJavaBeanIntrospector
	 */
	public SerializerGroupBuilder useJavaBeanIntrospector(boolean value) {
		return set(BEAN_useJavaBeanIntrospector, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_useInterfaceProxies} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_useInterfaceProxies
	 */
	public SerializerGroupBuilder useInterfaceProxies(boolean value) {
		return set(BEAN_useInterfaceProxies, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_ignoreUnknownBeanProperties} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_ignoreUnknownBeanProperties
	 */
	public SerializerGroupBuilder ignoreUnknownBeanProperties(boolean value) {
		return set(BEAN_ignoreUnknownBeanProperties, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_ignoreUnknownNullBeanProperties} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_ignoreUnknownNullBeanProperties
	 */
	public SerializerGroupBuilder ignoreUnknownNullBeanProperties(boolean value) {
		return set(BEAN_ignoreUnknownNullBeanProperties, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_ignorePropertiesWithoutSetters} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_ignorePropertiesWithoutSetters
	 */
	public SerializerGroupBuilder ignorePropertiesWithoutSetters(boolean value) {
		return set(BEAN_ignorePropertiesWithoutSetters, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_ignoreInvocationExceptionsOnGetters} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_ignoreInvocationExceptionsOnGetters
	 */
	public SerializerGroupBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
		return set(BEAN_ignoreInvocationExceptionsOnGetters, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_ignoreInvocationExceptionsOnSetters} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_ignoreInvocationExceptionsOnSetters
	 */
	public SerializerGroupBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
		return set(BEAN_ignoreInvocationExceptionsOnSetters, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_sortProperties} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_sortProperties
	 */
	public SerializerGroupBuilder sortProperties(boolean value) {
		return set(BEAN_sortProperties, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanPackages_add} property on all serializers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages_add
	 */
	public SerializerGroupBuilder notBeanPackages(String...values) {
		return set(BEAN_notBeanPackages_add, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanPackages_add} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages_add
	 */
	public SerializerGroupBuilder notBeanPackages(Collection<String> value) {
		return set(BEAN_notBeanPackages_add, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanPackages} property on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages
	 */
	public SerializerGroupBuilder setNotBeanPackages(String...values) {
		return set(BEAN_notBeanPackages, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanPackages} property on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages
	 */
	public SerializerGroupBuilder setNotBeanPackages(Collection<String> values) {
		return set(BEAN_notBeanPackages, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanPackages_remove} property on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages_remove
	 */
	public SerializerGroupBuilder removeNotBeanPackages(String...values) {
		return set(BEAN_notBeanPackages_remove, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanPackages_remove} property on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages_remove
	 */
	public SerializerGroupBuilder removeNotBeanPackages(Collection<String> values) {
		return set(BEAN_notBeanPackages_remove, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanClasses_add} property on all serializers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanClasses_add
	 */
	public SerializerGroupBuilder notBeanClasses(Class<?>...values) {
		return set(BEAN_notBeanClasses_add, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanClasses_add} property on all serializers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages_add
	 */
	public SerializerGroupBuilder notBeanClasses(Collection<Class<?>> values) {
		return set(BEAN_notBeanClasses_add, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanClasses} property on all serializers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanClasses
	 */
	public SerializerGroupBuilder setNotBeanClasses(Class<?>...values) {
		return set(BEAN_notBeanClasses, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanClasses} property on all serializers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanClasses
	 */
	public SerializerGroupBuilder setNotBeanClasses(Collection<Class<?>> values) {
		return set(BEAN_notBeanClasses, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanClasses_remove} property on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanClasses_remove
	 */
	public SerializerGroupBuilder removeNotBeanClasses(Class<?>...values) {
		return set(BEAN_notBeanClasses_remove, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanClasses_remove} property on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanClasses_remove
	 */
	public SerializerGroupBuilder removeNotBeanClasses(Collection<Class<?>> values) {
		return set(BEAN_notBeanClasses_remove, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanFilters_add} property on all serializers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanFilters_add
	 */
	public SerializerGroupBuilder beanFilters(Class<?>...values) {
		return set(BEAN_beanFilters_add, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanFilters_add} property on all serializers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanFilters_add
	 */
	public SerializerGroupBuilder beanFilters(Collection<Class<?>> values) {
		return set(BEAN_beanFilters_add, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanFilters} property on all serializers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanFilters
	 */
	public SerializerGroupBuilder setBeanFilters(Class<?>...values) {
		return set(BEAN_beanFilters, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanFilters} property on all serializers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanFilters
	 */
	public SerializerGroupBuilder setBeanFilters(Collection<Class<?>> values) {
		return set(BEAN_beanFilters, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanFilters_remove} property on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanFilters_remove
	 */
	public SerializerGroupBuilder removeBeanFilters(Class<?>...values) {
		return set(BEAN_beanFilters_remove, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanFilters_remove} property on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanFilters_remove
	 */
	public SerializerGroupBuilder removeBeanFilters(Collection<Class<?>> values) {
		return set(BEAN_beanFilters_remove, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_pojoSwaps_add} property on all serializers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_pojoSwaps_add
	 */
	public SerializerGroupBuilder pojoSwaps(Class<?>...values) {
		return set(BEAN_pojoSwaps_add, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_pojoSwaps_add} property on all serializers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_pojoSwaps_add
	 */
	public SerializerGroupBuilder pojoSwaps(Collection<Class<?>> values) {
		return set(BEAN_pojoSwaps_add, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_pojoSwaps} property on all serializers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_pojoSwaps
	 */
	public SerializerGroupBuilder setPojoSwaps(Class<?>...values) {
		return set(BEAN_pojoSwaps, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_pojoSwaps} property on all serializers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_pojoSwaps
	 */
	public SerializerGroupBuilder setPojoSwaps(Collection<Class<?>> values) {
		return set(BEAN_pojoSwaps, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_pojoSwaps_remove} property on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_pojoSwaps_remove
	 */
	public SerializerGroupBuilder removePojoSwaps(Class<?>...values) {
		return set(BEAN_pojoSwaps_remove, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_pojoSwaps_remove} property on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_pojoSwaps_remove
	 */
	public SerializerGroupBuilder removePojoSwaps(Collection<Class<?>> values) {
		return set(BEAN_pojoSwaps_remove, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_implClasses} property on all serializers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_implClasses
	 */
	public SerializerGroupBuilder implClasses(Map<String,Class<?>> values) {
		return set(BEAN_implClasses, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_implClasses} property on all serializers in this group.
	 *
	 * @param interfaceClass The interface class.
	 * @param implClass The implementation class.
	 * @param <T> The class type of the interface.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_implClasses
	 */
	public <T> SerializerGroupBuilder implClass(Class<T> interfaceClass, Class<? extends T> implClass) {
		return addTo(BEAN_implClasses, interfaceClass.getName(), implClass);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_includeProperties} property on all serializers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_includeProperties
	 */
	public SerializerGroupBuilder includeProperties(Map<String,String> values) {
		return set(BEAN_includeProperties, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_includeProperties} property on all serializers in this group.
	 *
	 * @param beanClassName The bean class name.  Can be a simple name, fully-qualified name, or <js>"*"</js>.
	 * @param properties Comma-delimited list of property names.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_includeProperties
	 */
	public SerializerGroupBuilder includeProperties(String beanClassName, String properties) {
		return addTo(BEAN_includeProperties, beanClassName, properties);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_includeProperties} property on all serializers in this group.
	 *
	 * @param beanClass The bean class.
	 * @param properties Comma-delimited list of property names.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_includeProperties
	 */
	public SerializerGroupBuilder includeProperties(Class<?> beanClass, String properties) {
		return addTo(BEAN_includeProperties, beanClass.getName(), properties);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_excludeProperties} property on all serializers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_excludeProperties
	 */
	public SerializerGroupBuilder excludeProperties(Map<String,String> values) {
		return set(BEAN_excludeProperties, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_excludeProperties} property on all serializers in this group.
	 *
	 * @param beanClassName The bean class name.  Can be a simple name, fully-qualified name, or <js>"*"</js>.
	 * @param properties Comma-delimited list of property names.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_excludeProperties
	 */
	public SerializerGroupBuilder excludeProperties(String beanClassName, String properties) {
		return addTo(BEAN_excludeProperties, beanClassName, properties);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_excludeProperties} property on all serializers in this group.
	 *
	 * @param beanClass The bean class.
	 * @param properties Comma-delimited list of property names.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_excludeProperties
	 */
	public SerializerGroupBuilder excludeProperties(Class<?> beanClass, String properties) {
		return addTo(BEAN_excludeProperties, beanClass.getName(), properties);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanDictionary_add} property on all serializers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanDictionary_add
	 */
	public SerializerGroupBuilder beanDictionary(Class<?>...values) {
		return set(BEAN_beanDictionary_add, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanDictionary_add} property on all serializers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanDictionary_add
	 */
	public SerializerGroupBuilder beanDictionary(Collection<Class<?>> values) {
		return set(BEAN_beanDictionary_add, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanDictionary} property on all serializers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanDictionary
	 */
	public SerializerGroupBuilder setBeanDictionary(Class<?>...values) {
		return set(BEAN_beanDictionary, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanDictionary} property on all serializers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanDictionary
	 */
	public SerializerGroupBuilder setBeanDictionary(Collection<Class<?>> values) {
		return set(BEAN_beanDictionary, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanDictionary_remove} property on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanDictionary_remove
	 */
	public SerializerGroupBuilder removeFromBeanDictionary(Class<?>...values) {
		return set(BEAN_beanDictionary_remove, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanDictionary_remove} property on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanDictionary_remove
	 */
	public SerializerGroupBuilder removeFromBeanDictionary(Collection<Class<?>> values) {
		return set(BEAN_beanDictionary_remove, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanTypePropertyName} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanTypePropertyName
	 */
	public SerializerGroupBuilder beanTypePropertyName(String value) {
		return set(BEAN_beanTypePropertyName, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_defaultParser} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_defaultParser
	 */
	public SerializerGroupBuilder defaultParser(Class<?> value) {
		return set(BEAN_defaultParser, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_locale} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_locale
	 */
	public SerializerGroupBuilder locale(Locale value) {
		return set(BEAN_locale, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_timeZone} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_timeZone
	 */
	public SerializerGroupBuilder timeZone(TimeZone value) {
		return set(BEAN_timeZone, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_mediaType} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_mediaType
	 */
	public SerializerGroupBuilder mediaType(MediaType value) {
		return set(BEAN_mediaType, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_debug} property to <jk>true</jk> on all serializers in this group.
	 *
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_debug
	 */
	public SerializerGroupBuilder debug() {
		return set(BEAN_debug, true);
	}
}
