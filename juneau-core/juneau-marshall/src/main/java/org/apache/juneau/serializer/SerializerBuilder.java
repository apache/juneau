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

import static org.apache.juneau.serializer.Serializer.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;

/**
 * Builder class for building instances of serializers.
 */
public class SerializerBuilder extends BeanContextBuilder {

	/**
	 * Constructor, default settings.
	 */
	public SerializerBuilder() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param ps The initial configuration settings for this builder.
	 */
	public SerializerBuilder(PropertyStore ps) {
		super(ps);
	}


	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	/**
	 * Configuration property:  Add <js>"_type"</js> properties when needed.
	 * 
	 * <p>
	 * If <jk>true</jk>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * through reflection.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_addBeanTypes}
	 * </ul>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public SerializerBuilder addBeanTypes(boolean value) {
		return set(SERIALIZER_addBeanTypes, value);
	}

	/**
	 * Configuration property:  Add <js>"_type"</js> properties when needed.
	 * 
	 * <p>
	 * Shortcut for calling <code>addBeanTypes(<jk>true</jk>)</code>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_addBeanTypes}
	 * </ul>
	 * 
	 * @return This object (for method chaining).
	 */
	public SerializerBuilder addBeanTypes() {
		return set(SERIALIZER_addBeanTypes, true);
	}

	/**
	 * Configuration property:  Add type attribute to root nodes.
	 * 
	 * <p>
	 * When disabled, it is assumed that the parser knows the exact Java POJO type being parsed, and therefore top-level
	 * type information that might normally be included to determine the data type will not be serialized.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_addRootType}
	 * </ul>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public SerializerBuilder addRootType(boolean value) {
		return set(SERIALIZER_addRootType, value);
	}

	/**
	 * Configuration property:  Add type attribute to root nodes.
	 * 
	 * <p>
	 * Shortcut for calling <code>addRootType(<jk>true</jk>)</code>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_addRootType}
	 * </ul>
	 * 
	 * @return This object (for method chaining).
	 */
	public SerializerBuilder addRootType() {
		return set(SERIALIZER_addRootType, true);
	}

	/**
	 * Configuration property:  Automatically detect POJO recursions.
	 * 
	 * <p>
	 * Specifies that recursions should be checked for during serialization.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Checking for recursion can cause a small performance penalty.
	 * </ul>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_detectRecursions}
	 * </ul>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public SerializerBuilder detectRecursions(boolean value) {
		return set(SERIALIZER_detectRecursions, value);
	}

	/**
	 * Configuration property:  Automatically detect POJO recursions.
	 * 
	 * <p>
	 * Shortcut for calling <code>detectRecursions(<jk>true</jk>)</code>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_detectRecursions}
	 * </ul>
	 * 
	 * @return This object (for method chaining).
	 */
	public SerializerBuilder detectRecursions() {
		return set(SERIALIZER_detectRecursions, true);
	}

	/**
	 * Configuration property:  Ignore recursion errors.
	 * 
	 * <p>
	 * If <jk>true</jk>, when we encounter the same object when serializing a tree, we set the value to <jk>null</jk>.
	 * Otherwise, an exception is thrown.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Checking for recursion can cause a small performance penalty.
	 * </ul>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_ignoreRecursions}
	 * </ul>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public SerializerBuilder ignoreRecursions(boolean value) {
		return set(SERIALIZER_ignoreRecursions, value);
	}

	/**
	 * Configuration property:  Ignore recursion errors.
	 * 
	 * <p>
	 * Shortcut for calling <code>ignoreRecursions(<jk>true</jk>)</code>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_ignoreRecursions}
	 * </ul>
	 * 
	 * @return This object (for method chaining).
	 */
	public SerializerBuilder ignoreRecursions() {
		return set(SERIALIZER_ignoreRecursions, true);
	}

	/**
	 * Configuration property:  Initial depth.
	 * 
	 * <p>
	 * The initial indentation level at the root.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_initialDepth}
	 * </ul>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default is <code>0</code>.
	 * @return This object (for method chaining).
	 */
	public SerializerBuilder initialDepth(int value) {
		return set(SERIALIZER_initialDepth, value);
	}

	/**
	 * Configuration property:  Serializer listener.
	 * 
	 * <p>
	 * Class used to listen for errors and warnings that occur during serialization.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_listener}
	 * </ul>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SerializerBuilder listener(Class<? extends SerializerListener> value) {
		return set(SERIALIZER_listener, value);
	}

	/**
	 * Configuration property:  Max serialization depth.
	 * 
	 * <p>
	 * Abort serialization if specified depth is reached in the POJO tree.
	 * <br>If this depth is exceeded, an exception is thrown.
	 * <br>This prevents stack overflows from occurring when trying to serialize models with recursive references.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_maxDepth}
	 * </ul>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default is <code>100</code>.
	 * @return This object (for method chaining).
	 */
	public SerializerBuilder maxDepth(int value) {
		return set(SERIALIZER_maxDepth, value);
	}

