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

import static org.apache.juneau.serializer.OutputStreamSerializer.*;
import static org.apache.juneau.serializer.WriterSerializer.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Builder class for creating instances of {@link SerializerGroup}.
 */
public class SerializerGroupBuilder extends BeanTraverseBuilder {

	private final AList<Object> serializers;

	/**
	 * Create an empty serializer group builder.
	 */
	public SerializerGroupBuilder() {
		this.serializers = AList.of();
	}

	/**
	 * Clone an existing serializer group builder.
	 *
	 * @param copyFrom The serializer group that we're copying settings and serializers from.
	 */
	public SerializerGroupBuilder(SerializerGroup copyFrom) {
		super(copyFrom.getPropertyStore());
		this.serializers = AList.of().arev(copyFrom.getSerializers());
	}

	/**
	 * Registers the specified serializers with this group.
	 *
	 * @param s The serializers to append to this group.
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder append(Class<?>...s) {
		serializers.arev(s);
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
		serializers.arev(s);
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
		serializers.arev(s);
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
		serializers.arev(s);
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
		PropertyStore ps = getPropertyStore();
		for (Object s : serializers) {
			Class<? extends Serializer> c = null;
			if (s instanceof Class) {
				c = (Class<? extends Serializer>)s;
				l.add(ContextCache.INSTANCE.create(c, ps));
			} else {
				l.add((Serializer)s);
			}
		}
		return new SerializerGroup(getPropertyStore(), ArrayUtils.toReverseArray(Serializer.class, l));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Add <js>"_type"</js> properties when needed.
	 *
	 * <p>
	 * Shortcut for calling <code>addBeanTypes(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_addBeanTypes}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SerializerGroupBuilder addBeanTypes() {
		return set(SERIALIZER_addBeanTypes, true);
	}

	/**
	 * Configuration property:  Add type attribute to root nodes.
	 *
	 * <p>
	 * Shortcut for calling <code>addRootType(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_addRootType}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SerializerGroupBuilder addRootType() {
		return set(SERIALIZER_addRootType, true);
	}

	/**
	 * Configuration property:  Don't trim null bean property values.
	 *
	 * <p>
	 * When enabled, null bean values will be serialized to the output.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_keepNullProperties}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SerializerGroupBuilder keepNullProperties() {
		return set(SERIALIZER_keepNullProperties, true);
	}

	/**
	 * Configuration property:  Serializer listener.
	 *
	 * <p>
	 * Class used to listen for errors and warnings that occur during serialization.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_listener}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SerializerGroupBuilder listener(Class<? extends SerializerListener> value) {
		return set(SERIALIZER_listener, value);
	}

	/**
	 * Configuration property:  Sort arrays and collections alphabetically.
	 *
	 * <p>
	 * Shortcut for calling <code>sortCollections(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_sortCollections}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SerializerGroupBuilder sortCollections() {
		return set(SERIALIZER_sortCollections, true);
	}

	/**
	 * Configuration property:  Sort maps alphabetically.
	 *
	 * <p>
	 * Shortcut for calling <code>sortMaps(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_sortMaps}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SerializerGroupBuilder sortMaps() {
		return set(SERIALIZER_sortMaps, true);
	}

	/**
	 * Configuration property:  Trim empty lists and arrays.
	 *
	 * <p>
	 * Shortcut for calling <code>trimEmptyCollections(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimEmptyCollections}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SerializerGroupBuilder trimEmptyCollections() {
		return set(SERIALIZER_trimEmptyCollections, true);
	}

	/**
	 * Configuration property:  Trim empty maps.
	 *
	 * <p>
	 * Shortcut for calling <code>trimEmptyMaps(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimEmptyMaps}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SerializerGroupBuilder trimEmptyMaps() {
		return set(SERIALIZER_trimEmptyMaps, true);
	}

	/**
	 * Configuration property:  Trim strings.
	 *
	 * <p>
	 * Shortcut for calling <code>trimStrings(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimStrings}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SerializerGroupBuilder trimStrings() {
		return set(SERIALIZER_trimStrings, true);
	}

	/**
	 * Configuration property:  URI context bean.
	 *
	 * <p>
	 * Bean used for resolution of URIs to absolute or root-relative form.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_uriContext}
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SerializerGroupBuilder uriContext(UriContext value) {
		return set(SERIALIZER_uriContext, value);
	}

	/**
	 * Configuration property:  URI relativity.
	 *
	 * <p>
	 * Defines what relative URIs are relative to when serializing URI/URL objects.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_uriRelativity}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link UriRelativity#RESOURCE}
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SerializerGroupBuilder uriRelativity(UriRelativity value) {
		return set(SERIALIZER_uriRelativity, value);
	}

	/**
	 * Configuration property:  URI resolution.
	 *
	 * <p>
	 * Defines the resolution level for URIs when serializing URI/URL objects.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_uriResolution}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link UriResolution#NONE}
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SerializerGroupBuilder uriResolution(UriResolution value) {
		return set(SERIALIZER_uriResolution, value);
	}

	//--- OutputStreamSerializer ---

	/**
	 * Configuration property:  Binary string format.
	 *
	 * <p>
	 * When using the {@link Serializer#serializeToString(Object)} method on stream-based serializers, this defines the format to use
	 * when converting the resulting byte array to a string.
	 *
	 * <ul class='javatree'>
	 * 	<li class='jf'>{@link OutputStreamSerializer#OSSERIALIZER_binaryFormat}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link BinaryFormat#HEX}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SerializerGroupBuilder binaryOutputFormat(BinaryFormat value) {
		return set(OSSERIALIZER_binaryFormat, value);
	}

	//--- WriterSerializer ---

	/**
	 * Configuration property: File charset.
	 *
	 * <p>
	 * The character set to use for writing Files to the file system.
	 *
	 * <p>
	 * Used when passing in files to {@link Serializer#serialize(Object, Object)}.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link WriterSerializer#WSERIALIZER_fileCharset}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is the system JVM setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SerializerGroupBuilder fileCharset(Charset value) {
		return set(WSERIALIZER_fileCharset, value);
	}

	/**
	 * Configuration property:  Maximum indentation.
	 *
	 * <p>
	 * Specifies the maximum indentation level in the serialized document.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link WriterSerializer#WSERIALIZER_maxIndent}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <c>100</c>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SerializerGroupBuilder maxIndent(int value) {
		return set(WSERIALIZER_maxIndent, value);
	}

	/**
	 * Configuration property:  Quote character.
	 *
	 * <p>
	 * This is the character used for quoting attributes and values.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link WriterSerializer#WSERIALIZER_quoteChar}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <js>'"'</js>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SerializerGroupBuilder quoteChar(char value) {
		return set(WSERIALIZER_quoteChar, value);
	}

	/**
	 * Configuration property:  Quote character.
	 *
	 * <p>
	 * Shortcut for calling <code>quoteChar(<js>'\''</js>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link WriterSerializer#WSERIALIZER_quoteChar}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SerializerGroupBuilder sq() {
		return quoteChar('\'');
	}

	/**
	 * Configuration property: Output stream charset.
	 *
	 * <p>
	 * The character set to use when writing to OutputStreams.
	 *
	 * <p>
	 * Used when passing in output streams and byte arrays to {@link WriterSerializer#serialize(Object, Object)}.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link WriterSerializer#WSERIALIZER_streamCharset}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is the system JVM setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SerializerGroupBuilder streamCharset(Charset value) {
		return set(WSERIALIZER_streamCharset, value);
	}

	/**
	 * Configuration property:  Use whitespace.
	 *
	 * <p>
	 * Shortcut for calling <code>useWhitespace(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link WriterSerializer#WSERIALIZER_useWhitespace}
	 * </ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SerializerGroupBuilder useWhitespace() {
		return set(WSERIALIZER_useWhitespace, true);
	}

	/**
	 * Configuration property:  Use whitespace.
	 *
	 * <p>
	 * Shortcut for calling <code>useWhitespace(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link WriterSerializer#WSERIALIZER_useWhitespace}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SerializerGroupBuilder ws() {
		return useWhitespace();
	}

	// <FluentSetters>

	@Override /* GENERATED - ContextBuilder */
	public SerializerGroupBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public SerializerGroupBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public SerializerGroupBuilder appendTo(String name, Object value) {
		super.appendTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public SerializerGroupBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public SerializerGroupBuilder applyAnnotations(java.lang.Class<?>...fromClasses) {
		super.applyAnnotations(fromClasses);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public SerializerGroupBuilder applyAnnotations(Method...fromMethods) {
		super.applyAnnotations(fromMethods);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public SerializerGroupBuilder applyAnnotations(AnnotationList al, VarResolverSession r) {
		super.applyAnnotations(al, r);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public SerializerGroupBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public SerializerGroupBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public SerializerGroupBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public SerializerGroupBuilder prependTo(String name, Object value) {
		super.prependTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public SerializerGroupBuilder putAllTo(String name, Object value) {
		super.putAllTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public SerializerGroupBuilder putTo(String name, String key, Object value) {
		super.putTo(name, key, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public SerializerGroupBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public SerializerGroupBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public SerializerGroupBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public SerializerGroupBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder annotations(Annotation...values) {
		super.annotations(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder beanInterceptor(Class<?> on, Class<? extends org.apache.juneau.transform.BeanInterceptor<?>> value) {
		super.beanInterceptor(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder beanMapPutReturnsOldValue() {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder beansDontRequireSomeProperties() {
		super.beansDontRequireSomeProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder beansRequireDefaultConstructor() {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder beansRequireSerializable() {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder beansRequireSettersForGetters() {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder beanProperties(Map<String,Object> values) {
		super.beanProperties(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder beanProperties(Class<?> beanClass, String properties) {
		super.beanProperties(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder beanProperties(String beanClassName, String properties) {
		super.beanProperties(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder beanPropertiesReadOnly(Map<String,Object> values) {
		super.beanPropertiesReadOnly(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder beanPropertiesReadOnly(Class<?> beanClass, String properties) {
		super.beanPropertiesReadOnly(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder beanPropertiesReadOnly(String beanClassName, String properties) {
		super.beanPropertiesReadOnly(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder beanPropertiesWriteOnly(Map<String,Object> values) {
		super.beanPropertiesWriteOnly(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder beanPropertiesWriteOnly(Class<?> beanClass, String properties) {
		super.beanPropertiesWriteOnly(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder beanPropertiesWriteOnly(String beanClassName, String properties) {
		super.beanPropertiesWriteOnly(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder beanPropertiesExcludes(Map<String,Object> values) {
		super.beanPropertiesExcludes(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder beanPropertiesExcludes(Class<?> beanClass, String properties) {
		super.beanPropertiesExcludes(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder beanPropertiesExcludes(String beanClassName, String properties) {
		super.beanPropertiesExcludes(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder dictionary(Object...values) {
		super.dictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
		super.dictionaryOn(on, values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder dontSilentlyIgnoreMissingSetters() {
		super.dontSilentlyIgnoreMissingSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder dontIgnoreTransientFields() {
		super.dontIgnoreTransientFields();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder dontIgnoreUnknownNullBeanProperties() {
		super.dontIgnoreUnknownNullBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder dontUseInterfaceProxies() {
		super.dontUseInterfaceProxies();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> SerializerGroupBuilder example(Class<T> pojoClass, T o) {
		super.example(pojoClass, o);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> SerializerGroupBuilder example(Class<T> pojoClass, String json) {
		super.example(pojoClass, json);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder fluentSetters() {
		super.fluentSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder fluentSetters(Class<?> on) {
		super.fluentSetters(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder ignoreInvocationExceptionsOnGetters() {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder ignoreInvocationExceptionsOnSetters() {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder ignoreUnknownBeanProperties() {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder implClass(Class<?> interfaceClass, Class<?> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder implClasses(Map<Class<?>,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder interfaceClass(Class<?> on, Class<?> value) {
		super.interfaceClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder interfaces(java.lang.Class<?>...value) {
		super.interfaces(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder notBeanPackages(Object...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder sortProperties(java.lang.Class<?>...on) {
		super.sortProperties(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder stopClass(Class<?> on, Class<?> value) {
		super.stopClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder swaps(Object...values) {
		super.swaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder typeName(Class<?> on, String value) {
		super.typeName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder typePropertyName(String value) {
		super.typePropertyName(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder typePropertyName(Class<?> on, String value) {
		super.typePropertyName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder useEnumNames() {
		super.useEnumNames();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerGroupBuilder useJavaBeanIntrospector() {
		super.useJavaBeanIntrospector();
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public SerializerGroupBuilder detectRecursions() {
		super.detectRecursions();
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public SerializerGroupBuilder ignoreRecursions() {
		super.ignoreRecursions();
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public SerializerGroupBuilder initialDepth(int value) {
		super.initialDepth(value);
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public SerializerGroupBuilder maxDepth(int value) {
		super.maxDepth(value);
		return this;
	}

	// </FluentSetters>
}
