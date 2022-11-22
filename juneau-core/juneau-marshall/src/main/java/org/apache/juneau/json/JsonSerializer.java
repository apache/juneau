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
package org.apache.juneau.json;

import static org.apache.juneau.collections.JsonMap.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utils.*;

/**
 * Serializes POJO models to JSON.
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Accept</c> types:  <bc>application/json, text/json</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>application/json</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * The conversion is as follows...
 * <ul class='spaced-list'>
 * 	<li>
 * 		Maps (e.g. {@link HashMap HashMaps}, {@link TreeMap TreeMaps}) are converted to JSON objects.
 * 	<li>
 * 		Collections (e.g. {@link HashSet HashSets}, {@link LinkedList LinkedLists}) and Java arrays are converted to
 * 		JSON arrays.
 * 	<li>
 * 		{@link String Strings} are converted to JSON strings.
 * 	<li>
 * 		{@link Number Numbers} (e.g. {@link Integer}, {@link Long}, {@link Double}) are converted to JSON numbers.
 * 	<li>
 * 		{@link Boolean Booleans} are converted to JSON booleans.
 * 	<li>
 * 		{@code nulls} are converted to JSON nulls.
 * 	<li>
 * 		{@code arrays} are converted to JSON arrays.
 * 	<li>
 * 		{@code beans} are converted to JSON objects.
 * </ul>
 *
 * <p>
 * The types above are considered "JSON-primitive" object types.
 * Any non-JSON-primitive object types are transformed into JSON-primitive object types through
 * {@link org.apache.juneau.swap.ObjectSwap ObjectSwaps} associated through the
 * {@link org.apache.juneau.BeanContext.Builder#swaps(Class...)} method.
 * Several default transforms are provided for transforming Dates, Enums, Iterators, etc...
 *
 * <p>
 * This serializer provides several serialization options.
 * Typically, one of the predefined DEFAULT serializers will be sufficient.
 * However, custom serializers can be constructed to fine-tune behavior.
 *
 * <h5 class='topic'>Behavior-specific subclasses</h5>
 * <p>
 * The following direct subclasses are provided for convenience:
 * <ul class='spaced-list'>
 * 	<li>
 * 		{@link Json5Serializer} - Default serializer, single quotes, simple mode.
 * 	<li>
 * 		{@link Json5Serializer.Readable} - Default serializer, single quotes, simple mode, with whitespace.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Use one of the default serializers to serialize a POJO</jc>
 * 	String <jv>json</jv> = JsonSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>someObject</jv>);
 *
 * 	<jc>// Create a custom serializer for lax syntax using single quote characters</jc>
 * 	JsonSerializer <jv>serializer</jv> = JsonSerializer.<jsm>create</jsm>().simple().sq().build();
 *
 * 	<jc>// Clone an existing serializer and modify it to use single-quotes</jc>
 * 	<jv>serializer</jv> = JsonSerializer.<jsf>DEFAULT</jsf>.copy().sq().build();
 *
 * 	<jc>// Serialize a POJO to JSON</jc>
 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jv>someObject</jv>);
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.JsonDetails">JSON Details</a>
 * </ul>
 */
public class JsonSerializer extends WriterSerializer implements JsonMetaProvider {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, all default settings.*/
	public static final JsonSerializer DEFAULT = new JsonSerializer(create());

	/** Default serializer, all default settings.*/
	public static final JsonSerializer DEFAULT_READABLE = new Readable(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Static subclasses
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, with whitespace. */
	public static class Readable extends JsonSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public Readable(Builder builder) {
			super(builder.useWhitespace());
		}
	}