	/**
	 * Configuration property:  Sort arrays and collections alphabetically.
	 * 
	 * <p>
	 * Copies and sorts the contents of arrays and collections before serializing them.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_sortCollections}
	 * </ul>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public SerializerBuilder sortCollections(boolean value) {
		return set(SERIALIZER_sortCollections, value);
	}

	/**
	 * Configuration property:  Sort arrays and collections alphabetically.
	 * 
	 * <p>
	 * Shortcut for calling <code>sortCollections(<jk>true</jk>)</code>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_sortCollections}
	 * </ul>
	 * 
	 * @return This object (for method chaining).
	 */
	public SerializerBuilder sortCollections() {
		return set(SERIALIZER_sortCollections, true);
	}

	/**
	 * Configuration property:  Sort maps alphabetically.
	 * 
	 * <p>
	 * Copies and sorts the contents of maps before serializing them.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_sortMaps}
	 * </ul>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public SerializerBuilder sortMaps(boolean value) {
		return set(SERIALIZER_sortMaps, value);
	}

	/**
	 * Configuration property:  Sort maps alphabetically.
	 * 
	 * <p>
	 * Shortcut for calling <code>sortMaps(<jk>true</jk>)</code>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_sortMaps}
	 * </ul>
	 * 
	 * @return This object (for method chaining).
	 */
	public SerializerBuilder sortMaps() {
		return set(SERIALIZER_sortMaps, true);
	}

	/**
	 * Configuration property:  Trim empty lists and arrays.
	 * 
	 * <p>
	 * If <jk>true</jk>, empty list values will not be serialized to the output.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimEmptyCollections}
	 * </ul>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public SerializerBuilder trimEmptyCollections(boolean value) {
		return set(SERIALIZER_trimEmptyCollections, value);
	}

	/**
	 * Configuration property:  Trim empty lists and arrays.
	 * 
	 * <p>
	 * Shortcut for calling <code>trimEmptyCollections(<jk>true</jk>)</code>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimEmptyCollections}
	 * </ul>
	 * 
	 * @return This object (for method chaining).
	 */
	public SerializerBuilder trimEmptyCollections() {
		return set(SERIALIZER_trimEmptyCollections, true);
	}

	/**
	 * Configuration property:  Trim empty maps.
	 * 
	 * <p>
	 * If <jk>true</jk>, empty map values will not be serialized to the output.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimEmptyMaps}
	 * </ul>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public SerializerBuilder trimEmptyMaps(boolean value) {
		return set(SERIALIZER_trimEmptyMaps, value);
	}

	/**
	 * Configuration property:  Trim empty maps.
	 * 
	 * <p>
	 * Shortcut for calling <code>trimEmptyMaps(<jk>true</jk>)</code>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimEmptyMaps}
	 * </ul>
	 * 
	 * @return This object (for method chaining).
	 */
	public SerializerBuilder trimEmptyMaps() {
		return set(SERIALIZER_trimEmptyMaps, true);
	}

	/**
	 * Configuration property:  Trim null bean property values.
	 * 
	 * <p>
	 * If <jk>true</jk>, null bean values will not be serialized to the output.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimNullProperties}
	 * </ul>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default is <jk>true</jk>.
	 * @return This object (for method chaining).
	 */
	public SerializerBuilder trimNullProperties(boolean value) {
		return set(SERIALIZER_trimNullProperties, value);
	}

	/**
	 * Configuration property:  Trim strings.
	 * 
	 * <p>
	 * If <jk>true</jk>, string values will be trimmed of whitespace using {@link String#trim()} before being serialized.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimStrings}
	 * </ul>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public SerializerBuilder trimStrings(boolean value) {
		return set(SERIALIZER_trimStrings, value);
	}

	/**
	 * Configuration property:  Trim strings.
	 * 
	 * <p>
	 * Shortcut for calling <code>trimStrings(<jk>true</jk>)</code>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimStrings}
	 * </ul>
	 * 
	 * @return This object (for method chaining).
	 */
	public SerializerBuilder trimStrings() {
		return set(SERIALIZER_trimStrings, true);
	}

