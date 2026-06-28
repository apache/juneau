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
import org.junit.jupiter.api.*;

import static org.apache.juneau.marshall.MarshalledPropApplyAnnotation.create;

/**
 * Integration-suite coverage tests for view-based property projection.
 *
 * <p>
 * These tests mirror the unit-level {@code ViewProjection_Test} (in juneau-core/juneau-marshall) and are
 * placed here so their execution is captured by the integration-tests JaCoCo exec file, making coverage
 * of {@link MarshalledProp#view()}, {@link MarshallingContext.Builder#activeView(String)},
 * {@link MarshallingContext.Builder#disableDefaultViewInclusion()},
 * {@link MarshallingSession#isPropertyInActiveView}, and
 * {@code MarshalledPropertyPostProcessor.addViews()} visible to the coverage reporter.
 */
class ViewProjection_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// A: baseline — no active view → all properties visible
	//------------------------------------------------------------------------------------------------------------------

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

	//------------------------------------------------------------------------------------------------------------------
	// B: summary / detail / unknown views — covers MarshallingContextable.activeView(),
	//    MarshallingContext.Builder.activeView(), MarshallingSession.isPropertyInActiveView() non-null path,
	//    and MarshalledPropertyPostProcessor.addViews() loop body.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_summaryView_includesTaggedAndUntagged() {
		// MarshallingContextable.Builder.activeView() at line 1684 and
		// MarshallingContext.Builder.activeView() at line 2405 are exercised here.
		// MarshallingSession.isPropertyInActiveView() non-null activeView path (lines 744-747)
		// is exercised for: untagged 'id' (views==null → ctx.isDefaultViewInclusion()),
		// 'name' tagged summary (views.contains → true), 'description' tagged detail (views.contains → false).
		var s = Json5Serializer.DEFAULT.copy().activeView("summary").build();
		assertSerialized(A.create(), s, "{id:'1',name:'Alice'}");
	}

	@Test void b02_detailView_includesTaggedAndUntagged() {
		var s = Json5Serializer.DEFAULT.copy().activeView("detail").build();
		assertSerialized(A.create(), s, "{description:'A person',id:'1'}");
	}

	@Test void b03_unknownView_onlyUntaggedProperties() {
		var s = Json5Serializer.DEFAULT.copy().activeView("other").build();
		assertSerialized(A.create(), s, "{id:'1'}");
	}

	@Test void b04_activeView_null_resetToAll() {
		// Explicitly passing null disables view filtering — same as no activeView.
		var s = Json5Serializer.DEFAULT.copy().activeView(null).build();
		assertSerialized(A.create(), s, "{description:'A person',id:'1',name:'Alice'}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// C: disableDefaultViewInclusion — covers MarshallingContextable.Builder.disableDefaultViewInclusion()
	//    at line 1705 and MarshallingContext.Builder.disableDefaultViewInclusion() / defaultViewInclusion=false path.
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_disableDefaultViewInclusion_summaryView() {
		// MarshallingContextable.Builder.disableDefaultViewInclusion() at line 1705 is exercised here.
		// isDefaultViewInclusion() returns false → untagged 'id' excluded.
		var s = Json5Serializer.DEFAULT.copy().activeView("summary").disableDefaultViewInclusion().build();
		assertSerialized(A.create(), s, "{name:'Alice'}");
	}

	@Test void c02_disableDefaultViewInclusion_detailView() {
		var s = Json5Serializer.DEFAULT.copy().activeView("detail").disableDefaultViewInclusion().build();
		assertSerialized(A.create(), s, "{description:'A person'}");
	}

	@Test void c03_disableDefaultViewInclusion_unknownView_nothingVisible() {
		var s = Json5Serializer.DEFAULT.copy().activeView("other").disableDefaultViewInclusion().build();
		assertSerialized(A.create(), s, "{}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// D: multi-view union membership — covers addViews() 'acc non-null' branch (accumulator is reused
	//    across multiple view names on the same property) and covers acc.add(v) with acc != null.
	//------------------------------------------------------------------------------------------------------------------

	public static class D {
		public String id = "1";

		@MarshalledProp(view = {"summary", "detail"})
		public String name = "Alice";

		@MarshalledProp(view = "detail")
		public String description = "A person";

		static D create() {
			return new D();
		}
	}

	@Test void d01_multiView_summaryViewIncludesName() {
		var s = Json5Serializer.DEFAULT.copy().activeView("summary").build();
		assertSerialized(D.create(), s, "{id:'1',name:'Alice'}");
	}

	@Test void d02_multiView_detailViewIncludesBoth() {
		// addViews() is called twice for 'name': once for "summary" (acc becomes non-null),
		// once for "detail" (acc != null branch taken — adds to existing set).
		var s = Json5Serializer.DEFAULT.copy().activeView("detail").build();
		assertSerialized(D.create(), s, "{description:'A person',id:'1',name:'Alice'}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// E: MarshalledPropAnnotation programmatic builder — covers MarshalledPropAnnotation.create(),
	//    Builder.view(), Object.view(), Builder.build(), and Object constructor.
	//------------------------------------------------------------------------------------------------------------------

	public static class E {
		public String id = "1";
		public String name = "Alice";
		public String description = "A person";

		static E create() {
			return new E();
		}
	}

	@Test void e01_programmaticAnnotation_summaryView() {
		// MarshalledPropAnnotation.create() at line 444, Builder.view() at line 285,
		// Object.view() at line 431, and Builder.build() / Object constructor are all exercised here
		// via MarshalledPropApplyAnnotation.Builder.value(MarshalledProp).
		var applyName = create("ViewProjection_Test$E.name").value(MarshalledPropAnnotation.create().view("summary").build()).build();
		var applyDesc = create("ViewProjection_Test$E.description").value(MarshalledPropAnnotation.create().view("detail").build()).build();
		var s = Json5Serializer.DEFAULT.copy()
			.annotations(applyName, applyDesc)
			.activeView("summary")
			.build();
		assertSerialized(E.create(), s, "{id:'1',name:'Alice'}");
	}

	@Test void e02_programmaticAnnotation_detailView() {
		var applyName = create("ViewProjection_Test$E.name").value(MarshalledPropAnnotation.create().view("summary").build()).build();
		var applyDesc = create("ViewProjection_Test$E.description").value(MarshalledPropAnnotation.create().view("detail").build()).build();
		var s = Json5Serializer.DEFAULT.copy()
			.annotations(applyName, applyDesc)
			.activeView("detail")
			.build();
		assertSerialized(E.create(), s, "{description:'A person',id:'1'}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// F: addViews() edge case — empty-string view name is silently ignored.
	//    Covers the v.isEmpty() → T branch inside the addViews() for-loop.
	//------------------------------------------------------------------------------------------------------------------

	public static class F {
		public String id = "1";

		@MarshalledProp(view = {"", "summary"})
		public String name = "Alice";

		static F create() {
			return new F();
		}
	}

	@Test void f01_emptyStringViewName_ignored() {
		// @MarshalledProp(view = {"", "summary"}) — the empty string is ignored by addViews(),
		// exercising the v.isEmpty() → true branch (skips the empty entry, adds "summary").
		var s = Json5Serializer.DEFAULT.copy().activeView("summary").build();
		assertSerialized(F.create(), s, "{id:'1',name:'Alice'}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// G: per-session override — context default overridden per-call via MarshallingSession.Builder.activeView().
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_perSessionOverride_overridesContextDefault() throws Exception {
		var s = Json5Serializer.DEFAULT.copy().activeView("summary").build();

		// Default from context: summary view.
		assertEquals("{id:'1',name:'Alice'}", s.serialize(A.create()));

		// Per-call override to detail view.
		assertEquals("{description:'A person',id:'1'}", s.createSession().activeView("detail").build().serialize(A.create()));

		// Per-call override to null → all properties visible.
		assertEquals("{description:'A person',id:'1',name:'Alice'}", s.createSession().activeView(null).build().serialize(A.create()));
	}

	//------------------------------------------------------------------------------------------------------------------
	// H: parser-side view — out-of-view properties treated as unknown during parsing.
	//------------------------------------------------------------------------------------------------------------------

	@Test void h01_parseSide_summaryView_outOfViewIgnored() {
		var p = Json5Parser.DEFAULT.copy().activeView("summary").ignoreUnknownBeanProperties().build();
		var x = p.parse("{id:'2',name:'Bob',description:'ignored'}", A.class);
		assertEquals("2", x.id);
		assertEquals("Bob", x.name);
		assertEquals("A person", x.description); // unchanged — out-of-view during parse
	}

	@Test void h02_parseSide_outOfViewThrowsWhenIgnoreDisabled() {
		var p = Json5Parser.DEFAULT.copy().activeView("summary").build();
		assertThrows(Exception.class, () -> p.parse("{id:'1',description:'x'}", A.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// I: hashKey isolation — different activeView settings produce different contexts (are cached separately).
	//------------------------------------------------------------------------------------------------------------------

	@Test void i01_differentActiveViews_differentContexts() {
		var s1 = Json5Serializer.DEFAULT.copy().activeView("summary").build();
		var s2 = Json5Serializer.DEFAULT.copy().activeView("detail").build();
		var s3 = Json5Serializer.DEFAULT.copy().activeView("summary").build();
		// Same active view → same serialization result.
		assertEquals(s1.serialize(A.create()), s3.serialize(A.create()));
		// Different active view → different serialization result.
		assertNotEquals(s1.serialize(A.create()), s2.serialize(A.create()));
	}

	//------------------------------------------------------------------------------------------------------------------
	// I2: MarshallingSession.getActiveView() — covers the getter body (line 738 in MarshallingSession).
	//------------------------------------------------------------------------------------------------------------------

	@Test void i02_getActiveView_returnsConfiguredValue() {
		var s = Json5Serializer.DEFAULT.copy().activeView("summary").build();
		var session = (MarshallingSession) s.createSession().build();
		assertEquals("summary", session.getActiveView());
	}

	@Test void i03_getActiveView_nullByDefault() {
		var s = Json5Serializer.DEFAULT.copy().build();
		var session = (MarshallingSession) s.createSession().build();
		assertNull(session.getActiveView());
	}

	//------------------------------------------------------------------------------------------------------------------
	// J: BeanIgnore precedence — @BeanIgnore overrides view membership.
	//------------------------------------------------------------------------------------------------------------------

	public static class J {
		public String id = "1";

		@BeanIgnore
		@MarshalledProp(view = "summary")
		public String ignored = "x";

		static J create() {
			return new J();
		}
	}

	@Test void j01_beanIgnore_precedenceOverView() {
		var s = Json5Serializer.DEFAULT.copy().activeView("summary").build();
		assertSerialized(J.create(), s, "{id:'1'}");
	}
}
