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
package org.apache.juneau.rest.swagger;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.*;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.http.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

/**
 * Coverage-targeted additions for {@link BasicSwaggerProviderSession}, focused on uncovered
 * code paths in operation parameter processing (@Path, @Header, @FormData), response/header
 * generation, @Items/@SubItems schema merging, and exception/edge-case branches.
 */
@SuppressWarnings({
	"java:S1874" // Intentional use of deprecated annotation members for coverage
})
class BasicSwaggerProviderSession_Coverage_Test extends TestBase {

	public void testMethod() { /* no-op */ }

	private static org.apache.juneau.bean.swagger.Swagger getSwagger(Object resource) throws Exception {
		var rc = new RestContext(new RestContext.Args(resource.getClass(), null, null, () -> resource, "", null, null, null, false));
		var roc = new RestOpContext(BasicSwaggerProviderSession_Coverage_Test.class.getMethod("testMethod"), rc);
		var call = RestSession.create(rc).resource(resource).req(new MockServletRequest()).res(new MockServletResponse()).build();
		var req = roc.createRequest(call);
		var ip = rc.getSwaggerProvider();
		return ip.getSwagger(rc, req.getLocale());
	}

	@Marshalled(typeName="Foo")
	public static class X {
		public int a;
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Path parameters — exercises BasicSwaggerProviderSession PATH parameter branch.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A1 {
		@RestGet(path="/path/{foo}")
		public X a(@Path("foo") @Schema(description="path-foo-desc",type="string") String foo) {
			return null;
		}
	}

	@Test void a01_path_param_default() throws Exception {
		var p = getSwagger(new A1()).getPaths().get("/path/{foo}").get("get").getParameter("path", "foo");
		assertNotNull(p);
		assertEquals("foo", p.getName());
		assertEquals("path", p.getIn());
		assertEquals("path-foo-desc", p.getDescription());
		assertTrue(p.getRequired());
	}

	@Rest
	public static class A2 {
		@RestGet(path="/path/{foo}")
		public X a(@Path String foo) {
			return null;
		}
	}

