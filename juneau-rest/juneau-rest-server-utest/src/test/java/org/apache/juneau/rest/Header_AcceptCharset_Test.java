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
package org.apache.juneau.rest;

import static org.apache.juneau.http.HttpMethod.*;
import static org.junit.runners.MethodSorters.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.parser.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Header_AcceptCharset_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Test that Q-values are being resolved correctly.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(defaultCharset="utf-8",serializers=PlainTextSerializer.class)
	public static class A {
		@RestMethod
		public String a() {
			return "foo";
		}
	}

	@Test
	public void a01_qValues() throws Exception {
		RestClient a = MockRestClient.build(A.class);
		a.get("/a").accept("text/plain").acceptCharset("utf-8").run().assertCharset().is("utf-8");
		a.get("/a").accept("text/plain").acceptCharset("iso-8859-1").run().assertCharset().is("iso-8859-1");
		a.get("/a").accept("text/plain").acceptCharset("bad,utf-8").run().assertCharset().is("utf-8");
		a.get("/a").accept("text/plain").acceptCharset("utf-8,bad").run().assertCharset().is("utf-8");
		a.get("/a").accept("text/plain").acceptCharset("bad;q=0.9,utf-8;q=0.1").run().assertCharset().is("utf-8");
		a.get("/a").accept("text/plain").acceptCharset("bad;q=0.1,utf-8;q=0.9").run().assertCharset().is("utf-8");
		a.get("/a").accept("text/plain").acceptCharset("utf-8;q=0.9,iso-8859-1;q=0.1").run().assertCharset().is("utf-8");
		a.get("/a").accept("text/plain").acceptCharset("utf-8;q=0.1,iso-8859-1;q=0.9").run().assertCharset().is("iso-8859-1");
		a.get("/a").accept("text/plain").acceptCharset("*").run().assertCharset().is("utf-8");
		a.get("/a").accept("text/plain").acceptCharset("bad,iso-8859-1;q=0.5,*;q=0.1").run().assertCharset().is("iso-8859-1");
		a.get("/a").accept("text/plain").acceptCharset("bad,iso-8859-1;q=0.1,*;q=0.5").run().assertCharset().is("utf-8");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Validate various Accept-Charset variations.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(defaultCharset="utf-8")
	public static class B {

		@RestMethod(name=PUT, parsers=TestParser.class, serializers=TestSerializer.class)
		public String a(@Body String in) {
			return in;
		}

		public static class TestParser extends InputStreamParser {
			public TestParser(PropertyStore ps) {
				super(ps, "text/plain");
			}
			@Override /* Parser */
			public InputStreamParserSession createSession(final ParserSessionArgs args) {
				return new InputStreamParserSession(args) {
					@Override /* ParserSession */
					@SuppressWarnings("unchecked")
					protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {
						return (T)args.getProperty(ReaderParser.RPARSER_streamCharset).toString();
					}
				};
			}
		}

		public static class TestSerializer extends OutputStreamSerializer {
			public TestSerializer(PropertyStore ps) {
				super(ps, "text/plain", null);
			}
			@Override /* Serializer */
			public OutputStreamSerializerSession createSession(SerializerSessionArgs args) {
				return new OutputStreamSerializerSession(args) {
					@Override /* SerializerSession */
					protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
						try (Writer w = new OutputStreamWriter(out.getOutputStream())) {
							Object sc = args.getProperty(WriterSerializer.WSERIALIZER_streamCharset);
							w.append(o.toString()).append('/').append(sc == null ? null : sc.toString());
						}
					}
				};
			}
		}
	}

	@Test
	public void b01_charsetOnResponse() throws Exception {
		RestClient b = MockRestClient.buildLax(B.class);
		b.put("/a", null).plainText().run().assertBody().is("UTF-8/UTF-8");
		b.put("/a", null).plainText().acceptCharset("Shift_JIS").run().assertBody().is("UTF-8/Shift_JIS");
		b.put("/a?noTrace=true", null).plainText().acceptCharset("BAD").run().assertCode().is(406).assertBody().contains("No supported charsets in header 'Accept-Charset': 'BAD'");
		b.put("/a", null).plainText().acceptCharset("UTF-8").run().assertBody().is("UTF-8/UTF-8");
		b.put("/a", null).plainText().acceptCharset("bad,iso-8859-1").run().assertBody().is("UTF-8/ISO-8859-1");
		b.put("/a", null).plainText().acceptCharset("bad;q=0.9,iso-8859-1;q=0.1").run().assertBody().is("UTF-8/ISO-8859-1");
		b.put("/a", null).plainText().acceptCharset("bad;q=0.1,iso-8859-1;q=0.9").run().assertBody().is("UTF-8/ISO-8859-1");
	}
}
