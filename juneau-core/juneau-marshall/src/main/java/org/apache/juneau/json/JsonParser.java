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

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.utils.*;

/**
 * Parses any valid JSON text into a POJO model.
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Content-Type</c> types:  <bc>application/json, text/json</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * This parser uses a state machine, which makes it very fast and efficient.  It parses JSON in about 70% of the
 * time that it takes the built-in Java DOM parsers to parse equivalent XML.
 *
 * <p>
 * This parser handles all valid JSON syntax.
 * In addition, when strict mode is disable, the parser also handles the following:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Javascript comments (both {@code /*} and {@code //}) are ignored.
 * 	<li>
 * 		Both single and double quoted strings.
 * 	<li>
 * 		Automatically joins concatenated strings (e.g. <code><js>"aaa"</js> + <js>'bbb'</js></code>).
 * 	<li>
 * 		Unquoted attributes and values.
 * </ul>
 *
 * <p>
 * Also handles negative, decimal, hexadecimal, octal, and double numbers, including exponential notation.
 *
 * <p>
 * This parser handles the following input, and automatically returns the corresponding Java class.
 * <ul class='spaced-list'>
 * 	<li>
 * 		JSON objects (<js>"{...}"</js>) are converted to {@link JsonMap JsonMaps}.
 * 		<b>Note:</b>  If a <code><xa>_type</xa>=<xs>'xxx'</xs></code> attribute is specified on the object, then an
 * 		attempt is made to convert the object to an instance of the specified Java bean class.
 * 		See the {@link org.apache.juneau.BeanContext.Builder#typePropertyName(String)} setting for more information about parsing
 * 		beans from JSON.
 * 	<li>
 * 		JSON arrays (<js>"[...]"</js>) are converted to {@link JsonList JsonLists}.
 * 	<li>
 * 		JSON string literals (<js>"'xyz'"</js>) are converted to {@link String Strings}.
 * 	<li>
 * 		JSON numbers (<js>"123"</js>, including octal/hexadecimal/exponential notation) are converted to
 * 		{@link Integer Integers}, {@link Long Longs}, {@link Float Floats}, or {@link Double Doubles} depending on
 * 		whether the number is decimal, and the size of the number.
 * 	<li>
 * 		JSON booleans (<js>"false"</js>) are converted to {@link Boolean Booleans}.
 * 	<li>
 * 		JSON nulls (<js>"null"</js>) are converted to <jk>null</jk>.
 * 	<li>
 * 		Input consisting of only whitespace or JSON comments are converted to <jk>null</jk>.
 * </ul>
 *
 * <p>
 * Input can be any of the following:
 * <ul class='spaced-list'>
 * 	<li>
 * 		<js>"{...}"</js> - Converted to an {@link JsonMap} or an instance of a Java bean if a <xa>_type</xa>
 * 		attribute is present.
 * 	<li>
 * 		<js>"[...]"</js> - Converted to an {@link JsonList}.
 * 	<li>
 * 		<js>"123..."</js> - Converted to a {@link Number} (either {@link Integer}, {@link Long}, {@link Float},
 * 		or {@link Double}).
 * 	<li>
 * 		<js>"true"</js>/<js>"false"</js> - Converted to a {@link Boolean}.
 * 	<li>
 * 		<js>"null"</js> - Returns <jk>null</jk>.
 * 	<li>
 * 		<js>"'xxx'"</js> - Converted to a {@link String}.
 * 	<li>
 * 		<js>"\"xxx\""</js> - Converted to a {@link String}.
 * 	<li>
 * 		<js>"'xxx' + \"yyy\""</js> - Converted to a concatenated {@link String}.
 * </ul>
 *
 * <p>
 * TIP:  If you know you're parsing a JSON object or array, it can be easier to parse it using the
 * {@link JsonMap#JsonMap(CharSequence) JsonMap(CharSequence)} or {@link JsonList#JsonList(CharSequence)
 * JsonList(CharSequence)} constructors instead of using this class.
 * The end result should be the same.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.JsonDetails">JSON Details</a>
 * </ul>
 */
public class JsonParser extends ReaderParser implements JsonMetaProvider {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	/** Default parser, all default settings.*/
	public static final JsonParser DEFAULT = new JsonParser(create());

