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
package org.apache.juneau.rest.server.console;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * C4 refactor safety net: the served {@code chrome.css} body, byte for byte, for the default mixin and for every
 * stock theme name. The static structural CSS prefix is asserted separately, so the golden holds only the token
 * blocks the theme code emits.
 */
@SuppressWarnings({
	"resource" // MockRestClient is a test-scoped client; fluent assertStatus returns this.
})
class ConsoleChromeMixin_Golden_Test extends TestBase {

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class DefaultHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() { return ConsoleChromeMixin.create().build(); }
	}

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class OpenHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() { return ConsoleChromeMixin.create().theme("open").build(); }
	}

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class LightRedHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() { return ConsoleChromeMixin.create().theme("light-red").build(); }
	}

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class LightBrownHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() { return ConsoleChromeMixin.create().theme("light-brown").build(); }
	}

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class RedHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() { return ConsoleChromeMixin.create().theme("red").build(); }
	}

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class GrayHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() { return ConsoleChromeMixin.create().theme("gray").build(); }
	}

	private static Class<?> hostFor(String name) {
		return switch (name) {
			case "default" -> DefaultHost.class;
			case "open" -> OpenHost.class;
			case "light-red" -> LightRedHost.class;
			case "light-brown" -> LightBrownHost.class;
			case "red" -> RedHost.class;
			case "gray" -> GrayHost.class;
			default -> throw new IllegalArgumentException(name);
		};
	}

	private static String staticChromeCss() throws IOException {
		try (var in = ConsoleChromeMixin_Golden_Test.class.getResourceAsStream("/org/apache/juneau/console/chrome.css")) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"default", "open", "light-red", "light-brown", "red", "gray"})
	void a01_servedChromeCss_matchesGolden(String name) throws Exception {
		var body = MockRestClient.buildLax(hostFor(name)).get(ConsoleChromeMixin.CHROME_CSS_PATH).run()
			.assertStatus(200).getContent().asString();
		var prefix = staticChromeCss();
		assertTrue(body.startsWith(prefix), () -> "served body no longer starts with the static chrome.css for " + name);
		GoldenFiles.assertGolden("chrome-css", name, body.substring(prefix.length()));
	}
}
