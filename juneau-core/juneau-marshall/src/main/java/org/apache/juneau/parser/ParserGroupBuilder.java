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
package org.apache.juneau.parser;

import static org.apache.juneau.BeanContext.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.parser.Parser.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;

/**
 * Builder class for creating instances of {@link ParserGroup}.
 */
public class ParserGroupBuilder {

	private final List<Object> parsers;
	private final PropertyStoreBuilder propertyStore;

	/**
	 * Create an empty parser group builder.
	 */
	public ParserGroupBuilder() {
		this.parsers = new ArrayList<>();
		this.propertyStore = PropertyStore.create();
	}

	/**
	 * Create an empty parser group using the specified property store for settings.
	 *
	 * <p>
	 * Note:  Modifying the specified property store externally will also modify it here.
	 *
	 * @param propertyStore The property store containing all settings common to all parsers in this group.
	 */
	public ParserGroupBuilder(PropertyStoreBuilder propertyStore) {
		this.parsers = new ArrayList<>();
		this.propertyStore = propertyStore;
	}

	/**
	 * Clone an existing parser group builder.
	 *
	 * @param copyFrom The parser group that we're copying settings and parsers from.
	 */
	public ParserGroupBuilder(ParserGroup copyFrom) {
		this.parsers = new ArrayList<>();
		addReverse(parsers, copyFrom.getParsers());
		this.propertyStore = copyFrom.getPropertyStore().builder();
	}

	/**
	 * Registers the specified parsers with this group.
	 *
	 * @param p The parsers to append to this group.
	 * @return This object (for method chaining).
	 */
	public ParserGroupBuilder append(Class<?>...p) {
		addReverse(parsers, p);
		return this;
	}

	/**
	 * Registers the specified parsers with this group.
	 *
	 * @param p The parsers to append to this group.
	 * @return This object (for method chaining).
	 */
	public ParserGroupBuilder append(Parser...p) {
		addReverse(parsers, p);
		return this;
	}

	/**
	 * Registers the specified parsers with this group.
	 *
	 * @param p The parsers to append to this group.
	 * @return This object (for method chaining).
	 */
	public ParserGroupBuilder append(List<Parser> p) {
		addReverse(parsers, p);
		return this;
	}

	/**
	 * Creates a new {@link ParserGroup} object using a snapshot of the settings defined in this builder.
	 *
	 * <p>
	 * This method can be called multiple times to produce multiple parser groups.
	 *
	 * @return A new {@link ParserGroup} object.
	 */
	@SuppressWarnings("unchecked")
	public ParserGroup build() {
		List<Parser> l = new ArrayList<>();
		for (Object p : parsers) {
			Class<? extends Parser> c = null;
			PropertyStore ps = propertyStore.build();
			if (p instanceof Class) {
				c = (Class<? extends Parser>)p;
			} else {
				// Note that if we added a serializer instance, we want a new instance with this builder's properties
				// on top of the previous serializer's properties.
				Parser p2 = (Parser)p;
				ps = p2.getPropertyStore().builder().apply(ps).build();
				c = p2.getClass();
			}
			l.add(ContextCache.INSTANCE.create(c, ps));
		}
		return new ParserGroup(propertyStore.build(), ArrayUtils.toReverseArray(Parser.class, l));
	}


	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	/**
	 * Sets a property on all parsers in this group.
	 *
	 * @param name The property name.
	 * @param value The property value.
	 * @return This object (for method chaining).
	 * @see PropertyStoreBuilder#set(String, Object)
	 */
	public ParserGroupBuilder set(String name, Object value) {
		propertyStore.set(name, value);
		return this;
	}

	/**
	 * Sets a set of properties on all parsers in this group.
	 *
	 * @param properties The properties to set on this class.
	 * @return This object (for method chaining).
	 * @see PropertyStoreBuilder#set(java.util.Map)
	 */
	public ParserGroupBuilder set(ObjectMap properties) {
		propertyStore.set(properties);
		return this;
	}

	/**
	 * Appends a set of properties on all parsers in this group.
	 *
	 * @param properties The properties to append on this class.
	 * @return This object (for method chaining).
	 * @see PropertyStoreBuilder#add(java.util.Map)
	 */
	public ParserGroupBuilder add(ObjectMap properties) {
		propertyStore.add(properties);
		return this;
	}

