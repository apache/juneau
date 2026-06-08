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
package org.apache.juneau.rest.server.docs;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.config.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link SwaggerMixin} composed onto a vanilla {@link RestServlet} — i.e. used
 * outside of the {@code BasicRestServlet} chain.
 *
 * <p>
 * The mixin's {@code @RestOp} methods are pure POJOs that inherit serializers/parsers from the
 * host's {@code RestContext} via the FINISHED-81 sub-context model. This test demonstrates that a
 * minimal host (vanilla {@link RestResource} + {@link BasicUniversalConfig}) is sufficient to mount
 * the {@code /api} endpoint without dragging in the rest of the {@code BasicRestOperations}
 * surface.
 */
@SuppressWarnings({
	"resource" // Static MockRestClient fields are a common test pattern; resources are managed by the mock framework.
})
class SwaggerMixin_Standalone_Test extends TestBase {

	@Rest(mixins=SwaggerMixin.class)
	public static class A extends RestResource implements BasicUniversalConfig {}

	private static final MockRestClient c = MockRestClient.buildLax(A.class);

	@Test void a01_apiServesSwaggerSpec_jsonAccept() throws Exception {
		c.get("/api")
			.accept("application/json")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("\"swagger\":\"2.0\"");
	}

	@Test void a02_apiServesSwaggerUi_htmlAccept() throws Exception {
		c.get("/api")
			.accept("text/html")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("<html");
	}
}
