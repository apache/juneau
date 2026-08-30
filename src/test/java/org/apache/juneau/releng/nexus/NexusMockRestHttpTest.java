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

package org.apache.juneau.releng.nexus;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.apache.juneau.commons.inject.StackOverlay;
import org.apache.juneau.http.entity.StringBody;
import org.apache.juneau.marshall.marshaller.Json;
import org.apache.juneau.rest.mock.MockRestClient;
import org.junit.jupiter.api.Test;

/**
 * HTTP-level regression coverage for {@link NexusMockRest}: dispatches real requests through the same
 * servlet + response-processor pipeline the running app uses ({@link MockRestClient}, in-process, no
 * socket). {@link NexusMockTest} calls {@link NexusMockRest#route} directly and so never exercises how a
 * handler's return value gets serialized onto the wire — which is exactly the layer where the SAFE
 * {@code nexus-staging-close} walkthrough failed: the handlers returned already-serialized JSON text as a
 * plain {@code String}, so the framework serialized it a <em>second</em> time (quoting and escaping it)
 * before {@link NexusStagingClient} tried to parse it.
 */
class NexusMockRestHttpTest {

	/**
	 * {@link MockRestClient#create(Object)} caches its {@code RestContext} per resource class, so a second
	 * {@code NexusMockRest} instance built by another test would silently dispatch against the <em>first</em>
	 * instance (and its already-mutated model state) ever created for this class in the JVM. Passing a
	 * (no-op) {@link StackOverlay} as the overriding bean store opts out of that cache — see
	 * {@code MockRestClient.Builder#overridingBeanStore}.
	 */
	@SuppressWarnings({
		"resource" // Caller owns and closes the returned MockRestClient (via try-with-resources); Eclipse JDT @Owning warning is by design.
	})
	private MockRestClient client() {
		return MockRestClient.builder(new NexusMockRest(NexusStagingClient.JUNEAU_PROFILE_ID))
				.overridingBeanStore(new StackOverlay())
				.build();
	}

	/** Routes the real client through the mock servlet's actual HTTP dispatch (not {@link NexusMockRest#route}). */
	private NexusStagingClient.Transport overHttp(MockRestClient client) {
		return (method, path, body) -> {
			try {
				var req = client.request(method, path);
				if (body != null)
					req = req.body(StringBody.of(body, "application/json"));
				try (var resp = req.run()) {
					return resp.getBodyAsString();
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
	}

	@Test
	void a01_discoveryResponseIsARawJsonArrayNotADoubleEncodedString() throws Exception {
		try (var client = client()) {
			try (var resp = client
					.request("GET", "/service/local/staging/profile_repositories/" + NexusStagingClient.JUNEAU_PROFILE_ID)
					.run()) {
				assertEquals(200, resp.getStatusCode());
				var body = resp.getBodyAsString();
				// A double-encoded body starts with a quote ("[{...) instead of the array itself ([{...).
				assertTrue(body.startsWith("["), "Expected a raw JSON array, got double-encoded text: " + body);
				var parsed = Json.DEFAULT.read(body, List.class);
				assertEquals(1, parsed.size());
				var repo = (Map<?, ?>) parsed.get(0);
				assertEquals("open", repo.get("state"));
			}
		}
	}

	@Test
	void a02_nexusStagingClientDiscoverCloseAndReleaseRoundTripOverRealHttpDispatch() throws Exception {
		try (var client = client()) {
			var nexus = NexusStagingClient.forTests(overHttp(client), NexusStagingClient.JUNEAU_PROFILE_ID);

			var repo = nexus.findLatestRepo().orElseThrow();
			assertEquals("open", repo.status);

			nexus.close(repo.id);
			assertEquals("closed", nexus.getRepo(repo.id).status);

			nexus.promote(repo.id);
			assertEquals("released", nexus.getRepo(repo.id).status);
		}
	}
}
