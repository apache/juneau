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
import org.apache.juneau.rest.server.Mutating;
import org.apache.juneau.rest.server.Rest;
import org.apache.juneau.rest.server.RestDelete;
import org.apache.juneau.rest.server.RestGet;
import org.apache.juneau.rest.server.RestPost;
import org.apache.juneau.rest.server.servlet.BasicRestResource;
import org.apache.juneau.rest.server.view.View;
import org.apache.juneau.rest.server.view.freemarker.FreemarkerMixin;
import org.apache.juneau.rest.server.view.freemarker.FreemarkerViewRenderer;
import org.apache.juneau.rest.server.view.freemarker.console.ConsoleFreemarkerMixin;
import org.apache.juneau.rest.server.views.Column;
import org.apache.juneau.rest.server.views.RibbonAction;
import org.apache.juneau.rest.server.views.ViewDef;
import org.apache.juneau.rest.server.views.ViewDef.DataMode;
import org.apache.juneau.rest.server.views.ViewDef.Dir;
import org.apache.juneau.releng.credential.CredentialService;
import org.apache.juneau.releng.credential.CredentialStatus;
import org.apache.juneau.releng.credential.Validator.ValidationResult;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Credentials tab: store + live-validate Apache/GPG/GitHub secrets. Never returns secret values.
 *
 * <p>{@code disableContentParam} is set because Juneau's default allows a {@code POST} body to arrive in a
 * {@code &content=} query parameter instead, and a secret that travels in a URL lands in browser history, in any
 * access log, and in the {@code Referer} of the next request. {@code LoopbackBoundary} already refuses that shape
 * from a hostile page (it carries no JSON content type), so this is not the attack control — it closes the accident
 * of a developer, a curl line or a copied URL doing it. See {@code CredentialWriteVectorTest}.
 */
@Rest(path = "/credentials", title = "Credentials", responseProcessors = FreemarkerViewRenderer.class,
	disableContentParam = "true")
public class CredentialRest extends BasicRestResource {

	/** This resource's absolute mount (RootRest {@code /rest/*} + {@code /credentials}), used by {@link #credentialsView()}. */
	static final String MOUNT = "/rest/credentials";

	private final CredentialService service;

	public CredentialRest(CredentialService service) {
		this.service = service;
	}

	/**
	 * The rich-view toolkit's declarative view of the Credentials list (TODO-399 Phase C dogfood): a second,
	 * independently-composable {@link ViewDef} alongside {@link ReleaseRest#releasesView()}, wired into the RM
	 * {@code Admin} tab page ({@code AdminRest}). Client-side data mode: {@link #status()} already returns the
	 * bare {@code List<CredentialStatus>} the toolkit's client-mode ajax (({@code dataSrc: ""})) expects, so no new
	 * server-side query wiring is needed.
	 */
	static ViewDef credentialsView() {
		return ViewDef.create("credentials")
			.rowType(CredentialStatus.class)
			.dataMode(DataMode.CLIENT)
			.dataUrl(MOUNT + "/status")
			.defaultOrder("name", Dir.ASC)
			.columns(
				Column.of("name").title("Name"),
				Column.of("label").title("Label"),
				Column.of("present").title("Present"),
				Column.of("lastValid").title("Valid"),
				Column.of("lastMessage").title("Message"))
			.ribbon(RibbonAction.refresh())
			.build();
	}

	// Return type stays FreemarkerMixin - FreemarkerViewRenderer does an exact-type bean lookup (see
	// ConsoleFreemarkerMixin's class Javadoc).
	@Bean
	public FreemarkerMixin freemarker() {
		return ConsoleFreemarkerMixin.create().basePath("/templates/").templateSuffix(".ftlh").build();
	}

	/** Human page. */
	@RestGet("/")
	public View page(HttpServletRequest req) {
		return ConsolePage.of("credentials", req).attr("credentials", service.status());
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
