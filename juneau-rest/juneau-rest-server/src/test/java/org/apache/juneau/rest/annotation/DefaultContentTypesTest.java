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

import static org.apache.juneau.http.HttpMethodName.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.serializer.*;
import org.junit.*;
import org.junit.runners.*;


/**
 * Tests related to @RestRequest(defaultRequestHeaders).
 */
@SuppressWarnings({"javadoc"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DefaultContentTypesTest {
	
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
				protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws Exception {
					return (T)name;
				}
			};
		}
	}

	public static class DummySerializer extends WriterSerializer {

		String name;

		DummySerializer(PropertyStore ps, String name, String produces) {
			super(ps, produces, null);
			this.name = name;
		}

		@Override /* Serializer */
		public WriterSerializerSession createSession(SerializerSessionArgs args) {
			return new WriterSerializerSession(args) {
				@Override /* SerializerSession */
				protected void doSerialize(SerializerPipe out, Object o) throws Exception {
					out.getWriter().write(name + "/" + o);
				}
			};
		}
	}
	
	public static class P1 extends DummyParser { public P1(PropertyStore ps) {super(ps, "p1", "text/p1");}}
	public static class P2 extends DummyParser { public P2(PropertyStore ps) {super(ps, "p2", "text/p2");}}
	public static class P3 extends DummyParser { public P3(PropertyStore ps) {super(ps, "p3", "text/p3");}}
	public static class S1 extends DummySerializer { public S1(PropertyStore ps) {super(ps, "s1", "text/s1");}}
	public static class S2 extends DummySerializer { public S2(PropertyStore ps) {super(ps, "s2", "text/s2");}}
	public static class S3 extends DummySerializer { public S3(PropertyStore ps) {super(ps, "s3", "text/s3");}}
	

	//====================================================================================================
	// Test that default Accept and Content-Type headers on servlet annotation are picked up.
	//====================================================================================================

	@RestResource(
		defaultRequestHeaders={" Accept : text/s2 "," Content-Type : text/p2 "},
		parsers={P1.class,P2.class}, serializers={S1.class,S2.class}
	)
	public static class A {
		@RestMethod(name=PUT)
		public String a01(@Body String in) {
			return in;
		}
	}
	
	private static MockRest a = MockRest.create(A.class);
	
	@Test
	public void a01_defaultHeadersOnServletAnnotation_valid() throws Exception {
		a.request("PUT", "/").execute().assertBody("s2/p2");
		a.request("PUT", "/").accept("text/s1").execute().assertBody("s1/p2");
		a.request("PUT", "/").contentType("text/p1").execute().assertBody("s2/p1");
		a.request("PUT", "/").accept("text/s1").contentType("text/p1").execute().assertBody("s1/p1");
		a.request("PUT", "/").accept("text/s2").execute().assertBody("s2/p2");
		a.request("PUT", "/").contentType("text/p2").execute().assertBody("s2/p2");
		a.request("PUT", "/").accept("text/s2").contentType("text/p2").execute().assertBody("s2/p2");
	}

	@Test
	public void a02_defaultHeadersOnServletAnnotation_invalid() throws Exception {
		a.request("PUT", "?noTrace=true").accept("text/s3").execute().assertStatus(406).assertBodyContains("Unsupported media-type in request header 'Accept': 'text/s3'");
		a.request("PUT", "?noTrace=true").contentType("text/p3").execute().assertStatus(415).assertBodyContains("Unsupported media-type in request header 'Content-Type': 'text/p3'");
		a.request("PUT", "?noTrace=true").accept("text/s3").contentType("text/p3").execute().assertStatus(415).assertBodyContains("Unsupported media-type in request header 'Content-Type': 'text/p3'");
	}

	//====================================================================================================
	// Test that default Accept and Content-Type headers on servlet annotation are picked up
	// when @RestMethod.parsers/serializers annotations are used.
	//====================================================================================================
	
	@RestResource(
		defaultRequestHeaders={" Accept : text/s2 "," Content-Type : text/p2 "},
		parsers={P1.class,P2.class}, serializers={S1.class,S2.class}
	)
	public static class B {
		@RestMethod(name=PUT, parsers=P3.class, serializers=S3.class)
		public String b(@Body String in) {
			return in;
		}
	}
	
	private static MockRest b = MockRest.create(B.class);
	
	@Test
	public void b01_restMethodWithParsersSerializers_valid() throws Exception {
		b.request("PUT", "/").accept("text/s3").contentType("text/p3").execute().assertBody("s3/p3");
	}

	@Test
	public void b02_restMethodWithParsersSerializers_invalid() throws Exception {
		b.request("PUT", "?noTrace=true").execute()
			.assertStatus(415)
			.assertBodyContains( 
				"Unsupported media-type in request header 'Content-Type': 'text/p2'",
				"Supported media-types: ['text/p3']"
			);
		b.request("PUT", "?noTrace=true").accept("text/s1").execute()
			.assertStatus(415)
			.assertBodyContains(
				"Unsupported media-type in request header 'Content-Type': 'text/p2'",
				"Supported media-types: ['text/p3']"
			);
		b.request("PUT", "?noTrace=true").contentType("text/p1").execute()
			.assertStatus(415)
			.assertBodyContains( 
				"Unsupported media-type in request header 'Content-Type': 'text/p1'",
				"Supported media-types: ['text/p3']"
			);
		b.request("PUT", "?noTrace=true").accept("text/s1").contentType("text/p1").execute()
			.assertStatus(415)
			.assertBodyContains(
				"Unsupported media-type in request header 'Content-Type': 'text/p1'",
				"Supported media-types: ['text/p3']"
			);
		b.request("PUT", "?noTrace=true").accept("text/s1").accept("text/s2").execute()
			.assertStatus(415)
			.assertBodyContains(
				"Unsupported media-type in request header 'Content-Type': 'text/p2'",
				"Supported media-types: ['text/p3']"
			);
		b.request("PUT", "?noTrace=true").accept("text/s1").contentType("text/p2").execute()
			.assertStatus(415)
			.assertBodyContains(
				"Unsupported media-type in request header 'Content-Type': 'text/p2'",
				"Supported media-types: ['text/p3']"
			);
		b.request("PUT", "?noTrace=true").accept("text/s2").contentType("text/p2").execute()
			.assertStatus(415)
			.assertBodyContains(
				"Unsupported media-type in request header 'Content-Type': 'text/p2'",
				"Supported media-types: ['text/p3']"
			);
		b.request("PUT", "?noTrace=true").accept("text/s3").execute()
			.assertStatus(415)
			.assertBodyContains(
				"Unsupported media-type in request header 'Content-Type': 'text/p2'",
				"Supported media-types: ['text/p3']"
			);
		b.request("PUT", "?noTrace=true").contentType("text/p3").execute()
			.assertStatus(406)
			.assertBodyContains(
				"Unsupported media-type in request header 'Accept': 'text/s2'",
				"Supported media-types: ['text/s3']"
			);
	}
	
	
	//====================================================================================================
	// Test that default Accept and Content-Type headers on servlet annotation are picked up
	// when @RestMethod.addParsers/addSerializers annotations are used.
	//====================================================================================================
	
	@RestResource(
		defaultRequestHeaders={" Accept : text/s2 "," Content-Type : text/p2 "},
		parsers={P1.class,P2.class}, serializers={S1.class,S2.class}
	)
	public static class C {
		@RestMethod(name=PUT, parsers=P3.class, serializers=S3.class, inherit="SERIALIZERS,PARSERS")
		public String c(@Body String in) {
			return in;
		}
	}
	
	private static MockRest c = MockRest.create(C.class);

	@Test
	public void c01_restMethodAddParsersSerializersInherit() throws Exception {
		c.request("PUT", "/").execute().assertBody("s2/p2");
		c.request("PUT", "/").accept("text/s1").execute().assertBody("s1/p2");
		c.request("PUT", "/").contentType("text/p1").execute().assertBody("s2/p1");
		c.request("PUT", "/").accept("text/s1").contentType("text/p1").execute().assertBody("s1/p1");
		c.request("PUT", "/").accept("text/s2").execute().assertBody("s2/p2");
		c.request("PUT", "/").contentType("text/p2").execute().assertBody("s2/p2");
		c.request("PUT", "/").accept("text/s2").contentType("text/p2").execute().assertBody("s2/p2");
		c.request("PUT", "/").accept("text/s3").execute().assertBody("s3/p2");
		c.request("PUT", "/").contentType("text/p3").execute().assertBody("s2/p3");
		c.request("PUT", "/").accept("text/s3").contentType("text/p3").execute().assertBody("s3/p3");
	}
	
	@Test
	public void c02_restMethodAddParsersSerializersInherit_invalid() throws Exception {
		c.request("PUT", "?noTrace=true").contentType("text/p4").execute()
			.assertStatus(415)
			.assertBodyContains(
				"Unsupported media-type in request header 'Content-Type': 'text/p4'",
				"Supported media-types: ['text/p3','text/p1','text/p2']"
			);
		c.request("PUT", "?noTrace=true").accept("text/s4").execute()
			.assertStatus(406)
			.assertBodyContains(
				"Unsupported media-type in request header 'Accept': 'text/s4'",
				"Supported media-types: ['text/s3','text/s1','text/s2']"
			);
	}


	//====================================================================================================
	// Various Accept incantations.
	//====================================================================================================
	@RestResource(
		defaultRequestHeaders={" Accept : text/s2 "," Content-Type : text/p2 "},
		parsers={P1.class,P2.class}, serializers={S1.class,S2.class}
	)
	public static class D {
		@RestMethod(name=PUT)
		public String d(@Body String in) {
			return in;
		}
	}
	
	private static MockRest d = MockRest.create(D.class);
	
	@Test
	public void d01_accept_valid() throws Exception {
		// "*/*" should match the first serializer, not the default serializer.
		d.request("PUT", "/").contentType("text/p1").accept("*/*").execute().assertBody("s1/p1");
		// "text/*" should match the first serializer, not the default serializer.
		d.request("PUT", "/").contentType("text/p1").accept("text/*").execute().assertBody("s1/p1");
		d.request("PUT", "/").contentType("text/p1").accept("bad/*,text/*").execute().assertBody("s1/p1");
		d.request("PUT", "/").contentType("text/p1").accept("text/*,bad/*").execute().assertBody("s1/p1");
		d.request("PUT", "/").contentType("text/p1").accept("text/s1;q=0.5,text/s2").execute().assertBody("s2/p1");
		d.request("PUT", "/").contentType("text/p1").accept("text/s1,text/s2;q=0.5").execute().assertBody("s1/p1");
	}
	@Test
	public void d02_accept_invalid() throws Exception {
		d.request("PUT", "?noTrace=true").contentType("text/p1").accept("bad/*").execute()
			.assertStatus(406)
			.assertBodyContains(
				"Unsupported media-type in request header 'Accept': 'bad/*'",
				"Supported media-types: ['text/s1','text/s2']"
			);
	}

	
	//====================================================================================================
	// Test that default Accept and Content-Type headers on method annotation are picked up
	// when @RestMethod.parsers/serializers annotations are used.
	//====================================================================================================
	@RestResource(
		defaultRequestHeaders={" Accept : text/s2 "," Content-Type : text/p2 "},
		parsers={P1.class,P2.class}, serializers={S1.class,S2.class}
	)
	public static class E {
		@RestMethod(name=PUT, defaultRequestHeaders={"Accept: text/s3","Content-Type: text/p3"}, parsers=P3.class, serializers=S3.class)
		public String e(@Body String in) {
			return in;
		}
	}
	
	private static MockRest e = MockRest.create(E.class);

	@Test
	public void e01_restMethodParserSerializerAnnotations_valid() throws Exception {
		e.request("PUT", "/").execute().assertBody("s3/p3");
		e.request("PUT", "/").accept("text/s3").execute().assertBody("s3/p3");
		e.request("PUT", "/").contentType("text/p3").execute().assertBody("s3/p3");
		e.request("PUT", "/").accept("text/s3").contentType("text/p3").execute().assertBody("s3/p3");
	}
	@Test
	public void e02_restMethodParserSerializerAnnotations_invalid() throws Exception {
		e.request("PUT", "?noTrace=true").accept("text/s1").execute()
			.assertStatus(406)
			.assertBodyContains(
				"Unsupported media-type in request header 'Accept': 'text/s1'",
				"Supported media-types: ['text/s3']"
			);
		e.request("PUT", "?noTrace=true").contentType("text/p1").execute()
			.assertStatus(415)
			.assertBodyContains(
				"Unsupported media-type in request header 'Content-Type': 'text/p1'",
				"Supported media-types: ['text/p3']"
			);
		e.request("PUT", "?noTrace=true").accept("text/s1").contentType("text/p1").execute()
			.assertStatus(415)
			.assertBodyContains(
				"Unsupported media-type in request header 'Content-Type': 'text/p1'",
				"Supported media-types: ['text/p3']"
			);
		e.request("PUT", "?noTrace=true").accept("text/s2").execute()
			.assertStatus(406)
			.assertBodyContains(
				"Unsupported media-type in request header 'Accept': 'text/s2'",
				"Supported media-types: ['text/s3']"
			);
		e.request("PUT", "?noTrace=true").contentType("text/p2").execute()
			.assertStatus(415)
			.assertBodyContains(
				"Unsupported media-type in request header 'Content-Type': 'text/p2'",
				"Supported media-types: ['text/p3']"
			);
		e.request("PUT", "?noTrace=true").accept("text/s2").contentType("text/p2").execute()
			.assertStatus(415)
			.assertBodyContains(
				"Unsupported media-type in request header 'Content-Type': 'text/p2'",
				"Supported media-types: ['text/p3']"
			);
	}

	
	//====================================================================================================
	// Test that default Accept and Content-Type headers on method annotation are picked up
	// 	when @RestMethod.addParsers/addSerializers annotations are used.
	//====================================================================================================
	@RestResource(
		defaultRequestHeaders={" Accept : text/s2 "," Content-Type : text/p2 "},
		parsers={P1.class,P2.class}, serializers={S1.class,S2.class}
	)
	public static class F {
		@RestMethod(name=PUT, defaultRequestHeaders={"Accept: text/s3","Content-Type: text/p3"}, parsers=P3.class, serializers=S3.class, inherit="SERIALIZERS,PARSERS")
		public String f(@Body String in) {
			return in;
		}
	}
	
	private static MockRest f = MockRest.create(F.class);

	@Test
	public void f01_restMethodAddParsersSerializersAnnotations_valid() throws Exception {
		f.request("PUT", "/").execute().assertBody("s3/p3");
		f.request("PUT", "/").accept("text/s1").execute().assertBody("s1/p3");
		f.request("PUT", "/").contentType("text/p1").execute().assertBody("s3/p1");
		f.request("PUT", "/").accept("text/s1").contentType("text/p1").execute().assertBody("s1/p1");
		f.request("PUT", "/").accept("text/s2").execute().assertBody("s2/p3");
		f.request("PUT", "/").contentType("text/p2").execute().assertBody("s3/p2");
		f.request("PUT", "/").accept("text/s2").contentType("text/p2").execute().assertBody("s2/p2");
		f.request("PUT", "/").accept("text/s3").execute().assertBody("s3/p3");
		f.request("PUT", "/").contentType("text/p3").execute().assertBody("s3/p3");
		f.request("PUT", "/").accept("text/s3").contentType("text/p3").execute().assertBody("s3/p3");
	}
}
