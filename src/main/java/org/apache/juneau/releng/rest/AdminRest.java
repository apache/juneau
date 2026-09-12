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

import org.apache.juneau.commons.inject.Bean;
import org.apache.juneau.http.response.Found;
import org.apache.juneau.rest.server.Rest;
import org.apache.juneau.rest.server.RestGet;
import org.apache.juneau.rest.server.RestRequest;
import org.apache.juneau.rest.server.servlet.BasicRestResource;
import org.apache.juneau.rest.server.view.View;
import org.apache.juneau.rest.server.view.freemarker.FreemarkerMixin;
import org.apache.juneau.rest.server.view.freemarker.FreemarkerViewRenderer;
import org.apache.juneau.rest.server.view.freemarker.console.ConsoleFreemarkerMixin;
import org.apache.juneau.rest.server.views.ViewsMixin;

/**
 * Admin tab: author-HTML pair pages that mount the app's existing
 * {@link ReleaseRest#releasesView() Releases} and {@link CredentialRest#credentialsView() Credentials} tables
 * into empty slots via {@code JuneauViews.regions.mount}.
 *
 * <p>
 * Each pair is a full page load. {@code GET /} 302s to the Releases pair. Tables are fetched as
 * {@code ViewSlot} JSON from the owning resources; this resource does not emit {@code ViewTable} markup
 * or load {@code juneau-pages.js}.
 */
@Rest(path = "/admin", title = "Admin", responseProcessors = FreemarkerViewRenderer.class, mixins = ViewsMixin.class)
public class AdminRest extends BasicRestResource {

	static final String RELEASES_URL = "/rest/admin/releases";
	static final String CREDENTIALS_URL = "/rest/admin/credentials";

	// Return type stays FreemarkerMixin - FreemarkerViewRenderer does an exact-type bean lookup (see
	// ConsoleFreemarkerMixin's class Javadoc).
	@Bean
	public FreemarkerMixin freemarker() {
		return ConsoleFreemarkerMixin.create().basePath("/templates/").templateSuffix(".ftlh").build();
	}

	/** Default Admin URL — 302 to the Releases pair. */
	@RestGet("/")
	public Found redirectToReleases() {
		return found(RELEASES_URL);
	}

	/** Admin / Releases pair: empty {@code #releases} slot mounted from {@code /rest/releases/view}. */
	@RestGet("/releases")
	public View releases(RestRequest req) {
		return pairPage(req, "releases", ReleaseRest.MOUNT + "/view", "releases");
	}

	/** Admin / Credentials pair: empty {@code #credentials} slot mounted from {@code /rest/credentials/view}. */
	@RestGet("/credentials")
	public View credentials(RestRequest req) {
		return pairPage(req, "credentials", CredentialRest.MOUNT + "/view", "credentials");
	}

	private static View pairPage(RestRequest req, String slotId, String tableUrl, String selectedChild) {
		return TableSlotPage.of("admin", req, slotId, tableUrl).attr("selectedChild", selectedChild);
	}
}
