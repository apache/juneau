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
 * juneau-icons.js - dependency-free icon registry for the Apache Juneau rich-view toolkit (DataTables ribbon
 * visual-parity pass).
 *
 * A pure, library-free registry mapping icon names to inline-SVG markup, mirroring juneau-renders.js's
 * registerRenderer/resolveRenderer pattern exactly (registerIcon/resolveIcon).  Apps can register additional or
 * overriding icons at runtime via window.JuneauViews.icons.registerIcon(name, svgMarkup).
 *
 * Ships the glyphs this pass's ribbon/paging-ribbon components actually use - each a minimal, single-<path>,
 * currentColor-filled Material Symbols Outlined glyph, sourced from Google's public google/material-design-icons
 * repository (Apache License, Version 2.0) - see the repo-root NOTICE file for the attribution paragraph.  This is
 * a clean-room, spec-from-behavior asset, NOT ported from IRS (design doc §6).
 */
(function () {
	"use strict";

	var NS = window.JuneauViews = window.JuneauViews || {};

	var registry = NS._icons = NS._icons || {};

	/** Registers (or overrides) an icon's inline-SVG markup under `name`. */
	function registerIcon(name, svgMarkup) {
		registry[name] = svgMarkup;
		return registry[name];
	}

	/** Looks up an icon's markup by name; returns null when unknown (callers fall back to rendering raw text). */
	function resolveIcon(name) {
		return Object.prototype.hasOwnProperty.call(registry, name) ? registry[name] : null;
	}

	NS.icons = {
		registerIcon: registerIcon,
		resolveIcon: resolveIcon
	};

	// Bundled glyphs (Material Symbols Outlined, Apache License 2.0, Google upstream - see NOTICE).
	registerIcon("content_copy", "<svg viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M360-240q-33 0-56.5-23.5T280-320v-480q0-33 23.5-56.5T360-880h360q33 0 56.5 23.5T800-800v480q0 33-23.5 56.5T720-240H360Zm0-80h360v-480H360v480ZM200-80q-33 0-56.5-23.5T120-160v-560h80v560h440v80H200Zm160-240v-480 480Z\"/></svg>");
	registerIcon("csv", "<svg viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M230-360h120v-60H250v-120h100v-60H230q-17 0-28.5 11.5T190-560v160q0 17 11.5 28.5T230-360Zm156 0h120q17 0 28.5-11.5T546-400v-60q0-17-11.5-31.5T506-506h-60v-34h100v-60H426q-17 0-28.5 11.5T386-560v60q0 17 11.5 30.5T426-456h60v36H386v60Zm264 0h60l70-240h-60l-40 138-40-138h-60l70 240ZM160-160q-33 0-56.5-23.5T80-240v-480q0-33 23.5-56.5T160-800h640q33 0 56.5 23.5T880-720v480q0 33-23.5 56.5T800-160H160Zm0-80h640v-480H160v480Zm0 0v-480 480Z\"/></svg>");
	registerIcon("table", "<svg viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M200-120q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840h560q33 0 56.5 23.5T840-760v560q0 33-23.5 56.5T760-120H200Zm240-240H200v160h240v-160Zm80 0v160h240v-160H520Zm-80-80v-160H200v160h240Zm80 0h240v-160H520v160ZM200-680h560v-80H200v80Z\"/></svg>");
	registerIcon("picture_as_pdf", "<svg viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M360-460h40v-80h40q17 0 28.5-11.5T480-580v-40q0-17-11.5-28.5T440-660h-80v200Zm40-120v-40h40v40h-40Zm120 120h80q17 0 28.5-11.5T640-500v-120q0-17-11.5-28.5T600-660h-80v200Zm40-40v-120h40v120h-40Zm120 40h40v-80h40v-40h-40v-40h40v-40h-80v200ZM320-240q-33 0-56.5-23.5T240-320v-480q0-33 23.5-56.5T320-880h480q33 0 56.5 23.5T880-800v480q0 33-23.5 56.5T800-240H320Zm0-80h480v-480H320v480ZM160-80q-33 0-56.5-23.5T80-160v-560h80v560h560v80H160Zm160-720v480-480Z\"/></svg>");
	registerIcon("refresh", "<svg viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M480-160q-134 0-227-93t-93-227q0-134 93-227t227-93q69 0 132 28.5T720-690v-110h80v280H520v-80h168q-32-56-87.5-88T480-720q-100 0-170 70t-70 170q0 100 70 170t170 70q77 0 139-44t87-116h84q-28 106-114 173t-196 67Z\"/></svg>");
	registerIcon("manage_search", "<svg viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M80-200v-80h400v80H80Zm0-200v-80h200v80H80Zm0-200v-80h200v80H80Zm744 400L670-354q-24 17-52.5 25.5T560-320q-83 0-141.5-58.5T360-520q0-83 58.5-141.5T560-720q83 0 141.5 58.5T760-520q0 29-8.5 57.5T726-410l154 154-56 56ZM560-400q50 0 85-35t35-85q0-50-35-85t-85-35q-50 0-85 35t-35 85q0 50 35 85t85 35Z\"/></svg>");
	registerIcon("unfold_less", "<svg viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"m356-160-56-56 180-180 180 180-56 56-124-124-124 124Zm124-404L300-744l56-56 124 124 124-124 56 56-180 180Z\"/></svg>");
	registerIcon("tune", "<svg viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M440-120v-240h80v80h320v80H520v80h-80Zm-320-80v-80h240v80H120Zm160-160v-80H120v-80h160v-80h80v240h-80Zm160-80v-80h400v80H440Zm160-160v-240h80v80h160v80H680v80h-80Zm-480-80v-80h400v80H120Z\"/></svg>");
	registerIcon("first_page", "<svg viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M240-240v-480h80v480h-80Zm440 0L440-480l240-240 56 56-184 184 184 184-56 56Z\"/></svg>");
	registerIcon("chevron_left", "<svg viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M560-240 320-480l240-240 56 56-184 184 184 184-56 56Z\"/></svg>");
	registerIcon("chevron_right", "<svg viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M504-480 320-664l56-56 240 240-240 240-56-56 184-184Z\"/></svg>");
	registerIcon("last_page", "<svg viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"m280-240-56-56 184-184-184-184 56-56 240 240-240 240Zm360 0v-480h80v480h-80Z\"/></svg>");
	registerIcon("filter_alt", "<svg viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M440-160q-17 0-28.5-11.5T400-200v-240L168-736q-15-20-4.5-42t36.5-22h560q26 0 36.5 22t-4.5 42L560-440v240q0 17-11.5 28.5T520-160h-80Zm40-308 198-252H282l198 252Zm0 0Z\"/></svg>");
	// Unified paging ribbon's page-size menu-button caret (visual-parity follow-up - see juneau-views.js's
	// buildPageSizeMenu(...)); previously missing from this pass's original 13-glyph set.
	registerIcon("expand_more", "<svg viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M480-345 240-585l56-56 184 184 184-184 56 56-240 240Z\"/></svg>");
})();
