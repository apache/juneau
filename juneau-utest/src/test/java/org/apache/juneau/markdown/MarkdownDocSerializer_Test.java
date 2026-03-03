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
package org.apache.juneau.markdown;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.*;

/**
 * Tests for {@link MarkdownDocSerializer} and {@link MarkdownDocSerializerSession}.
 */
class MarkdownDocSerializer_Test {

	//-----------------------------------------------------------------------------------------------------------------
	// b01 - Simple bean with title
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_simpleBeanDoc() throws Exception {
		var s = MarkdownDocSerializer.create().title("Person").build();
		var bean = new A();
		bean.name = "Alice";
		bean.age = 30;
		var md = s.serialize(bean);
		assertTrue(md.contains("# Person"), "Expected H1 title: " + md);
		assertTrue(md.contains("| Property | Value |"), "Expected header row: " + md);
		assertTrue(md.contains("| name | Alice |"), "Expected name row: " + md);
		assertTrue(md.contains("| age | 30 |"), "Expected age row: " + md);
	}

	public static class A {
		public String name;
		public int age;
		public A() {}
		public A(String name, int age) { this.name = name; this.age = age; }
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b02 - Nested bean gets H2 sub-heading
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b02_nestedBeanDoc() throws Exception {
		var s = MarkdownDocSerializer.create().title("Person").build();
		var bean = new B();
		bean.name = "Alice";
		bean.age = 30;
		bean.address = new Address();
		bean.address.city = "Boston";
		bean.address.state = "MA";
		var md = s.serialize(bean);
		assertTrue(md.contains("# Person"), "Expected H1: " + md);
		assertTrue(md.contains("## address"), "Expected H2 for address: " + md);
		assertTrue(md.contains("| city | Boston |"), "Expected city row: " + md);
		assertTrue(md.contains("| state | MA |"), "Expected state row: " + md);
	}

	public static class B {
		public String name;
		public int age;
		public Address address;
	}

	public static class Address {
		public String city;
		public String state;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b03 - Deeply nested (3 levels)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b03_deeplyNestedDoc() throws Exception {
		var s = MarkdownDocSerializer.create().title("Root").build();
		var bean = new C();
		bean.name = "root";
		bean.mid = new Mid();
		bean.mid.name = "mid";
		bean.mid.inner = new Address();
		bean.mid.inner.city = "Boston";
		bean.mid.inner.state = "MA";
		var md = s.serialize(bean);
		assertTrue(md.contains("# Root"), "Expected H1: " + md);
		assertTrue(md.contains("## mid"), "Expected H2: " + md);
		assertTrue(md.contains("### inner"), "Expected H3: " + md);
		assertTrue(md.contains("| city | Boston |"), "Expected city: " + md);
	}

	public static class C {
		public String name;
		public Mid mid;
	}

	public static class Mid {
		public String name;
		public Address inner;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b04 - Collection property gets H2 + multi-column table
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b04_beanWithCollectionDoc() throws Exception {
		var s = MarkdownDocSerializer.create().title("Report").build();
		var bean = new D();
		bean.name = "Summary";
		bean.items = List.of(new A("Alice", 30), new A("Bob", 25));
		var md = s.serialize(bean);
		assertTrue(md.contains("# Report"), "Expected H1: " + md);
		assertTrue(md.contains("## items"), "Expected H2 for items: " + md);
		assertTrue(md.contains("| name |") || md.contains("| age |"), "Expected multi-column header with name/age: " + md);
		assertTrue(md.contains("Alice"), "Expected Alice: " + md);
		assertTrue(md.contains("Bob"), "Expected Bob: " + md);
	}

	public static class D {
		public String name;
		public List<A> items;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b05 - String list property gets H2 + bulleted list
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b05_beanWithListOfStrings() throws Exception {
		var s = MarkdownDocSerializer.create().title("Report").build();
		var bean = new E();
		bean.name = "Tags";
		bean.tags = List.of("alpha", "beta", "gamma");
		var md = s.serialize(bean);
		assertTrue(md.contains("# Report"), "Expected H1: " + md);
		assertTrue(md.contains("## tags"), "Expected H2 for tags: " + md);
		assertTrue(md.contains("- alpha"), "Expected bullet alpha: " + md);
		assertTrue(md.contains("- beta"), "Expected bullet beta: " + md);
		assertTrue(md.contains("- gamma"), "Expected bullet gamma: " + md);
	}

