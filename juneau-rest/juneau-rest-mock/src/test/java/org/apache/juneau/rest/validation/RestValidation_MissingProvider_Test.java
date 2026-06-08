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
package org.apache.juneau.rest.validation;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.validation.*;
import org.junit.jupiter.api.*;

import jakarta.validation.*;
import jakarta.validation.constraints.*;

/**
 * Graceful-degradation coverage for the missing-provider scenario.
 *
 * <p>
 * Simulates the &quot;{@code jakarta.validation-api} is on the classpath at compile time but no
 * concrete provider engine is reachable at runtime&quot; case &mdash; i.e. the deployment forgot to
 * ship Hibernate Validator (or an equivalent). The contract is: {@code @Valid} markers are still
 * detected (so arg resolvers don't crash), but the actual {@code validator.validate(...)} call is
 * silently skipped and the bean reaches the handler unchanged. A one-shot {@code WARNING} log is
 * emitted so misconfigured deployments are visible without flooding the log.
 *
 * <p>
 * Implementation note: We can't actually remove Hibernate Validator from the test classpath, so the
 * &quot;no provider&quot; state is forced via {@link BeanValidator#simulateProviderMissingForTesting()}
 * and reset between tests so other test classes aren't affected.
 */
@SuppressWarnings({
	"resource" // MockRestClient instances are short-lived test fixtures; the mock framework manages lifecycle.
})
class RestValidation_MissingProvider_Test extends TestBase {

	@AfterEach
	void resetValidatorCache() {
		BeanValidator.resetCachedDefaultForTesting();
	}

	public static class Payload {

		@NotBlank
		public String name;
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class A {

		@RestPost("/echo")
		public String echo(@Valid @Content Payload p) {
			return "ok:[" + (p.name == null ? "" : p.name) + "]";
		}
	}

	@Test
	void a01_missingProvider_violatingPayload_isPassedThrough() throws Exception {
		BeanValidator.simulateProviderMissingForTesting();
		var a = MockRestClient.buildLax(A.class);
		// Even though @Valid is present and the bean has a @NotBlank-violating field, with the provider
		// simulated as missing the validator is skipped and the bean reaches the handler unchanged.
		a.post("/echo", "{\"name\":\"\"}")
			.contentType("application/json")
			.run()
			.assertStatus(200)
			.assertContent("\"ok:[]\"");
	}

	@Test
	void a02_missingProvider_validPayload_alsoPassesThrough() throws Exception {
		BeanValidator.simulateProviderMissingForTesting();
		var a = MockRestClient.buildLax(A.class);
		// Sanity check &mdash; the missing-provider path also lets valid payloads through. We're verifying the
		// no-op behavior is symmetric (not biased toward letting only good requests through by coincidence).
		a.post("/echo", "{\"name\":\"alice\"}")
			.contentType("application/json")
			.run()
			.assertStatus(200)
			.assertContent("\"ok:[alice]\"");
	}
}
