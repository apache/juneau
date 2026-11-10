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
 * Custom cards page: the petstore's example card types.  Shows the two ways to write one (the JSON-only kpi, and the
 * Java-validated inventory-gauge shaped by InventoryGaugeCardType), the card API (JuneauConsole.refreshCard and
 * JuneauConsole.cardApi), the juneau:card-mounted event, and an app chart decorator.
 */
(function () {
	'use strict';

	// Each handler draws into its own body element, below the card title the shell renders.  Looked up among the
	// card's own children, so a same-id element elsewhere in the page is never drawn into.
	function body(card, el) {
		var id = card.id + '-body', b = null;
		for (var i = 0; i < el.children.length && !b; i++)
			if (el.children[i].id === id) b = el.children[i];
		if (!b) {
			b = document.createElement('div');
			b.id = card.id + '-body';
			b.className = 'jc-card-body';
			el.appendChild(b);
		}
		return b;
	}

	// Mount order, for the browser test and for anyone reading the console.  Attached before the handlers are queued,
	// so no mount of theirs is missed.
	window.petstoreMounted = window.petstoreMounted || [];
	document.addEventListener('juneau:card-mounted', function (e) {
		window.petstoreMounted.push(e.detail.id + ':' + e.detail.type);
	});

	var queue = (window.JuneauConsoleCards = window.JuneauConsoleCards || []);

	// Cards extend the console's Card base.  A script that loads before the console flags its classes (lateBase) and
	// the console puts the base under them when it registers them.
	var Base = (window.JuneauViews && window.JuneauViews.model && window.JuneauViews.model.Card)
		|| (window.JuneauConsole && window.JuneauConsole.Card) || class {};

	// kpi: JSON-only (no Java class).  The body is {label, dataUrl}; the endpoint returns {label, value}, plus an
	// optional unit shown after the value.
	class KpiCard extends Base {
		static lateBase = true;
		render() {
			var card = this.spec, b = body(card, this.el);
			this.paintLoading(b);
			return this.fetchJson(card.dataUrl).then(function (d) {
				var v = document.createElement('div');
				v.className = 'petstore-kpi-value';
				v.textContent = d.unit ? d.value + ' ' + d.unit : String(d.value);
				var l = document.createElement('div');
				l.className = 'petstore-kpi-label';
				l.textContent = d.label || card.label || '';
				b.replaceChildren(v, l);
			}, function (err) {
				if (!this.isAbort(err)) this.paintError(b, err);
			}.bind(this));
		}
	}
	queue.push(['kpi', KpiCard]);

	// A custom URL-state directive: ?state=species(CAT) picks the gauge's species, and back/forward restores it.  The
	// built-in directives drive a datatable; the gauge has none, so it reads and writes the directive itself.  The
	// directive and its page listeners are set up when a gauge first renders, so a page that loads this script for
	// its kpi alone (the Store) gets neither.

	// The species the gauge accepts.  This must match the server's Species enum: the inventory endpoint rejects any
	// other name, so a species added there must be added here too.
	var SPECIES = ['BIRD', 'CAT', 'DOG', 'FISH', 'MOUSE', 'RABBIT', 'SNAKE'];

	// The species for a name in any case, or null if it isn't one.
	function speciesOf(name) {
		var v = String(name == null ? '' : name).toUpperCase();
		return SPECIES.indexOf(v) >= 0 ? v : null;
	}

	// The url-state module with the species directive registered, or null when the views toolkit isn't loaded.  The
	// body is written with the module's own argument encoding, as its directive contract asks.
	function urlState() {
		var u = window.JuneauViews && window.JuneauViews.urlState;
		if (!u) return null;
		if (!u.hasDirective('species')) {
			u.registerDirective('species', {
				parse: function (body) { return speciesOf(u.decodeArg(String(body || '')).trim()); },
				serialize: function (v) { return u.encodeArg(v); },
				isEmpty: function (v) { return v == null; }
			});
		}
		return u;
	}

	// The species the address bar asks for, or null.
	function speciesFromUrl() {
		var u = urlState(), st = u ? u.readFromSearch(window.location.search) : null;
		return st && st.ext.species ? st.ext.species : null;
	}

	// Pushes a history entry carrying the species, keeping the rest of the query string.  Without the url-state module
	// there is no address to keep it in, so nothing is pushed.
	function pushSpecies(species) {
		var u = urlState();
		if (!u) return;
		var search = u.buildSearch(window.location.search, u.encode({ ext: { species: species } }));
		window.history.pushState(null, '', window.location.pathname + search + window.location.hash);
	}

	// The page's own controls (data-cards-action buttons) drive the gauge's card API, and back/forward redraws the
	// gauge for the restored address.  Installed once, by the first gauge render.
	var listening = false;
	function listen() {
		if (listening) return;
		listening = true;
		document.addEventListener('click', function (e) {
			var b = e.target.closest && e.target.closest('[data-cards-action]');
			if (!b) return;
			var out = document.getElementById('cards-api-result') || {};
			var action = b.getAttribute('data-cards-action');
			if (action === 'cat') {
				window.JuneauConsole.cardApi('gauge').setSpecies('CAT');
				out.textContent = 'Showing cats';
			} else if (action === 'refresh') {
				window.JuneauConsole.refreshCard('gauge');
				out.textContent = 'Refreshed';
			} else {
				var api = window.JuneauConsole.cardApi('gauge');
				out.textContent = api.species() + ', ' + api.reads() + ' reads';
			}
		});
		window.addEventListener('popstate', function () {
			if (document.getElementById('gauge')) window.JuneauConsole.refreshCard('gauge');
		});
	}

	// The card body's gauge, with the species the address bar asks for, if any.  The data URL's species parameter
	// is set (or added) through URLSearchParams, so the rest of the URL is kept as written.
	function gaugeOf(card) {
		var g = card.gauge, s = speciesFromUrl();
		if (!s) return g;
		var u = new URL(g.dataUrl, window.location.href);
		u.searchParams.set('species', s);
		var dataUrl = u.origin === window.location.origin ? u.pathname + u.search + u.hash : u.href;
		return Object.assign({}, g, { species: s, dataUrl: dataUrl });
	}

	// inventory-gauge: the fragment is {gauge:{species, low, high, dataUrl}}, shaped by InventoryGaugeCardType.
	class GaugeCard extends Base {
		static lateBase = true;

		render() {
			listen();
			this.readCount = 0;    // successful reads
			this.below = false;    // true while stock is under low (announced once per crossing)
			this.paintLoading(body(this.spec, this.el));
			this.every(30000, function () { this.load(); }.bind(this));
			return this.load();
		}
		// JuneauConsole.refreshCard re-reads the data; it must not start a second timer.
		refresh() { return this.load(); }

		species() { return gaugeOf(this.spec).species; }
		setSpecies(species) {
			var s = speciesOf(species);
			if (!s) return Promise.reject(new Error("Unknown species '" + species + "'"));
			pushSpecies(s);
			return this.load();
		}
		reads() { return this.readCount || 0; }
		reload() { return this.load(); }

		load() {
			var self = this, g = gaugeOf(this.spec);
			var b = body(this.spec, this.el);
			return this.fetchJson(g.dataUrl).then(function (d) {
				self.readCount = (self.readCount || 0) + 1;
				var pct = Math.max(0, Math.min(100, Math.round(100 * (d.available - g.low) / (g.high - g.low))));
				var meter = document.createElement('meter');
				meter.min = g.low;
				meter.max = g.high;
				meter.value = d.available;
				meter.setAttribute('data-gauge-species', g.species);
				var t = document.createElement('p');
				t.className = 'petstore-gauge-text';
				t.textContent = d.available + ' ' + g.species + ' available (' + pct + '%)';
				b.replaceChildren(meter, t);
				var under = d.available < g.low;
				if (under && !self.below) self.announce(g.species + ' stock is below ' + g.low);
				self.below = under;
			}, function (err) {
				if (!self.isAbort(err)) self.paintError(b, err);
			});
		}
	}
	queue.push(['inventory-gauge', GaugeCard]);

	// petstore.stockTotals: a chart decorator for the stock bands chart.  It reads the frame (numeric columns are
	// Float64Arrays) and puts the total pet count in the title subtext.  Chart extensions queue on
	// window.JuneauConsoleCharts as [registry, name, impl]; app names are dotted.
	(window.JuneauConsoleCharts = window.JuneauConsoleCharts || []).push(['decorator', 'petstore.stockTotals', function (option, frame, spec) {
		var fields = Array.isArray(spec.fields) ? spec.fields : ['available', 'pending', 'sold'];
		var total = 0;
		fields.forEach(function (f) {
			var col = frame.columns[f];
			if (col) for (var i = 0; i < frame.rowCount; i++) if (!isNaN(col[i])) total += col[i];
		});
		var title = option.title && !Array.isArray(option.title) ? option.title : {};
		option.title = Object.assign({}, title, { subtext: total + ' pets in all', right: 0 });
		return option;
	}]);
})();
