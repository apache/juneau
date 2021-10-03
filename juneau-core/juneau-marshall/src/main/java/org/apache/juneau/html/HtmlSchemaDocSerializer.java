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
package org.apache.juneau.html;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;

/**
 * Serializes POJO metamodels to HTML.
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <c>Accept</c> types:  <bc>text/html+schema</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>text/html</bc>
 *
 * <h5 class='topic'>Description</h5>
 *
 * Essentially the same as {@link HtmlDocSerializer}, except serializes the POJO metamodel instead of the model itself.
 *
 * <p>
 * Produces output that describes the POJO metamodel similar to an XML schema document.
 *
 * <p>
 * The easiest way to create instances of this class is through the {@link HtmlSerializer#getSchemaSerializer()},
 * which will create a schema serializer with the same settings as the originating serializer.
 */
public final class HtmlSchemaDocSerializer extends HtmlDocSerializer {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends HtmlDocSerializer.Builder {

		JsonSchemaGenerator.Builder generatorBuilder;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			super();
			produces("text/html");
			accept("text/html+schema");
			type(HtmlSchemaDocSerializer.class);
			generatorBuilder = (JsonSchemaGenerator.Builder) JsonSchemaGenerator.create().beanContext(beanContext());
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(HtmlSchemaDocSerializer copyFrom) {
			super(copyFrom);
			generatorBuilder = (JsonSchemaGenerator.Builder) copyFrom.generator.copy().beanContext(beanContext());
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
			generatorBuilder = (JsonSchemaGenerator.Builder) copyFrom.generatorBuilder.copy().beanContext(beanContext());
		}

