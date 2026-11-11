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
package org.apache.juneau.rest;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.html.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.config.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

/**
 * Verifies that the stylesheet page-chrome entry is resolved and encoded like the sibling style/script entries.
 */
class HtmlDoc_Stylesheet_Test extends TestBase {

	private static final String HOSTILE = "x.css\"; } </style><script>alert(1)</script>";

	@Rest
	@HtmlDocConfig(stylesheet="$RQ{css}")
	public static class A implements BasicUniversalConfig {
		@RestGet
		public String x() { return "ok"; }
	}

	@Test void a01_requestVariableNotResolvedInStylesheet() throws Exception {
		var body = MockRestClient.buildLax(A.class).get("/x?css=" + java.net.URLEncoder.encode(HOSTILE, "UTF-8")).accept("text/html").run().getContent().asString();
		assertFalse(body.contains("alert(1)"), body);
	}

	@Rest
	@HtmlDocConfig(stylesheet="a.css\"; } </style><script>alert(2)</script>")
	public static class B implements BasicUniversalConfig {
		@RestGet
		public String x() { return "ok"; }
	}

	@Test void a02_literalValueEncodedInStylesheet() throws Exception {
		var body = MockRestClient.buildLax(B.class).get("/x").accept("text/html").run().getContent().asString();
		assertFalse(body.contains("alert(2)</script>"), body);
		assertTrue(body.contains("a.css%22;"), body);
	}
}
