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
import org.apache.juneau.annotation.*;
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
	public SerializerBuilder(PropertyStore2 ps) {
		super(ps);
	}


	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	/**
	 * <b>Configuration property:</b>  Max serialization depth.
	 *
	 * <p>
	 * Abort serialization if specified depth is reached in the POJO tree.
	 * If this depth is exceeded, an exception is thrown.
	 * This prevents stack overflows from occurring when trying to serialize models with recursive references.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>SERIALIZER_maxDepth</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_maxDepth
	 */
	public SerializerBuilder maxDepth(int value) {
		return set(SERIALIZER_maxDepth, value);
	}

	/**
	 * <b>Configuration property:</b>  Initial depth.
	 *
	 * <p>
	 * The initial indentation level at the root.
	 * Useful when constructing document fragments that need to be indented at a certain level.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>SERIALIZER_initialDepth</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_initialDepth
	 */
	public SerializerBuilder initialDepth(int value) {
		return set(SERIALIZER_initialDepth, value);
	}

	/**
	 * <b>Configuration property:</b>  Automatically detect POJO recursions.
	 *
	 * <p>
	 * Specifies that recursions should be checked for during serialization.
	 *
	 * <p>
	 * Recursions can occur when serializing models that aren't true trees, but rather contain loops.
	 *
	 * <p>
	 * The behavior when recursions are detected depends on the value for
	 * {@link Serializer#SERIALIZER_ignoreRecursions}.
	 *
	 * <p>
	 * For example, if a model contains the links A-&gt;B-&gt;C-&gt;A, then the JSON generated will look like
	 * the following when <jsf>SERIALIZER_ignoreRecursions</jsf> is <jk>true</jk>...
	 * <code>{A:{B:{C:null}}}</code>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>SERIALIZER_detectRecursions</jsf>, value)</code>.
	 * 	<li>Checking for recursion can cause a small performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_detectRecursions
	 */
	public SerializerBuilder detectRecursions(boolean value) {
		return set(SERIALIZER_detectRecursions, value);
	}

	/**
	 * <b>Configuration property:</b>  Ignore recursion errors.
	 *
	 * <p>
	 * Used in conjunction with {@link Serializer#SERIALIZER_detectRecursions}.
	 * Setting is ignored if <jsf>SERIALIZER_detectRecursions</jsf> is <jk>false</jk>.
	 *
	 * <p>
	 * If <jk>true</jk>, when we encounter the same object when serializing a tree, we set the value to <jk>null</jk>.
	 * Otherwise, an exception is thrown.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>SERIALIZER_ignoreRecursions</jsf>, value)</code>.
	 * 	<li>Checking for recursion can cause a small performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_ignoreRecursions
	 */
	public SerializerBuilder ignoreRecursions(boolean value) {
		return set(SERIALIZER_ignoreRecursions, value);
	}

	/**
	 * <b>Configuration property:</b>  Use whitespace.
	 *
	 * <p>
	 * If <jk>true</jk>, newlines and indentation and spaces are added to the output to improve readability.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>SERIALIZER_useWhitespace</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_useWhitespace
	 */
	public SerializerBuilder useWhitespace(boolean value) {
		return set(SERIALIZER_useWhitespace, value);
	}

	/**
	 * Shortcut for calling <code>useWhitespace(<jk>true</jk>)</code>.
	 *
	 * @return This object (for method chaining).
	 */
	public SerializerBuilder ws() {
		return useWhitespace(true);
	}

	/**
	 * <b>Configuration property:</b>  Maximum indentation.
	 *
	 * <p>
	 * Specifies the maximum indentation level in the serialized document.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>SERIALIZER_maxIndent</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_maxIndent
	 */
	public SerializerBuilder maxIndent(int value) {
		return set(SERIALIZER_maxIndent, value);
	}

	/**
	 * <b>Configuration property:</b>  Add <js>"_type"</js> properties when needed.
	 *
	 * <p>
	 * If <jk>true</jk>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * through reflection.
	 * This is used to recreate the correct objects during parsing if the object types cannot be inferred.
	 * For example, when serializing a {@code Map<String,Object>} field, where the bean class cannot be determined from
	 * the value type.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>SERIALIZER_addBeanTypeProperties</jsf>, value)</code>.
	 * 	<li>Checking for recursion can cause a small performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_addBeanTypeProperties
	 */
	public SerializerBuilder addBeanTypeProperties(boolean value) {
		return set(SERIALIZER_addBeanTypeProperties, value);
	}

	/**
	 * <b>Configuration property:</b>  Quote character.
	 *
	 * <p>
	 * This is the character used for quoting attributes and values.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>SERIALIZER_quoteChar</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_quoteChar
	 */
	public SerializerBuilder quoteChar(char value) {
		return set(SERIALIZER_quoteChar, value);
	}

	/**
	 * Shortcut for calling <code>quoteChar(<js>'\''</js>)</code>.
	 *
	 * @return This object (for method chaining).
	 */
	public SerializerBuilder sq() {
		return quoteChar('\'');
	}

	/**
	 * <b>Configuration property:</b>  Trim null bean property values.
	 *
	 * <p>
	 * If <jk>true</jk>, null bean values will not be serialized to the output.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>SERIALIZER_trimNullProperties</jsf>, value)</code>.
	 * 	<li>Enabling this setting has the following effects on parsing:
	 * 	<ul>
	 * 		<li>Map entries with <jk>null</jk> values will be lost.
	 * 	</ul>
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_trimNullProperties
	 */
	public SerializerBuilder trimNullProperties(boolean value) {
		return set(SERIALIZER_trimNullProperties, value);
	}

	/**
	 * <b>Configuration property:</b>  Trim empty lists and arrays.
	 *
	 * <p>
	 * If <jk>true</jk>, empty list values will not be serialized to the output.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>SERIALIZER_trimEmptyCollections</jsf>, value)</code>.
	 * 	<li>Enabling this setting has the following effects on parsing:
	 * 	<ul>
	 * 		<li>Map entries with empty list values will be lost.
	 * 		<li>Bean properties with empty list values will not be set.
	 * 	</ul>
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_trimEmptyCollections
	 */
	public SerializerBuilder trimEmptyCollections(boolean value) {
		return set(SERIALIZER_trimEmptyCollections, value);
	}

	/**
	 * <b>Configuration property:</b>  Trim empty maps.
	 *
	 * <p>
	 * If <jk>true</jk>, empty map values will not be serialized to the output.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>SERIALIZER_trimEmptyMaps</jsf>, value)</code>.
	 * 	<li>Enabling this setting has the following effects on parsing:
	 * 	<ul>
	 * 		<li>Bean properties with empty map values will not be set.
	 * 	</ul>
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_trimEmptyMaps
	 */
	public SerializerBuilder trimEmptyMaps(boolean value) {
		return set(SERIALIZER_trimEmptyMaps, value);
	}

	/**
	 * <b>Configuration property:</b>  Trim strings.
	 *
	 * <p>
	 * If <jk>true</jk>, string values will be trimmed of whitespace using {@link String#trim()} before being serialized.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>SERIALIZER_trimStrings</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_trimStrings
	 */
	public SerializerBuilder trimStrings(boolean value) {
		return set(SERIALIZER_trimStrings, value);
	}

	/**
	 * <b>Configuration property:</b>  URI context bean.
	 *
	 * <p>
	 * Bean used for resolution of URIs to absolute or root-relative form.
	 *
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	<js>"{authority:'http://localhost:10000',contextRoot:'/myContext',servletPath:'/myServlet',pathInfo:'/foo'}"</js>
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>SERIALIZER_uriContext</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_uriContext
	 */
	public SerializerBuilder uriContext(UriContext value) {
		return set(SERIALIZER_uriContext, value);
	}

	/**
	 * <b>Configuration property:</b>  URI resolution.
	 *
	 * <p>
	 * Defines the resolution level for URIs when serializing any of the following:
	 * <ul>
	 * 	<li>{@link java.net.URI}
	 * 	<li>{@link java.net.URL}
	 * 	<li>Properties annotated with {@link org.apache.juneau.annotation.URI @URI}
	 * </ul>
	 *
	 * <p>
	 * Possible values are:
	 * <ul>
	 * 	<li>{@link UriResolution#ABSOLUTE}
	 * 		- Resolve to an absolute URL (e.g. <js>"http://host:port/context-root/servlet-path/path-info"</js>).
	 * 	<li>{@link UriResolution#ROOT_RELATIVE}
	 * 		- Resolve to a root-relative URL (e.g. <js>"/context-root/servlet-path/path-info"</js>).
	 * 	<li>{@link UriResolution#NONE}
	 * 		- Don't do any URL resolution.
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>SERIALIZER_uriResolution</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_uriResolution
	 */
	public SerializerBuilder uriResolution(UriResolution value) {
		return set(SERIALIZER_uriResolution, value);
	}

	/**
	 * <b>Configuration property:</b>  URI relativity.
	 *
	 * <p>
	 * Defines what relative URIs are relative to when serializing any of the following:
	 * <ul>
	 * 	<li>{@link java.net.URI}
	 * 	<li>{@link java.net.URL}
	 * 	<li>Properties annotated with {@link org.apache.juneau.annotation.URI @URI}
	 * </ul>
	 *
	 * <p>
	 * Possible values are:
	 * <ul>
	 * 	<li>{@link UriRelativity#RESOURCE}
	 * 		- Relative URIs should be considered relative to the servlet URI.
	 * 	<li>{@link UriRelativity#PATH_INFO}
	 * 		- Relative URIs should be considered relative to the request URI.
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>SERIALIZER_uriRelativity</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_uriRelativity
	 */
	public SerializerBuilder uriRelativity(UriRelativity value) {
		return set(SERIALIZER_uriRelativity, value);
	}

	/**
	 * <b>Configuration property:</b>  Sort arrays and collections alphabetically.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>SERIALIZER_sortCollections</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_sortCollections
	 */
	public SerializerBuilder sortCollections(boolean value) {
		return set(SERIALIZER_sortCollections, value);
	}

	/**
	 * <b>Configuration property:</b>  Sort maps alphabetically.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>SERIALIZER_sortMaps</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_sortMaps
	 */
	public SerializerBuilder sortMaps(boolean value) {
		return set(SERIALIZER_sortMaps, value);
	}

	/**
	 * <b>Configuration property:</b>  Abridged output.
	 *
	 * <p>
	 * When enabled, it is assumed that the parser knows the exact Java POJO type being parsed, and therefore top-level
	 * type information that might normally be included to determine the data type will not be serialized.
	 *
	 * <p>
	 * For example, when serializing a POJO with a {@link Bean#typeName()} value, a <js>"_type"</js> will be added when
	 * this setting is disabled, but not added when it is enabled.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see Serializer#SERIALIZER_sortMaps
	 */
	public SerializerBuilder abridged(boolean value) {
		return set(SERIALIZER_abridged, value);
	}

	/**
	 * <b>Configuration property:</b>  Serializer listener.
	 *
	 * <p>
	 * Class used to listen for errors and warnings that occur during serialization.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SerializerBuilder listener(Class<? extends SerializerListener> value) {
		return set(SERIALIZER_listener, value);
	}

	@Override /* ContextBuilder */
	public SerializerBuilder beansRequireDefaultConstructor(boolean value) {
		super.beansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder beansRequireSerializable(boolean value) {
		super.beansRequireSerializable(value);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder beansRequireSettersForGetters(boolean value) {
		super.beansRequireSettersForGetters(value);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder beansRequireSomeProperties(boolean value) {
		super.beansRequireSomeProperties(value);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder beanMapPutReturnsOldValue(boolean value) {
		super.beanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder methodVisibility(Visibility value) {
		super.methodVisibility(value);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder useJavaBeanIntrospector(boolean value) {
		super.useJavaBeanIntrospector(value);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder useInterfaceProxies(boolean value) {
		super.useInterfaceProxies(value);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder ignoreUnknownBeanProperties(boolean value) {
		super.ignoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder ignoreUnknownNullBeanProperties(boolean value) {
		super.ignoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder ignorePropertiesWithoutSetters(boolean value) {
		super.ignorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
		super.ignoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
		super.ignoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder sortProperties(boolean value) {
		super.sortProperties(value);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder notBeanPackages(String...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder notBeanPackages(Collection<String> values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder setNotBeanPackages(String...values) {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder setNotBeanPackages(Collection<String> values) {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder removeNotBeanPackages(String...values) {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder removeNotBeanPackages(Collection<String> values) {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder notBeanClasses(Class<?>...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder notBeanClasses(Collection<Class<?>> values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder setNotBeanClasses(Class<?>...values) {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder setNotBeanClasses(Collection<Class<?>> values) {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder removeNotBeanClasses(Class<?>...values) {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder removeNotBeanClasses(Collection<Class<?>> values) {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder beanFilters(Class<?>...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder beanFilters(Collection<Class<?>> values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder setBeanFilters(Class<?>...values) {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder setBeanFilters(Collection<Class<?>> values) {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder removeBeanFilters(Class<?>...values) {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder removeBeanFilters(Collection<Class<?>> values) {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder pojoSwaps(Class<?>...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder pojoSwaps(Collection<Class<?>> values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder setPojoSwaps(Class<?>...values) {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder setPojoSwaps(Collection<Class<?>> values) {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder removePojoSwaps(Class<?>...values) {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder removePojoSwaps(Collection<Class<?>> values) {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder implClasses(Map<String,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* ContextBuilder */
	public <T> SerializerBuilder implClass(Class<T> interfaceClass, Class<? extends T> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder includeProperties(Map<String,String> values) {
		super.includeProperties(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder includeProperties(String beanClassName, String properties) {
		super.includeProperties(beanClassName, properties);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder includeProperties(Class<?> beanClass, String properties) {
		super.includeProperties(beanClass, properties);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder excludeProperties(Map<String,String> values) {
		super.excludeProperties(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder excludeProperties(String beanClassName, String properties) {
		super.excludeProperties(beanClassName, properties);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder excludeProperties(Class<?> beanClass, String properties) {
		super.excludeProperties(beanClass, properties);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder beanDictionary(Class<?>...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder beanDictionary(Collection<Class<?>> values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder setBeanDictionary(Class<?>...values) {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder setBeanDictionary(Collection<Class<?>> values) {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder removeFromBeanDictionary(Class<?>...values) {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder removeFromBeanDictionary(Collection<Class<?>> values) {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder beanTypePropertyName(String value) {
		super.beanTypePropertyName(value);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder defaultParser(Class<?> value) {
		super.defaultParser(value);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* ContextBuilder */
	public SerializerBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* ContextBuilder */
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
	public SerializerBuilder apply(PropertyStore2 copyFrom) {
		super.apply(copyFrom);
		return this;
	}
	
	@Override /* Context */
	public Serializer build() {
		return null;
	}
}