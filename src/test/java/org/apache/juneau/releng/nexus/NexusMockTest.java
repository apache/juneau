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

import org.junit.jupiter.api.Test;

/**
 * Tier-A fidelity: the real {@link NexusStagingClient} drives the full discovery + lifecycle against the
 * in-app {@link NexusMockModel} through the same {@link NexusMockRest#route routing table} the SAFE servlet
 * uses — an in-process round-trip with no HTTP server (mirrors {@code ReleaseRunRestTest}'s no-mock-dep note).
 */
class NexusMockTest {

	private NexusStagingClient clientFor(NexusMockModel model) {
		return NexusStagingClient.forTests((m, p, b) -> NexusMockRest.route(model, m, p, b));
	}

	@Test
	void discoveryLazilySynthesizesOpenRepo() {
		var model = new NexusMockModel(NexusStagingClient.JUNEAU_PROFILE_ID);
		var repo = clientFor(model).findLatestRepo().orElseThrow();
		assertTrue(repo.id.startsWith("orgapachejuneau-"));
		assertEquals("open", repo.status);
	}

	@Test
	void openToClosedToReleasedTransitions() {
		var model = new NexusMockModel(NexusStagingClient.JUNEAU_PROFILE_ID);
		var client = clientFor(model);
		var id = client.findLatestRepo().orElseThrow().id;

		client.close(id);
		assertEquals("closed", client.getRepo(id).status);

		client.promote(id);
		assertEquals("released", client.getRepo(id).status);
	}

	@Test
	void openToDroppedTransition() {
		var model = new NexusMockModel(NexusStagingClient.JUNEAU_PROFILE_ID);
		var client = clientFor(model);
		var id = client.findLatestRepo().orElseThrow().id;

		client.drop(id);
		assertEquals("dropped", client.getRepo(id).status);
	}

	@Test
	void illegalTransitionSurfacesError() {
		var model = new NexusMockModel(NexusStagingClient.JUNEAU_PROFILE_ID);
		var client = clientFor(model);
		var id = client.findLatestRepo().orElseThrow().id;
		client.close(id);
		client.promote(id); // now RELEASED

		// Closing an already-released repo is an illegal transition; the mock rejects it.
		var ex = assertThrows(RuntimeException.class, () -> client.close(id));
		assertTrue(ex.getMessage().toLowerCase().contains("illegal") || ex.getMessage().contains("state"));
	}

	@Test
	void resetSynthesizesAFreshRepoForTheNextRun() {
		var model = new NexusMockModel(NexusStagingClient.JUNEAU_PROFILE_ID);
		var client = clientFor(model);
		var first = client.findLatestRepo().orElseThrow().id;
		client.drop(first);

		model.reset();
		var second = client.findLatestRepo().orElseThrow().id;
		assertNotEquals(first, second);
		assertEquals("open", client.getRepo(second).status);
	}
}