	/** Default parser, all default settings.*/
	public static final JsonParser DEFAULT_STRICT = new JsonParser.Strict(create());

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

	/** Default parser, strict mode. */
	public static class Strict extends JsonParser {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public Strict(Builder builder) {
			super(builder.strict().validateEnd());
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Builder
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends ReaderParser.Builder {

		private static final Cache<HashKey,JsonParser> CACHE = Cache.of(HashKey.class, JsonParser.class).build();

		boolean validateEnd;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			super();
			consumes("application/json,text/json");
			validateEnd = env("JsonParser.validateEnd", false);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(JsonParser copyFrom) {
			super(copyFrom);
			validateEnd = copyFrom.validateEnd;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
			validateEnd = copyFrom.validateEnd;
		}

		@Override /* Context.Builder */
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* Context.Builder */
		public JsonParser build() {
			return cache(CACHE).build(JsonParser.class);
		}

		@Override /* Context.Builder */
		public HashKey hashKey() {
			return HashKey.of(
				super.hashKey(),
				validateEnd
			);
		}

		//-----------------------------------------------------------------------------------------------------------------
		// Properties
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Validate end.
		 *
		 * <p>
		 * When enabled, after parsing a POJO from the input, verifies that the remaining input in
		 * the stream consists of only comments or whitespace.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a parser that validates that there's no garbage at the end of the input.</jc>
		 * 	ReaderParser <jv>parser</jv> = JsonParser.
		 * 		.<jsm>create</jsm>()
		 * 		.validateEnd()
		 * 		.build();
		 *
		 * 	<jc>// Should fail because input has multiple POJOs.</jc>
		 * 	String <jv>json</jv> = <js>"{foo:'bar'}{baz:'qux'}"</js>;
		 * 	MyBean <jv>myBean</jv> =<jv>parser</jv>.parse(<jv>json</jv>, MyBean.<jk>class</jk>);
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder validateEnd() {
			return validateEnd(true);
		}

		/**
		 * Same as {@link #validateEnd()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder validateEnd(boolean value) {
			validateEnd = value;
			return this;
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

		@Override /* GENERATED - org.apache.juneau.parser.Parser.Builder */
		public Builder autoCloseStreams() {
			super.autoCloseStreams();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.Parser.Builder */
		public Builder autoCloseStreams(boolean value) {
			super.autoCloseStreams(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.Parser.Builder */
		public Builder consumes(String value) {
			super.consumes(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.Parser.Builder */
		public Builder debugOutputLines(int value) {
			super.debugOutputLines(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.Parser.Builder */
		public Builder listener(Class<? extends org.apache.juneau.parser.ParserListener> value) {
			super.listener(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.Parser.Builder */
		public Builder strict() {
			super.strict();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.Parser.Builder */
		public Builder strict(boolean value) {
			super.strict(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.Parser.Builder */
		public Builder trimStrings() {
			super.trimStrings();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.Parser.Builder */
		public Builder trimStrings(boolean value) {
			super.trimStrings(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.Parser.Builder */
		public Builder unbuffered() {
			super.unbuffered();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.Parser.Builder */
		public Builder unbuffered(boolean value) {
			super.unbuffered(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.ReaderParser.Builder */
		public Builder fileCharset(Charset value) {
			super.fileCharset(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.ReaderParser.Builder */
		public Builder streamCharset(Charset value) {
			super.streamCharset(value);
			return this;
		}

		// </FluentSetters>
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final boolean validateEnd;

	private final Map<ClassMeta<?>,JsonClassMeta> jsonClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,JsonBeanPropertyMeta> jsonBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public JsonParser(Builder builder) {
		super(builder);
		validateEnd = builder.validateEnd;
	}

	@Override /* Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Context */
	public JsonParserSession.Builder createSession() {
		return JsonParserSession.create(this);
	}

	@Override /* Context */
	public JsonParserSession getSession() {
		return createSession().build();
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
	 * Validate end.
	 *
	 * @see Builder#validateEnd()
	 * @return
	 * 	<jk>true</jk> if after parsing a POJO from the input, verifies that the remaining input in
	 * 	the stream consists of only comments or whitespace.
	 */
	protected final boolean isValidateEnd() {
		return validateEnd;
	}
}