	@Test void a02_path_param_no_name() throws Exception {
		// When @Path has no name, swagger uses the parameter name extracted via reflection.
		var op = getSwagger(new A2()).getPaths().get("/path/{foo}").get("get");
		assertNotNull(op);
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Header parameters
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B1 {
		@RestGet(path="/hdr")
		public X a(@Header(name="X-Foo") @Schema(description="hdr-desc",type="integer",format="int32") Integer h) {
			return null;
		}
	}

	@Test void b01_header_param_default() throws Exception {
		var p = getSwagger(new B1()).getPaths().get("/hdr").get("get").getParameter("header", "X-Foo");
		assertNotNull(p);
		assertEquals("X-Foo", p.getName());
		assertEquals("header", p.getIn());
		assertEquals("hdr-desc", p.getDescription());
	}

	//------------------------------------------------------------------------------------------------------------------
	// @FormData parameters
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C1 {
		@RestPost(path="/form")
		public X a(@FormData(name="field1") @Schema(description="form-desc",type="string") String field1) {
			return null;
		}
	}

	@Test void c01_formdata_param_default() throws Exception {
		var p = getSwagger(new C1()).getPaths().get("/form").get("post").getParameter("formData", "field1");
		assertNotNull(p);
		assertEquals("field1", p.getName());
		assertEquals("formData", p.getIn());
		assertEquals("form-desc", p.getDescription());
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Content body parameter with @Schema (exercises addBodyExamples + Schema merge).
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class D1 {
		@RestPost(path="/body")
		public X a(@Content @Schema(description="body-desc",examples="{a:42}") X body) {
			return null;
		}
	}

	@Test void d01_body_with_schema_example() throws Exception {
		var p = getSwagger(new D1()).getPaths().get("/body").get("post").getParameter("body", null);
		assertNotNull(p);
		assertEquals("body", p.getIn());
		assertTrue(p.getRequired());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Response @Header at @Response class level — exercises response-header path (exception type, lines 540-552).
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class E1 {
		@RestGet(path="/exc")
		public X a() throws E1Exception {
			return null;
		}
	}

	@Response @StatusCode(404)
	public static class E1Exception extends Exception {
		private static final long serialVersionUID = 1L;
		@Header(name="X-Err",schema=@Schema(type="string",description="err-desc"))
		public String getErrorHeader() { return null; }
	}

	@Test void e01_response_exception_with_header() throws Exception {
		var op = getSwagger(new E1()).getPaths().get("/exc").get("get");
		assertNotNull(op);
		var r = op.getResponse(404);
		assertNotNull(r);
		assertNotNull(r.getHeaders());
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Header on response method (Response class with @Header on a getter) — covers lines 575-587.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class F1 {
		@RestGet(path="/respHdr")
		public F1Resp a() {
			return null;
		}
	}

	@Response @StatusCode(200)
	public static class F1Resp {
		@Header(name="X-Foo",schema=@Schema(type="integer",format="int32",description="foo-hdr"))
		public Integer getFoo() { return null; }
	}

	@Test void f01_response_method_with_header() throws Exception {
		var op = getSwagger(new F1()).getPaths().get("/respHdr").get("get");
		assertNotNull(op);
		var r = op.getResponse(200);
		assertNotNull(r);
		assertNotNull(r.getHeaders());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Holder<T> @Header parameter — covers lines 600-619.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class G1 {
		@RestGet(path="/valHdr")
		public void a(@Header(name="X-V",schema=@Schema(type="string",description="value-hdr")) Holder<String> h, @StatusCode Holder<Integer> sc) {
			/* no-op */
		}
	}

	@Test void g01_value_header_response() throws Exception {
		var op = getSwagger(new G1()).getPaths().get("/valHdr").get("get");
		assertNotNull(op);
		// Default StatusCode 200 should be assigned.
		var r = op.getResponse(200);
		assertNotNull(r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Response on a parameter (Holder<T>) — covers lines 622-637.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class H1 {
		@RestGet(path="/valResp")
		public void a(@Response @StatusCode(201) @Schema(description="resp-desc",type="string") Holder<String> r) {
			/* no-op */
		}
	}

	@Test void h01_value_response_param() throws Exception {
		var op = getSwagger(new H1()).getPaths().get("/valResp").get("get");
		assertNotNull(op);
		var r = op.getResponse(201);
		assertNotNull(r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Default response description — exercises lines 642-647 (isDecimal branch + getHttpResponseText).
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class I1 {
		@RestGet(path="/respDesc",
			swagger=@OpSwagger("responses:{200:{},404:{},notACode:{description:'manual'}}"))
		public X a() {
			return null;
		}
	}

	@Test void i01_default_response_descriptions() throws Exception {
		var op = getSwagger(new I1()).getPaths().get("/respDesc").get("get");
		assertNotNull(op);
		var r200 = op.getResponse(200);
		assertNotNull(r200.getDescription());  // Should be "OK" (auto-filled by RestUtils).
		var r404 = op.getResponse(404);
		assertNotNull(r404.getDescription());  // Should be "Not Found" (auto-filled).
	}

	//------------------------------------------------------------------------------------------------------------------
	// Schema with @Items (covers merge(Schema) and merge(Items) — lines 891-921).
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class J1 {
		@RestGet(path="/arrQ")
		public X a(@Query("foo") @Schema(type="array",items=@Items(type="string",cf="csv",mini=1,maxi=5,p="abc",f="myfmt",max="10",min="1",mo="2",maxl=10L,minl=1L,emax=true,emin=true,ui=true)) String[] foo) {
			return null;
		}
	}

	@Test void j01_query_with_items() throws Exception {
		var p = getSwagger(new J1()).getPaths().get("/arrQ").get("get").getParameter("query", "foo");
		assertNotNull(p);
		assertEquals("array", p.getType());
		assertNotNull(p.getItems());
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Items with @SubItems (covers merge(SubItems) — lines 991-1019).
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class K1 {
		@RestGet(path="/sub")
		public X a(@Query("foo") @Schema(type="array",items=@Items(type="array",items=@SubItems(type="string",cf="ssv",mini=0,maxi=10))) String[][] foo) {
			return null;
		}
	}

	@Test void k01_query_with_subitems() throws Exception {
		var p = getSwagger(new K1()).getPaths().get("/sub").get("get").getParameter("query", "foo");
		assertNotNull(p);
		assertEquals("array", p.getType());
		assertNotNull(p.getItems());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Schema with externalDocs (covers merge(Schema) externalDocs branch — line 962).
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class L1 {
		@RestGet(path="/ext")
		public X a(@Query("q") @Schema(description="d",externalDocs=@ExternalDocs(description="ed",url="http://example.com")) String q) {
			return null;
		}
	}

	@Test void l01_schema_with_externaldocs() throws Exception {
		var p = getSwagger(new L1()).getPaths().get("/ext").get("get").getParameter("query", "q");
		assertNotNull(p);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Schema with allOf, discriminator, readOnly, required, summary, title, xml, $ref — covers various merge(Schema) branches.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class M1 {
		@RestGet(path="/full")
		public X a(@Query("q") @Schema(allOf={"x","y"},discriminator="t",readOnly=true,required=true,summary="s",title="t",xml={"a","b"},$ref="r",d={"a","b"}) String q) {
			return null;
		}
	}

	@Test void m01_full_schema_branches() throws Exception {
		var p = getSwagger(new M1()).getPaths().get("/full").get("get").getParameter("query", "q");
		assertNotNull(p);
	}

	//------------------------------------------------------------------------------------------------------------------
	// @OpSwagger(ignore=true) on a method — covers lines 370-371 (the continue branch).
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class N1 {
		@RestGet(path="/visible")
		public X a() { return null; }

		@RestGet(path="/hidden",swagger=@OpSwagger(ignore=true))
		public X b() { return null; }
	}

	@Test void n01_op_ignore_true() throws Exception {
		var s = getSwagger(new N1());
		assertNotNull(s.getPaths().get("/visible"));
		// Hidden path should not appear since @OpSwagger(ignore=true) is set.
		assertNull(s.getPaths().get("/hidden"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Multi-tag without trailing comma — covers various tag branches.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(swagger=@Swagger(value="{tags:[{name:'tagA'}]}"))
	public static class O1 {
		@RestGet(path="/o",swagger=@OpSwagger(tags={"tagA","tagB"}))
		public X a() { return null; }
	}

	@Test void o01_op_tags_register_new_tags() throws Exception {
		var s = getSwagger(new O1());
		var tags = s.getTags();
		assertNotNull(tags);
		// tagB should have been created as a new tag entry from the op-level tag.
		assertEquals(2, tags.size());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Malformed swagger JSON object on a class — exercises SwaggerException paths.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(swagger=@Swagger("{this is :: not :: json::"))
	public static class P1 {}

	@Test void p01_malformed_swagger_throws() {
		assertThrows(Exception.class, () -> getSwagger(new P1()));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Tag without name in resource swagger — should throw SwaggerException.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(swagger=@Swagger("{tags:[{description:'no-name'}]}"))
	public static class Q1 {}

	@Test void q01_tag_without_name_throws() {
		assertThrows(Exception.class, () -> getSwagger(new Q1()));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Schema(ignore=true) — covers lines 827-828 (return null).
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class R1 {
		@RestGet(path="/ig",swagger=@OpSwagger("parameters:[{in:'query',name:'q',schema:{ignore:true}}]"))
		public X a(@Query("q") String q) { return null; }
	}

	@Test void r01_schema_ignore_true() throws Exception {
		// Should not throw; the schema is omitted because of ignore=true.
		var s = getSwagger(new R1());
		assertNotNull(s);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Schema with $ref — covers line 830 (early return).
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class S1 {
		@RestGet(path="/refq",swagger=@OpSwagger("parameters:[{in:'query',name:'q',schema:{$ref:'#/definitions/Foo'}}]"))
		public X a(@Query("q") String q) { return null; }
	}

	@Test void s01_schema_with_ref() throws Exception {
		var p = getSwagger(new S1()).getPaths().get("/refq").get("get").getParameter("query", "q");
		assertNotNull(p);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Schema with type already set — covers line 830 (early return).
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class T1 {
		@RestGet(path="/typeq",swagger=@OpSwagger("parameters:[{in:'query',name:'q',schema:{type:'string'}}]"))
		public X a(@Query("q") String q) { return null; }
	}

	@Test void t01_schema_with_type() throws Exception {
		var p = getSwagger(new T1()).getPaths().get("/typeq").get("get").getParameter("query", "q");
		assertNotNull(p);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Definitions in @Swagger — covers lines 350-352 (definitions handling).
	//------------------------------------------------------------------------------------------------------------------

	@Rest(swagger=@Swagger("{definitions:{Bar:{type:'object',properties:{x:{type:'string'}}}}}"))
	public static class U1 {
		@RestGet(path="/u")
		public X a() { return null; }
	}

	@Test void u01_definitions_in_swagger() throws Exception {
		var s = getSwagger(new U1());
		assertNotNull(s.getDefinitions());
	}

	//------------------------------------------------------------------------------------------------------------------
	// @OpSwagger with messages bundle — exercises the mb.findFirstString paths for tags/schemes/consumes/produces.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(messages="BasicRestInfoProviderTest")
	public static class V1 {
		@RestGet(path="/v")
		public X a() { return null; }
	}

	@Test void v01_op_messages() throws Exception {
		// Just verify swagger is generated without error when messages are defined.
		var s = getSwagger(new V1());
		assertNotNull(s);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Path parameter with no name (uses param Java name) — covers line 506-514 PATH branch with name fallback.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class W1 {
		@RestGet(path="/p1/{foo}")
		public X a(@Path("foo") String foo) { return null; }
	}

	@Test void w01_path_param_with_name() throws Exception {
		var p = getSwagger(new W1()).getPaths().get("/p1/{foo}").get("get").getParameter("path", "foo");
		assertNotNull(p);
		assertTrue(p.getRequired());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Operation with @Deprecated on the Java method — covers line 408-410 deprecated branch.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class Y1 {
		@RestGet(path="/dep")
		@Deprecated
		public X a() { return null; }
	}

	@Test void y01_deprecated_method() throws Exception {
		var op = getSwagger(new Y1()).getPaths().get("/dep").get("get");
		assertEquals(Boolean.TRUE, op.getDeprecated());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Operation with @Deprecated on the class — covers line 408-410 deprecated class branch.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	@Deprecated
	public static class Z1 {
		@RestGet(path="/dep")
		public X a() { return null; }
	}

	@Test void z01_deprecated_class() throws Exception {
		var op = getSwagger(new Z1()).getPaths().get("/dep").get("get");
		assertEquals(Boolean.TRUE, op.getDeprecated());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Operation with parameters in @OpSwagger — covers parameter map merging (lines 461-464).
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class AA1 {
		@RestGet(path="/aa",swagger=@OpSwagger("parameters:[{in:'query',name:'q1',type:'string'},{in:'header',name:'h1',type:'string'},{in:'path',name:'p1',type:'string'},{in:'formData',name:'f1',type:'string'},{in:'body',schema:{type:'string'}}]"))
		public X a() { return null; }
	}

	@Test void aa01_swagger_only_parameters() throws Exception {
		var op = getSwagger(new AA1()).getPaths().get("/aa").get("get");
		assertNotNull(op.getParameters());
		// All five swagger-only parameter types should be present.
		assertEquals(5, op.getParameters().size());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test pushupSchemaFields when type=object on parameter — covers lines 1122-1123.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class BB1 {
		@RestGet(path="/bb")
		public X a(@Query("q") @Schema(type="object") X q) { return null; }
	}

	@Test void bb01_object_pushup() throws Exception {
		var p = getSwagger(new BB1()).getPaths().get("/bb").get("get").getParameter("query", "q");
		assertNotNull(p);
	}

	//------------------------------------------------------------------------------------------------------------------
	// External docs at the operation level + via Messages — covers many merge paths.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(swagger=@Swagger(externalDocs=@ExternalDocs(description="d",url="http://r.example.com")))
	public static class CC1 {
		@RestGet(path="/cc",swagger=@OpSwagger(externalDocs=@ExternalDocs(description="opd",url="http://op.example.com")))
		public X a() { return null; }
	}

	@Test void cc01_op_externaldocs() throws Exception {
		var s = getSwagger(new CC1());
		assertNotNull(s.getExternalDocs());
		var op = s.getPaths().get("/cc").get("get");
		assertNotNull(op.getExternalDocs());
	}
}
