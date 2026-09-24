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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.juneau.commons.inject.Bean;
import org.apache.juneau.http.Path;
import org.apache.juneau.http.response.NotFound;
import org.apache.juneau.marshall.json.JsonSerializer;
import org.apache.juneau.releng.release.Release;
import org.apache.juneau.releng.release.ReleaseListService;
import org.apache.juneau.rest.server.Rest;
import org.apache.juneau.rest.server.RestGet;
import org.apache.juneau.rest.server.RestRequest;
import org.apache.juneau.rest.server.converter.ProtocolQueryable;
import org.apache.juneau.rest.server.converter.QueryableSettings;
import org.apache.juneau.rest.server.datatables.DataTablesQueryProtocol;
import org.apache.juneau.rest.server.servlet.BasicRestResource;
import org.apache.juneau.rest.server.view.View;
import org.apache.juneau.rest.server.view.freemarker.FreemarkerMixin;
import org.apache.juneau.rest.server.view.freemarker.FreemarkerViewRenderer;
import org.apache.juneau.rest.server.view.freemarker.console.ConsoleFreemarkerMixin;
import org.apache.juneau.rest.server.views.ViewsMixin;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Releases tab: server-rendered HTML page + DataTables server-side-processing data endpoint.
 *
 * <p>
 * The table catalog (columns + renderers + ribbon + Detail View) is authored in FTL — the
 * {@code <@card type="datatables">} escape hatch in {@code releases.ftlh} — and mounted client-side by the
 * {@code juneau-page-cards.js} runtime; there is no Java {@code ViewDef}. {@link #page(RestRequest)} serves the
 * page shell, and {@link #data()} serves the {@code DataTablesResults} envelope via {@link ProtocolQueryable} +
 * {@link #queryableSettings()}. Toolkit assets are served by the composed {@link ViewsMixin} at this resource's
 * mount and pulled in by the page's {@code toolkit="views"} pack.
 */
@Rest(path = "/releases", title = "Releases", responseProcessors = FreemarkerViewRenderer.class, mixins = ViewsMixin.class)
public class ReleaseRest extends BasicRestResource {

	/**
	 * This resource's page id, shared by {@link #page(RestRequest)}'s template name and the FTL card id.
	 */
	static final String NAME = "releases";

	private final ReleaseListService service;

	/**
	 * Wires the release list service backing {@link #data()}.
	 */
	public ReleaseRest(ReleaseListService service) {
		this.service = service;
	}

	/**
	 * The Freemarker mixin used to render this resource's templates.
	 */
	// Return type stays FreemarkerMixin - FreemarkerViewRenderer does an exact-type bean lookup (see
	// ConsoleFreemarkerMixin's class Javadoc).
	@Bean
	public FreemarkerMixin freemarker() {
		return ConsoleFreemarkerMixin.create().basePath("/templates/").templateSuffix(".ftlh").build();
	}

	/**
	 * The DataTables server-side-processing settings for {@link #data()}: a {@link DataTablesQueryProtocol} bound to
	 * {@link Release} so the protocol's positional {@code columns[i]} resolution maps to the row bean's properties.
	 * The FTL {@code <@card type="datatables">} catalog declares the same column order client-side; this is the
	 * server-side half of that contract.
	 */
	@Bean
	public QueryableSettings queryableSettings() {
		return QueryableSettings.create().protocol(new DataTablesQueryProtocol(Release.class)).build();
	}

	/**
	 * Human page — the Releases shell; the table catalog is authored in {@code releases.ftlh} and mounted client-side.
	 */
	@RestGet("/")
	public View page(RestRequest req) {
		return ConsolePage.of(NAME, req);
	}

	/**
	 * Machine endpoint — the DataTables server-side-processing envelope ({@code {draw, recordsTotal, recordsFiltered,
	 * data}}). The method returns the row {@code List}; {@link ProtocolQueryable} parses the DataTables request, runs
	 * the shared query engine (search/sort/paginate), and wraps the page in a {@code DataTablesResults} envelope.
	 */
	@RestGet(path = "/data", converters = ProtocolQueryable.class)
	public List<Release> data() {
		return service.list();
	}

	/**
	 * Detail View expand GET — {@code {contractVersion, fields}} for one Releases row.
	 *
	 * @param id {@link Release#rowId()}.
	 * @return The expand envelope.
	 */
	@RestGet(path = "/expand/{id}", produces = "application/json", serializers = JsonSerializer.class)
	public Map<String, Object> expand(@Path("id") String id) {
		return detailEnvelope(findByRowId(id));
	}

	/**
	 * Human page — a single release's standalone detail view (bookmarkable). The Summary View version cell expands
	 * via {@link #expand(String)} rather than linking here directly.
	 * {@code rc} is the RC number (e.g. {@code 1} for RC1); it's not currently used to pick among multiple
	 * historical RCs of the same version (only one {@link Release} row exists per version today), but is
	 * part of the path so a future multi-RC history view doesn't need a URL-breaking change.
	 */
	@RestGet("/{version}/{rc}")
	public View detail(@Path("version") String version, @Path("rc") String rc, HttpServletRequest req) {
		var release = findByVersion(version);
		return ConsolePage.of("release-detail", req).attr("release", release).attr("rc", rc);
	}

	private Release findByVersion(String version) {
		return service.list().stream().filter(r -> version.equals(r.version)).findFirst()
			.orElseThrow(() -> new NotFound("No release found for %s", version));
	}

	private Release findByRowId(String id) {
		return service.list().stream().filter(r -> id.equals(r.rowId())).findFirst()
			.orElseThrow(() -> new NotFound("No release found for %s", id));
	}

	static Map<String, Object> detailEnvelope(Release r) {
		var fields = new LinkedHashMap<String, Object>();
		put(fields, "version", r.version);
		put(fields, "rc", r.rc);
		put(fields, "status", r.status);
		put(fields, "stage", r.stage);
		put(fields, "voteCloses", r.voteCloses);
		put(fields, "released", r.released);
		put(fields, "source", r.source);
		put(fields, "githubReleaseUrl", r.githubReleaseUrl);
		put(fields, "githubTagUrl", r.githubTagUrl());
		put(fields, "milestoneUrl", r.milestoneUrl);
		put(fields, "jiraVersionUrl", r.jiraVersionUrl());
		put(fields, "distUrl", r.distUrl());
		put(fields, "releaseNotesUrl", Release.RELEASE_NOTES_URL);
		var out = new LinkedHashMap<String, Object>();
		// The row-detail expand wire-contract version. A literal "1" (not RowDetailDef.CONTRACT_VERSION) so this
		// resource carries no compile dependency on the deprecated MOVE type — matches releases.ftlh's
		// detail.contractVersion. The juneau-regions.js populator handshake fails loud on a mismatch.
		out.put("contractVersion", "1");
		out.put("fields", fields);
		return out;
	}

	private static void put(Map<String, Object> fields, String key, String value) {
		if (value != null && !value.isBlank())
			fields.put(key, value);
	}
}
