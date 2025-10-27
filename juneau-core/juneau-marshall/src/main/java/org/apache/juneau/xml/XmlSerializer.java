/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.xml;

import static org.apache.juneau.collections.JsonMap.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.annotation.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.collections.*;
import org.apache.juneau.common.function.*;
import org.apache.juneau.common.reflect.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utils.*;

/**
 * Serializes POJO models to XML.
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Accept</c> types:  <bc>text/xml</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>text/xml</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * See the {@link JsonSerializer} class for details on how Java models map to JSON.
 *
 * <p>
 * For example, the following JSON...
 * <p class='bjson'>
 * 	{
 * 		name:<js>'John Smith'</js>,
 * 		address: {
 * 			streetAddress: <js>'21 2nd Street'</js>,
 * 			city: <js>'New York'</js>,
 * 			state: <js>'NY'</js>,
 * 			postalCode: <js>10021</js>
 * 		},
 * 		phoneNumbers: [
 * 			<js>'212 555-1111'</js>,
 * 			<js>'212 555-2222'</js>
 * 		],
 * 		additionalInfo: <jk>null</jk>,
 * 		remote: <jk>false</jk>,
 * 		height: <js>62.4</js>,
 * 		<js>'fico score'</js>:  <js>' &gt; 640'</js>
 * 	}
 * <p>
 * 	...maps to the following XML using the default serializer...
 * <p class='bxml'>
 * 	<xt>&lt;object&gt;</xt>
 * 		<xt>&lt;name&gt;</xt>John Smith<xt>&lt;/name&gt;</xt>
 * 		<xt>&lt;address&gt;</xt>
 * 			<xt>&lt;streetAddress&gt;</xt>21 2nd Street<xt>&lt;/streetAddress&gt;</xt>
 * 			<xt>&lt;city&gt;</xt>New York<xt>&lt;/city&gt;</xt>
 * 			<xt>&lt;state&gt;</xt>NY<xt>&lt;/state&gt;</xt>
 * 			<xt>&lt;postalCode&gt;</xt>10021<xt>&lt;/postalCode&gt;</xt>
 * 		<xt>&lt;/address&gt;</xt>
 * 		<xt>&lt;phoneNumbers&gt;</xt>
 * 			<xt>&lt;string&gt;</xt>212 555-1111<xt>&lt;/string&gt;</xt>
 * 			<xt>&lt;string&gt;</xt>212 555-2222<xt>&lt;/string&gt;</xt>
 * 		<xt>&lt;/phoneNumbers&gt;</xt>
 * 		<xt>&lt;additionalInfo</xt> <xa>_type</xa>=<xs>'null'</xs><xt>&gt;&lt;/additionalInfo&gt;</xt>
 * 		<xt>&lt;remote&gt;</xt>false<xt>&lt;/remote&gt;</xt>
 * 		<xt>&lt;height&gt;</xt>62.4<xt>&lt;/height&gt;</xt>
 * 		<xt>&lt;fico_x0020_score&gt;</xt> &amp;gt; 640<xt>&lt;/fico_x0020_score&gt;</xt>
 * 	<xt>&lt;/object&gt;</xt>
 *
 * <p>
 * An additional "add-json-properties" mode is also provided to prevent loss of JSON data types...
 * <p class='bxml'>
 * 	<xt>&lt;object&gt;</xt>
 * 		<xt>&lt;name</xt> <xa>_type</xa>=<xs>'string'</xs><xt>&gt;</xt>John Smith<xt>&lt;/name&gt;</xt>
 * 		<xt>&lt;address</xt> <xa>_type</xa>=<xs>'object'</xs><xt>&gt;</xt>
 * 			<xt>&lt;streetAddress</xt> <xa>_type</xa>=<xs>'string'</xs><xt>&gt;</xt>21 2nd Street<xt>&lt;/streetAddress&gt;</xt>
 * 			<xt>&lt;city</xt> <xa>_type</xa>=<xs>'string'</xs><xt>&gt;</xt>New York<xt>&lt;/city&gt;</xt>
 * 			<xt>&lt;state</xt> <xa>_type</xa>=<xs>'string'</xs><xt>&gt;</xt>NY<xt>&lt;/state&gt;</xt>
 * 			<xt>&lt;postalCode</xt> <xa>_type</xa>=<xs>'number'</xs><xt>&gt;</xt>10021<xt>&lt;/postalCode&gt;</xt>
 * 		<xt>&lt;/address&gt;</xt>
 * 		<xt>&lt;phoneNumbers</xt> <xa>_type</xa>=<xs>'array'</xs><xt>&gt;</xt>
 * 			<xt>&lt;string&gt;</xt>212 555-1111<xt>&lt;/string&gt;</xt>
 * 			<xt>&lt;string&gt;</xt>212 555-2222<xt>&lt;/string&gt;</xt>
 * 		<xt>&lt;/phoneNumbers&gt;</xt>
 * 		<xt>&lt;additionalInfo</xt> <xa>_type</xa>=<xs>'null'</xs><xt>&gt;&lt;/additionalInfo&gt;</xt>
 * 		<xt>&lt;remote</xt> <xa>_type</xa>=<xs>'boolean'</xs><xt>&gt;</xt>false<xt>&lt;/remote&gt;</xt>
 * 		<xt>&lt;height</xt> <xa>_type</xa>=<xs>'number'</xs><xt>&gt;</xt>62.4<xt>&lt;/height&gt;</xt>
 * 		<xt>&lt;fico_x0020_score</xt> <xa>_type</xa>=<xs>'string'</xs><xt>&gt;</xt> &amp;gt; 640<xt>&lt;/fico_x0020_score&gt;</xt>
 * 	<xt>&lt;/object&gt;</xt>
 * </p>
 *
 * <p>
 * This serializer provides several serialization options.
 * Typically, one of the predefined <jsf>DEFAULT</jsf> serializers will be sufficient.
 * However, custom serializers can be constructed to fine-tune behavior.
 *
 * <p>
 * If an attribute name contains any non-valid XML element characters, they will be escaped using standard
 * {@code _x####_} notation.
 *
 * <h5 class='topic'>Behavior-specific subclasses</h5>
 * <p>
 * The following direct subclasses are provided for convenience:
 * <ul>
 * 	<li>{@link Sq} - Default serializer, single quotes.
 * 	<li>{@link SqReadable} - Default serializer, single quotes, whitespace added.
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/XmlBasics">XML Basics</a>

 * </ul>
 */
