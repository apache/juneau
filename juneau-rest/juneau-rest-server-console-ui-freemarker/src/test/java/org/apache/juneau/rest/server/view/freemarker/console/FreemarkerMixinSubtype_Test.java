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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.junit.jupiter.api.*;

/**
 * P8: {@code FreemarkerViewRenderer} finds a mixin bean declared as any registered subtype, and fails loud on two
 * different mixin beans.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // FreemarkerMixin/ConsoleFreemarkerMixin instances built in the @Bean fixtures and a01 are owned by the test host or discarded; nothing to close
})
class FreemarkerMixinSubtype_Test extends TestBase {

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class TwoBeansHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin plain() {
			return FreemarkerMixin.create().basePath("/templates/").build();
		}
		@Bean public ConsoleFreemarkerMixin console() {
			return (ConsoleFreemarkerMixin) ConsoleFreemarkerMixin.create().basePath("/templates/").build();
		}
		@RestGet(path="/x")
		public View x() {
			return FreemarkerView.of("c1/console-min.ftlh");
		}
	}

	@Test void a01_consoleSubtypeIsRegistered() {
		ConsoleFreemarkerMixin.create();  // forces class init
		assertTrue(FreemarkerMixin.registeredSubtypes().contains(ConsoleFreemarkerMixin.class));
	}

	@Test void a02_twoDifferentBeans_isAnError() throws Exception {
		try (var c = MockRestClient.buildLax(TwoBeansHost.class);
			var rsp = c.get("/x").run()) {
			rsp.assertStatus(500);
			var body = rsp.getContent().asString();
			assertTrue(body.contains("Found more than one FreemarkerMixin bean ('FreemarkerMixin' and 'ConsoleFreemarkerMixin'); declare exactly one."), () -> body);
		}
	}

	@Test void a03_registerSubtype_rejectsNull() {
		assertThrows(IllegalArgumentException.class, () -> FreemarkerMixin.registerSubtype(null));
	}
}