	/**
	 * Adds a value to a SET/LIST/MAP property on all parsers in this group.
	 *
	 * @param name The property name.
	 * @param value The new value to add to the SET property.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a SET property.
	 */
	public ParserGroupBuilder addTo(String name, Object value) {
		propertyStore.addTo(name, value);
		return this;
	}

	/**
	 * Adds or overwrites a value to a MAP property on all parsers in this group.
	 *
	 * @param name The property name.
	 * @param key The property value map key.
	 * @param value The property value map value.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a MAP property.
	 */
	public ParserGroupBuilder addTo(String name, String key, Object value) {
		propertyStore.addTo(name, key, value);
		return this;
	}

	/**
	 * Removes a value from a SET property on all parsers in this group.
	 *
	 * @param name The property name.
	 * @param value The property value in the SET property.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a SET property.
	 */
	public ParserGroupBuilder removeFrom(String name, Object value) {
		propertyStore.removeFrom(name, value);
		return this;
	}

	/**
	 * Sets the {@link Parser#PARSER_trimStrings} property on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Parser#PARSER_trimStrings
	 */
	public ParserGroupBuilder trimStrings(boolean value) {
		return set(PARSER_trimStrings, value);
	}

	/**
	 * Sets the {@link Parser#PARSER_strict} property on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Parser#PARSER_strict
	 */
	public ParserGroupBuilder strict(boolean value) {
		return set(PARSER_strict, value);
	}

	/**
	 * Sets the {@link Parser#PARSER_inputStreamCharset} property on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Parser#PARSER_inputStreamCharset
	 */
	public ParserGroupBuilder inputStreamCharset(String value) {
		return set(PARSER_inputStreamCharset, value);
	}

	/**
	 * Sets the {@link Parser#PARSER_fileCharset} property on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Parser#PARSER_fileCharset
	 */
	public ParserGroupBuilder fileCharset(String value) {
		return set(PARSER_fileCharset, value);
	}

