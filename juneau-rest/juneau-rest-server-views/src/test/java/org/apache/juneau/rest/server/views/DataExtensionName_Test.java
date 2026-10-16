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

import static java.nio.charset.StandardCharsets.*;
import static org.apache.juneau.test.bct.BctAssertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;

/** {@code EXTENSION_NAME_RE} in juneau-data.js is the same literal as {@link BusWiringValidator#EXTENSION_NAME}. */
class DataExtensionName_Test extends TestBase {

	@Test void a01_sameLiteral() throws Exception {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.DATA_JS_RESOURCE)) {
			var js = new String(in.readAllBytes(), UTF_8);
			assertContains("const EXTENSION_NAME_RE = /" + BusWiringValidator.EXTENSION_NAME.pattern() + "/;", js);
		}
	}
}
