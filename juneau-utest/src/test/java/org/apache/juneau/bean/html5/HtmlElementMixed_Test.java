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
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class HtmlElementMixed_Test extends TestBase {

	@Test void a01_getChild_nullChildren() {
		P x = new P();
		assertNull(x.getChild(0));
	}

	@Test void a02_getChild_outOfBounds() {
		P x = p("child1");
		assertNull(x.getChild(5));
		assertNull(x.getChild(-1));
	}

	@Test void a03_getChild_valid() {
		P x = p("child1", "child2");
		assertString("child1", x.getChild(0));
		assertString("child2", x.getChild(1));
	}

	@Test void a04_getChild_typed_nullChildren() {
		P x = new P();
		assertNull(x.getChild(String.class, 0));
	}

	@Test void a05_getChild_typed_outOfBounds() {
		P x = p("child1");
		assertNull(x.getChild(String.class, 5));
		assertNull(x.getChild(String.class, -1));
	}

	@Test void a06_getChild_typed_valid() {
		P x = p("child1", "child2");
		assertString("child1", x.getChild(String.class, 0));
		assertString("child2", x.getChild(String.class, 1));
	}

	@Test void a07_child_withCollection() {
		P x = new P();
		x.child(java.util.Arrays.asList("text1", "text2"));
		assertString("<p>text1text2</p>", x);
	}

	@Test void a08_child_withSingleValue() {
		P x = new P();
		x.child("text1");
		assertString("<p>text1</p>", x);
	}

	@Test void a09_getChildren() {
		P x1 = new P();
		assertNull(x1.getChildren());
		
		P x2 = p("child1", "child2");
		assertString("[child1,child2]", x2.getChildren());
	}

	@Test void a10_children_emptyArray() {
		P x = new P();
		x.children();
		assertNull(x.getChildren());
	}

	@Test void a11_children_withValues() {
		P x = new P();
		x.children("child1", "child2");
		assertString("[child1,child2]", x.getChildren());
	}

	@Test void a12_getChild_varargs_emptyArray() {
		P x = p("child1");
		assertNull(x.getChild(new int[]{}));
	}

	@Test void a13_getChild_varargs_singleIndex() {
		P x = p("child1", "child2");
		assertString("child1", x.getChild(new int[]{0}));
	}

	@Test void a14_getChild_varargs_multipleIndices() {
		// Create nested structure: p -> span -> strong -> "text"
		P x = p(
			span(
				strong("text1"),
				strong("text2")
			),
			span("text3")
		);
		
		// Navigate to nested elements
		assertString("text1", x.getChild(0, 0, 0));
		assertString("text2", x.getChild(0, 1, 0));
		assertString("text3", x.getChild(1, 0));
	}

	@Test void a15_getChild_varargs_invalidPath() {
		P x = p("text");
		// Try to get child of a text node (which is not a container or mixed element)
		assertNull(x.getChild(0, 0));
	}

	@Test void a16_getChild_varargs_throughContainer() {
		// Test navigation from Mixed through Container element
		P x = p(
			div(
				span("nested")
			)
		);
		
		assertString("nested", x.getChild(0, 0, 0));
	}

	@Test void a17_setChildren() {
		P x = new P();
		java.util.List<Object> children = java.util.Arrays.asList("child1", "child2");
		x.setChildren(children);
		assertString("[child1,child2]", x.getChildren());
	}

	@Test void a18_child_withCollection_existingChildren() {
		// Test adding a collection when children list already exists
		P x = new P();
		x.child("existing");  // Initialize children list
		x.child(java.util.Arrays.asList("new1", "new2"));  // Add collection to existing list
		assertString("<p>existingnew1new2</p>", x);
	}
}