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
 * Secure "Try it" card: each [data-secure-call] button calls the guarded whoami endpoint with no token
 * ("none"), or with the bearer token named by the attribute, and prints the status and body in #secure-result.
 */
(function () {
	'use strict';
	document.addEventListener('click', async function (e) {
		var b = e.target.closest('[data-secure-call]');
		if (!b) return;
		var token = b.getAttribute('data-secure-call');
		var out = document.getElementById('secure-result');
		var headers = { 'Accept': 'application/json' };
		if (token !== 'none') headers['Authorization'] = 'Bearer ' + token;
		out.removeAttribute('data-status');
		out.textContent = 'Calling...';
		try {
			var r = await fetch(b.getAttribute('data-secure-url'), { credentials: 'same-origin', headers: headers });
			var text = await r.text();
			out.textContent = r.status + ' ' + (r.ok ? 'OK' : r.statusText) + '\n' + text;
			out.setAttribute('data-status', String(r.status));
		} catch (err) {
			out.textContent = 'Request failed: ' + err;
			out.setAttribute('data-status', 'error');
		}
	});
})();