	public static class E {
		public String name;
		public List<String> tags;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b06 - Custom title
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b06_customTitle() throws Exception {
		var s = MarkdownDocSerializer.create().title("My Report").build();
		var bean = new A();
		bean.name = "Alice";
		bean.age = 30;
		var md = s.serialize(bean);
		assertTrue(md.contains("# My Report"), "Expected custom title: " + md);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b07 - Custom heading level
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b07_customHeadingLevel() throws Exception {
		var s = MarkdownDocSerializer.create().title("Report").headingLevel(2).build();
		var bean = new A();
		bean.name = "Alice";
		bean.age = 30;
		var md = s.serialize(bean);
		assertTrue(md.contains("## Report"), "Expected H2 title: " + md);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b08 - Horizontal rules between sections
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b08_horizontalRules() throws Exception {
		var s = MarkdownDocSerializer.create().title("Person").addHorizontalRules(true).build();
		var bean = new B();
		bean.name = "Alice";
		bean.address = new Address();
		bean.address.city = "Boston";
		var md = s.serialize(bean);
		assertTrue(md.contains("---"), "Expected horizontal rule: " + md);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b09 - Header content prepended
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b09_headerContent() throws Exception {
		var s = MarkdownDocSerializer.create().title("Doc").headerContent("---\nlayout: post\n---").build();
		var bean = new A();
		bean.name = "Alice";
		var md = s.serialize(bean);
		assertTrue(md.contains("---"), "Expected header content: " + md);
		assertTrue(md.contains("layout: post"), "Expected layout in header: " + md);
		assertTrue(md.contains("# Doc"), "Expected title after header: " + md);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b10 - Footer content appended
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b10_footerContent() throws Exception {
		var s = MarkdownDocSerializer.create().title("Doc").footerContent("*Generated by Juneau*").build();
		var bean = new A();
		bean.name = "Alice";
		var md = s.serialize(bean);
		assertTrue(md.contains("*Generated by Juneau*"), "Expected footer: " + md);
		assertTrue(md.indexOf("# Doc") < md.indexOf("*Generated by Juneau*"), "Footer should be after content");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b11 - Simple properties appear before nested sections
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b11_propertyOrdering() throws Exception {
		var s = MarkdownDocSerializer.create().title("Person").build();
		var bean = new B();
		bean.name = "Alice";
		bean.age = 30;
		bean.address = new Address();
		bean.address.city = "Boston";
		var md = s.serialize(bean);
		int tablePos = md.indexOf("| Property | Value |");
		int addressPos = md.indexOf("## address");
		assertTrue(tablePos >= 0 && addressPos >= 0, "Expected both table and address section: " + md);
		assertTrue(tablePos < addressPos, "Table should appear before address section: " + md);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b12 - Multiple nested beans each get their own section
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b12_multipleNestedBeans() throws Exception {
		var s = MarkdownDocSerializer.create().title("Person").build();
		var bean = new F();
		bean.name = "Alice";
		bean.home = new Address();
		bean.home.city = "Boston";
		bean.home.state = "MA";
		bean.work = new Address();
		bean.work.city = "Cambridge";
		bean.work.state = "MA";
		var md = s.serialize(bean);
		assertTrue(md.contains("## home"), "Expected home section: " + md);
		assertTrue(md.contains("## work"), "Expected work section: " + md);
		assertTrue(md.contains("| city | Boston |") && md.contains("| city | Cambridge |"), "Expected both addresses: " + md);
	}

	public static class F {
		public String name;
		public Address home;
		public Address work;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b13 - Map property renders as H2 + key-value table
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b13_mapPropertyDoc() throws Exception {
		var s = MarkdownDocSerializer.create().title("Config").build();
		var bean = new G();
		bean.name = "settings";
		bean.settings = new LinkedHashMap<>();
		bean.settings.put("host", "localhost");
		bean.settings.put("port", 8080);
		var md = s.serialize(bean);
		assertTrue(md.contains("## settings"), "Expected settings section: " + md);
		assertTrue(md.contains("| Key | Value |") || md.contains("| host | localhost |"), "Expected map table: " + md);
	}

	public static class G {
		public String name;
		public Map<String, Object> settings;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b14 - Empty bean produces heading but empty/minimal table
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b14_emptyBeanDoc() throws Exception {
		var s = MarkdownDocSerializer.create().title("Empty").build();
		var bean = new A();
		bean.name = null;
		bean.age = 0;
		var md = s.serialize(bean);
		assertTrue(md.contains("# Empty"), "Expected title: " + md);
		assertTrue(md.contains("| Property | Value |") || md.contains("| age | 0 |"), "Expected table structure: " + md);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b15 - Root collection renders as table (no heading wrapping)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b15_collectionRootDoc() throws Exception {
		var s = MarkdownDocSerializer.create().title("Data").build();
		var list = List.of(new A("Alice", 30), new A("Bob", 25));
		var md = s.serialize(list);
		assertTrue(md.contains("# Data"), "Expected title: " + md);
		assertTrue(md.contains("| name |") || md.contains("| age |"), "Expected multi-column table: " + md);
		assertTrue(md.contains("Alice") && md.contains("Bob"), "Expected both rows: " + md);
	}
}
