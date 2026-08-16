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

package org.apache.juneau.releng.rest;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.apache.juneau.commons.inject.StackOverlay;
import org.apache.juneau.http.response.NotFound;
import org.apache.juneau.releng.release.Release;
import org.apache.juneau.releng.release.ReleaseListService;
import org.apache.juneau.rest.mock.MockRestClient;
import org.junit.jupiter.api.Test;

class ReleaseRestTest {

	private ReleaseRest rest(List<Release> releases) {
		return new ReleaseRest(new ReleaseListService(List::of, List::of, () -> releases));
	}

	private static Release release(String version, String status) {
		return new Release(version, status, "state");
	}

	/**
	 * {@link MockRestClient#create(Object)} caches its {@code RestContext} per resource class, so a second
	 * {@code ReleaseRest} instance built with different test data would silently dispatch against the
	 * <em>first</em> instance ever created for this class in the JVM. Passing a (no-op) {@link StackOverlay}
	 * as the overriding bean store opts out of that cache — see {@code MockRestClient.Builder#overridingBeanStore}.
	 */
	@SuppressWarnings({
		"resource" // Caller owns and closes the returned MockRestClient (via try-with-resources); Eclipse JDT @Owning warning is by design.
	})
	private static MockRestClient client(ReleaseRest rest) {
		return MockRestClient.builder(rest).overridingBeanStore(new StackOverlay()).build();
	}

	@Test
	void detailReturnsAViewCarryingTheMatchingRelease() {
		var rest = rest(List.of(release("9.2.1", "RELEASED")));
		var view = rest.detail("9.2.1", "1");
		assertNotNull(view);
	}

	@Test
	void detailForAnUnknownVersionIs404() {
		var rest = rest(List.of(release("9.2.1", "RELEASED")));
		var ex = assertThrows(NotFound.class, () -> rest.detail("9.9.9", "1"));
		assertEquals(404, ex.getStatusCode());
	}

	/**
	 * Real HTTP dispatch (via {@code juneau-rest-mock}, in-process, no socket) through the two-segment
	 * {@code /{version}/{rc}} route, guarding against the exact failure this endpoint originally hit:
	 * the request fell through to {@code RestContext.handleNotFound} with a stray {@code 200} already set
	 * ("Invalid method response: 200") because no correctly declared op returned a renderable View for it.
	 * A direct call to {@link ReleaseRest#detail} alone wouldn't exercise Juneau's own path-matching/dispatch,
	 * so this is the layer that actually proves the route is reachable and renders.
	 */
	@Test
	void detailRendersOverRealHttpDispatch() throws Exception {
		try (var client = client(rest(List.of(release("9.2.1", "RELEASED"))))) {
			try (var resp = client.request("GET", "/9.2.1/1").run()) {
				assertEquals(200, resp.getStatusCode());
				var body = resp.getBodyAsString();
				assertTrue(body.contains("9.2.1"), "Expected the release version in the rendered page: " + body);
			}
		}
	}

	@Test
	void detailForAnUnknownVersionIs404OverRealHttpDispatch() throws Exception {
		try (var client = client(rest(List.of()))) {
			try (var resp = client.request("GET", "/9.9.9/1").run()) {
				assertEquals(404, resp.getStatusCode());
			}
		}
	}
}
