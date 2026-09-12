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
 * Mounts a Juneau table into an empty HTML slot.
 *
 * Reads #rm-table-slot JSON {slotId, tableUrl} and calls
 * JuneauViews.regions.mount({ [slotId]: { table: tableUrl } }). Fetch/handshake
 * failures are left to the toolkit, which paints a banner in the slot.
 */
(function () {
	'use strict';

	function parseCfg() {
		var el = document.getElementById('rm-table-slot');
		if (!el)
			return null;
		return JSON.parse(el.textContent || 'null');
	}

	function mount() {
		var cfg = parseCfg();
		if (!cfg || !cfg.slotId || !cfg.tableUrl)
			return;
		var regions = globalThis.JuneauViews && globalThis.JuneauViews.regions;
		if (!regions || typeof regions.mount !== 'function') {
			if (globalThis.console && console.error)
				console.error('JuneauViews.regions.mount is not available; table slot was not mounted.');
			return;
		}
		var map = {};
		map[cfg.slotId] = { table: cfg.tableUrl };
		regions.mount(map);
	}

	if (document.readyState === 'loading')
		document.addEventListener('DOMContentLoaded', mount);
	else
		mount();
})();
