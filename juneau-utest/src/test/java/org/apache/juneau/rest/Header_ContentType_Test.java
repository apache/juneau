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

import java.io.IOException;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.http.header.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
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
		public String a(@Body String in) {
			return in;
		}
	}

	@Test
	public void a01_defaultHeadersOnServletAnnotation() throws Exception {
		RestClient a = MockRestClient.buildLax(A.class);
		a.put("/a", null, ContentType.of(""))
			.run()
			.assertBody().is("p2");
		a.put("/a", null, ContentType.of("text/p1"))
			.run()
			.assertBody().is("p1");
		a.put("/a", null, ContentType.of("text/p2"))
			.run()
			.assertBody().is("p2");
		a.put("/a?noTrace=true", null, ContentType.of("text/p3"))
			.run()
			.assertCode().is(415)
			.assertBody().contains("Unsupported media-type in request header 'Content-Type': 'text/p3'");
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
		public String a(@Body String in) {
			return in;
		}
	}

	@Test
	public void b01_restMethodWithParsersSerializers() throws Exception {
		RestClient b = MockRestClient.buildLax(B.class);
		b.put("/a", null).contentType("text/p3").run().assertBody().is("p3");
		b.put("/a?noTrace=true", null, ContentType.of(""))
			.run()
			.assertCode().is(415)
			.assertBody().contains(
				"Unsupported media-type in request header 'Content-Type': 'text/p2'",
				"Supported media-types: ['text/p3']"
			);
		b.put("/a?noTrace=true", null, ContentType.of("text/p1"))
			.run()
			.assertCode().is(415)
			.assertBody().contains(
				"Unsupported media-type in request header 'Content-Type': 'text/p1'",
				"Supported media-types: ['text/p3']"
			);
		b.put("/a?noTrace=true", null, ContentType.of("text/p2"))
			.run()
			.assertCode().is(415)
			.assertBody().contains(
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
		public String a(@Body String in) {
			return in;
		}
	}

	@Test
	public void c01_restMethodAddParsersSerializersInherit() throws Exception {
		RestClient c = MockRestClient.buildLax(C.class);
		c.put("/a", null, ContentType.of("")).run().assertBody().is("p2");
		c.put("/a", null, ContentType.of("text/p1")).run().assertBody().is("p1");
		c.put("/a", null, ContentType.of("text/p2")).run().assertBody().is("p2");
		c.put("/a", null, ContentType.of("text/p3")).run().assertBody().is("p3");
		c.put("/a?noTrace=true", null).contentType("text/p4")
			.run()
			.assertCode().is(415)
			.assertBody().contains(
				"Unsupported media-type in request header 'Content-Type': 'text/p4'",
				"Supported media-types: ['text/p3','text/p1','text/p2']"
			);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test that default Content-Type headers on method annotation are picked up
	// when @RestOp.parsers/serializers annotations are used.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		defaultRequestHeaders={" Content-Type : text/p2 "},
		parsers={P1.class,P2.class}
	)
	public static class D {
		@RestPut(defaultRequestHeaders={"Content-Type: text/p3"}, parsers=P3.class)
		public String a(@Body String in) {
			return in;
		}
	}

	@Test
	public void d01_restMethodParserSerializerAnnotations() throws Exception {
		RestClient d = MockRestClient.buildLax(D.class);
		d.put("/a", null, ContentType.of(""))
			.run()
			.assertBody().is("p3");
		d.put("/a", null, ContentType.of("text/p3"))
			.run()
			.assertBody().is("p3");
		d.put("/a?noTrace=true", null, ContentType.of("text/p1"))
			.run()
			.assertCode().is(415)
			.assertBody().contains(
				"Unsupported media-type in request header 'Content-Type': 'text/p1'",
				"Supported media-types: ['text/p3']"
			);
		d.put("/a?noTrace=true", null, ContentType.of("text/p2"))
			.run()
			.assertCode().is(415)
			.assertBody().contains(
				"Unsupported media-type in request header 'Content-Type': 'text/p2'",
				"Supported media-types: ['text/p3']"
			);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test that default Content-Type headers on method annotation are picked up
	// 	when @RestOp.addParsers/addSerializers annotations are used.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		defaultRequestHeaders={" Content-Type : text/p2 "},
		parsers={P1.class,P2.class}
	)
	public static class E {
		@RestPut(defaultRequestHeaders={"Content-Type: text/p3"}, parsers={Inherit.class,P3.class})
		public String a(@Body String in) {
			return in;
		}
	}

	@Test
	public void e01_restMethodAddParsersSerializersAnnotations() throws Exception {
		RestClient e = MockRestClient.build(E.class);
		e.put("/a", null, ContentType.of(""))
			.run()
			.assertBody().is("p3");
		e.put("/a", null, ContentType.of("text/p1"))
			.run()
			.assertBody().is("p1");
		e.put("/a", null, ContentType.of("text/p2"))
			.run()
			.assertBody().is("p2");
		e.put("/a", null, ContentType.of("text/p3"))
			.run()
			.assertBody().is("p3");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------------------------------------------------------

	public static class DummyParser extends ReaderParser {
		String name;
		DummyParser(ContextProperties cp, String name, String...consumes) {
			super(cp, consumes);
			this.name = name;
		}
		@Override /* Parser */
		public ReaderParserSession createSession(ParserSessionArgs args) {
			return new ReaderParserSession(args) {
				@Override /* ParserSession */
				@SuppressWarnings("unchecked")
				protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {
					return (T)name;
				}
			};
		}
	}

	public static class P1 extends DummyParser { public P1(ContextProperties cp) {super(cp, "p1", "text/p1");}}
	public static class P2 extends DummyParser { public P2(ContextProperties cp) {super(cp, "p2", "text/p2");}}
	public static class P3 extends DummyParser { public P3(ContextProperties cp) {super(cp, "p3", "text/p3");}}
}
