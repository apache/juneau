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
package org.apache.juneau.rest.server.datatables.adapter;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.http.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.config.*;
import org.apache.juneau.rest.server.datatables.*;
import org.junit.jupiter.api.*;

/**
 * End-to-end MockRest coverage for the DataTables wire contract: a real {@code @RestPost} endpoint
 * posting/returning the exact DataTables JSON shapes, the JSON-only body requirement, and
 * {@code BeanQueryExecutionException} reaching the generic HTTP&nbsp;500 mapping untouched.
 */
@SuppressWarnings({
	"resource" // MockRestClient.close() is a no-op (no real OS resource).
})
class DataTablesQuery_Rest_Test extends TestBase {

	public static class Rec {
		private final String name;
		private final int age;

		Rec(String name, int age) {
			this.name = name;
			this.age = age;
		}

		public String getName() { return name; }
		public int getAge() { return age; }
	}

	private static final List<Rec> ROWS = List.of(new Rec("Alice", 30), new Rec("Bob", 25), new Rec("Carol", 40));

	/** The happy-path host: a POST query endpoint plus the mixed-in glue script. */
	@Rest(mixins=DataTablesMixin.class)
	public static class Host implements BasicUniversalConfig {
		@RestPost(path="/query", parsers=JsonParser.class)
		public DataTablesResults<Rec> query(@Content DataTablesRequest request) {
			try (var s = InMemoryBeanQueryContext.create(Rec.class).columns("name", "age").build().getSession(ROWS)) {
				return DataTablesQuery.run(request, s);
			}
		}
	}

	/** A bean whose {@code getAge()} always throws, to drive a genuine {@code BeanQueryExecutionException}. */
	public static class BoomRec {
		public String getName() { return "Zoe"; }
		public int getAge() { throw new RuntimeException("boom"); }
	}

	/** A second host whose session always fails reading {@code age}, to exercise the 500 path. */
	@Rest
	public static class BoomHost implements BasicUniversalConfig {
		@RestPost(path="/query", parsers=JsonParser.class)
		public DataTablesResults<BoomRec> query(@Content DataTablesRequest request) {
			try (var s = InMemoryBeanQueryContext.create(BoomRec.class).build().getSession(List.of(new BoomRec()))) {
				return DataTablesQuery.run(request, s);
			}
		}
	}

	private static final String JSON = "application/json";

	@Nested class A_wire {

		@Test void a01_jsonPost_globalSearchOrderAndPaging() throws Exception {
			var c = MockRestClient.buildLax(Host.class);
			var body = "{"
				+ "\"draw\":1,\"start\":0,\"length\":2,"
				+ "\"search\":{\"value\":\"a\"},"
				+ "\"order\":[{\"column\":1,\"dir\":\"desc\"}],"
				+ "\"columns\":[{\"data\":\"name\"},{\"data\":\"age\"}]"
				+ "}";
			c.post("/query", body).contentType(JSON).accept(JSON).run()
				.assertStatus(200)
				.assertContent().isContains("\"draw\":1", "\"recordsTotal\":3", "\"Carol\"");
		}

		@Test void a02_columnDataAsJsonNumberResolvesPositionally() throws Exception {
			var c = MockRestClient.buildLax(Host.class);
			// data:0 (a JSON number, not a quoted string) must resolve to the 0th declared column ("name").
			var body = "{\"draw\":2,\"length\":10,\"columns\":[{\"data\":0,\"search\":{\"value\":\"$eq(Bob)\"}},{\"data\":1}]}";
			c.post("/query", body).contentType(JSON).accept(JSON).run()
				.assertStatus(200)
				.assertContent().isContains("\"draw\":2", "\"recordsFiltered\":1", "\"Bob\"");
		}

		@Test void a03_malformedExpression_400WithBeanQueryErrorHeader() throws Exception {
			var c = MockRestClient.create(Host.class).ignoreErrors().build();
			var body = "{\"draw\":3,\"length\":10,\"columns\":[{\"data\":\"name\",\"search\":{\"value\":\"$noSuchOp(x)\"}}]}";
			c.post("/query", body).contentType(JSON).accept(JSON).run()
				.assertStatus(400)
				.assertHeader("X-BeanQuery-Error").isExists();
		}

		@Test void a04_executionException_genericHttp500() throws Exception {
			var c = MockRestClient.create(BoomHost.class).ignoreErrors().build();
			var body = "{\"draw\":4,\"length\":10,\"columns\":[{\"data\":\"age\",\"search\":{\"value\":\"$gt(0)\"}}]}";
			c.post("/query", body).contentType(JSON).accept(JSON).run()
				.assertStatus(500)
				.assertContent().isNotContains("boom", "RuntimeException");
		}