public class XmlSerializer extends WriterSerializer implements XmlMetaProvider {
	/**
	 * Builder class.
	 */
	public static class Builder extends WriterSerializer.Builder {

		private static final Cache<HashKey,XmlSerializer> CACHE = Cache.of(HashKey.class, XmlSerializer.class).build();

		boolean addBeanTypesXml, addNamespaceUrisToRoot, disableAutoDetectNamespaces, disableJsonTags, enableNamespaces;
		Namespace defaultNamespace;
		List<Namespace> namespaces;
		String textNodeDelimiter;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			produces("text/xml");
			addBeanTypesXml = env("XmlSerializer.addBeanTypes", false);
			addNamespaceUrisToRoot = env("XmlSerializer.addNamespaceUrisToRoot", false);
			disableAutoDetectNamespaces = env("XmlSerializer.disableAutoDetectNamespaces", false);
			disableJsonTags = env("XmlSerializer.disableJsonTags", false);
			enableNamespaces = env("XmlSerializer.enableNamespaces", false);
			defaultNamespace = null;
			namespaces = null;
			textNodeDelimiter = env("XmlSerializer.textNodeDelimiter", "");
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
			addBeanTypesXml = copyFrom.addBeanTypesXml;
			addNamespaceUrisToRoot = copyFrom.addNamespaceUrisToRoot;
			disableAutoDetectNamespaces = copyFrom.disableAutoDetectNamespaces;
			disableJsonTags = copyFrom.disableJsonTags;
			enableNamespaces = copyFrom.enableNamespaces;
			defaultNamespace = copyFrom.defaultNamespace;
			namespaces = copyOf(copyFrom.namespaces);
			textNodeDelimiter = copyFrom.textNodeDelimiter;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(XmlSerializer copyFrom) {
			super(copyFrom);
			addBeanTypesXml = copyFrom.addBeanTypesXml;
			addNamespaceUrisToRoot = copyFrom.addNamespaceUrlsToRoot;
			disableAutoDetectNamespaces = ! copyFrom.autoDetectNamespaces;
			disableJsonTags = ! copyFrom.addJsonTags;
			enableNamespaces = copyFrom.enableNamespaces;
			defaultNamespace = copyFrom.defaultNamespace;
			namespaces = copyFrom.namespaces.length == 0 ? null : list(copyFrom.namespaces);
			textNodeDelimiter = copyFrom.textNodeDelimiter;
		}

