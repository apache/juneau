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
package org.apache.juneau.yaml.proto;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;

/**
 * @deprecated Never implemented.
 */
@Deprecated
public class YamlParser extends ReaderParser {

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Default parser, all default settings.*/
	public static final YamlParser DEFAULT = new YamlParser(PropertyStore.DEFAULT);

	/** Default parser, all default settings.*/
	public static final YamlParser DEFAULT_STRICT = new YamlParser.Strict(PropertyStore.DEFAULT);


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined subclasses
	//-------------------------------------------------------------------------------------------------------------------

	/** Default parser, strict mode. */
	public static class Strict extends YamlParser {

		/**
		 * Constructor.
		 *
		 * @param ps The property store containing all the settings for this object.
		 */
		public Strict(PropertyStore ps) {
			super(ps.builder().set(PARSER_strict, true).build());
		}
	}


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param ps The property store containing all the settings for this object.
	 */
	public YamlParser(PropertyStore ps) {
		this(ps, "application/json", "text/json");
	}

	/**
	 * Constructor.
	 *
	 * @param ps The property store containing all the settings for this object.
	 * @param consumes The list of media types that this parser consumes (e.g. <js>"application/json"</js>).
	 */
	public YamlParser(PropertyStore ps, String...consumes) {
		super(ps, consumes);
	}

	@Override /* Context */
	public YamlParserBuilder builder() {
		return new YamlParserBuilder(getPropertyStore());
	}

	/**
	 * Instantiates a new clean-slate {@link YamlParserBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> YamlParserBuilder()</code>.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #builder()} copies
	 * the settings of the object called on.
	 *
	 * @return A new {@link YamlParserBuilder} object.
	 */
	public static YamlParserBuilder create() {
		return new YamlParserBuilder();
	}

	@Override /* Parser */
	public ReaderParserSession createSession(ParserSessionArgs args) {
		return new YamlParserSession(this, args);
	}

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("YamlParser", new ObjectMap());
	}
}
