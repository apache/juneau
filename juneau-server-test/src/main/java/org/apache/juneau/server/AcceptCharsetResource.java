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
package org.apache.juneau.server;

import static org.apache.juneau.server.RestServletContext.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.server.annotation.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testAcceptCharset",
	serializers={PlainTextSerializer.class},
	properties={
		// Some versions of Jetty default to ISO8601, so specify UTF-8 for test consistency.
		@Property(name=REST_defaultCharset,value="utf-8")
	}
)
public class AcceptCharsetResource extends RestServlet {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Test that Q-values are being resolved correctly.
	//====================================================================================================
	@RestMethod(name="GET", path="/testQValues")
	public String testQValues() {
		return "foo";
	}

	//====================================================================================================
	// Validate various Accept-Charset variations.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testCharsetOnResponse", parsers=TestParser.class, serializers=TestSerializer.class)
	public String testCharsetOnResponse(@Content String in) {
		return in;
	}

	@Consumes("text/plain")
	public static class TestParser extends InputStreamParser {
		@SuppressWarnings("unchecked")
		@Override /* Parser */
		protected <T> T doParse(ParserSession session, ClassMeta<T> type) throws Exception {
			return (T)session.getProperties().getString("characterEncoding");
		}
	}

	@Produces("text/plain")
	public static class TestSerializer extends OutputStreamSerializer {
		@Override /* Serializer */
		protected void doSerialize(SerializerSession session, Object o) throws Exception {
			Writer w = new OutputStreamWriter(session.getOutputStream());
			w.append(o.toString()).append('/').append(session.getProperties().getString("characterEncoding"));
			w.flush();
			w.close();
		}
	}
}
