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
package org.apache.juneau.petstore.rest;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"resource" // Closeable test fixtures held in static fields; lifecycle managed by the test/framework, not a real leak.
})
class PetstoreUiResource_Test extends TestBase {

	private static final MockRestClient CLIENT = MockRestClient.buildLax(PetstoreUiResource.class);

	@Test void a01_uiHtml_servedFromClasspath() throws Exception {
		CLIENT.get("/static/petstore-ui.html")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("<title>Juneau Petstore — React UI</title>");
	}

	@Test void a02_uiHtml_referencesPetstoreEndpoint() throws Exception {
		// Sanity check that the SPA actually points at the right backend route — guards against
		// accidental rename of /petstore/pets without updating the SPA.
		CLIENT.get("/static/petstore-ui.html")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("/petstore/pets");
	}

	@Test void a03_unknownStaticFile_404() throws Exception {
		CLIENT.get("/static/no-such-file.html")
			.run()
			.assertStatus(404);
	}
}
