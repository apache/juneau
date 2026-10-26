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

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.console.*;
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
		var target = PersistSpec.page(ns).topics(Topics.filter("r"));
		assertEquals("E-77: dashboard 'd': " + detail,
			assertThrows(IllegalArgumentException.class, () -> target.check("d")).getMessage());
	}

	@Test void a04_errors() {
		var target2 = PersistSpec.page("a").version(0).topics(Topics.filter("r"));
		assertEquals("E-77: dashboard 'd': persist version 0 must be at least 1",
			assertThrows(IllegalArgumentException.class, () -> target2.check("d")).getMessage());
		var target3 = PersistSpec.page("a");
		assertEquals("E-77: dashboard 'd': persist needs at least one topic",
			assertThrows(IllegalArgumentException.class, () -> target3.check("d")).getMessage());
		var target4 = PersistSpec.page("a").topics(Topics.selection("t"));
		assertEquals("E-77: dashboard 'd': persist topic 'selection:t' is not a filter:, zoom: or legend: state topic",
			assertThrows(IllegalArgumentException.class, () -> target4.check("d")).getMessage());
		var target5 = PersistSpec.page("a").topics("app.x");
		assertEquals("E-77: dashboard 'd': persist topic 'app.x' is not a filter:, zoom: or legend: state topic",
			assertThrows(IllegalArgumentException.class, () -> target5.check("d")).getMessage());
		var target6 = PersistSpec.page("a").topics(Topics.filter("r"), Topics.filter("r"));
		assertEquals("E-77: dashboard 'd': persist lists topic 'filter:r' twice",
			assertThrows(IllegalArgumentException.class, () -> target6.check("d")).getMessage());
		var target7 = PersistSpec.page("a").topics(Topics.filter("r")).decoder("goto");
		assertEquals("E-77: dashboard 'd': decoder name 'goto' is not a dotted extension name",
			assertThrows(IllegalArgumentException.class, () -> target7.check("d")).getMessage());
		var target8 = PersistSpec.page("a").topics(Topics.filter("r")).decoder("app.go").decoder("app.go");
		assertEquals("E-77: dashboard 'd': decoder 'app.go' is listed twice",
			assertThrows(IllegalArgumentException.class, () -> target8.check("d")).getMessage());
	}
}
