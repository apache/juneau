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
package org.apache.juneau.marshall.yaml;

import static org.apache.juneau.BasicTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.swap.spi.*;
import org.junit.jupiter.api.*;

/**
 * Coverage-focused tests for {@link YamlSerializerSession} targeting branches not already exercised by
 * {@link Yaml_Test} and friends:
 *  - {@code getYamlWriter}'s reuse arm (output already a {@link YamlWriter}).
 *  - {@code writeAnything}'s recursion-detected arm (an already-in-progress element re-encountered).
 *  - {@code writeAnything}'s swap-to-Object rewrap arm.
 *  - {@code writeAnything}'s direct-{@link BeanMap} dispatch (isBeanMap() as opposed to a plain Map).
 *  - {@code writeAnything}'s {@code isUri()}/{@code pMeta.isUri()} disjuncts.
 *  - {@code writeAnything}'s {@code Reader}/{@code InputStream} scalar arms (including the pipe-failure path).
 *  - {@code isComplexValue}'s {@code null} and swap-present arms.
 *  - {@code writeBeanMap}/{@code writeMap}'s complex-vs-simple value dispatch for both collection/array and
 *    plain-complex (bean/map) values.
 *  - {@code writeBeanMap}'s getter-exception handling.
 */
@SuppressWarnings({
	"resource" // Reader/InputStream test fixtures wrap in-memory buffers with no OS resources to close.
})
class YamlSerializerSession_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// a. getYamlWriter reuse arm.
	//------------------------------------------------------------------------------------------------------------------

	public static class A01_Bean {
		public int x = 5;
	}

	@Test void a01_getYamlWriter_alreadyYamlWriter() throws Exception {
		var sw = new StringWriter();
		var yw = new YamlWriter(sw, false, -1, false, null);
		YamlSerializer.DEFAULT.write(new A01_Bean(), yw);
		assertTrue(sw.toString().contains("x:"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// b. writeAnything: recursion-detected arm (push2 returns null for an already-in-progress ancestor).
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_selfReferencingListElement_writesNull() throws Exception {
		// detectRecursions()+ignoreRecursions() is required to reach the *silent-null* recursion arm here --
		// without it, push() only truncates on exceeding maxDepth (a size guard, not a cycle detector), which
		// would produce deeply-nested output instead.
		var list = new ArrayList<Object>();
		list.add("x");
		list.add(list);
		var s = YamlSerializer.create().detectRecursions().ignoreRecursions().build();
		var result = s.write(list);
		assertTrue(result.contains("- x"), "Expected '- x' in: " + result);
		assertTrue(result.contains("null"), "Expected 'null' in: " + result);
	}

	//------------------------------------------------------------------------------------------------------------------
	// c. writeAnything: swap-to-Object rewrap arm.
	//------------------------------------------------------------------------------------------------------------------

	public static class C01_ObjSwapTarget {
		public int x = 1;
	}

	public static class C01_ObjSwap extends ObjectSwap<C01_ObjSwapTarget,Object> {
		@Override public Object swap(MarshallingSession session, C01_ObjSwapTarget o) { return "swapped-" + o.x; }
	}

	@Test void c01_swapToObjectTypeRewrapsToRuntimeType() throws Exception {
		// Bean *property* values whose declared type has a registered swap are pre-swapped upstream, so the
		// swap must instead sit on a raw List *element* to reach writeAnything's own swap resolution
		// (mirrors the analogous finding for CborSerializerSession).
		var s = YamlSerializer.create().swaps(C01_ObjSwap.class).build();
		var result = s.write(List.of(new C01_ObjSwapTarget()));
		assertTrue(result.contains("swapped-1"), "Expected 'swapped-1' in: " + result);
	}

	//------------------------------------------------------------------------------------------------------------------
	// d. writeAnything: direct BeanMap dispatch (isBeanMap(), as opposed to a plain Map).
	//------------------------------------------------------------------------------------------------------------------

	public static class D01_Bean {
		public String name = "x";
	}

	@Test void d01_beanMapRoot_writesAsMap() throws Exception {
		var session = YamlSerializer.DEFAULT.createSession().build();
		var bm = session.toBeanMap(new D01_Bean());
		var result = YamlSerializer.DEFAULT.write(bm);
		assertTrue(result.contains("name:"), "Expected 'name:' in: " + result);
	}

	//------------------------------------------------------------------------------------------------------------------
	// e. writeAnything: isUri()/pMeta.isUri() disjuncts.
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_rootUriType_hitsSTypeIsUriDisjunct() throws Exception {
		var result = YamlSerializer.DEFAULT.write(URI.create("http://example.com/"));
		assertTrue(result.contains("http://example.com/"), "Expected URI in: " + result);
	}

	public static class E02_Bean {
		@Uri
		public String link = "http://example.com/foo";
	}

	@Test void e02_uriAnnotatedStringProperty_hitsPMetaIsUriDisjunct() throws Exception {
		var result = YamlSerializer.DEFAULT.write(new E02_Bean());
		assertTrue(result.contains("http://example.com/foo"), "Expected URI in: " + result);
	}

	public static class E03_Bean {
		public String plain = "not-a-uri";
	}

	@Test void e03_plainStringProperty_hitsNeitherUriDisjunct() throws Exception {
		var result = YamlSerializer.DEFAULT.write(new E03_Bean());
		assertTrue(result.contains("not-a-uri"), "Expected plain string in: " + result);
	}

	//------------------------------------------------------------------------------------------------------------------
	// f. writeAnything: Reader/InputStream scalar arms (happy path and pipe-failure path).
	//------------------------------------------------------------------------------------------------------------------

	public static class F01_Bean {
		public Reader r;
	}

	@Test void f01_readerProperty_pipedAsText() throws Exception {
		var a = new F01_Bean();
		a.r = reader("hello");
		var result = YamlSerializer.DEFAULT.write(a);
		assertTrue(result.contains("hello"), "Expected 'hello' in: " + result);
	}

	public static class F02_Bean {
		public InputStream in;
	}

	@Test void f02_inputStreamProperty_pipedAsText() throws Exception {
		var a = new F02_Bean();
		a.in = inputStream("hello");
		var result = YamlSerializer.DEFAULT.write(a);
		assertTrue(result.contains("hello"), "Expected 'hello' in: " + result);
	}

	// javac emits a synthetic bridge lambda for the `SerializerSession::handleThrown` method reference (its
	// generic <T extends Throwable> signature needs an IOException-typed shim to satisfy Consumer<IOException>).
	// That bridge only executes when pipe() itself catches an I/O error, so a failing Reader/InputStream is
	// needed to reach it (a happy-path pipe, as in f01/f02 above, never touches it).
	public static class F03_Bean {
		public Reader r;
	}

	@Test void f03_readerThrowsDuringPipe_handledViaHandleThrown() {
		var a = new F03_Bean();
		a.r = new Reader() {
			@Override public int read(char[] cbuf, int off, int len) throws IOException { throw new IOException("boom"); }
			@Override public void close() { /* no-op */ }
		};
		assertThrowsWithMessage(SerializeException.class, "boom", () -> YamlSerializer.DEFAULT.write(a));
	}

	public static class F04_Bean {
		public InputStream in;
	}

	@Test void f04_inputStreamThrowsDuringPipe_handledViaHandleThrown() {
		var a = new F04_Bean();
		a.in = new InputStream() {
			@Override public int read() throws IOException { throw new IOException("boom"); }
		};
		assertThrowsWithMessage(SerializeException.class, "boom", () -> YamlSerializer.DEFAULT.write(a));
	}

	//------------------------------------------------------------------------------------------------------------------
	// g. isComplexValue: null and swap-present arms.
	//------------------------------------------------------------------------------------------------------------------

	// A @BeanProp("*") Map field exposes per-key "dyna" properties with no static declared type
	// (BeanPropertyMeta.getBeanInfo() is null for them), which is the only way to reach
	// isComplexValue(null) -- writeBeanMap's non-dyna path always resolves a non-null actualType.
	public static class G01_Bean {
		@BeanProp("*")
		public Map<String,Object> extra = new LinkedHashMap<>(Map.of("dynaKey", "dynaValue"));
	}

	@Test void g01_dynaPropertyWithNoStaticType_hitsIsComplexValueNullArm() throws Exception {
		var result = YamlSerializer.DEFAULT.write(new G01_Bean());
		assertTrue(result.contains("dynaKey"), "Expected 'dynaKey' in: " + result);
	}

	// isComplexValue first checks the *unswapped* actualType's own isMapOrBean()/isCollectionOrArrayOrOptional()
	// (an earlier disjunct on a different line) before ever consulting the registered swap -- so the swap source
	// type must itself be neither bean- nor collection-shaped, or that first check short-circuits before the
	// swap-classification logic below is ever reached. A package-private field (no public field/getter) keeps
	// G02_SwapSource from being auto-detected as a bean.
	public static class G02_SwapSource {
		int x = 1;
	}

	public static class G02_MapSwap extends ObjectSwap<G02_SwapSource,Map<String,Object>> {
		@Override public Map<String,Object> swap(MarshallingSession session, G02_SwapSource o) { return Map.of("x", o.x); }
	}

	public static class G02_Bean {
		public G02_SwapSource inner = new G02_SwapSource();
	}

	@Test void g02_beanPropertySwapToMap_hitsIsComplexValueSwapPresentArm() throws Exception {
		var s = YamlSerializer.create().swaps(G02_MapSwap.class).build();
		var result = s.write(new G02_Bean());
		assertTrue(result.contains("inner"), "Expected 'inner' in: " + result);
	}

	public static class G03_ScalarSwap extends ObjectSwap<G02_SwapSource,String> {
		@Override public String swap(MarshallingSession session, G02_SwapSource o) { return "scalar-" + o.x; }
	}

	public static class G03_Bean {
		public G02_SwapSource inner = new G02_SwapSource();
	}

	@Test void g03_beanPropertySwapToScalar_hitsIsComplexValueSwapPresentFalseArm() throws Exception {
		var s = YamlSerializer.create().swaps(G03_ScalarSwap.class).build();
		var result = s.write(new G03_Bean());
		assertTrue(result.contains("scalar-1"), "Expected 'scalar-1' in: " + result);
	}

	// g02 above short-circuits on isMapOrBean()==true; this covers the OR's other (isCollectionOrArrayOrOptional())
	// side by swapping to a List instead.
	public static class G04_ListSwap extends ObjectSwap<G02_SwapSource,List<Integer>> {
		@Override public List<Integer> swap(MarshallingSession session, G02_SwapSource o) { return List.of(o.x); }
	}

	public static class G04_Bean {
		public G02_SwapSource inner = new G02_SwapSource();
	}

	@Test void g04_beanPropertySwapToList_hitsIsComplexValueSwapPresentCollectionArm() throws Exception {
		var s = YamlSerializer.create().swaps(G04_ListSwap.class).build();
		var result = s.write(new G04_Bean());
		assertTrue(result.contains("- 1"), "Expected '- 1' in: " + result);
	}

	//------------------------------------------------------------------------------------------------------------------
	// h. writeBeanMap/writeMap: complex-collection vs complex-non-collection value dispatch.
	//------------------------------------------------------------------------------------------------------------------

	public static class H01_Bean {
		public List<String> items = List.of("a", "b");
		public D01_Bean nested = new D01_Bean();
	}

	@Test void h01_beanProperty_collectionValue_hitsCollectionBranch() throws Exception {
		var result = YamlSerializer.DEFAULT.write(new H01_Bean());
		assertTrue(result.contains("- a"), "Expected '- a' in: " + result);
	}

	@Test void h02_beanProperty_nestedBeanValue_hitsNonCollectionComplexBranch() throws Exception {
		var result = YamlSerializer.DEFAULT.write(new H01_Bean());
		assertTrue(result.contains("nested:"), "Expected 'nested:' in: " + result);
	}

	@Test void h03_mapValue_collectionValue_hitsCollectionBranch() throws Exception {
		var m = new LinkedHashMap<String,Object>();
		m.put("items", List.of("a", "b"));
		var result = YamlSerializer.DEFAULT.write(m);
		assertTrue(result.contains("- a"), "Expected '- a' in: " + result);
	}

	@Test void h04_mapValue_nestedMapValue_hitsNonCollectionComplexBranch() throws Exception {
		var inner = new LinkedHashMap<String,Object>();
		inner.put("k", "v");
		var m = new LinkedHashMap<String,Object>();
		m.put("nested", inner);
		var result = YamlSerializer.DEFAULT.write(m);
		assertTrue(result.contains("nested:"), "Expected 'nested:' in: " + result);
	}

	// writeMap's `(key == null ? null : toString(key))` ternary needs an actual null map key -- h03/h04's
	// string keys never exercise the null arm.
	@Test void h05_mapValue_nullKey_collectionValue_hitsCollectionBranchNullKeyArm() throws Exception {
		Map<String,Object> m = new LinkedHashMap<>();
		m.put(null, List.of("a", "b"));
		var result = YamlSerializer.DEFAULT.write(m);
		assertTrue(result.contains("- a"), "Expected '- a' in: " + result);
	}

	@Test void h06_mapValue_nullKey_nestedMapValue_hitsNonCollectionComplexBranchNullKeyArm() throws Exception {
		var inner = new LinkedHashMap<String,Object>();
		inner.put("k", "v");
		Map<String,Object> m = new LinkedHashMap<>();
		m.put(null, inner);
		var result = YamlSerializer.DEFAULT.write(m);
		assertTrue(result.contains("k:"), "Expected 'k:' in: " + result);
	}

	//------------------------------------------------------------------------------------------------------------------
	// i. writeBeanMap: getter-exception handling.
	//------------------------------------------------------------------------------------------------------------------

	public static class I01_Bean {
		public String getX() { throw new RuntimeException("boom"); }
	}

	@Test void i01_beanGetterExceptionDefaultThrows() {
		assertThrowsWithMessage(SerializeException.class, "boom", () -> YamlSerializer.DEFAULT.write(new I01_Bean()));
	}
}
