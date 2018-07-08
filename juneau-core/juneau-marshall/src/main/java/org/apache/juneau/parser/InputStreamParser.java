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
package org.apache.juneau.parser;

import org.apache.juneau.*;

/**
 * Subclass of {@link Parser} for byte-based parsers.
 *
 * <h5 class='topic'>Description</h5>
 *
 * This class is typically the parent class of all byte-based parsers.
 * It has 1 abstract method to implement...
 * <ul>
 * 	<li><code>parse(InputStream, ClassMeta, Parser)</code>
 * </ul>
  */
public abstract class InputStreamParser extends Parser {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	private static final String PREFIX = "InputStreamParser.";


	/**
	 * Configuration property:  Binary input format.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"InputStreamParser.binaryFormat.s"</js>
	 * 	<li><b>Data type:</b>  {@link BinaryFormat}
	 * 	<li><b>Default:</b>  {@link BinaryFormat#HEX}
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link InputStreamParserBuilder#binaryFormat(BinaryFormat)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * When using the {@link #parse(Object,Class)} method on stream-based parsers and the input is a string, this defines the format to use
	 * when converting the string into a byte array.
	 *
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Create a parser that parses from BASE64.</jc>
	 * 	InputStreamParser p = MsgPackParser
	 * 		.<jsm>create</jsm>()
	 * 		.binaryFormat(<jsf>BASE64</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	InputStreamParser p = MsgPackParser
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>ISPARSER_binaryFormat</jsf>, <js>"BASE64"</js>)
	 * 		.build();
	 * </p>
	 */
	public static final String ISPARSER_binaryFormat = PREFIX + "binaryFormat.s";

	static final InputStreamParser DEFAULT = new InputStreamParser(PropertyStore.create().build(), "") {
		@Override
		public InputStreamParserSession createSession(ParserSessionArgs args) {
			throw new NoSuchMethodError();
		}
	};

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final BinaryFormat binaryFormat;

	/**
	 * Constructor.
	 *
	 * @param ps The property store containing all the settings for this object.
	 * @param consumes The list of media types that this parser consumes (e.g. <js>"application/json"</js>).
	 */
	protected InputStreamParser(PropertyStore ps, String...consumes) {
		super(ps, consumes);
		binaryFormat = getProperty(ISPARSER_binaryFormat, BinaryFormat.class, BinaryFormat.HEX);
	}

	@Override /* Parser */
	public final boolean isReaderParser() {
		return false;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Binary input format.
	 *
	 * @see #ISPARSER_binaryFormat
	 * @return
	 * 	The format to use when converting strings to byte arrays.
	 */
	protected final BinaryFormat getBinaryFormat() {
		return binaryFormat;
	}

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("InputStreamParser", new ObjectMap()
				.append("binaryFormat", binaryFormat)
			);
	}
}