	/**
	 * Default serializer, single quotes, simple mode, with whitespace and recursion detection.
	 * Note that recursion detection introduces a small performance penalty.
	 */
	public static class ReadableSafe extends JsonSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public ReadableSafe(Builder builder) {
			super(builder.simpleAttrs().useWhitespace().detectRecursions());
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Builder
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends WriterSerializer.Builder {

		private static final Cache<HashKey,JsonSerializer> CACHE = Cache.of(HashKey.class, JsonSerializer.class).build();

		boolean addBeanTypesJson, escapeSolidus, simpleAttrs;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			super();
			produces("application/json");
			accept("application/json,text/json");
			addBeanTypesJson = env("JsonSerializer.addBeanTypes", false);
			escapeSolidus = env("JsonSerializer.escapeSolidus", false);
			simpleAttrs = env("JsonSerializer.simpleAttrs", false);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(JsonSerializer copyFrom) {
			super(copyFrom);
			addBeanTypesJson = copyFrom.addBeanTypesJson;
			escapeSolidus = copyFrom.escapeSolidus;
			simpleAttrs = copyFrom.simpleAttrs;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
			addBeanTypesJson = copyFrom.addBeanTypesJson;
			escapeSolidus = copyFrom.escapeSolidus;
			simpleAttrs = copyFrom.simpleAttrs;
		}

		@Override /* Context.Builder */
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* Context.Builder */
		public JsonSerializer build() {
			return cache(CACHE).build(JsonSerializer.class);
		}

		@Override /* Context.Builder */
		public HashKey hashKey() {
			return HashKey.of(
				super.hashKey(),
				addBeanTypesJson,
				escapeSolidus,
				simpleAttrs
			);
		}

		//-----------------------------------------------------------------------------------------------------------------
		// Properties
		//-----------------------------------------------------------------------------------------------------------------

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
		@FluentSetter
		public Builder addBeanTypesJson() {
			return addBeanTypesJson(true);
		}

		/**
		 * Same as {@link #addBeanTypesJson()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder addBeanTypesJson(boolean value) {
			addBeanTypesJson = value;
			return this;
		}

		/**
		 * Prefix solidus <js>'/'</js> characters with escapes.
		 *
		 * <p>
		 * If enabled, solidus (e.g. slash) characters should be escaped.
		 *
		 * <p>
		 * The JSON specification allows for either format.
		 * <br>However, if you're embedding JSON in an HTML script tag, this setting prevents confusion when trying to serialize
		 * <xt>&lt;\/script&gt;</xt>.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a JSON serializer that escapes solidus characters.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.simple()
		 * 		.escapeSolidus()
		 * 		.build();
		 *
		 * 	<jc>// Produces: "{foo:'&lt;\/bar&gt;'"</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(JsonMap.<jsm>of</jsm>(<js>"foo"</js>, <js>"&lt;/bar&gt;"</js>);
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder escapeSolidus() {
			return escapeSolidus(true);
		}

		/**
		 * Same as {@link #escapeSolidus()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder escapeSolidus(boolean value) {
			escapeSolidus = value;
			return this;
		}

		/**
		 * Simple JSON attributes mode.
		 *
		 * <p>
		 * If enabled, JSON attribute names will only be quoted when necessary.
		 * <br>Otherwise, they are always quoted.
		 *
		 * <p>
		 * Attributes do not need to be quoted when they conform to the following:
		 * <ol class='spaced-list'>
		 * 	<li>They start with an ASCII character or <js>'_'</js>.
		 * 	<li>They contain only ASCII characters or numbers or <js>'_'</js>.
		 * 	<li>They are not one of the following reserved words:
		 * 		<p class='bcode'>
		 * 	arguments, break, case, catch, class, const, continue, debugger, default,
		 * 	delete, do, else, enum, eval, export, extends, false, finally, for, function,
		 * 	if, implements, import, in, instanceof, interface, let, new, null, package,
		 * 	private, protected, public, return, static, super, switch, this, throw,
		 * 	true, try, typeof, var, void, while, with, undefined, yield
		 * 		</p>
		 * </ol>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a JSON serializer in normal mode.</jc>
		 * 	WriterSerializer <jv>serializer1</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.build();
		 *
		 * 	<jc>// Create a JSON serializer in simple mode.</jc>
		 * 	WriterSerializer <jv>serializer2</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.simpleAttrs()
		 * 		.build();
		 *
		 * 	JsonMap <jv>myMap</jv> = JsonMap.<jsm>of</jsm>(
		 * 		<js>"foo"</js>, <js>"x1"</js>,
		 * 		<js>"_bar"</js>, <js>"x2"</js>,
		 * 		<js>" baz "</js>, <js>"x3"</js>,
		 * 		<js>"123"</js>, <js>"x4"</js>,
		 * 		<js>"return"</js>, <js>"x5"</js>,
		 * 		<js>""</js>, <js>"x6"</js>
		 *  );
		 *
		 * 	<jc>// Produces:</jc>
		 * 	<jc>// {</jc>
		 * 	<jc>// 	"foo": "x1"</jc>
		 * 	<jc>// 	"_bar": "x2"</jc>
		 * 	<jc>// 	" baz ": "x3"</jc>
		 * 	<jc>// 	"123": "x4"</jc>
		 * 	<jc>// 	"return": "x5"</jc>
		 * 	<jc>// 	"": "x6"</jc>
		 * 	<jc>// }</jc>
		 * 	String <jv>json1</jv> = <jv>serializer1</jv>.serialize(<jv>myMap</jv>);
		 *
		 * 	<jc>// Produces:</jc>
		 * 	<jc>// {</jc>
		 * 	<jc>// 	foo: "x1"</jc>
		 * 	<jc>// 	_bar: "x2"</jc>
		 * 	<jc>// 	" baz ": "x3"</jc>
		 * 	<jc>// 	"123": "x4"</jc>
		 * 	<jc>// 	"return": "x5"</jc>
		 * 	<jc>// 	"": "x6"</jc>
		 * 	<jc>// }</jc>
		 * 	String <jv>json2</jv> = <jv>serializer2</jv>.serialize(<jv>myMap</jv>);
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder simpleAttrs() {
			return simpleAttrs(true);
		}

		/**
		 * Same as {@link #simpleAttrs()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder simpleAttrs(boolean value) {
			simpleAttrs = value;
			return this;
		}

		/**
		 * Simple JSON mode and single quote.
		 *
		 * <p>
		 * Shortcut for calling <c>simple().sq()</c>.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.serializer.WriterSerializer.Builder#quoteChar(char)}
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder json5() {
			return simpleAttrs().sq();
		}

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder annotations(Annotation...values) {
			super.annotations(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder apply(AnnotationWorkList work) {
			super.apply(work);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder applyAnnotations(java.lang.Class<?>...fromClasses) {
			super.applyAnnotations(fromClasses);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder applyAnnotations(Method...fromMethods) {
			super.applyAnnotations(fromMethods);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder cache(Cache<HashKey,? extends org.apache.juneau.Context> value) {
			super.cache(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder debug() {
			super.debug();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder debug(boolean value) {
			super.debug(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder impl(Context value) {
			super.impl(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder type(Class<? extends org.apache.juneau.Context> value) {
			super.type(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanClassVisibility(Visibility value) {
			super.beanClassVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanConstructorVisibility(Visibility value) {
			super.beanConstructorVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanContext(BeanContext value) {
			super.beanContext(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanContext(BeanContext.Builder value) {
			super.beanContext(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanDictionary(java.lang.Class<?>...values) {
			super.beanDictionary(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanFieldVisibility(Visibility value) {
			super.beanFieldVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanInterceptor(Class<?> on, Class<? extends org.apache.juneau.swap.BeanInterceptor<?>> value) {
			super.beanInterceptor(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanMapPutReturnsOldValue() {
			super.beanMapPutReturnsOldValue();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanMethodVisibility(Visibility value) {
			super.beanMethodVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanProperties(Map<String,Object> values) {
			super.beanProperties(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanProperties(Class<?> beanClass, String properties) {
			super.beanProperties(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanProperties(String beanClassName, String properties) {
			super.beanProperties(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesExcludes(Map<String,Object> values) {
			super.beanPropertiesExcludes(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesExcludes(Class<?> beanClass, String properties) {
			super.beanPropertiesExcludes(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesExcludes(String beanClassName, String properties) {
			super.beanPropertiesExcludes(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesReadOnly(Map<String,Object> values) {
			super.beanPropertiesReadOnly(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesReadOnly(Class<?> beanClass, String properties) {
			super.beanPropertiesReadOnly(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesReadOnly(String beanClassName, String properties) {
			super.beanPropertiesReadOnly(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesWriteOnly(Map<String,Object> values) {
			super.beanPropertiesWriteOnly(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesWriteOnly(Class<?> beanClass, String properties) {
			super.beanPropertiesWriteOnly(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesWriteOnly(String beanClassName, String properties) {
			super.beanPropertiesWriteOnly(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beansRequireDefaultConstructor() {
			super.beansRequireDefaultConstructor();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beansRequireSerializable() {
			super.beansRequireSerializable();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beansRequireSettersForGetters() {
			super.beansRequireSettersForGetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
			super.dictionaryOn(on, values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableBeansRequireSomeProperties() {
			super.disableBeansRequireSomeProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableIgnoreMissingSetters() {
			super.disableIgnoreMissingSetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableIgnoreTransientFields() {
			super.disableIgnoreTransientFields();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableIgnoreUnknownNullBeanProperties() {
			super.disableIgnoreUnknownNullBeanProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableInterfaceProxies() {
			super.disableInterfaceProxies();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T> Builder example(Class<T> pojoClass, T o) {
			super.example(pojoClass, o);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T> Builder example(Class<T> pojoClass, String json) {
			super.example(pojoClass, json);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder findFluentSetters() {
			super.findFluentSetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder findFluentSetters(Class<?> on) {
			super.findFluentSetters(on);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreInvocationExceptionsOnGetters() {
			super.ignoreInvocationExceptionsOnGetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreInvocationExceptionsOnSetters() {
			super.ignoreInvocationExceptionsOnSetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreUnknownBeanProperties() {
			super.ignoreUnknownBeanProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreUnknownEnumValues() {
			super.ignoreUnknownEnumValues();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder implClass(Class<?> interfaceClass, Class<?> implClass) {
			super.implClass(interfaceClass, implClass);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder implClasses(Map<Class<?>,Class<?>> values) {
			super.implClasses(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder interfaceClass(Class<?> on, Class<?> value) {
			super.interfaceClass(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder interfaces(java.lang.Class<?>...value) {
			super.interfaces(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder notBeanClasses(java.lang.Class<?>...values) {
			super.notBeanClasses(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder notBeanPackages(String...values) {
			super.notBeanPackages(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
			super.propertyNamer(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
			super.propertyNamer(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder sortProperties() {
			super.sortProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder sortProperties(java.lang.Class<?>...on) {
			super.sortProperties(on);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder stopClass(Class<?> on, Class<?> value) {
			super.stopClass(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T, S> Builder swap(Class<T> normalClass, Class<S> swappedClass, ThrowingFunction<T,S> swapFunction) {
			super.swap(normalClass, swappedClass, swapFunction);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T, S> Builder swap(Class<T> normalClass, Class<S> swappedClass, ThrowingFunction<T,S> swapFunction, ThrowingFunction<S,T> unswapFunction) {
			super.swap(normalClass, swappedClass, swapFunction, unswapFunction);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder swaps(java.lang.Class<?>...values) {
			super.swaps(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder typeName(Class<?> on, String value) {
			super.typeName(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder typePropertyName(String value) {
			super.typePropertyName(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder typePropertyName(Class<?> on, String value) {
			super.typePropertyName(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder useEnumNames() {
			super.useEnumNames();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder useJavaBeanIntrospector() {
			super.useJavaBeanIntrospector();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder detectRecursions() {
			super.detectRecursions();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder detectRecursions(boolean value) {
			super.detectRecursions(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder ignoreRecursions() {
			super.ignoreRecursions();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder ignoreRecursions(boolean value) {
			super.ignoreRecursions(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder initialDepth(int value) {
			super.initialDepth(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder maxDepth(int value) {
			super.maxDepth(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder accept(String value) {
			super.accept(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder addBeanTypes() {
			super.addBeanTypes();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder addBeanTypes(boolean value) {
			super.addBeanTypes(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder addRootType() {
			super.addRootType();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder addRootType(boolean value) {
			super.addRootType(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder keepNullProperties() {
			super.keepNullProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder keepNullProperties(boolean value) {
			super.keepNullProperties(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder listener(Class<? extends org.apache.juneau.serializer.SerializerListener> value) {
			super.listener(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder produces(String value) {
			super.produces(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder sortCollections() {
			super.sortCollections();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder sortCollections(boolean value) {
			super.sortCollections(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder sortMaps() {
			super.sortMaps();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder sortMaps(boolean value) {
			super.sortMaps(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder trimEmptyCollections() {
			super.trimEmptyCollections();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder trimEmptyCollections(boolean value) {
			super.trimEmptyCollections(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder trimEmptyMaps() {
			super.trimEmptyMaps();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder trimEmptyMaps(boolean value) {
			super.trimEmptyMaps(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder trimStrings() {
			super.trimStrings();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder trimStrings(boolean value) {
			super.trimStrings(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder uriContext(UriContext value) {
			super.uriContext(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder uriRelativity(UriRelativity value) {
			super.uriRelativity(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder uriResolution(UriResolution value) {
			super.uriResolution(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder fileCharset(Charset value) {
			super.fileCharset(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder maxIndent(int value) {
			super.maxIndent(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder quoteChar(char value) {
			super.quoteChar(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder quoteCharOverride(char value) {
			super.quoteCharOverride(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder sq() {
			super.sq();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder streamCharset(Charset value) {
			super.streamCharset(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder useWhitespace() {
			super.useWhitespace();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder useWhitespace(boolean value) {
			super.useWhitespace(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder ws() {
			super.ws();
			return this;
		}

		// </FluentSetters>
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final boolean addBeanTypesJson, escapeSolidus, simpleAttrs;

	private final boolean addBeanTypes;
	private final Map<ClassMeta<?>,JsonClassMeta> jsonClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,JsonBeanPropertyMeta> jsonBeanPropertyMetas = new ConcurrentHashMap<>();

	private volatile JsonSchemaSerializer schemaSerializer;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public JsonSerializer(Builder builder) {
		super(builder);
		addBeanTypesJson = builder.addBeanTypesJson;
		simpleAttrs = builder.simpleAttrs;
		escapeSolidus = builder.escapeSolidus;

		addBeanTypes = addBeanTypesJson || super.isAddBeanTypes();
	}

	@Override /* Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Context */
	public JsonSerializerSession.Builder createSession() {
		return JsonSerializerSession.create(this);
	}

	@Override /* Context */
	public JsonSerializerSession getSession() {
		return createSession().build();
	}

	/**
	 * Returns the schema serializer based on the settings of this serializer.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #copy()} copies
	 * the settings of the object called on.
	 *
	 * @return The schema serializer.
	 */
	public JsonSchemaSerializer getSchemaSerializer() {
		if (schemaSerializer == null)
			schemaSerializer = JsonSchemaSerializer.create().beanContext(getBeanContext()).build();
		return schemaSerializer;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Extended metadata
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* JsonMetaProvider */
	public JsonClassMeta getJsonClassMeta(ClassMeta<?> cm) {
		JsonClassMeta m = jsonClassMetas.get(cm);
		if (m == null) {
			m = new JsonClassMeta(cm, this);
			jsonClassMetas.put(cm, m);
		}
		return m;
	}

	@Override /* JsonMetaProvider */
	public JsonBeanPropertyMeta getJsonBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return JsonBeanPropertyMeta.DEFAULT;
		JsonBeanPropertyMeta m = jsonBeanPropertyMetas.get(bpm);
		if (m == null) {
			m = new JsonBeanPropertyMeta(bpm.getDelegateFor(), this);
			jsonBeanPropertyMetas.put(bpm, m);
		}
		return m;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Add <js>"_type"</js> properties when needed.
	 *
	 * @see Builder#addBeanTypesJson()
	 * @return
	 * 	<jk>true</jk> if <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * 	through reflection.
	 */
	@Override
	protected final boolean isAddBeanTypes() {
		return addBeanTypes;
	}

	/**
	 * Prefix solidus <js>'/'</js> characters with escapes.
	 *
	 * @see Builder#escapeSolidus()
	 * @return
	 * 	<jk>true</jk> if solidus (e.g. slash) characters should be escaped.
	 */
	protected final boolean isEscapeSolidus() {
		return escapeSolidus;
	}

	/**
	 * Simple JSON mode.
	 *
	 * @see Builder#simpleAttrs()
	 * @return
	 * 	<jk>true</jk> if JSON attribute names will only be quoted when necessary.
	 * 	<br>Otherwise, they are always quoted.
	 */
	protected final boolean isSimpleAttrs() {
		return simpleAttrs;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	protected JsonMap properties() {
		return filteredMap("simpleAttrs", simpleAttrs, "escapeSolidus", escapeSolidus, "addBeanTypesJson", addBeanTypesJson);
	}
}