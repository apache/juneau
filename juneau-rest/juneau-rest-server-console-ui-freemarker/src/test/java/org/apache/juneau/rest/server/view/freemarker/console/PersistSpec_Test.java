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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

class PersistSpec_Test extends TestBase {

	@Test void a01_shape() {
		var p = PersistSpec.page("app.noqc").version(3).topics(Topics.filter("range"), "zoom:daily").shareLink().decoder("app.goto");
		p.check("d");
		assertEquals(Json5.to("{ns:'app.noqc',version:3,topics:['filter:range','zoom:daily'],shareLink:true,decoders:['app.goto']}", Map.class),
			Json5.to(Json.of(p.toMap()), Map.class));
	}

	@Test void a02_defaults() {
		var p = PersistSpec.page("app").topics(Topics.filter("r"));
		p.check("d");
		assertEquals(Json5.to("{ns:'app',version:1,topics:['filter:r'],shareLink:false,decoders:[]}", Map.class),
			Json5.to(Json.of(p.toMap()), Map.class));
	}

	@ParameterizedTest
	@CsvSource(delimiter='|', value={
		"Bad.ns|namespace 'Bad.ns' does not match ^[a-z][a-z0-9.-]{0,63}$",
		"1app|namespace '1app' does not match ^[a-z][a-z0-9.-]{0,63}$",
	})
	void a03_badNamespace(String ns, String detail) {
		assertEquals("E-77: dashboard 'd': " + detail,
			assertThrows(IllegalArgumentException.class, () -> PersistSpec.page(ns).topics(Topics.filter("r")).check("d")).getMessage());
	}

	@Test void a04_errors() {
		assertEquals("E-77: dashboard 'd': persist version 0 must be at least 1",
			assertThrows(IllegalArgumentException.class, () -> PersistSpec.page("a").version(0).topics(Topics.filter("r")).check("d")).getMessage());
		assertEquals("E-77: dashboard 'd': persist needs at least one topic",
			assertThrows(IllegalArgumentException.class, () -> PersistSpec.page("a").check("d")).getMessage());
		assertEquals("E-77: dashboard 'd': persist topic 'selection:t' is not a filter:, zoom: or legend: state topic",
			assertThrows(IllegalArgumentException.class, () -> PersistSpec.page("a").topics(Topics.selection("t")).check("d")).getMessage());
		assertEquals("E-77: dashboard 'd': persist topic 'app.x' is not a filter:, zoom: or legend: state topic",
			assertThrows(IllegalArgumentException.class, () -> PersistSpec.page("a").topics("app.x").check("d")).getMessage());
		assertEquals("E-77: dashboard 'd': persist lists topic 'filter:r' twice",
			assertThrows(IllegalArgumentException.class, () -> PersistSpec.page("a").topics(Topics.filter("r"), Topics.filter("r")).check("d")).getMessage());
		assertEquals("E-77: dashboard 'd': decoder name 'goto' is not a dotted extension name",
			assertThrows(IllegalArgumentException.class, () -> PersistSpec.page("a").topics(Topics.filter("r")).decoder("goto").check("d")).getMessage());
		assertEquals("E-77: dashboard 'd': decoder 'app.go' is listed twice",
			assertThrows(IllegalArgumentException.class, () -> PersistSpec.page("a").topics(Topics.filter("r")).decoder("app.go").decoder("app.go").check("d")).getMessage());
	}
}