	/**
	 * Configuration property:  URI context bean.
	 * 
	 * <p>
	 * Bean used for resolution of URIs to absolute or root-relative form.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_uriContext}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SerializerBuilder uriContext(UriContext value) {
		return set(SERIALIZER_uriContext, value);
	}

	/**
	 * Configuration property:  URI context bean.
	 * 
	 * <p>
	 * Same as {@link #uriContext(UriContext)} but allows you to pass in a JSON string.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.uriContext(<js>"{authority:'http://localhost:10000',contextRoot:'/myContext',servletPath:'/myServlet',pathInfo:'/foo'}"</js>)
	 * 		.build();
	 * </p>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_uriContext}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SerializerBuilder uriContext(String value) {
		return set(SERIALIZER_uriContext, value);
	}

	/**
	 * Configuration property:  URI relativity.
	 * 
	 * <p>
	 * Defines what relative URIs are relative to when serializing URI/URL objects.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_uriRelativity}
	 * </ul>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default is {@link UriRelativity#RESOURCE}
	 * @return This object (for method chaining).
	 */
	public SerializerBuilder uriRelativity(UriRelativity value) {
		return set(SERIALIZER_uriRelativity, value);
	}

	/**
	 * Configuration property:  URI relativity.
	 * 
	 * <p>
	 * Same as {@link #uriRelativity(UriRelativity)} but allows you to pass in a string.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.uriRelativity(<js>"PATH_INFO"</js>)
	 * 		.build();
	 * </p>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_uriRelativity}
	 * </ul>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default is {@link UriRelativity#RESOURCE}
	 * @return This object (for method chaining).
	 */
	public SerializerBuilder uriRelativity(String value) {
		return set(SERIALIZER_uriRelativity, value);
	}
	
	/**
	 * Configuration property:  URI resolution.
	 * 
	 * <p>
	 * Defines the resolution level for URIs when serializing URI/URL objects.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_uriResolution}
	 * </ul>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default is {@link UriResolution#NONE}
	 * @return This object (for method chaining).
	 */
	public SerializerBuilder uriResolution(UriResolution value) {
		return set(SERIALIZER_uriResolution, value);
	}

	/**
	 * Configuration property:  URI resolution.
	 * 
	 * <p>
	 * Same as {@link #uriResolution(UriResolution)} but allows you to pass in a string.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.uriResolution(<js>"ROOT_RELATIVE"</js>)
	 * 		.build();
	 * </p>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_uriResolution}
	 * </ul>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default is {@link UriResolution#NONE}
	 * @return This object (for method chaining).
	 */
	public SerializerBuilder uriResolution(String value) {
		return set(SERIALIZER_uriResolution, value);
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder beansRequireDefaultConstructor(boolean value) {
		super.beansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder beansRequireDefaultConstructor() {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder beansRequireSerializable(boolean value) {
		super.beansRequireSerializable(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder beansRequireSerializable() {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder beansRequireSettersForGetters(boolean value) {
		super.beansRequireSettersForGetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder beansRequireSettersForGetters() {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder beansRequireSomeProperties(boolean value) {
		super.beansRequireSomeProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder beanMapPutReturnsOldValue(boolean value) {
		super.beanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder beanMapPutReturnsOldValue() {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder useJavaBeanIntrospector(boolean value) {
		super.useJavaBeanIntrospector(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder useJavaBeanIntrospector() {
		super.useJavaBeanIntrospector();
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder useInterfaceProxies(boolean value) {
		super.useInterfaceProxies(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder ignoreUnknownBeanProperties(boolean value) {
		super.ignoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder ignoreUnknownBeanProperties() {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder ignoreUnknownNullBeanProperties(boolean value) {
		super.ignoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder ignorePropertiesWithoutSetters(boolean value) {
		super.ignorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
		super.ignoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder ignoreInvocationExceptionsOnGetters() {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
		super.ignoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder ignoreInvocationExceptionsOnSetters() {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder sortProperties(boolean value) {
		super.sortProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder notBeanPackages(Object...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder notBeanPackages(String...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder notBeanPackages(boolean append, Object...values) {
		super.notBeanPackages(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder notBeanPackagesRemove(Object...values) {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder notBeanClasses(Class<?>...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder notBeanClasses(boolean append, Object...values) {
		super.notBeanClasses(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder notBeanClassesRemove(Object...values) {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder beanFilters(Object...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder beanFilters(Class<?>...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder beanFilters(boolean append, Object...values) {
		super.beanFilters(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder beanFiltersRemove(Object...values) {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder pojoSwaps(Object...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder pojoSwaps(Class<?>...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder pojoSwaps(boolean append, Object...values) {
		super.pojoSwaps(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder pojoSwapsRemove(Object...values) {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder implClasses(Map<String,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public <T> SerializerBuilder implClass(Class<T> interfaceClass, Class<? extends T> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder beanDictionary(Object...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder beanDictionary(Class<?>...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder beanDictionary(boolean append, Object...values) {
		super.beanDictionary(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder beanDictionaryRemove(Object...values) {
		super.beanDictionaryRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder beanTypePropertyName(String value) {
		super.beanTypePropertyName(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SerializerBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder set(boolean append, String name, Object value) {
		super.set(append, name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder addTo(String name, String key, Object value) {
		super.addTo(name, key, value);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}
	
	@Override /* Context */
	public Serializer build() {
		return null;
	}
}