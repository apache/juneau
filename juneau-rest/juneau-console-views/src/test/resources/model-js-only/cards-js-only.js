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
// A page built only with the client object model: a pets table whose row detail has an Orders table tab bound to
// juneau.countLabel.  Loaded by a plain script tag after the console and views scripts.
(function () {
	"use strict";
	var M = window.JuneauViews.model;
	var page = new M.Page({ title: "Pets", nav: [], activeNav: [], modelVersion: "1" });
	var pets = new M.Table({
		id: "pets", dataMode: "client", dataUrl: "/api/pets", selection: { rowIdField: "id", selectAll: true, scope: "persistent" },
		columns: [{ key: "id", label: "Id" }, { key: "name", label: "Name" }],
		ribbon: [{ type: "action", id: "reload", icon: "refresh-cw" }]
	});
	var reload = pets.ribbon.item("reload") || pets.ribbon.addItem({ type: "action", id: "reload", icon: "refresh-cw" });
	reload.action("click", "refresh-cw", "juneau.refreshClosest", { kind: "table" });
	var detail = pets.setDetail({ endpoint: "/api/pets/{id}/detail", title: "Pet {id}", labels: { id: "Id", name: "Name" }, model: { actions: [{ id: "refresh", icon: "refresh-cw", handler: "juneau.refresh" }] } });
	detail.addTab({ id: "info", title: "Info", kind: "fields", fields: ["name"] });
	detail.addTab({
		id: "orders", title: "Orders", kind: "table", refreshOnPoll: window.__refreshOnPoll ? true : undefined,
		nested: {
			contractVersion: "2", parentScopeParam: "petId",
			view: { id: "pet-orders", columns: [{ data: "id", title: "Id" }, { data: "status", title: "Status" }] },
			meta: { contractVersion: "5", id: "pet-orders", dataMode: "client", dataUrl: "/api/orders",
				columns: [{ data: "id", title: "Id" }, { data: "status", title: "Status" }] }
		}
	}).bind("loaded", window.__ordersHandler || "juneau.countLabel");
	page.addCard("datatables", { id: "pets", title: "Pets" })._add(pets);
	if (window.__onPage) window.__onPage(page);
	window.__page = page;
	page.mount();
})();
