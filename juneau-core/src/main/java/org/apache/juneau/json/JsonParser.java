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

import static org.apache.juneau.parser.ParserContext.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.parser.*;

/**
 * Parses any valid JSON text into a POJO model.
 *
 * <h5 class='section'>Media types:</h5>
 *
 * Handles <code>Content-Type</code> types: <code>application/json, text/json</code>
 *
 * <h5 class='section'>Description:</h5>
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
 * 		JSON objects (<js>"{...}"</js>) are converted to {@link ObjectMap ObjectMaps}.
 * 		<b>Note:</b>  If a <code><xa>_type</xa>=<xs>'xxx'</xs></code> attribute is specified on the object, then an
 * 		attempt is made to convert the object to an instance of the specified Java bean class.
 * 		See the <code>beanTypeName</code> setting on the {@link PropertyStore} for more information about parsing
 * 		beans from JSON.
 * 	<li>
 * 		JSON arrays (<js>"[...]"</js>) are converted to {@link ObjectList ObjectLists}.
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
 * 		<js>"{...}"</js> - Converted to a {@link ObjectMap} or an instance of a Java bean if a <xa>_type</xa>
 * 		attribute is present.
 * 	<li>
 * 		<js>"[...]"</js> - Converted to a {@link ObjectList}.
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
 * {@link ObjectMap#ObjectMap(CharSequence) ObjectMap(CharSequence)} or {@link ObjectList#ObjectList(CharSequence)
 * ObjectList(CharSequence)} constructors instead of using this class.
 * The end result should be the same.
 *
 * <h5 class='section'>Configurable properties:</h5>
 *
 * This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link JsonParserContext}
 * </ul>
 */
@Consumes("application/json,text/json")
public class JsonParser extends ReaderParser {

	/** Default parser, all default settings.*/
	public static final JsonParser DEFAULT = new JsonParser(PropertyStore.create());

	/** Default parser, all default settings.*/
	public static final JsonParser DEFAULT_STRICT = new JsonParser.Strict(PropertyStore.create());

	/** Default parser, strict mode. */
	public static class Strict extends JsonParser {

		/**
		 * Constructor.
		 *
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public Strict(PropertyStore propertyStore) {
			super(propertyStore.copy().append(PARSER_strict, true));
		}
	}


	private final JsonParserContext ctx;

	/**
	 * Constructor.
	 *
	 * @param propertyStore The property store containing all the settings for this object.
	 */
	public JsonParser(PropertyStore propertyStore) {
		super(propertyStore);
		this.ctx = createContext(JsonParserContext.class);
	}

	@Override /* CoreObject */
	public JsonParserBuilder builder() {
		return new JsonParserBuilder(propertyStore);
	}

	@Override /* Parser */
	public ReaderParserSession createSession(ParserSessionArgs args) {
		return new JsonParserSession(ctx, args);
	}
}
