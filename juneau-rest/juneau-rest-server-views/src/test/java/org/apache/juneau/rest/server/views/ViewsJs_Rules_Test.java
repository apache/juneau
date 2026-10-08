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
import org.junit.jupiter.api.*;

/**
 * Cross-language parity proof: {@code rules.cjs} runs the shared {@code visibility-corpus.json} through
 * {@code JuneauViews.rules.test} (the Java side is {@link VisibilityRule_Corpus_Test}), then checks the
 * facts-aware row scope.  Runs when node is on PATH.
 */
class ViewsJs_Rules_Test extends TestBase {

	private static Map<?,?> report() {
		var r = NodeHarness.report("rules.cjs", ViewsMixin.VIEWS_JS_RESOURCE);
		assumeTrue(r != null, "node (or rules.cjs) not available - JS/Java parity layer skipped");
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

	@Test void a02_namespaceIsFrozen() {
		assertEquals(true, report().get("frozen"));
	}

	@Test void a03_rowScopeMergesRowAndFacts() {
		var r = report();
		assertEquals("{\"viewer\":{\"roles\":[\"admin\"]}}", r.get("factsRead"));
		assertEquals(true, r.get("rowScopeSeesFacts"));
		assertEquals(true, r.get("rowScopeSeesRow"));
		assertEquals(false, r.get("rowScopeBoth"));
	}

	@Test void a04_singleRuleObjectIsAListOfOne() {
		assertEquals(true, report().get("singleRuleObject"));
	}

	@Test void a05_noConsolePageMeansEmptyFactsAndFailsClosed() {
		var r = report();
		assertEquals("{}", r.get("noContractFacts"));
		assertEquals(false, r.get("noContractFailsClosed"));
	}

	@Test void a06_unknownOpHidesAndLogsE73() {
		var r = report();
		assertEquals(false, r.get("unknownOp"));
		assertEquals(true, r.get("unknownOpLogged"));
	}
}
