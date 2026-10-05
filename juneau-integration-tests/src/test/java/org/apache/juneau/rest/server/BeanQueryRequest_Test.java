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
package org.apache.juneau.rest.server;

import static org.apache.juneau.test.bct.BctAssertions.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

/**
 * Tests the stock {@link BeanQueryRequest} &mdash; the concrete {@link org.apache.juneau.commons.beanquery.BeanQuery
 * BeanQuery} subclass that binds one {@code search} / {@code view} / {@code sort} / {@code position} / {@code limit} /
 * {@code opts} query parameter each (locked shape: single-valued, raw strings, <jk>null</jk>=absent).
 */
class BeanQueryRequest_Test extends TestBase {

	private static JsonMap echo(BeanQueryRequest q) {
		return JsonMap.create()
			.append("search", q.getSearch())
			.append("view", q.getView())
			.append("sort", q.getSort())
			.append("position", q.getPosition())
			.append("limit", q.getLimit())
			.append("opts", q.getOpts());
	}

	@Rest(serializers=Json5Serializer.class)
	public static class A {
		@RestGet
		public JsonMap a(BeanQueryRequest q) {
			return echo(q);
		}
		@RestGet("/v")
		public JsonMap v(AppBeanQuery q) {
			return echo(q);
		}
		@RestGet("/def")
		public JsonMap def(DefViewQuery q) {
			return echo(q);
		}
	}

	@Request
	public static class AppBeanQuery extends BeanQueryRequest {
		@Query("v")
		@Override
		public String getView() {
			return super.getView();
		}
	}

	@Request
	public static class DefViewQuery extends BeanQueryRequest {
		@Query(def="*")
		@Override
		public String getView() {
			return super.getView();
		}
	}

	@Test void a01_search_singleRawString() throws Exception {
		var a = MockRestClient.build(A.class);
		a.get("/a?search=name=$eq(Alice),age=$gt(20)").run().assertContent()
			.isContains("search:'name=$eq(Alice),age=$gt(20)'");
	}

	@Test void a02_searchMissing_null() throws Exception {
		var a = MockRestClient.build(A.class);
		a.get("/a").run().assertContent().isContains("search:null");
	}

	@Test void a03_opts_singleRawString() throws Exception {
		var a = MockRestClient.build(A.class);
		a.get("/a?opts=counts=true").run().assertContent().isContains("opts:'counts=true'");
	}

	@Test void a04_optsMissing_null() throws Exception {
		var a = MockRestClient.build(A.class);
		a.get("/a").run().assertContent().isContains("opts:null");
	}

	@Test void a05_opts_singularNameIgnored() throws Exception {
		var a = MockRestClient.build(A.class);
		a.get("/a?opt=counts=true").run().assertContent().isContains("opts:null");
	}

	@Test void a06_scalars_viewSortPositionLimit() throws Exception {
		var a = MockRestClient.build(A.class);
		a.get("/a?view=id,name&sort=id&position=10&limit=25").run().assertContent()
			.isContains("view:'id,name'", "sort:'id'", "position:10", "limit:25");
	}

	@Test void a07_subtype_remapsView() throws Exception {
		var a = MockRestClient.build(A.class);
		a.get("/v?v=cols&view=ignored").run().assertContent().isContains("view:'cols'");
	}

	@Test void a08_subtype_defOnView() throws Exception {
		var a = MockRestClient.build(A.class);
		a.get("/def").run().assertContent().isContains("view:'*'");
		a.get("/def?view=id").run().assertContent().isContains("view:'id'");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Client @Remote + concrete @Request BeanQueryRequest
	//-----------------------------------------------------------------------------------------------------------------

	@Remote
	public interface A2 {
		@RemoteGet("/a") String a(BeanQueryRequest q);
	}

	@Test void b01_clientRemote_searchAndView() {
		var x = MockRestClient.create(A.class).allowPrivateUrls(true).build().getRemote(A2.class);
		var q = new BeanQueryRequest();
		q.setSearch("name=$eq(a)");
		q.setView("id");
		var body = x.a(q);
		assertContainsAll(body, "search:'name=$eq(a)'", "view:'id'");
	}

	@Test void b02_clientRemote_opts() {
		var x = MockRestClient.create(A.class).allowPrivateUrls(true).build().getRemote(A2.class);
		var q = new BeanQueryRequest();
		q.setOpts("counts=true");
		var body = x.a(q);
		assertContains(body, "opts:'counts=true'");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Swagger (@Schema) and part validation
	//-----------------------------------------------------------------------------------------------------------------

	private static org.apache.juneau.bean.swagger.Swagger getSwagger(Object resource) throws Exception {
		var rc = new RestContext(new RestContext.Args(resource.getClass(), null, null, () -> resource, "", null, null, null, RestContext.ContextKind.ROOT));
		var roc = new RestOpContext(BeanQueryRequest_Test.class.getMethod("testMethod"), rc);
		var call = RestSession.create(rc).resource(resource).req(new org.apache.juneau.rest.mock.MockServletRequest()).res(new org.apache.juneau.rest.mock.MockServletResponse()).build();
		var req = roc.createRequest(call);
		var ip = rc.getSwaggerProvider();
		return ip.getSwagger(rc, req.getLocale());
	}

	public void testMethod() { /* no-op, method-handle target for RestOpContext construction above */ }

	@Test void c01_swagger_descriptionsOnAllSixParameters() throws Exception {
		var swagger = getSwagger(new A());
		for (var n : new String[]{"search", "view", "sort", "position", "limit", "opts"})
			assertNotEmpty(swagger.getParameterInfo("/a", "get", "query", n).getDescription());
		assertMatchesGlob("Search expression: comma-separated column=expression clauses*https://juneau.apache.org/docs/topics/JuneauCommonsBeanQuery*",
			swagger.getParameterInfo("/a", "get", "query", "search").getDescription());
	}

	@Test void c02_swagger_minimumOnPositionOnly() throws Exception {
		var swagger = getSwagger(new A());
		assertBean(swagger.getParameterInfo("/a", "get", "query", "position"), "type,minimum", "integer,0");
		assertBean(swagger.getParameterInfo("/a", "get", "query", "limit"), "type,minimum", "integer,<null>");
		assertBean(swagger.getParameterInfo("/a", "get", "query", "search"), "type,minimum", "string,<null>");
	}

	@Test void c03_swagger_subtypeRemappedName() throws Exception {
		var swagger = getSwagger(new A());
		assertBean(swagger.getParameterInfo("/v", "get", "query", "v"), "name,in", "v,query");
	}

	@Test void c04_negativePosition_400() throws Exception {
		var a = MockRestClient.create(A.class).ignoreErrors().build();
		a.get("/a?position=-1").run().assertStatus(400).assertHeader("X-BeanQuery-Error").isNull();
	}

	@Test void c05_negativeLimit_binds() throws Exception {
		var a = MockRestClient.build(A.class);
		a.get("/a?limit=-1").run().assertContent().isContains("limit:-1");
	}

	@Test void c06_errorHeaderConstant() {
		assertString("X-BeanQuery-Error", BeanQueryRequest.ERROR_HEADER);
	}

	private static void assertContains(String body, String fragment) {
		Assertions.assertTrue(body != null && body.contains(fragment),
			() -> "Expected '" + fragment + "' in: " + body);
	}
}
