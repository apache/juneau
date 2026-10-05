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

import static org.apache.juneau.rest.server.console.test.PageContractAssert.*;
import static org.apache.juneau.rest.server.view.freemarker.console.C1Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Strict booleans (spec §4.4, E-5) for {@code <@node visible|selected>} and {@code <@console chrome>}.
 *
 * @since 10.0.0
 */
class NodeDirective_StrictBoolean_Test extends TestBase {

	@ParameterizedTest
	@CsvSource({"bool-visible-true,true", "bool-visible-false,false", "bool-visible-model,false"})
	void a01_visible(String fixture, boolean shown) {
		var a = assertPage(render(fixture)).isValid();
		assertEquals(shown ? 1 : 0, a.contract().getList("nav").size());
	}

	@ParameterizedTest
	@CsvSource({"bool-selected-true", "bool-selected-model"})
	void a02_selected(String fixture) {
		assertPage(render(fixture)).isValid().hasActiveNav("a");
	}

	@ParameterizedTest
	@CsvSource({"bool-chrome-true,true", "bool-chrome-model,true", "bool-chrome-false,false"})
	void a03_chrome(String fixture, boolean on) {
		var h = assertPage(render(fixture)).contract().getMap("header");
		assertEquals(on, h != null && Boolean.TRUE.equals(h.get("chrome")));
	}

	@ParameterizedTest
	@CsvSource(delimiter='|', value={
		"bool-visible-tru|<@node> visible= must be true or false; got 'tru'.",
		"bool-visible-empty|<@node> visible= must be true or false; got ''.",
		"bool-selected-tru|<@node> selected= must be true or false; got 'tru'.",
		"bool-selected-empty|<@node> selected= must be true or false; got ''.",
		"bool-chrome-tru|<@console> chrome= must be true or false; got 'tru'.",
		"bool-chrome-empty|<@console> chrome= must be true or false; got ''."
	})
	void a04_rejected_E5(String fixture, String message) {
		assertError(fixture, message);
	}
}
