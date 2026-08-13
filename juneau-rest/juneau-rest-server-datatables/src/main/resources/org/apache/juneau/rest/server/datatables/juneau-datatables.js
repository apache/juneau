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

/*
 * juneau-datatables.js - first-party glue for the Apache Juneau DataTables integration.
 *
 * This is the ONLY DataTables-related asset Juneau ships.  The DataTables library itself (jQuery + the DataTables
 * JS/CSS) is NOT bundled here - its license is not an ASF category-A license - so it must be supplied by the caller,
 * either from a CDN or self-hosted.  This script only auto-initializes tables Juneau already rendered.
 *
 * It scans for any <table data-juneau-datatable> element (e.g. one produced by
 * org.apache.juneau.rest.server.datatables.DataTablesTable) and calls jQuery DataTables on it, reading optional
 * init options from the attribute value as JSON.  It is a no-op when jQuery / DataTables are absent, and is
 * idempotent (already-initialized tables are skipped).
 */
(function () {
	"use strict";

	function initAll() {
		var $ = window.jQuery;
		if (!$ || !$.fn || !$.fn.DataTable) {
			return;
		}
		$("table[data-juneau-datatable]").each(function () {
			if ($.fn.dataTable.isDataTable(this)) {
				return;
			}
			var raw = this.getAttribute("data-juneau-datatable");
			var opts = {};
			if (raw) {
				try {
					opts = JSON.parse(raw);
				} catch (e) {
					opts = {};
				}
			}
			$(this).DataTable(opts);
		});
	}

	if (document.readyState === "loading") {
		document.addEventListener("DOMContentLoaded", initAll);
	} else {
		initAll();
	}
})();
