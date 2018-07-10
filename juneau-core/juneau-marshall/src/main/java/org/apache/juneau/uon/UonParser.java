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

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.urlencoding.*;

/**
 * Parses UON (a notation for URL-encoded query parameter values) text into POJO models.
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <code>Content-Type</code> types:  <code><b>text/uon</b></code>
 *
 * <h5 class='topic'>Description</h5>
 *
 * This parser uses a state machine, which makes it very fast and efficient.
 */
public class UonParser extends ReaderParser {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	private static final String PREFIX = "UonParser.";

	/**
	 * Configuration property: Decode <js>"%xx"</js> sequences.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"UonParser.decoding.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk> for {@link UonParser}, <jk>true</jk> for {@link UrlEncodingParser}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link UonParserBuilder#decoding(boolean)}
	 * 			<li class='jm'>{@link UonParserBuilder#decoding()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specify <jk>true</jk> if URI encoded characters should be decoded, <jk>false</jk> if they've already been decoded
	 * before being passed to this parser.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Create a decoding UON parser.</jc>
	 * 	ReaderParser p = UonParser.
	 * 		.<jsm>create</jsm>()
	 * 		.decoding()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	ReaderParser p = UonParser.
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>UON_decoding</jsf>, <jk>true</jk>)
	 * 		.build();
	 *
	 * <jc>// Produces: ["foo bar", "baz quz"].</jc>
	 * 	String[] foo = p.parse(<js>"@(foo%20bar,baz%20qux)"</js>, String[].<jk>class</jk>);
	 * </p>
	 */
	public static final String UON_decoding = PREFIX + "decoding.b";

	/**
	 * Configuration property:  Validate end.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"UonParser.validateEnd.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link UonParserBuilder#validateEnd(boolean)}
	 * 			<li class='jm'>{@link UonParserBuilder#validateEnd()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, after parsing a POJO from the input, verifies that the remaining input in
	 * the stream consists of only comments or whitespace.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Create a parser using strict mode.</jc>
	 * 	ReaderParser p = UonParser.
	 * 		.<jsm>create</jsm>()
	 * 		.validateEnd()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	ReaderParser p = UonParser.
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>UON_validateEnd</jsf>, <jk>true</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Should fail because input has multiple POJOs.</jc>
	 * 	String in = <js>"(foo=bar)(baz=qux)"</js>;
	 * 	MyBean myBean = p.parse(in, MyBean.<jk>class</jk>);
	 * </p>
	 */
	public static final String UON_validateEnd = PREFIX + "validateEnd.b";

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Reusable instance of {@link UonParser}, all default settings. */
	public static final UonParser DEFAULT = new UonParser(PropertyStore.DEFAULT);

	/** Reusable instance of {@link UonParser} with decodeChars set to true. */
	public static final UonParser DEFAULT_DECODING = new UonParser.Decoding(PropertyStore.DEFAULT);


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined subclasses
	//-------------------------------------------------------------------------------------------------------------------

	/** Default parser, decoding. */
	public static class Decoding extends UonParser {

		/**
		 * Constructor.
		 *
		 * @param ps The property store containing all the settings for this object.
		 */
		public Decoding(PropertyStore ps) {
			super(ps.builder().set(UON_decoding, true).build());
		}
	}


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final boolean
		decodeChars, validateEnd;

	/**
	 * Constructor.
	 *
	 * @param ps
	 * 	The property store containing all the settings for this object.
	 */
	public UonParser(PropertyStore ps) {
		this(ps, "text/uon");
	}

	/**
	 * Constructor.
	 *
	 * @param ps
	 * 	The property store containing all the settings for this object.
	 * @param consumes
	 * 	The list of media types that this parser consumes (e.g. <js>"application/json"</js>, <js>"*&#8203;/json"</js>).
	 */
	public UonParser(PropertyStore ps, String...consumes) {
		super(ps, consumes);
		this.decodeChars = getBooleanProperty(UON_decoding, false);
		this.validateEnd = getBooleanProperty(UON_validateEnd, false);
	}

	@Override /* Context */
	public UonParserBuilder builder() {
		return new UonParserBuilder(getPropertyStore());
	}

	/**
	 * Instantiates a new clean-slate {@link UonParserBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> UonParserBuilder()</code>.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #builder()} copies
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
		return new UonParserSession(this, createDefaultSessionArgs(), false);
	}

	@Override /* Parser */
	public UonParserSession createSession(ParserSessionArgs args) {
		return new UonParserSession(this, args);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property: Decode <js>"%xx"</js> sequences.
	 *
	 * @see #UON_decoding
	 * @return
	 * 	<jk>true</jk> if URI encoded characters should be decoded, <jk>false</jk> if they've already been decoded
	 * 	before being passed to this parser.
	 */
	protected final boolean isDecodeChars() {
		return decodeChars;
	}

	/**
	 * Configuration property:  Validate end.
	 *
	 * @see #UON_validateEnd
	 * @return
	 * 	<jk>true</jk> if after parsing a POJO from the input, verifies that the remaining input in
	 * 	the stream consists of only comments or whitespace.
	 */
	protected final boolean isValidateEnd() {
		return validateEnd;
	}

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("UonParser", new ObjectMap()
				.append("decodeChars", decodeChars)
			);
	}
}
