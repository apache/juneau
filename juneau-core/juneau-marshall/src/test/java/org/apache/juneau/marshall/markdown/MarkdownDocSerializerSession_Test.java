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
package org.apache.juneau.marshall.markdown;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.swap.*;
import org.junit.jupiter.api.*;

/**
 * Coverage-focused tests for {@link MarkdownDocSerializerSession} targeting branches not already exercised
 * by {@link MarkdownDocSerializer_Test}:
 *  - {@code doWrite}'s null-root arm, root-level {@link ObjectSwap} dispatch, and root array (as opposed to
 *    {@link java.util.List}) rendering.
 *  - {@code writeBeanWithHeadings}'s per-property {@link ObjectSwap} dispatch (both at the simple/complex
 *    classification stage and at the sub-section-render stage), the all-simple-properties (no complex
 *    sub-sections) and all-complex-properties (no key/value table) arms, {@code showHeaders(false)}, the
 *    heading-depth cap (falls back to {@code writeBeanMap} past level 6), and an array-typed (as opposed to
 *    {@link List}) nested property.
 */
class MarkdownDocSerializerSession_Test {

	//------------------------------------------------------------------------------------------------------------------
	// a0x - doWrite: null root, root-level ObjectSwap, and root array (vs. List) rendering.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_nullRoot() {
		var s = MarkdownDocSerializer.create().title("Empty").build();
		var md = s.write(null);
		assertTrue(md.contains("*null*"), "Expected null placeholder: " + md);
	}

	public static class A02_Wrapped {
		public String val;
	}

	public static class A02_Swap extends ObjectSwap<A02_Wrapped,String> {
		@Override public String swap(MarshallingSession session, A02_Wrapped o) { return o == null ? null : o.val; }
	}

	@Test void a02_rootLevelObjectSwap() {
		var s = MarkdownDocSerializer.create().title("Doc").swaps(A02_Swap.class).build();
		var w = new A02_Wrapped();
		w.val = "hello";
		var md = s.write(w);
		assertTrue(md.contains("hello"), "Expected swapped value: " + md);
	}

	@Test void a03_rootArrayNotList() {
		var s = MarkdownDocSerializer.create().title("Data").build();
		var md = s.write(new String[]{"alpha", "beta"});
		assertTrue(md.contains("alpha") && md.contains("beta"), "Expected both array elements: " + md);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b0x - writeBeanWithHeadings: per-property ObjectSwap at the simple/complex classification stage.
	//------------------------------------------------------------------------------------------------------------------

	public static class B01_Wrapped {
		public String val;
	}

	public static class B01_Swap extends ObjectSwap<B01_Wrapped,String> {
		@Override public String swap(MarshallingSession session, B01_Wrapped o) { return o == null ? null : o.val; }
	}

	public static class B01_Bean {
		public String name;
		public B01_Wrapped wrapped;
	}

	@Test void b01_propertySwap_swappedToSimpleType() {
		// wrapped's swap target (String) is simple, so it lands in simpleProps despite the field itself
		// being a bean type -- exercises writeBeanWithHeadings' classification-stage swap != null arm.
		var s = MarkdownDocSerializer.create().title("Doc").swaps(B01_Swap.class).build();
		var b = new B01_Bean();
		b.name = "Alice";
		b.wrapped = new B01_Wrapped();
		b.wrapped.val = "swapped-value";
		var md = s.write(b);
		assertTrue(md.contains("swapped-value"), "Expected swapped value inline: " + md);
		assertFalse(md.contains("## wrapped"), "Should NOT get its own sub-heading: " + md);
	}

	//------------------------------------------------------------------------------------------------------------------
	// c0x - writeBeanWithHeadings: all-simple-properties (no complex sub-sections) and all-complex-properties
	// (no key/value table) arms; showHeaders(false).
	//------------------------------------------------------------------------------------------------------------------

	public static class C01_Bean {
		public String name;
		public int age;
	}

	@Test void c01_allSimpleProperties_noComplexSubsections() {
		var s = MarkdownDocSerializer.create().title("Doc").build();
		var b = new C01_Bean();
		b.name = "Alice";
		b.age = 30;
		var md = s.write(b);
		assertTrue(md.contains("| name | Alice |"), md);
		assertFalse(md.contains("##"), "No sub-headings expected when there are no complex properties: " + md);
	}

	public static class C02_Address {
		public String city;
	}

	public static class C02_Bean {
		public C02_Address address;
	}

	@Test void c02_allComplexProperties_noSimpleTable() {
		// The root bean has no simple properties of its own, so it produces no key/value table before
		// "## address" (line 204's false arm). The recursive call for "address" itself DOES have a simple
		// property ("city"), so its OWN table appears after the heading (line 204's true arm) -- both arms
		// are exercised in a single pass.
		var s = MarkdownDocSerializer.create().title("Doc").build();
		var b = new C02_Bean();
		b.address = new C02_Address();
		b.address.city = "Boston";
		var md = s.write(b);
		var headingPos = md.indexOf("## address");
		assertTrue(headingPos >= 0, md);
		assertFalse(md.substring(0, headingPos).contains("| Property | Value |"),
			"No root-level key/value table expected when the root bean has no simple properties: " + md);
		assertTrue(md.contains("Boston"), md);
	}

	@Test void c03_showHeadersFalse_omitsTableHeaderRow() {
		var s = MarkdownDocSerializer.create().title("Doc").showHeaders(false).build();
		var b = new C01_Bean();
		b.name = "Alice";
		b.age = 30;
		var md = s.write(b);
		assertFalse(md.contains("| Property | Value |"), "Table header row should be suppressed: " + md);
		assertTrue(md.contains("| name | Alice |"), md);
	}

	//------------------------------------------------------------------------------------------------------------------
	// d0x - writeBeanWithHeadings sub-section render: per-property ObjectSwap (again, at the render stage
	// where the property is already classified as complex), heading-depth cap (falls back to writeBeanMap
	// past level 6), and an array-typed (vs. List) nested property.
	//------------------------------------------------------------------------------------------------------------------

	public static class D01_Inner {
		public Map<String,Object> data;
	}

	public static class D01_InnerSwap extends ObjectSwap<D01_Inner,Map<String,Object>> {
		@Override public Map<String,Object> swap(MarshallingSession session, D01_Inner o) { return o == null ? null : o.data; }
	}

	public static class D01_Bean {
		public String name;
		public D01_Inner inner;
	}

	@Test void d01_complexPropertySwap_atRenderStage() {
		// inner's swap target (Map) is ALSO complex, so it stays in complexProps and gets rendered as its
		// own sub-section -- but via the swapped Map shape, not the original bean shape. Exercises the
		// (distinct) swap != null arm inside the complex-property render loop.
		var s = MarkdownDocSerializer.create().title("Doc").swaps(D01_InnerSwap.class).build();
		var b = new D01_Bean();
		b.name = "Alice";
		b.inner = new D01_Inner();
		b.inner.data = new LinkedHashMap<>();
		b.inner.data.put("k", "v");
		var md = s.write(b);
		assertTrue(md.contains("## inner"), md);
		assertTrue(md.contains("k") && md.contains("v"), md);
	}

	public static class D02_L6 {
		public String leaf = "leafValue";
	}
	public static class D02_L5 { public D02_L6 l6 = new D02_L6(); }
	public static class D02_L4 { public D02_L5 l5 = new D02_L5(); }
	public static class D02_L3 { public D02_L4 l4 = new D02_L4(); }
	public static class D02_L2 { public D02_L3 l3 = new D02_L3(); }
	public static class D02_L1 { public D02_L2 l2 = new D02_L2(); }

	@Test void d02_headingDepthCap_fallsBackToWriteBeanMap() {
		// Root call is level=1 (headingLevel default). Each nesting level bumps by 1: l2 is processed at
		// level=1 (recurses to level=2, since 2<6); l3 at level=2 (recurses to 3); l4 at level=3 (recurses to
		// 4); l5 at level=4 (recurses to 5); l6 is processed at level=5, where "level + 1 < 6" is "6 < 6" ==
		// false -- l6's bean is rendered via writeBeanMap instead of recursing into writeBeanWithHeadings
		// again (which would otherwise emit an invalid H7+ heading).
		var s = MarkdownDocSerializer.create().title("Doc").build();
		var md = s.write(new D02_L1());
		assertTrue(md.contains("leafValue"), "Expected leaf value even past the heading-depth cap: " + md);
	}

	public static class D03_Bean {
		public String name;
		public Integer[] scores;
	}

	@Test void d03_arrayTypedNestedProperty() {
		var s = MarkdownDocSerializer.create().title("Doc").build();
		var b = new D03_Bean();
		b.name = "Alice";
		b.scores = new Integer[]{90, 85, 77};
		var md = s.write(b);
		assertTrue(md.contains("## scores"), md);
		assertTrue(md.contains("90") && md.contains("85") && md.contains("77"), md);
	}

	//------------------------------------------------------------------------------------------------------------------
	// e0x - a Stream-typed property is neither a bean, map, nor collection/array, but IS streamable, so it
	// still lands in complexProps (classification-stage isStreamable() arm) and, at render time, falls all
	// the way through the isBean/isMap/isCollectionOrArray chain to the final writeAnything(...) fallback arm.
	//------------------------------------------------------------------------------------------------------------------

	public static class E01_Bean {
		public String name;
		public java.util.stream.Stream<String> tags;
	}

	// Double-swap chain: F02_Outer's swap targets F02_Leaf (a bean, so it's still "complex" and gets
	// classified into complexProps as the *swapped* F02_Leaf instance). F02_Leaf independently has its own
	// registered swap to a Map. writeBeanWithHeadings' render-stage swap check (line 229) re-resolves
	// getSwap() on that already-swapped complexProps value, so this is the only way to make IT see a
	// non-null swap (classify-stage already stores the post-swap value, so a single swap only ever hits the
	// swap!=null arm at the classification stage, never at render).
	public static class F02_Leaf {
		public String v;
	}

	public static class F02_LeafSwap extends ObjectSwap<F02_Leaf,Map<String,Object>> {
		@Override public Map<String,Object> swap(MarshallingSession session, F02_Leaf o) {
			var m = new LinkedHashMap<String,Object>();
			m.put("v", o.v);
			return m;
		}
	}

	public static class F02_Outer {
		public String tag;
	}

	public static class F02_OuterSwap extends ObjectSwap<F02_Outer,F02_Leaf> {
		@Override public F02_Leaf swap(MarshallingSession session, F02_Outer o) {
			var leaf = new F02_Leaf();
			leaf.v = o.tag;
			return leaf;
		}
	}

	public static class F02_Bean {
		public String name;
		public F02_Outer outer;
	}

	@Test void f02_doubleSwapChain_renderStageSwapReResolved() {
		var s = MarkdownDocSerializer.create().title("Doc").swaps(F02_OuterSwap.class, F02_LeafSwap.class).build();
		var b = new F02_Bean();
		b.name = "Alice";
		b.outer = new F02_Outer();
		b.outer.tag = "outer-tag";
		var md = s.write(b);
		assertTrue(md.contains("## outer"), md);
		assertTrue(md.contains("outer-tag"), "Expected the leaf's swapped Map value rendered: " + md);
	}

	@Test void e01_streamableProperty_writeAnythingFallback() {
		var s = MarkdownDocSerializer.create().title("Doc").build();
		var b = new E01_Bean();
		b.name = "Alice";
		b.tags = java.util.stream.Stream.of("x", "y");
		var md = s.write(b);
		assertTrue(md.contains("## tags"), md);
		assertTrue(md.contains("x") && md.contains("y"), md);
	}
}
