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

import org.apache.juneau.commons.beanquery.InMemoryBeanQueryContext;
import org.apache.juneau.commons.beanquery.SearchType;
import org.apache.juneau.commons.inject.Bean;
import org.apache.juneau.http.Content;
import org.apache.juneau.releng.engine.AuditEntry;
import org.apache.juneau.releng.engine.AuditService;
import org.apache.juneau.rest.server.Rest;
import org.apache.juneau.rest.server.RestGet;
import org.apache.juneau.rest.server.RestPost;
import org.apache.juneau.rest.server.RestRequest;
import org.apache.juneau.rest.server.datatables.DataTablesMixin;
import org.apache.juneau.rest.server.datatables.DataTablesRequest;
import org.apache.juneau.rest.server.datatables.DataTablesResults;
import org.apache.juneau.rest.server.datatables.adapter.DataTablesQuery;
import org.apache.juneau.rest.server.servlet.BasicRestResource;
import org.apache.juneau.rest.server.staticfile.WebJarsMixin;
import org.apache.juneau.rest.server.view.View;
import org.apache.juneau.rest.server.view.freemarker.FreemarkerMixin;
import org.apache.juneau.rest.server.view.freemarker.FreemarkerViewRenderer;
import org.apache.juneau.rest.server.view.freemarker.console.ConsoleFreemarkerMixin;
import org.apache.juneau.rest.server.views.ViewsMixin;

/**
 * Audit tab: a read-only, server-paged list of what the release engine has already recorded &mdash; each run's events
 * and each dropped release candidate.
 *
 * <p>
 * {@link #page(RestRequest)} serves the page shell (the table catalog is authored in {@code audit.ftlh}), and
 * {@link #query(DataTablesRequest)} serves the {@link DataTablesResults} envelope from a fresh query session over
 * {@link AuditService#list()}. Nothing here writes.
 */
@Rest(path = "/audit", title = "Audit", responseProcessors = FreemarkerViewRenderer.class, mixins = { ViewsMixin.class, DataTablesMixin.class, WebJarsMixin.class })
public class AuditRest extends BasicRestResource {

	/**
	 * This resource's page id, shared by {@link #page(RestRequest)}'s template name and the FTL card id.
	 */
	static final String NAME = "audit";

	private final AuditService service;

	/**
	 * The query schema for {@link #query(DataTablesRequest)}: immutable, built once over {@link AuditEntry}; each request
	 * opens a fresh session over {@link AuditService#list()}. The column names match the {@code data} keys of the FTL
	 * catalog.
	 */
	private final InMemoryBeanQueryContext<AuditEntry> queryContext = InMemoryBeanQueryContext.create(AuditEntry.class)
		.column("run", SearchType.VERSION)
		.build();

	/**
	 * Wires the audit service backing {@link #query(DataTablesRequest)}.
	 */
	public AuditRest(AuditService service) {
		this.service = service;
	}

	/**
	 * The Freemarker mixin used to render this resource's templates.
	 */
	// Return type stays FreemarkerMixin - FreemarkerViewRenderer does an exact-type bean lookup (see
	// ConsoleFreemarkerMixin's class Javadoc).
	@Bean
	public FreemarkerMixin freemarker() {
		var builder = ConsoleFreemarkerMixin.create();
		builder.adopterAssets(getClass().getClassLoader(), "static");
		builder.basePath("/templates/").templateSuffix(".ftlh");
		return builder.build();
	}

	/**
	 * Human page &mdash; the Audit shell; the table catalog is authored in {@code audit.ftlh} and mounted client-side.
	 */
	@RestGet("/")
	public View page(RestRequest req) {
		return ConsolePage.of(NAME, req);
	}

	/**
	 * Machine endpoint &mdash; the DataTables server-side-processing envelope over the audit rows.
	 *
	 * @param request The DataTables request body.
	 * @return The DataTables results envelope.
	 */
	@RestPost("/query")
	public DataTablesResults<AuditEntry> query(@Content DataTablesRequest request) {
		try (var session = queryContext.getSession(service.list())) {
			return DataTablesQuery.run(request, session);
		}
	}
}
