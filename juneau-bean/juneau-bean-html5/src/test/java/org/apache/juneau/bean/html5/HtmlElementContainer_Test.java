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
package org.apache.juneau.bean.html5;

import static org.apache.juneau.bean.html5.HtmlBuilder.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class HtmlElementContainer_Test extends TestBase {

	@Test void a01_getChild() {
		var x = new Div();
		assertNull(new Div().getChild(0));
		assertNull(x.getChild(String.class, 0));

		x = div("child1");
		assertNull(x.getChild(5));
		assertNull(x.getChild(-1));
		assertNull(x.getChild(String.class, 5));
		assertNull(x.getChild(String.class, -1));

		x = div("child1", "child2");
		assertString("child1", x.getChild(0));
		assertString("child2", x.getChild(1));
		assertString("child1", x.getChild(String.class, 0));
		assertString("child2", x.getChild(String.class, 1));

		x = new Div();
		assertNull(x.getChildren());
		x.children();
		assertNull(x.getChildren());

		x = div("child1", "child2");
		assertString("[child1,child2]", x.getChildren());
		x.children("child1", "child2");
		assertString("[child1,child2,child1,child2]", x.getChildren());

		x = new Div();
		x.child("child1");
		assertString("[child1]", x.getChildren());

		x = div("child1");
		assertNull(x.getChild(ints()));

		x = div("child1", "child2");
		assertString("child1", x.getChild(ints(0)));

		x = div(
			div(
				div("text1", "text2"),
				div("text3")
			),
			div("text4")
		);

		// Navigate to nested elements
		assertString("text1", x.getChild(0, 0, 0));
		assertString("text2", x.getChild(0, 0, 1));
		assertString("text3", x.getChild(0, 1, 0));
		assertString("text4", x.getChild(1, 0));

		x = div("text");
		assertNull(x.getChild(0, 0));// Try to get child of a text node (which is not a container)
	}

	@Test void a02_getChild_varargs_mixedElement() {
		// Test navigation through HtmlElementMixed
		P x = p(
			span("text1"),
			span("text2")
		);

		assertString("text1", x.getChild(0, 0));
		assertString("text2", x.getChild(1, 0));
	}

	@Test void a03_setChildren() {
		var x = new Div();
		java.util.List<Object> children = l("child1", "child2");
		x.setChildren(children);
		assertString("[child1,child2]", x.getChildren());
	}

	@Test void a04_container_getChild_typed() {
		// Use Table (extends HtmlElementContainer directly) to test HtmlElementContainer.getChild(Class, int)
		var x = new Table();
		assertNull(x.getChild(String.class, 0));   // children == null branch

		var t = table("row1", "row2");
		assertNull(t.getChild(String.class, 5));   // children.size() <= index branch
		assertNull(t.getChild(String.class, -1));  // index < 0 branch
		assertString("row1", t.getChild(String.class, 0));  // valid branch
	}

	@Test void a05_container_getChild_int() {
		// Use Table to test HtmlElementContainer.getChild(int)
		var x = new Table();
		assertNull(x.getChild(0));                 // children == null → true branch

		var t = table("row1", "row2");
		assertNull(t.getChild(5));                 // children.size() <= index → true branch
		assertNull(t.getChild(-1));                // index < 0 → true branch (need non-null children to reach this)
		assertString("row1", t.getChild(0));       // valid path
	}

	@Test void a06_container_children_emptyArgs() {
		// Use Table to test HtmlElementContainer.children() with empty array (false branch of value.length > 0)
		var x = new Table();
		x.children();
		assertNull(x.getChildren());
	}

	@Test void a07_container_getChild_varargs() {
		// Use Table to test HtmlElementContainer.getChild(int...) vararg paths
		var t = table("row1");

		assertNull(t.getChild(ints()));            // index.length == 0 branch
		assertString("row1", t.getChild(ints(0))); // index.length == 1 branch

		// Navigate through nested HtmlElementContainer elements: Table[0]=Tr, Tr[0]="cell1"
		var x = new Table();
		x.children(tr("cell1"));
		assertString("cell1", x.getChild(0, 0));

		// Navigate: Table[0]=Tr (HtmlElementContainer), Tr[0]=Td (HtmlElementMixed), Td[0]="data"
		// This exercises the HtmlElementContainer else-if branch (Tr is a HtmlElementContainer)
		var y = table(tr(td("data")));
		assertString("data", y.getChild(0, 0, 0));

		// Try to navigate into a non-container leaf (returns null - else branch)
		var z = table("text");
		assertNull(z.getChild(0, 0));
	}
}