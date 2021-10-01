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
package org.apache.juneau.uon;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.parser.*;

/**
 * Parses UON (a notation for URL-encoded query parameter values) text into POJO models.
 * {@review}
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <c>Content-Type</c> types:  <bc>text/uon</bc>
 *
 * <h5 class='topic'>Description</h5>
 *
 * This parser uses a state machine, which makes it very fast and efficient.
 */
public class UonParser extends ReaderParser implements HttpPartParser, UonMetaProvider {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	/** Reusable instance of {@link UonParser}, all default settings. */
	public static final UonParser DEFAULT = new UonParser(create());

	/** Reusable instance of {@link UonParser} with decodeChars set to true. */
	public static final UonParser DEFAULT_DECODING = new UonParser.Decoding(create());

	//-------------------------------------------------------------------------------------------------------------------
	// Static subclasses
	//-------------------------------------------------------------------------------------------------------------------

	/** Default parser, decoding. */
	public static class Decoding extends UonParser {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		protected Decoding(UonParserBuilder builder) {
			super(builder.decoding());
		}
	}

	/**
	 * Converts the specified input to the specified class type.
	 *
	 * @param partType The part type being parsed.
	 * @param schema
	 * 	Schema information about the part.
	 * 	<br>May be <jk>null</jk>.
	 * 	<br>Not all part parsers use the schema information.
	 * @param in The input being parsed.
	 * @param toType The POJO type to transform the input into.
	 * @return The parsed value.
	 * @throws ParseException Malformed input encountered.
	 * @throws SchemaValidationException If the input or resulting HTTP part object fails schema validation.
	 */
	public <T> T parse(HttpPartType partType, HttpPartSchema schema, String in, ClassMeta<T> toType) throws ParseException, SchemaValidationException {
		return createPartSession(null).parse(partType, schema, in, toType);
	}

	/**
	 * Converts the specified input to the specified class type.
	 *
	 * @param partType The part type being parsed.
	 * @param schema
	 * 	Schema information about the part.
	 * 	<br>May be <jk>null</jk>.
	 * 	<br>Not all part parsers use the schema information.
	 * @param in The input being parsed.
	 * @param toType The POJO type to transform the input into.
	 * @return The parsed value.
	 * @throws ParseException Malformed input encountered.
	 * @throws SchemaValidationException If the input or resulting HTTP part object fails schema validation.
	 */
	public <T> T parse(HttpPartType partType, HttpPartSchema schema, String in, Class<T> toType) throws ParseException, SchemaValidationException {
		return createPartSession(null).parse(partType, schema, in, getClassMeta(toType));
	}

	/**
	 * Converts the specified input to the specified class type.
	 *
	 * @param partType The part type being parsed.
	 * @param schema
	 * 	Schema information about the part.
	 * 	<br>May be <jk>null</jk>.
	 * 	<br>Not all part parsers use the schema information.
	 * @param in The input being parsed.
	 * @param toType The POJO type to transform the input into.
	 * @param toTypeArgs The generic type arguments of the POJO type to transform the input into.
	 * @return The parsed value.
	 * @throws ParseException Malformed input encountered.
	 * @throws SchemaValidationException If the input or resulting HTTP part object fails schema validation.
	 */
	public <T> T parse(HttpPartType partType, HttpPartSchema schema, String in, Type toType, Type...toTypeArgs) throws ParseException, SchemaValidationException {
		return createPartSession(null).parse(partType, schema, in, getClassMeta(toType, toTypeArgs));
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final boolean decoding, validateEnd;

	private final Map<ClassMeta<?>,UonClassMeta> uonClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,UonBeanPropertyMeta> uonBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected UonParser(UonParserBuilder builder) {
		super(builder);
		decoding = builder.decoding;
		validateEnd = builder.validateEnd;
	}

	@Override /* Context */
	public UonParserBuilder copy() {
		return new UonParserBuilder(this);
	}

	/**
	 * Instantiates a new clean-slate {@link UonParserBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> UonParserBuilder()</code>.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #copy()} copies
	 * the settings of the object called on.
	 *
	 * @return A new {@link UonParserBuilder} object.
	 */
	public static UonParserBuilder create() {
		return new UonParserBuilder();
	}

	/**
	 * Create a UON parser session for parsing parameter values.
	 *
	 * @return A new parser session.
	 */
	protected final UonParserSession createParameterSession() {
		return new UonParserSession(this, defaultArgs(), false);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Entry point methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Parser */
	public UonParserSession createSession(ParserSessionArgs args) {
		return new UonParserSession(this, args);
	}

	@Override /* HttpPartParser */
	public UonParserSession createSession() {
		return createSession(null);
	}

	@Override /* HttpPartParser */
	public UonParserSession createPartSession(ParserSessionArgs args) {
		return new UonParserSession(this, args);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Extended metadata
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* UonMetaProvider */
	public UonClassMeta getUonClassMeta(ClassMeta<?> cm) {
		UonClassMeta m = uonClassMetas.get(cm);
		if (m == null) {
			m = new UonClassMeta(cm, this);
			uonClassMetas.put(cm, m);
		}
		return m;
	}

	@Override /* UonMetaProvider */
	public UonBeanPropertyMeta getUonBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return UonBeanPropertyMeta.DEFAULT;
		UonBeanPropertyMeta m = uonBeanPropertyMetas.get(bpm);
		if (m == null) {
			m = new UonBeanPropertyMeta(bpm.getDelegateFor(), this);
			uonBeanPropertyMetas.put(bpm, m);
		}
		return m;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Decode <js>"%xx"</js> sequences enabled
	 *
	 * @see UonParserBuilder#decoding()
	 * @return
	 * 	<jk>true</jk> if URI encoded characters should be decoded, <jk>false</jk> if they've already been decoded
	 * 	before being passed to this parser.
	 */
	protected final boolean isDecoding() {
		return decoding;
	}

	/**
	 * Validate end enabled.
	 *
	 * @see UonParserBuilder#validateEnd()
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

	@Override
	public <T> ClassMeta<T> getClassMeta(Class<T> c) {
		return getBeanContext().getClassMeta(c);
	}

	@Override
	public <T> ClassMeta<T> getClassMeta(Type t, Type... args) {
		return getBeanContext().getClassMeta(t, args);
	}

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"UonParser",
				OMap
					.create()
					.filtered()
					.a("decoding", decoding)
					.a("validateEnd", validateEnd)
			);
	}
}
