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

import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * C4 refactor safety net: the stock-theme {@code <link>} and the inline override {@code <style>} that
 * {@code <@console>}/{@code <@theme>}/{@code <@token>} render for each adopter's real theme block. The
 * {@code ?v=} cache-buster is normalized because it hashes the theme stylesheet, whose comment wording changes
 * in this sub-project.
 */
@SuppressWarnings({
	"java:S8786", // Test-only patterns scan small rendered pages; the scan-to-marker shape needs backtracking.
	"resource" // MockRestClient/RestResponse are closed in try-with-resources.
})
class ThemeOverride_Golden_Test extends TestBase {

	private static final Pattern THEME_LINK = Pattern.compile("<link[^>]*juneau-theme-[^>]*>");
	private static final Pattern OVERRIDE_STYLE = Pattern.compile("<style[^>]*>\\s*html:root\\{.*?</style>", Pattern.DOTALL);

	private static FreemarkerMixin mixin(String chrome) {
		return ConsoleFreemarkerMixin.create().basePath("/templates/").chromeTemplate("admin/" + chrome).build();
	}

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class SscHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() { return mixin("adopter-ssc.ftlh"); }
		@RestGet(path="/naked") public View naked() { return FreemarkerView.of("admin/page-naked.ftlh"); }
	}

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class JrmHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() { return mixin("adopter-jrm.ftlh"); }
		@RestGet(path="/naked") public View naked() { return FreemarkerView.of("admin/page-naked.ftlh"); }
	}

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class FoundryHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() { return mixin("adopter-foundry.ftlh"); }
		@RestGet(path="/naked") public View naked() { return FreemarkerView.of("admin/page-naked.ftlh"); }
	}

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class LeafAliasHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() { return mixin(LEAF_ALIAS_FIXTURE); }
		@RestGet(path="/naked") public View naked() { return FreemarkerView.of("admin/page-naked.ftlh"); }
	}

	static final String LEAF_ALIAS_FIXTURE = "console-chrome-theme-override.ftlh";

	private static Class<?> hostFor(String name) {
		return switch (name) {
			case "ssc" -> SscHost.class;
			case "jrm" -> JrmHost.class;
			case "foundry" -> FoundryHost.class;
			case "leaf-alias" -> LeafAliasHost.class;
			default -> throw new IllegalArgumentException(name);
		};
	}

	static String themeHead(String body) {
		var sb = new StringBuilder();
		var m = THEME_LINK.matcher(body);
		while (m.find())
			sb.append(m.group().replaceAll("\\?v=[^\"]*", "?v=*")).append('\n');
		m = OVERRIDE_STYLE.matcher(body);
		while (m.find())
			sb.append(m.group()).append('\n');
		return sb.toString();
	}

	@ParameterizedTest
	@ValueSource(strings = {"ssc", "jrm", "foundry", "leaf-alias"})
	void a01_renderedThemeHead_matchesGolden(String name) throws Exception {
		String body;
		try (var c = MockRestClient.buildLax(hostFor(name)); var rsp = c.get("/naked").run()) {
			rsp.assertStatus(200);
			body = rsp.getContent().asString();
		}
		GoldenFiles.assertGolden("theme-override", name, themeHead(body));
	}
}
