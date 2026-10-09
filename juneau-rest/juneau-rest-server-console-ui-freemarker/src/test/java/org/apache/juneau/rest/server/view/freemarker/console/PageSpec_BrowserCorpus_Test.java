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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.apache.juneau.rest.server.console.test.PageContractAssert.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

/**
 * Keeps the {@code pagespec-corpus} fixtures the {@code PageSpec*_BrowserTest} classes (in {@code juneau-rest-server-views},
 * which cannot depend on this module) render in a real browser in step with what {@link PageSpec} produces.
 *
 * <p>
 * Each fixture is the {@code #juneau-page} contract and the {@code <template>} set of one {@link PageSpec_Parity_Test}
 * case. Regenerate with {@code -Djuneau.regenPageSpecCorpus=true}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	mvn test -pl juneau-rest/juneau-rest-server-console-ui-freemarker -Dtest=PageSpec_BrowserCorpus_Test -Djuneau.regenPageSpecCorpus=true
 * </p>
 */
class PageSpec_BrowserCorpus_Test extends TestBase {

	private static final Path DIR = Path.of("..", "juneau-rest-server-views", "src", "test", "resources", "pagespec-corpus");

	private static void check(String caseName) throws Exception {
		String html;
		try (var c = MockRestClient.buildLax(PageSpec_Parity_Test.Host.class); var rsp = c.get("/spec/" + caseName).run()) {
			html = rsp.getContent().asString();
			assertEquals(200, rsp.getStatusCode(), html);
		}
		var page = assertPage(html).isValid();
		var fresh = new LinkedHashMap<String,Object>();
		fresh.put("contract", page.contract());
		fresh.put("templates", page.templates());
		var json = Json.of(fresh);
		var file = DIR.resolve(caseName + ".json");
		if (Boolean.getBoolean("juneau.regenPageSpecCorpus")) {
			Files.createDirectories(DIR);
			Files.writeString(file, json + "\n");
		}
		Assumptions.assumeTrue(Files.isDirectory(DIR), "views module not alongside; corpus not checked: " + DIR);
		assertTrue(Files.exists(file), () -> "missing corpus fixture; regenerate with -Djuneau.regenPageSpecCorpus=true: " + file);
		assertEquals(Json.to(Files.readString(file), Map.class), Json.to(json, Map.class),
			() -> file + " is stale; regenerate with -Djuneau.regenPageSpecCorpus=true");
	}

	@Test void a01_tableEveryFeature() throws Exception { check("03-table-every-feature"); }
	@Test void a02_navUnderDepth3() throws Exception { check("13-navunder-depth3"); }
	@Test void a03_visibleWhenPassthrough() throws Exception { check("19-visible-when-passthrough"); }
}
