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
package org.apache.juneau.marshall.hjson;

import static org.apache.juneau.BasicTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.swap.*;
import org.junit.jupiter.api.*;

/**
 * Coverage-focused tests for {@link HjsonSerializerSession} targeting branches not already exercised by
 * {@link HjsonSerializer_Test}, {@link HjsonRoundTrip_Test}, and {@link HjsonEdgeCases_Test}:
 *  - {@code getHjsonWriter}'s reuse arm (output already an {@link HjsonWriter}).
 *  - {@code doWrite}/{@code writeAnything}'s swap-to-Object rewrap arm.
 *  - {@code writeString}'s {@code useMultilineStrings} false-disjunct combos.
 *  - {@code writeAnything}'s recursion-detected arm.
 *  - {@code writeAnything}'s direct-{@link BeanMap} dispatch (isBeanMap() as opposed to a plain Map).
 *  - {@code writeAnything}'s {@code isUri()}/{@code pMeta.isUri()} disjuncts.
 *  - {@code writeAnything}'s {@code Reader}/{@code InputStream} scalar arms.
 */
@SuppressWarnings({
	"resource" // Reader/InputStream test fixtures wrap in-memory buffers with no OS resources to close.
})
class HjsonSerializerSession_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// a. getHjsonWriter reuse arm.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_getHjsonWriter_reusesExistingHjsonWriter() throws Exception {
		// getHjsonWriter's `output instanceof HjsonWriter` true branch -- writing directly to an
		// already-constructed HjsonWriter (passed as the raw output object) reuses it as-is.
		var sw = new StringWriter();
		var hw = new HjsonWriter(sw, false, -1, true, true, true, true, false, true, true, null);
		HjsonSerializer.DEFAULT.write(Map.of("a", "b"), hw);
		assertTrue(sw.toString().contains("a") && sw.toString().contains("b"), sw.toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// b. doWrite: root-level swap-to-Object rewrap arm (sType.isObject() true -> re-resolve).
	//------------------------------------------------------------------------------------------------------------------

	public static class B01_ObjSwapTarget {
		public int x = 1;
	}

	public static class B01_ObjSwap extends ObjectSwap<B01_ObjSwapTarget,Object> {
		@Override public Object swap(MarshallingSession session, B01_ObjSwapTarget o) { return "swapped-" + o.x; }
	}

	@Test void b01_rootSwapToObjectTypeRewrapsToRuntimeType() throws Exception {
		// The swap's declared target type is Object (isObject() true), forcing a re-resolve via
		// getClassMetaForObject(o) to the swap's actual runtime return type (String here).
		var s = HjsonSerializer.create().swaps(B01_ObjSwap.class).build();
		var hjson = s.write(new B01_ObjSwapTarget());
		assertTrue(hjson.contains("swapped-1"), hjson);
	}

	//------------------------------------------------------------------------------------------------------------------
	// c. writeAnything: swap-to-Object rewrap arm (mirrors b01, but for a nested list element -- bean
	// *property* values with a registered swap are pre-swapped upstream, so a raw list element is needed
	// to reach writeAnything's own swap resolution).
	//------------------------------------------------------------------------------------------------------------------

	public static class C01_Bean {
		public List<B01_ObjSwapTarget> inner = new ArrayList<>(List.of(new B01_ObjSwapTarget()));
	}

	@Test void c01_nestedSwapToObjectTypeRewrapsToRuntimeType() throws Exception {
		var s = HjsonSerializer.create().swaps(B01_ObjSwap.class).build();
		var hjson = s.write(new C01_Bean());
		assertTrue(hjson.contains("swapped-1"), hjson);
	}

	//------------------------------------------------------------------------------------------------------------------
	// d. writeString: useMultilineStrings disabled, with newline-containing content (the false-disjunct
	// combo of `ctx.useMultilineStrings && s.contains("\n")` -- HjsonSerializer_Test's a08 only covers the
	// true+true combo, and the rest of the suite only covers true+false with no-newline content).
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_multilineStringsDisabled_newlineContentQuotedInline() throws Exception {
		var s = HjsonSerializer.create().useMultilineStrings(false).build();
		var hjson = s.write(Map.of("desc", "line1\nline2"));
		assertFalse(hjson.contains("'''"), hjson);
		assertTrue(hjson.contains("line1"), hjson);
	}

	//------------------------------------------------------------------------------------------------------------------
	// e. writeAnything: recursion-detected arm (push2 returns null for an already-in-progress ancestor).
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_selfReferencingListElement_appendsNull() throws Exception {
		// detectRecursions()+ignoreRecursions() is required to reach the *silent-null* recursion arm here --
		// without it, push() only truncates on exceeding maxDepth (a size guard, not a cycle detector).
		var list = new ArrayList<Object>();
		list.add("x");
		list.add(list);
		var s = HjsonSerializer.create().detectRecursions().ignoreRecursions().build();
		var hjson = s.write(list);
		assertTrue(hjson.contains("x"), hjson);
		assertTrue(hjson.contains("null"), hjson);
	}

	public static class E02_SelfRefBean {
		public String name = "root";
		public E02_SelfRefBean self;
	}

	@Test void e02_selfReferencingBeanProperty_omittedViaWillRecurse() throws Exception {
		var b = new E02_SelfRefBean();
		b.self = b;
		var s = HjsonSerializer.create().detectRecursions().ignoreRecursions().build();
		var hjson = s.write(b);
		assertTrue(hjson.contains("root"), hjson);
	}

	//------------------------------------------------------------------------------------------------------------------
	// f. writeAnything: direct BeanMap dispatch (isBeanMap(), as opposed to a plain Map).
	//------------------------------------------------------------------------------------------------------------------

	public static class F01_Bean {
		public String name = "x";
	}

	@Test void f01_beanMapRoot_writesAsMap() throws Exception {
		// A root BeanMap is caught by doWrite's own `sType.isBean() || sType.isMap()` special-case (for
		// omitRootBraces support), which routes through writeMap directly -- it never reaches writeAnything's
		// own isBeanMap()-vs-plain-Map distinction below. See f02 for that arm.
		var session = HjsonSerializer.DEFAULT.createSession().build();
		var bm = session.toBeanMap(new F01_Bean());
		var hjson = HjsonSerializer.DEFAULT.write(bm);
		assertTrue(hjson.contains("name") && hjson.contains("x"), hjson);
	}

	public static class F02_Bean {
		public Object nested;
	}

	@Test void f02_nestedBeanMapProperty_hitsWriteAnythingIsBeanMapDisjunct() throws Exception {
		// Unlike f01, a *nested* BeanMap property value goes through writeAnything (not doWrite's root
		// special-case), so this reaches writeAnything's own `sType.isBeanMap()` check.
		var session = HjsonSerializer.DEFAULT.createSession().build();
		var outer = new F02_Bean();
		outer.nested = session.toBeanMap(new F01_Bean());
		var hjson = HjsonSerializer.DEFAULT.write(outer);
		assertTrue(hjson.contains("name") && hjson.contains("x"), hjson);
	}

	//------------------------------------------------------------------------------------------------------------------
	// g. writeAnything: isUri()/pMeta.isUri() disjuncts.
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_rootUriType_hitsSTypeIsUriDisjunct() throws Exception {
		var hjson = HjsonSerializer.DEFAULT.write(URI.create("http://example.com/"));
		assertTrue(hjson.contains("http://example.com/"), hjson);
	}

	public static class G02_Bean {
		@Uri
		public String link = "http://example.com/foo";
	}

	@Test void g02_uriAnnotatedStringProperty_hitsPMetaIsUriDisjunct() throws Exception {
		var hjson = HjsonSerializer.DEFAULT.write(new G02_Bean());
		assertTrue(hjson.contains("http://example.com/foo"), hjson);
	}

	//------------------------------------------------------------------------------------------------------------------
	// h. writeAnything: Reader/InputStream scalar arms.
	//------------------------------------------------------------------------------------------------------------------

	public static class H01_Bean {
		public Reader r;
	}

	@Test void h01_readerProperty_pipedAsText() throws Exception {
		var a = new H01_Bean();
		a.r = reader("hello");
		var hjson = HjsonSerializer.DEFAULT.write(a);
		assertTrue(hjson.contains("hello"), hjson);
	}

	public static class H02_Bean {
		public InputStream in;
	}

	@Test void h02_inputStreamProperty_pipedAsText() throws Exception {
		var a = new H02_Bean();
		a.in = inputStream("hello");
		var hjson = HjsonSerializer.DEFAULT.write(a);
		assertTrue(hjson.contains("hello"), hjson);
	}

	// javac emits a synthetic bridge lambda for the `SerializerSession::handleThrown` method reference (its
	// generic <T extends Throwable> signature needs an IOException-typed shim to satisfy Consumer<IOException>).
	// That bridge only executes when pipe() itself catches an I/O error, so a failing Reader/InputStream is
	// needed to reach it (a happy-path pipe, as in h01/h02 above, never touches it).
	public static class H03_Bean {
		public Reader r;
	}

	@Test void h03_readerThrowsDuringPipe_handledViaHandleThrown() {
		var a = new H03_Bean();
		a.r = new Reader() {
			@Override public int read(char[] cbuf, int off, int len) throws IOException { throw new IOException("boom"); }
			@Override public void close() { /* no-op */ }
		};
		assertThrowsWithMessage(SerializeException.class, "boom", () -> HjsonSerializer.DEFAULT.write(a));
	}

	public static class H04_Bean {
		public InputStream in;
	}

	@Test void h04_inputStreamThrowsDuringPipe_handledViaHandleThrown() {
		var a = new H04_Bean();
		a.in = new InputStream() {
			@Override public int read() throws IOException { throw new IOException("boom"); }
		};
		assertThrowsWithMessage(SerializeException.class, "boom", () -> HjsonSerializer.DEFAULT.write(a));
	}

	//------------------------------------------------------------------------------------------------------------------
	// i. writeBeanMap: getter-exception handling (onBeanGetterException(pMeta, thrown)). NOTE -- under the
	// default (throw) setting, BeanPropertyMeta.getInner() lets the BeanRuntimeException propagate into
	// forEachValue's `thrown` callback param, where onBeanGetterException re-wraps+re-throws it (i01); under
	// ignoreInvocationExceptionsOnGetters(), the exception is swallowed upstream (getInner() returns null),
	// so `thrown` is never populated -- the property is instead omitted via the ordinary null-value filter (i02).
	//------------------------------------------------------------------------------------------------------------------

	public static class I01_Bean {
		public String getX() { throw new RuntimeException("boom"); }
	}

	@Test void i01_beanGetterExceptionDefaultThrows() {
		assertThrowsWithMessage(SerializeException.class, "boom", () -> HjsonSerializer.DEFAULT.write(new I01_Bean()));
	}

	@Test void i02_beanGetterExceptionIgnoredOmitsPropertyViaNullValueFilter() throws Exception {
		var s = HjsonSerializer.create().ignoreInvocationExceptionsOnGetters().build();
		var hjson = s.write(new I01_Bean());
		assertFalse(hjson.contains("x"), hjson);
	}
}
