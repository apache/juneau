/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.annotation;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.utest.utils.*;
import org.junit.jupiter.api.*;

class Restx_Parsers_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Setup
	//------------------------------------------------------------------------------------------------------------------

	public static class PA extends FakeReaderParser {
		public PA(FakeReaderParser.Builder b) {
			super(b.consumes("text/a").function((session,in,type)->"text/a - " + in));
		}
	}

	public static class PB extends FakeReaderParser {
		public PB(FakeReaderParser.Builder b) {
			super(b.consumes("text/b").function((session,in,type)->"text/b - " + in));
		}
	}

	public static class PC extends FakeReaderParser {
		public PC(FakeReaderParser.Builder b) {
			super(b.consumes("text/c").function((session,in,type)->"text/c - " + in));
		}
	}

	public static class PD extends FakeReaderParser {
		public PD(FakeReaderParser.Builder b) {
			super(b.consumes("text/d").function((session,in,type)->"text/d - " + in));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	@Rest(parsers=PA.class)
	public static class A {
		@RestPut
		public String a(@Content String in) {
			return in;
		}
		@RestPut(parsers=PB.class)
		public String b(@Content String in) {
			return in;
		}
		@RestPut(parsers={Inherit.class, PB.class,PC.class})
		public String c(@Content String in) {
			return in;
		}
		@RestPut(parsers={Inherit.class, PD.class})
		public String d(@Content String in) {
			return in;
		}
		@RestPut
		public String e(@Content String in) {
			return in;
		}
	}

	@Test void a01_basic() throws Exception {
		var a = MockRestClient.buildLax(A.class);

		a.put("/a", "test1")
			.contentType("text/a")
			.run()
			.assertContent("text/a - test1");
		a.put("/a?noTrace=true", "test1")
			.contentType("text/b")
			.run()
			.assertStatus(415)
			.assertContent().isContains(
				"Unsupported media-type in request header 'Content-Type': 'text/b'",
				"Supported media-types: ['text/a"
			);

		a.put("/b", "test2")
			.contentType("text/b")
			.run()
			.assertContent("text/b - test2");
		a.put("/b?noTrace=true", "test2")
			.contentType("text/a")
			.run()
			.assertStatus(415)
			.assertContent().isContains(
				"Unsupported media-type in request header 'Content-Type': 'text/a'",
				"Supported media-types: ['text/b']"
			);

		a.put("/c", "test3")
			.contentType("text/a")
			.run()
			.assertContent("text/a - test3");
		a.put("/c", "test3")
			.contentType("text/b")
			.run()
			.assertContent("text/b - test3");

		a.put("/d", "test4")
			.contentType("text/a")
			.run()
			.assertContent("text/a - test4");
		a.put("/d", "test4")
			.contentType("text/d")
			.run()
			.assertContent("text/d - test4");

		a.put("/e?noTrace=true", "test1")
			.contentType("text/bad")
			.run()
			.assertStatus(415)
			.assertContent().isContains(
				"Unsupported media-type in request header 'Content-Type': 'text/bad'",
				"Supported media-types: ['text/a"
			);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test parser inheritance.
	//------------------------------------------------------------------------------------------------------------------

	public static class P1 extends FakeReaderParser{ public P1(FakeReaderParser.Builder b) {super(b.consumes("text/p1"));} }
	public static class P2 extends FakeReaderParser{ public P2(FakeReaderParser.Builder b) {super(b.consumes("text/p2"));} }
	public static class P3 extends FakeReaderParser{ public P3(FakeReaderParser.Builder b) {super(b.consumes("text/p3"));} }
	public static class P4 extends FakeReaderParser{ public P4(FakeReaderParser.Builder b) {super(b.consumes("text/p4"));} }
	public static class P5 extends FakeReaderParser{ public P5(FakeReaderParser.Builder b) {super(b.consumes("text/p5"));} }

	@Rest(parsers={P1.class,P2.class})
	public static class B {}

	@Rest(parsers={P3.class,P4.class})
	public static class B1 extends B {}

	@Rest
	public static class B2 extends B1 {
		@RestOp
		public JsonList a(RestRequest req) {
			// Should show ['text/p3','text/p4','text/p1','text/p2']
			return JsonList.of(req.getOpContext().getSupportedContentTypes());
		}
		@RestOp(parsers=P5.class)
		public JsonList b(RestRequest req) {
			// Should show ['text/p5']
			return JsonList.of(req.getOpContext().getSupportedContentTypes());
		}
		@RestOp(parsers={P5.class,Inherit.class})
		public JsonList c(RestRequest req) {
			// Should show ['text/p5','text/p3','text/p4','text/p1','text/p2']
			return JsonList.of(req.getOpContext().getSupportedContentTypes());
		}
	}

	@Test void b01_inheritence() throws Exception {
		var b = MockRestClient.build(B2.class);
		b.get("/a").run().assertContent("['text/p3','text/p4','text/p1','text/p2']");
		b.get("/b").run().assertContent("['text/p5']");
		b.get("/c").run().assertContent("['text/p5','text/p3','text/p4','text/p1','text/p2']");
	}
}