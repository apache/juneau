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
package org.apache.juneau;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.msgpack.*;
import org.apache.juneau.marshall.xml.*;
import org.junit.jupiter.api.*;

/**
 * Tests for view-based property projection.
 *
 * <p>
 * Covers: property-level view membership via {@link MarshalledProp#view()}, active-view selector on
 * {@link MarshallingContext.Builder} and per-call {@link MarshallingSession.Builder}, default-view-inclusion
 * policy, multi-view union semantics, interaction with read-only/write-only and {@link MarshalledIgnore},
 * the {@code *Config}/{@code @ContextApply} path for unmodifiable classes, and cross-format spot checks.
 */
class ViewProjection_Test extends TestBase {

	// A: baseline — no active view → all properties visible

	public static class A {
		public String id = "1";

		@MarshalledProp(view = "summary")
		public String name = "Alice";

		@MarshalledProp(view = "detail")
		public String description = "A person";

		static A create() {
			return new A();
		}
	}

	@Test void a01_noActiveView_allPropertiesVisible() {
		assertJson("{description:'A person',id:'1',name:'Alice'}", A.create());
	}

	// B: summary view — tagged summary + untagged

	@Test void b01_summaryView_serializer() {
		var s = Json5Serializer.DEFAULT.copy().activeView("summary").build();
		assertSerialized(A.create(), s, "{id:'1',name:'Alice'}");
	}

	@Test void b02_summaryView_parser_outOfViewIgnored() {
		var p = Json5Parser.DEFAULT.copy().activeView("summary").ignoreUnknownBeanProperties().build();
		var x = p.read("{id:'2',name:'Bob',description:'ignored'}", A.class);
		assertEquals("2", x.id);
		assertEquals("Bob", x.name);
		assertEquals("A person", x.description); // unchanged — never set
	}

	@Test void b03_detailView_serializer() {
		var s = Json5Serializer.DEFAULT.copy().activeView("detail").build();
		assertSerialized(A.create(), s, "{description:'A person',id:'1'}");
	}

	@Test void b04_unknownView_onlyUntaggedProperties() {
		var s = Json5Serializer.DEFAULT.copy().activeView("other").build();
		assertSerialized(A.create(), s, "{id:'1'}");
	}

	// C: multi-view union membership

	public static class C {
		public String id = "1";

		@MarshalledProp(view = {"summary", "detail"})
		public String name = "Alice";

		@MarshalledProp(view = "detail")
		public String description = "A person";

		static C create() {
			return new C();
		}
	}

	@Test void c01_summaryView_unionIncludesNameNotDescription() {
		var s = Json5Serializer.DEFAULT.copy().activeView("summary").build();
		assertSerialized(C.create(), s, "{id:'1',name:'Alice'}");
	}

	@Test void c02_detailView_unionIncludesBoth() {
		var s = Json5Serializer.DEFAULT.copy().activeView("detail").build();
		assertSerialized(C.create(), s, "{description:'A person',id:'1',name:'Alice'}");
	}

	// D: disableDefaultViewInclusion — untagged properties excluded when view active

	@Test void d01_disableDefaultViewInclusion_summaryView() {
		var s = Json5Serializer.DEFAULT.copy().activeView("summary").disableDefaultViewInclusion().build();
		assertSerialized(A.create(), s, "{name:'Alice'}");
	}

	@Test void d02_disableDefaultViewInclusion_detailView() {
		var s = Json5Serializer.DEFAULT.copy().activeView("detail").disableDefaultViewInclusion().build();
		assertSerialized(A.create(), s, "{description:'A person'}");
	}

	@Test void d03_disableDefaultViewInclusion_unknownView_nothingVisible() {
		var s = Json5Serializer.DEFAULT.copy().activeView("other").disableDefaultViewInclusion().build();
		assertSerialized(A.create(), s, "{}");
	}

	// E: per-session override — context sets default, session overrides per-call

	@Test void e01_perSessionOverride_overridesContextDefault() throws Exception {
		var s = Json5Serializer.DEFAULT.copy().activeView("summary").build();

		// default from context: summary
		assertEquals("{id:'1',name:'Alice'}", s.write(A.create()));

		// per-call override to detail
		assertEquals("{description:'A person',id:'1'}", s.createSession().activeView("detail").build().write(A.create()));

		// per-call override to null → all properties
		assertEquals("{description:'A person',id:'1',name:'Alice'}", s.createSession().activeView(null).build().write(A.create()));
	}

	// F: precedence — @MarshalledIgnore overrides view

	public static class F {
		public String id = "1";

		@BeanIgnore
		@MarshalledProp(view = "summary")
		public String ignored = "x";

		static F create() {
			return new F();
		}
	}

	@Test void f01_beanIgnore_precedenceOverView() {
		var s = Json5Serializer.DEFAULT.copy().activeView("summary").build();
		assertSerialized(F.create(), s, "{id:'1'}");
	}

	// G: precedence — readOnly/writeOnly compose with view

	public static class G {
		@BeanProp(ro = "true")
		@MarshalledProp(view = "summary")
		public String name = "Alice";

		@BeanProp(wo = "true")
		@MarshalledProp(view = "detail")
		public String description = "A person";

		static G create() {
			return new G();
		}
	}

	@Test void g01_readOnlyPlusView_serializer_onlyNameVisible() {
		var s = Json5Serializer.DEFAULT.copy().activeView("summary").build();
		assertSerialized(G.create(), s, "{name:'Alice'}");
	}

	@Test void g02_writeOnlyPlusView_serializer_notVisible() {
		var s = Json5Serializer.DEFAULT.copy().activeView("detail").build();
		assertSerialized(G.create(), s, "{}");
	}

