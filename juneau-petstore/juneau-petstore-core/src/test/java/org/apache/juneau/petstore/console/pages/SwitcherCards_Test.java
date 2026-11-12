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
package org.apache.juneau.petstore.console.pages;

import static org.apache.juneau.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.console.views.*;
import org.apache.juneau.console.views.TableSpec.*;
import org.junit.jupiter.api.*;

class SwitcherCards_Test extends TestBase {

	@Test void a01_tableBecomesADatatablesCardWithTheTableBody() {
		var t = TableSpec.create("t1").dataMode(DataMode.CLIENT).dataUrl("/x/rows")
			.columns(Column.create("a").label("A"));
		var c = SwitcherCards.table(t).toMap();
		assertBean(c, "type,id", "datatables,t1");
		assertEquals("client", c.get("dataMode"));
		assertEquals("/x/rows", c.get("dataUrl"));
	}

	@Test void a02_invalidTableIsRejected() {
		var bad = TableSpec.create("bad");
		assertThrows(IllegalStateException.class, () -> SwitcherCards.table(bad));
	}

	@Test void a03_tableModelIsNotCopiedAsBody() {
		var t = TableSpec.create("t2").dataMode(DataMode.CLIENT).dataUrl("/x/rows")
			.columns(Column.create("a").label("A"));
		assertDoesNotThrow(() -> SwitcherCards.table(t));
	}
}
