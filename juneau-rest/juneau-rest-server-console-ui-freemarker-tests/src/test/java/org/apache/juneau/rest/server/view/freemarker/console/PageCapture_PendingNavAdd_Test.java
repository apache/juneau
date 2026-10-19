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
import org.junit.jupiter.api.*;

import freemarker.template.*;

/**
 * {@link PageCapture}'s pending nav additions: resolution happens later, at {@code </@console>}.
 *
 * @since 10.0.0
 */
class PageCapture_PendingNavAdd_Test extends TestBase {

	@Test void a01_resolvesAgainstExistingPath() throws TemplateModelException {
		var cap = new PageCapture();
		cap.navRoot().add("fleet", "Fleet", null).add("instances", "Instances", "/fleet/instances");
		var added = new ArrayList<NavNode>();
		cap.addPendingNavUnder("fleet/instances", added::add);
		cap.applyPendingNavAdds();
		assertEquals(1, added.size());
		assertEquals("instances", added.get(0).id());
	}

	@Test void a02_appliesInDeclarationOrder() throws TemplateModelException {
		var cap = new PageCapture();
		cap.navRoot().add("fleet", "Fleet", "/fleet");
		var order = new ArrayList<String>();
		cap.addPendingNavUnder("fleet", x -> order.add("first"));
		cap.addPendingNavUnder("fleet", x -> order.add("second"));
		cap.applyPendingNavAdds();
		assertEquals(List.of("first", "second"), order);
	}

	@Test void a03_unknownPathRejected() throws TemplateModelException {
		var cap = new PageCapture();
		cap.navRoot().add("fleet", "Fleet", "/fleet");
		cap.addPendingNavUnder("nope", x -> {});
		var ex = assertThrows(TemplateModelException.class, cap::applyPendingNavAdds);
		assertEquals("Nav addition under 'nope' does not match a <@node> path in the chrome; known paths: 'fleet'.", ex.getMessage());
	}

	@Test void a04_noPendingIsNoOp() {
		var cap = new PageCapture();
		assertDoesNotThrow(cap::applyPendingNavAdds);
	}

	@Test void a05_unknownPathListsEveryKnownPath() throws TemplateModelException {
		var cap = new PageCapture();
		var fleet = cap.navRoot().add("fleet", "Fleet", null);
		fleet.add("instances", "Instances", "/fleet/instances");
		cap.navRoot().add("home", "Home", "/home");
		cap.addPendingNavUnder("nope", x -> {});
		var ex = assertThrows(TemplateModelException.class, cap::applyPendingNavAdds);
		assertEquals("Nav addition under 'nope' does not match a <@node> path in the chrome; known paths: 'fleet, fleet/instances, home'.", ex.getMessage());
	}

	@Test void a06_adderMayThrowAndDuplicateSiblingIdIsRejected() throws TemplateModelException {
		var cap = new PageCapture();
		cap.navRoot().add("fleet", "Fleet", "/fleet");
		cap.addPendingNavUnder("fleet", parent -> {
			parent.add("x", "X", "/x");
			parent.add("x", "X again", "/x2");
		});
		var ex = assertThrows(TemplateModelException.class, cap::applyPendingNavAdds);
		assertEquals("<@node id='x'> duplicates a sibling id under 'fleet'.", ex.getMessage());
	}

	@Test void a07_secondCallIsNoOp() throws TemplateModelException {
		var cap = new PageCapture();
		cap.navRoot().add("fleet", "Fleet", "/fleet");
		var count = new int[1];
		cap.addPendingNavUnder("fleet", x -> count[0]++);
		cap.applyPendingNavAdds();
		cap.applyPendingNavAdds();
		assertEquals(1, count[0]);
	}

	@Test void a08_adderMayRegisterAnotherAdder() throws TemplateModelException {
		var cap = new PageCapture();
		cap.navRoot().add("fleet", "Fleet", "/fleet");
		var order = new ArrayList<String>();
		cap.addPendingNavUnder("fleet", x -> {
			order.add("first");
			cap.addPendingNavUnder("fleet", y -> order.add("second"));
		});
		cap.applyPendingNavAdds();
		assertEquals(List.of("first"), order);
		cap.applyPendingNavAdds();
		assertEquals(List.of("first", "second"), order);
	}
}
