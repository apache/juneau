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
package org.apache.juneau.server.test;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.server.*;
import org.apache.juneau.server.annotation.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testCharsetEncodings",
	defaultRequestHeaders={"Accept: text/s", "Content-Type: text/p"},
	parsers={CharsetEncodingsResource.CtParser.class}, serializers={CharsetEncodingsResource.ASerializer.class}
)
public class CharsetEncodingsResource extends RestServlet {
	private static final long serialVersionUID = 1L;

	@Consumes("text/p")
	public static class CtParser extends ReaderParser {
		@SuppressWarnings("unchecked")
		@Override /* Parser */
		protected <T> T doParse(ParserSession session, ClassMeta<T> type) throws Exception {
			return (T)IOUtils.read(session.getReader());
		}
	}

	@Produces("text/s")
	public static class ASerializer extends WriterSerializer {
		@Override /* Serializer */
		protected void doSerialize(SerializerSession session, Object o) throws Exception {
			session.getWriter().write(o.toString());
		}
	}

	@RestMethod(name="PUT", path="/")
	public String test1(RestRequest req, @Content String in) {
		return req.getCharacterEncoding() + "/" + in + "/" + req.getCharacterEncoding();
	}
}
