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
package org.apache.juneau.microservice.management;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.runtime.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the {@code /info} management endpoint (mixin + resource flavors + the {@link InfoManager}
 * worker), covering manifest rendering (including build-side stamped fields) and clean degradation when no
 * manifest is present.
 */
@SuppressWarnings({
	"resource" // Closeable MockRestClient fixtures; lifecycle managed by the test/framework, not a real leak.
})
class Info_Test extends TestBase {

	/** A manifest stamped with the Q7 build/version/git fields plus a stock Main-* attribute. */
	private static ManifestFile stampedManifest() throws IOException {
		var mf = """
				Manifest-Version: 1.0
				Main-Class: org.apache.juneau.microservice.MyApp
				Implementation-Version: 10.0.0
				Build-Time: 2026-06-17T00:00:00Z
				Git-Commit-Id: abc1234
				""";
		return new ManifestFile(new StringReader(mf));
	}

	// =================================================================================
	// A. InfoManager worker
	// =================================================================================

	@Test void a01_getInfoRendersAttributes() throws Exception {
		var m = new InfoManager();
		var info = m.getInfo(stampedManifest());
		assertEquals("10.0.0", info.getString("Implementation-Version"));
		assertEquals("abc1234", info.getString("Git-Commit-Id"));
		assertEquals("2026-06-17T00:00:00Z", info.getString("Build-Time"));
	}

	@Test void a02_getInfoNullManifestEmpty() {
		assertTrue(new InfoManager().getInfo(null).isEmpty());
	}

	@Test void a03_resolveNullContextNull() {
		assertNull(new InfoManager().resolveManifest(null));
	}

	// =================================================================================
	// B. Mixin flavor — manifest present
	// =================================================================================

	@Rest(mixins={InfoMixin.class})
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ManifestFile manifest() throws IOException { return stampedManifest(); }
	}

	@Test void b01_mixinRendersStampedFields() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		c.get("/info").accept("application/json").run().assertStatus(200)
			.assertContent().asString().isContains("Git-Commit-Id", "abc1234", "Implementation-Version", "10.0.0");
	}

	// =================================================================================
	// C. Degrade cleanly — no manifest bean yields an empty (but 200) payload
	// =================================================================================

	@Rest(mixins={InfoMixin.class})
	public static class B extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void c01_noManifestEmpty200() throws Exception {
		var c = MockRestClient.buildLax(B.class);
		c.get("/info").accept("application/json").run().assertStatus(200).assertContent().asString().is("{}");
	}

	// =================================================================================
	// D. Resource flavor
	// =================================================================================

	// A routed child resolves beans from its own bean store, so the manifest bean is declared on the child
	// subclass — mirroring the HealthResource child-flavor test precedent.
	@Rest(path="/info")
	public static class InfoChild extends InfoResource {
		@Bean public ManifestFile manifest() throws IOException { return stampedManifest(); }
	}

	@Rest(children={InfoChild.class})
	public static class D extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void d01_resourceRendersStampedFields() throws Exception {
		var c = MockRestClient.buildLax(D.class);
		c.get("/info").accept("application/json").run().assertStatus(200)
			.assertContent().asString().isContains("Git-Commit-Id", "abc1234");
	}
}
