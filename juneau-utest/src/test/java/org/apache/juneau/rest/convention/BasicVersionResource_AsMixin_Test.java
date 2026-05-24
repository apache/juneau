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
package org.apache.juneau.rest.convention;

import java.util.*;
import java.util.jar.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link BasicVersionResource} mounted as a mixin via {@code @Rest(mixins=...)} on a
 * vanilla {@link RestServlet}.
 *
 * <p>
 * Cases:
 * <ul>
 * 	<li>{@code /version}, {@code /info}, {@code /about} all return the same JSON map.
 * 	<li>{@code Content-Type: application/json}.
 * 	<li>Importer's {@code @Bean BasicVersionResource} factory drives the entries map (manifest
 * 		read, programmatic entries, custom Manifest).
 * 	<li>Missing manifest gracefully resolves to {@code (unknown)}.
 * </ul>
 *
 * @since 9.5.0
 */
class BasicVersionResource_AsMixin_Test extends TestBase {

	@Rest(mixins=BasicVersionResource.class)
	public static class A extends RestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	private static final MockRestClient ca = MockRestClient.buildLax(A.class);

	@Test void a01_versionEndpointServed() throws Exception {
		ca.get("/version")
			.run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("application/json");
	}

	@Test void a02_versionInfoAboutAreSynonyms() throws Exception {
		var v = ca.get("/version").run().assertStatus(200).getContent().asString();
		var i = ca.get("/info").run().assertStatus(200).getContent().asString();
		var ab = ca.get("/about").run().assertStatus(200).getContent().asString();
		Assertions.assertEquals(v, i, "/version and /info must be synonyms");
		Assertions.assertEquals(v, ab, "/version and /about must be synonyms");
	}

	@Test void a03_defaultPayloadHasJavaVersionAtMinimum() throws Exception {
		var body = ca.get("/version").run().assertStatus(200).getContent().asString();
		var parsed = JsonParser.DEFAULT.parse(body, Map.class);
		Assertions.assertNotNull(parsed.get("javaVersion"), "javaVersion present");
	}

	@Test void a04_hostEndpointStillReachable() throws Exception {
		ca.get("/items").run().assertStatus(200).assertContent().asString().isContains("items");
	}

	/** Host providing a programmatic version map via @Bean factory. */
	@Rest(mixins=BasicVersionResource.class)
	public static class B extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public BasicVersionResource version() {
			return BasicVersionResource.create()
				.entry("name", "my-app")
				.entry("version", "1.2.3")
				.entry("gitCommit", "abc123")
				.entry("gitBranch", "main")
				.entry("buildTime", "2026-05-24T18:00:00Z")
				.fromJavaVersion()
				.build();
		}
	}

	private static final MockRestClient cb = MockRestClient.buildLax(B.class);

	@Test void b01_programmaticEntriesSurface() throws Exception {
		var body = cb.get("/version").run().assertStatus(200).getContent().asString();
		var parsed = JsonParser.DEFAULT.parse(body, Map.class);
		Assertions.assertEquals("my-app", parsed.get("name"));
		Assertions.assertEquals("1.2.3", parsed.get("version"));
		Assertions.assertEquals("abc123", parsed.get("gitCommit"));
		Assertions.assertEquals("main", parsed.get("gitBranch"));
		Assertions.assertEquals("2026-05-24T18:00:00Z", parsed.get("buildTime"));
		Assertions.assertNotNull(parsed.get("javaVersion"));
	}

	/** Host providing a synthetic Manifest via fromManifest(Manifest). */
	@Rest(mixins=BasicVersionResource.class)
	public static class C extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public BasicVersionResource version() {
			var manifest = new Manifest();
			var attrs = manifest.getMainAttributes();
			attrs.putValue("Manifest-Version", "1.0");
			attrs.putValue("Implementation-Title", "synthetic-app");
			attrs.putValue("Implementation-Version", "9.9.9");
			attrs.putValue("Implementation-Vendor", "Acme");
			attrs.putValue("Build-Jdk", "21.0.0");
			return BasicVersionResource.create().fromManifest(manifest).build();
		}
	}

	private static final MockRestClient cc = MockRestClient.buildLax(C.class);

	@Test void c01_syntheticManifestSurfaces() throws Exception {
		var body = cc.get("/version").run().assertStatus(200).getContent().asString();
		var parsed = JsonParser.DEFAULT.parse(body, Map.class);
		Assertions.assertEquals("synthetic-app", parsed.get("name"));
		Assertions.assertEquals("9.9.9", parsed.get("version"));
		Assertions.assertEquals("Acme", parsed.get("vendor"));
		Assertions.assertEquals("21.0.0", parsed.get("javaVersion"));
	}

	@Test void d01_missingManifestGracefulFallback() {
		var v = BasicVersionResource.create()
			.fromManifest(new java.net.URLClassLoader(new java.net.URL[0], null))
			.build();
		Assertions.assertEquals(BasicVersionResource.UNKNOWN, v.getInfoMap().get("name"),
			"missing manifest must yield (unknown) name");
		Assertions.assertEquals(BasicVersionResource.UNKNOWN, v.getInfoMap().get("version"),
			"missing manifest must yield (unknown) version");
	}
}