		@Test void a05_formUrlEncodedBody_rejected() throws Exception {
			var c = MockRestClient.create(Host.class).ignoreErrors().build();
			var r = c.post("/query", "draw=1&start=0&length=10").contentType("application/x-www-form-urlencoded").accept(JSON).run();
			assertTrue(r.getStatusCode() >= 400 && r.getStatusCode() < 500, "Expected a 4xx status, got " + r.getStatusCode());
		}

		@Test void a06_glueScriptStillServedAlongsideQueryEndpoint() throws Exception {
			var c = MockRestClient.buildLax(Host.class);
			c.get(DataTablesMixin.GLUE_PATH).run()
				.assertStatus(200)
				.assertHeader("Content-Type").isContains("text/javascript");
		}

		/**
		 * The exact body DataTables 2.1.8's {@code _fnAjaxParameters} sends: 2.x adds {@code search.fixed} (global and
		 * per column) and {@code order[].name} on top of the 1.x shape, and a strict parser must still accept it.
		 */
		private static final String DT2_BODY = "{"
			+ "\"draw\":1,"
			+ "\"columns\":["
			// A views-runtime synthetic column (row expander / actions): data:null, neither searchable nor orderable.
			+ "{\"data\":null,\"name\":\"\",\"searchable\":false,\"orderable\":false,\"search\":{\"value\":\"\",\"regex\":false,\"fixed\":[]}},"
			+ "{\"data\":\"name\",\"name\":\"\",\"searchable\":true,\"orderable\":true,\"search\":{\"value\":\"\",\"regex\":false,\"fixed\":[]}},"
			+ "{\"data\":\"age\",\"name\":\"\",\"searchable\":true,\"orderable\":true,\"search\":{\"value\":\"\",\"regex\":false,\"fixed\":[{\"name\":\"f\",\"term\":\"x\"}]}}"
			+ "],"
			+ "\"order\":[{\"column\":1,\"dir\":\"asc\",\"name\":\"\"}],"
			+ "\"start\":0,\"length\":10,"
			+ "\"search\":{\"value\":\"\",\"regex\":false,\"fixed\":[]}"
			+ "}";

		@Test void a07_dataTables2xRequestBody_accepted() throws Exception {
			var c = MockRestClient.buildLax(Host.class);
			c.post("/query", DT2_BODY).contentType(JSON).accept(JSON).run()
				.assertStatus(200)
				.assertContent().isContains("\"draw\":1", "\"recordsTotal\":3", "\"Bob\"");
		}

		@Test void a08_dataTables2xRequestBody_parsesWithDefaultJson() throws Exception {
			// The plain Json.to path (no REST layer) - what a hand-rolled handler, e.g. a JDK HttpServer, uses.
			var r = org.apache.juneau.marshall.marshaller.Json.to(DT2_BODY, DataTablesRequest.class);
			assertBean(r, "draw,start,length,columns{length},order{length}", "1,0,10,{3},{1}");
			assertList(r.getColumns().stream().map(DataTablesRequest.Column::getData).toList(), "<null>", "name", "age");
			assertBean(r.getOrder().get(0), "column,dir,name", "1,asc,");
			assertBean(r.getColumns().get(2).getSearch().getFixed().get(0), "name,term", "f,x");
			assertList(r.getSearch().getFixed());
		}

		/** DT2_BODY plus an app param merged in by ajax(url, {data:{...}}) and a SearchBuilder extension key. */
		private static final String DT2_EXTRA_BODY = DT2_BODY.substring(0, DT2_BODY.length() - 1)
			+ ",\"tenant\":\"x\",\"searchBuilder\":{}}";

		@Test void a09_unknownTopLevelProperties_collectedNotRejected() throws Exception {
			var c = MockRestClient.buildLax(Host.class);
			c.post("/query", DT2_EXTRA_BODY).contentType(JSON).accept(JSON).run()
				.assertStatus(200)
				.assertContent().isContains("\"draw\":1", "\"recordsTotal\":3");
			var r = org.apache.juneau.marshall.marshaller.Json.to(DT2_EXTRA_BODY, DataTablesRequest.class);
			assertBean(r, "draw,extra{tenant},extra{searchBuilder}", "1,{x},{{}}");
		}
	}
}
