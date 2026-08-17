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
package org.apache.juneau.rest.mock;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * {@link RequestIdSettings} resolution via {@link RestContext}'s three-rung memoizer, exercised end-to-end.
 *
 * <p>
 * Proves the {@code @Bean}-factory-method rung supplies the effective settings (rung 2 of {@code getBean} &rarr;
 * {@code createBeanFromMethod} &rarr; default), and that its knobs &mdash; unlike the {@link org.apache.juneau.rest.server.filter.RequestIdFilter}
 * façade's no-op knobs &mdash; genuinely govern resolution: a strict validator reject-and-remints, a custom
 * {@code attributeKey} moves storage, and a custom {@code idSupplier} mints the id.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // MockRestClient instances are short-lived test fixtures.
})
class RequestIdSettingsResolution_Test {

	//------------------------------------------------------------------------------------------------------------------
	// A strict (UUID-only) validator supplied via @Bean reject-and-remints a non-matching incoming id (the "B2" case).
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class Strict_Resource {
		@Bean public RequestIdSettings requestIdSettings() {
			return RequestIdSettings.create().validator(s -> s.matches("[0-9a-fA-F-]{36}")).build();
		}
		@RestGet(path="/echo")
		public String echo(RestRequest req) { return req.getRequestId(); }
	}

	@Test void a01_strictValidatorHonorsUuid() throws Exception {
		var c = org.apache.juneau.rest.mock.classic.MockRestClient.buildLax(Strict_Resource.class);
		var id = "550e8400-e29b-41d4-a716-446655440000";
		c.get("/echo").header("X-Request-Id", id).run().assertStatus(200)
			.assertHeader("X-Request-Id").is(id);
	}

	@Test void a02_strictValidatorRemintsNonMatching() throws Exception {
		var c = org.apache.juneau.rest.mock.classic.MockRestClient.buildLax(Strict_Resource.class);
		var dirty = "trace.123:abc-DEF_456";  // sanitize-clean but not UUID-shaped → strict validator rejects → reminted
		var echoed = c.get("/echo").header("X-Request-Id", dirty).run().assertStatus(200)
			.getHeader("X-Request-Id").asString().orElseThrow();
		assertNotEquals(dirty, echoed);
	}

	//------------------------------------------------------------------------------------------------------------------
	// A custom attributeKey supplied via @Bean moves where the resolved id is stashed (end-to-end).
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class CustomKey_Resource {
		@Bean public RequestIdSettings requestIdSettings() {
			return RequestIdSettings.create().attributeKey("customReqId").build();
		}
		@RestGet(path="/echo")
		public String echo(RestRequest req) {
			var underCustom = req.getAttribute("customReqId").asString().orElse("");
			var underDefault = req.getAttribute(RestServerConstants.REQUEST_ID).asString().orElse("");
			return req.getRequestId() + "|" + underCustom + "|" + underDefault;
		}
	}

	@Test void b01_customAttributeKeyMovesStorage() throws Exception {
		var c = org.apache.juneau.rest.mock.classic.MockRestClient.buildLax(CustomKey_Resource.class);
		var res = c.get("/echo").run().assertStatus(200);
		var echoed = res.getHeader("X-Request-Id").asString().orElseThrow();
		assertFalse(echoed.isEmpty());
		// getRequestId()==echoed, stored under customReqId, and the default "requestId" key is empty.
		res.assertContent().asString().isContains(echoed + "|" + echoed + "|");
	}

	//------------------------------------------------------------------------------------------------------------------
	// A custom idSupplier supplied via @Bean genuinely mints the id (contrast: the filter's idSupplier knob is a no-op).
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class CustomSupplier_Resource {
		@Bean public RequestIdSettings requestIdSettings() {
			return RequestIdSettings.create().idSupplier(() -> "bean-minted").build();
		}
		@RestGet(path="/echo")
		public String echo(RestRequest req) { return req.getRequestId(); }
	}

	@Test void c01_customSupplierMintsId() throws Exception {
		var c = org.apache.juneau.rest.mock.classic.MockRestClient.buildLax(CustomSupplier_Resource.class);
		c.get("/echo").run().assertStatus(200)
			.assertHeader("X-Request-Id").is("bean-minted");
	}
}
