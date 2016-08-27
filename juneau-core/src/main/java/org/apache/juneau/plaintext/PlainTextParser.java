/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.plaintext;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.transform.*;

/**
 * Parsers HTTP plain text request bodies into <a href='../package-summary.html#PojoCategories'>Group 5</a> POJOs.
 *
 *
 * <h6 class='topic'>Media types</h6>
 * <p>
 * 	Handles <code>Accept</code> types: <code>text/plain</code>
 * <p>
 * 	Produces <code>Content-Type</code> types: <code>text/plain</code>
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	Essentially just converts plain text to POJOs via static <code>fromString()</code> or <code>valueOf()</code>, or
 * 	through constructors that take a single string argument.
 * <p>
 * 	Also parses objects using a transform if the object class has an {@link PojoSwap PojoSwap&lt;?,String&gt;} transform defined on it.
 *
 *
 * <h6 class='topic'>Configurable properties</h6>
 * <p>
 * 	This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link ParserContext}
 * </ul>
 *
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@Consumes("text/plain")
public final class PlainTextParser extends ReaderParser {

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Parser */
	protected <T> T doParse(ParserSession session, ClassMeta<T> type) throws Exception {
		return session.getBeanContext().convertToType(IOUtils.read(session.getReader()), type);
	}

	@Override /* Lockable */
	public PlainTextParser clone() {
		try {
			return (PlainTextParser)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen.
		}
	}
}