		@Override /* Overridden from Builder */
		public Builder accept(String value) {
			super.accept(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder addBeanTypes() {
			super.addBeanTypes();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder addBeanTypes(boolean value) {
			super.addBeanTypes(value);
			return this;
		}

		/**
		 * Add <js>"_type"</js> properties when needed.
		 *
		 * <p>
		 * If <jk>true</jk>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
		 * through reflection.
		 *
		 * <p>
		 * When present, this value overrides the {@link org.apache.juneau.serializer.Serializer.Builder#addBeanTypes()} setting and is
		 * provided to customize the behavior of specific serializers in a {@link SerializerSet}.
		 *
		 * @return This object.
		 */
		public Builder addBeanTypesXml() {
			return addBeanTypesXml(true);
		}

		/**
		 * Same as {@link #addBeanTypesXml()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Builder addBeanTypesXml(boolean value) {
			addBeanTypesXml = value;
			return this;
		}

		/**
		 * Add namespace URLs to the root element.
		 *
		 * <p>
		 * Use this setting to add {@code xmlns:x} attributes to the root element for the default and all mapped namespaces.
		 *
		 * <p>
		 * This setting is ignored if {@link #enableNamespaces()} is not enabled.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/XmlNamespaces">Namespaces</a>
		 * </ul>
		 *
		 * @return This object.
		 */
		public Builder addNamespaceUrisToRoot() {
			return addNamespaceUrisToRoot(true);
		}

		/**
		 * Same as {@link #addNamespaceUrisToRoot()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Builder addNamespaceUrisToRoot(boolean value) {
			addNamespaceUrisToRoot = value;
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder addRootType() {
			super.addRootType();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder addRootType(boolean value) {
			super.addRootType(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder annotations(Annotation...values) {
			super.annotations(values);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder apply(AnnotationWorkList work) {
			super.apply(work);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder applyAnnotations(Class<?>...from) {
			super.applyAnnotations(from);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder applyAnnotations(Object...from) {
			super.applyAnnotations(from);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanClassVisibility(Visibility value) {
			super.beanClassVisibility(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanConstructorVisibility(Visibility value) {
			super.beanConstructorVisibility(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanContext(BeanContext value) {
			super.beanContext(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanContext(BeanContext.Builder value) {
			super.beanContext(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanDictionary(java.lang.Class<?>...values) {
			super.beanDictionary(values);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanFieldVisibility(Visibility value) {
			super.beanFieldVisibility(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanInterceptor(Class<?> on, Class<? extends org.apache.juneau.swap.BeanInterceptor<?>> value) {
			super.beanInterceptor(on, value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanMapPutReturnsOldValue() {
			super.beanMapPutReturnsOldValue();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanMethodVisibility(Visibility value) {
			super.beanMethodVisibility(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanProperties(Class<?> beanClass, String properties) {
			super.beanProperties(beanClass, properties);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanProperties(Map<String,Object> values) {
			super.beanProperties(values);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanProperties(String beanClassName, String properties) {
			super.beanProperties(beanClassName, properties);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanPropertiesExcludes(Class<?> beanClass, String properties) {
			super.beanPropertiesExcludes(beanClass, properties);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanPropertiesExcludes(Map<String,Object> values) {
			super.beanPropertiesExcludes(values);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanPropertiesExcludes(String beanClassName, String properties) {
			super.beanPropertiesExcludes(beanClassName, properties);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanPropertiesReadOnly(Class<?> beanClass, String properties) {
			super.beanPropertiesReadOnly(beanClass, properties);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanPropertiesReadOnly(Map<String,Object> values) {
			super.beanPropertiesReadOnly(values);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanPropertiesReadOnly(String beanClassName, String properties) {
			super.beanPropertiesReadOnly(beanClassName, properties);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanPropertiesWriteOnly(Class<?> beanClass, String properties) {
			super.beanPropertiesWriteOnly(beanClass, properties);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanPropertiesWriteOnly(Map<String,Object> values) {
			super.beanPropertiesWriteOnly(values);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanPropertiesWriteOnly(String beanClassName, String properties) {
			super.beanPropertiesWriteOnly(beanClassName, properties);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beansRequireDefaultConstructor() {
			super.beansRequireDefaultConstructor();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beansRequireSerializable() {
			super.beansRequireSerializable();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beansRequireSettersForGetters() {
			super.beansRequireSettersForGetters();
			return this;
		}

		@Override /* Overridden from Context.Builder */
		public XmlSerializer build() {
			return cache(CACHE).build(XmlSerializer.class);
		}

		@Override /* Overridden from Builder */
		public Builder cache(Cache<HashKey,? extends org.apache.juneau.Context> value) {
			super.cache(value);
			return this;
		}

		@Override /* Overridden from Context.Builder */
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* Overridden from Builder */
		public Builder debug() {
			super.debug();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder debug(boolean value) {
			super.debug(value);
			return this;
		}

		/**
		 * Default namespace.
		 *
		 * <p>
		 * Specifies the default namespace URI for this document.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/XmlNamespaces">Namespaces</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is <js>"juneau: http://www.apache.org/2013/Juneau"</js>.
		 * @return This object.
		 */
		public Builder defaultNamespace(Namespace value) {
			defaultNamespace = value;
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder detectRecursions() {
			super.detectRecursions();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder detectRecursions(boolean value) {
			super.detectRecursions(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
			super.dictionaryOn(on, values);
			return this;
		}

		/**
		 * Don't auto-detect namespace usage.
		 *
		 * <p>
		 * Don't detect namespace usage before serialization.
		 *
		 * <p>
		 * Used in conjunction with {@link Builder#addNamespaceUrisToRoot()} to reduce the list of namespace URLs appended to the
		 * root element to only those that will be used in the resulting document.
		 *
		 * <p>
		 * If disabled, then the data structure will first be crawled looking for namespaces that will be encountered before
		 * the root element is serialized.
		 *
		 * <p>
		 * This setting is ignored if {@link Builder#enableNamespaces()} is not enabled.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>
		 * 		Auto-detection of namespaces can be costly performance-wise.
		 * 		<br>In high-performance environments, it's recommended that namespace detection be
		 * 		disabled, and that namespaces be manually defined through the {@link Builder#namespaces(Namespace...)} property.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/XmlNamespaces">Namespaces</a>
		 * </ul>
		 *
		 * @return This object.
		 */
		public Builder disableAutoDetectNamespaces() {
			return disableAutoDetectNamespaces(true);
		}

		/**
		 * Same as {@link #disableAutoDetectNamespaces()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Builder disableAutoDetectNamespaces(boolean value) {
			disableAutoDetectNamespaces = value;
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder disableBeansRequireSomeProperties() {
			super.disableBeansRequireSomeProperties();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder disableIgnoreMissingSetters() {
			super.disableIgnoreMissingSetters();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder disableIgnoreTransientFields() {
			super.disableIgnoreTransientFields();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder disableIgnoreUnknownNullBeanProperties() {
			super.disableIgnoreUnknownNullBeanProperties();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder disableInterfaceProxies() {
			super.disableInterfaceProxies();
			return this;
		}

		/**
		 * <i><l>XmlSerializer</l> configuration property:&emsp;</i>  Disable use of JSON type identifier tags.
		 *
		 * <p>
		 * When enabled, JSON type tags (e.g. <js>"&lt;string&gt;"</js>) tags and attributes will not be added to the output.
		 * Note that JSON type tags are used to ensure parsers are able to recreate the original data types passed
		 * into the serializer.  Disabling JSON tags can cause different data to be parsed (e.g. strings instead of numbers).
		 *
		 * @return This object.
		 */
		public Builder disableJsonTags() {
			return disableJsonTags(true);
		}

		/**
		 * Same as {@link #disableJsonTags()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Builder disableJsonTags(boolean value) {
			disableJsonTags = value;
			return this;
		}

		/**
		 * Enable support for XML namespaces.
		 *
		 * <p>
		 * If not enabled, XML output will not contain any namespaces regardless of any other settings.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/XmlNamespaces">Namespaces</a>
		 * </ul>
		 *
		 * @return This object.
		 */
		public Builder enableNamespaces() {
			return enableNamespaces(true);
		}

		/**
		 * Same as {@link #enableNamespaces()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Builder enableNamespaces(boolean value) {
			enableNamespaces = value;
			return this;
		}

		@Override /* Overridden from Builder */
		public <T> Builder example(Class<T> pojoClass, String json) {
			super.example(pojoClass, json);
			return this;
		}

		@Override /* Overridden from Builder */
		public <T> Builder example(Class<T> pojoClass, T o) {
			super.example(pojoClass, o);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder fileCharset(Charset value) {
			super.fileCharset(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder findFluentSetters() {
			super.findFluentSetters();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder findFluentSetters(Class<?> on) {
			super.findFluentSetters(on);
			return this;
		}

		@Override /* Overridden from Context.Builder */
		public HashKey hashKey() {
			// @formatter:off
			return HashKey.of(
				super.hashKey(),
				addBeanTypesXml,
				addNamespaceUrisToRoot,
				disableAutoDetectNamespaces,
				disableJsonTags,
				enableNamespaces,
				defaultNamespace,
				namespaces,
				textNodeDelimiter
			);
			// @formatter:on
		}

		@Override /* Overridden from Builder */
		public Builder ignoreInvocationExceptionsOnGetters() {
			super.ignoreInvocationExceptionsOnGetters();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder ignoreInvocationExceptionsOnSetters() {
			super.ignoreInvocationExceptionsOnSetters();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder ignoreRecursions() {
			super.ignoreRecursions();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder ignoreRecursions(boolean value) {
			super.ignoreRecursions(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder ignoreUnknownBeanProperties() {
			super.ignoreUnknownBeanProperties();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder ignoreUnknownEnumValues() {
			super.ignoreUnknownEnumValues();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder impl(Context value) {
			super.impl(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder implClass(Class<?> interfaceClass, Class<?> implClass) {
			super.implClass(interfaceClass, implClass);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder implClasses(Map<Class<?>,Class<?>> values) {
			super.implClasses(values);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder initialDepth(int value) {
			super.initialDepth(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder interfaceClass(Class<?> on, Class<?> value) {
			super.interfaceClass(on, value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder interfaces(java.lang.Class<?>...value) {
			super.interfaces(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder keepNullProperties() {
			super.keepNullProperties();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder keepNullProperties(boolean value) {
			super.keepNullProperties(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder listener(Class<? extends org.apache.juneau.serializer.SerializerListener> value) {
			super.listener(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder maxDepth(int value) {
			super.maxDepth(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder maxIndent(int value) {
			super.maxIndent(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return this;
		}

		/**
		 * Default namespaces.
		 *
		 * <p>
		 * The default list of namespaces associated with this serializer.
		 *
		 * @param values The new value for this property.
		 * @return This object.
		 */
		public Builder namespaces(Namespace...values) {
			namespaces = addAll(namespaces, values);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder notBeanClasses(java.lang.Class<?>...values) {
			super.notBeanClasses(values);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder notBeanPackages(String...values) {
			super.notBeanPackages(values);
			return this;
		}

		/**
		 * Enable support for XML namespaces.
		 *
		 * <p>
		 * Shortcut for calling <code>enableNamespaces(<jk>true</jk>)</code>.
		 *
		 * @return This object.
		 */
		public Builder ns() {
			return enableNamespaces();
		}

		@Override /* Overridden from Builder */
		public Builder produces(String value) {
			super.produces(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
			super.propertyNamer(on, value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
			super.propertyNamer(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder quoteChar(char value) {
			super.quoteChar(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder quoteCharOverride(char value) {
			super.quoteCharOverride(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder sortCollections() {
			super.sortCollections();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder sortCollections(boolean value) {
			super.sortCollections(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder sortMaps() {
			super.sortMaps();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder sortMaps(boolean value) {
			super.sortMaps(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder sortProperties() {
			super.sortProperties();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder sortProperties(java.lang.Class<?>...on) {
			super.sortProperties(on);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder sq() {
			super.sq();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder stopClass(Class<?> on, Class<?> value) {
			super.stopClass(on, value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder streamCharset(Charset value) {
			super.streamCharset(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public <T,S> Builder swap(Class<T> normalClass, Class<S> swappedClass, ThrowingFunction<T,S> swapFunction) {
			super.swap(normalClass, swappedClass, swapFunction);
			return this;
		}

		@Override /* Overridden from Builder */
		public <T,S> Builder swap(Class<T> normalClass, Class<S> swappedClass, ThrowingFunction<T,S> swapFunction, ThrowingFunction<S,T> unswapFunction) {
			super.swap(normalClass, swappedClass, swapFunction, unswapFunction);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder swaps(Class<?>...values) {
			super.swaps(values);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder swaps(Object...values) {
			super.swaps(values);
			return this;
		}

		/**
		 * Text node delimiter.
		 *
		 * <p>
		 * Specifies the delimiter string to insert between consecutive text nodes.
		 * This is useful for adding spacing between text elements to improve readability.
		 *
		 * <p>
		 * The default value is an empty string (no delimiter).
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	XmlSerializer.<jsm>create</jsm>()
		 * 		.textNodeDelimiter(<js>" "</js>)
		 * 		.build();
		 * </p>
		 *
		 * <p>
		 * With this setting, serializing:
		 * <p class='bjava'>
		 * 	<jk>new</jk> Audio().children(<js>"a"</js>, <js>"b"</js>, <jk>new</jk> Strong(<js>"c"</js>));
		 * </p>
		 *
		 * <p>
		 * Will produce: <code>&lt;audio&gt;a b&lt;strong&gt;c&lt;/strong&gt;&lt;/audio&gt;</code>
		 * <br>Instead of: <code>&lt;audio&gt;ab&lt;strong&gt;c&lt;/strong&gt;&lt;/audio&gt;</code>
		 *
		 * @param value
		 * 	The delimiter string.
		 * 	<br>Can be <jk>null</jk> (interpreted as empty string).
		 * @return This object.
		 */
		public Builder textNodeDelimiter(String value) {
			textNodeDelimiter = value == null ? "" : value;
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder trimEmptyCollections() {
			super.trimEmptyCollections();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder trimEmptyCollections(boolean value) {
			super.trimEmptyCollections(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder trimEmptyMaps() {
			super.trimEmptyMaps();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder trimEmptyMaps(boolean value) {
			super.trimEmptyMaps(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder trimStrings() {
			super.trimStrings();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder trimStrings(boolean value) {
			super.trimStrings(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder type(Class<? extends org.apache.juneau.Context> value) {
			super.type(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder typeName(Class<?> on, String value) {
			super.typeName(on, value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder typePropertyName(Class<?> on, String value) {
			super.typePropertyName(on, value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder typePropertyName(String value) {
			super.typePropertyName(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder uriContext(UriContext value) {
			super.uriContext(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder uriRelativity(UriRelativity value) {
			super.uriRelativity(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder uriResolution(UriResolution value) {
			super.uriResolution(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder useEnumNames() {
			super.useEnumNames();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder useJavaBeanIntrospector() {
			super.useJavaBeanIntrospector();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder useWhitespace() {
			super.useWhitespace();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder useWhitespace(boolean value) {
			super.useWhitespace(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder ws() {
			super.ws();
			return this;
		}
	}

	/** Default serializer without namespaces. */
	public static class Ns extends XmlSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public Ns(Builder builder) {
			super(builder.enableNamespaces());
		}
	}

	/** Default serializer without namespaces, single quotes. */
	public static class NsSq extends XmlSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public NsSq(Builder builder) {
			super(builder.enableNamespaces().quoteChar('\''));
		}
	}

	/** Default serializer without namespaces, single quotes, with whitespace. */
	public static class NsSqReadable extends XmlSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public NsSqReadable(Builder builder) {
			super(builder.enableNamespaces().quoteChar('\'').useWhitespace());
		}
	}

	/** Default serializer, single quotes. */
	public static class Sq extends XmlSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public Sq(Builder builder) {
			super(builder.quoteChar('\''));
		}
	}

	/** Default serializer, single quotes, whitespace added. */
	public static class SqReadable extends XmlSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public SqReadable(Builder builder) {
			super(builder.quoteChar('\'').useWhitespace());
		}
	}

	private static final Namespace[] EMPTY_NAMESPACE_ARRAY = {};

	/** Default serializer without namespaces. */
	public static final XmlSerializer DEFAULT = new XmlSerializer(create());
	/** Default serializer without namespaces, with single quotes. */
	public static final XmlSerializer DEFAULT_SQ = new Sq(create());

	/** Default serializer without namespaces, with single quotes, whitespace added. */
	public static final XmlSerializer DEFAULT_SQ_READABLE = new SqReadable(create());

	/** Default serializer, all default settings. */
	public static final XmlSerializer DEFAULT_NS = new Ns(create());

	/** Default serializer, single quotes. */
	public static final XmlSerializer DEFAULT_NS_SQ = new NsSq(create());

	/** Default serializer, single quotes, whitespace added. */
	public static final XmlSerializer DEFAULT_NS_SQ_READABLE = new NsSqReadable(create());

	protected static final Namespace DEFAULT_JUNEAU_NAMESPACE = Namespace.of("juneau", "http://www.apache.org/2013/Juneau"),
		DEFAULT_XS_NAMESPACE = Namespace.of("xs", "http://www.w3.org/2001/XMLSchema");

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	final boolean autoDetectNamespaces, enableNamespaces, addNamespaceUrlsToRoot, addBeanTypesXml, addJsonTags;

	final Namespace defaultNamespace;
	final Namespace[] namespaces;
	final String textNodeDelimiter;

	private final boolean addBeanTypes;
	private final Map<ClassMeta<?>,XmlClassMeta> xmlClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanMeta<?>,XmlBeanMeta> xmlBeanMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,XmlBeanPropertyMeta> xmlBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder
	 * 	The builder for this object.
	 */
	public XmlSerializer(Builder builder) {
		super(builder);
		autoDetectNamespaces = ! builder.disableAutoDetectNamespaces;
		enableNamespaces = builder.enableNamespaces;
		addNamespaceUrlsToRoot = builder.addNamespaceUrisToRoot;
		addBeanTypesXml = builder.addBeanTypesXml;
		addJsonTags = ! builder.disableJsonTags;
		defaultNamespace = nn(builder.defaultNamespace) ? builder.defaultNamespace : DEFAULT_JUNEAU_NAMESPACE;
		namespaces = nn(builder.namespaces) ? builder.namespaces.toArray(EMPTY_NAMESPACE_ARRAY) : EMPTY_NAMESPACE_ARRAY;
		textNodeDelimiter = builder.textNodeDelimiter;
		addBeanTypes = addBeanTypesXml || super.isAddBeanTypes();
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Overridden from Context */
	public XmlSerializerSession.Builder createSession() {
		return XmlSerializerSession.create(this);
	}

	@Override /* Overridden from Context */
	public XmlSerializerSession getSession() { return createSession().build(); }

	@Override /* Overridden from XmlMetaProvider */
	public XmlBeanMeta getXmlBeanMeta(BeanMeta<?> bm) {
		XmlBeanMeta m = xmlBeanMetas.get(bm);
		if (m == null) {
			m = new XmlBeanMeta(bm, this);
			xmlBeanMetas.put(bm, m);
		}
		return m;
	}

	@Override /* Overridden from XmlMetaProvider */
	public XmlBeanPropertyMeta getXmlBeanPropertyMeta(BeanPropertyMeta bpm) {
		XmlBeanPropertyMeta m = xmlBeanPropertyMetas.get(bpm);
		if (m == null) {
			m = new XmlBeanPropertyMeta(bpm.getDelegateFor(), this);
			xmlBeanPropertyMetas.put(bpm, m);
		}
		return m;
	}

	@Override /* Overridden from XmlMetaProvider */
	public XmlClassMeta getXmlClassMeta(ClassMeta<?> cm) {
		XmlClassMeta m = xmlClassMetas.get(cm);
		if (m == null) {
			m = new XmlClassMeta(cm, this);
			xmlClassMetas.put(cm, m);
		}
		return m;
	}

	/**
	 * Default namespace.
	 *
	 * @see Builder#defaultNamespace(Namespace)
	 * @return
	 * 	The default namespace URI for this document.
	 */
	protected final Namespace getDefaultNamespace() { return defaultNamespace; }

	/**
	 * Default namespaces.
	 *
	 * @see Builder#namespaces(Namespace...)
	 * @return
	 * 	The default list of namespaces associated with this serializer.
	 */
	protected final Namespace[] getNamespaces() { return namespaces; }

	/**
	 * Add <js>"_type"</js> properties when needed.
	 *
	 * @see Builder#addBeanTypesXml()
	 * @return
	 * 	<jk>true</jk> if<js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * 	through reflection.
	 */
	@Override
	protected boolean isAddBeanTypes() { return addBeanTypes; }

	/**
	 * Add namespace URLs to the root element.
	 *
	 * @see Builder#addNamespaceUrisToRoot()
	 * @return
	 * 	<jk>true</jk> if {@code xmlns:x} attributes are added to the root element for the default and all mapped namespaces.
	 */
	protected final boolean isAddNamespaceUrlsToRoot() { return addNamespaceUrlsToRoot; }

	/**
	 * Auto-detect namespace usage.
	 *
	 * @see Builder#disableAutoDetectNamespaces()
	 * @return
	 * 	<jk>true</jk> if namespace usage is detected before serialization.
	 */
	protected final boolean isAutoDetectNamespaces() { return autoDetectNamespaces; }

	/**
	 * Enable support for XML namespaces.
	 *
	 * @see Builder#enableNamespaces()
	 * @return
	 * 	<jk>false</jk> if XML output will not contain any namespaces regardless of any other settings.
	 */
	protected final boolean isEnableNamespaces() { return enableNamespaces; }

	@Override /* Overridden from Context */
	protected JsonMap properties() {
		// @formatter:off
		return filteredMap()
			.append("autoDetectNamespaces", autoDetectNamespaces)
			.append("enableNamespaces", enableNamespaces)
			.append("addNamespaceUrlsToRoot", addNamespaceUrlsToRoot)
			.append("defaultNamespace", defaultNamespace)
			.append("namespaces", namespaces)
			.append("addBeanTypes", addBeanTypes);
		// @formatter:on
	}
}