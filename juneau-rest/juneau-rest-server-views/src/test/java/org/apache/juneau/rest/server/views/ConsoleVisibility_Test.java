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
package org.apache.juneau.rest.server.views;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;

/**
 * Card-level {@code visibleWhen} in {@code juneau-console.js}: the shell hides a card whose rules do not match the
 * page's top-level {@code facts}, proven against the same corpus as the Java and views evaluators.  Runs when node is
 * on PATH.
 */
class ConsoleVisibility_Test extends TestBase {

	private static Map<?,?> report() {
		var r = NodeHarness.report("console-visibility.cjs", ConsoleChromeMixin.CONSOLE_JS_RESOURCE);
		assumeTrue(r != null, "node (or console-visibility.cjs) not available - JS layer skipped");
		return r;
	}

	@Test void a01_everyCorpusCaseAgreesWithJava() {
		var cases = (List<?>)report().get("cases");
		assertFalse(cases.isEmpty());
		for (var o : cases) {
			var m = (Map<?,?>)o;
			assertEquals(m.get("expected"), m.get("actual"), String.valueOf(m.get("name")));
		}
	}

	@Test void a02_noFactsFailsClosedExceptForCardsWithNoRule() {
		assertEquals(List.of("plain"), report().get("noFactsMounted"));
	}

	@Test void a03_singleRuleObjectIsAListOfOne() {
		assertEquals(List.of("single"), report().get("singleObjectMounted"));
	}

	@Test void a04_unknownOpHidesTheCardAndLogs() {
		var r = report();
		assertEquals(List.of(), r.get("unknownOpMounted"));
		assertEquals(true, r.get("unknownOpLogged"));
	}
}
