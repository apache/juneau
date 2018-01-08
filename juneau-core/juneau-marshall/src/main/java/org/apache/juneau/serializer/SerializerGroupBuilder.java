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

import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.serializer.Serializer.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;

/**
 * Builder class for creating instances of {@link SerializerGroup}.
 */
public class SerializerGroupBuilder extends BeanContextBuilder {

	private final List<Object> serializers;

	/**
	 * Create an empty serializer group builder.
	 */
	public SerializerGroupBuilder() {
		this.serializers = new ArrayList<>();
	}

	/**
	 * Clone an existing serializer group builder.
	 *
	 * @param copyFrom The serializer group that we're copying settings and serializers from.
	 */
	public SerializerGroupBuilder(SerializerGroup copyFrom) {
		super(copyFrom.getPropertyStore());
		this.serializers = new ArrayList<>();
		addReverse(serializers, copyFrom.getSerializers());
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
	 * <p>
	 * When passing in pre-instantiated serializers to this group, applying properties and transforms to the group
	 * do not affect them.
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
	 * <p>
	 * Objects can either be instances of serializers or serializer classes.
	 *
	 * @param s The serializers to append to this group.
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder append(List<Object> s) {
		addReverse(serializers, s);
		return this;
	}

	/**
	 * Registers the specified serializers with this group.
	 * 
	 * <p>
	 * Objects can either be instances of serializers or serializer classes.
	 *
	 * @param s The serializers to append to this group.
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder append(Object...s) {
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
	@Override /* ContextBuilder */
	@SuppressWarnings("unchecked")
	public SerializerGroup build() {
		List<Serializer> l = new ArrayList<>();
		for (Object s : serializers) {
			Class<? extends Serializer> c = null;
			PropertyStore ps = getPropertyStore();
			if (s instanceof Class) {
				c = (Class<? extends Serializer>)s;
				l.add(ContextCache.INSTANCE.create(c, ps));
			} else {
				l.add((Serializer)s);
			}
		}
		return new SerializerGroup(getPropertyStore(), ArrayUtils.toReverseArray(Serializer.class, l));
	}


	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	/**
	 * Sets the {@link Serializer#SERIALIZER_abridged} property on all serializers in this group.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_abridged}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder abridged(boolean value) {
		return set(SERIALIZER_abridged, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_addBeanTypeProperties} property on all serializers in this group.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_addBeanTypeProperties}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder addBeanTypeProperties(boolean value) {
		return set(SERIALIZER_addBeanTypeProperties, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_detectRecursions} property on all serializers in this group.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_detectRecursions}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder detectRecursions(boolean value) {
		return set(SERIALIZER_detectRecursions, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_ignoreRecursions} property on all serializers in this group.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_ignoreRecursions}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder ignoreRecursions(boolean value) {
		return set(SERIALIZER_ignoreRecursions, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_initialDepth} property on all serializers in this group.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_initialDepth}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder initialDepth(int value) {
		return set(SERIALIZER_initialDepth, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_listener} property on all serializers in this group.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_listener}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder listener(Class<? extends SerializerListener> value) {
		return set(SERIALIZER_listener, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_maxDepth} property on all serializers in this group.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_maxDepth}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder maxDepth(int value) {
		return set(SERIALIZER_maxDepth, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_maxIndent} property on all serializers in this group.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_maxIndent}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder maxIndent(boolean value) {
		return set(SERIALIZER_maxIndent, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_quoteChar} property on all serializers in this group.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_quoteChar}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder quoteChar(char value) {
		return set(SERIALIZER_quoteChar, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_sortCollections} property on all serializers in this group.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_sortCollections}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder sortCollections(boolean value) {
		return set(SERIALIZER_sortCollections, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_sortMaps} property on all serializers in this group.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_sortMaps}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder sortMaps(boolean value) {
		return set(SERIALIZER_sortMaps, value);
	}

	/**
	 * Shortcut for calling <code>quoteChar(<js>'\''</js>)</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_quoteChar}
	 * </ul>
	 * 
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder sq() {
		return quoteChar('\'');
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_trimEmptyCollections} property on all serializers in this group.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimEmptyCollections}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder trimEmptyCollections(boolean value) {
		return set(SERIALIZER_trimEmptyCollections, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_trimEmptyMaps} property on all serializers in this group.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimEmptyMaps}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder trimEmptyMaps(boolean value) {
		return set(SERIALIZER_trimEmptyMaps, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_trimNullProperties} property on all serializers in this group.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimNullProperties}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder trimNullProperties(boolean value) {
		return set(SERIALIZER_trimNullProperties, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_trimStrings} property on all serializers in this group.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimStrings}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder trimStrings(boolean value) {
		return set(SERIALIZER_trimStrings, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_uriContext} property on all serializers in this group.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_uriContext}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder uriContext(UriContext value) {
		return set(SERIALIZER_uriContext, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_uriRelativity} property on all serializers in this group.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_uriRelativity}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder uriRelativity(UriRelativity value) {
		return set(SERIALIZER_uriRelativity, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_uriResolution} property on all serializers in this group.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_uriResolution}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder uriResolution(UriResolution value) {
		return set(SERIALIZER_uriResolution, value);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_useWhitespace} property on all serializers in this group.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_useWhitespace}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder useWhitespace(boolean value) {
		return set(SERIALIZER_useWhitespace, value);
	}

	/**
	 * Shortcut for calling <code>useWhitespace(<jk>true</jk>)</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_useWhitespace}
	 * </ul>
	 * 
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder ws() {
		return useWhitespace(true);
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder beansRequireDefaultConstructor(boolean value) {
		super.beansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder beansRequireSerializable(boolean value) {
		super.beansRequireSerializable(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder beansRequireSettersForGetters(boolean value) {
		super.beansRequireSettersForGetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder beansRequireSomeProperties(boolean value) {
		super.beansRequireSomeProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder beanMapPutReturnsOldValue(boolean value) {
		super.beanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder methodVisibility(Visibility value) {
		super.methodVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder useJavaBeanIntrospector(boolean value) {
		super.useJavaBeanIntrospector(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder useInterfaceProxies(boolean value) {
		super.useInterfaceProxies(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder ignoreUnknownBeanProperties(boolean value) {
		super.ignoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder ignoreUnknownNullBeanProperties(boolean value) {
		super.ignoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder ignorePropertiesWithoutSetters(boolean value) {
		super.ignorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
		super.ignoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
		super.ignoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder sortProperties(boolean value) {
		super.sortProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder notBeanPackages(Object...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder notBeanPackages(String...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder notBeanPackages(boolean append, Object...values) {
		super.notBeanPackages(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder notBeanPackagesRemove(Object...values) {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder notBeanClasses(Class<?>...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder notBeanClasses(boolean append, Object...values) {
		super.notBeanClasses(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder notBeanClassesRemove(Object...values) {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder beanFilters(Object...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder beanFilters(Class<?>...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder beanFilters(boolean append, Object...values) {
		super.beanFilters(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder beanFiltersRemove(Object...values) {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder pojoSwaps(Object...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder pojoSwaps(Class<?>...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder pojoSwaps(boolean append, Object...values) {
		super.pojoSwaps(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder pojoSwapsRemove(Object...values) {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder implClasses(Map<String,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public <T> SerializerGroupBuilder implClass(Class<T> interfaceClass, Class<? extends T> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder beanDictionary(Object...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder beanDictionary(Class<?>...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder beanDictionary(boolean append, Object...values) {
		super.beanDictionary(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder beanDictionaryRemove(Object...values) {
		super.beanDictionaryRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder beanTypePropertyName(String value) {
		super.beanTypePropertyName(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerGroupBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerGroupBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerGroupBuilder set(boolean append, String name, Object value) {
		super.set(append, name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerGroupBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerGroupBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerGroupBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerGroupBuilder addTo(String name, String key, Object value) {
		super.addTo(name, key, value);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerGroupBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerGroupBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}
}