	/**
	 * Sets the {@link Parser#PARSER_listener} property on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Parser#PARSER_listener
	 */
	public ParserGroupBuilder listener(Class<? extends ParserListener> value) {
		return set(PARSER_listener, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beansRequireDefaultConstructor} property on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beansRequireDefaultConstructor
	 */
	public ParserGroupBuilder beansRequireDefaultConstructor(boolean value) {
		return set(BEAN_beansRequireDefaultConstructor, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beansRequireSerializable} property on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beansRequireSerializable
	 */
	public ParserGroupBuilder beansRequireSerializable(boolean value) {
		return set(BEAN_beansRequireSerializable, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beansRequireSettersForGetters} property on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beansRequireSettersForGetters
	 */
	public ParserGroupBuilder beansRequireSettersForGetters(boolean value) {
		return set(BEAN_beansRequireSettersForGetters, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beansRequireSomeProperties} property on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beansRequireSomeProperties
	 */
	public ParserGroupBuilder beansRequireSomeProperties(boolean value) {
		return set(BEAN_beansRequireSomeProperties, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanMapPutReturnsOldValue} property on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanMapPutReturnsOldValue
	 */
	public ParserGroupBuilder beanMapPutReturnsOldValue(boolean value) {
		return set(BEAN_beanMapPutReturnsOldValue, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanConstructorVisibility} property on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanConstructorVisibility
	 */
	public ParserGroupBuilder beanConstructorVisibility(Visibility value) {
		return set(BEAN_beanConstructorVisibility, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanClassVisibility} property on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanClassVisibility
	 */
	public ParserGroupBuilder beanClassVisibility(Visibility value) {
		return set(BEAN_beanClassVisibility, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanFieldVisibility} property on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanFieldVisibility
	 */
	public ParserGroupBuilder beanFieldVisibility(Visibility value) {
		return set(BEAN_beanFieldVisibility, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_methodVisibility} property on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_methodVisibility
	 */
	public ParserGroupBuilder methodVisibility(Visibility value) {
		return set(BEAN_methodVisibility, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_useJavaBeanIntrospector} property on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_useJavaBeanIntrospector
	 */
	public ParserGroupBuilder useJavaBeanIntrospector(boolean value) {
		return set(BEAN_useJavaBeanIntrospector, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_useInterfaceProxies} property on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_useInterfaceProxies
	 */
	public ParserGroupBuilder useInterfaceProxies(boolean value) {
		return set(BEAN_useInterfaceProxies, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_ignoreUnknownBeanProperties} property on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_ignoreUnknownBeanProperties
	 */
	public ParserGroupBuilder ignoreUnknownBeanProperties(boolean value) {
		return set(BEAN_ignoreUnknownBeanProperties, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_ignoreUnknownNullBeanProperties} property on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_ignoreUnknownNullBeanProperties
	 */
	public ParserGroupBuilder ignoreUnknownNullBeanProperties(boolean value) {
		return set(BEAN_ignoreUnknownNullBeanProperties, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_ignorePropertiesWithoutSetters} property on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_ignorePropertiesWithoutSetters
	 */
	public ParserGroupBuilder ignorePropertiesWithoutSetters(boolean value) {
		return set(BEAN_ignorePropertiesWithoutSetters, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_ignoreInvocationExceptionsOnGetters} property on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_ignoreInvocationExceptionsOnGetters
	 */
	public ParserGroupBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
		return set(BEAN_ignoreInvocationExceptionsOnGetters, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_ignoreInvocationExceptionsOnSetters} property on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_ignoreInvocationExceptionsOnSetters
	 */
	public ParserGroupBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
		return set(BEAN_ignoreInvocationExceptionsOnSetters, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_sortProperties} property on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_sortProperties
	 */
	public ParserGroupBuilder sortProperties(boolean value) {
		return set(BEAN_sortProperties, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanPackages_add} property on all parsers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages_add
	 */
	public ParserGroupBuilder notBeanPackages(String...values) {
		return set(BEAN_notBeanPackages_add, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanPackages_add} property on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages_add
	 */
	public ParserGroupBuilder notBeanPackages(Collection<String> value) {
		return set(BEAN_notBeanPackages_add, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanPackages} property on all parsers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages
	 */
	public ParserGroupBuilder setNotBeanPackages(String...values) {
		return set(BEAN_notBeanPackages, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanPackages} property on all parsers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages
	 */
	public ParserGroupBuilder setNotBeanPackages(Collection<String> values) {
		return set(BEAN_notBeanPackages, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanPackages_remove} property on all parsers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages
	 * @see BeanContext#BEAN_notBeanPackages_remove
	 */
	public ParserGroupBuilder removeNotBeanPackages(String...values) {
		return set(BEAN_notBeanPackages_remove, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanPackages_remove} property on all parsers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages
	 * @see BeanContext#BEAN_notBeanPackages_remove
	 */
	public ParserGroupBuilder removeNotBeanPackages(Collection<String> values) {
		return set(BEAN_notBeanPackages_remove, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanClasses_add} property on all parsers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanClasses_add
	 */
	public ParserGroupBuilder notBeanClasses(Class<?>...values) {
		return set(BEAN_notBeanClasses_add, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanClasses_add} property on all parsers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages_add
	 */
	public ParserGroupBuilder notBeanClasses(Collection<Class<?>> values) {
		return set(BEAN_notBeanClasses_add, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanClasses} property on all parsers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanClasses
	 */
	public ParserGroupBuilder setNotBeanClasses(Class<?>...values) {
		return set(BEAN_notBeanClasses, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanClasses} property on all parsers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanClasses
	 */
	public ParserGroupBuilder setNotBeanClasses(Collection<Class<?>> values) {
		return set(BEAN_notBeanClasses, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanClasses_remove} property on all parsers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanClasses_remove
	 */
	public ParserGroupBuilder removeNotBeanClasses(Class<?>...values) {
		return set(BEAN_notBeanClasses_remove, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_notBeanClasses_remove} property on all parsers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanClasses_remove
	 */
	public ParserGroupBuilder removeNotBeanClasses(Collection<Class<?>> values) {
		return set(BEAN_notBeanClasses_remove, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanFilters_add} property on all parsers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanFilters_add
	 */
	public ParserGroupBuilder beanFilters(Class<?>...values) {
		return set(BEAN_beanFilters_add, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanFilters_add} property on all parsers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanFilters_add
	 */
	public ParserGroupBuilder beanFilters(Collection<Class<?>> values) {
		return set(BEAN_beanFilters_add, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanFilters} property on all parsers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanFilters
	 */
	public ParserGroupBuilder setBeanFilters(Class<?>...values) {
		return set(BEAN_beanFilters, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanFilters} property on all parsers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanFilters
	 */
	public ParserGroupBuilder setBeanFilters(Collection<Class<?>> values) {
		return set(BEAN_beanFilters, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanFilters_remove} property on all parsers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanFilters_remove
	 */
	public ParserGroupBuilder removeBeanFilters(Class<?>...values) {
		return set(BEAN_beanFilters_remove, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanFilters_remove} property on all parsers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanFilters_remove
	 */
	public ParserGroupBuilder removeBeanFilters(Collection<Class<?>> values) {
		return set(BEAN_beanFilters_remove, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_pojoSwaps_add} property on all parsers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_pojoSwaps_add
	 */
	public ParserGroupBuilder pojoSwaps(Class<?>...values) {
		return set(BEAN_pojoSwaps_add, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_pojoSwaps_add} property on all parsers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_pojoSwaps_add
	 */
	public ParserGroupBuilder pojoSwaps(Collection<Class<?>> values) {
		return set(BEAN_pojoSwaps_add, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_pojoSwaps} property on all parsers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_pojoSwaps
	 */
	public ParserGroupBuilder setPojoSwaps(Class<?>...values) {
		return set(BEAN_pojoSwaps, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_pojoSwaps} property on all parsers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_pojoSwaps
	 */
	public ParserGroupBuilder setPojoSwaps(Collection<Class<?>> values) {
		return set(BEAN_pojoSwaps, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_pojoSwaps_remove} property on all parsers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_pojoSwaps_remove
	 */
	public ParserGroupBuilder removePojoSwaps(Class<?>...values) {
		return set(BEAN_pojoSwaps_remove, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_pojoSwaps_remove} property on all parsers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_pojoSwaps_remove
	 */
	public ParserGroupBuilder removePojoSwaps(Collection<Class<?>> values) {
		return set(BEAN_pojoSwaps_remove, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_implClasses} property on all parsers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_implClasses
	 */
	public ParserGroupBuilder implClasses(Map<String,Class<?>> values) {
		return set(BEAN_implClasses, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_implClasses} property on all parsers in this group.
	 *
	 * @param interfaceClass The interface class.
	 * @param implClass The implementation class.
	 * @param <T> The class type of the interface.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_implClasses
	 */
	public <T> ParserGroupBuilder implClass(Class<T> interfaceClass, Class<? extends T> implClass) {
		return addTo(BEAN_implClasses, interfaceClass.getName(), implClass);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanDictionary_add} property on all parsers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanDictionary_add
	 */
	public ParserGroupBuilder beanDictionary(Class<?>...values) {
		return set(BEAN_beanDictionary_add, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanDictionary_add} property on all parsers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanDictionary_add
	 */
	public ParserGroupBuilder beanDictionary(Collection<Class<?>> values) {
		return set(BEAN_beanDictionary_add, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanDictionary} property on all parsers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanDictionary
	 */
	public ParserGroupBuilder setBeanDictionary(Class<?>...values) {
		return set(BEAN_beanDictionary, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanDictionary} property on all parsers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanDictionary
	 */
	public ParserGroupBuilder setBeanDictionary(Collection<Class<?>> values) {
		return set(BEAN_beanDictionary, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanDictionary_remove} property on all parsers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanDictionary_remove
	 */
	public ParserGroupBuilder removeFromBeanDictionary(Class<?>...values) {
		return set(BEAN_beanDictionary_remove, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanDictionary_remove} property on all parsers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanDictionary_remove
	 */
	public ParserGroupBuilder removeFromBeanDictionary(Collection<Class<?>> values) {
		return set(BEAN_beanDictionary_remove, values);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_beanTypePropertyName} property on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanTypePropertyName
	 */
	public ParserGroupBuilder beanTypePropertyName(String value) {
		return set(BEAN_beanTypePropertyName, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_defaultParser} property on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_defaultParser
	 */
	public ParserGroupBuilder defaultParser(Class<?> value) {
		return set(BEAN_defaultParser, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_locale} property on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_locale
	 */
	public ParserGroupBuilder locale(Locale value) {
		return set(BEAN_locale, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_timeZone} property on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_timeZone
	 */
	public ParserGroupBuilder timeZone(TimeZone value) {
		return set(BEAN_timeZone, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_mediaType} property on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_mediaType
	 */
	public ParserGroupBuilder mediaType(MediaType value) {
		return set(BEAN_mediaType, value);
	}

	/**
	 * Sets the {@link BeanContext#BEAN_debug} property on all parsers in this group.
	 *
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_debug
	 */
	public ParserGroupBuilder debug() {
		return set(BEAN_debug, true);
	}
}
