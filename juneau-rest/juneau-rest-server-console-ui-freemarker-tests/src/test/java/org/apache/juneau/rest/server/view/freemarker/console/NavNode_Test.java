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

class NavNode_Test extends TestBase {

	@Test void a01_addFindAnyDepth() throws Exception {
		var root = NavNode.root();
		var a = root.add("a", "A", null);
		var b = a.add("b", "B", null);
		var c = b.add("c", "C", null);
		c.add("d", "D", "/a/b/c/d");
		assertEquals("d", root.find(List.of("a", "b", "c", "d")).orElseThrow().id());
		assertTrue(root.find(List.of("a", "x")).isEmpty());
		assertEquals(List.of("a", "b"), b.idPath());
	}

	@Test void a02_duplicateSibling_isE3() throws Exception {
		var root = NavNode.root();
		root.add("a", "A", "/a");
		var e = assertThrows(Exception.class, () -> root.add("a", "A2", "/a2"));
		assertEquals("<@node id='a'> duplicates a sibling id under '(root)'.", e.getMessage());
	}

	@Test void a03_paths() throws Exception {
		var root = NavNode.root();
		root.add("a", "A", "/a").add("b", "B", "/a/b");
		root.add("c", "C", "/c");
		assertEquals(List.of("a", "a/b", "c"), root.paths());
	}

	@Test void a04_toList() throws Exception {
		var root = NavNode.root();
		root.add("a", "A", null).add("b", "B", "/b");
		assertEquals("[{id=a, label=A, children=[{id=b, label=B, href=/b}]}]", root.toList().toString());
	}
}
