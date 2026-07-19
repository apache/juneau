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
import java.nio.charset.*;
import java.nio.file.Files;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.commons.lang.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.Path;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.httppart.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.xml.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;
import org.junit.jupiter.api.io.*;

/**
 * Capability tests for the <b>next-generation</b> REST client engine
 * ({@link RestClient#remote(Class)} &rarr; {@code RemoteClient}) covering the Priority-1 parity gaps:
 *
 * <ul>
 * 	<li>Interface-level constant headers ({@code @Remote(headers=...)}) and client-versioning
 * 		({@code @Remote(version=.../versionHeader=...)} &rarr; {@code Client-Version}).
 * 	<li>Method-level part defaults ({@code @Header/@Query/@FormData/@Path(def=...)} placed on the
 * 		method) filling a {@code null} argument, with the parameter-level {@code def} taking precedence.
 * 	<li>Always-applied constant <i>method-level</i> parts
 * 		({@code @RemoteOp/@RemoteGet/...(headers=..., queryData=..., formData=...)}) emitted on every call with no
 * 		parameter required.
 * 	<li>Always-applied constant <i>interface-level</i> parts
 * 		({@code @Remote(queryData=..., formData=...)}) emitted on every method call.
 * </ul>
 *
 * <p>
 * Constant parse formats follow the {@code @Remote(headers={"Name: value"})} precedent: <b>headers</b> use the
 * colon form ({@code "Name: value"}); <b>queryData</b>/<b>formData</b> use the equals form ({@code "name=value"}).
 * All constant values resolve through {@code VarResolver.DEFAULT} (e.g. {@code "$S{sysprop}"}).  When the same part
 * name is declared at both scopes, the method-level constant wins; a caller-supplied parameter value composes with
 * (does not erase) the constant.  Constant <i>path</i> values are intentionally out of scope (path stays handled by
 * the {@code path} template + {@code @Path} params).
 *
 * <p>
 * These tests pin the desired <i>capability/outcome</i> (the constant/default value reaches the request and is
 * echoed back by the in-JVM mock server), not the classic engine's internal wiring.
 *
 * <p>
 * Per-arg serializer is intentionally not covered here: empirically the classic engine does not honor a per-arg
 * serializer for direct method parameters either (there is no annotation that populates it on the remote path), so it
 * is not a reproducible parity gap — it is the net-new {@code @Schema(serializer=...)} capability.
 */
