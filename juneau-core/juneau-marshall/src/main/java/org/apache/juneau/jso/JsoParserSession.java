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
import org.apache.juneau.collections.*;
import org.apache.juneau.parser.*;

/**
 * Session object that lives for the duration of a single use of {@link JsoParser}.
 *
 * <p>
 * This class is NOT thread safe.
 * It is typically discarded after one-time use although it can be reused against multiple inputs.
 */
@SuppressWarnings("unchecked")
public class JsoParserSession extends InputStreamParserSession {

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param args
	 * 	Runtime session arguments.
	 */
	protected JsoParserSession(ParserSessionArgs args) {
		super(args);
	}

	@Override /* ParserSession */
	protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException {
		ObjectInputStream ois = new ObjectInputStream(pipe.getInputStream());
		try {
			return (T)ois.readObject();
		} catch (ClassNotFoundException e) {
			throw new ParseException(e);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Session */
	public OMap toMap() {
		return super.toMap()
			.a("JsoParserSession", new DefaultFilteringOMap()
			);
	}
}
