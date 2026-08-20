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

import org.apache.juneau.commons.inject.Bean;
import org.apache.juneau.marshall.html.HtmlSerializer;
import org.apache.juneau.rest.server.Rest;
import org.apache.juneau.rest.server.RestGet;
import org.apache.juneau.rest.server.RestRequest;
import org.apache.juneau.rest.server.servlet.BasicRestResource;
import org.apache.juneau.rest.server.view.View;
import org.apache.juneau.rest.server.view.freemarker.FreemarkerMixin;
import org.apache.juneau.rest.server.view.freemarker.FreemarkerViewRenderer;
import org.apache.juneau.rest.server.view.freemarker.console.ConsoleFreemarkerMixin;
import org.apache.juneau.rest.server.views.PageDef;
import org.apache.juneau.rest.server.views.PageTable;
import org.apache.juneau.rest.server.views.Tab;
import org.apache.juneau.rest.server.views.ViewsMixin;

/**
 * Admin tab: a single multi-tab page composing the app's existing
 * {@link ReleaseRest#releasesView() Releases} and {@link CredentialRest#credentialsView() Credentials} rich views
 * into one {@link PageDef}, rendered by {@link PageTable}.
 *
 * <p>
 * This resource references each tab's child {@link org.apache.juneau.rest.server.views.ViewDef ViewDef} by calling
 * the sibling resources' declarative view-builder methods directly &mdash; it does not duplicate their column/ribbon
 * definitions, and each child view's {@code dataUrl} stays absolute (its owning resource's own mount), so the
 * ajax data draws still hit {@link ReleaseRest#data()} / {@link CredentialRest#status()} exactly as they do from
 * the standalone Releases/Credentials pages. Per {@link PageTable}'s contract, the emitted per-view markup (marker
 * table + VIEW_META sidecar) is byte-for-byte identical to what {@link ReleaseRest#page(RestRequest)} /
 * {@code CredentialRest}'s own view would emit standalone &mdash; this resource only adds the tab-bar/panel shell
 * and the PAGE_META sidecar around them.
 */
@Rest(path = "/admin", title = "Admin", responseProcessors = FreemarkerViewRenderer.class, mixins = ViewsMixin.class)
public class AdminRest extends BasicRestResource {

	// Return type stays FreemarkerMixin - FreemarkerViewRenderer does an exact-type bean lookup (see
	// ConsoleFreemarkerMixin's class Javadoc).
	@Bean
	public FreemarkerMixin freemarker() {
		return ConsoleFreemarkerMixin.create().basePath("/templates/").templateSuffix(".ftlh").build();
	}

	/**
	 * The composed page definition: one leaf tab per existing rich view. {@code build()} validates unique tab ids
	 * and unique referenced {@code ViewDef} ids across the page.
	 */
	static PageDef adminPage() {
		return PageDef.create("admin")
			.title("Admin")
			.tabs(
				Tab.create("releases", "Releases").view(ReleaseRest.releasesView()),
				Tab.create("credentials", "Credentials").view(CredentialRest.credentialsView()))
			.build();
	}

	/** Human page &mdash; the composed tab/sub-tab page shell (emitted as trusted markup) + PAGE_META sidecar. */
	@RestGet("/")
	public View page(RestRequest req) {
		var markup = HtmlSerializer.DEFAULT_SIMPLE_SQ.toString(PageTable.of(req, adminPage()));
		return ConsolePage.of("admin", req)
			.attr("pageTable", markup)
			.attr("viewsCssUrl", asset(req, ViewsMixin.VIEWS_CSS_PATH))
			.attr("configCssUrl", asset(req, ViewsMixin.CONFIG_CSS_PATH))
			.attr("rendersJsUrl", asset(req, ViewsMixin.RENDERS_JS_PATH))
			.attr("iconsJsUrl", asset(req, ViewsMixin.ICONS_JS_PATH))
			.attr("ribbonJsUrl", asset(req, ViewsMixin.RIBBON_JS_PATH))
			.attr("viewsJsUrl", asset(req, ViewsMixin.VIEWS_JS_PATH))
			.attr("configJsUrl", asset(req, ViewsMixin.CONFIG_JS_PATH))
			.attr("pagesJsUrl", asset(req, ViewsMixin.PAGES_JS_PATH));
	}

	/**
	 * Resolves a toolkit asset to an absolute, cache-busted URL for the FreeMarker head block via the
	 * request-aware {@link ViewsMixin#viewAssetUrl(RestRequest, String)} &mdash; resolved per-request
	 * against this resource's actual mount/context path, rather than a hardcoded {@code MOUNT} constant string-
	 * replace, so moving this resource no longer silently breaks asset loading.
	 */
	private static String asset(RestRequest req, String path) {
		return ViewsMixin.viewAssetUrl(req, path);
	}
}
