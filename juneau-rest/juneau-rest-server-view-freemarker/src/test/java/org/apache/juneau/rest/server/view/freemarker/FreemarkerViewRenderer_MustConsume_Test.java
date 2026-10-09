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
package org.apache.juneau.rest.server.view.freemarker;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.view.*;
import org.junit.jupiter.api.*;

/**
 * {@link FreemarkerView.MustConsume}: the renderer's post-render adoption check.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // MockRestClient/RestResponse are closed in try-with-resources; fluent run() returns this.
})
class FreemarkerViewRenderer_MustConsume_Test extends TestBase {

	static final class Stub implements FreemarkerView.MustConsume {
		private final boolean consumed;
		private final String label;

		Stub(String label, boolean consumed) {
			this.label = label;
			this.consumed = consumed;
		}

		@Override
		public boolean consumed() { return consumed; }

		@Override
		public String notConsumedMessage(String templateName) {
			return "Stub " + label + " was never adopted by '" + templateName + "'.";
		}
	}

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return FreemarkerMixin.create().basePath("/freemarker-templates/").build();
		}
		@RestGet(path="/plain")
		public View plain() { return FreemarkerView.of("about.ftlh"); }
		@RestGet(path="/consumed")
		public View consumed() { return FreemarkerView.of("about.ftlh").attr("s", new Stub("A", true)); }
		@RestGet(path="/unconsumed")
		public View unconsumed() { return FreemarkerView.of("about.ftlh").attr("s", new Stub("A", false)); }
		@RestGet(path="/two")
		public View two() {
			return FreemarkerView.of("about.ftlh").attr("ok", new Stub("OK", true)).attr("a", new Stub("FIRST", false))
				.attr("b", new Stub("SECOND", false));
		}
	}

	private static String body(String path, int status) throws Exception {
		try (var c = MockRestClient.buildLax(Host.class); var rsp = c.get(path).run()) {
			rsp.assertStatus(status);
			return rsp.getContent().asString();
		}
	}

	@Test void a01_noMustConsumeAttribute_rendersAsBefore() throws Exception {
		assertTrue(body("/plain", 200).contains("About Juneau"));
	}

	@Test void a02_consumed_renders200() throws Exception {
		assertTrue(body("/consumed", 200).contains("About Juneau"));
	}

	@Test void a03_unconsumed_is500WithExactMessage() throws Exception {
		assertTrue(body("/unconsumed", 500).contains("Stub A was never adopted by 'about.ftlh'."));
	}

	@Test void a04_unconsumed_writesNoneOfTheTemplate() throws Exception {
		assertFalse(body("/unconsumed", 500).contains("About Juneau"));
	}

	@Test void a05_twoUnconsumed_reportsTheFirstInAttributeOrder() throws Exception {
		var b = body("/two", 500);
		assertTrue(b.contains("Stub FIRST was never adopted"), b);
		assertFalse(b.contains("Stub SECOND"), b);
	}
}
