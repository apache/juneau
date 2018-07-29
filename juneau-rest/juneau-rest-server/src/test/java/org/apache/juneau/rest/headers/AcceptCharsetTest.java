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
package org.apache.juneau.rest.headers;

import static org.apache.juneau.http.HttpMethodName.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.serializer.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Validates the handling of the Accept-Charset header.
 */
@SuppressWarnings({"javadoc"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AcceptCharsetTest {

	//=================================================================================================================
	// Test that Q-values are being resolved correctly.
	//=================================================================================================================

	@RestResource(defaultCharset="utf-8",serializers=PlainTextSerializer.class)
	public static class A {
		@RestMethod
		public String qValues() {
			return "foo";
		}
	}
	static MockRest a = MockRest.create(A.class);

	@Test
	public void a01_qValues() throws Exception {
		a.get("/qValues").accept("text/plain").acceptCharset("utf-8").execute().assertCharset("utf-8");
		a.get("/qValues").accept("text/plain").acceptCharset("iso-8859-1").execute().assertCharset("iso-8859-1");
		a.get("/qValues").accept("text/plain").acceptCharset("bad,utf-8").execute().assertCharset("utf-8");
		a.get("/qValues").accept("text/plain").acceptCharset("utf-8,bad").execute().assertCharset("utf-8");
		a.get("/qValues").accept("text/plain").acceptCharset("bad;q=0.9,utf-8;q=0.1").execute().assertCharset("utf-8");
		a.get("/qValues").accept("text/plain").acceptCharset("bad;q=0.1,utf-8;q=0.9").execute().assertCharset("utf-8");
		a.get("/qValues").accept("text/plain").acceptCharset("utf-8;q=0.9,iso-8859-1;q=0.1").execute().assertCharset("utf-8");
		a.get("/qValues").accept("text/plain").acceptCharset("utf-8;q=0.1,iso-8859-1;q=0.9").execute().assertCharset("iso-8859-1");
		a.get("/qValues").accept("text/plain").acceptCharset("*").execute().assertCharset("utf-8");
		a.get("/qValues").accept("text/plain").acceptCharset("bad,iso-8859-1;q=0.5,*;q=0.1").execute().assertCharset("iso-8859-1");
		a.get("/qValues").accept("text/plain").acceptCharset("bad,iso-8859-1;q=0.1,*;q=0.5").execute().assertCharset("utf-8");
	}

	//=================================================================================================================
	// Validate various Accept-Charset variations.
	//=================================================================================================================

	@RestResource(defaultCharset="utf-8")
	public static class B {

		@RestMethod(name=PUT, parsers=TestParser.class, serializers=TestSerializer.class)
		public String charsetOnResponse(@Body String in) {
			return in;
		}

		public static class TestParser extends InputStreamParser {
			public TestParser(PropertyStore ps) {
				super(ps, "text/plain");
			}
			@Override /* Parser */
			public InputStreamParserSession createSession(ParserSessionArgs args) {
				return new InputStreamParserSession(args) {
					@Override /* ParserSession */
					@SuppressWarnings("unchecked")
					protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws Exception {
						return (T)getProperty("characterEncoding", String.class);
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
					protected void doSerialize(SerializerPipe out, Object o) throws Exception {
						try (Writer w = new OutputStreamWriter(out.getOutputStream())) {
							w.append(o.toString()).append('/').append(getProperty("characterEncoding", String.class));
						}
					}
				};
			}
		}
	}
	static MockRest b = MockRest.create(B.class);

	@Test
	public void b01_testCharsetOnResponse() throws Exception {
		b.put("/charsetOnResponse", null).plainText().execute().assertBody("utf-8/utf-8");
		b.put("/charsetOnResponse", null).plainText().acceptCharset("Shift_JIS").execute().assertBody("utf-8/Shift_JIS");
		b.put("/charsetOnResponse?noTrace=true", null).plainText().acceptCharset("BAD").execute().assertStatus(406).assertBodyContains("No supported charsets in header 'Accept-Charset': 'BAD'");
		b.put("/charsetOnResponse", null).plainText().acceptCharset("UTF-8").execute().assertBody("utf-8/UTF-8");
		b.put("/charsetOnResponse", null).plainText().acceptCharset("bad,iso-8859-1").execute().assertBody("utf-8/iso-8859-1");
		b.put("/charsetOnResponse", null).plainText().acceptCharset("bad;q=0.9,iso-8859-1;q=0.1").execute().assertBody("utf-8/iso-8859-1");
		b.put("/charsetOnResponse", null).plainText().acceptCharset("bad;q=0.1,iso-8859-1;q=0.9").execute().assertBody("utf-8/iso-8859-1");
	}
}
