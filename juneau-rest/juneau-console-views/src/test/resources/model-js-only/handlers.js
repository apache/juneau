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
// Handlers the JS-only model pages declare by name.  Loaded by the shell (page "scripts") or by a plain script tag.
(function () {
	"use strict";
	const probe = window.__probe = window.__probe || {};
	probe.log = probe.log || [];
	const H = window.JuneauViews.handlers;
	H.register("app.go", function (target) { probe.log.push("go:" + target.id); });
	H.register("app.boom", function () { throw new Error("kaboom"); });
	H.register("app.pageLoaded", function () { probe.log.push("pageLoaded"); });
	H.register("app.cardLoaded", function (target) { probe.log.push("cardLoaded:" + target.id); });
})();
