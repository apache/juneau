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
 * <h5 class='section'>Media types:</h5>
 * <p>
 * Consumes <code>Content-Type</code> types: <code>application/x-java-serialized-object</code>
 */
@Consumes("application/x-java-serialized-object")
public final class JsoParser extends InputStreamParser {

	/** Default parser, all default settings.*/
	public static final JsoParser DEFAULT = new JsoParser(PropertyStore.create());


	/**
	 * Constructor.
	 *
	 * @param propertyStore The property store containing all the settings for this object.
	 */
	public JsoParser(PropertyStore propertyStore) {
		super(propertyStore);
	}

	@Override /* CoreObject */
	public JsoParserBuilder builder() {
		return new JsoParserBuilder(propertyStore);
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	@Override /* InputStreamParser */
	protected <T> T doParse(ParserSession session, ClassMeta<T> type) throws Exception {
		ObjectInputStream ois = new ObjectInputStream(session.getInputStream());
		return (T)ois.readObject();
	}
}
