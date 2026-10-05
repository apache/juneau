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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class CardSpec_Test extends TestBase {

	@Test void a01_htmlToMap_keyOrder() {
		var c = CardSpec.html("intro").title("Welcome").template("intro");
		assertEquals("{id=intro, type=html, title=Welcome, template=intro}", c.toMap().toString());
	}

	@Test void a02_bareOnlyWhenTrue() {
		assertEquals("{id=s, type=html, template=s, bare=true}", CardSpec.html("s").template("s").bare(true).toMap().toString());
		assertFalse(CardSpec.html("s").template("s").toMap().containsKey("bare"));
	}

	@Test void a03_bodyFieldsAfterCommonFields() {
		var c = CardSpec.of("datatables", "releases").body("table", Map.of("dataUrl", "/x")).title("Releases");
		assertEquals("{id=releases, type=datatables, title=Releases, table={dataUrl=/x}}", c.toMap().toString());
		assertBean(c, "type,id", "datatables,releases");
	}
}
