/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
 * juneau-page-cards.js - scan <@card> sidecar envelopes and hand table/populate slots to
 * JuneauViews.regions.mount.  Named registries (templates / fn / utils) let init= modules register HTML
 * template functions, populator helpers, and utilities by name; name-only template= is looked up here.
 *
 * Binds to the same window.JuneauViews namespace juneau-views.js / juneau-regions.js create (this asset is
 * the last <script> in the "views" toolkit pack, so that namespace already exists at boot).
 */
(function () {
	"use strict";
	var page = window.JuneauPage = window.JuneauPage || {};
	page.templates = page.templates || {};
	page.fn = page.fn || {};
	page.utils = page.utils || {};

	page.templates.register = function (name, fn) {
		if (!name || typeof fn !== "function") throw new Error("JuneauPage.templates.register: name + function required");
		page.templates[name] = fn;
	};
	page.fn.register = function (name, fn) {
		if (!name || typeof fn !== "function") throw new Error("JuneauPage.fn.register: name + function required");
		page.fn[name] = fn;
	};
	page.utils.register = function (name, fn) {
		if (!name || typeof fn !== "function") throw new Error("JuneauPage.utils.register: name + function required");
		page.utils[name] = fn;
	};

	var JUNEAU_PAGE_CARDS_CONTRACT_VERSION = "1"; // MUST equal CardDirectiveModel.SIDECAR_CONTRACT_VERSION

	function compileSource(text) {
		return new Function("return (" + text + ");")();
	}

	function loadScript(src) {
		return new Promise(function (resolve, reject) {
			var s = document.createElement("script");
			s.src = src;
			s.onload = resolve;
			s.onerror = function () { reject(new Error("JuneauPage: failed to load " + src)); };
			document.head.appendChild(s);
		});
	}

	function applyOverride(sidecar) {
		var jobs = [];
		if (sidecar.src) jobs.push(loadScript(sidecar.src));
		// F8 / Q11 C: nested source *registers* a name. A name with no source is still valid - lookup later.
		if (sidecar.source && sidecar.template) {
			page.templates.register(sidecar.template, compileSource(sidecar.source));
		}
		["fn", "renderers", "utils"].forEach(function (key) {
			var v = sidecar[key];
			if (!v || typeof v === "string") return;
			if (v.src) jobs.push(loadScript(v.src));
			if (v.source && key !== "renderers") {
				page[key].register(key, compileSource(v.source));
			}
		});
		return Promise.all(jobs);
	}

	function applyNamedHtmlTemplate(sidecar) {
		if (!sidecar || !sidecar.template || sidecar.populate || Object.prototype.hasOwnProperty.call(sidecar, "table"))
			return;
		var fn = page.templates[sidecar.template];
		if (typeof fn !== "function") {
			if (window.console && console.error)
				console.error("JuneauPage: no template registered under '" + sidecar.template + "'.");
			return;
		}
		var el = sidecar.id ? document.getElementById(sidecar.id) : null;
		if (!el) {
			if (window.console && console.error)
				console.error("JuneauPage: no element with id '" + sidecar.id + "' for template '" + sidecar.template + "'.");
			return;
		}
		el.innerHTML = fn();
	}

	function collect() {
		var nodes = document.querySelectorAll("script.juneau-card-sidecar");
		var sidecars = [];
		for (var i = 0; i < nodes.length; i++) {
			var raw = nodes[i].textContent || "null";
			var parsed = JSON.parse(raw);
			if (!parsed || parsed.contractVersion !== JUNEAU_PAGE_CARDS_CONTRACT_VERSION) {
				if (window.console && console.error)
					console.error("JuneauPage: sidecar contractVersion mismatch (page='" +
						(parsed && parsed.contractVersion) + "', runtime='" + JUNEAU_PAGE_CARDS_CONTRACT_VERSION + "'); skipping.");
				continue;
			}
			sidecars.push(parsed);
		}
		return sidecars;
	}

	function hookupFrom(sidecars) {
		var hookup = {};
		for (var i = 0; i < sidecars.length; i++) {
			var s = sidecars[i];
			if (!s || !s.id) continue;
			if (s.populate) hookup[s.id] = s.populate;
			else if (Object.prototype.hasOwnProperty.call(s, "table")) hookup[s.id] = { table: s.table };
		}
		return hookup;
	}

	function boot() {
		var sidecars = collect();
		var jobs = sidecars.map(applyOverride);
		return Promise.all(jobs).then(function () {
			for (var i = 0; i < sidecars.length; i++)
				applyNamedHtmlTemplate(sidecars[i]);
			var hookup = hookupFrom(sidecars);
			if (!Object.keys(hookup).length) return;
			var regions = window.JuneauViews && window.JuneauViews.regions;
			if (!regions || typeof regions.mount !== "function") {
				if (window.console && console.error)
					console.error("JuneauViews.regions.mount is not available; cards were not mounted.");
				return;
			}
			return regions.mount(hookup);
		});
	}

	page.boot = boot;

	if (document.readyState === "loading")
		document.addEventListener("DOMContentLoaded", boot);
	else
		boot();
})();
