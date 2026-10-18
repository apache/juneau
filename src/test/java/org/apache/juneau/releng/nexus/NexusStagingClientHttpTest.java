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

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Covers the real HTTP transport of {@link NexusStagingClient} against an in-process loopback server, plus
 * the JSON-shape handling in {@code findLatestRepo}/{@code getRepo}.
 */
class NexusStagingClientHttpTest {

	private static String repos(String... items) {
		return "[" + String.join(",", items) + "]";
	}

	@Test
	@SuppressWarnings({
		"resource" // HttpServer is stopped in finally block.
	})
	void a01_realTransportSendsAuthHeadersAndBody() throws Exception {
		var seen = new AtomicReference<String>();
		var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", ex -> {
			var body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			seen.set(ex.getRequestMethod() + " " + ex.getRequestURI() + " " + ex.getRequestHeaders().getFirst("Authorization")
				+ " " + ex.getRequestHeaders().getFirst("X-Extra") + " " + body);
			var out = repos("{\"repositoryId\":\"r1\",\"state\":\"open\",\"created\":\"2026-01-01\"}")
				.getBytes(StandardCharsets.UTF_8);
			ex.sendResponseHeaders(200, out.length);
			ex.getResponseBody().write(out);
			ex.close();
		});
		server.start();
		try {
			var base = "http://127.0.0.1:" + server.getAddress().getPort();
			var client = NexusStagingClient.create(base, "prof", "user", "pw", Map.of("X-Extra", "1"));
			var repo = client.findLatestRepo().orElseThrow();
			assertEquals("r1", repo.id);
			assertEquals("open", repo.status);
			assertTrue(seen.get().startsWith("GET /service/local/staging/profile_repositories/prof Basic "), seen.get());
			assertTrue(seen.get().contains(" 1 "), seen.get());
			client.close("r1");
			assertTrue(seen.get().startsWith("POST /service/local/staging/bulk/close "), seen.get());
			assertTrue(seen.get().contains("stagedRepositoryIds"), seen.get());
			// 4-arg overload (no extra headers)
			assertTrue(NexusStagingClient.create(base, "prof", "u", "p").findLatestRepo().isPresent());
		} finally {
			server.stop(0);
		}
	}

	@Test
	@SuppressWarnings({
		"resource" // HttpServer is stopped in finally block.
	})
	void a02_nonSuccessStatusFails() throws Exception {
		var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", ex -> {
			var out = "nope".getBytes(StandardCharsets.UTF_8);
			ex.sendResponseHeaders(500, out.length);
			ex.getResponseBody().write(out);
			ex.close();
		});
		server.start();
		try {
			var client = NexusStagingClient.create("http://127.0.0.1:" + server.getAddress().getPort(), "p", "u", "pw");
			var e = assertThrows(IllegalStateException.class, () -> client.drop("r1"));
			assertTrue(e.getMessage().contains("Nexus call failed"), e.getMessage());
			assertTrue(e.getCause().getMessage().contains("500"), e.getCause().getMessage());
		} finally {
			server.stop(0);
		}
	}

	@Test
	@SuppressWarnings({
		"resource" // NexusStagingClient is never closed; it does not manage resources that need closing.
	})
	void a03_unreachableServerFails() {
		var client = NexusStagingClient.create("http://127.0.0.1:1", "p", "u", "pw");
		assertThrows(IllegalStateException.class, () -> client.promote("r1"));
	}

	@Test
	void a04_interruptedCallFailsAndKeepsInterruptFlag() {
		var client = NexusStagingClient.create("http://127.0.0.1:1", "p", "u", "pw");
		Thread.currentThread().interrupt();
		try {
			assertThrows(IllegalStateException.class, () -> client.getRepo("r1"));
		} finally {
			Thread.interrupted();
		}
	}

	@Test
	void b01_emptyListYieldsEmpty() {
		assertTrue(NexusStagingClient.forTests((m, p, b) -> "").findLatestRepo().isEmpty());
	}

	@Test
	void b02_latestByCreatedWins_nullCreatedSortsLast_typeBeatsState() {
		var json = repos(
			"{\"repositoryId\":\"old\",\"type\":\"closed\",\"created\":\"2026-01-01\"}",
			"{\"repositoryId\":\"none\",\"state\":\"open\"}",
			"{\"repositoryId\":\"new\",\"type\":\"open\",\"state\":\"x\",\"created\":\"2026-03-01\",\"transitioning\":true}");
		var repo = NexusStagingClient.forTests((m, p, b) -> json, "prof").findLatestRepo().orElseThrow();
		assertEquals("new", repo.id);
		assertEquals("open", repo.status);
		assertTrue(repo.transitioning);
		assertEquals("prof", repo.profileId);
	}

	@Test
	void b03_getRepoParsesFields() {
		var json = "{\"repositoryId\":\"r\",\"profileId\":\"p\",\"state\":\"closed\",\"transitioning\":false}";
		var repo = NexusStagingClient.forTests((m, p, b) -> json).getRepo("r");
		assertEquals("r", repo.id);
		assertEquals("p", repo.profileId);
		assertEquals("closed", repo.status);
		assertFalse(repo.transitioning);
		var bare = NexusStagingClient.forTests((m, p, b) -> "{}").getRepo("r");
		assertNull(bare.id);
		assertNull(bare.status);
	}

	@Test
	void c01_stagingRepoConstructors() {
		var r = new StagingRepo("id", "open");
		assertEquals("id", r.id);
		assertEquals("open", r.status);
		assertNull(new StagingRepo().id);
	}
}
