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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.juneau.marshall.marshaller.Json;

/**
 * In-process HTTPS client for the Apache Nexus staging suite. No CLI, no argv exposure — Basic auth built
 * in-process, normally from the committer's Keychain-backed Apache LDAP credentials (see
 * {@link #create(String, String, String, String)}), falling back to {@code ~/.m2/settings.xml}'s
 * {@code apache.releases.https} server entry (see {@link #create(String, String, String)}) only when that
 * Keychain entry is absent.
 */
public class NexusStagingClient {

	/** Test seam: (method, path, jsonBody) -> jsonResponse. */
	public interface Transport {
		String send(String method, String path, String body);
	}

	/** The default {@code org.apache.juneau} staging profile id. */
	public static final String JUNEAU_PROFILE_ID = "1a24bc7f954a70";

	private final Transport transport;
	private final String profileId;

	private NexusStagingClient(Transport transport, String profileId) {
		this.transport = transport;
		this.profileId = profileId;
	}

	/**
	 * Production factory: real HTTPS with Basic auth against {@code baseUrl}, credentials supplied directly —
	 * normally the committer's Apache LDAP availid + password, resolved through the
	 * {@link org.apache.juneau.commons.secret.SecretStore} SPI (see
	 * {@code AppConfiguration.secretResolver()}'s {@code nexus()}), not hardcoded to any particular backend
	 * here. Under SAFE mode {@code baseUrl} is the in-app loopback mock and the credential is a placeholder.
	 */
	public static NexusStagingClient create(String baseUrl, String profileId, String username, String password) {
		return create(baseUrl, profileId, username, password, Map.of());
	}

	/**
	 * As {@link #create(String, String, String, String)}, plus {@code extraHeaders} added to every request.
	 *
	 * <p>
	 * Exists for the SAFE-mode loopback mock. That mock is mounted on this application's own port, behind the
	 * {@link org.apache.juneau.rest.server.filter.LoopbackBoundary LoopbackBoundary}, which grants no exemption
	 * to a caller merely because it happens to be this process — so the close/drop/promote {@code POST}s below
	 * must present the same {@code Origin} and CSRF token the browser does. Pass
	 * {@link org.apache.juneau.rest.server.filter.LoopbackBoundary#selfCallHeaders() selfCallHeaders()} here.
	 * The real Nexus needs none of this and is given an empty map.
	 */
	public static NexusStagingClient create(String baseUrl, String profileId, String username, String password,
			Map<String, String> extraHeaders) {
		var http = HttpClient.newHttpClient();
		var basic = "Basic "
				+ Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
		return new NexusStagingClient(transport(http, basic, baseUrl, extraHeaders), profileId);
	}

	/**
	 * Fallback factory: credentials resolved from the named {@code ~/.m2/settings.xml} {@code <server>}
	 * entry ({@code serverId="apache.releases.https"}). Used only when the Keychain-backed
	 * {@link #create(String, String, String, String)} path has no stored Apache LDAP credential yet — see
	 * {@code AppConfiguration.secretResolver()}. Resolution mirrors how {@code mvn deploy}/the release
	 * plugin itself reads that same entry (a minimal local settings.xml parse, rather than pulling in the
	 * full Maven settings-builder dependency). Does not handle settings-security.xml-encrypted passwords
	 * (see {@link MavenSettingsCredentials}).
	 */
	public static NexusStagingClient create(String baseUrl, String profileId, String serverId) {
		var creds = MavenSettingsCredentials.resolve(serverId); // {username, password} from the <server> entry
		return create(baseUrl, profileId, creds.username(), creds.password());
	}

	private static Transport transport(HttpClient http, String basic, String baseUrl,
			Map<String, String> extraHeaders) {
		return (method, path, body) -> {
			try {
				var b = HttpRequest.newBuilder(URI.create(baseUrl + path)).header("Authorization", basic)
						.header("Accept", "application/json").header("Content-Type", "application/json");
				extraHeaders.forEach(b::header);
				var req = (body == null) ? b.method(method, HttpRequest.BodyPublishers.noBody()).build()
						: b.method(method, HttpRequest.BodyPublishers.ofString(body)).build();
				var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
				if (resp.statusCode() / 100 != 2)
					throw isex("Nexus %s %s -> %s: %s", method, path, resp.statusCode(), resp.body());
				return resp.body();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw isex(e, "Nexus call failed: %s %s", method, path);
			} catch (Exception e) {
				throw isex(e, "Nexus call failed: %s %s", method, path);
			}
		};
	}

	/** Test factory (default juneau profile id). */
	public static NexusStagingClient forTests(Transport transport) {
		return new NexusStagingClient(transport, JUNEAU_PROFILE_ID);
	}

	/** Test factory pinned to a specific profile id. */
	public static NexusStagingClient forTests(Transport transport, String profileId) {
		return new NexusStagingClient(transport, profileId);
	}

	/**
	 * Discover the most-recently-created staging repo for the juneau profile. Uses the profile-scoped
	 * endpoint ({@code /profile_repositories/{profileId}}) rather than the unscoped global list, since the
	 * account otherwise sees every ASF project's staging repos.
	 */
	@SuppressWarnings({ "unchecked" // Parsed JSON is assigned to its known generic shape (unchecked conversion from the raw parse result).
	})
	public Optional<StagingRepo> findLatestRepo() {
		var json = transport.send("GET", "/service/local/staging/profile_repositories/" + profileId, null);
		if (ib(json))
			return Optional.empty();
		List<Map<String, Object>> parsed = Json.DEFAULT.read(json, List.class);
		return parsed.stream().map(m -> {
			var r = new StagingRepo();
			r.id = str(m.get("repositoryId"));
			r.profileId = profileId;
			// The raw status field may be named "type" or "state" depending on the endpoint; fall back
			// to "state" when "type" is absent.
			r.status = str(m.getOrDefault("type", m.get("state")));
			r.transitioning = Boolean.TRUE.equals(m.get("transitioning"));
			r.created = str(m.get("created"));
			r.description = str(m.get("description"));
			return r;
		}).sorted(Comparator.comparing((StagingRepo r) -> r.created == null ? "" : r.created).reversed()).findFirst();
	}

	/** Single-repo detail read (state model OPEN/CLOSED/RELEASED + transitioning flag). */
	@SuppressWarnings({ "unchecked" // Parsed JSON is assigned to its known generic shape (unchecked conversion from the raw parse result).
	})
	public StagingRepo getRepo(String repoId) {
		var json = transport.send("GET", "/service/local/staging/repository/" + repoId, null);
		Map<String, Object> m = Json.DEFAULT.read(json, Map.class);
		var r = new StagingRepo();
		r.id = str(m.get("repositoryId"));
		r.profileId = str(m.get("profileId"));
		r.status = str(m.getOrDefault("type", m.get("state")));
		r.transitioning = Boolean.TRUE.equals(m.get("transitioning"));
		return r;
	}

	// Close and promote/release are separate, deliberate calls — no auto-release-on-close.
	public void close(String repoId) {
		transport.send("POST", "/service/local/staging/bulk/close", body(repoId));
	}

	public void drop(String repoId) {
		transport.send("POST", "/service/local/staging/bulk/drop", body(repoId));
	}

	public void promote(String repoId) {
		transport.send("POST", "/service/local/staging/bulk/promote", body(repoId));
	}

	private static String body(String repoId) {
		return "{\"data\":{\"stagedRepositoryIds\":[\"" + repoId + "\"],\"description\":\"juneau-release-manager\"}}";
	}

	private static String str(Object o) {
		return o == null ? null : String.valueOf(o);
	}
}
