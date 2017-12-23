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
package org.apache.juneau.rest.test;

import static org.apache.juneau.internal.IOUtils.*;
import static org.apache.juneau.rest.annotation.HookEvent.*;
import static org.apache.juneau.http.HttpMethodName.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.serializer.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testGroups"
)
public class GroupsResource extends RestServlet {
	private static final long serialVersionUID = 1L;

	@RestHook(INIT)
	public void init(RestConfig config) throws Exception {
		config.addSerializers(SSerializer.class).addParsers(PParser.class);
	}

	public static class SSerializer extends WriterSerializer {

		public SSerializer(PropertyStore2 ps) {
			super(ps, "text/s1", "text/s1", "text/s2");
		}

		@Override /* Serializer */
		public WriterSerializerSession createSession(SerializerSessionArgs args) {
			return new WriterSerializerSession(args) {

				@Override /* SerializerSession */
				protected void doSerialize(SerializerPipe out, Object o) throws Exception {
					out.getWriter().write("text/s," + o);
				}
			};
		}
	}

	public static class PParser extends ReaderParser {

		public PParser(PropertyStore2 ps) {
			super(ps, "text/p1", "text/p2");
		}

		@Override /* Parser */
		public ReaderParserSession createSession(ParserSessionArgs args) {
			return new ReaderParserSession(args) {

				@Override /* ParserSession */
				@SuppressWarnings("unchecked")
				protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws Exception {
					return (T)read(pipe.getReader());
				}
			};
		}
	}

	//====================================================================================================
	// Serializer defined on class.
	//====================================================================================================
	@RestMethod(name=GET, path="/testSerializerDefinedOnClass")
	public String testSerializerDefinedOnClass_get() {
		return GET;
	}

	@RestMethod(name=PUT, path="/testSerializerDefinedOnClass")
	public String testSerializerDefinedOnClass_put(@Body String in) {
		return in;
	}
}
