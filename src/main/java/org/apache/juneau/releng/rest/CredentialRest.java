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

import java.util.List;
import org.apache.juneau.commons.inject.Bean;
import org.apache.juneau.http.Content;
import org.apache.juneau.http.Path;
import org.apache.juneau.rest.server.Rest;
import org.apache.juneau.rest.server.RestDelete;
import org.apache.juneau.rest.server.RestGet;
import org.apache.juneau.rest.server.RestPost;
import org.apache.juneau.rest.server.servlet.BasicRestResource;
import org.apache.juneau.rest.server.view.View;
import org.apache.juneau.rest.server.view.freemarker.FreemarkerMixin;
import org.apache.juneau.rest.server.view.freemarker.FreemarkerView;
import org.apache.juneau.rest.server.view.freemarker.FreemarkerViewRenderer;
import org.apache.juneau.releng.credential.CredentialService;
import org.apache.juneau.releng.credential.CredentialStatus;
import org.apache.juneau.releng.credential.Validator.ValidationResult;

/** Credentials tab: store + live-validate Apache/GPG/GitHub secrets. Never returns secret values. */
@Rest(path = "/credentials", title = "Credentials", responseProcessors = FreemarkerViewRenderer.class)
public class CredentialRest extends BasicRestResource {

	private final CredentialService service;

	public CredentialRest(CredentialService service) {
		this.service = service;
	}

	@Bean
	public FreemarkerMixin freemarker() {
		return FreemarkerMixin.create().basePath("/templates/").templateSuffix(".ftlh").build();
	}

	/** Human page. */
	@RestGet("/")
	public View page() {
		return FreemarkerView.of("credentials").attr("credentials", service.status());
	}

	/** JSON status for all credentials (no secrets). */
	@RestGet("/status")
	public List<CredentialStatus> status() {
		return service.status();
	}

	/** Store/update a credential. Body: {account?, secret}. Apache/GPG send account (availid/keyId). */
	@RestPost("/{name}")
	public CredentialStatus set(@Path("name") String name, @Content StoreRequest body) {
		service.store(name, body.account, body.secret);
		return service.status().stream().filter(c -> c.name.equals(name)).findFirst().orElseThrow();
	}

	/** Run the live validation. */
	@RestPost("/{name}/validate")
	public ValidationResult validate(@Path("name") String name) {
		return service.validate(name);
	}

	/** Remove a credential from the Keychain. */
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
