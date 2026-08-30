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
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class NexusStagingClientTest {

	/** The client is built around a String->String "send JSON request, get JSON response" seam we can stub. */
	@Test
	void a01_discoversMostRecentOpenRepoForProfile() {
		var json = "[{\"repositoryId\":\"orgapachejuneau-1041\",\"type\":\"closed\",\"created\":\"2026-08-10\"},"
				+ "{\"repositoryId\":\"orgapachejuneau-1042\",\"type\":\"open\",\"created\":\"2026-08-14\"}]";
		var client = NexusStagingClient.forTests((method, path, body) -> json);
		var repo = client.findLatestRepo().orElseThrow();
		assertEquals("orgapachejuneau-1042", repo.id);
		assertEquals("open", repo.status);
	}

	@Test
	void b01_closeIssuesCloseCallForRepoId() {
		var calls = new ArrayList<String>();
		var client = NexusStagingClient.forTests((method, path, body) -> {
			calls.add(method + " " + path);
			return "";
		});
		client.close("orgapachejuneau-1042");
		assertTrue(calls.stream().anyMatch(c -> c.startsWith("POST") && c.contains("close")));
	}

	@Test
	void b02_dropIssuesDropCall() {
		var calls = new ArrayList<String>();
		var client = NexusStagingClient.forTests((method, path, body) -> {
			calls.add(method + " " + path);
			return "";
		});
		client.drop("orgapachejuneau-1042");
		assertTrue(calls.stream().anyMatch(c -> c.contains("drop")));
	}

	@Test
	void b03_promoteIssuesReleaseCall() {
		var calls = new ArrayList<String>();
		var client = NexusStagingClient.forTests((method, path, body) -> {
			calls.add(method + " " + path);
			return "";
		});
		client.promote("orgapachejuneau-1042");
		assertTrue(calls.stream().anyMatch(c -> c.contains("promote") || c.contains("release")));
	}

	/**
	 * {@code create(username, password)} (the Keychain-backed path) builds a usable client from
	 * credentials supplied directly — no {@code ~/.m2/settings.xml} read, no network
	 * call made merely by constructing the client (Basic auth header is built eagerly; nothing is sent
	 * until a method like {@code close()}/{@code drop()} is actually invoked).
	 */
	@Test
	void c01_createWithDirectCredentialsNeedsNoSettingsXml() {
		assertNotNull(NexusStagingClient.create("https://repository.apache.org", NexusStagingClient.JUNEAU_PROFILE_ID,
				"jbognar", "s3cr3t"));
	}
}
