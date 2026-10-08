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
package org.apache.juneau.rest.server.datatables;

import static org.apache.juneau.test.bct.BctAssertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.config.*;
import org.apache.juneau.rest.server.staticfile.*;
import org.junit.jupiter.api.*;

/**
 * MockRest tests of {@link WebJarsMixin}.  They live here because {@code juneau-rest-server} has no mock-client
 * test dependency, and this module has both the mock client and the real WebJars on its classpath.
 */
@SuppressWarnings({
	"resource" // MockRestClient.close() is a no-op (no real OS resource).
})
class WebJarsMixin_Test extends TestBase {

	@Rest(mixins=WebJarsMixin.class)
	public static class Host implements BasicUniversalConfig {}

	private static final MockRestClient C = MockRestClient.buildLax(Host.class);

	@Test void a01_knownAsset_200_contentType_immutableCaching() throws Exception {
		var r = C.get("/webjars/fixture/1.0/Upper.JS").run().assertStatus(200);
		assertContains("text/javascript", r.getHeader("Content-Type").asString().orElse("(none)"));
		assertString("public, max-age=31536000, immutable", r.getHeader("Cache-Control").asString().orElse("(none)"));
		assertContains("window.fixture", r.getContent().asString());
	}

	@Test void a02_realWebJar_isServed() throws Exception {
		var r = C.get("/webjars/jquery/3.7.1/jquery.min.js").run().assertStatus(200);
		assertContains("text/javascript", r.getHeader("Content-Type").asString().orElse("(none)"));
		C.get("/webjars/datatables.net-dt/2.3.8/css/dataTables.dataTables.min.css").run().assertStatus(200);
	}

	// The empty-segment case ("a//b") is covered by WebJarsMixin_PathCheck_Test: the REST layer collapses "//" before the mixin sees it.
	@Test void a03_traversalAndEncoding_404() throws Exception {
		for (var p : new String[] {
				"a/../../x", "%2e%2e/x", "fixture/%2e%2e/fixture/1.0/Upper.JS", "fixture%2f1.0%2fUpper.JS",
				"fixture%5c1.0%5cUpper.JS", "./fixture/1.0/Upper.JS" })
			C.get("/webjars/" + p).run().assertStatus(404);
	}

	@Test void a04_disallowedExtension_directory_missing_404() throws Exception {
		for (var p : new String[] { "fixture/1.0/readme.txt", "fixture/1.0", "fixture", "fixture/1.0/missing.js" })
			C.get("/webjars/" + p).run().assertStatus(404);
	}

	@Test void a05_head_headersWithoutBody() throws Exception {
		var r = C.head("/webjars/fixture/1.0/Upper.JS").run().assertStatus(200);
		assertContains("text/javascript", r.getHeader("Content-Type").asString().orElse("(none)"));
		assertEmpty(r.getContent().asString());
	}
}
