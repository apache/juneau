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
 * either from a CDN or self-hosted.
 *
 * Two independent things live here:
 *
 *   1. window.JuneauDataTables.ajax(url, extra) - builds a DataTables `ajax` option that POSTs the server-side
 *      request as JSON to a @RestOp(method=POST) endpoint taking @Content DataTablesRequest, instead of DataTables'
 *      default flat form/query-parameter GET.  Pass the result as the `ajax` option: `new DataTable('#t', {
 *      serverSide: true, ajax: JuneauDataTables.ajax('/releases/query') })`.
 *
 *   2. initAll() - scans for <table data-juneau-datatable> (e.g. one produced by
 *      org.apache.juneau.rest.server.datatables.DataTablesTable) and calls jQuery DataTables on it, reading optional
 *      init options from the attribute value as JSON.  A table that also carries
 *      data-juneau-datatable-ajax="<url>" and whose parsed options have no `ajax` key gets `ajax` wired to
 *      JuneauDataTables.ajax(url) and `serverSide` defaulted to true, so server-rendered markup can opt into
 *      server-side processing with no inline script.  It is a no-op when jQuery / DataTables are absent, and is
 *      idempotent (already-initialized tables are skipped).
 *
 * Errors: a malformed search expression makes the server answer HTTP 400 with an `X-BeanQuery-Error` header (the
 * code) and the message in the body.  ajax() installs an `xhr.dt` listener that shows that message through the
 * normal DataTables error path (`error.dt` event, then `DataTable.ext.errMode`: alert by default) instead of a
 * generic "Ajax error".  Any other failed request keeps the DataTables default.
 *
 * Usage examples:
 *
 *   // Programmatic:
 *   new DataTable('#releases', {
 *       serverSide: true,
 *       ajax: JuneauDataTables.ajax('/releases/query'),
 *       columns: [{data: 'name', title: 'Name'}, {data: 'version', title: 'Version'}]
 *   });
 *
 *   // Extra request fields (object merged into the JSON body, or a function returning the body) and extra jQuery
 *   // ajax settings (headers etc.) passed through:
 *   JuneauDataTables.ajax('/releases/query', {
 *       data: {tenant: 'acme'},
 *       headers: {'X-Requested-By': 'juneau'}
 *   });
 *
 *   // Markup only (no inline script):
 *   <table data-juneau-datatable data-juneau-datatable-ajax="/releases/query"></table>
 */
