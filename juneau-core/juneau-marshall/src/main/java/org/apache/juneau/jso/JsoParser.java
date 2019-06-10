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
package org.apache.juneau.jso;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.parser.*;

/**
 * Parses POJOs from HTTP responses as Java {@link ObjectInputStream ObjectInputStreams}.
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Consumes <code>Content-Type</code> types:  <code><b>application/x-java-serialized-object</b></code>
 */
@ConfigurableContext
public final class JsoParser extends InputStreamParser {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "JsoParser";

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Default parser, all default settings.*/
	public static final JsoParser DEFAULT = new JsoParser(PropertyStore.DEFAULT);

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param ps The property store containing all the settings for this object.
	 */
	public JsoParser(PropertyStore ps) {
		super(ps, "application/x-java-serialized-object");
	}

	@Override /* Context */
	public JsoParserBuilder builder() {
		return new JsoParserBuilder(getPropertyStore());
	}

	/**
	 * Instantiates a new clean-slate {@link JsoParserBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> JsoParserBuilder()</code>.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #builder()} copies
	 * the settings of the object called on.
	 *
	 * @return A new {@link JsoParserBuilder} object.
	 */
	public static JsoParserBuilder create() {
		return new JsoParserBuilder();
	}

	@Override /* Parser */
	public JsoParserSession createSession() {
		return createSession(createDefaultSessionArgs());
	}

	@Override /* Parser */
	public JsoParserSession createSession(ParserSessionArgs args) {
		return new JsoParserSession(args);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("JsoParser", new DefaultFilteringObjectMap()
			);
	}
}
