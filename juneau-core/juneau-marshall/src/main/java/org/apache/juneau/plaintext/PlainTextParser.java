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
package org.apache.juneau.plaintext;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.transform.*;

/**
 * Parsers HTTP plain text request bodies into Group 5 POJOs.
 *
 * <p>
 * See {@doc PojoCategories}.
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <code>Accept</code> types:  <code><b>text/plain</b></code>
 * <p>
 * Produces <code>Content-Type</code> types:  <code><b>text/plain</b></code>
 *
 * <h5 class='topic'>Description</h5>
 *
 * Essentially just converts plain text to POJOs via static <code>fromString()</code> or <code>valueOf()</code>, or
 * through constructors that take a single string argument.
 *
 * <p>
 * Also parses objects using a transform if the object class has an {@link PojoSwap PojoSwap&lt;?,String&gt;} transform
 * defined on it.
 */
@ConfigurableContext
public class PlainTextParser extends ReaderParser {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "PlainTextParser";

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined subclasses
	//-------------------------------------------------------------------------------------------------------------------

	/** Default parser, all default settings.*/
	public static final PlainTextParser DEFAULT = new PlainTextParser(PropertyStore.DEFAULT);


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param ps The property store containing all the settings for this object.
	 */
	public PlainTextParser(PropertyStore ps) {
		this(ps, "text/plain");
	}

	/**
	 * Constructor.
	 *
	 * @param ps The property store containing all the settings for this object.
	 * @param consumes The media types that this parser consumes.
	 * 	<p>
	 * 	Can contain meta-characters per the <code>media-type</code> specification of {@doc RFC2616.section14.1}
	 */
	public PlainTextParser(PropertyStore ps, String...consumes) {
		super(ps, consumes);
	}

	@Override /* Context */
	public PlainTextParserBuilder builder() {
		return new PlainTextParserBuilder(getPropertyStore());
	}

	/**
	 * Instantiates a new clean-slate {@link PlainTextParserBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> PlainTextParserBuilder()</code>.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #builder()} copies
	 * the settings of the object called on.
	 *
	 * @return A new {@link PlainTextParserBuilder} object.
	 */
	public static PlainTextParserBuilder create() {
		return new PlainTextParserBuilder();
	}

	@Override /* Parser */
	public PlainTextParserSession createSession() {
		return createSession(createDefaultSessionArgs());
	}

	@Override /* Parser */
	public PlainTextParserSession createSession(ParserSessionArgs args) {
		return new PlainTextParserSession(this, args);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public ObjectMap toMap() {
		return super.toMap()
			.append("PlainTextParser", new DefaultFilteringObjectMap()
			);
	}
}
