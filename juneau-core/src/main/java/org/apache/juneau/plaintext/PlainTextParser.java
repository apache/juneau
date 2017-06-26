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

import static org.apache.juneau.internal.IOUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.transform.*;

/**
 * Parsers HTTP plain text request bodies into <a class="doclink"
 * href="../../../../overview-summary.html#Core.PojoCategories">Group 5</a> POJOs.
 *
 * <h5 class='section'>Media types:</h5>
 * <p>
 * Handles <code>Accept</code> types: <code>text/plain</code>
 * <p>
 * Produces <code>Content-Type</code> types: <code>text/plain</code>
 *
 * <h5 class='section'>Description:</h5>
 * <p>
 * Essentially just converts plain text to POJOs via static <code>fromString()</code> or <code>valueOf()</code>, or
 * through constructors that take a single string argument.
 * <p>
 * Also parses objects using a transform if the object class has an {@link PojoSwap PojoSwap&lt;?,String&gt;} transform
 * defined on it.
 *
 * <h5 class='section'>Configurable properties:</h5>
 * <p>
 * This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link ParserContext}
 * </ul>
 */
@Consumes("text/plain")
public class PlainTextParser extends ReaderParser {

	/** Default parser, all default settings.*/
	public static final PlainTextParser DEFAULT = new PlainTextParser(PropertyStore.create());


	/**
	 * Constructor.
	 *
	 * @param propertyStore The property store containing all the settings for this object.
	 */
	public PlainTextParser(PropertyStore propertyStore) {
		super(propertyStore);
	}

	@Override /* CoreObject */
	public PlainTextParserBuilder builder() {
		return new PlainTextParserBuilder(propertyStore);
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Parser */
	protected <T> T doParse(ParserSession session, ClassMeta<T> type) throws Exception {
		return session.convertToType(read(session.getReader()), type);
	}
}