@SuppressWarnings({
	"java:S114" // Snake_case fixture interface/resource names are intentional test-local naming (matches RemoteProxy_FeatureParity_Test).
})
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class RemoteProxy_NextGenParityGaps_Test {

	// =================================================================================================================
	// Interface-level constant headers + client-versioning
	// =================================================================================================================

	@Rest
	public static class N1_Resource {
		@RestGet("/n1/echo")
		public String echo(@Header("Foo") String foo, @Header("Baz") String baz, @Header("Client-Version") String cv, @Header("X-Ver") String xv) {
			return "Foo=" + foo + ",Baz=" + baz + ",Client-Version=" + cv + ",X-Ver=" + xv;
		}
	}

	/** Constant headers + default client-version header. */
	@Remote(path="/n1", headers={"Foo: bar", "Baz: qux"}, version="1.2.3")
	public interface N1_HeadersClient {
		@RemoteGet("/echo") String echo();
	}

	/** Custom version header name. */
	@Remote(path="/n1", version="2.0.0", versionHeader="X-Ver")
	public interface N1_VersionHeaderClient {
		@RemoteGet("/echo") String echo();
	}

	@Nested class A_InterfaceHeadersAndVersion {

		@Test void a01_constantHeaders_reachRequest() throws Exception {
			try (var mrc = MockRestClient.create(N1_Resource.class)) {
				var p = mrc.getClient().remote(N1_HeadersClient.class);
				assertEquals("Foo=bar,Baz=qux,Client-Version=1.2.3,X-Ver=null", p.echo());
			}
		}

		@Test void a02_customVersionHeader_reachRequest() throws Exception {
			try (var mrc = MockRestClient.create(N1_Resource.class)) {
				var p = mrc.getClient().remote(N1_VersionHeaderClient.class);
				assertEquals("Foo=null,Baz=null,Client-Version=null,X-Ver=2.0.0", p.echo());
			}
		}
	}

	// =================================================================================================================
	// Method-level part defaults (fill a null arg; parameter-level def wins)
	// =================================================================================================================

	@Rest
	public static class N2_Resource {
		@RestGet("/n2/hdr")  public String hdr(@Header("Foo") String foo, @Header("Bar") String bar) { return "Foo=" + foo + ",Bar=" + bar; }
		@RestGet("/n2/qry")  public String qry(@Query("foo") String foo, @Query("bar") String bar) { return "foo=" + foo + ",bar=" + bar; }
		@RestPost("/n2/frm") public String frm(@FormData("foo") String foo, @FormData("bar") String bar) { return "foo=" + foo + ",bar=" + bar; }
		@RestGet("/n2/pth/{foo}/{bar}") public String pth(@Path("foo") String foo, @Path("bar") String bar) { return "foo=" + foo + ",bar=" + bar; }
	}

	@Remote(path="/n2")
	public interface N2_Client {
		@RemoteGet("/hdr")  @Header(name="Foo", def="defFoo")   String hdr(@Header("Foo") String foo, @Header("Bar") String bar);
		@RemoteGet("/qry")  @Query(name="foo", def="defFoo")    String qry(@Query("foo") String foo, @Query("bar") String bar);
		@RemotePost("/frm") @FormData(name="foo", def="defFoo") String frm(@FormData("foo") String foo, @FormData("bar") String bar);
		@RemoteGet("/pth/{foo}/{bar}") @Path(name="foo", def="defFoo") String pth(@Path("foo") String foo, @Path("bar") String bar);

		// Parameter-level def must win over method-level def.
		@RemoteGet("/qry") @Query(name="foo", def="methodDef") String qryPrecedence(@Query(name="foo", def="paramDef") String foo, @Query("bar") String bar);
	}

	@Nested class B_MethodLevelDefaults {

		private N2_Client proxy(MockRestClient mrc) {
			return mrc.getClient().remote(N2_Client.class);
		}

		@Test void b01_headerDefault_fillsNull() throws Exception {
			try (var mrc = MockRestClient.create(N2_Resource.class)) {
				assertEquals("Foo=defFoo,Bar=x", proxy(mrc).hdr(null, "x"));
			}
		}

		@Test void b02_headerDefault_providedValueWins() throws Exception {
			try (var mrc = MockRestClient.create(N2_Resource.class)) {
				assertEquals("Foo=custom,Bar=x", proxy(mrc).hdr("custom", "x"));
			}
		}

		@Test void b03_queryDefault_fillsNull() throws Exception {
			try (var mrc = MockRestClient.create(N2_Resource.class)) {
				assertEquals("foo=defFoo,bar=x", proxy(mrc).qry(null, "x"));
			}
		}

		@Test void b04_formDataDefault_fillsNull() throws Exception {
			try (var mrc = MockRestClient.create(N2_Resource.class)) {
				assertEquals("foo=defFoo,bar=x", proxy(mrc).frm(null, "x"));
			}
		}

		@Test void b05_pathDefault_fillsNull() throws Exception {
			try (var mrc = MockRestClient.create(N2_Resource.class)) {
				assertEquals("foo=defFoo,bar=x", proxy(mrc).pth(null, "x"));
			}
		}

		@Test void b06_parameterDefaultWinsOverMethodDefault() throws Exception {
			try (var mrc = MockRestClient.create(N2_Resource.class)) {
				assertEquals("foo=paramDef,bar=x", proxy(mrc).qryPrecedence(null, "x"));
			}
		}
	}

	// =================================================================================================================
	// Shared capture transport (asserts the serialized request shape the engine emits)
	// =================================================================================================================

	/** Records every request, replies 200/"result" — used to assert query/header/form composition + precedence. */
	private static MockHttpTransport.Builder captureTransport(List<TransportRequest> captured) {
		return MockHttpTransport.builder().fallback(req -> {
			captured.add(req);
			return TransportResponse.builder().statusCode(200).body(new ByteArrayInputStream("result".getBytes())).build();
		});
	}

	private static RestClient captureClient(List<TransportRequest> captured) {
		return RestClient.builder().transport(captureTransport(captured).build()).rootUrl("http://x.com").defaultSerializer(JsonSerializer.DEFAULT).build();
	}

	private static String readBody(TransportRequest req) throws IOException {
		var body = req.getBody();
		if (body == null)
			return null;
		var baos = new ByteArrayOutputStream();
		body.writeTo(baos);
		return baos.toString();
	}

	// =================================================================================================================
	// Interface-level constant query / formData (the header case is already covered above)
	// =================================================================================================================

	@Rest
	public static class C_Resource {
		@RestGet("/c/echo")
		public String echoGet(@Query("iq") String iq, @Header("X-IH") String ih) {
			return "iq=" + iq + ",X-IH=" + ih;
		}
		@RestPost("/c/echo")
		public String echoPost(@FormData("iff") String iff, @Header("X-IH") String ih) {
			return "iff=" + iff + ",X-IH=" + ih;
		}
	}

	/** Interface-level constant query + formData + header (the header overlaps the constant-header case to prove they coexist). */
	@Remote(path="/c", headers={"X-IH: ihval"}, queryData={"iq=ival"}, formData={"iff=ifval"})
	public interface C_IfaceClient {
		@RemoteGet("/echo")  String get();
		@RemotePost("/echo") String post();
	}

	@Nested class C_InterfaceLevelConstants {

		@Test void c01_constantQuery_reachesRequest() throws Exception {
			try (var mrc = MockRestClient.create(C_Resource.class)) {
				assertEquals("iq=ival,X-IH=ihval", mrc.getClient().remote(C_IfaceClient.class).get());
			}
		}

		@Test void c02_constantFormData_reachesRequest() throws Exception {
			try (var mrc = MockRestClient.create(C_Resource.class)) {
				assertEquals("iff=ifval,X-IH=ihval", mrc.getClient().remote(C_IfaceClient.class).post());
			}
		}
	}

	// =================================================================================================================
	// Method-level constant header / query / formData (verb annotations + @RemoteOp)
	// =================================================================================================================

	@Rest
	public static class D_Resource {
		@RestGet("/d/echo")
		public String echoGet(@Header("X-MH") String mh, @Query("mq") String mq) {
			return "X-MH=" + mh + ",mq=" + mq;
		}
		@RestPost("/d/echo")
		public String echoPost(@FormData("mf") String mf, @Header("X-MH") String mh) {
			return "mf=" + mf + ",X-MH=" + mh;
		}
		@RestGet("/d/op")
		public String echoOp(@Header("X-OP") String op, @Query("oq") String oq) {
			return "X-OP=" + op + ",oq=" + oq;
		}
	}

	/** Method-level constants declared on the verb annotations and on {@code @RemoteOp}. */
	@Remote(path="/d")
	public interface D_MethodClient {
		@RemoteGet(path="/echo", headers={"X-MH: mhval"}, queryData={"mq=mqval"}) String get();
		@RemotePost(path="/echo", formData={"mf=mfval"}, headers={"X-MH: mhval"}) String post();
		@RemoteOp(value="GET /op", headers={"X-OP: opval"}, queryData={"oq=oqval"}) String op();
	}

	@Nested class D_MethodLevelConstants {

		@Test void d01_constantHeaderAndQuery_reachRequest() throws Exception {
			try (var mrc = MockRestClient.create(D_Resource.class)) {
				assertEquals("X-MH=mhval,mq=mqval", mrc.getClient().remote(D_MethodClient.class).get());
			}
		}

		@Test void d02_constantFormDataAndHeader_reachRequest() throws Exception {
			try (var mrc = MockRestClient.create(D_Resource.class)) {
				assertEquals("mf=mfval,X-MH=mhval", mrc.getClient().remote(D_MethodClient.class).post());
			}
		}

		@Test void d03_remoteOpConstants_reachRequest() throws Exception {
			try (var mrc = MockRestClient.create(D_Resource.class)) {
				assertEquals("X-OP=opval,oq=oqval", mrc.getClient().remote(D_MethodClient.class).op());
			}
		}
	}

	// =================================================================================================================
	// Precedence (method wins), caller composition (does not erase), and VarResolver expansion
	// =================================================================================================================

	@Rest
	public static class E_Resource {
		@RestGet("/e/echo")
		public String echo(@Query("shared") String shared, @Query("vq") String vq) {
			return "shared=" + shared + ",vq=" + vq;
		}
	}

	/** Same query name at both scopes — method-level constant must win over the interface-level constant. */
	@Remote(path="/e", queryData={"shared=iface"})
	public interface E_PrecedenceClient {
		@RemoteGet(path="/echo", queryData={"shared=method"}) String get();
	}

	/** Constant query plus a caller-supplied parameter of the same name — both must compose. */
	@Remote(path="/e")
	public interface E_ComposeClient {
		@RemoteGet(path="/echo", queryData={"shared=const"}) String get(@Query("shared") String shared);
	}

	/** VarResolver expansion of a {@code $S{...}} system property inside a constant value. */
	@Remote(path="/e", queryData={"vq=$S{RemoteProxy_NextGenParityGaps_Test.e}"})
	public interface E_VarClient {
		@RemoteGet("/echo") String get();
	}

	@Nested class E_PrecedenceCompositionVars {

		@Test void e01_methodLevelWinsOverInterfaceLevel() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = captureClient(captured)) {
				c.remote(E_PrecedenceClient.class).get();
				var uri = captured.get(0).getUri().toString();
				assertTrue(uri.contains("shared=method"), uri);
				assertFalse(uri.contains("shared=iface"), uri);
			}
		}

		@Test void e02_callerValueComposesWithConstant() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = captureClient(captured)) {
				c.remote(E_ComposeClient.class).get("caller");
				var uri = captured.get(0).getUri().toString();
				assertTrue(uri.contains("shared=const"), uri);
				assertTrue(uri.contains("shared=caller"), uri);
			}
		}

		@Test void e03_varResolverExpansion() throws Exception {
			System.setProperty("RemoteProxy_NextGenParityGaps_Test.e", "resolved");
			try (var mrc = MockRestClient.create(E_Resource.class)) {
				assertEquals("shared=null,vq=resolved", mrc.getClient().remote(E_VarClient.class).get());
			} finally {
				System.clearProperty("RemoteProxy_NextGenParityGaps_Test.e");
			}
		}
	}

	// =================================================================================================================
	// Content default and constant body behaviors
	//   (a) parameter-level default fills a null body arg          (mirrors classic i01/i02)
	//   (b) method-level default with a body param fills a null    (mirrors classic e02)
	//   (c) param-less method-level default sends a constant body  (mirrors classic e03)
	//   (d) a caller-supplied body value still wins
	// =================================================================================================================

	@Rest
	public static class F_Resource {
		@RestPost("/f/echo")
		public String echo(@Content String content) {
			return "content=" + content;
		}
	}

	@Remote(path="/f")
	public interface F_ContentClient {
		// (a) parameter-level def fills a null body.
		@RemotePost("/echo") String paramDef(@Content(def="{paramDefault:true}") String content);
		// (b) method-level def fills a null body when the @Content param is null.
		@RemotePost("/echo") @Content(def="{methodDefault:true}") String methodDef(@Content String content);
		// (c) param-less constant body (no @Content parameter at all).
		@RemotePost("/echo") @Content(def="{constant:true}") String constantBody();
	}

	@Nested class F_ContentDefaults {

		private F_ContentClient proxy(MockRestClient mrc) {
			return mrc.getClient().remote(F_ContentClient.class);
		}

		@Test void f01_parameterLevelDef_fillsNullBody() throws Exception {
			try (var mrc = MockRestClient.create(F_Resource.class)) {
				assertEquals("content={paramDefault:true}", proxy(mrc).paramDef(null));
			}
		}

		@Test void f02_providedBodyWinsOverDef() throws Exception {
			try (var mrc = MockRestClient.create(F_Resource.class)) {
				assertEquals("content={custom:true}", proxy(mrc).paramDef("{custom:true}"));
			}
		}

		@Test void f03_methodLevelDef_fillsNullBody() throws Exception {
			try (var mrc = MockRestClient.create(F_Resource.class)) {
				assertEquals("content={methodDefault:true}", proxy(mrc).methodDef(null));
			}
		}

		@Test void f04_paramlessConstantBody_reachesRequest() throws Exception {
			try (var mrc = MockRestClient.create(F_Resource.class)) {
				assertEquals("content={constant:true}", proxy(mrc).constantBody());
			}
		}
	}

	// =================================================================================================================
	// Per-param / per-method / per-interface custom HTTP-part serializer via @HttpPartMarshalling
	//   (a) param-level @HttpPartMarshalling(serializer=...) changes the wire value for @Query/@Header/@Path/@FormData
	//   (b) a method-level default applies when the param declares none
	//   (c) an interface-level default applies when neither param nor method declares one
	//   (d) precedence: param-level › method-level › interface-level
	//   (e) absent annotation → byte-for-byte unchanged (default OpenAPI part serialization)
	//
	// Each custom serializer prefixes the value with a scope tag so the effect is observable on the echoed request.
	// =================================================================================================================

	/** Param-level marker serializer. */
	public static class G_ParamSerializer implements HttpPartSerializer {
		@Override public HttpPartSerializerSession getPartSession() {
			return (type, schema, value) -> "param:" + value;
		}
	}

	/** Method-level marker serializer. */
	public static class G_MethodSerializer implements HttpPartSerializer {
		@Override public HttpPartSerializerSession getPartSession() {
			return (type, schema, value) -> "method:" + value;
		}
	}

	/** Interface-level marker serializer. */
	public static class G_IfaceSerializer implements HttpPartSerializer {
		@Override public HttpPartSerializerSession getPartSession() {
			return (type, schema, value) -> "iface:" + value;
		}
	}

	@Rest
	public static class G_Resource {
		@RestGet("/g/qry")  public String qry(@Query("foo") String foo) { return "foo=" + foo; }
		@RestGet("/g/hdr")  public String hdr(@Header("Foo") String foo) { return "Foo=" + foo; }
		@RestGet("/g/pth/{foo}") public String pth(@Path("foo") String foo) { return "foo=" + foo; }
		@RestPost("/g/frm") public String frm(@FormData("foo") String foo) { return "foo=" + foo; }
	}

	/** Parameter-level serializer on each part type, plus a plain (absent) control. */
	@Remote(path="/g")
	public interface G_ParamClient {
		@RemoteGet("/qry")      String qry(@Query("foo") @HttpPartMarshalling(serializer=G_ParamSerializer.class) String foo);
		@RemoteGet("/hdr")      String hdr(@Header("Foo") @HttpPartMarshalling(serializer=G_ParamSerializer.class) String foo);
		@RemoteGet("/pth/{foo}") String pth(@Path("foo") @HttpPartMarshalling(serializer=G_ParamSerializer.class) String foo);
		@RemotePost("/frm")     String frm(@FormData("foo") @HttpPartMarshalling(serializer=G_ParamSerializer.class) String foo);
		@RemoteGet("/qry")      String plain(@Query("foo") String foo);
	}

	/** Method-level default serializer, and a method where the param-level serializer must win. */
	@Remote(path="/g")
	public interface G_MethodClient {
		@RemoteGet("/qry") @HttpPartMarshalling(serializer=G_MethodSerializer.class) String qry(@Query("foo") String foo);
		@RemoteGet("/qry") @HttpPartMarshalling(serializer=G_MethodSerializer.class) String qryParamWins(@Query("foo") @HttpPartMarshalling(serializer=G_ParamSerializer.class) String foo);
	}

	/** Interface-level default serializer, and a method where the method-level serializer must win. */
	@Remote(path="/g")
	@HttpPartMarshalling(serializer=G_IfaceSerializer.class)
	public interface G_IfaceClient {
		@RemoteGet("/qry") String qry(@Query("foo") String foo);
		@RemoteGet("/qry") @HttpPartMarshalling(serializer=G_MethodSerializer.class) String qryMethodWins(@Query("foo") String foo);
	}

	@Nested class G_PerPartSerializer {

		@Test void g01_paramLevelSerializer_query() throws Exception {
			try (var mrc = MockRestClient.create(G_Resource.class)) {
				assertEquals("foo=param:x", mrc.getClient().remote(G_ParamClient.class).qry("x"));
			}
		}

		@Test void g02_paramLevelSerializer_header() throws Exception {
			try (var mrc = MockRestClient.create(G_Resource.class)) {
				assertEquals("Foo=param:x", mrc.getClient().remote(G_ParamClient.class).hdr("x"));
			}
		}

		@Test void g03_paramLevelSerializer_path() throws Exception {
			try (var mrc = MockRestClient.create(G_Resource.class)) {
				assertEquals("foo=param:x", mrc.getClient().remote(G_ParamClient.class).pth("x"));
			}
		}

		@Test void g04_paramLevelSerializer_formData() throws Exception {
			try (var mrc = MockRestClient.create(G_Resource.class)) {
				assertEquals("foo=param:x", mrc.getClient().remote(G_ParamClient.class).frm("x"));
			}
		}

		@Test void g05_methodLevelDefault_appliesWhenParamHasNone() throws Exception {
			try (var mrc = MockRestClient.create(G_Resource.class)) {
				assertEquals("foo=method:x", mrc.getClient().remote(G_MethodClient.class).qry("x"));
			}
		}

		@Test void g06_interfaceLevelDefault_appliesWhenNoMethodOrParam() throws Exception {
			try (var mrc = MockRestClient.create(G_Resource.class)) {
				assertEquals("foo=iface:x", mrc.getClient().remote(G_IfaceClient.class).qry("x"));
			}
		}

		@Test void g07_paramLevelWinsOverMethodLevel() throws Exception {
			try (var mrc = MockRestClient.create(G_Resource.class)) {
				assertEquals("foo=param:x", mrc.getClient().remote(G_MethodClient.class).qryParamWins("x"));
			}
		}

		@Test void g08_methodLevelWinsOverInterfaceLevel() throws Exception {
			try (var mrc = MockRestClient.create(G_Resource.class)) {
				assertEquals("foo=method:x", mrc.getClient().remote(G_IfaceClient.class).qryMethodWins("x"));
			}
		}

		@Test void g09_absentAnnotation_unchangedDefault() throws Exception {
			try (var mrc = MockRestClient.create(G_Resource.class)) {
				assertEquals("foo=x", mrc.getClient().remote(G_ParamClient.class).plain("x"));
			}
		}
	}

	// =================================================================================================================
	// Next-gen streaming efficiency
	//   - Response InputStream lifecycle — the caller owns the live stream; the connection is released on close,
	//     and the stream stays readable after the proxy method returns (no premature close).
	//   - Reader return type — a lazy reader over the live response body.
	//   - Streaming POJO request body — serialize straight to the wire (no full in-memory materialization),
	//     repeatable (re-serialize on resend).
	//   - Streaming Reader request body — wrap the Reader, do not drain it to a String during request building.
	//
	// These tests pin OUTCOMES: a streamed return survives the method return even when the transport's close callback
	// would close the body; a request body advertises a chunked (-1) length instead of a pre-buffered byte length; an
	// instrumented Reader is never drained eagerly.
	// =================================================================================================================

	/** A response body stream that records close and rejects reads after close (to expose premature-close bugs). */
	private static final class StreamLifecycle_BodyStream extends FilterInputStream {
		boolean closed;
		StreamLifecycle_BodyStream(byte[] payload) { super(new ByteArrayInputStream(payload)); }
		@Override public int read() throws IOException {
			if (closed) throw new IOException("stream read after close");
			return super.read();
		}
		@Override public int read(byte[] b, int off, int len) throws IOException {
			if (closed) throw new IOException("stream read after close");
			return super.read(b, off, len);
		}
		@Override public void close() throws IOException { closed = true; super.close(); }
	}

	/** Builds a single-response client whose transport hands back the given body + a connection-releasing close callback. */
	private static RestClient streamingClient(StreamLifecycle_BodyStream body, Flag releasedOnClose) {
		var transport = MockHttpTransport.builder().fallback(req ->
			TransportResponse.builder()
				.statusCode(200)
				.header("Content-Type", "text/plain")
				.body(body)
				.closeCallback(() -> { releasedOnClose.set(); body.close(); })
				.build()
		).build();
		return RestClient.builder().transport(transport).rootUrl("http://x.com").build();
	}

	private static String drain(Reader r) throws IOException {
		var sb = new StringBuilder();
		var buf = new char[256];
		int n;
		while ((n = r.read(buf)) != -1)
			sb.append(buf, 0, n);
		return sb.toString();
	}

	@Remote
	public interface H_StreamReturnClient {
		@RemoteGet("/stream") InputStream getStream();
		@RemoteGet(path="/resp", returns=RemoteReturn.RESPONSE) RestResponse getResponse();
	}

	@Nested class H_StreamingResponseLifecycle {

		/** The returned InputStream is readable AFTER the proxy method returns — even though the transport's close
		 *  callback would close the body — and the connection is released only when the caller closes the stream. */
		@Test void h01_inputStreamReturn_readableAfterReturn_releasesOnClose() throws Exception {
			var released = Flag.create();
			var body = new StreamLifecycle_BodyStream("STREAMED-RESPONSE-BODY".getBytes(StandardCharsets.UTF_8));
			try (var client = streamingClient(body, released)) {
				var in = client.remote(H_StreamReturnClient.class).getStream();
				assertFalse(released.isSet(), "response closed before caller read the stream");
				assertEquals("STREAMED-RESPONSE-BODY", new String(in.readAllBytes(), StandardCharsets.UTF_8));
				assertFalse(released.isSet(), "connection released before caller closed the stream");
				in.close();
				assertTrue(released.isSet(), "connection not released when caller closed the stream");
			}
		}

		/** RESPONSE-mode parity: the RestResponse body is readable after return and closing releases the connection. */
		@Test void h02_responseModeReturn_readableAfterReturn_releasesOnClose() throws Exception {
			var released = Flag.create();
			var body = new StreamLifecycle_BodyStream("RESPONSE-MODE-BODY".getBytes(StandardCharsets.UTF_8));
			try (var client = streamingClient(body, released)) {
				var resp = client.remote(H_StreamReturnClient.class).getResponse();
				assertFalse(released.isSet());
				assertEquals("RESPONSE-MODE-BODY", resp.getBodyAsString());
				resp.close();
				assertTrue(released.isSet());
			}
		}
	}

	@Remote
	public interface I_ReaderReturnClient {
		@RemoteGet("/reader") Reader getReader();
	}

	@Nested class I_ReaderResponseReturn {

		/** A method declaring a Reader return receives a lazy reader over the live response body. */
		@Test void i01_readerReturn_readableAfterReturn_releasesOnClose() throws Exception {
			var released = Flag.create();
			var body = new StreamLifecycle_BodyStream("READER-RESPONSE-CONTENT".getBytes(StandardCharsets.UTF_8));
			try (var client = streamingClient(body, released)) {
				var r = client.remote(I_ReaderReturnClient.class).getReader();
				assertFalse(released.isSet());
				assertEquals("READER-RESPONSE-CONTENT", drain(r));
				assertFalse(released.isSet());
				r.close();
				assertTrue(released.isSet());
			}
		}
	}

	// Streaming POJO request body.

	public static class J_Bean {
		public String a = "x";
		public int b = 42;
	}

	/** An output stream that records that bytes were written incrementally (evidence of streaming, not pre-buffering). */
	private static final class J_RecordingOutputStream extends ByteArrayOutputStream {
		int writeCalls;
		@Override public synchronized void write(int b) { writeCalls++; super.write(b); }
		@Override public synchronized void write(byte[] b, int off, int len) { writeCalls++; super.write(b, off, len); }
	}

	@Nested class J_StreamingPojoRequestBody {

		/** A POJO body is handed to the transport as a streaming body (chunked length -1), not pre-buffered. */
		@Test void j01_pojoBody_streamsNotBuffered() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = captureClient(captured)) {
				c.post("/x").body(new J_Bean()).run();
			}
			var tb = captured.get(0).getBody();
			// A streaming serializer body reports an unknown length (-1 → chunked); the old pre-buffered StringBody
			// reported its exact byte length.
			assertEquals(-1, tb.getContentLength(), "POJO body was pre-buffered (non-streaming) — expected chunked length -1");
		}

		/** The streaming POJO body is repeatable (re-serializes deterministically) and writes straight to the stream. */
		@Test void j02_pojoBody_isRepeatable_reserializes() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = captureClient(captured)) {
				c.post("/x").body(new J_Bean()).run();
			}
			var tb = captured.get(0).getBody();
			assertTrue(tb.isRepeatable(), "streaming POJO body must be repeatable (re-serialize on retry)");
			var expected = (String) JsonSerializer.DEFAULT.write(new J_Bean());
			var first = new J_RecordingOutputStream();
			tb.writeTo(first);
			var second = new J_RecordingOutputStream();
			tb.writeTo(second);
			assertEquals(expected, first.toString(StandardCharsets.UTF_8));
			assertEquals(expected, second.toString(StandardCharsets.UTF_8));
			assertTrue(first.writeCalls > 0, "serializer did not write directly to the supplied stream");
		}
	}

	// Streaming Reader request body.

	/** A reader that records how many characters were read (to prove the engine does not drain it to a String). */
	private static final class K_CountingReader extends FilterReader {
		int charsRead;
		K_CountingReader(String content) { super(new StringReader(content)); }
		@Override public int read() throws IOException {
			var c = super.read();
			if (c != -1) charsRead++;
			return c;
		}
		@Override public int read(char[] cbuf, int off, int len) throws IOException {
			var n = super.read(cbuf, off, len);
			if (n > 0) charsRead += n;
			return n;
		}
	}

	@Remote(path="/f")
	public interface K_ReaderBodyClient {
		@RemotePost("/echo") String echo(@Content Reader content);
	}

	@Nested class K_StreamingReaderRequestBody {

		/** A Reader @Content is wrapped in a streaming body and NOT drained during request building. */
		@Test void k01_readerBody_notDrainedEagerly() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			var reader = new K_CountingReader("reader-streamed-payload");
			try (var c = captureClient(captured)) {
				c.remote(K_ReaderBodyClient.class).echo(reader);
			}
			// The capture transport never writes the request body, so a lazily-wrapped reader is never read.
			assertEquals(0, reader.charsRead, "Reader @Content was drained to a String during request building");
			assertEquals(-1, captured.get(0).getBody().getContentLength(), "Reader body was not streamed (chunked length -1 expected)");
		}

		/** The reader content is transmitted correctly end-to-end (no regression vs the old drain-to-String path). */
		@Test void k02_readerBody_contentTransmitted() throws Exception {
			try (var mrc = MockRestClient.create(F_Resource.class)) {
				var p = mrc.getClient().remote(K_ReaderBodyClient.class);
				assertEquals("content=reader-streamed-payload", p.echo(new StringReader("reader-streamed-payload")));
			}
		}
	}

	// =================================================================================================================
	// Annotation-level interceptors
	//   (a) an interface-level + a method-level interceptor both fire (onInit), composed with a builder interceptor
	//   (b) documented order: builder → interface → method (method runs closest to the call)
	// =================================================================================================================

	/** Shared firing log for the interceptor-order test. */
	static final List<String> L_FIRED = new ArrayList<>();

	/** Interface-level interceptor (records "iface" on init). */
	public static class L_IfaceInterceptor implements RestCallInterceptor {
		@Override public void onInit(RestRequest req) { L_FIRED.add("iface"); }
	}

	/** Method-level interceptor (records "method" on init). */
	public static class L_MethodInterceptor implements RestCallInterceptor {
		@Override public void onInit(RestRequest req) { L_FIRED.add("method"); }
	}

	@Remote(path="/l", interceptors=L_IfaceInterceptor.class)
	public interface L_InterceptorClient {
		@RemoteGet(path="/echo", interceptors=L_MethodInterceptor.class) String get();
	}

	@Nested class L_AnnotationInterceptors {

		/** A 200-replying client carrying a builder-level interceptor that records "builder" on init. */
		private RestClient builderInterceptorClient() {
			var transport = MockHttpTransport.builder().fallback(req ->
				TransportResponse.builder().statusCode(200).body(new ByteArrayInputStream("ok".getBytes())).build()
			).build();
			RestCallInterceptor builder = new RestCallInterceptor() {
				@Override public void onInit(RestRequest req) { L_FIRED.add("builder"); }
			};
			return RestClient.builder().transport(transport).rootUrl("http://x.com").interceptors(builder).build();
		}

		@Test void l01_interfaceAndMethodInterceptors_fireInOrder() throws Exception {
			L_FIRED.clear();
			try (var c = builderInterceptorClient()) {
				assertEquals("ok", c.remote(L_InterceptorClient.class).get());
			}
			// Union order: builder → interface → method (method closest to the call).
			assertEquals(L_FIRED, List.of("builder", "iface", "method"));
		}
	}

	// =================================================================================================================
	// Per-call response timeout
	//   (a) a method-level timeout("...") reaches the transport request
	//   (b) method-level overrides the interface-level default
	//   (c) absent at both levels → no timeout
	// =================================================================================================================

	@Remote(path="/m", timeout="5s")
	public interface M_TimeoutClient {
		@RemoteGet("/a") String ifaceTimeout();
		@RemoteGet(path="/b", timeout="50ms") String methodTimeout();
	}

	@Remote(path="/m")
	public interface M_NoTimeoutClient {
		@RemoteGet("/c") String none();
	}

	@Nested class M_PerCallTimeout {

		@Test void m01_methodTimeout_overridesInterface() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = captureClient(captured)) {
				c.remote(M_TimeoutClient.class).methodTimeout();
			}
			assertEquals(Duration.ofMillis(50), captured.get(0).getTimeout());
		}

		@Test void m02_interfaceTimeout_appliedWhenMethodSilent() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = captureClient(captured)) {
				c.remote(M_TimeoutClient.class).ifaceTimeout();
			}
			assertEquals(Duration.ofSeconds(5), captured.get(0).getTimeout());
		}

		@Test void m03_noTimeout_whenAbsentAtBothLevels() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = captureClient(captured)) {
				c.remote(M_NoTimeoutClient.class).none();
			}
			assertNull(captured.get(0).getTimeout());
		}
	}

	// =================================================================================================================
	// Safe retries
	//   (a) a GET retries on a retryable status (503) up to the cap then succeeds
	//   (b) a connection failure (TransportException) is retried, then exhausts the cap and propagates
	//   (c) a POST is NOT auto-retried (idempotency gating) unless retryNonIdempotent=true
	//   (d) a non-repeatable body (Reader) is NEVER retried even with opt-in
	//   (e) RESPONSE (live stream) and Future return modes are NOT retried
	// =================================================================================================================

	/** Records each request and replies with a programmable status/exception sequence. */
	private static RestClient retryClient(AtomicInteger calls, MockHttpTransport.RequestHandler handler) {
		var transport = MockHttpTransport.builder().fallback(req -> {
			calls.incrementAndGet();
			return handler.handle(req);
		}).build();
		return RestClient.builder().transport(transport).rootUrl("http://x.com").build();
	}

	private static TransportResponse ok200() {
		return TransportResponse.builder().statusCode(200).body(new ByteArrayInputStream("ok".getBytes())).build();
	}

	private static TransportResponse status(int sc) {
		return TransportResponse.builder().statusCode(sc).body(new ByteArrayInputStream("fail".getBytes())).build();
	}

	@Remote(path="/n")
	public interface N_RetryClient {
		@RemoteGet(path="/get", retries=3) String get();
		@RemotePost(path="/post", retries=3) String post(@Content String body);
		@RemotePost(path="/post", retries=3, retryNonIdempotent=true) String postOptIn(@Content String body);
		@RemotePost(path="/post", retries=3, retryNonIdempotent=true) String postReader(@Content Reader body);
		@RemoteGet(path="/get", retries=3, returns=RemoteReturn.RESPONSE) RestResponse getResponse();
		@RemoteGet(path="/get", retries=3) CompletableFuture<String> getFuture();
	}

	@Nested class N_SafeRetries {

		@Test void n01_get_retriesOn503_thenSucceeds() throws Exception {
			var calls = new AtomicInteger();
			try (var c = retryClient(calls, req -> calls.get() <= 2 ? status(503) : ok200())) {
				assertEquals("ok", c.remote(N_RetryClient.class).get());
			}
			assertEquals(3, calls.get());
		}

		@Test void n02_get_connectionFailure_retriedThenExhausts() throws Exception {
			var calls = new AtomicInteger();
			try (var c = retryClient(calls, req -> { throw new TransportException("boom"); })) {
				// The checked TransportException propagates out of the proxy wrapped in an UndeclaredThrowableException
				// (standard JDK dynamic-proxy behavior for a checked exception not declared by the method).
				var proxy = c.remote(N_RetryClient.class);
				var e = assertThrows(java.lang.reflect.UndeclaredThrowableException.class, proxy::get);
				assertInstanceOf(TransportException.class, e.getCause());
			}
			assertEquals(4, calls.get());  // 1 initial + 3 retries
		}

		@Test void n03_post_notAutoRetried_withoutOptIn() throws Exception {
			var calls = new AtomicInteger();
			try (var c = retryClient(calls, req -> status(503))) {
				c.remote(N_RetryClient.class).post("x");
			}
			assertEquals(1, calls.get());
		}

		@Test void n04_post_retriedWithOptIn_repeatableBody() throws Exception {
			var calls = new AtomicInteger();
			try (var c = retryClient(calls, req -> calls.get() <= 2 ? status(503) : ok200())) {
				assertEquals("ok", c.remote(N_RetryClient.class).postOptIn("x"));
			}
			assertEquals(3, calls.get());
		}

		@Test void n05_post_nonRepeatableBody_notRetried() throws Exception {
			var calls = new AtomicInteger();
			try (var c = retryClient(calls, req -> status(503))) {
				c.remote(N_RetryClient.class).postReader(new StringReader("x"));
			}
			assertEquals(1, calls.get());
		}

		@Test void n06_responseMode_notRetried() throws Exception {
			var calls = new AtomicInteger();
			try (var c = retryClient(calls, req -> status(503))) {
				try (var resp = c.remote(N_RetryClient.class).getResponse()) {
					assertEquals(503, resp.getStatusCode());
				}
			}
			assertEquals(1, calls.get());
		}

		@Test void n07_futureMode_notRetried() throws Exception {
			var calls = new AtomicInteger();
			try (var c = retryClient(calls, req -> status(503))) {
				c.remote(N_RetryClient.class).getFuture();
			}
			assertEquals(1, calls.get());
		}
	}

	// =================================================================================================================
	// throwOnError
	//   (a) a 4xx with no matching throws type throws a generic exception when throwOnError=true
	//   (b) the same call returns the body normally when throwOnError=false
	//   (c) a declared typed exception still wins over the generic throwOnError exception
	// =================================================================================================================

	private static RestClient status404Client() {
		var transport = MockHttpTransport.builder().fallback(req ->
			TransportResponse.builder().statusCode(404).body(new ByteArrayInputStream("nope".getBytes())).build()
		).build();
		return RestClient.builder().transport(transport).rootUrl("http://x.com").build();
	}

	@Remote(path="/o")
	public interface O_ThrowOnErrorClient {
		@RemoteGet(path="/x", throwOnError=true) String throwOn();
		@RemoteGet(path="/x") String noThrow();
		@RemoteGet(path="/x", throwOnError=true) String typedWins() throws NotFound;
	}

	@Nested class O_ThrowOnError {

		@Test void o01_throwsGenericException_whenEnabled() throws Exception {
			try (var c = status404Client()) {
				var p = c.remote(O_ThrowOnErrorClient.class);
				var e = assertThrows(BasicHttpException.class, p::throwOn);
				assertEquals(404, e.getStatusCode());
			}
		}

		@Test void o02_returnsBody_whenDisabled() throws Exception {
			try (var c = status404Client()) {
				assertEquals("nope", c.remote(O_ThrowOnErrorClient.class).noThrow());
			}
		}

		@Test void o03_typedThrowsWins_overGeneric() throws Exception {
			try (var c = status404Client()) {
				var p = c.remote(O_ThrowOnErrorClient.class);
				assertThrows(NotFound.class, p::typedWins);
			}
		}
	}

	// =================================================================================================================
	// Option A: @Url parameter (Retrofit-style call-time URL override)
	//   (a) absolute @Url replaces the whole computed endpoint and bypasses rootUrl (different host)
	//   (b) relative @Url resolves against rootUrl only (NOT the interface @Remote(path)) — endpoint replacement
	//   (c) @Url with {var} + @Path fills tokens inside the caller-supplied URL
	//   (d) @Url wins over @RemoteOp(path=...)
	//   (e) null/blank @Url → clear IllegalArgumentException; two @Url params → build-time error
	//   (f) SSRF guardrail: a non-http(s) scheme in @Url is rejected
	// =================================================================================================================

	@Remote(path="/p")
	public interface P_UrlParamClient {
		@RemoteGet("/method") String abs(@Url String url);
		@RemoteGet("/method") String rel(@Url String url);
		@RemoteGet("/u/{id}") String withVar(@Url String url, @Path("id") String id);
		@RemoteOp("GET /baked") String wins(@Url String url);
		@RemoteGet("/method") String req(@Url String url);
	}

	@Remote(path="/p2")
	public interface P_TwoUrlClient {
		@RemoteGet("/m") String twoUrls(@Url String a, @Url String b);
	}

	@Nested class P_UrlParameter {

		@Test void p01_absoluteUrl_replacesEndpoint_bypassesRootUrl() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = captureClient(captured)) {
				c.remote(P_UrlParamClient.class).abs("http://other.com/abs");
			}
			assertEquals("http://other.com/abs", captured.get(0).getUri().toString());
		}

		@Test void p02_relativeUrl_resolvesAgainstRootUrl_notInterfacePath() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = captureClient(captured)) {
				c.remote(P_UrlParamClient.class).rel("/override");
			}
			var uri = captured.get(0).getUri().toString();
			assertEquals("http://x.com/override", uri);
			assertFalse(uri.contains("/p"), uri);
			assertFalse(uri.contains("/method"), uri);
		}

		@Test void p03_urlWithVar_filledByPathParam() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = captureClient(captured)) {
				c.remote(P_UrlParamClient.class).withVar("http://other.com/u/{id}", "42");
			}
			assertEquals("http://other.com/u/42", captured.get(0).getUri().toString());
		}

		@Test void p04_urlParam_winsOverRemoteOpPath() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = captureClient(captured)) {
				c.remote(P_UrlParamClient.class).wins("http://other.com/win");
			}
			var uri = captured.get(0).getUri().toString();
			assertEquals("http://other.com/win", uri);
			assertFalse(uri.contains("baked"), uri);
		}

		@Test void p05_nullOrBlankUrl_rejected() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = captureClient(captured)) {
				var p = c.remote(P_UrlParamClient.class);
				assertThrows(IllegalArgumentException.class, () -> p.req(null));
				assertThrows(IllegalArgumentException.class, () -> p.req("   "));
			}
		}

		@Test void p06_nonHttpScheme_rejected() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = captureClient(captured)) {
				var p = c.remote(P_UrlParamClient.class);
				assertThrows(IllegalArgumentException.class, () -> p.req("file:///etc/passwd"));
				assertThrows(IllegalArgumentException.class, () -> p.req("gopher://evil/x"));
			}
		}

		@Test void p07_twoUrlParams_buildTimeError() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = captureClient(captured)) {
				assertThrows(IllegalArgumentException.class, () -> c.remote(P_TwoUrlClient.class));
			}
		}
	}

	// =================================================================================================================
	// Option B: declarative baseUrl override on @Remote / @RemoteOp / verb annotations
	//   (a) method-level baseUrl overrides interface-level baseUrl; method path + templating preserved
	//   (b) interface-level baseUrl applied when the method declares none
	//   (c) @Url param beats baseUrl
	//   (d) absent @Url/baseUrl → existing rootUrl + path/templating behavior unchanged (no regression)
	//   (e) baseUrl yielding a non-http(s) scheme is rejected (SSRF guardrail)
	//   (f) baseUrl resolves $S{...} via VarResolver.DEFAULT
	// =================================================================================================================

	@Remote(path="/qbase", baseUrl="http://iface.example")
	public interface Q_BaseUrlClient {
		@RemoteGet(path="/m/{id}", baseUrl="http://method.example") String methodOverride(@Path("id") String id);
		@RemoteGet("/m") String ifaceOverride();
		@RemoteGet(path="/m", baseUrl="http://method.example") String urlWins(@Url String url);
	}

	@Remote(path="/qbase")
	public interface Q_NoOverrideClient {
		@RemoteGet("/m/{id}") String normal(@Path("id") String id);
	}

	@Remote(path="/qbase", baseUrl="file://bad")
	public interface Q_BadSchemeClient {
		@RemoteGet("/m") String get();
	}

	@Remote(path="/qbase", baseUrl="$S{RemoteProxy_NextGenParityGaps_Test.q}")
	public interface Q_VarBaseUrlClient {
		@RemoteGet("/m") String get();
	}

	@Nested class Q_BaseUrlOverride {

		@Test void q01_methodBaseUrl_overridesInterface_preservesPath() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = captureClient(captured)) {
				c.remote(Q_BaseUrlClient.class).methodOverride("42");
			}
			assertEquals("http://method.example/qbase/m/42", captured.get(0).getUri().toString());
		}

		@Test void q02_interfaceBaseUrl_appliedWhenMethodSilent() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = captureClient(captured)) {
				c.remote(Q_BaseUrlClient.class).ifaceOverride();
			}
			assertEquals("http://iface.example/qbase/m", captured.get(0).getUri().toString());
		}

		@Test void q03_urlParam_beatsBaseUrl() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = captureClient(captured)) {
				c.remote(Q_BaseUrlClient.class).urlWins("http://other.com/z");
			}
			assertEquals("http://other.com/z", captured.get(0).getUri().toString());
		}

		@Test void q04_noOverride_unchangedRootUrlBehavior() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = captureClient(captured)) {
				c.remote(Q_NoOverrideClient.class).normal("42");
			}
			assertEquals("http://x.com/qbase/m/42", captured.get(0).getUri().toString());
		}

		@Test void q05_baseUrlNonHttpScheme_rejected() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = captureClient(captured)) {
				var p = c.remote(Q_BadSchemeClient.class);
				assertThrows(IllegalArgumentException.class, p::get);
			}
		}

		@Test void q06_baseUrl_varResolverExpansion() throws Exception {
			System.setProperty("RemoteProxy_NextGenParityGaps_Test.q", "http://resolved.example");
			var captured = new ArrayList<TransportRequest>();
			try (var c = captureClient(captured)) {
				c.remote(Q_VarBaseUrlClient.class).get();
				assertEquals("http://resolved.example/qbase/m", captured.get(0).getUri().toString());
			} finally {
				System.clearProperty("RemoteProxy_NextGenParityGaps_Test.q");
			}
		}
	}

	// =================================================================================================================
	// Option A: declarative multipart/file-upload (@Multipart method marker + @Part parameters)
	//   The engine adapts @Part-annotated parameters into the existing streaming RFC-7578 MultipartBody.  Accepted
	//   part-source types: String/CharSequence (and other scalars), byte[], File, InputStream, Reader, and beans
	//   (serialized via the client's default serializer).  Each @Part carries an optional name/fileName/contentType.
	//   File/InputStream/Reader/byte[] parts STREAM (reuse the streaming bodies); they are never pre-buffered.
	// =================================================================================================================

	public static class R_Bean {
		public String a = "x";
		public int b = 42;
	}

	/** Reads the raw request body (as a Reader, bypassing content-type parser negotiation) so the test can assert
	 *  the multipart wire the engine emitted. */
	@Rest
	public static class R_Resource {
		@RestPost("/r/echo")
		public String echo(@Content Reader content) throws IOException {
			return drain(content);
		}
	}

	@Remote(path="/r")
	public interface R_MultipartClient {
		// Mixed: a text field + a byte[] part carrying an explicit filename + content-type.
		@RemotePost("/echo") @Multipart String mixed(
			@Part("title") String title,
			@Part(name="attachment", fileName="data.bin", contentType="application/octet-stream") byte[] data);

		// File part: filename defaults to the file's name; content-type is explicit.
		@RemotePost("/echo") @Multipart String fileUpload(@Part(name="file", contentType="application/pdf") File file);

		// One-shot InputStream part (must stream, not buffer).
		@RemotePost("/echo") @Multipart String streamUpload(@Part(name="upload", fileName="s.bin", contentType="application/octet-stream") InputStream in);

		// One-shot Reader part (must not be drained during request building).
		@RemotePost("/echo") @Multipart String readerUpload(@Part(name="notes", contentType="text/plain") Reader notes);

		// Bean part: serialized via the client's default serializer (JSON).
		@RemotePost("/echo") @Multipart String beanPart(@Part(name="meta", contentType="application/json") R_Bean bean);
	}

	@Nested class R_MultipartParts {

		@Test void r01_textAndBytePart_receivedByResource() throws Exception {
			try (var mrc = MockRestClient.create(R_Resource.class)) {
				var wire = mrc.getClient().remote(R_MultipartClient.class).mixed("My Report", "PDF-CONTENT".getBytes(StandardCharsets.UTF_8));
				assertTrue(wire.contains("Content-Disposition: form-data; name=\"title\""), wire);
				assertTrue(wire.contains("My Report"), wire);
				assertTrue(wire.contains("name=\"attachment\""), wire);
				assertTrue(wire.contains("filename=\"data.bin\""), wire);
				assertTrue(wire.contains("Content-Type: application/octet-stream"), wire);
				assertTrue(wire.contains("PDF-CONTENT"), wire);
			}
		}

		@Test void r02_multipartContentType_carriesBoundary() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = captureClient(captured)) {
				c.remote(R_MultipartClient.class).mixed("t", "b".getBytes(StandardCharsets.UTF_8));
			}
			var ct = captured.get(0).getBody().getContentType();
			assertTrue(ct != null && ct.startsWith("multipart/form-data; boundary="), String.valueOf(ct));
		}

		@Test void r03_filePart_streamsAndReceived(@TempDir File dir) throws Exception {
			var f = new File(dir, "report.pdf");
			Files.writeString(f.toPath(), "FILE-PAYLOAD");
			try (var mrc = MockRestClient.create(R_Resource.class)) {
				var wire = mrc.getClient().remote(R_MultipartClient.class).fileUpload(f);
				assertTrue(wire.contains("name=\"file\""), wire);
				assertTrue(wire.contains("filename=\"report.pdf\""), wire);
				assertTrue(wire.contains("Content-Type: application/pdf"), wire);
				assertTrue(wire.contains("FILE-PAYLOAD"), wire);
			}
		}

		@Test void r04_inputStreamPart_streamsNotBuffered() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = captureClient(captured)) {
				c.remote(R_MultipartClient.class).streamUpload(new ByteArrayInputStream("STREAMED".getBytes(StandardCharsets.UTF_8)));
			}
			// A one-shot InputStream part makes the multipart body non-repeatable — evidence the engine wraps it in a
			// streaming body (StreamBody) instead of pre-buffering it into a repeatable byte[] part.
			assertFalse(captured.get(0).getBody().isRepeatable(),
				"InputStream @Part was pre-buffered (repeatable) — expected a streaming, non-repeatable multipart body");
		}

		@Test void r05_readerPart_notDrainedEagerly() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			var reader = new K_CountingReader("reader-multipart-payload");
			try (var c = captureClient(captured)) {
				c.remote(R_MultipartClient.class).readerUpload(reader);
			}
			// The capture transport never writes the body, so a lazily-wrapped reader is never read.
			assertEquals(0, reader.charsRead, "Reader @Part was drained during request building");
			assertFalse(captured.get(0).getBody().isRepeatable(), "Reader @Part must produce a non-repeatable streaming body");
		}

		@Test void r06_beanPart_serializedAsJson() throws Exception {
			try (var mrc = MockRestClient.create(R_Resource.class)) {
				var wire = mrc.getClient().remote(R_MultipartClient.class).beanPart(new R_Bean());
				assertTrue(wire.contains("name=\"meta\""), wire);
				assertTrue(wire.contains("Content-Type: application/json"), wire);
				assertTrue(wire.contains("\"a\":\"x\""), wire);
				assertTrue(wire.contains("\"b\":42"), wire);
			}
		}
	}

	// =================================================================================================================
	// Body-mode exclusivity + misuse errors, and Option B (user-built MultipartBody) regression
	// =================================================================================================================

	@Rest
	public static class S_Resource {
		@RestPost("/s/echo")
		public String echo(@Content Reader content) throws IOException {
			return drain(content);
		}
	}

	/** Conflict: a method is either multipart OR single-@Content, never both. */
	@Remote(path="/s")
	public interface S_ConflictClient {
		@RemotePost("/echo") @Multipart String bad(@Part("a") String a, @Content String body);
	}

	/** Misuse: @Multipart with no @Part parameters. */
	@Remote(path="/s")
	public interface S_NoPartClient {
		@RemotePost("/echo") @Multipart String bad(@Query("a") String a);
	}

	/** Misuse: @Part parameter without a method-level @Multipart marker. */
	@Remote(path="/s")
	public interface S_PartWithoutMultipartClient {
		@RemotePost("/echo") String bad(@Part("a") String a);
	}

	/** Option B (escape hatch): pass a user-built MultipartBody straight through as @Content. */
	@Remote(path="/s")
	public interface S_OptionBClient {
		@RemotePost("/echo") String userBuilt(@Content MultipartBody body);
	}

	@Nested class S_ExclusivityAndOptionB {

		@Test void s01_multipartPlusContent_buildTimeError() throws Exception {
			try (var mrc = MockRestClient.create(S_Resource.class)) {
				var client = mrc.getClient();
				assertThrows(IllegalArgumentException.class, () -> client.remote(S_ConflictClient.class));
			}
		}

		@Test void s02_multipartWithNoPart_buildTimeError() throws Exception {
			try (var mrc = MockRestClient.create(S_Resource.class)) {
				var client = mrc.getClient();
				assertThrows(IllegalArgumentException.class, () -> client.remote(S_NoPartClient.class));
			}
		}

		@Test void s03_partWithoutMultipart_buildTimeError() throws Exception {
			try (var mrc = MockRestClient.create(S_Resource.class)) {
				var client = mrc.getClient();
				assertThrows(IllegalArgumentException.class, () -> client.remote(S_PartWithoutMultipartClient.class));
			}
		}

		@Test void s04_optionB_userBuiltMultipartBody_stillWorks() throws Exception {
			try (var mrc = MockRestClient.create(S_Resource.class)) {
				var body = MultipartBody.builder()
					.boundary("optb123")
					.field("title", "Hand Built")
					.part(MultipartBody.MultipartPart.of("notes", null, "text/plain", StringBody.of("manual")))
					.build();
				var wire = mrc.getClient().remote(S_OptionBClient.class).userBuilt(body);
				assertTrue(wire.contains("--optb123"), wire);
				assertTrue(wire.contains("name=\"title\""), wire);
				assertTrue(wire.contains("Hand Built"), wire);
				assertTrue(wire.contains("manual"), wire);
			}
		}
	}

	// =================================================================================================================
	// Per-method content negotiation (accept / contentType attributes)
	//   contentType SELECTS the request serializer + sets the Content-Type label (single header, no duplicate);
	//   accept sets the Accept header and is the response-parser FALLBACK (response Content-Type stays authoritative).
	//   No-match media types fall back to the default marshaller but still send the overridden label.
	//   Method-level overrides interface-level; the dedicated attribute wins over a constant header.
	// =================================================================================================================

	public static class T8_Bean {
		public String name;
		public int age;
		public T8_Bean() {}
		public T8_Bean(String name, int age) { this.name = name; this.age = age; }
	}

	/** A serializer set with JSON (default) + an XML serializer that matches/accepts {@code application/xml}. */
	private static SerializerSet jsonXmlSerializers() {
		return SerializerSet.create().add(JsonSerializer.DEFAULT, XmlSerializer.create().accept("application/xml").build()).build();
	}

	/** A parser set with JSON (default) + an XML parser that consumes {@code application/xml}. */
	private static ParserSet jsonXmlParsers() {
		return ParserSet.create().add(JsonParser.DEFAULT, XmlParser.create().consumes("application/xml").build()).build();
	}

	private static RestClient captureClient(List<TransportRequest> captured, SerializerSet serializers, ParserSet parsers) {
		var b = RestClient.builder().transport(captureTransport(captured).build()).rootUrl("http://x.com").defaultSerializer(JsonSerializer.DEFAULT);
		if (serializers != null)
			b.serializers(serializers);
		if (parsers != null)
			b.parsers(parsers);
		return b.build();
	}

	/** Records every request AND replies 200 with the given body + (optional) Content-Type, for parser-fallback tests. */
	private static MockHttpTransport.Builder captureAndRespond(List<TransportRequest> captured, String body, String contentType) {
		return MockHttpTransport.builder().fallback(req -> {
			captured.add(req);
			var rb = TransportResponse.builder().statusCode(200).body(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
			if (contentType != null)
				rb.header("Content-Type", contentType);
			return rb.build();
		});
	}

	private static int countHeaders(TransportRequest req, String name) {
		return (int) req.getHeaders().stream().filter(h -> h.name().equalsIgnoreCase(name)).count();
	}

	private static String headerValue(TransportRequest req, String name) {
		var h = req.getFirstHeader(name);
		return h == null ? null : h.value();
	}

	// --- contentType (request serializer selection + Content-Type replacement) ---------------------------------------

	@Remote(path="/t")
	public interface T_ContentTypeClient {
		// Selects the registered XML serializer; body becomes XML, Content-Type=application/xml (single header).
		@RemotePost(path="/echo", contentType="application/xml") String xml(@Content T8_Bean bean);
		// Vendor media type with no matching serializer: default (JSON) bytes, but the vendor label is still sent.
		@RemotePost(path="/echo", contentType="application/vnd.foo+json") String vendor(@Content T8_Bean bean);
		// No attribute: existing default behavior (JSON), single Content-Type.
		@RemotePost("/echo") String plain(@Content T8_Bean bean);
	}

	@Nested class T_ContentTypeSelection {

		@Test void t01_contentType_selectsXmlSerializer_singleHeader() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = captureClient(captured, jsonXmlSerializers(), null)) {
				c.remote(T_ContentTypeClient.class).xml(new T8_Bean("Bob", 30));
			}
			var req = captured.get(0);
			var body = readBody(req);
			assertTrue(body.contains("<") && body.contains("Bob"), body);          // XML, not JSON
			assertFalse(body.trim().startsWith("{"), body);
			assertEquals(1, countHeaders(req, "Content-Type"));
			assertEquals("application/xml", headerValue(req, "Content-Type"));
		}

		@Test void t02_noMatchVendorType_fallsBackToDefault_butSendsLabel() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			// JSON-only client: application/vnd.foo+json matches no serializer → default (JSON) bytes, vendor label.
			try (var c = captureClient(captured, null, null)) {
				c.remote(T_ContentTypeClient.class).vendor(new T8_Bean("Bob", 30));
			}
			var req = captured.get(0);
			assertTrue(readBody(req).trim().startsWith("{"), readBody(req));        // JSON (default serializer)
			assertEquals(1, countHeaders(req, "Content-Type"));
			assertEquals("application/vnd.foo+json", headerValue(req, "Content-Type"));
		}

		@Test void t03_absentAttribute_defaultJson_singleHeader() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = captureClient(captured, null, null)) {
				c.remote(T_ContentTypeClient.class).plain(new T8_Bean("Bob", 30));
			}
			var req = captured.get(0);
			assertTrue(readBody(req).trim().startsWith("{"));
			assertEquals(1, countHeaders(req, "Content-Type"));
			assertEquals("application/json", headerValue(req, "Content-Type"));
		}
	}

	// --- accept (Accept header + response-parser fallback) -----------------------------------------------------------

	@Remote(path="/u")
	public interface U_AcceptClient {
		@RemoteGet(path="/echo", accept="application/xml") T8_Bean get();
	}

	@Remote(path="/u", accept="application/json")
	public interface U_LevelClient {
		@RemoteGet(path="/echo", accept="application/xml") T8_Bean methodWins();
		@RemoteGet("/echo") T8_Bean ifaceDefault();
	}

	@Nested class U_Accept {

		@Test void u01_acceptHeaderSent_and_responseParsedByContentType() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			var xmlBody = (String) XmlSerializer.DEFAULT.write(new T8_Bean("Bob", 30));
			try (var c = RestClient.builder().transport(captureAndRespond(captured, xmlBody, "application/xml").build()).rootUrl("http://x.com").parsers(jsonXmlParsers()).build()) {
				var bean = c.remote(U_AcceptClient.class).get();
				assertEquals("Bob", bean.name);
				assertEquals(30, bean.age);
			}
			var req = captured.get(0);
			assertEquals(1, countHeaders(req, "Accept"));
			assertEquals("application/xml", headerValue(req, "Accept"));
		}

		@Test void u02_acceptFallbackParser_whenResponseUnlabeled() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			var xmlBody = (String) XmlSerializer.DEFAULT.write(new T8_Bean("Sue", 25));
			// Response carries NO Content-Type → must fall back to the accept (XML) parser, not the default JSON parser
			// (parsing XML as JSON would throw — a clean success proves the fallback fired).
			try (var c = RestClient.builder().transport(captureAndRespond(captured, xmlBody, null).build()).rootUrl("http://x.com").parsers(jsonXmlParsers()).build()) {
				var bean = c.remote(U_AcceptClient.class).get();
				assertEquals("Sue", bean.name);
				assertEquals(25, bean.age);
			}
		}

		@Test void u03_methodAcceptOverridesInterfaceDefault() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			var xmlBody = (String) XmlSerializer.DEFAULT.write(new T8_Bean("a", 1));
			try (var c = RestClient.builder().transport(captureAndRespond(captured, xmlBody, "application/xml").build()).rootUrl("http://x.com").parsers(jsonXmlParsers()).build()) {
				c.remote(U_LevelClient.class).methodWins();
				assertEquals("application/xml", headerValue(captured.get(0), "Accept"));
			}
			captured.clear();
			var jsonBody = (String) JsonSerializer.DEFAULT.write(new T8_Bean("a", 1));
			try (var c = RestClient.builder().transport(captureAndRespond(captured, jsonBody, "application/json").build()).rootUrl("http://x.com").parsers(jsonXmlParsers()).build()) {
				c.remote(U_LevelClient.class).ifaceDefault();
				assertEquals("application/json", headerValue(captured.get(0), "Accept"));
			}
		}
	}

	// --- precedence vs a constant header (dedicated attribute wins, single header) --------------------------------

	@Remote(path="/v")
	public interface V_PrecedenceClient {
		@RemotePost(path="/echo", headers={"Content-Type: text/plain"}, contentType="application/xml") String contentTypeWins(@Content T8_Bean bean);
		@RemoteGet(path="/echo", headers={"Accept: text/plain"}, accept="application/xml") T8_Bean acceptWins();
	}

	@Nested class V_PrecedenceVsConstantHeader {

		@Test void v01_contentTypeAttributeWinsOverConstantHeader() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = captureClient(captured, jsonXmlSerializers(), null)) {
				c.remote(V_PrecedenceClient.class).contentTypeWins(new T8_Bean("Bob", 30));
			}
			var req = captured.get(0);
			assertEquals(1, countHeaders(req, "Content-Type"));
			assertEquals("application/xml", headerValue(req, "Content-Type"));
			assertTrue(readBody(req).contains("<"), readBody(req));
		}

		@Test void v02_acceptAttributeWinsOverConstantHeader() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			var xmlBody = (String) XmlSerializer.DEFAULT.write(new T8_Bean("a", 1));
			try (var c = RestClient.builder().transport(captureAndRespond(captured, xmlBody, "application/xml").build()).rootUrl("http://x.com").parsers(jsonXmlParsers()).build()) {
				c.remote(V_PrecedenceClient.class).acceptWins();
			}
			var req = captured.get(0);
			assertEquals(1, countHeaders(req, "Accept"));
			assertEquals("application/xml", headerValue(req, "Accept"));
		}
	}

	// --- interface-level contentType default + method override -------------------------------------------------------

	@Remote(path="/w", contentType="application/xml")
	public interface W_IfaceContentTypeClient {
		@RemotePost("/echo") String ifaceContentType(@Content T8_Bean bean);
		@RemotePost(path="/echo", contentType="application/json") String methodOverride(@Content T8_Bean bean);
	}

	@Nested class W_InterfaceLevelContentType {

		@Test void w01_interfaceContentTypeApplied() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = captureClient(captured, jsonXmlSerializers(), null)) {
				c.remote(W_IfaceContentTypeClient.class).ifaceContentType(new T8_Bean("Bob", 30));
			}
			var req = captured.get(0);
			assertEquals(1, countHeaders(req, "Content-Type"));
			assertEquals("application/xml", headerValue(req, "Content-Type"));
			assertTrue(readBody(req).contains("<"), readBody(req));
		}

		@Test void w02_methodContentTypeOverridesInterface() throws Exception {
			var captured = new ArrayList<TransportRequest>();
			try (var c = captureClient(captured, jsonXmlSerializers(), null)) {
				c.remote(W_IfaceContentTypeClient.class).methodOverride(new T8_Bean("Bob", 30));
			}
			var req = captured.get(0);
			assertEquals(1, countHeaders(req, "Content-Type"));
			assertEquals("application/json", headerValue(req, "Content-Type"));
			assertTrue(readBody(req).trim().startsWith("{"), readBody(req));
		}
	}
}
