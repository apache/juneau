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
import org.apache.juneau.rest.server.Rest;
import org.apache.juneau.rest.server.RestGet;
import org.apache.juneau.rest.server.servlet.BasicRestResource;
import org.apache.juneau.rest.server.view.View;
import org.apache.juneau.rest.server.view.freemarker.FreemarkerMixin;
import org.apache.juneau.rest.server.view.freemarker.FreemarkerView;
import org.apache.juneau.rest.server.view.freemarker.FreemarkerViewRenderer;
import org.apache.juneau.releng.release.Release;
import org.apache.juneau.releng.release.ReleaseListService;

/** Releases tab: server-rendered HTML page + JSON data endpoint. */
@Rest(path = "/releases", title = "Releases", responseProcessors = FreemarkerViewRenderer.class)
public class ReleaseRest extends BasicRestResource {

	private final ReleaseListService service;

	public ReleaseRest(ReleaseListService service) {
		this.service = service;
	}

	@Bean
	public FreemarkerMixin freemarker() {
		return FreemarkerMixin.create().basePath("/templates/").templateSuffix(".ftlh").build();
	}

	/** Human page — server-rendered releases table, enhanced by DataTables. */
	@RestGet("/")
	public View page() {
		return FreemarkerView.of("releases").attr("releases", service.list());
	}

	/** Machine endpoint — bare JSON array for curl/CLI/DataTables ajax. */
	@RestGet("/data")
	public List<Release> data() {
		return service.list();
	}
}
