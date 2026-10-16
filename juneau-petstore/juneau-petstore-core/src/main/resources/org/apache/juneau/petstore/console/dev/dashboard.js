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
 * Petstore dashboard page: the "Late only" rail section.  It is a custom dashboard section that publishes
 * {schemaVersion:1, viewId, options:{lateOnly}} on its filter topic; the orders source maps options.lateOnly to the
 * lateOnly query parameter.  It builds its DOM with createElement and textContent only.
 */
(function () {
	'use strict';
	(window.JuneauConsoleDashboard = window.JuneauConsoleDashboard || []).push(['section', 'petstore.late', {
		validate: function (p) {
			return p.options === undefined || typeof p.options.lateOnly === 'boolean';
		},
		mount: function (el, spec, ctx) {
			var doc = el.ownerDocument;
			var label = doc.createElement('label');
			var box = doc.createElement('input');
			box.type = 'checkbox';
			box.setAttribute('data-late', '');
			box.checked = !!(ctx.initial && ctx.initial.options && ctx.initial.options.lateOnly === true);
			var send = function () {
				ctx.publish({ schemaVersion: 1, viewId: ctx.viewId, options: { lateOnly: box.checked } });
			};
			box.addEventListener('change', send);
			label.appendChild(box);
			label.appendChild(doc.createTextNode(' ' + (ctx.args && typeof ctx.args.label === 'string' ? ctx.args.label : 'Late only')));
			el.appendChild(label);
			send();
		}
	}]);
})();
