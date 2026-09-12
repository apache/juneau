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
import org.apache.juneau.marshall.marshaller.Json;
import org.apache.juneau.rest.server.RestRequest;
import org.apache.juneau.rest.server.view.freemarker.FreemarkerView;
import org.apache.juneau.rest.server.views.ViewsMixin;

/**
 * Shared empty-slot + toolkit-asset attrs for pages that mount a table via
 * {@code JuneauViews.regions.mount({ [slotId]: { table: url } })}.
 */
final class TableSlotPage {

	/** Static page script that reads {@code #rm-table-slot} and calls {@code regions.mount}. */
	static final String JS_URL = "/js/table-slot.js";

	private TableSlotPage() {}

	/**
	 * Seeds the named template with CSRF (via {@link ConsolePage}), slot hookup JSON, and toolkit asset URLs.
	 *
	 * @param template The FreeMarker template name.
	 * @param req The current request. Must not be {@code null}.
	 * @param slotId HTML id of the empty slot. Must not be blank.
	 * @param tableUrl Same-origin envelope URL. Must not be blank.
	 * @return The view; callers may add further attrs.
	 */
	static FreemarkerView of(String template, RestRequest req, String slotId, String tableUrl) {
		return ConsolePage.of(template, req)
			.attr("slotId", slotId)
			.attr("tableUrl", tableUrl)
			.attr("tableSlotJson", slotJson(slotId, tableUrl))
			.attr("tableSlotJsUrl", JS_URL)
			.attr("viewsCssUrl", asset(req, ViewsMixin.VIEWS_CSS_PATH))
			.attr("configCssUrl", asset(req, ViewsMixin.CONFIG_CSS_PATH))
			.attr("rendersJsUrl", asset(req, ViewsMixin.RENDERS_JS_PATH))
			.attr("iconsJsUrl", asset(req, ViewsMixin.ICONS_JS_PATH))
			.attr("ribbonJsUrl", asset(req, ViewsMixin.RIBBON_JS_PATH))
			.attr("viewsJsUrl", asset(req, ViewsMixin.VIEWS_JS_PATH))
			.attr("configJsUrl", asset(req, ViewsMixin.CONFIG_JS_PATH))
			.attr("regionsJsUrl", asset(req, ViewsMixin.REGIONS_JS_PATH))
			.attr("helpersJsUrl", asset(req, ViewsMixin.HELPERS_JS_PATH));
	}

	static String slotJson(String slotId, String tableUrl) {
		var m = new LinkedHashMap<String,String>();
		m.put("slotId", slotId);
		m.put("tableUrl", tableUrl);
		return Json.DEFAULT.write(m);
	}

	private static String asset(RestRequest req, String path) {
		return ViewsMixin.viewAssetUrl(req, path);
	}
}
