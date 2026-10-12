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
 * JuneauConsole.cardApi) and the juneau:card-mounted event.
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

	// kpi: JSON-only (no Java class).  The body is {label, dataUrl}; the endpoint returns {label, value}.
	queue.push(['kpi', {
		render: function (card, el, ctx) {
			var b = body(card, el);
			ctx.paintLoading(b);
			return ctx.fetchJson(card.dataUrl).then(function (d) {
				var v = document.createElement('div');
				v.className = 'petstore-kpi-value';
				v.textContent = String(d.value);
				var l = document.createElement('div');
				l.className = 'petstore-kpi-label';
				l.textContent = d.label || card.label || '';
				b.replaceChildren(v, l);
			}, function (err) {
				if (!ctx.isAbort(err)) ctx.paintError(b, err);
			});
		}
	}]);

	// inventory-gauge: the fragment is {gauge:{species, low, high, dataUrl}}, shaped by InventoryGaugeCardType.
	queue.push(['inventory-gauge', (function () {
		var reads = new Map();    // card id -> successful reads
		var below = new Map();    // card id -> true while stock is under low (announced once per crossing)

		function load(card, el, ctx) {
			var g = card.gauge;
			var b = body(card, el);
			return ctx.fetchJson(g.dataUrl).then(function (d) {
				reads.set(card.id, (reads.get(card.id) || 0) + 1);
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
				if (under && !below.get(card.id)) ctx.announce(g.species + ' stock is below ' + g.low);
				below.set(card.id, under);
			}, function (err) {
				if (!ctx.isAbort(err)) ctx.paintError(b, err);
			});
		}

		return {
			render: function (card, el, ctx) {
				ctx.setApi({
					species: function () { return card.gauge.species; },
					reads: function () { return reads.get(card.id) || 0; },
					reload: function () { return load(card, el, ctx); }
				});
				ctx.paintLoading(body(card, el));
				ctx.every(30000, function () { load(card, el, ctx); });
				return load(card, el, ctx);
			},
			// JuneauConsole.refreshCard re-reads the data; it must not start a second timer.
			refresh: load,
			destroy: function (card) {
				reads.delete(card.id);
				below.delete(card.id);
			}
		};
	})()]);

	// The card API, from the page's own controls.
	document.addEventListener('click', function (e) {
		var b = e.target.closest('[data-cards-action]');
		if (!b) return;
		var out = document.getElementById('cards-api-result');
		if (b.getAttribute('data-cards-action') === 'refresh') {
			window.JuneauConsole.refreshCard('gauge');
			out.textContent = 'Refreshed';
		} else {
			var api = window.JuneauConsole.cardApi('gauge');
			out.textContent = api.species() + ', ' + api.reads() + ' reads';
		}
	});
})();
