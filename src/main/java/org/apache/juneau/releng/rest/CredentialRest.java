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

import static org.apache.juneau.http.HttpResponses.*;

import java.util.List;
import org.apache.juneau.http.Content;
import org.apache.juneau.http.Path;
import org.apache.juneau.http.response.NotFound;
import org.apache.juneau.rest.server.Mutating;
import org.apache.juneau.rest.server.Rest;
import org.apache.juneau.rest.server.RestDelete;
import org.apache.juneau.rest.server.RestGet;
import org.apache.juneau.rest.server.RestPost;
import org.apache.juneau.rest.server.servlet.BasicRestResource;
import org.apache.juneau.releng.credential.CredentialService;
import org.apache.juneau.releng.credential.CredentialStatus;
import org.apache.juneau.releng.credential.Validator.ValidationResult;

/**
 * Credential write APIs for Setup Details. The human page is gone (404).
 *
 * <p>{@code disableContentParam} is set because Juneau's default allows a {@code POST} body to arrive in a
 * {@code &content=} query parameter instead, and a secret that travels in a URL lands in browser history, in any
 * access log, and in the {@code Referer} of the next request. {@code LoopbackBoundary} already refuses that shape
 * from a hostile page (it carries no JSON content type), so this is not the attack control — it closes the accident
 * of a developer, a curl line or a copied URL doing it. See {@code CredentialWriteVectorTest}.
 */
@Rest(path = "/credentials", title = "Credentials", disableContentParam = "true")
public class CredentialRest extends BasicRestResource {

	/** This resource's absolute mount (RootRest {@code /rest/*} + {@code /credentials}). */
	static final String MOUNT = "/rest/credentials";

	private final CredentialService service;

	public CredentialRest(CredentialService service) {
		this.service = service;
	}

	/** Human page retired — bookmarks dead-end. */
	@RestGet("/")
	public NotFound pageGone() {
		return notFound();
	}

	/** Admin table envelope retired with the Admin Credentials child. */
	@RestGet("/view")
	public NotFound viewGone() {
		return notFound();
	}

	/** JSON status for all credentials (no secrets). */
	@RestGet("/status")
	public List<CredentialStatus> status() {
		return service.status();
	}

	/** Store/update a credential. Body: {account?, secret}. Apache/GPG send account (availid/keyId). */
	@Mutating("replaces a stored credential in the Keychain")
	@RestPost("/{name}")
	public CredentialStatus set(@Path("name") String name, @Content StoreRequest body) {
		service.store(name, body.account, body.secret);
		return service.status().stream().filter(c -> c.name.equals(name)).findFirst().orElseThrow();
	}

	/** Run the live validation. */
	@Mutating("caches a new validation verdict, and makes an authenticated call as the user")
	@RestPost("/{name}/validate")
	public ValidationResult validate(@Path("name") String name) {
		return service.validate(name);
	}

	/** Remove a credential from the Keychain. */
	@Mutating("deletes a stored credential from the Keychain")
	@RestDelete("/{name}")
	public CredentialStatus remove(@Path("name") String name) {
		service.delete(name);
		return service.status().stream().filter(c -> c.name.equals(name)).findFirst().orElseThrow();
	}

	/** POST body for storing a credential. */
	public static class StoreRequest {
		public String account; // availid (Apache) or key ID (GPG); ignored for GitHub
		public String secret;
	}
}
