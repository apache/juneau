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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

import freemarker.template.Configuration;
import freemarker.template.Template;

/** Tests for the {@code assetUrl(path)} adopter static-asset versioning hook ({@code Builder.adopterAssets}). */
class AssetUrl_Test extends TestBase {

	private static final String PROBE_CRC = "36b350df"; // CRC32 of src/test/resources/adopter-static/js/probe.js

	private static String render(ConsoleFreemarkerMixin mixin, String ftl) throws Exception {
		Configuration cfg = mixin.resolveConfiguration(request());
		var out = new StringWriter();
		new Template("t", ftl, cfg).process(null, out);
		return out.toString();
	}

	private static ConsoleFreemarkerMixin.Builder builder() {
		return ConsoleFreemarkerMixin.create().basePath("/templates/");
	}

	@Test void a01_bundledAsset_getsCrc32Token() throws Exception {
		var m = builder().adopterAssets(AssetUrl_Test.class.getClassLoader(), "adopter-static").build();
		assertEquals("/js/probe.js?v=" + PROBE_CRC, render(m, "${assetUrl('/js/probe.js')}"));
	}

	@Test void a02_rootSlashesOptional() throws Exception {
		var m = builder().adopterAssets(AssetUrl_Test.class.getClassLoader(), "/adopter-static/").build();
		assertEquals("/js/probe.js?v=" + PROBE_CRC, render(m, "${assetUrl('/js/probe.js')}"));
	}

	@Test void a03_unversionablePaths_passThrough() throws Exception {
		var m = builder().adopterAssets(AssetUrl_Test.class.getClassLoader(), "adopter-static").build();
		assertList(
			java.util.List.of(
				render(m, "${assetUrl('/js/missing.js')}"),
				render(m, "${assetUrl('/js/probe.js?v=1')}"),
				render(m, "${assetUrl('https://cdn.example.com/x.js')}")
			),
			"/js/missing.js", "/js/probe.js?v=1", "https://cdn.example.com/x.js");
	}

	@Test void a04_devModeMissing_stillPassesThrough() throws Exception {
		var m = builder().devMode(true).adopterAssets(AssetUrl_Test.class.getClassLoader(), "adopter-static").build();
		assertEquals("/js/missing.js", render(m, "${assetUrl('/js/missing.js')}"));
	}

	@Test void a05_notConfigured_functionAbsent() throws Exception {
		var cfg = builder().build().resolveConfiguration(request());
		assertNull(cfg.getSharedVariable(AssetUrlMethodModel.NAME));
	}

	@Test void a06_wrongArity_isRejected() throws Exception {
		var m = builder().adopterAssets(AssetUrl_Test.class.getClassLoader(), "adopter-static").build();
		assertThrows(Exception.class, () -> render(m, "${assetUrl()}"));
	}

	@Test void a07_versioned_isStableAcrossCalls() {
		var model = new AssetUrlMethodModel(AssetUrl_Test.class.getClassLoader(), "adopter-static", false);
		assertBean(java.util.Map.of("first", model.versioned("/js/probe.js"), "second", model.versioned("/js/probe.js")),
			"first,second", "/js/probe.js?v=" + PROBE_CRC + ",/js/probe.js?v=" + PROBE_CRC);
	}

	private static RestRequest request() throws Exception {
		MockRestClient.buildLax(Host.class).get("/x").run();
		return Host.CAPTURED.get();
	}

	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		static final ThreadLocal<RestRequest> CAPTURED = new ThreadLocal<>();
		@RestGet(path="/x")
		public String x(RestRequest req) {
			CAPTURED.set(req);
			return "x";
		}
	}
}
