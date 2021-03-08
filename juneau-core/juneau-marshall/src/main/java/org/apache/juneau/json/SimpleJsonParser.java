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

import org.apache.juneau.*;
import org.apache.juneau.collections.*;

/**
 * Parses any valid JSON text into a POJO model.
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <c>Content-Type</c> types:  <bc>application/json+simple, text/json+simple</bc>
 *
 * <h5 class='topic'>Description</h5>
 *
 * Identical to {@link JsonParser} but with the media type <bc>application/json+simple</bc>.
 */
public class SimpleJsonParser extends JsonParser {

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Default parser, Accept=application/json+simple. */
	public static final SimpleJsonParser DEFAULT = new SimpleJsonParser(ContextProperties.DEFAULT);

	/**
	 * Constructor.
	 *
	 * @param cp The property store containing all the settings for this object.
	 */
	public SimpleJsonParser(ContextProperties cp) {
		super(cp, "application/json+simple", "text/json+simple", "application/json", "text/json");
	}

	@Override /* Context */
	public SimpleJsonParserBuilder copy() {
		return new SimpleJsonParserBuilder(this);
	}

	/**
	 * Instantiates a new clean-slate {@link SimpleJsonParserBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> SimpleJsonParserBuilder()</code>.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #copy()} copies
	 * the settings of the object called on.
	 *
	 * @return A new {@link SimpleJsonParserBuilder} object.
	 */
	public static SimpleJsonParserBuilder create() {
		return new SimpleJsonParserBuilder();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"SimpleJsonSerializer",
				OMap
					.create()
					.filtered()
			);
	}
}