(function () {
	"use strict";

	const ERROR_HEADER = "X-BeanQuery-Error";

	/**
	 * Extracts the server's own message from a BeanQuery 400 response: a JSON object's `message` / `detail` / `title`,
	 * a JSON string, or a short plain-text body; falls back to the `X-BeanQuery-Error` code.
	 */
	function errorMessage(xhr, code) {
		let text = typeof xhr.responseText === "string" ? xhr.responseText.trim() : "";
		if (text) {
			try {
				const parsed = JSON.parse(text);
				if (typeof parsed === "string") {
					text = parsed;
				} else if (parsed && typeof parsed === "object") {
					text = [parsed.message, parsed.detail, parsed.title].find(v => typeof v === "string" && v) || "";
				}
			} catch {
				// Not JSON: use the raw text unless it looks like an HTML error page.
				if (text.startsWith("<")) {
					text = "";
				}
			}
		}
		if (text) {
			return text.length > 500 ? text.substring(0, 500) + "..." : text;
		}
		return "Invalid query (" + code + ").";
	}

	/** Reports `message` the way DataTables reports its own errors: `error.dt` event, then `DataTable.ext.errMode`. */
	function reportError(settings, message) {
		const $ = window.jQuery;
		const id = settings?.sTableId;
		const msg = "DataTables warning: " + (id ? "table id=" + id + " - " : "") + message;
		if (settings?.nTable && $) {
			$(settings.nTable).trigger("error.dt", [settings, 0, msg]);
		}
		const mode = $?.fn?.dataTable?.ext?.errMode ?? "alert";
		if (typeof mode === "function") {
			mode(settings, 0, msg);
		} else if (mode === "throw") {
			throw new Error(msg);
		} else if (mode !== "none") {
			window.alert(msg);
		}
	}

	/**
	 * `xhr.dt` listener: a failed request carrying an `X-BeanQuery-Error` header (the server's HTTP 400 for a
	 * malformed search expression) is reported with the server's message instead of DataTables' generic "Ajax error".
	 * Returning `true` tells DataTables the error has been handled; every other failure keeps the default handling.
	 */
	function onXhr(e, settings, json, xhr) {
		if (json || !xhr || !(xhr.status >= 400) || typeof xhr.getResponseHeader !== "function") {
			return undefined;
		}
		const code = xhr.getResponseHeader(ERROR_HEADER);
		if (!code) {
			return undefined;
		}
		reportError(settings, errorMessage(xhr, code));
		return true;
	}

	function bindErrorHandler(settings) {
		const $ = window.jQuery;
		if ($ && settings?.nTable) {
			$(settings.nTable).off("xhr.dt.juneauErr").on("xhr.dt.juneauErr", onXhr);
		}
	}

	/**
	 * Builds a DataTables `ajax` option that POSTs the server-side request as a JSON body.
	 *
	 * @param {string} url The endpoint URL (a POST op taking `@Content DataTablesRequest`).
	 * @param {Object} [extra] Optional settings.  `extra.data` is either an object whose keys are merged into the
	 *     request before serializing, or a function `(d) => body` whose non-null return replaces the request (a
	 *     string return is sent as-is, not re-encoded).  Every other key (except `contentType`, which is always
	 *     `application/json`) is passed through to the jQuery ajax settings unchanged (e.g. `headers`, `beforeSend`,
	 *     `dataSrc`).
	 * @return {Object} The DataTables `ajax` option.
	 */
	function ajax(url, extra) {
		extra = extra || {};
		const extraData = extra.data;
		const opts = {
			url: url,
			type: "POST",
			contentType: "application/json",
			dataType: "json",
			data: function (d, settings) {
				bindErrorHandler(settings);
				if (typeof extraData === "function") {
					const replacement = extraData(d);
					if (replacement) {
						d = replacement;
					}
				} else if (extraData) {
					for (const k in extraData) {
						if (Object.hasOwn(extraData, k)) {
							d[k] = extraData[k];
						}
					}
				}
				// A function that already serialized its body returns a string; never encode it twice.
				return typeof d === "string" ? d : JSON.stringify(d);
			}
		};
		for (const k in extra) {
			if (Object.hasOwn(extra, k) && k !== "data" && k !== "contentType") {
				opts[k] = extra[k];
			}
		}
		return opts;
	}

	function initAll() {
		const $ = window.jQuery;
		if (!$?.fn?.DataTable) {
			return;
		}
		$("table[data-juneau-datatable]").each(function () {
			if ($.fn.dataTable.isDataTable(this)) {
				return;
			}
			const raw = this.dataset.juneauDatatable;
			let opts = {};
			if (raw) {
				try {
					opts = JSON.parse(raw);
				} catch {
					// Malformed JSON is treated like no options.
					opts = {};
				}
				// Valid JSON that is not an options object (e.g. "null" or "[]") is treated like no options.
				if (!opts || typeof opts !== "object" || Array.isArray(opts)) {
					opts = {};
				}
			}
			const ajaxUrl = this.dataset.juneauDatatableAjax;
			if (ajaxUrl && !opts.ajax) {
				opts.ajax = window.JuneauDataTables.ajax(ajaxUrl);
				if (opts.serverSide === undefined) {
					opts.serverSide = true;
				}
			}
			$(this).DataTable(opts);
		});
	}

	window.JuneauDataTables = window.JuneauDataTables || {
		ajax: ajax,
		initAll: initAll,
		errorMessage: errorMessage,
		onXhr: onXhr
	};

	if (document.readyState === "loading") {
		document.addEventListener("DOMContentLoaded", initAll);
	} else {
		initAll();
	}
})();