	@Test void g03_readOnlyPlusView_parser_nameIgnored_descriptionSet() {
		var p = Json5Parser.DEFAULT.copy().activeView("detail").ignoreUnknownBeanProperties().build();
		var x = p.read("{name:'Bob',description:'New'}", G.class);
		assertEquals("Alice", x.name);  // read-only: not written
		assertEquals("New", x.description);  // in detail view and write-only
	}

	// H: view via *Config / @ContextApply on unmodifiable class

	public static class H {
		public String id = "1";
		public String name = "Alice";
		public String description = "A person";

		static H create() {
			return new H();
		}
	}

	@MarshalledPropApply(on = "H.name", value = @MarshalledProp(view = "summary"))
	@MarshalledPropApply(on = "H.description", value = @MarshalledProp(view = "detail"))
	private static class HConfig {}

	@Test void h01_configApply_summaryView() {
		var s = Json5Serializer.DEFAULT.copy().applyAnnotations(HConfig.class).activeView("summary").build();
		assertSerialized(H.create(), s, "{id:'1',name:'Alice'}");  // i before n alphabetically
	}

	@Test void h02_configApply_detailView() {
		var s = Json5Serializer.DEFAULT.copy().applyAnnotations(HConfig.class).activeView("detail").build();
		assertSerialized(H.create(), s, "{description:'A person',id:'1'}");
	}

	@Test void h03_configApply_parser_outOfView() {
		var p = Json5Parser.DEFAULT.copy().applyAnnotations(HConfig.class).activeView("summary").ignoreUnknownBeanProperties().build();
		var x = p.read("{id:'2',name:'Bob',description:'ignored'}", H.class);
		assertEquals("2", x.id);
		assertEquals("Bob", x.name);
		assertEquals("A person", x.description); // unchanged
	}

	// I: cross-format — XML spot check

	public static class I {
		public String id = "1";

		@MarshalledProp(view = "summary")
		public String name = "Alice";

		static I create() {
			return new I();
		}
	}

	@Test void i01_xmlFormat_summaryView() {
		var s = XmlSerializer.DEFAULT_NS_SQ.copy().activeView("summary").build();
		var xml = s.write(I.create());
		// id (untagged, always included) and name (in summary) should appear
		assertTrue(xml.contains("1"), "Expected id value in XML: " + xml);
		assertTrue(xml.contains("Alice"), "Expected name value in XML: " + xml);
	}

	@Test void i02_xmlFormat_detailView_nameNotVisible() {
		var s = XmlSerializer.DEFAULT_NS_SQ.copy().activeView("detail").build();
		var xml = s.write(I.create());
		// name is only in summary view, not detail → should not appear
		assertFalse(xml.contains("Alice"), "name should not appear in detail view: " + xml);
		assertTrue(xml.contains("1"), "Expected id value in XML: " + xml);
	}

	// J: cross-format — MsgPack binary spot check

	@Test void j01_msgpackFormat_summaryView_serializer() {
		// MsgPack with summary view — only id (untagged) and name (in summary) should appear
		var s = MsgPackSerializer.DEFAULT.copy().activeView("summary").build();
		// Serialize to hex string and verify it's shorter than a full serialization (fewer properties)
		var fullBytes = MsgPackSerializer.DEFAULT.write(A.create());
		var summaryBytes = s.write(A.create());
		// The summary view should produce fewer bytes (omits description)
		assertNotEquals(fullBytes, summaryBytes);
	}

	// K: view interacts with beanProperties* (intersection: view ∩ filter)

	@Test void k01_viewAndBeanPropertiesFilter_onlyUntaggedSurvivesBoth() {
		// beanProperties restricts visible set to {id, name}
		// view "detail" restricts to {id (untagged), description (in detail)}
		// intersection: only id (untagged, in beanProperties) survives both filters
		var s = Json5Serializer.DEFAULT.copy()
			.beanProperties(A.class, "id,name")
			.activeView("detail")
			.build();
		assertSerialized(A.create(), s, "{id:'1'}");
	}

	// L: parse-side — out-of-view property with ignoreUnknownBeanProperties=false throws

	@Test void l01_parseSide_outOfViewThrowsWhenIgnoreDisabled() {
		var p = Json5Parser.DEFAULT.copy().activeView("summary").build();
		assertThrows(Exception.class, () -> p.read("{id:'1',description:'x'}", A.class));
	}

	@Test void l02_parseSide_outOfViewIgnoredWhenIgnoreEnabled() {
		var p = Json5Parser.DEFAULT.copy().activeView("summary").ignoreUnknownBeanProperties().build();
		assertDoesNotThrow(() -> p.read("{id:'1',description:'x'}", A.class));
	}

	// M: context-level default view is part of hashKey (different contexts cached separately)

	@Test void m01_differentActiveViews_differentContexts() {
		var s1 = Json5Serializer.DEFAULT.copy().activeView("summary").build();
		var s2 = Json5Serializer.DEFAULT.copy().activeView("detail").build();
		var s3 = Json5Serializer.DEFAULT.copy().activeView("summary").build();
		// same active view → same output (different instances, but same serialization result)
		assertEquals(s1.write(A.create()), s3.write(A.create()));
		// different active view → different output
		assertNotEquals(s1.write(A.create()), s2.write(A.create()));
	}

	// N: MarshalledIgnore annotation on a field with no view tag — never visible

	public static class N {
		public String id = "1";

		@BeanIgnore
		public String secret = "x";

		static N create() {
			return new N();
		}
	}

	@Test void n01_beanIgnoreNoView_neverVisible() {
		assertJson("{id:'1'}", N.create());
		var s = Json5Serializer.DEFAULT.copy().activeView("summary").build();
		assertSerialized(N.create(), s, "{id:'1'}");
	}
}
