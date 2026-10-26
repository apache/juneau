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
package org.apache.juneau.console.freemarker;

import static org.apache.juneau.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.console.*;
import org.junit.jupiter.api.*;

class Decorators_Test extends TestBase {

	private static Object j(String json5) { return Json5.to(json5, Map.class); }

	@Test void a01_fn_ref() {
		assertEquals(j("{'$fn':'compact'}"), Fn.ref("compact"));
		assertEquals(j("{'$fn':'template',args:{text:'{name}'}}"), Fn.ref("template", JsonMap.of("text", "{name}")));
		assertEquals(j("{'$fn':'app.x'}"), Fn.ref("app.x"));
	}

	@Test void a02_fn_badName_isE74() {
		var e = assertThrows(IllegalArgumentException.class, () -> Fn.ref("Bad"));
		assertString("E-74: 'Bad' is not a valid $fn name", e.getMessage());
		assertThrows(IllegalArgumentException.class, () -> Fn.ref(null));
	}

	@Test void a03_markers() {
		assertEquals(j("{use:'markers',src:'/rest/releases',at:'date',label:'name',topic:'app.release'}"),
			Markers.from("/rest/releases").at("date").label("name").onClick(Topics.custom("app", "release")).toJson());
		assertEquals(j("{use:'markers',values:[{at:'2026-10-01',label:'Freeze'}],style:'dashed'}"),
			Markers.values(List.of(Map.of("at", "2026-10-01", "label", "Freeze"))).style("dashed").toJson());
		assertEquals(j("{use:'markers',source:'data:releases',at:'date',label:'name'}"),
			Markers.source(Topics.data("releases")).at("date").label("name").toJson());
		assertEquals(j("{use:'markers',now:{label:'Today'}}"), Markers.now("Today").toJson());
	}

	@Test void a04_markers_rejects() {
		assertThrows(IllegalArgumentException.class, () -> Markers.from("https://evil/x"));
		assertThrows(IllegalArgumentException.class, () -> Markers.source("filter:x"));
		var target = Markers.now("x");
		assertThrows(IllegalArgumentException.class, () -> target.style("wavy"));
	}

	@Test void a05_bands() {
		assertEquals(j("{use:'bands',src:'/rest/outages',start:'from',end:'to',label:'Outage'}"),
			Bands.from("/rest/outages").start("from").end("to").label("Outage").toJson());
		assertEquals(j("{use:'bands',auto:'gaps',label:'No data'}"), Bands.gaps().label("No data").toJson());
		assertEquals(j("{use:'bands',auto:'gaps',minGap:2}"), Bands.gaps().minGap(2).toJson());
		var target2 = Bands.gaps();
		assertThrows(IllegalArgumentException.class, () -> target2.minGap(0));
	}

	@Test void a06_thresholds() {
		assertEquals(j("{use:'thresholds',values:[{y:500,label:'SLO',tone:'danger'}]}"),
			Thresholds.at(500).label("SLO").tone("danger").toJson());
		var target3 = Thresholds.at(1);
		assertThrows(IllegalArgumentException.class, () -> target3.tone("pink"));
	}

	@Test void a07_annotations() {
		assertEquals(j("{use:'annotations',presets:['max','min'],label:'{name}'}"),
			Annotations.max().withMin().label("{name}").toJson());
		assertEquals(j("{use:'annotations',values:[{x:'Mon',y:3,label:'Peak'}]}"),
			Annotations.points(List.of(Map.of("x", "Mon", "y", 3, "label", "Peak"))).toJson());
	}

	@Test void a08_toJson_isACopy() {
		var m = Markers.now("x");
		m.toJson().put("use", "other");
		assertEquals("markers", m.toJson().get("use"));
	}
}
