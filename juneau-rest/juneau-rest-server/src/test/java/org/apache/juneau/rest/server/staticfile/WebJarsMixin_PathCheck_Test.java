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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class WebJarsMixin_PathCheck_Test extends TestBase {

	@Test void a01_canonicalPath_passes() {
		assertString("jquery/3.7.1/jquery.min.js", WebJarsMixin.checkedPath("jquery/3.7.1/jquery.min.js"));
	}

	@Test void a02_badPaths_areRejected() {
		for (var raw : new String[] {
				null, "", "/jquery/x.js", "a/../../x", "%2e%2e/x", "a/%2e%2e/b", "a/./b.js", "./a.js",
				"a//b.js", "a/", "a%2fb.js", "a%2Fb.js", "a%5cb.js", "a\\b.js", "a/%2e/b.js", "a%252fb.js", "a/%zz.js" })
			assertNull(WebJarsMixin.checkedPath(raw), () -> "accepted: " + raw);
	}

	@Test void a03_encodedButSafe_isDecoded() {
		assertString("a b/c+d.js", WebJarsMixin.checkedPath("a%20b/c+d.js"));
	}

	@Test void b01_contentType_byExtension_ignoringCase() {
		assertString("text/javascript;charset=utf-8", WebJarsMixin.contentType("a/B.JS"));
		assertString("text/css;charset=utf-8", WebJarsMixin.contentType("a/b.css"));
		assertString("application/json", WebJarsMixin.contentType("a/b.min.js.map"));
		assertString("font/woff2", WebJarsMixin.contentType("a/f.woff2"));
		assertString("font/woff", WebJarsMixin.contentType("a/f.woff"));
		assertString("font/ttf", WebJarsMixin.contentType("a/f.ttf"));
		assertString("image/svg+xml", WebJarsMixin.contentType("a/i.svg"));
		assertString("image/png", WebJarsMixin.contentType("a/i.png"));
	}

	@Test void b02_contentType_otherExtensions_areNull() {
		for (var p : new String[] { "a/README.md", "a/LICENSE", "a/b.txt", "a/b.class", "a/1.0", "a/b.properties" })
			assertNull(WebJarsMixin.contentType(p), () -> p);
	}
}
