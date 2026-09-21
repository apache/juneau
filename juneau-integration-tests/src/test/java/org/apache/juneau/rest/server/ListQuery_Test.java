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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

class ListQuery_Test extends TestBase {

	private static JsonMap echo(ListQuery q, RestRequest req) {
		return JsonMap.create()
			.append("search", q.getSearch().orElse(null))
			.append("searchPresent", q.getSearch().isPresent())
			.append("view", q.getView().orElse(null))
			.append("sort", q.getSort().orElse(null))
			.append("position", q.getPosition().orElse(null))
			.append("limit", q.getLimit().orElse(null))
			.append("opts", q.getOpts().orElse(null))
			.append("optsPresent", q.getOpts().isPresent())
			.append("optRaw", req.getQueryParams().getAll("opt").stream().map(x -> x.getValue()).toList());
	}

	@Rest(serializers=Json5Serializer.class)
	public static class A {
		@RestGet
		public JsonMap a(ListQuery q, RestRequest req) {
			return echo(q, req);
		}
		@RestGet("/v")
		public JsonMap v(AppListQuery q, RestRequest req) {
			return echo(q, req);
		}
		@RestGet("/def")
		public JsonMap def(DefViewQuery q, RestRequest req) {
			return echo(q, req);
		}
	}

	@Request
	public interface AppListQuery extends ListQuery {
		@Query("v")
		@Override
		Optional<String> getView();
	}

	@Request
	public interface DefViewQuery extends ListQuery {
		@Query(def="*")
		@Override
		Optional<String> getView();
	}

	@Test void a01_multiSearch_repeatedParams() throws Exception {
		var a = MockRestClient.build(A.class);
		a.get("/a?search=a&search=b").run().assertContent().isContains("search:['a','b']", "searchPresent:true");
	}

	@Test void a02_searchMissing_emptyOptional() throws Exception {
		var a = MockRestClient.build(A.class);
		a.get("/a").run().assertContent().isContains("search:null", "searchPresent:false");
	}

	@Test void a03_searchPresentEmpty_emptyArray() throws Exception {
		var a = MockRestClient.build(A.class);
		a.get("/a?search=").run().assertContent().isContains("search:[]", "searchPresent:true");
		a.get("/a?search=&search=").run().assertContent().isContains("search:[]", "searchPresent:true");
	}

	@Test void a04_opts_repeatedKeyUonValue() throws Exception {
		var a = MockRestClient.build(A.class);
		a.get("/a?opt=environment=prod&opt=period=7d").run().assertContent()
			.isContains("opts:{", "environment:'prod'", "period:'7d'", "optsPresent:true");
	}

	@Test void a05_opts_uonListValue() throws Exception {
		var a = MockRestClient.build(A.class);
		a.get("/a?opt=flags=(a,b,c)").run().assertContent()
			.isContains("flags:['a','b','c']", "optsPresent:true");
	}

	@Test void a06_opts_nestedUonMap() throws Exception {
		var a = MockRestClient.build(A.class);
		a.get("/a?opt=nested=(x=1,y=2)").run().assertContent()
			.isContains("nested:{", "x:1", "y:2");
	}

	@Test void a07_opts_laterKeyWins() throws Exception {
		var a = MockRestClient.build(A.class);
		a.get("/a?opt=a=1&opt=a=2").run().assertContent()
			.isContains("opts:{a:2}");
	}

	@Test void a08_opts_parenthesizedUonMap() throws Exception {
		var a = MockRestClient.build(A.class);
		a.get("/a?opt=(environment=prod,period=7d)").run().assertContent()
			.isContains("environment:'prod'", "period:'7d'");
	}

	@Test void a09_optsMissing_emptyOptional() throws Exception {
		var a = MockRestClient.build(A.class);
		a.get("/a").run().assertContent().isContains("opts:null", "optsPresent:false");
	}

	@Test void a13_opts_pluralNameIgnored() throws Exception {
		var a = MockRestClient.build(A.class);
		a.get("/a?opts=environment=prod").run().assertContent()
			.isContains("optsPresent:false", "optRaw:[]");
	}

	@Test void a10_scalars_viewSortPositionLimit() throws Exception {
		var a = MockRestClient.build(A.class);
		a.get("/a?view=id,name&sort=id&position=10&limit=25").run().assertContent()
			.isContains("view:'id,name'", "sort:'id'", "position:10", "limit:25");
	}

	@Test void a11_subtype_remapsView() throws Exception {
		var a = MockRestClient.build(A.class);
		a.get("/v?v=cols&view=ignored").run().assertContent()
			.isContains("view:'cols'");
	}

	@Test void a12_subtype_defOnView() throws Exception {
		var a = MockRestClient.build(A.class);
		a.get("/def").run().assertContent().isContains("view:'*'");
		a.get("/def?view=id").run().assertContent().isContains("view:'id'");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Client @Remote + @Request ListQuery
	//-----------------------------------------------------------------------------------------------------------------

	@Remote
	public interface A2 {
		@RemoteGet("/a") String a(ListQuery q);
	}

	static class A3 implements ListQuery {
		private final String[] search;
		private final String view;
		private final JsonMap opts;
		A3(String[] search, String view) {
			this(search, view, null);
		}
		A3(String[] search, String view, JsonMap opts) {
			this.search = search;
			this.view = view;
			this.opts = opts;
		}
		@Override public Optional<String[]> getSearch() { return o(search); }
		@Override public Optional<String> getView() { return o(view); }
		@Override public Optional<String> getSort() { return Optional.empty(); }
		@Override public Optional<Integer> getPosition() { return Optional.empty(); }
		@Override public Optional<Integer> getLimit() { return Optional.empty(); }
		@Override public Optional<JsonMap> getOpts() { return o(opts); }
	}

	@Test void b01_clientRemote_multiSearchAndView() {
		var x = MockRestClient.create(A.class).allowPrivateUrls(true).build().getRemote(A2.class);
		var body = x.a(new A3(new String[]{"a", "b"}, "id"));
		assertContains(body, "search:['a','b']");
		assertContains(body, "view:'id'");
		assertContains(body, "searchPresent:true");
	}

	@Test void b02_clientRemote_optsRepeatedKeyUonValue() {
		var x = MockRestClient.create(A.class).allowPrivateUrls(true).build().getRemote(A2.class);
		var body = x.a(new A3(null, null, JsonMap.of("environment", "prod", "period", "7d")));
		assertContains(body, "environment:'prod'");
		assertContains(body, "period:'7d'");
		assertContains(body, "optsPresent:true");
		assertContains(body, "optRaw:['environment=prod','period=7d']");
	}

	private static void assertContains(String body, String fragment) {
		org.junit.jupiter.api.Assertions.assertTrue(body != null && body.contains(fragment),
			() -> "Expected '" + fragment + "' in: " + body);
	}
}
