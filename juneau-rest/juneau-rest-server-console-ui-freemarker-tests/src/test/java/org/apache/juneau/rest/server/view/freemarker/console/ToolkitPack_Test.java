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

class ToolkitPack_Test extends TestBase {

	@Test void a01_builder_copiesEveryField() {
		ToolkitPackRegistry.AssetUrlResolver echo = (req, path) -> path;
		var p = ToolkitPack.create("export").css("a.css").js("x.js", "y.js").dependsOn("datatables-buttons")
			.kind(ToolkitPack.Kind.VENDOR).resolver(echo).build();
		assertBean(p, "name,cssPaths,jsPaths,dependsOn,kind", "export,[a.css],[x.js,y.js],[datatables-buttons],VENDOR");
		assertSame(echo, p.resolver());
	}

	@Test void a02_listOverloads_andRepeatedCalls_append() {
		var p = ToolkitPack.create("p").js(List.of("a.js")).js("b.js").css(List.of("a.css")).dependsOn("x").dependsOn("y")
			.kind(ToolkitPack.Kind.RUNTIME).build();
		assertBean(p, "cssPaths,jsPaths,dependsOn,kind", "[a.css],[a.js,b.js],[x,y],RUNTIME");
	}

	@Test void a03_defaults_areEmptyLists() {
		var p = ToolkitPack.create("p").kind(ToolkitPack.Kind.VENDOR).build();
		assertBean(p, "cssPaths,jsPaths,dependsOn", "[],[],[]");
		assertNotNull(p.resolver());
	}

	@Test void a04_kind_isRequired() {
		var b = ToolkitPack.create("p").js("x.js");
		var e = assertThrows(IllegalArgumentException.class, b::build);
		assertString("Toolkit pack 'p' needs kind(VENDOR) or kind(RUNTIME).", e.getMessage());
	}

	@Test void a05_blankName_fails() {
		for (var n : new String[] { null, "", "  " }) {
			var e = assertThrows(IllegalArgumentException.class, () -> ToolkitPack.create(n));
			assertString("Toolkit pack name must not be blank.", e.getMessage());
		}
	}

	@Test void a06_nullKindOrResolver_fails() {
		var b = ToolkitPack.create("p");
		assertThrows(NullPointerException.class, () -> b.kind(null));
		assertThrows(NullPointerException.class, () -> b.resolver(null));
	}
}
