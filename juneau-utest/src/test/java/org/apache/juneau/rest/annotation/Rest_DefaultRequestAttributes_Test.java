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
package org.apache.juneau.rest.annotation;

import static java.util.Collections.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.http.annotation.Content;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.RestResponse;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.httppart.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.testutils.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Rest_DefaultRequestAttributes_Test {

	//------------------------------------------------------------------------------------------------------------------
	// @RestPreCall
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		parsers=A1.class,
		defaultRequestAttributes={
			"p1:sp1", // Unchanged servlet-level property.
			"p2:sp2", // Servlet-level property overridden by onPreCall.
			"p3:sp3", // Servlet-level property overridded by method.
			"p4:sp4"  // Servlet-level property overridden by method then onPreCall.
		}
	)
	public static class A {

		@RestPreCall
		public void onPreCall(RestRequest req) {
			RequestAttributes attrs = req.getAttributes();
			attrs.set("p2", "xp2");
			attrs.set("p4", "xp4");
			attrs.set("p5", "xp5"); // New property
			String overrideContentType = req.getHeaderParam("Override-Content-Type").orElse(null);
			if (overrideContentType != null)
				req.getHeaders().set("Content-Type", overrideContentType);
		}

		@RestPut(
			defaultRequestAttributes={
				"p3:mp3",
				"p4:mp4"
			}
		)
		public String a(@Content String in) {
			return in;
		}

		@RestPut
		public String b(RestRequest req, RequestAttributes attrs) throws Exception {
			attrs.set("p3", "pp3");
			attrs.set("p4", "pp4");
			return req.getContent().as(String.class);
		}
	}

	public static class A1 extends MockReaderParser {
		public A1(MockReaderParser.Builder b) {
			super(b.consumes("text/a1,text/a2,text/a3").function((session,in,type)->in(session)));
		}

		private static Object in(ReaderParserSession session) {
			JsonMap sp = session.getSessionProperties();
			return "p1="+sp.get("p1",null)+",p2="+sp.get("p2",null)+",p3="+sp.get("p3",null)+",p4="+sp.get("p4",null)+",p5="+sp.get("p5",null);

		}
	}

	@Test
	public void a01_preCall() throws Exception {
		RestClient a = MockRestClient.build(A.class);
		a.put("/a", null).contentType("text/a1").run().assertContent("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5");
		a.put("/a", null).contentType("text/a1").header("Override-Content-Type", "text/a2").run().assertContent("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5");
		a.put("/b", null).contentType("text/a1").run().assertContent("p1=sp1,p2=xp2,p3=pp3,p4=pp4,p5=xp5");
		a.put("/b", null).contentType("text/a1").header("Override-Content-Type", "text/a2").run().assertContent("p1=sp1,p2=xp2,p3=pp3,p4=pp4,p5=xp5");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @RestPostCall
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		serializers=B1.class,
		defaultRequestAttributes={
			"p1:sp1", // Unchanged servlet-level property.
			"p2:sp2", // Servlet-level property overridden by onPostCall.
			"p3:sp3", // Servlet-level property overridded by method.
			"p4:sp4"  // Servlet-level property overridden by method then onPostCall.
		}
	)
	public static class B {

		@RestPostCall
		public void onPostCall(RestRequest req, RestResponse res) {
			RequestAttributes attrs = req.getAttributes();
			attrs.set("p2", "xp2");
			attrs.set("p4", "xp4");
			attrs.set("p5", "xp5"); // New property
			String overrideAccept = req.getHeaderParam("Override-Accept").orElse(null);
			if (overrideAccept != null)
				req.getHeaders().set("Accept", overrideAccept);
			String overrideContentType = req.getHeaderParam("Override-Content-Type").orElse(null);
			if (overrideContentType != null)
				attrs.set("Override-Content-Type", overrideContentType);
		}

		@RestPut(
			defaultRequestAttributes={
				"p3:mp3",
				"p4:mp4"
			},
			defaultRequestHeaders="Accept: text/s2"
		)
		public String a() {
			return null;
		}

		@RestPut
		public String b(RestRequest req, RequestAttributes attrs) throws Exception {
			attrs.set("p3", "pp3");
			attrs.set("p4", "pp4");
			String accept = req.getHeaderParam("Accept").orElse(null);
			if (accept == null || accept.isEmpty())
				req.getHeaders().set("Accept", "text/s2");
			return null;
		}
	}

	public static class B1 extends MockWriterSerializer {
		public B1(MockWriterSerializer.Builder b) {
			super(b.produces("test/s1").accept("text/s1,text/s2,text/s3").function((s,o) -> out(s)).headers(s->headers(s)));
		}
		public static String out(SerializerSession s) {
			JsonMap sp = s.getSessionProperties();
			return "p1="+sp.get("p1",null)+",p2="+sp.get("p2",null)+",p3="+sp.get("p3",null)+",p4="+sp.get("p4",null)+",p5="+sp.get("p5",null);
		}
		public static Map<String,String> headers(SerializerSession s) {
			JsonMap sp = s.getSessionProperties();
			if (sp.containsKey("Override-Content-Type"))
				return map("Content-Type",sp.getString("Override-Content-Type",null));
			return emptyMap();
		}
	}

	@Test
	public void b01_postCall() throws Exception {
		RestClient b = MockRestClient.build(B.class);
		b.put("/a", null).accept("text/s1").run().assertContent("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5");
		b.put("/a", null).accept("text/s1").header("Override-Accept", "text/s2").run().assertContent("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5");
		b.put("/a", null).accept("text/s1").header("Override-Content-Type", "text/s3").run().assertContent("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5");
		b.put("/a", null).run().assertContent("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5");
		b.put("/a", null).header("Override-Accept", "text/s3").run().assertContent("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5");
		b.put("/a", null).header("Override-Content-Type", "text/s3").run().assertContent("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5");
		b.put("/b", null).accept("text/s1").run().assertContent("p1=sp1,p2=xp2,p3=pp3,p4=xp4,p5=xp5");
		b.put("/b", null).accept("text/s1").header("Override-Accept", "text/s2").run().assertContent("p1=sp1,p2=xp2,p3=pp3,p4=xp4,p5=xp5");
		b.put("/b", null).accept("text/s1").header("Override-Content-Type", "text/s3").run().assertContent("p1=sp1,p2=xp2,p3=pp3,p4=xp4,p5=xp5");
		b.put("/b", null).run().assertContent("p1=sp1,p2=xp2,p3=pp3,p4=xp4,p5=xp5");
		b.put("/b", null).header("Override-Accept", "text/s3").run().assertContent("p1=sp1,p2=xp2,p3=pp3,p4=xp4,p5=xp5");
		b.put("/b", null).header("Override-Content-Type", "text/s3").run().assertContent("p1=sp1,p2=xp2,p3=pp3,p4=xp4,p5=xp5");
	}
}
