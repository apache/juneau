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
package org.apache.juneau.rest.server.processor;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * Tests {@link ResponseBeanProcessor} — the processor that handles {@link Response @Response}-annotated
 * return values, dispatching status code, response headers ({@link Header @Header}), and body content
 * ({@link Content @Content}) into the underlying {@code RestResponse}.
 *
 * <p>
 * These tests build minimal {@code @Response} bean shapes inline and exercise them through
 * {@link MockRestClient}, covering: default status / explicit {@link StatusCode @StatusCode} /
 * {@link StatusCode @StatusCode}-via-method, {@link Header @Header}-annotated single-value methods,
 * {@link Header @Header}-with-name-{@code "*"} methods returning Map / Collection / Array /
 * {@code HttpHeader} / {@code HttpPart}, {@link Content @Content}-annotated body methods including
 * {@code OutputStream}/{@code Writer} parameter overloads, response-bean-as-{@link Throwable}, and
 * pass-through (NEXT) for non-response output.
 */
@SuppressWarnings({"serial"})
class ResponseBeanProcessor_Test extends TestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// A: @Response bean — explicit @StatusCode on class, @Content method, plain @Header methods.
	// -----------------------------------------------------------------------------------------------------------------

	@Response @StatusCode(201)
	public static class A1 {
		@Header("X-Foo")
		public String getFoo() { return "foo-value"; }

		@Header("X-Bar")
		public int getBar() { return 42; }

		@Content
		public String getContent() { return "a1-body"; }
	}

	@Rest
	public static class A {
		@RestGet("/a1")
		public A1 a1() { return new A1(); }
	}

	private static final MockRestClient CA = MockRestClient.buildLax(A.class);

	@Test void a01_responseBean_emitsStatus201() throws Exception {
		CA.get("/a1").run().assertStatus(201);
	}

	@Test void a02_responseBean_emitsHeaders() throws Exception {
		CA.get("/a1").run()
			.assertHeader("X-Foo").is("foo-value")
			.assertHeader("X-Bar").is("42");
	}

	@Test void a03_responseBean_emitsContent() throws Exception {
		CA.get("/a1").run().assertContent("a1-body");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: @Response bean with @StatusCode on a method (status getter) — overrides class-level / default.
	// -----------------------------------------------------------------------------------------------------------------

	@Response
	public static class B1 {
		@StatusCode
		public int getStatus() { return 202; }

		@Header("X-B")
		public String getB() { return "b-val"; }

		@Content
		public String getContent() { return "b1-body"; }
	}

	@Rest
	public static class B {
		@RestGet("/b1")
		public B1 b1() { return new B1(); }
	}

	private static final MockRestClient CB = MockRestClient.buildLax(B.class);

	@Test void b01_statusMethod_setsResponseStatus() throws Exception {
		CB.get("/b1").run()
			.assertStatus(202)
			.assertHeader("X-B").is("b-val")
			.assertContent("b1-body");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: @Response bean with no explicit status (default 200) and no @Content (lets PojoProcessor render).
	// -----------------------------------------------------------------------------------------------------------------

	@Response
	public static class C1 {
		@Header("X-C")
		public String getC() { return "c-val"; }

		@Override
		public String toString() { return "c1-tostring"; }
	}

	@Rest
	public static class C {
		@RestGet("/c1")
		public C1 c1() { return new C1(); }
	}

	private static final MockRestClient CC = MockRestClient.buildLax(C.class);

	@Test void c01_noStatusCode_defaults200() throws Exception {
		CC.get("/c1").run().assertStatus(200);
	}

	@Test void c02_noContentMethod_passesThroughToPojoProcessor() throws Exception {
		// No @Content method: ResponseBeanProcessor returns NEXT and PojoProcessor renders toString().
		CC.get("/c1").run()
			.assertHeader("X-C").is("c-val")
			.assertContent("c1-tostring");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: @Header(name="*") — Map, Collection, array, HttpHeader, HttpPart entries; single HttpHeader/HttpPart values.
	// -----------------------------------------------------------------------------------------------------------------

	@Response
	public static class D1 {
		@Header("*")
		public Map<String,Object> getHeaders() {
			Map<String,Object> m = new LinkedHashMap<>();
			m.put("X-Map-A", "ma");
			m.put("X-Map-B", 7);
			return m;
		}

		@Override
		public String toString() { return "d1"; }
	}

	@Response
	public static class D2 {
		@Header("*")
		public Collection<HttpStringHeader> getHeaders() {
			return List.of(HttpStringHeader.of("X-Coll-A", "ca"), HttpStringHeader.of("X-Coll-B", "cb"));
		}

		@Override
		public String toString() { return "d2"; }
	}

	@Response
	public static class D3 {
		@Header("*")
		public HttpStringHeader[] getHeaders() {
			return new HttpStringHeader[] {
				HttpStringHeader.of("X-Arr-A", "aa"),
				HttpStringHeader.of("X-Arr-B", "ab")
			};
		}

		@Override
		public String toString() { return "d3"; }
	}

	@Response
	public static class D4 {
		// Single HttpHeader-typed @Header method (non-"*" name path: HttpHeader instanceof branch).
		@Header("X-D")
		public HttpStringHeader getH() { return HttpStringHeader.of("X-D", "d-direct"); }

		@Override
		public String toString() { return "d4"; }
	}

	@Response
	public static class D5 {
		// Single HttpPart-typed @Header method (non-"*" name path: HttpPart instanceof branch).
		@Header("X-D5")
		public HttpPart getH() {
			return new HttpPart() {
				@Override public String getName() { return "X-D5"; }
				@Override public String getValue() { return "d5-part"; }
			};
		}

		@Override
		public String toString() { return "d5"; }
	}

	@Response
	public static class D6 {
		// "*" with array of HttpPart entries.
		@Header("*")
		public HttpPart[] getHeaders() {
			return new HttpPart[] {
				new HttpPart() {
					@Override public String getName() { return "X-D6-A"; }
					@Override public String getValue() { return "d6a"; }
				},
				new HttpPart() {
					@Override public String getName() { return "X-D6-B"; }
					@Override public String getValue() { return "d6b"; }
				}
			};
		}

		@Override
		public String toString() { return "d6"; }
	}

	@Rest
	public static class D {
		@RestGet("/d1") public D1 d1() { return new D1(); }
		@RestGet("/d2") public D2 d2() { return new D2(); }
		@RestGet("/d3") public D3 d3() { return new D3(); }
		@RestGet("/d4") public D4 d4() { return new D4(); }
		@RestGet("/d5") public D5 d5() { return new D5(); }
		@RestGet("/d6") public D6 d6() { return new D6(); }
	}

	private static final MockRestClient CD = MockRestClient.buildLax(D.class);

	@Test void d01_starHeader_mapEntries() throws Exception {
		CD.get("/d1").run()
			.assertHeader("X-Map-A").is("ma")
			.assertHeader("X-Map-B").is("7");
	}

	@Test void d02_starHeader_collectionOfHttpHeader() throws Exception {
		CD.get("/d2").run()
			.assertHeader("X-Coll-A").is("ca")
			.assertHeader("X-Coll-B").is("cb");
	}

	@Test void d03_starHeader_arrayOfHttpHeader() throws Exception {
		CD.get("/d3").run()
			.assertHeader("X-Arr-A").is("aa")
			.assertHeader("X-Arr-B").is("ab");
	}

	@Test void d04_singleHttpHeaderTyped() throws Exception {
		CD.get("/d4").run().assertHeader("X-D").is("d-direct");
	}

	@Test void d05_singleHttpPartTyped() throws Exception {
		CD.get("/d5").run().assertHeader("X-D5").is("d5-part");
	}

	@Test void d06_starHeader_arrayOfHttpPart() throws Exception {
		CD.get("/d6").run()
			.assertHeader("X-D6-A").is("d6a")
			.assertHeader("X-D6-B").is("d6b");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// E: @Content method writes directly to OutputStream / Writer (parameter-typed body method).
	// -----------------------------------------------------------------------------------------------------------------

	@Response
	public static class E1 {
		@Content
		public void write(OutputStream out) throws IOException {
			out.write("e1-stream-body".getBytes());
		}
	}

	@Response
	public static class E2 {
		@Content
		public void write(Writer w) throws IOException {
			w.write("e2-writer-body");
		}
	}

	@Rest
	public static class E {
		@RestGet("/e1") public E1 e1() { return new E1(); }
		@RestGet("/e2") public E2 e2() { return new E2(); }
	}

	private static final MockRestClient CE = MockRestClient.buildLax(E.class);

	@Test void e01_contentMethod_writesToOutputStream() throws Exception {
		CE.get("/e1").run().assertStatus(200).assertContent("e1-stream-body");
	}

	@Test void e02_contentMethod_writesToWriter() throws Exception {
		CE.get("/e2").run().assertStatus(200).assertContent("e2-writer-body");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// F: Detection — explicit @Response on the method (return type itself NOT @Response-annotated).
	// -----------------------------------------------------------------------------------------------------------------

	public static class F1Plain {
		@Override public String toString() { return "f1-plain"; }
	}

	@Rest
	public static class F {
		@RestGet("/f1")
		@Response
		public F1Plain f1() { return new F1Plain(); }

		// Non-response output: plain string → ResponseBeanProcessor returns NEXT immediately.
		@RestGet("/f2")
		public String f2() { return "plain-string"; }

		// Null output → ResponseBeanProcessor returns NEXT immediately.
		@RestGet("/f3")
		public Object f3() { return null; }
	}

	private static final MockRestClient CF = MockRestClient.buildLax(F.class);

	@Test void f01_methodLevelResponseAnnotation() throws Exception {
		CF.get("/f1").run().assertStatus(200).assertContent("f1-plain");
	}

	@Test void f02_nonResponseContent_passesThrough() throws Exception {
		CF.get("/f2").run().assertStatus(200).assertContent("plain-string");
	}

	@Test void f03_nullContent_passesThrough() throws Exception {
		// Null output: ResponseBeanProcessor sees null and returns NEXT immediately (line 60-61 false branch).
		CF.get("/f3").run().assertStatus(200);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// G: @Response bean returned as Throwable subclass — exception path through ResponseBeanProcessor.
	// -----------------------------------------------------------------------------------------------------------------

	@Response @StatusCode(418)
	public static class G1 extends RuntimeException {
		@Header("X-G")
		public String getG() { return "g-thrown"; }

		@Content
		public String getContent() { return "i-am-a-teapot"; }
	}

	@Rest
	public static class G {
		@RestGet("/g1")
		public String g1() { throw new G1(); }
	}

	private static final MockRestClient CG = MockRestClient.buildLax(G.class);

	@Test void g01_throwableResponseBean_rendersStatusHeadersBody() throws Exception {
		CG.get("/g1").run()
			.assertStatus(418)
			.assertHeader("X-G").is("g-thrown")
			.assertContent("i-am-a-teapot");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// H: @Header method whose value is null — toHeader(...) null branch and skipIfEmpty semantics.
	// -----------------------------------------------------------------------------------------------------------------

	@Response
	public static class H1 {
		@Header("X-H")
		public String getH() { return null; }

		@Override public String toString() { return "h1"; }
	}

	@Rest
	public static class H {
		@RestGet("/h1") public H1 h1() { return new H1(); }
	}

	private static final MockRestClient CH = MockRestClient.buildLax(H.class);

	@Test void h01_nullHeader_passesThrough() throws Exception {
		// Null header values should not appear (or appear as blank) — assert status only.
		CH.get("/h1").run().assertStatus(200).assertContent("h1");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// I: Iterate(...) error path — @Header("*") returning a non-iterable, non-array, non-Map, non-Collection value.
	//    Surfaces as a 500 InternalServerError ("Could not iterate over Headers ...").
	// -----------------------------------------------------------------------------------------------------------------

	@Response
	public static class I1 {
		@Header("*")
		public String getBadHeaders() { return "not-iterable"; }

		@Override public String toString() { return "i1"; }
	}

	@Rest
	public static class I {
		@RestGet("/i1") public I1 i1() { return new I1(); }
	}

	private static final MockRestClient CI = MockRestClient.buildLax(I.class);

	@Test void i01_starHeader_nonIterable_returns500() throws Exception {
		CI.get("/i1").run().assertStatus(500);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// J: @Header("*") returning null — iterate(null) returns empty list; no headers added; success.
	// -----------------------------------------------------------------------------------------------------------------

	@Response
	public static class J1 {
		@Header("*")
		public Map<String,Object> getHeaders() { return null; }

		@Override public String toString() { return "j1"; }
	}

	@Rest
	public static class J {
		@RestGet("/j1") public J1 j1() { return new J1(); }
	}

	private static final MockRestClient CJ = MockRestClient.buildLax(J.class);

	@Test void j01_starHeader_null_emitsNoHeaders() throws Exception {
		CJ.get("/j1").run().assertStatus(200).assertContent("j1");
	}
}
