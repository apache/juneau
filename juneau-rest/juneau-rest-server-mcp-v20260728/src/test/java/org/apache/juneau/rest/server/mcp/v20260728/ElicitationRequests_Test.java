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
package org.apache.juneau.rest.server.mcp.v20260728;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@link ElicitationRequests}.
 */
class ElicitationRequests_Test {

	@Test void a01_of_singleQuestion_buildsMapShapedValue() {
		var a = new ElicitRequest().setMessage("Pick one").setRequestedSchema(ElicitSchema.create().stringField("choice").build());
		var signal = ElicitationRequests.of("q1", a, "cont");
		var raw = (JsonMap)signal.getInputRequests().get("q1");
		assertEquals("Pick one", raw.getString("message"));
		var schema = raw.getMap("requestedSchema");
		var choice = schema.getMap("properties").getMap("choice");
		assertEquals("string", choice.getString("type"));
		assertEquals("cont", signal.getContinuation());
	}

	@Test void a02_of_multiQuestion_preservesAllKeysAndOrder() {
		var a1 = new ElicitRequest().setMessage("Question 1");
		var a2 = new ElicitRequest().setMessage("Question 2");
		var requests = new LinkedHashMap<String,ElicitRequest>();
		requests.put("q1", a1);
		requests.put("q2", a2);
		var signal = ElicitationRequests.of(requests, "cont");
		var keys = new ArrayList<>(signal.getInputRequests().keySet());
		assertEquals(List.of("q1", "q2"), keys);
		assertEquals("Question 1", ((JsonMap)signal.getInputRequests().get("q1")).getString("message"));
		assertEquals("Question 2", ((JsonMap)signal.getInputRequests().get("q2")).getString("message"));
	}

	@Test void a03_of_singleQuestion_nullIdThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> ElicitationRequests.of(null, new ElicitRequest(), "cont"));
		assertEquals("Argument 'id' cannot be null.", e.getMessage());
	}

	@Test void a04_of_singleQuestion_nullRequestThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> ElicitationRequests.of("q1", null, "cont"));
		assertEquals("Argument 'request' cannot be null.", e.getMessage());
	}

	@Test void a05_of_multiQuestion_nullMapThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> ElicitationRequests.of((Map<String,ElicitRequest>)null, "cont"));
		assertEquals("Argument 'requests' cannot be null.", e.getMessage());
	}

	@Test void a06_of_multiQuestion_emptyMapThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> ElicitationRequests.of(Map.of(), "cont"));
		assertEquals("requests must not be empty", e.getMessage());
	}

	@Test void a07_of_singleQuestion_nullContinuationAllowed() {
		var signal = ElicitationRequests.of("q1", new ElicitRequest().setMessage("Pick one"), null);
		assertNull(signal.getContinuation());
	}

	@Test void a08_of_multiQuestion_nullValueThrows() {
		var requests = new LinkedHashMap<String,ElicitRequest>();
		requests.put("q1", new ElicitRequest().setMessage("Question 1"));
		requests.put("q2", null);
		var e = assertThrows(IllegalArgumentException.class, () -> ElicitationRequests.of(requests, "cont"));
		assertEquals("Argument 'requests[q2]' cannot be null.", e.getMessage());
	}
}
