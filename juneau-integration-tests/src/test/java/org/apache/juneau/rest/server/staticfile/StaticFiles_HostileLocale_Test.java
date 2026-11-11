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
package org.apache.juneau.rest.server.staticfile;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

/**
 * Verifies a hostile {@code Accept-Language} header (e.g. <c>..-..</c>) cannot read files outside the static root
 * nor turn into a server error: the request must produce a clean {@code 404}.
 *
 * @since 10.0.0
 */
class StaticFiles_HostileLocale_Test extends TestBase {

	private static Path root;

	@Rest
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@Bean public StaticFiles staticFiles(BeanStore bs) {
			return BasicStaticFiles.create(bs).dir(root.toString()).include(".*\\.txt").build();
		}
	}

	@BeforeAll static void setUp(@TempDir Path tmp) throws Exception {
		root = Files.createDirectories(tmp.resolve("root"));
		Files.writeString(root.resolve("inside.txt"), "inside-content");
		Files.writeString(tmp.resolve("OUTSIDE.txt"), "SECRET-OUTSIDE");
	}

	@Test void a01_hostileAcceptLanguage_returns404() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		var r = c.get("/htdocs/OUTSIDE.txt").header("Accept-Language", "..-..").run();
		assertEquals(404, r.getStatusLine().getStatusCode());
		assertFalse(r.getContent().asString().contains("SECRET-OUTSIDE"));
	}

	@Test void a02_control_noHeader_returns404() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		c.get("/htdocs/OUTSIDE.txt").run().assertStatus(404);
	}

	@Test void a03_legitFileServed() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		c.get("/htdocs/inside.txt").header("Accept-Language", "en-US").run()
			.assertStatus(200).assertContent().asString().isContains("inside-content");
	}
}