		@Override /* Context.Builder */
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* Context.Builder */
		public HtmlSchemaDocSerializer build() {
			return (HtmlSchemaDocSerializer)super.build();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// Properties
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * <i><l>HtmlSchemaSerializer</l> configuration property:&emsp;</i>  Add descriptions.
		 *
		 * <p>
		 * Identifies which categories of types that descriptions should be automatically added to generated schemas.
		 * <p>
		 * The description is the result of calling {@link ClassMeta#getFullName()}.
		 *
		 * <ul class='seealso'>
		 * 	<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGenerator.Builder#addDescriptionsTo(TypeCategory...)}
		 * </ul>
		 *
		 * @param values
		 * 	The values to add to this setting.
		 * 	<br>The default is an empty string.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder addDescriptionsTo(TypeCategory...values) {
			generatorBuilder.addDescriptionsTo(values);
			return this;
		}

		/**
		 * <i><l>HtmlSchemaSerializer</l> configuration property:&emsp;</i>  Add examples.
		 *
		 * <p>
		 * Identifies which categories of types that examples should be automatically added to generated schemas.
		 * <p>
		 * The examples come from calling {@link ClassMeta#getExample(BeanSession,JsonParserSession)} which in turn gets examples
		 * from the following:
		 * <ul class='javatree'>
		 * 	<li class='ja'>{@link Example}
		 * 	<li class='ja'>{@link Marshalled#example() Marshalled(example)}
		 * </ul>
		 *
		 * <ul class='seealso'>
		 * 	<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGenerator.Builder#addExamplesTo(TypeCategory...)}
		 * </ul>
		 *
		 * @param values
		 * 	The values to add to this setting.
		 * 	<br>The default is an empty string.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder addExamplesTo(TypeCategory...values) {
			generatorBuilder.addExamplesTo(values);
			return this;
		}

		/**
		 * <i><l>HtmlSchemaSerializer</l> configuration property:&emsp;</i>  Allow nested descriptions.
		 *
		 * <p>
		 * Identifies whether nested descriptions are allowed in schema definitions.
		 *
		 * <ul class='seealso'>
		 * 	<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGenerator.Builder#allowNestedDescriptions()}
		 * </ul>
		 *
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder allowNestedDescriptions() {
			generatorBuilder.allowNestedDescriptions();
			return this;
		}

		/**
		 * <i><l>HtmlSchemaSerializer</l> configuration property:&emsp;</i>  Allow nested examples.
		 *
		 * <p>
		 * Identifies whether nested examples are allowed in schema definitions.
		 *
		 * <ul class='seealso'>
		 * 	<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGenerator.Builder#allowNestedExamples()}
		 * </ul>
		 *
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder allowNestedExamples() {
			generatorBuilder.allowNestedExamples();
			return this;
		}

		/**
		 * <i><l>HtmlSchemaSerializer</l> configuration property:&emsp;</i>  Schema definition mapper.
		 *
		 * <p>
		 * Interface to use for converting Bean classes to definition IDs and URIs.
		 * <p>
		 * Used primarily for defining common definition sections for beans in Swagger JSON.
		 * <p>
		 * This setting is ignored if {@link org.apache.juneau.jsonschema.JsonSchemaGenerator.Builder#useBeanDefs()} is not enabled.
		 *
		 * <ul class='seealso'>
		 * 	<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGenerator.Builder#beanDefMapper(Class)}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is {@link org.apache.juneau.jsonschema.BasicBeanDefMapper}.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder beanDefMapper(Class<? extends BeanDefMapper> value) {
			generatorBuilder.beanDefMapper(value);
			return this;
		}

		/**
		 * <i><l>HtmlSchemaSerializer</l> configuration property:&emsp;</i>  Default schemas.
		 *
		 * <p>
		 * Allows you to override or provide custom schema information for particular class types.
		 * <p>
		 * Keys are full class names.
		 *
		 * <ul class='seealso'>
		 * 	<li class='jf'>{@link Schema#value}
		 * </ul>
		 *
		 * @param c
		 * 	The class to define a default schema for.
		 * @param schema
		 * 	The schema.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder defaultSchema(Class<?> c, OMap schema) {
			return annotations(SchemaAnnotation.create(c).value(schema.toString()).build());
		}

		/**
		 * <i><l>HtmlSchemaSerializer</l> configuration property:&emsp;</i>  Use bean definitions.
		 *
		 * <p>
		 * When enabled, schemas on beans will be serialized as the following:
		 * <p class='bcode w800'>
		 * 	{
		 * 		type: <js>'object'</js>,
		 * 		<js>'$ref'</js>: <js>'#/definitions/TypeId'</js>
		 * 	}
		 * </p>
		 *
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder useBeanDefs() {
			generatorBuilder.useBeanDefs();
			return this;
		}

		// <FluentSetters>

		@Override /* Context.Builder */
		public Builder beanContext(BeanContext value) {
			super.beanContext(value);
			return this;
		}

		@Override /* Context.Builder */
		public Builder applyAnnotations(java.lang.Class<?>...fromClasses) {
			super.applyAnnotations(fromClasses);
			return this;
		}

		@Override /* Context.Builder */
		public Builder applyAnnotations(Method...fromMethods) {
			super.applyAnnotations(fromMethods);
			return this;
		}

		@Override /* Context.Builder */
		public Builder apply(AnnotationWorkList work) {
			super.apply(work);
			return this;
		}

		@Override /* Context.Builder */
		public Builder debug() {
			super.debug();
			return this;
		}

		@Override /* Context.Builder */
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override /* Context.Builder */
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return this;
		}

		@Override /* Context.Builder */
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder annotations(Annotation...values) {
			super.annotations(values);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanClassVisibility(Visibility value) {
			super.beanClassVisibility(value);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanConstructorVisibility(Visibility value) {
			super.beanConstructorVisibility(value);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanFieldVisibility(Visibility value) {
			super.beanFieldVisibility(value);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanInterceptor(Class<?> on, Class<? extends org.apache.juneau.transform.BeanInterceptor<?>> value) {
			super.beanInterceptor(on, value);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanMapPutReturnsOldValue() {
			super.beanMapPutReturnsOldValue();
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanMethodVisibility(Visibility value) {
			super.beanMethodVisibility(value);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanProperties(Map<String,Object> values) {
			super.beanProperties(values);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanProperties(Class<?> beanClass, String properties) {
			super.beanProperties(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanProperties(String beanClassName, String properties) {
			super.beanProperties(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanPropertiesExcludes(Map<String,Object> values) {
			super.beanPropertiesExcludes(values);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanPropertiesExcludes(Class<?> beanClass, String properties) {
			super.beanPropertiesExcludes(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanPropertiesExcludes(String beanClassName, String properties) {
			super.beanPropertiesExcludes(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanPropertiesReadOnly(Map<String,Object> values) {
			super.beanPropertiesReadOnly(values);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanPropertiesReadOnly(Class<?> beanClass, String properties) {
			super.beanPropertiesReadOnly(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanPropertiesReadOnly(String beanClassName, String properties) {
			super.beanPropertiesReadOnly(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanPropertiesWriteOnly(Map<String,Object> values) {
			super.beanPropertiesWriteOnly(values);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanPropertiesWriteOnly(Class<?> beanClass, String properties) {
			super.beanPropertiesWriteOnly(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanPropertiesWriteOnly(String beanClassName, String properties) {
			super.beanPropertiesWriteOnly(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beansRequireDefaultConstructor() {
			super.beansRequireDefaultConstructor();
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beansRequireSerializable() {
			super.beansRequireSerializable();
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beansRequireSettersForGetters() {
			super.beansRequireSettersForGetters();
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanDictionary(Class<?>...values) {
			super.beanDictionary(values);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
			super.dictionaryOn(on, values);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder disableBeansRequireSomeProperties() {
			super.disableBeansRequireSomeProperties();
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder disableIgnoreMissingSetters() {
			super.disableIgnoreMissingSetters();
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder disableIgnoreTransientFields() {
			super.disableIgnoreTransientFields();
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder disableIgnoreUnknownNullBeanProperties() {
			super.disableIgnoreUnknownNullBeanProperties();
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder disableInterfaceProxies() {
			super.disableInterfaceProxies();
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public <T> Builder example(Class<T> pojoClass, T o) {
			super.example(pojoClass, o);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public <T> Builder example(Class<T> pojoClass, String json) {
			super.example(pojoClass, json);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder findFluentSetters() {
			super.findFluentSetters();
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder findFluentSetters(Class<?> on) {
			super.findFluentSetters(on);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder ignoreInvocationExceptionsOnGetters() {
			super.ignoreInvocationExceptionsOnGetters();
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder ignoreInvocationExceptionsOnSetters() {
			super.ignoreInvocationExceptionsOnSetters();
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder ignoreUnknownBeanProperties() {
			super.ignoreUnknownBeanProperties();
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder implClass(Class<?> interfaceClass, Class<?> implClass) {
			super.implClass(interfaceClass, implClass);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder implClasses(Map<Class<?>,Class<?>> values) {
			super.implClasses(values);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder interfaceClass(Class<?> on, Class<?> value) {
			super.interfaceClass(on, value);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder interfaces(java.lang.Class<?>...value) {
			super.interfaces(value);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder notBeanClasses(Class<?>...values) {
			super.notBeanClasses(values);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder notBeanPackages(String...values) {
			super.notBeanPackages(values);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
			super.propertyNamer(value);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
			super.propertyNamer(on, value);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder sortProperties() {
			super.sortProperties();
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder sortProperties(java.lang.Class<?>...on) {
			super.sortProperties(on);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder stopClass(Class<?> on, Class<?> value) {
			super.stopClass(on, value);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder swaps(Class<?>...values) {
			super.swaps(values);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder typeName(Class<?> on, String value) {
			super.typeName(on, value);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder typePropertyName(String value) {
			super.typePropertyName(value);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder typePropertyName(Class<?> on, String value) {
			super.typePropertyName(on, value);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder useEnumNames() {
			super.useEnumNames();
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder useJavaBeanIntrospector() {
			super.useJavaBeanIntrospector();
			return this;
		}

		@Override /* GENERATED - BeanTraverse.Builder */
		public Builder detectRecursions() {
			super.detectRecursions();
			return this;
		}

		@Override /* GENERATED - BeanTraverse.Builder */
		public Builder ignoreRecursions() {
			super.ignoreRecursions();
			return this;
		}

		@Override /* GENERATED - BeanTraverse.Builder */
		public Builder initialDepth(int value) {
			super.initialDepth(value);
			return this;
		}

		@Override /* GENERATED - BeanTraverse.Builder */
		public Builder maxDepth(int value) {
			super.maxDepth(value);
			return this;
		}

		@Override /* GENERATED - Serializer.Builder */
		public Builder addBeanTypes() {
			super.addBeanTypes();
			return this;
		}

		@Override /* GENERATED - Serializer.Builder */
		public Builder addRootType() {
			super.addRootType();
			return this;
		}

		@Override /* GENERATED - Serializer.Builder */
		public Builder keepNullProperties() {
			super.keepNullProperties();
			return this;
		}

		@Override /* GENERATED - Serializer.Builder */
		public Builder listener(Class<? extends org.apache.juneau.serializer.SerializerListener> value) {
			super.listener(value);
			return this;
		}

		@Override /* GENERATED - Serializer.Builder */
		public Builder sortCollections() {
			super.sortCollections();
			return this;
		}

		@Override /* GENERATED - Serializer.Builder */
		public Builder sortMaps() {
			super.sortMaps();
			return this;
		}

		@Override /* GENERATED - Serializer.Builder */
		public Builder trimEmptyCollections() {
			super.trimEmptyCollections();
			return this;
		}

		@Override /* GENERATED - Serializer.Builder */
		public Builder trimEmptyMaps() {
			super.trimEmptyMaps();
			return this;
		}

		@Override /* GENERATED - Serializer.Builder */
		public Builder trimStrings() {
			super.trimStrings();
			return this;
		}

		@Override /* GENERATED - Serializer.Builder */
		public Builder uriContext(UriContext value) {
			super.uriContext(value);
			return this;
		}

		@Override /* GENERATED - Serializer.Builder */
		public Builder uriRelativity(UriRelativity value) {
			super.uriRelativity(value);
			return this;
		}

		@Override /* GENERATED - Serializer.Builder */
		public Builder uriResolution(UriResolution value) {
			super.uriResolution(value);
			return this;
		}

		@Override /* GENERATED - WriterSerializer.Builder */
		public Builder fileCharset(Charset value) {
			super.fileCharset(value);
			return this;
		}

		@Override /* GENERATED - WriterSerializer.Builder */
		public Builder maxIndent(int value) {
			super.maxIndent(value);
			return this;
		}

		@Override /* GENERATED - WriterSerializer.Builder */
		public Builder quoteChar(char value) {
			super.quoteChar(value);
			return this;
		}

		@Override /* GENERATED - WriterSerializer.Builder */
		public Builder sq() {
			super.sq();
			return this;
		}

		@Override /* GENERATED - WriterSerializer.Builder */
		public Builder streamCharset(Charset value) {
			super.streamCharset(value);
			return this;
		}

		@Override /* GENERATED - WriterSerializer.Builder */
		public Builder useWhitespace() {
			super.useWhitespace();
			return this;
		}

		@Override /* GENERATED - WriterSerializer.Builder */
		public Builder ws() {
			super.ws();
			return this;
		}

		@Override /* GENERATED - XmlSerializer.Builder */
		public Builder addNamespaceUrisToRoot() {
			super.addNamespaceUrisToRoot();
			return this;
		}

		@Override /* GENERATED - XmlSerializer.Builder */
		public Builder disableAutoDetectNamespaces() {
			super.disableAutoDetectNamespaces();
			return this;
		}

		@Override /* GENERATED - XmlSerializer.Builder */
		public Builder enableNamespaces() {
			super.enableNamespaces();
			return this;
		}

		@Override /* GENERATED - XmlSerializer.Builder */
		public Builder namespaces(Namespace...values) {
			super.namespaces(values);
			return this;
		}

		@Override /* GENERATED - XmlSerializer.Builder */
		public Builder ns() {
			super.ns();
			return this;
		}

		@Override /* GENERATED - HtmlSerializer.Builder */
		public Builder addKeyValueTableHeaders() {
			super.addKeyValueTableHeaders();
			return this;
		}

		@Override /* GENERATED - HtmlSerializer.Builder */
		public Builder disableDetectLabelParameters() {
			super.disableDetectLabelParameters();
			return this;
		}

		@Override /* GENERATED - HtmlSerializer.Builder */
		public Builder disableDetectLinksInStrings() {
			super.disableDetectLinksInStrings();
			return this;
		}

		@Override /* GENERATED - HtmlSerializer.Builder */
		public Builder labelParameter(String value) {
			super.labelParameter(value);
			return this;
		}

		@Override /* GENERATED - HtmlSerializer.Builder */
		public Builder uriAnchorText(AnchorText value) {
			super.uriAnchorText(value);
			return this;
		}

		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	final JsonSchemaGenerator generator;

	/**
	 * Constructor.
	 *
	 * @param builder
	 * 	The builder for this object.
	 */
	protected HtmlSchemaDocSerializer(HtmlDocSerializer.Builder builder) {
		super(builder.detectRecursions().ignoreRecursions());

		generator = JsonSchemaGenerator.create().beanContext(getBeanContext()).build();
	}

	@Override /* Serializer */
	public HtmlSchemaDocSerializerSession createSession() {
		return createSession(defaultArgs());
	}

	@Override /* Serializer */
	public HtmlSchemaDocSerializerSession createSession(SerializerSessionArgs args) {
		return new HtmlSchemaDocSerializerSession(this, args);
	}

	JsonSchemaGenerator getGenerator() {
		return generator;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"HtmlSchemaDocSerializer",
				OMap
					.create()
					.filtered()
			);
	}
}
