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
package org.apache.juneau.rest.client;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * Remote-proxy <b>feature-parity</b> suite for the <b>next-generation</b> REST client engine
 * ({@link RestClient#remote(Class)} &rarr; {@link org.apache.juneau.rest.server.server.client.remote.RemoteClient}), exercised against
 * a synthetic {@code @Remote} interface that reproduces a comprehensive remote-proxy feature matrix (F1&ndash;F24) plus
 * verb-completeness rows (PUT/DELETE/{@code @RemoteOp}).
 *
 * <p>
 * This is the next-gen counterpart to the classic {@code org.apache.juneau.http.remote.Remote_*_Test} corpus and tracks
 * The single shared engine
 * ({@code RemoteClient}) is the system-under-test; it is driven through two columns:
 * <ul>
 * 	<li><b>{@link B_MockServerColumn}</b> &mdash; the {@link MockRestClient} in-JVM servlet round-trip
 * 		(via {@code MockRestClient.create(Fixture.class).getClient().remote(iface)}), asserting the
 * 		<i>deserialized/echoed response</i>.
 * 	<li><b>{@link C_RealTransportColumn}</b> &mdash; a next-gen {@link RestClient} built directly over a network-free
 * 		{@link MockHttpTransport} stub, asserting the <i>serialized request shape</i> the engine emits.
 * </ul>
 *
 * <p>
 * Cross-walk: each synthetic method's comment names the feature id (F-row) it exercises.
 */
@SuppressWarnings({
	"java:S5961",  // High assertion/case count is expected in a comprehensive parity matrix.
	"java:S114",   // Snake_case fixture interface names (A_ParityClient, A_OpClient) are intentional test-local naming.
	"resource",    // try-with-resources closes clients; RestResponse closed where applicable.
	"unused"       // body parameters in fixture REST methods kept for @Content annotation binding even when return value is hardcoded
})
class RemoteProxy_FeatureParity_Test {

	// =================================================================================================================
	// Synthetic DTOs / enums (kept minimal)
	// =================================================================================================================

	/** Enum used for the {@code @Query} enum capability (F9). */
	public enum A_InstanceSource { USER, SYSTEM }

	/** Enum used for the {@code @Header} enum capability (F11). */
	public enum A_DataFileType { CSV, JSON }

	/** Simple bean used for the bean content/return capabilities (F12/F13/F15/F16). */
	public static class A_Bean {
		private String name;
		public String getName() { return name; }
		public void setName(String value) { name = value; }
	}

	/** Two-property bean used for the G8 "dynamic name/value pairs from a bean" forms (a&rarr;part, b&rarr;part). */
	public static class A_PairBean {
		private String a, b;
		public String getA() { return a; }
		public void setA(String value) { a = value; }
		public String getB() { return b; }
		public void setB(String value) { b = value; }
		static A_PairBean of(String a, String b) { var x = new A_PairBean(); x.a = a; x.b = b; return x; }
	}

	/** A {@code @Request}-style bean for the {@code @Request} binding capability (G5). */
	@Request
	public static class A_RequestBean {
		@Query("rqA") public String getA() { return "ra"; }
		@Header("X-Rq") public String getH() { return "rh"; }
	}

	// =================================================================================================================
	// Synthetic @Remote client interface — reproduces F1–F24 + verb completeness + the gap rows
	// =================================================================================================================

	@Remote(path="/rest")
	public interface A_ParityClient {

		// ---- Supported today (active cells) -------------------------------------------------------------------------

		@RemoteGet("/ping")                       String ping();                                  // F2 GET, F20 String return
		@RemotePost("/echo")                      String postEcho(@Content String body);          // F3 POST + raw String body
		@RemotePut("/echo")                       String putEcho(@Content String body);           // PUT verb completeness
		@RemotePatch("/echo")                     String patchEcho(@Content String body);         // F4 PATCH
		@RemoteDelete("/echo")                    String deleteEcho();                            // DELETE verb completeness

		@RemoteGet("/instances/{name}")           String getInstanceName(@Path("name") String name);                       // F5 @Path
		@RemoteGet("/instances/{name}/{field}")   String getInstanceField(@Path("name") String name, @Path("field") String field); // F5 multi @Path

		// F6 @Query String, F7 @Query boxed (Integer/Boolean), F8 @Query primitive boolean, F9 @Query enum
		@RemoteGet("/query")
		String query(
			@Query("environment") String environment,
			@Query("position") Integer position,
			@Query("includeDeleted") Boolean includeDeleted,
			@Query("replace") boolean replace,
			@Query("source") A_InstanceSource source
		);

		@RemoteGet("/headers")                    String headers(@Header("X-Env") String env, @Header("X-Type") A_DataFileType type); // F10 @Header String + F11 @Header enum

		@RemoteGet(path="/ping", returns=RemoteReturn.STATUS)   int pingStatus();               // RemoteReturn.STATUS (int)
		@RemoteGet(path="/ping", returns=RemoteReturn.STATUS)   boolean pingOk();               // RemoteReturn.STATUS (boolean)
		@RemoteGet(path="/ping", returns=RemoteReturn.RESPONSE) RestResponse pingResponse();    // RemoteReturn.RESPONSE
		@RemoteGet("/ping")                                     void pingVoid();                // void return

		// ---- Former gap rows (all now supported) --------------------------------------------------------------------

		@RemotePost("/beanContent")               String postBean(@Content A_Bean bean);          // F12 @Content bean            (G1)
		@RemotePost("/listContent")               String postList(@Content List<A_Bean> beans);   // F13 @Content List<bean>      (G1)
		@RemotePost("/readerContent")             String postReader(@Content Reader body);        // F14 @Content Reader          (G7)
		@RemoteGet("/bean")                       A_Bean getBean();                               // F15 return bean              (G2)
		@RemoteGet("/list")                       List<A_Bean> getList();                         // F16 return List<bean>        (G2)
		@RemoteGet("/map")                        Map<String,String> getMap();                    // F17 return Map               (G2)
		@RemotePatch("/count")                    Integer patchCount(@Content String body);       // F18 return Integer           (G2)
		@RemoteGet("/object")                     Object getObject();                             // F19 return Object            (G2)
		@RemotePost("/ok")                        Ok postOk(@Content String body);                // F21 return HTTP-response bean (G2/G3)
		@RemoteGet("/notfound/{name}")            String getOrThrow(@Path("name") String name) throws NotFound, BadRequest, InternalServerError; // F22 typed exception (G4)

		@RemotePost("/form")                      String postForm(@FormData("a") String a);       // G5 @FormData unbound
		@RemoteGet("/req")                        String getReq(@Request A_RequestBean req);      // G5 @Request unbound

		// G8 — dynamic name/value pairs, QUERY (each form should expand to N distinct query params: a=1&b=2)
		@RemoteGet("/dynq")                       String dynQueryMap(@Query Map<String,Object> params);         // @Query Map (== @Query("*"))
		@RemoteGet("/dynq")                       String dynQueryMapStar(@Query("*") Map<String,Object> params);// @Query("*") Map
		@RemoteGet("/dynq")                       String dynQueryPartList(@Query("*") PartList params);         // NameValuePairs / part-list form
		@RemoteGet("/dynq")                       String dynQueryBean(@Query A_PairBean params);                // bean -> name/value pairs

		// G8 — dynamic name/value pairs, HEADER (each form should expand to N distinct headers: a:1, b:2)
		@RemoteGet("/dynh")                       String dynHeaderMap(@Header Map<String,Object> headers);          // @Header Map (== @Header("*"))
		@RemoteGet("/dynh")                       String dynHeaderMapStar(@Header("*") Map<String,Object> headers); // @Header("*") Map
		@RemoteGet("/dynh")                       String dynHeaderList(@Header("*") HttpHeaderList headers);        // HeaderList / NameValuePairs form
		@RemoteGet("/dynh")                       String dynHeaderBean(@Header A_PairBean headers);                 // bean -> name/value pairs

		// G8 — dynamic name/value pairs, FORM DATA (each form should expand to N distinct form fields: a=1&b=2)
		@RemotePost("/dynf")                      String dynFormDataMap(@FormData Map<String,Object> params);          // @FormData Map (== @FormData("*"))
		@RemotePost("/dynf")                      String dynFormDataMapStar(@FormData("*") Map<String,Object> params); // @FormData("*") Map

		@RemoteGet("/def")                        String withDefault(@Query(name="view", def="*") String view); // G9 param default

		@RemoteGet("/list")                       java.util.concurrent.CompletableFuture<List<A_Bean>> getListAsync(); // G11 async return
		@RemoteGet("/bean")                       Optional<A_Bean> getBeanOptional();             // G11 Optional return

		// G6 — part serializer honors @Schema (collection format, skipIfEmpty) instead of toString()/enum-name luck
		@RemoteGet("/csv")                        String getPipes(@Query(name="tags", schema=@Schema(collectionFormat="pipes")) String[] tags);     // G6 collectionFormat=pipes
		@RemoteGet("/skip")                       String getSkip(@Query(name="q", schema=@Schema(skipIfEmpty=true)) String q, @Query("keep") String keep); // G6 skipIfEmpty

		// G12 — @PathRemainder appended as the trailing path remainder
		@RemoteGet("/remainder")                  String getRemainder(@PathRemainder String remainder);  // G12 @PathRemainder
	}

	/** Tiny secondary interface to assert {@code @RemoteOp} verb resolution (F-row verb completeness). */
	@Remote(path="/op")
	public interface A_OpClient {
		@RemoteOp(method="POST", path="/x") String opNormalForm();
		@RemoteOp("PUT /y")                  String opShortForm();
	}

	// =================================================================================================================
	// Shared in-JVM @Rest mock-server fixture (root-mounted; method paths carry the /rest prefix the proxy emits)
	// =================================================================================================================

	@Rest
	public static class A_ParityResource {

		@RestGet("/rest/ping")  public String ping() { return "pong"; }

		@RestPost("/rest/echo") public String echoPost(@Content String body) { return "posted:" + body; }
		@RestPut("/rest/echo")  public String echoPut(@Content String body) { return "put:" + body; }
		@RestOp(method="PATCH", path="/rest/echo") public String echoPatch(@Content String body) { return "patched:" + body; }
		@RestDelete("/rest/echo") public String echoDelete() { return "deleted"; }

		@RestGet("/rest/instances/{name}") public String inst(@Path("name") String name) { return "name=" + name; }
		@RestGet("/rest/instances/{name}/{field}") public String instField(@Path("name") String name, @Path("field") String field) { return name + "/" + field; }

		@RestGet("/rest/query")
		public String query(
			@Query("environment") String environment,
			@Query("position") String position,
			@Query("includeDeleted") String includeDeleted,
			@Query("replace") String replace,
			@Query("source") String source
		) {
			return environment + "|" + position + "|" + includeDeleted + "|" + replace + "|" + source;
		}

		@RestGet("/rest/headers")
		public String headers(@Header("X-Env") String env, @Header("X-Type") String type) {
			return env + "|" + type;
		}

		// Gap endpoints — return plain strings so the fixture needs no server-side serializer config.
		@RestPost("/rest/beanContent") public String beanContent(@Content String body) { return body; }
		@RestPost("/rest/listContent") public String listContent(@Content String body) { return body; }
		@RestPost("/rest/readerContent") public String readerContent(@Content String body) { return body; }
		@RestGet("/rest/bean")  public String bean() { return "{\"name\":\"na44\"}"; }
		@RestGet("/rest/list")  public String list() { return "[{\"name\":\"na44\"}]"; }
		@RestGet("/rest/map")   public String map() { return "{\"k\":\"v\"}"; }
		@RestPatch("/rest/count") public String count(@Content String body) { return "5"; }
		@RestGet("/rest/object") public String object() { return "123"; }
		@RestPost("/rest/ok")   public String ok(@Content String body) { return "OK"; }
		@RestGet("/rest/notfound/{name}") public String notfound(@Path("name") String name, org.apache.juneau.rest.server.RestResponse res) { res.setStatus(404); return "not found:" + name; }
		@RestPost("/rest/form") public String form(@FormData("a") String a) { return "a=" + a; }
		@RestGet("/rest/req")   public String req(@Query("rqA") String a) { return "a=" + a; }
		@RestGet("/rest/dynq")  public String dynQ(@Query("a") String a, @Query("b") String b) { return "a=" + a + ",b=" + b; }
		@RestGet("/rest/dynh")  public String dynH(@Header("a") String a, @Header("b") String b) { return "a=" + a + ",b=" + b; }
		@RestPost("/rest/dynf") public String dynF(@FormData("a") String a, @FormData("b") String b) { return "a=" + a + ",b=" + b; }
		@RestGet("/rest/def")   public String def(@Query("view") String view) { return "view=" + view; }
		@RestGet("/rest/csv")   public String csv(@Query("tags") String tags) { return "tags=" + tags; }
		@RestGet("/rest/skip")  public String skip(@Query("q") String q, @Query("keep") String keep) { return "q=" + q + ",keep=" + keep; }
		@RestGet("/rest/remainder/*") public String remainder(@PathRemainder String r) { return "r=" + r; }
	}

	// =================================================================================================================
	// Shared helpers
	// =================================================================================================================

	/** Capture transport for the real-{@link RestClient} column: records every request, replies 200/"result". */
	static MockHttpTransport.Builder captureTransport(List<TransportRequest> captured) {
		return MockHttpTransport.builder().fallback(req -> {
			captured.add(req);
			return TransportResponse.builder().statusCode(200).body(new ByteArrayInputStream("result".getBytes())).build();
		});
	}

	static String readBody(TransportRequest req) throws IOException {
		var body = req.getBody();
		if (body == null)
			return null;
		var baos = new ByteArrayOutputStream();
		body.writeTo(baos);
		return baos.toString();
	}

	// =================================================================================================================
	// A — Metadata / verb resolution (RrpcInterfaceMeta) — F1–F4 + @RemoteOp
	// =================================================================================================================

	@Nested class A_Metadata {

		@Test void a01_basePath_F1() {
			var meta = RrpcInterfaceMeta.of(A_ParityClient.class);
			assertEquals("/rest", meta.getBasePath());
			assertEquals(A_ParityClient.class, meta.getInterface());
		}

		@Test void a02_verbResolution_F2_F4() throws Exception {
			var meta = RrpcInterfaceMeta.of(A_ParityClient.class);
			assertEquals("GET", meta.getMethodMeta(A_ParityClient.class.getMethod("ping")).getHttpMethod());
			assertEquals("POST", meta.getMethodMeta(A_ParityClient.class.getMethod("postEcho", String.class)).getHttpMethod());
			assertEquals("PUT", meta.getMethodMeta(A_ParityClient.class.getMethod("putEcho", String.class)).getHttpMethod());
			assertEquals("PATCH", meta.getMethodMeta(A_ParityClient.class.getMethod("patchEcho", String.class)).getHttpMethod());
			assertEquals("DELETE", meta.getMethodMeta(A_ParityClient.class.getMethod("deleteEcho")).getHttpMethod());
		}

		@Test void a03_returnModes_resolved() throws Exception {
			var meta = RrpcInterfaceMeta.of(A_ParityClient.class);
			assertEquals(RemoteReturn.BODY, meta.getMethodMeta(A_ParityClient.class.getMethod("ping")).getReturnType());
			assertEquals(RemoteReturn.STATUS, meta.getMethodMeta(A_ParityClient.class.getMethod("pingStatus")).getReturnType());
			assertEquals(RemoteReturn.RESPONSE, meta.getMethodMeta(A_ParityClient.class.getMethod("pingResponse")).getReturnType());
		}

		@Test void a04_remoteOp_normalAndShortForm() throws Exception {
			var meta = RrpcInterfaceMeta.of(A_OpClient.class);
			var mm1 = meta.getMethodMeta(A_OpClient.class.getMethod("opNormalForm"));
			assertEquals("POST", mm1.getHttpMethod());
			assertEquals("x", mm1.getPath()); // @RemoteOp trims slashes
			var mm2 = meta.getMethodMeta(A_OpClient.class.getMethod("opShortForm"));
			assertEquals("PUT", mm2.getHttpMethod());
			assertEquals("y", mm2.getPath());
		}
	}

	// =================================================================================================================
	// B — Mock-server column (MockRestClient round-trip; asserts deserialized/echoed response)
	// =================================================================================================================

	@Nested class B_MockServerColumn {

		private A_ParityClient proxy(MockRestClient mrc) {
			return mrc.getClient().remote(A_ParityClient.class);
		}

		// ---- Active cells -------------------------------------------------------------------------------------------

		@Test void b01_get_F2_F20() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				assertEquals("pong", proxy(mrc).ping());
			}
		}

		@Test void b02_post_rawStringBody_F3() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				assertEquals("posted:payload", proxy(mrc).postEcho("payload"));
			}
		}

		@Test void b03_put_verb() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				assertEquals("put:payload", proxy(mrc).putEcho("payload"));
			}
		}

		@Test void b04_patch_F4() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				assertEquals("patched:payload", proxy(mrc).patchEcho("payload"));
			}
		}

		@Test void b05_delete_verb() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				assertEquals("deleted", proxy(mrc).deleteEcho());
			}
		}

		@Test void b06_path_F5() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				assertEquals("name=na44", proxy(mrc).getInstanceName("na44"));
			}
		}

		@Test void b07_pathMulti_F5() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				assertEquals("na44/version", proxy(mrc).getInstanceField("na44", "version"));
			}
		}

		@Test void b08_query_F6_F7_F8_F9() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				assertEquals("stmfa|3|true|true|USER", proxy(mrc).query("stmfa", 3, Boolean.TRUE, true, A_InstanceSource.USER));
			}
		}

		@Test void b09_header_F10_F11() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				assertEquals("prod|CSV", proxy(mrc).headers("prod", A_DataFileType.CSV));
			}
		}

		@Test void b10_returnStatus_int() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				assertEquals(200, proxy(mrc).pingStatus());
			}
		}

		@Test void b11_returnStatus_boolean() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				assertTrue(proxy(mrc).pingOk());
			}
		}

		@Test void b12_returnResponse() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				try (var resp = proxy(mrc).pingResponse()) {
					assertEquals(200, resp.getStatusCode());
					assertEquals("pong", resp.getBodyAsString());
				}
			}
		}

		@Test void b13_returnVoid() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				assertDoesNotThrow(() -> proxy(mrc).pingVoid());
			}
		}

		// ---- Former gap cells (response/body-centric) — now active ------------------------------------------------

		@Test void b20_contentBean_serialized_F12() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				var b = new A_Bean(); b.setName("na44");
				assertTrue(proxy(mrc).postBean(b).contains("na44"));
			}
		}

		@Test void b21_contentList_serialized_F13() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				var b = new A_Bean(); b.setName("na44");
				assertTrue(proxy(mrc).postList(List.of(b)).contains("na44"));
			}
		}

		@Test void b22_contentReader_streamed_F14() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				assertEquals("hello", proxy(mrc).postReader(new StringReader("hello")));
			}
		}

		@Test void b23_returnBean_parsed_F15() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				var bean = proxy(mrc).getBean();
				assertEquals("na44", bean.getName());
			}
		}

		@Test void b24_returnList_parsed_F16() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				var l = proxy(mrc).getList();
				assertEquals(1, l.size());
				assertEquals("na44", l.get(0).getName());
			}
		}

		@Test void b25_returnMap_parsed_F17() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				assertEquals("v", proxy(mrc).getMap().get("k"));
			}
		}

		@Test void b26_returnInteger_parsed_F18() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				assertEquals(Integer.valueOf(5), proxy(mrc).patchCount("x"));
			}
		}

		@Test void b27_returnObject_parsed_F19() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				assertEquals(123, proxy(mrc).getObject());
			}
		}

		@Test void b28_returnResponseBean_F21() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				assertNotNull(proxy(mrc).postOk("x"));
			}
		}

		@Test void b29_typedException_onErrorStatus_F22() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				var p = proxy(mrc);
				assertThrows(NotFound.class, () -> p.getOrThrow("missing"));
			}
		}

		@Test void b30_formData_bound_G5() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				assertEquals("a=v", proxy(mrc).postForm("v"));
			}
		}

		@Test void b31_requestBean_expanded_G5() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				assertEquals("a=ra", proxy(mrc).getReq(new A_RequestBean()));
			}
		}

		@Test void b32_optionalReturn_G11() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				assertTrue(proxy(mrc).getBeanOptional().isPresent());
			}
		}

		// ---- G6 (part serializer / @Schema) + G12 (@PathRemainder) — final slice -----------------------------------

		@Test void b33_partSerializer_collectionFormat_G6() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				// @Schema(collectionFormat="pipes") -> "a|b|c" rather than the array's toString()
				assertEquals("tags=a|b|c", proxy(mrc).getPipes(new String[]{"a", "b", "c"}));
			}
		}

		@Test void b34_partSerializer_skipIfEmpty_G6() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				// @Schema(skipIfEmpty=true) on an empty value omits the "q" param entirely
				assertEquals("q=null,keep=y", proxy(mrc).getSkip("", "y"));
			}
		}

		@Test void b35_pathRemainder_G12() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				assertEquals("r=abc", proxy(mrc).getRemainder("abc"));
			}
		}

		// ---- G8: dynamic name/value pairs (round-trip) — server echoes the N distinct parts it received ----------
		// Parity: each map/part-list/bean expands to TWO distinct parts (a=1, b=2) rather than one bogus part (G8 closed).

		@Test void b40_dynQueryMap_G8() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				assertEquals("a=1,b=2", proxy(mrc).dynQueryMap(Map.<String,Object>of("a", "1", "b", "2")));
			}
		}

		@Test void b41_dynQueryMapStar_G8() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				assertEquals("a=1,b=2", proxy(mrc).dynQueryMapStar(Map.<String,Object>of("a", "1", "b", "2")));
			}
		}

		@Test void b42_dynQueryPartList_G8() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				assertEquals("a=1,b=2", proxy(mrc).dynQueryPartList(PartList.of(HttpParts.part("a", "1"), HttpParts.part("b", "2"))));
			}
		}

		@Test void b43_dynQueryBean_G8() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				assertEquals("a=1,b=2", proxy(mrc).dynQueryBean(A_PairBean.of("1", "2")));
			}
		}

		@Test void b44_dynHeaderMap_G8() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				assertEquals("a=1,b=2", proxy(mrc).dynHeaderMap(Map.<String,Object>of("a", "1", "b", "2")));
			}
		}

		@Test void b45_dynHeaderMapStar_G8() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				assertEquals("a=1,b=2", proxy(mrc).dynHeaderMapStar(Map.<String,Object>of("a", "1", "b", "2")));
			}
		}

		@Test void b46_dynHeaderList_G8() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				assertEquals("a=1,b=2", proxy(mrc).dynHeaderList(HttpHeaderList.ofPairs("a", "1", "b", "2")));
			}
		}

		@Test void b47_dynHeaderBean_G8() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				assertEquals("a=1,b=2", proxy(mrc).dynHeaderBean(A_PairBean.of("1", "2")));
			}
		}

		@Test void b48_dynFormDataMap_G8() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				assertEquals("a=1,b=2", proxy(mrc).dynFormDataMap(Map.<String,Object>of("a", "1", "b", "2")));
			}
		}

		@Test void b49_dynFormDataMapStar_G8() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				assertEquals("a=1,b=2", proxy(mrc).dynFormDataMapStar(Map.<String,Object>of("a", "1", "b", "2")));
			}
		}
	}

	// =================================================================================================================
	// C — Real-transport column (RestClient over MockHttpTransport stub; asserts serialized request shape)
	// =================================================================================================================

	@Nested class C_RealTransportColumn {

		private RestClient client(List<TransportRequest> captured) {
			return RestClient.builder().transport(captureTransport(captured).build()).rootUrl("http://x.com").build();
		}

		// ---- Active cells -------------------------------------------------------------------------------------------

		@Test void c01_get_F2() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = client(captured)) {
				assertEquals("result", c.remote(A_ParityClient.class).ping());
				assertEquals("GET", captured.get(0).getMethod());
				assertTrue(captured.get(0).getUri().getPath().endsWith("/rest/ping"));
			}
		}

		@Test void c02_post_rawBody_F3() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = client(captured)) {
				c.remote(A_ParityClient.class).postEcho("payload");
				assertEquals("POST", captured.get(0).getMethod());
				assertEquals("payload", readBody(captured.get(0)));
			}
		}

		@Test void c03_put_verb() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = client(captured)) {
				c.remote(A_ParityClient.class).putEcho("payload");
				assertEquals("PUT", captured.get(0).getMethod());
			}
		}

		@Test void c04_patch_F4() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = client(captured)) {
				c.remote(A_ParityClient.class).patchEcho("payload");
				assertEquals("PATCH", captured.get(0).getMethod());
			}
		}

		@Test void c05_delete_verb() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = client(captured)) {
				c.remote(A_ParityClient.class).deleteEcho();
				assertEquals("DELETE", captured.get(0).getMethod());
			}
		}

		@Test void c06_path_F5() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = client(captured)) {
				c.remote(A_ParityClient.class).getInstanceName("na44");
				assertTrue(captured.get(0).getUri().getPath().endsWith("/rest/instances/na44"));
			}
		}

		@Test void c07_pathMulti_F5() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = client(captured)) {
				c.remote(A_ParityClient.class).getInstanceField("na44", "version");
				assertTrue(captured.get(0).getUri().getPath().endsWith("/rest/instances/na44/version"));
			}
		}

		@Test void c08_query_F6_F7_F8_F9() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = client(captured)) {
				c.remote(A_ParityClient.class).query("stmfa", 3, Boolean.TRUE, true, A_InstanceSource.USER);
				var uri = captured.get(0).getUri().toString();
				assertTrue(uri.contains("environment=stmfa"), uri);
				assertTrue(uri.contains("position=3"), uri);
				assertTrue(uri.contains("includeDeleted=true"), uri);
				assertTrue(uri.contains("replace=true"), uri);
				assertTrue(uri.contains("source=USER"), uri);
			}
		}

		@Test void c09_header_F10_F11() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = client(captured)) {
				c.remote(A_ParityClient.class).headers("prod", A_DataFileType.CSV);
				assertEquals("prod", captured.get(0).getFirstHeader("X-Env").value());
				assertEquals("CSV", captured.get(0).getFirstHeader("X-Type").value());
			}
		}

		@Test void c10_transportConfig_F24() throws Exception {
			// F24: classic exposes httpClientConnectionManager(...); the next-gen equivalent is a pluggable HttpTransport.
			var captured = new ArrayList<TransportRequest>();
			try (var c = client(captured)) {
				assertEquals("result", c.remote(A_ParityClient.class).ping());
				assertEquals(1, captured.size());
			}
		}

		// ---- Former gap cells (request-serialization-centric) — now active ----------------------------------------

		@Test void c20_contentBean_serializedRequest_F12() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = client(captured)) {
				var b = new A_Bean(); b.setName("na44");
				c.remote(A_ParityClient.class).postBean(b);
				assertEquals("{\"name\":\"na44\"}", readBody(captured.get(0)));
			}
		}

		@Test void c21_contentReader_streamedRequest_F14() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = client(captured)) {
				c.remote(A_ParityClient.class).postReader(new StringReader("hello"));
				assertEquals("hello", readBody(captured.get(0)));
			}
		}

		// ---- G8: dynamic name/value pairs (request shape) — assert TWO distinct parts are emitted from the map ----

		@Test void c22_dynQueryMap_G8() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = client(captured)) {
				c.remote(A_ParityClient.class).dynQueryMap(Map.<String,Object>of("a", "1", "b", "2"));
				var uri = captured.get(0).getUri().toString();
				assertTrue(uri.contains("a=1") && uri.contains("b=2"), uri);
			}
		}

		@Test void c23_dynQueryMapStar_G8() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = client(captured)) {
				c.remote(A_ParityClient.class).dynQueryMapStar(Map.<String,Object>of("a", "1", "b", "2"));
				var uri = captured.get(0).getUri().toString();
				assertTrue(uri.contains("a=1") && uri.contains("b=2"), uri);
			}
		}

		@Test void c24_dynQueryPartList_G8() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = client(captured)) {
				c.remote(A_ParityClient.class).dynQueryPartList(PartList.of(HttpParts.part("a", "1"), HttpParts.part("b", "2")));
				var uri = captured.get(0).getUri().toString();
				assertTrue(uri.contains("a=1") && uri.contains("b=2"), uri);
			}
		}

		@Test void c25_dynQueryBean_G8() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = client(captured)) {
				c.remote(A_ParityClient.class).dynQueryBean(A_PairBean.of("1", "2"));
				var uri = captured.get(0).getUri().toString();
				assertTrue(uri.contains("a=1") && uri.contains("b=2"), uri);
			}
		}

		@Test void c26_dynHeaderMap_G8() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = client(captured)) {
				c.remote(A_ParityClient.class).dynHeaderMap(Map.<String,Object>of("a", "1", "b", "2"));
				assertEquals("1", captured.get(0).getFirstHeader("a").value());
				assertEquals("2", captured.get(0).getFirstHeader("b").value());
			}
		}

		@Test void c27_dynHeaderMapStar_G8() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = client(captured)) {
				c.remote(A_ParityClient.class).dynHeaderMapStar(Map.<String,Object>of("a", "1", "b", "2"));
				assertEquals("1", captured.get(0).getFirstHeader("a").value());
				assertEquals("2", captured.get(0).getFirstHeader("b").value());
			}
		}

		@Test void c28_dynHeaderList_G8() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = client(captured)) {
				c.remote(A_ParityClient.class).dynHeaderList(HttpHeaderList.ofPairs("a", "1", "b", "2"));
				assertEquals("1", captured.get(0).getFirstHeader("a").value());
				assertEquals("2", captured.get(0).getFirstHeader("b").value());
			}
		}

		@Test void c29_dynHeaderBean_G8() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = client(captured)) {
				c.remote(A_ParityClient.class).dynHeaderBean(A_PairBean.of("1", "2"));
				assertEquals("1", captured.get(0).getFirstHeader("a").value());
				assertEquals("2", captured.get(0).getFirstHeader("b").value());
			}
		}

		@Test void c30_dynFormDataMap_G8() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = client(captured)) {
				c.remote(A_ParityClient.class).dynFormDataMap(Map.<String,Object>of("a", "1", "b", "2"));
				var body = readBody(captured.get(0));
				assertTrue(body != null && body.contains("a=1") && body.contains("b=2"), String.valueOf(body));
			}
		}

		@Test void c31_dynFormDataMapStar_G8() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = client(captured)) {
				c.remote(A_ParityClient.class).dynFormDataMapStar(Map.<String,Object>of("a", "1", "b", "2"));
				var body = readBody(captured.get(0));
				assertTrue(body != null && body.contains("a=1") && body.contains("b=2"), String.valueOf(body));
			}
		}

		@Test void c40_paramDefault_G9() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = client(captured)) {
				c.remote(A_ParityClient.class).withDefault(null);
				assertTrue(captured.get(0).getUri().toString().contains("view="), captured.get(0).getUri().toString());
			}
		}

		@Test void c41_formData_request_G5() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = client(captured)) {
				c.remote(A_ParityClient.class).postForm("v");
				assertTrue(readBody(captured.get(0)).contains("a=v"));
			}
		}

		@Test void c42_asyncReturn_G11() throws Exception {
			// Dedicated transport: the shared capture stub returns the literal body "result" (asserted by c01/c10),
			// which is not valid JSON for List<A_Bean>; this stub returns a parseable list so the async return can be verified.
			var t = MockHttpTransport.builder()
				.fallback(req -> TransportResponse.builder().statusCode(200).body(new ByteArrayInputStream("[{\"name\":\"na44\"}]".getBytes())).build())
				.build();
			try (var c = RestClient.builder().transport(t).rootUrl("http://x.com").build()) {
				var f = c.remote(A_ParityClient.class).getListAsync();
				var list = f.get();
				assertNotNull(list);
				assertEquals(1, list.size());
				assertEquals("na44", list.get(0).getName());
			}
		}

		// ---- G6 (part serializer / @Schema) + G12 (@PathRemainder) — request-shape ---------------------------------

		@Test void c44_partSerializer_collectionFormat_G6() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = client(captured)) {
				c.remote(A_ParityClient.class).getPipes(new String[]{"a", "b", "c"});
				var uri = java.net.URLDecoder.decode(captured.get(0).getUri().toString(), java.nio.charset.StandardCharsets.UTF_8);
				assertTrue(uri.contains("tags=a|b|c"), uri);
			}
		}

		@Test void c45_partSerializer_skipIfEmpty_G6() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = client(captured)) {
				c.remote(A_ParityClient.class).getSkip("", "y");
				var uri = captured.get(0).getUri().toString();
				assertTrue(uri.contains("keep=y"), uri);
				assertFalse(uri.contains("q="), uri);
			}
		}

		@Test void c46_pathRemainder_G12() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = client(captured)) {
				c.remote(A_ParityClient.class).getRemainder("a/b/c");  // multi-segment: slashes preserved
				var path = captured.get(0).getUri().getPath();
				assertTrue(path.endsWith("/rest/remainder/a/b/c"), path);
			}
		}

		@Test void c43_jsonRoundTrip_F23() throws Exception {
			try (var mrc = MockRestClient.create(A_ParityResource.class)) {
				var svc = mrc.getClient().remote(A_ParityClient.class);
				var b = new A_Bean(); b.setName("na44");
				assertTrue(svc.postBean(b).contains("na44"));
				assertEquals("na44", svc.getBean().getName());
			}
		}
	}
}
