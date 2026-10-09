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
 * Test-only card type for the console-bus browser suite.  It counts renders (so a `refresh` subscription is
 * observable) and records every role call (`filter`, `state`) with its already-mapped payload.
 */
(window.JuneauConsoleCards = window.JuneauConsoleCards || []).push(['recorder', {
	render(card, el) {
		const T = window.__busTest;
		T.renders[card.id] = (T.renders[card.id] || 0) + 1;
		el.textContent = 'rendered ' + T.renders[card.id];
	},
	refresh(card, el) {
		const T = window.__busTest;
		T.renders[card.id] = (T.renders[card.id] || 0) + 1;
		el.textContent = 'rendered ' + T.renders[card.id];
	},
	roles: {
		filter(payload, meta, card) { window.__busTest.roleCalls.push({ card: card.id, role: 'filter', payload }); },
		state(payload, meta, card) { window.__busTest.roleCalls.push({ card: card.id, role: 'state', payload }); }
	}
}]);
