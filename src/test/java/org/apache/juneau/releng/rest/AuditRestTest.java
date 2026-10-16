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

package org.apache.juneau.releng.rest;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.apache.juneau.commons.inject.StackOverlay;
import org.apache.juneau.http.entity.StringBody;
import org.apache.juneau.marshall.marshaller.Json;
import org.apache.juneau.releng.engine.AuditService;
import org.apache.juneau.releng.engine.RcHistoryEntry;
import org.apache.juneau.releng.engine.RunState;
import org.apache.juneau.releng.engine.RunStateStore;
import org.apache.juneau.releng.log.RunEventStore;
import org.apache.juneau.rest.mock.MockRestClient;
import org.apache.juneau.rest.server.runreport.RunEvent.EndStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuditRestTest {

	@SuppressWarnings({
		"resource" // Caller owns and closes the returned MockRestClient (via try-with-resources).
	})
	private static MockRestClient client(Path dir) {
		var events = new RunEventStore(dir);
		var store = new RunStateStore(dir);
		for (var v : List.of("9.2.1", "9.10.0")) {
			events.begin(v, "build", "Build", 1);
			events.end(v, "build", EndStatus.OK, 5);
		}
		var rs = RunState.create("9.2.1", "b", List.of("build"));
		rs.rcHistory.add(new RcHistoryEntry(1, "2026-10-01T00:00:00Z", "bad RC"));
		store.save(rs);
		return MockRestClient.builder(new AuditRest(new AuditService(store, events))).overridingBeanStore(new StackOverlay()).build();
	}

	private static Map<?,?> query(MockRestClient client, String request) throws Exception {
		try (var resp = client.request("POST", "/query").header("Accept", "application/json").body(StringBody.of(request, "application/json")).run()) {
			assertEquals(200, resp.getStatusCode());
			return Json.to(resp.getBodyAsString(), Map.class);
		}
	}

	private static List<String> col(Map<?,?> envelope, String key) {
		return ((List<?>)envelope.get("data")).stream().map(r -> String.valueOf(((Map<?,?>)r).get(key))).toList();
	}

	@Test
	void a01_queryPagesTheFlattenedRows(@TempDir Path dir) throws Exception {
		try (var client = client(dir)) {
			var first = query(client, "{\"draw\":2,\"start\":0,\"length\":2,\"columns\":[{\"data\":\"run\",\"searchable\":true,\"orderable\":true},{\"data\":\"seq\",\"searchable\":true,\"orderable\":true}],\"order\":[{\"column\":0,\"dir\":\"asc\"},{\"column\":1,\"dir\":\"asc\"}]}");
			assertBean(first, "draw,recordsTotal,recordsFiltered", "2,5,5");
			assertList(col(first, "run"), "9.2.1", "9.2.1");
			var last = query(client, "{\"draw\":3,\"start\":4,\"length\":2,\"columns\":[{\"data\":\"run\",\"searchable\":true,\"orderable\":true}],\"order\":[{\"column\":0,\"dir\":\"desc\"}]}");
			assertEquals(1, ((List<?>)last.get("data")).size());
		}
	}

	@Test
	void a02_runColumnOrdersSemantically(@TempDir Path dir) throws Exception {
		try (var client = client(dir)) {
			var env = query(client, "{\"draw\":1,\"start\":0,\"length\":1,\"columns\":[{\"data\":\"run\",\"searchable\":true,\"orderable\":true}],\"order\":[{\"column\":0,\"dir\":\"desc\"}]}");
			assertList(col(env, "run"), "9.10.0");
		}
	}

	@Test
	void a03_filtersByKind(@TempDir Path dir) throws Exception {
		try (var client = client(dir)) {
			var env = query(client, "{\"draw\":1,\"start\":0,\"length\":10,\"columns\":[{\"data\":\"kind\",\"searchable\":true,\"orderable\":true,\"search\":{\"value\":\"rc-dropped\"}}]}");
			assertBean(env, "recordsTotal,recordsFiltered", "5,1");
			assertList(col(env, "detail"), "bad RC");
		}
	}

	@Test
	void a04_pageRendersWithTheAuditCard(@TempDir Path dir) throws Exception {
		try (var client = client(dir)) {
			try (var resp = client.request("GET", "/").run()) {
				assertEquals(200, resp.getStatusCode());
				var body = resp.getBodyAsString();
				assertTrue(body.contains("/rest/audit/query"), body);
			}
		}
	}
}
