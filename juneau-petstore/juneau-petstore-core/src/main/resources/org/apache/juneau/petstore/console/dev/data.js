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
 * Data sources page: declares petstore.range (retained) and publishes it as the last three UTC days through tomorrow,
 * so the orders source loads four day chunks (three closed, one open).  A page query ?end=YYYY-MM-DD pins the window
 * to the four days before that UTC date instead; an unparseable value falls back to the live window.  The topic is
 * declared as the script runs, so it is retained for any subscriber, however late.
 */
(function () {
	'use strict';
	var DAY = 24 * 3600 * 1000;
	var bus = window.JuneauViews.bus;
	bus.declare('petstore.range', { retain: true });
	function windowEnd() {
		var m = /[?&]end=(\d{4}-\d{2}-\d{2})(?:&|$)/.exec(location.search);
		var pinned = m ? Date.parse(m[1] + 'T00:00:00Z') : NaN;
		return isNaN(pinned) ? Math.floor(Date.now() / DAY) * DAY + DAY : pinned;
	}
	function publish() {
		var end = windowEnd();
		bus.publish('petstore.range', {
			schemaVersion: 1,
			start: new Date(end - 4 * DAY).toISOString(),
			end: new Date(end).toISOString()
		});
	}
	if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', publish);
	else publish();
})();
