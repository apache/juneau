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
 * Releases Detail View populator. Registers before juneau-page-cards.js mounts the table.
 * Dual-hat: look/behavior only — no slds-* / SSC class copies.
 *
 * Two-column fieldGrid (Juneau --juneau-view-detail-columns + cols-2 class; inline labels).
 * One Links field with labeled anchors; missing URLs omitted.
 */
(function () {
	'use strict';

	// The views toolkit pack (<@page toolkit="views">) loads juneau-icons.js generically, so the icon sprite
	// defaults to the "original" pack. This app wants Material Symbols. Select it here — this
	// file is in the page's init= list, parsed after juneau-icons.js and before the DOMContentLoaded mount, so
	// icons.pack() reloads the sprite in time for the ribbon/paging glyphs. (This is JuneauViews.icons.pack, the
	// icon-sprite selector — NOT the ConsoleChromeMixin CSS ThemePack.)
	function selectIconPack() {
		var icons = globalThis.JuneauViews && globalThis.JuneauViews.icons;
		if (icons && typeof icons.pack === 'function')
			icons.pack('material');
	}

	// Row-detail header icon.  The header pattern (Juneau buildDetailHeader) shows a titled icon tile when the
	// detail config carries an `icon` name, resolved through JuneauViews.icons.resolveIcon.  The Material pack
	// selected above ships only the ribbon/paging glyphs, so this app registers its OWN release-marker glyph as
	// self-contained inline SVG (not a sprite <use>), which renders under any pack.  ORIGINAL artwork authored
	// for this app, licensed Apache-2.0 with the rest of the tree — a plain filled bookmark/ribbon, no Material
	// path and no Salesforce/IRS/SLDS artwork.  Fill is currentColor so it takes the icon tile's contrast colour.
	var RELEASE_ICON =
		'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" aria-hidden="true">'
		+ '<path fill="currentColor" d="M7 4 H17 V20 L12 16.5 L7 20 Z"/></svg>';

	function registerDetailIcon() {
		var icons = globalThis.JuneauViews && globalThis.JuneauViews.icons;
		if (icons && typeof icons.registerIcon === 'function')
			icons.registerIcon('release', RELEASE_ICON);
	}

	var LINK_SPECS = [
		{ data: 'jiraVersionUrl', label: 'Jira Version' },
		{ data: 'githubTagUrl', label: 'GitHub Tag' },
		{ data: 'releaseNotesUrl', label: 'Release Notes' }
	];

	function present(value) {
		return value != null && String(value).trim() !== '';
	}

	function linkSpecs(values) {
		return LINK_SPECS.filter(function (spec) {
			return present(values[spec.data]);
		});
	}

	function paintLinks(slot, specs, values) {
		if (!slot)
			return;
		slot.replaceChildren();
		var wrap = slot.closest('.juneau-view-detail-field-value') || slot;
		wrap.classList.add('links-cell');
		specs.forEach(function (spec, i) {
			if (i)
				slot.appendChild(document.createTextNode(' '));
			var a = document.createElement('a');
			a.href = values[spec.data];
			a.textContent = spec.label;
			slot.appendChild(a);
		});
	}

	function register() {
		var regions = globalThis.JuneauViews && globalThis.JuneauViews.regions;
		if (!regions || typeof regions.register !== 'function')
			return;
		regions.register('releases-detail', function (ctx, container) {
			var helpers = globalThis.JuneauViews && globalThis.JuneauViews.helpers;
			if (!helpers || typeof helpers.fieldGrid !== 'function')
				return;
			var values = Object.assign({}, ctx.data || {});
			var fields = [
				{ data: 'version', label: 'Version' },
				{ data: 'rc', label: 'RC' },
				{ data: 'status', label: 'Status', render: 'pill:status' },
				{ data: 'stage', label: 'Stage', render: 'pill:stage' },
				{ data: 'voteCloses', label: 'Vote closes' },
				{ data: 'released', label: 'Released' },
				{ data: 'source', label: 'Source' }
			];
			var links = linkSpecs(values);
			if (links.length)
				fields.push({ data: 'links', label: 'Links', span: 'full' });
			var grid = helpers.fieldGrid(fields, { columns: 2, values: values });
			// cols-2 is what juneau-views.css actually keys off; opts.columns only sets a custom
			// property the stylesheet does not currently consume.
			grid.classList.add('juneau-view-detail-fields-inline', 'juneau-view-detail-fields-cols-2');
			if (links.length)
				paintLinks(grid.querySelector('[data-juneau-field="links"]'), links, values);
			container.replaceChildren(grid);
		});
	}

	selectIconPack();
	registerDetailIcon();
	register();
})();
