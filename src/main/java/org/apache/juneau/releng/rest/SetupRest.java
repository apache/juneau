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
import org.apache.juneau.http.Path;
import org.apache.juneau.marshall.json.JsonSerializer;
import org.apache.juneau.releng.setup.SetupProbeService;
import org.apache.juneau.releng.setup.SetupProbeService.InstallResult;
import org.apache.juneau.releng.setup.SetupProbeService.SetupData;
import org.apache.juneau.rest.server.Mutating;
import org.apache.juneau.rest.server.Rest;
import org.apache.juneau.rest.server.RestGet;
import org.apache.juneau.rest.server.RestPost;
import org.apache.juneau.rest.server.servlet.BasicRestResource;
import org.apache.juneau.rest.server.view.View;
import org.apache.juneau.rest.server.view.freemarker.FreemarkerMixin;
import org.apache.juneau.rest.server.view.freemarker.FreemarkerViewRenderer;
import org.apache.juneau.rest.server.view.freemarker.console.ConsoleFreemarkerMixin;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Setup tab: probe inventory + Details. Install is a loopback-mutating local shell (CSRF + loopback
 * filter), not the LIVE arm phrase.
 */
@Rest(path = "/setup", title = "Setup", responseProcessors = FreemarkerViewRenderer.class)
public class SetupRest extends BasicRestResource {

	private final SetupProbeService setup;

	public SetupRest(SetupProbeService setup) {
		this.setup = setup;
	}

	// Return type stays FreemarkerMixin - FreemarkerViewRenderer does an exact-type bean lookup (see
	// ConsoleFreemarkerMixin's class Javadoc).
	@Bean
	public FreemarkerMixin freemarker() {
		return ConsoleFreemarkerMixin.create().basePath("/templates/").templateSuffix(".ftlh").build();
	}

	/** Human page: probe pills only; browser then GET {@code /data}. */
	@RestGet("/")
	public View page(HttpServletRequest req) {
		return ConsolePage.of("setup", req).attr("inventory", setup.inventory());
	}

	/** Eager verdicts. */
	@RestGet(path = "/data", produces = "application/json", serializers = JsonSerializer.class)
	public SetupData data() {
		return setup.data();
	}

	/** Install a PATH tool via brew or apt-get. */
	@Mutating("installs a local PATH prerequisite via brew or apt-get")
	@RestPost(path = "/install/{probeId}", produces = "application/json", serializers = JsonSerializer.class)
	public InstallResult install(@Path("probeId") String probeId) {
		return setup.install(probeId);
	}
}
