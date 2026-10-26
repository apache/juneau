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
package org.apache.juneau.console.widgets;

import static org.apache.juneau.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * {@link ActionRef} construction and enabled-rule validation.
 */
class ActionRef_Test extends TestBase {

	@Test void a01_of_defaults() {
		var a = ActionRef.of("approve");
		assertEquals("approve", a.id);
		assertEquals(ActionRef.Emphasis.SECONDARY, a.emphasis);
		assertNull(a.enabledWhen);
	}

	@Test void a02_of_blankOrNull_rejected() {
		assertThrows(IllegalArgumentException.class, () -> ActionRef.of(null));
		assertThrows(IllegalArgumentException.class, () -> ActionRef.of("  "));
	}

	@Test void a03_emphasis_setsAndReturnsThis() {
		var a = ActionRef.of("x");
		assertSame(a, a.emphasis(ActionRef.Emphasis.PRIMARY));
		assertEquals(ActionRef.Emphasis.PRIMARY, a.emphasis);
	}

	@Test void a04_enabledWhen_valueForm_recordsRulesInOrder() {
		var a = ActionRef.of("x")
			.enabledWhen("status", Op.EQ, "OPEN", "Must be open")
			.enabledWhen("kind", Op.NE, "legacy", "Not for legacy");
		assertSize(2, a.enabledWhen);
		assertBean(a.enabledWhen.get(0), "field,op,value,reason", "status,EQ,OPEN,Must be open");
		assertBean(a.enabledWhen.get(1), "field,op,value,reason", "kind,NE,legacy,Not for legacy");
	}

	@Test void a05_enabledWhen_presenceForm_recordsRuleWithNullValue() {
		var a = ActionRef.of("x").enabledWhen("owner", Op.PRESENT, "Needs an owner");
		assertSize(1, a.enabledWhen);
		assertBean(a.enabledWhen.get(0), "field,op,reason", "owner,PRESENT,Needs an owner");
		assertNull(a.enabledWhen.get(0).value);
		a.enabledWhen("owner", Op.ABSENT, "Must be unowned");
		assertEquals(Op.ABSENT, a.enabledWhen.get(1).op);
	}

	@Test void a06_enabledWhen_valueForm_badOp_rejected() {
		var a = ActionRef.of("x");
		assertThrows(IllegalArgumentException.class, () -> a.enabledWhen("f", Op.IN, "v", "r"));
		assertThrows(IllegalArgumentException.class, () -> a.enabledWhen("f", Op.CONTAINS, "v", "r"));
		assertThrows(IllegalArgumentException.class, () -> a.enabledWhen("f", Op.PRESENT, "v", "r"));
		assertThrows(IllegalArgumentException.class, () -> a.enabledWhen("f", Op.ABSENT, "v", "r"));
		assertThrows(IllegalArgumentException.class, () -> a.enabledWhen("f", null, "v", "r"));
	}

	@Test void a07_enabledWhen_valueForm_nullValue_rejected() {
		var a = ActionRef.of("x");
		assertThrows(IllegalArgumentException.class, () -> a.enabledWhen("f", Op.EQ, null, "r"));
	}

	@Test void a08_enabledWhen_presenceForm_badOp_rejected() {
		var a = ActionRef.of("x");
		assertThrows(IllegalArgumentException.class, () -> a.enabledWhen("f", Op.EQ, "r"));
		assertThrows(IllegalArgumentException.class, () -> a.enabledWhen("f", Op.IN, "r"));
		assertThrows(IllegalArgumentException.class, () -> a.enabledWhen("f", null, "r"));
	}

	@Test void a09_enabledWhen_blankFieldOrReason_rejected() {
		var a = ActionRef.of("x");
		assertThrows(IllegalArgumentException.class, () -> a.enabledWhen(null, Op.EQ, "v", "r"));
		assertThrows(IllegalArgumentException.class, () -> a.enabledWhen(" ", Op.PRESENT, "r"));
		assertThrows(IllegalArgumentException.class, () -> a.enabledWhen("f", Op.EQ, "v", null));
		assertThrows(IllegalArgumentException.class, () -> a.enabledWhen("f", Op.PRESENT, " "));
		assertNull(a.enabledWhen);
	}
}
