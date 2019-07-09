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

import java.io.IOException;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;
import org.junit.runners.*;


/**
 * Tests behavior related to the Content-Type header
 */
@SuppressWarnings({})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ContentTypeTest {

	//=================================================================================================================
	// Setup classes
	//=================================================================================================================

	public static class DummyParser extends ReaderParser {
		String name;
		DummyParser(PropertyStore ps, String name, String...consumes) {
			super(ps, consumes);
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

	public static class P1 extends DummyParser { public P1(PropertyStore ps) {super(ps, "p1", "text/p1");}}
	public static class P2 extends DummyParser { public P2(PropertyStore ps) {super(ps, "p2", "text/p2");}}
	public static class P3 extends DummyParser { public P3(PropertyStore ps) {super(ps, "p3", "text/p3");}}

	//=================================================================================================================
	// Test that default Content-Type headers on servlet annotation are picked up.
	//=================================================================================================================

	@RestResource(
		defaultRequestHeaders={" Content-Type : text/p2 "},
		parsers={P1.class,P2.class}
	)
	public static class A {
		@RestMethod(name=PUT)
		public String a01(@Body String in) {
			return in;
		}
	}
	private static MockRest a = MockRest.build(A.class, null);

	@Test
	public void a01_defaultHeadersOnServletAnnotation_valid() throws Exception {
		a.put("/a01", null).execute().assertBody("p2");
		a.put("/a01", null).contentType("text/p1").execute().assertBody("p1");
		a.put("/a01", null).contentType("text/p2").execute().assertBody("p2");
	}

	@Test
	public void a02_defaultHeadersOnServletAnnotation_invalid() throws Exception {
		a.put("/a01?noTrace=true", null).contentType("text/p3").execute().assertStatus(415).assertBodyContains("Unsupported media-type in request header 'Content-Type': 'text/p3'");
	}

	//=================================================================================================================
	// Test that default Content-Type headers on servlet annotation are picked up
	// when @RestMethod.parsers/serializers annotations are used.
	//=================================================================================================================

	@RestResource(
		defaultRequestHeaders={" Content-Type : text/p2 "},
		parsers={P1.class,P2.class}
	)
	public static class B {
		@RestMethod(name=PUT, parsers=P3.class)
		public String b(@Body String in) {
			return in;
		}
	}
	private static MockRest b = MockRest.build(B.class, null);

	@Test
	public void b01_restMethodWithParsersSerializers_valid() throws Exception {
		b.put("/b", null).contentType("text/p3").execute().assertBody("p3");
	}

	@Test
	public void b02_restMethodWithParsersSerializers_invalid() throws Exception {
		b.put("/b?noTrace=true", null).execute()
			.assertStatus(415)
			.assertBodyContains(
				"Unsupported media-type in request header 'Content-Type': 'text/p2'",
				"Supported media-types: ['text/p3']"
			);
		b.put("/b?noTrace=true", null).contentType("text/p1").execute()
			.assertStatus(415)
			.assertBodyContains(
				"Unsupported media-type in request header 'Content-Type': 'text/p1'",
				"Supported media-types: ['text/p3']"
			);
		b.put("/b?noTrace=true", null).contentType("text/p2").execute()
			.assertStatus(415)
			.assertBodyContains(
				"Unsupported media-type in request header 'Content-Type': 'text/p2'",
				"Supported media-types: ['text/p3']"
			);
	}

	//=================================================================================================================
	// Test that default Content-Type headers on servlet annotation are picked up
	// when @RestMethod.addParsers/addSerializers annotations are used.
	//=================================================================================================================

	@RestResource(
		defaultRequestHeaders={" Content-Type : text/p2 "},
		parsers={P1.class,P2.class}
	)
	public static class C {
		@RestMethod(name=PUT, parsers={P3.class,Inherit.class})
		public String c(@Body String in) {
			return in;
		}
	}
	private static MockRest c = MockRest.build(C.class, null);

	@Test
	public void c01_restMethodAddParsersSerializersInherit() throws Exception {
		c.put("/c", null).execute().assertBody("p2");
		c.put("/c", null).contentType("text/p1").execute().assertBody("p1");
		c.put("/c", null).contentType("text/p2").execute().assertBody("p2");
		c.put("/c", null).contentType("text/p3").execute().assertBody("p3");
	}

	@Test
	public void c02_restMethodAddParsersSerializersInherit_invalid() throws Exception {
		c.put("/c?noTrace=true", null).contentType("text/p4").execute()
			.assertStatus(415)
			.assertBodyContains(
				"Unsupported media-type in request header 'Content-Type': 'text/p4'",
				"Supported media-types: ['text/p3','text/p1','text/p2']"
			);
	}

	//=================================================================================================================
	// Test that default Content-Type headers on method annotation are picked up
	// when @RestMethod.parsers/serializers annotations are used.
	//=================================================================================================================

	@RestResource(
		defaultRequestHeaders={" Content-Type : text/p2 "},
		parsers={P1.class,P2.class}
	)
	public static class E {
		@RestMethod(name=PUT, defaultRequestHeaders={"Content-Type: text/p3"}, parsers=P3.class)
		public String e(@Body String in) {
			return in;
		}
	}
	private static MockRest e = MockRest.build(E.class, null);

	@Test
	public void e01_restMethodParserSerializerAnnotations_valid() throws Exception {
		e.put("/e", null).execute().assertBody("p3");
		e.put("/e", null).contentType("text/p3").execute().assertBody("p3");
	}
	@Test
	public void e02_restMethodParserSerializerAnnotations_invalid() throws Exception {
		e.put("/e?noTrace=true", null).contentType("text/p1").execute()
			.assertStatus(415)
			.assertBodyContains(
				"Unsupported media-type in request header 'Content-Type': 'text/p1'",
				"Supported media-types: ['text/p3']"
			);
		e.put("/e?noTrace=true", null).contentType("text/p2").execute()
			.assertStatus(415)
			.assertBodyContains(
				"Unsupported media-type in request header 'Content-Type': 'text/p2'",
				"Supported media-types: ['text/p3']"
			);
	}

	//=================================================================================================================
	// Test that default Content-Type headers on method annotation are picked up
	// 	when @RestMethod.addParsers/addSerializers annotations are used.
	//=================================================================================================================

	@RestResource(
		defaultRequestHeaders={" Content-Type : text/p2 "},
		parsers={P1.class,P2.class}
	)
	public static class F {
		@RestMethod(name=PUT, defaultRequestHeaders={"Content-Type: text/p3"}, parsers={Inherit.class,P3.class})
		public String f(@Body String in) {
			return in;
		}
	}

	private static MockRest f = MockRest.build(F.class, null);

	@Test
	public void f01_restMethodAddParsersSerializersAnnotations_valid() throws Exception {
		f.put("/f", null).execute().assertBody("p3");
		f.put("/f", null).contentType("text/p1").execute().assertBody("p1");
		f.put("/f", null).contentType("text/p2").execute().assertBody("p2");
		f.put("/f", null).contentType("text/p3").execute().assertBody("p3");
	}
}
