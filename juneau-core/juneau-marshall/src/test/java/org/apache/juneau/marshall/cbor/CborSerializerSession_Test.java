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
package org.apache.juneau.marshall.cbor;

import static org.apache.juneau.BasicTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.swap.spi.*;
import org.junit.jupiter.api.*;

/**
 * Coverage-focused tests for {@link CborSerializerSession} targeting branches not already exercised by
 * {@link CborRoundTrip_Test}, {@link CborEdgeCases_Test}, and {@link CborConformanceFixes_Test}:
 *  - {@code getCborOutputStream}'s reuse arm (output already a {@link CborOutputStream}).
 *  - {@code writeAnything}'s recursion-detected arm (an already-in-progress element re-encountered).
 *  - {@code writeAnything}'s swap-to-Object rewrap arm.
 *  - {@code writeAnything}'s direct-{@link BeanMap} dispatch (isBeanMap() as opposed to a plain Map).
 *  - {@code writeAnything}'s {@code isUri()}/{@code pMeta.isUri()} disjuncts.
 *  - {@code writeAnything}'s {@code Reader}/{@code InputStream} scalar arms.
 *  - {@code willRecurse}'s recursion-detected arm (mirrors the {@code writeAnything} arm above, for bean properties).
 */
@SuppressWarnings({
	"resource" // Reader/InputStream test fixtures wrap in-memory buffers with no OS resources to close.
})
class CborSerializerSession_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// a. getCborOutputStream reuse arm.
	//------------------------------------------------------------------------------------------------------------------

	public static class A01_Bean {
		public int x = 5;
	}

	@Test void a01_getCborOutputStream_alreadyCborOutputStream() throws Exception {
		var baos = new ByteArrayOutputStream();
		var cos = new CborOutputStream(baos);
		CborSerializer.DEFAULT.write(new A01_Bean(), cos);
		assertTrue(baos.size() > 0);
		var p = CborParser.DEFAULT.read(baos.toByteArray(), A01_Bean.class);
		assertEquals(5, p.x);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b. writeAnything: recursion-detected arm (push2 returns null for an already-in-progress ancestor).
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_selfReferencingListElement_appendsNull() throws Exception {
		// detectRecursions()+ignoreRecursions() is required to reach the *silent-null* recursion arm here --
		// without it, push() only truncates on exceeding maxDepth (a size guard, not a cycle detector; see
		// MarshallingTraverseContext.Builder#maxDepth), which produces deeply-nested output instead.
		var list = new ArrayList<Object>();
		list.add("x");
		list.add(list);
		var s = CborSerializer.create().detectRecursions().ignoreRecursions().build();
		var bytes = s.write(list);
		@SuppressWarnings("unchecked")
		var p = (List<Object>) CborParser.DEFAULT.read(bytes, List.class, Object.class);
		assertEquals("x", p.get(0));
		assertNull(p.get(1));
	}

	//------------------------------------------------------------------------------------------------------------------
	// c. writeAnything: swap-to-Object rewrap arm (mirrors ProtobufSerializerSession_Test's d02).
	//------------------------------------------------------------------------------------------------------------------

	public static class C01_ObjSwapTarget {
		public int x = 1;
	}

	public static class C01_ObjSwap extends ObjectSwap<C01_ObjSwapTarget,Object> {
		@Override public Object swap(MarshallingSession session, C01_ObjSwapTarget o) { return "swapped-" + o.x; }
	}

	public static class C01_Bean {
		public List<C01_ObjSwapTarget> inner = new ArrayList<>(List.of(new C01_ObjSwapTarget()));
	}

	@Test void c01_swapToObjectTypeRewrapsToRuntimeType() throws Exception {
		// Bean *property* values whose declared type has a registered swap are pre-swapped upstream, so the
		// swap must instead sit on a raw List *element* to reach writeAnything's own swap resolution
		// (mirrors the analogous finding for ProtobufSerializerSession). We only assert on the CBOR-encoded
		// string form here since C01_ObjSwap is a one-way swap (no unswap) -- round-tripping isn't the point.
		var s = CborSerializer.create().swaps(C01_ObjSwap.class).build();
		var bytes = s.write(new C01_Bean());
		var p = CborParser.DEFAULT.read(bytes, Map.class);
		var inner = (List<?>) p.get("inner");
		assertEquals("swapped-1", inner.get(0));
	}

	//------------------------------------------------------------------------------------------------------------------
	// d. writeAnything: direct BeanMap dispatch (isBeanMap(), as opposed to a plain Map).
	//------------------------------------------------------------------------------------------------------------------

	public static class D01_Bean {
		public String name = "x";
	}

	@Test void d01_beanMapRoot_writesAsMap() throws Exception {
		var session = CborSerializer.DEFAULT.createSession().build();
		var bm = session.toBeanMap(new D01_Bean());
		var bytes = CborSerializer.DEFAULT.write(bm);
		var p = CborParser.DEFAULT.read(bytes, D01_Bean.class);
		assertEquals("x", p.name);
	}

	//------------------------------------------------------------------------------------------------------------------
	// e. writeAnything: isUri()/pMeta.isUri() disjuncts.
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_rootUriType_hitsSTypeIsUriDisjunct() throws Exception {
		var bytes = CborSerializer.DEFAULT.write(URI.create("http://example.com/"));
		var p = CborParser.DEFAULT.read(bytes, String.class);
		assertEquals("http://example.com/", p);
	}

	public static class E02_Bean {
		@Uri
		public String link = "http://example.com/foo";
	}

	@Test void e02_uriAnnotatedStringProperty_hitsPMetaIsUriDisjunct() throws Exception {
		var bytes = CborSerializer.DEFAULT.write(new E02_Bean());
		var p = CborParser.DEFAULT.read(bytes, E02_Bean.class);
		assertEquals("http://example.com/foo", p.link);
	}

	//------------------------------------------------------------------------------------------------------------------
	// f. writeAnything: Reader/InputStream scalar arms.
	//------------------------------------------------------------------------------------------------------------------

	public static class F01_Bean {
		public Reader r;
	}

	@Test void f01_readerProperty_pipedAsBytes() throws Exception {
		var a = new F01_Bean();
		a.r = reader("hello");
		var bytes = CborSerializer.DEFAULT.write(a);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	public static class F02_Bean {
		public InputStream in;
	}

	@Test void f02_inputStreamProperty_pipedAsBytes() throws Exception {
		var a = new F02_Bean();
		a.in = inputStream("hello");
		var bytes = CborSerializer.DEFAULT.write(a);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
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
		assertThrowsWithMessage(SerializeException.class, "boom", () -> CborSerializer.DEFAULT.write(a));
	}

	public static class F04_Bean {
		public InputStream in;
	}

	@Test void f04_inputStreamThrowsDuringPipe_handledViaHandleThrown() {
		var a = new F04_Bean();
		a.in = new InputStream() {
			@Override public int read() throws IOException { throw new IOException("boom"); }
		};
		assertThrowsWithMessage(SerializeException.class, "boom", () -> CborSerializer.DEFAULT.write(a));
	}

	//------------------------------------------------------------------------------------------------------------------
	// g. willRecurse: recursion-detected arm for a self-referencing bean *property*.
	//------------------------------------------------------------------------------------------------------------------

	public static class G01_SelfRefBean {
		public String name = "root";
		public G01_SelfRefBean self;
	}

	@Test void g01_selfReferencingBeanProperty_omittedViaWillRecurse() throws Exception {
		// See b01's note: detectRecursions()+ignoreRecursions() is required for willRecurse's push2 call to
		// silently return null on recursion, rather than throwing or (with the settings off) truncating on maxDepth.
		var b = new G01_SelfRefBean();
		b.self = b;
		var s = CborSerializer.create().detectRecursions().ignoreRecursions().build();
		var bytes = s.write(b);
		var p = CborParser.DEFAULT.read(bytes, G01_SelfRefBean.class);
		assertEquals("root", p.name);
		assertNull(p.self);
	}

	//------------------------------------------------------------------------------------------------------------------
	// h. writeBeanMap: getter-exception handling. NOTE -- BeanPropertyMeta.getInner() gates on
	//    isIgnoreInvocationExceptionsOnGetters() *before* the value ever reaches writeBeanMap's own
	//    onBeanGetterException(pMeta, thrown) call: under the default (throw) setting it propagates a
	//    BeanRuntimeException that lands in forEachValue's `thrown` callback param and CborSerializerSession's
	//    onBeanGetterException call re-wraps+re-throws it (h01); under ignoreInvocationExceptionsOnGetters(), the
	//    exception is already swallowed upstream (getInner() returns null), so `thrown` is never populated at all
	//    here -- the property is instead omitted via the ordinary null-value checkNull filter (h02).
	//------------------------------------------------------------------------------------------------------------------

	public static class H01_Bean {
		public String getX() { throw new RuntimeException("boom"); }
	}

	@Test void h01_beanGetterExceptionDefaultThrows() {
		assertThrowsWithMessage(SerializeException.class, "boom", () -> CborSerializer.DEFAULT.write(new H01_Bean()));
	}

	@Test void h02_beanGetterExceptionIgnoredOmitsPropertyViaNullValueFilter() throws Exception {
		var s = CborSerializer.create().ignoreInvocationExceptionsOnGetters().build();
		var bytes = s.write(new H01_Bean());
		var p = CborParser.DEFAULT.read(bytes, Map.class);
		assertTrue(p.isEmpty());
	}
}
