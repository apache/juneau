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
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;

class Derived_Test extends TestBase {

	@Test void a01_shape() {
		var d = Derived.from("orgRequests")
			.transform("filter", JsonMap.of("field", "status", "in", List.of("{reasons}")))
			.transform("bucket", JsonMap.of("field", "start", "unit", "auto"))
			.params(Subscription.to(Topics.filter("reasons")).as("params").map("reasons", "columns.reason"))
			.as("trendBuckets");
		assertEquals("trendBuckets", d.id());
		assertEquals("{\"id\":\"trendBuckets\",\"kind\":\"derived\",\"from\":\"orgRequests\",\"transforms\":["
			+ "{\"use\":\"filter\",\"args\":{\"field\":\"status\",\"in\":[\"{reasons}\"]}},"
			+ "{\"use\":\"bucket\",\"args\":{\"field\":\"start\",\"unit\":\"auto\"}}],"
			+ "\"subscribes\":[{\"topic\":\"filter:reasons\",\"as\":\"params\","
			+ "\"map\":{\"reasons\":\"columns.reason\"}}]}",
			d.toMap().toString());
	}

	@Test void a02_errors() {
		var target = Derived.from("a");
		var e = assertThrows(IllegalArgumentException.class, () -> target.transform("Bad", null));
		assertEquals("E-74: 'Bad' is not a valid transform name", e.getMessage());
		var target2 = Derived.from("a");
		var s = assertThrows(IllegalStateException.class, target2::toMap);
		assertEquals("Derived.from('a'): as(id) not called", s.getMessage());
		var target3 = Derived.from("a");
		e = assertThrows(IllegalArgumentException.class, () -> target3.as("a"));
		assertEquals("E-76: data source 'a': a derived source cannot read its own topic", e.getMessage());
		var target4 = Derived.from("a")
			.params(Subscription.to("app.x").as("refresh")).as("b");
		e = assertThrows(IllegalArgumentException.class, target4::toMap);
		assertEquals("E-76: data source 'b': subscription to 'app.x' has role 'refresh'; "
			+ "a derived source accepts params", e.getMessage());
		assertThrows(IllegalArgumentException.class, () -> Derived.from("bad id"));
	}

	@Test void a03_copies() {
		var args = JsonMap.of("field", "x");
		var d = Derived.from("a").transform("filter", args).as("b");
		args.put("field", "changed");
		var m = d.toMap();
		assertEquals("{\"field\":\"x\"}", ((JsonMap)((List<?>)m.get("transforms")).get(0)).get("args").toString());
		((JsonMap)((List<?>)m.get("transforms")).get(0)).put("use", "zzz");
		assertEquals("filter", ((JsonMap)((List<?>)d.toMap().get("transforms")).get(0)).get("use"));
		var target5 = Derived.from("a");
		var e = assertThrows(IllegalArgumentException.class, () -> target5.params((Subscription)null));
		assertEquals("E-76: data source 'a': subscription is null", e.getMessage());
	}

	@Test void a99_idsMustMatchCardIdGrammar() {
		var msg = "E-76: data source '%s': id must match ^[A-Za-z][A-Za-z0-9_-]{0,63}$";
		assertEquals(msg.formatted("1st"), assertThrows(IllegalArgumentException.class, () -> Derived.from("1st"))
			.getMessage());
		var target6 = Derived.from("a");
		assertEquals(msg.formatted("a.b"),
			assertThrows(IllegalArgumentException.class, () -> target6.as("a.b")).getMessage());
	}
}
