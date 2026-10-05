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
package org.apache.juneau.rest.server.views;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.test.assertions.*;
import org.junit.jupiter.api.*;

/**
 * Guards every served {@code org/apache/juneau/views/*.js} resource against a literal closing script tag.
 *
 * <p>
 * Host pages (and this module's own browser fixtures) may inline these files into an HTML {@code <script>} block.
 * The HTML parser ends that block at the first closing script tag it sees, even one inside a JavaScript comment
 * or string, which silently truncates the rest of the file.  Nothing fails loudly, the runtime just never finishes
 * loading, so this always-on check names each offending file and line instead of leaving it to a browser timeout.
 */
class ViewsJs_InlineScriptSafety_Test extends TestBase {

	@Test void b01_scriptsWereFound() throws Exception {
		var found = InlineScriptSafety.listResources(ViewsMixin.class, ViewsMixin.VIEWS_JS_RESOURCE, ".js");
		assertTrue(found.size() >= 10, "expected the views toolkit scripts, found: " + found);
	}

	@Test void b02_noServedScriptContainsAClosingScriptTag() throws Exception {
		InlineScriptSafety.assertNoScriptCloseTag(ViewsMixin.class, ViewsMixin.VIEWS_JS_RESOURCE, ".js");
	}
}
