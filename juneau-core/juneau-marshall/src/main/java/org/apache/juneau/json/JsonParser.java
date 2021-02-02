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

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.parser.*;

/**
 * Parses any valid JSON text into a POJO model.
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <c>Content-Type</c> types:  <bc>application/json, text/json</bc>
 *
 * <h5 class='topic'>Description</h5>
 *
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
 * 		Unquoted attributes.
 * </ul>
 *
 * <p>
 * Also handles negative, decimal, hexadecimal, octal, and double numbers, including exponential notation.
 *
 * <p>
 * This parser handles the following input, and automatically returns the corresponding Java class.
 * <ul class='spaced-list'>
 * 	<li>
 * 		JSON objects (<js>"{...}"</js>) are converted to {@link OMap OMaps}.
 * 		<b>Note:</b>  If a <code><xa>_type</xa>=<xs>'xxx'</xs></code> attribute is specified on the object, then an
 * 		attempt is made to convert the object to an instance of the specified Java bean class.
 * 		See the <c>beanTypeName</c> setting on the {@link PropertyStore} for more information about parsing
 * 		beans from JSON.
 * 	<li>
 * 		JSON arrays (<js>"[...]"</js>) are converted to {@link OList OLists}.
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
 * 		<js>"{...}"</js> - Converted to an {@link OMap} or an instance of a Java bean if a <xa>_type</xa>
 * 		attribute is present.
 * 	<li>
 * 		<js>"[...]"</js> - Converted to an {@link OList}.
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
 * {@link OMap#OMap(CharSequence) OMap(CharSequence)} or {@link OList#OList(CharSequence)
 * OList(CharSequence)} constructors instead of using this class.
 * The end result should be the same.
 */
@ConfigurableContext
public class JsonParser extends ReaderParser implements JsonMetaProvider, JsonCommon {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "JsonParser";

	/**
	 * Configuration property:  Validate end.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.json.JsonParser#JSON_validateEnd JSON_validateEnd}
	 * 	<li><b>Name:</b>  <js>"JsonParser.validateEnd.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>JsonParser.validateEnd</c>
	 * 	<li><b>Environment variable:</b>  <c>JSONPARSER_VALIDATEEND</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.json.annotation.JsonConfig#validateEnd()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.json.JsonParserBuilder#validateEnd()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, after parsing a POJO from the input, verifies that the remaining input in
	 * the stream consists of only comments or whitespace.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a parser that validates that there's no garbage at the end of the input.</jc>
	 * 	ReaderParser p = JsonParser.
	 * 		.<jsm>create</jsm>()
	 * 		.validateEnd()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	ReaderParser p = JsonParser.
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>JSON_validateEnd</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Should fail because input has multiple POJOs.</jc>
	 * 	String in = <js>"{foo:'bar'}{baz:'qux'}"</js>;
	 * 	MyBean myBean = p.parse(in, MyBean.<jk>class</jk>);
	 * </p>
	 */
	public static final String JSON_validateEnd = PREFIX + ".validateEnd.b";

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Default parser, all default settings.*/
	public static final JsonParser DEFAULT = new JsonParser(PropertyStore.DEFAULT);

	/** Default parser, all default settings.*/
	public static final JsonParser DEFAULT_STRICT = new JsonParser.Strict(PropertyStore.DEFAULT);


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined subclasses
	//-------------------------------------------------------------------------------------------------------------------

	/** Default parser, strict mode. */
	public static class Strict extends JsonParser {

		/**
		 * Constructor.
		 *
		 * @param ps The property store containing all the settings for this object.
		 */
		public Strict(PropertyStore ps) {
			super(ps.builder().setDefault(PARSER_strict, true).setDefault(JSON_validateEnd, true).build());
		}
	}



	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final boolean validateEnd;
	private final Map<ClassMeta<?>,JsonClassMeta> jsonClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,JsonBeanPropertyMeta> jsonBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param ps The property store containing all the settings for this object.
	 */
	public JsonParser(PropertyStore ps) {
		this(ps, "application/json", "text/json");
	}

	/**
	 * Constructor.
	 *
	 * @param ps The property store containing all the settings for this object.
	 * @param consumes The list of media types that this parser consumes (e.g. <js>"application/json"</js>).
	 */
	public JsonParser(PropertyStore ps, String...consumes) {
		super(ps, consumes);
		validateEnd = getBooleanProperty(JSON_validateEnd);
	}

	@Override /* Context */
	public JsonParserBuilder builder() {
		return new JsonParserBuilder(getPropertyStore());
	}

	/**
	 * Instantiates a new clean-slate {@link JsonParserBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> JsonParserBuilder()</code>.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #builder()} copies
	 * the settings of the object called on.
	 *
	 * @return A new {@link JsonParserBuilder} object.
	 */
	public static JsonParserBuilder create() {
		return new JsonParserBuilder();
	}

	@Override /* Parser */
	public JsonParserSession createSession() {
		return createSession(createDefaultSessionArgs());
	}

	@Override /* Parser */
	public JsonParserSession createSession(ParserSessionArgs args) {
		return new JsonParserSession(this, args);
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
	 * @see #JSON_validateEnd
	 * @return
	 * 	<jk>true</jk> if after parsing a POJO from the input, verifies that the remaining input in
	 * 	the stream consists of only comments or whitespace.
	 */
	protected final boolean isValidateEnd() {
		return validateEnd;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"JsonParser",
				OMap
					.create()
					.filtered()
			);
	}
}
