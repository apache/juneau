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

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;

class ConsoleFreemarkerMixin_CardType_Test extends TestBase {

	@Test void a01_reservedType_throwsE22AtBuilder() {
		var b = ConsoleFreemarkerMixin.create();
		var arg = new Named("datatables");
		var ex = assertThrows(IllegalArgumentException.class, () -> b.cardType(arg));
		assertTrue(ex.getMessage().startsWith("Card type 'datatables' is reserved and cannot be replaced; reserved: '"), ex.getMessage());
	}

	@Test void a02_badName_throwsE21AtBuilder() {
		var target = ConsoleFreemarkerMixin.create();
		var arg2 = new Named("KPI");
		assertThrows(IllegalArgumentException.class, () -> target.cardType(arg2));
	}

	@Test void a03_duplicate_throwsE20AtBuilder() {
		var b = ConsoleFreemarkerMixin.create().cardType(new Named("kpi"));
		var arg3 = new Named("kpi");
		assertThrows(IllegalArgumentException.class, () -> b.cardType(arg3));
	}

	@Test void a04_terminalIsRegisteredInStandard() {
		assertTrue(CardTypeRegistry.standard().handler("terminal") instanceof TerminalCardType);
	}

	private static final class Named implements CardTypeHandler {
		private final String type;
		Named(String type) { this.type = type; }
		@Override public String type() { return type; }
		@Override public JsonMap toFragment(CardSource source) { return new JsonMap(); }
	}
}
