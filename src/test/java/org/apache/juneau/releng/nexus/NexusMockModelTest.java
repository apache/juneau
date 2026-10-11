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

class NexusMockModelTest {

	private static final String CLOSE_BODY = "{\"data\":{\"stagedRepositoryIds\":[\"%s\"]}}";

	@Test
	void a01_unknownRepoIsRejected() {
		var m = new NexusMockModel("p");
		assertThrows(IllegalStateException.class, () -> m.repository("x"));
		assertThrows(IllegalStateException.class, () -> m.close("x"));
		m.profileRepositories();
		assertThrows(IllegalStateException.class, () -> m.repository("other"));
		assertThrows(IllegalStateException.class, () -> m.promote("other"));
		assertThrows(IllegalStateException.class, () -> m.drop("other"));
	}

	@Test
	void a02_repositoryViewIsReturnedForCurrentId() {
		var m = new NexusMockModel("prof");
		m.profileRepositories();
		var json = m.repository(m.currentRepoId());
		assertTrue(json.contains("\"profileId\":\"prof\""), json);
		assertTrue(json.contains("open"), json);
	}

	@Test
	void a03_illegalTransitionsAreRejected() {
		var m = new NexusMockModel("p");
		m.profileRepositories();
		var id = m.currentRepoId();
		// promote straight from open is illegal
		assertThrows(IllegalStateException.class, () -> m.promote(id));
		m.close(id);
		// close twice is illegal
		assertThrows(IllegalStateException.class, () -> m.close(id));
		// drop from closed is legal
		m.drop(id);
		assertEquals(NexusMockModel.DROPPED, m.currentState());
		// drop after drop is illegal
		assertThrows(IllegalStateException.class, () -> m.drop(id));
	}

	@Test
	void a04_releasedOrDroppedRepoIsReseeded() {
		var m = new NexusMockModel("p");
		m.profileRepositories();
		var first = m.currentRepoId();
		m.close(first);
		m.promote(first);
		assertEquals(NexusMockModel.RELEASED, m.currentState());
		m.profileRepositories();
		assertNotEquals(first, m.currentRepoId());
		assertEquals(NexusMockModel.OPEN, m.currentState());
	}

	@Test
	void b01_repoIdFromBody() {
		assertNull(NexusMockModel.repoIdFromBody(null));
		assertNull(NexusMockModel.repoIdFromBody(""));
		assertNull(NexusMockModel.repoIdFromBody("{}"));
		assertNull(NexusMockModel.repoIdFromBody("{\"data\":{\"stagedRepositoryIds\":[]}}"));
		assertEquals("abc", NexusMockModel.repoIdFromBody(CLOSE_BODY.formatted("abc")));
	}

	@Test
	void a05_droppedRepoIsReseeded() {
		var m = new NexusMockModel("p");
		m.profileRepositories();
		var first = m.currentRepoId();
		m.drop(first);
		m.profileRepositories();
		assertNotEquals(first, m.currentRepoId());
		m.reset();
		assertNull(m.currentRepoId());
		assertNull(m.currentState());
	}

	@Test
	void a06_openRepoIsReusedAcrossDiscoveryCalls() {
		var m = new NexusMockModel("p");
		m.profileRepositories();
		var first = m.currentRepoId();
		m.profileRepositories();
		assertEquals(first, m.currentRepoId());
		m.close(first);
		m.profileRepositories();
		assertEquals(first, m.currentRepoId());
	}
}
