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
window.JuneauConsoleCards ||= [];
window.JuneauConsoleCards.push(['recorder', class {
	static lateBase = true;
	static roles = { filter: 'filterRole', state: 'stateRole' };
	count() {
		const T = window.__busTest;
		T.renders[this.spec.id] = (T.renders[this.spec.id] || 0) + 1;
		this.el.textContent = 'rendered ' + T.renders[this.spec.id];
	}
	render() { this.count(); }
	refresh() { this.count(); }
	filterRole(payload) { window.__busTest.roleCalls.push({ card: this.spec.id, role: 'filter', payload }); }
	stateRole(payload) { window.__busTest.roleCalls.push({ card: this.spec.id, role: 'state', payload }); }
}]);
