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
package org.apache.juneau.rest.server.views;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

class ConsoleOutputMixin_Test extends TestBase {

	@Rest
	public static class R extends BasicRestServlet implements ConsoleOutputMixin {
		private static final long serialVersionUID = 1L;
		static final Map<String,ConsoleOutputSource> SOURCES = new ConcurrentHashMap<>();
		static final List<String> RESOLVED = new CopyOnWriteArrayList<>();

		@Override
		public Optional<ConsoleOutputSource> consoleOutputSource(String logId, RestRequest req) {
			RESOLVED.add(logId);
			return Optional.ofNullable(SOURCES.get(logId));
		}
	}

	private static final MockRestClient c = MockRestClient.buildLax(R.class);

	@BeforeAll static void setUp() {
		var log = new ConsoleOutputLog();
		log.append(ConsoleOutputLine.info("hello"));
		log.append(ConsoleOutputLine.warning("careful"));
		R.SOURCES.put("job-1", log);
	}

	@BeforeEach void clear() {
		R.RESOLVED.clear();
	}

	@Test void a01_constants() {
		assertEquals("/juneau-console-output", ConsoleOutputMixin.CONSOLE_OUTPUT_PREFIX);
		assertEquals("/juneau-console-output/{logId}/lines", ConsoleOutputMixin.LINES_PATH);
		assertEquals("/juneau-console-output/{logId}/download", ConsoleOutputMixin.DOWNLOAD_PATH);
	}

	@Test void a02_forMixinMatchesRoutes() {
		var m = ConsoleOutputDef.forMixin("x", "/petstore", "job-1").toMap();
		assertEquals("/petstore" + ConsoleOutputMixin.LINES_PATH.replace("{logId}", "job-1"), m.get("linesUrl"));
		assertEquals("/petstore" + ConsoleOutputMixin.DOWNLOAD_PATH.replace("{logId}", "job-1"), m.get("downloadUrl"));
	}

	@Test void b01_linesRoutes() throws Exception {
		var body = c.get("/juneau-console-output/job-1/lines").header("Accept", "application/json").run()
			.assertStatus(200).getContent().asString();
		var p = Json.to(body, Map.class);
		assertBean(p, "contractVersion,next,more", "1,2,false");
		assertList(R.RESOLVED, "job-1");
	}

	@Test void b02_linesAcceptAnyIsJson() throws Exception {
		c.get("/juneau-console-output/job-1/lines").header("Accept", "*/*").run().assertStatus(200)
			.assertHeader("Content-Type").isContains("application/json");
	}

	@Test void b03_unknownIdIs404() throws Exception {
		c.get("/juneau-console-output/nope/lines").run().assertStatus(404);
		c.get("/juneau-console-output/nope/download").run().assertStatus(404);
		assertList(R.RESOLVED, "nope", "nope");
	}

	@Test void b04_badLogIdIs400BeforeResolver() throws Exception {
		c.get("/juneau-console-output/a.b/lines").run().assertStatus(400);
		c.get("/juneau-console-output/" + "x".repeat(129) + "/lines").run().assertStatus(400);
		c.get("/juneau-console-output/a.b/download").run().assertStatus(400);
		assertList(R.RESOLVED);
	}

	@Test void b05_downloadRoutes() throws Exception {
		c.get("/juneau-console-output/job-1/download").run().assertStatus(200)
			.assertHeader("Content-Type").isContains("application/jsonl");
	}

	@Test void b06_oldSuffixRouteIs404() throws Exception {
		c.get("/juneau-console-output/abc/lines.jsonl").run().assertStatus(404);
		c.get("/juneau-console-output/job-1/lines.jsonl").run().assertStatus(404);
	}

	@Test void b07_badParamsAre400ThroughMixin() throws Exception {
		c.get("/juneau-console-output/job-1/lines?after=0&tail=1").run().assertStatus(400);
	}
}
