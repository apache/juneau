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

import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.http.annotation.Content;
import org.apache.juneau.http.header.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.testutils.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Header_ContentType_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Test that default Content-Type headers on servlet annotation are picked up.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		defaultRequestHeaders={" Content-Type : text/p2 "},
		parsers={P1.class,P2.class}
	)
	public static class A {
		@RestPut
		public String a(@Content String in) {
			return in;
		}
	}

	@Test
	public void a01_defaultHeadersOnServletAnnotation() throws Exception {
		RestClient a = MockRestClient.buildLax(A.class);
		a.put("/a", null, ContentType.of(""))
			.run()
			.assertContent("p2");
		a.put("/a", null, ContentType.of("text/p1"))
			.run()
			.assertContent("p1");
		a.put("/a", null, ContentType.of("text/p2"))
			.run()
			.assertContent("p2");
		a.put("/a?noTrace=true", null, ContentType.of("text/p3"))
			.run()
			.assertStatus(415)
			.assertContent().isContains("Unsupported media-type in request header 'Content-Type': 'text/p3'");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test that default Content-Type headers on servlet annotation are picked up
	// when @RestOp.parsers/serializers annotations are used.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		defaultRequestHeaders={" Content-Type : text/p2 "},
		parsers={P1.class,P2.class}
	)
	public static class B {
		@RestPut(parsers=P3.class)
		public String a(@Content String in) {
			return in;
		}
	}

	@Test
	public void b01_restMethodWithParsersSerializers() throws Exception {
		RestClient b = MockRestClient.buildLax(B.class);
		b.put("/a", null).contentType("text/p3").run().assertContent("p3");
		b.put("/a?noTrace=true", null, ContentType.of(""))
			.run()
			.assertStatus(415)
			.assertContent().isContains(
				"Unsupported media-type in request header 'Content-Type': 'text/p2'",
				"Supported media-types: ['text/p3']"
			);
		b.put("/a?noTrace=true", null, ContentType.of("text/p1"))
			.run()
			.assertStatus(415)
			.assertContent().isContains(
				"Unsupported media-type in request header 'Content-Type': 'text/p1'",
				"Supported media-types: ['text/p3']"
			);
		b.put("/a?noTrace=true", null, ContentType.of("text/p2"))
			.run()
			.assertStatus(415)
			.assertContent().isContains(
				"Unsupported media-type in request header 'Content-Type': 'text/p2'",
				"Supported media-types: ['text/p3']"
			);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test that default Content-Type headers on servlet annotation are picked up
	// when @RestOp.addParsers/addSerializers annotations are used.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		defaultRequestHeaders={" Content-Type : text/p2 "},
		parsers={P1.class,P2.class}
	)
	public static class C {
		@RestPut(parsers={P3.class,Inherit.class})
		public String a(@Content String in) {
			return in;
		}
	}

	@Test
	public void c01_restMethodAddParsersSerializersInherit() throws Exception {
		RestClient c = MockRestClient.buildLax(C.class);
		c.put("/a", null, ContentType.of("")).run().assertContent("p2");
		c.put("/a", null, ContentType.of("text/p1")).run().assertContent("p1");
		c.put("/a", null, ContentType.of("text/p2")).run().assertContent("p2");
		c.put("/a", null, ContentType.of("text/p3")).run().assertContent("p3");
		c.put("/a?noTrace=true", null).contentType("text/p4")
			.run()
			.assertStatus(415)
			.assertContent().isContains(
				"Unsupported media-type in request header 'Content-Type': 'text/p4'",
				"Supported media-types: ['text/p3','text/p1','text/p2']"
			);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test that default Content-Type headers on method annotation are picked up
	// when @RestOp.parsers/serializers annotations are used.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		defaultRequestHeaders={" Content-Type : text/p3 "},
		parsers={P1.class,P2.class}
	)
	public static class D {
		@RestPut(defaultRequestHeaders={"Content-Type: text/p2"}, parsers=P3.class)
		public String a(@Content String in) {
			return in;
		}
	}

	@Test
	public void d01_restMethodParserSerializerAnnotations() throws Exception {
		RestClient d = MockRestClient.buildLax(D.class);
		d.put("/a", null, ContentType.of(""))
			.run()
			.assertContent("p3");
		d.put("/a", null, ContentType.of("text/p3"))
			.run()
			.assertContent("p3");
		d.put("/a?noTrace=true", null, ContentType.of("text/p1"))
			.run()
			.assertStatus(415)
			.assertContent().isContains(
				"Unsupported media-type in request header 'Content-Type': 'text/p1'",
				"Supported media-types: ['text/p3']"
			);
		d.put("/a?noTrace=true", null, ContentType.of("text/p2"))
			.run()
			.assertStatus(415)
			.assertContent().isContains(
				"Unsupported media-type in request header 'Content-Type': 'text/p2'",
				"Supported media-types: ['text/p3']"
			);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test that default Content-Type headers on method annotation are picked up
	// 	when @RestOp.addParsers/addSerializers annotations are used.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		defaultRequestHeaders={" Content-Type : text/p3 "},
		parsers={P1.class,P2.class}
	)
	public static class E {
		@RestPut(defaultRequestHeaders={"Content-Type: text/p2"}, parsers={Inherit.class,P3.class})
		public String a(@Content String in) {
			return in;
		}
	}

	@Test
	public void e01_restMethodAddParsersSerializersAnnotations() throws Exception {
		RestClient e = MockRestClient.build(E.class);
		e.put("/a", null, ContentType.of(""))
			.run()
			.assertContent("p3");
		e.put("/a", null, ContentType.of("text/p1"))
			.run()
			.assertContent("p1");
		e.put("/a", null, ContentType.of("text/p2"))
			.run()
			.assertContent("p2");
		e.put("/a", null, ContentType.of("text/p3"))
			.run()
			.assertContent("p3");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------------------------------------------------------

	public static class P1 extends MockReaderParser { public P1(MockReaderParser.Builder b) {super(b.consumes("text/p1").function((session,in,type)->"p1"));}}
	public static class P2 extends MockReaderParser { public P2(MockReaderParser.Builder b) {super(b.consumes("text/p2").function((session,in,type)->"p2"));}}
	public static class P3 extends MockReaderParser { public P3(MockReaderParser.Builder b) {super(b.consumes("text/p3").function((session,in,type)->"p3"));}}
}