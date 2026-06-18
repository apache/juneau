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
package org.apache.juneau.rest.server;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates the permissive {@link Object}-typed return contract for the {@code getPaths()} virtual
 * getter on {@link RestServlet} / {@link RestResource}.
 *
 * <p>
 * Since 10.0.0 the {@code getPaths()} signature returns {@link Object}, accepting any of:
 * <ul>
 * 	<li>{@code null} &mdash; no override (falls through to {@code @Rest(paths=...)} annotation default).
 * 	<li>{@link String} &mdash; single path or comma-delimited list.
 * 	<li>{@code String[]} &mdash; each element may itself be a comma-delimited list.
 * 	<li>{@link Collection}, {@link List}, {@link Set}, {@link Iterable}, {@link java.util.stream.Stream Stream}.
 * 	<li>Nested mixes of the above (recursively flattened via
 * 		{@link org.apache.juneau.commons.utils.CollectionUtils#accumulate(Object) CollectionUtils.accumulate}).
 * 	<li>Primitive arrays (covered free by the flattening helper).
 * 	<li>Any other type &mdash; coerced via {@link String#valueOf(Object)} (loose, permissive contract).
 * </ul>
 *
 * <p>
 * Each flattened leaf flows through the same per-element pipeline used for {@code @Rest(paths=...)}
 * annotation elements: SVL substitution via the bean-store-backed
 * {@link org.apache.juneau.commons.svl.VarResolverSession VarResolverSession}, then comma-split on the
 * post-SVL value (each piece trimmed, empty pieces dropped).
 *
 * <p>
 * Backward-compat with the prior {@code String[]}-typed signature is covered by
 * {@link RestPathsRuntimeOverride_Getter_Test} &mdash; this test focuses on the new shapes the
 * permissive contract enables.
 *
 * @since 10.0.0
 */
class RestPathsRuntimeOverride_GetterShapes_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// a01 — getPaths() returns null → falls back to @Rest(paths) annotation default
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(paths={"/from-annotation"})
	public static class A01_ReturnsNull extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Override public Object getPaths() { return null; }
	}

	@Test
	void a01_returnsNull_fallsBackToAnnotation() throws Exception {
		var args = new RestContext.Args(A01_ReturnsNull.class, null, null, A01_ReturnsNull::new, "", null, null, null, RestContext.ContextKind.ROOT);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertArrayEquals(new String[]{"/from-annotation"}, ctx.getPaths(),
			"null return from getPaths() should fall through to the @Rest(paths) annotation default");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a02 — getPaths() returns a single String → one mount path
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(paths={"/from-annotation"})
	public static class A02_ReturnsString extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Override public Object getPaths() { return "/foo"; }
	}

	@Test
	void a02_returnsString_single() throws Exception {
		var args = new RestContext.Args(A02_ReturnsString.class, null, null, A02_ReturnsString::new, "", null, null, null, RestContext.ContextKind.ROOT);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertArrayEquals(new String[]{"/foo"}, ctx.getPaths(),
			"A single-String getter return should mount one path");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a03 — getPaths() returns a comma-delimited String → multiple mount paths (whitespace trimmed)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(paths={"/from-annotation"})
	public static class A03_ReturnsStringCdl extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Override public Object getPaths() { return "/foo, /bar , /baz"; }
	}

	@Test
	void a03_returnsString_cdl() throws Exception {
		var args = new RestContext.Args(A03_ReturnsStringCdl.class, null, null, A03_ReturnsStringCdl::new, "", null, null, null, RestContext.ContextKind.ROOT);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertArrayEquals(new String[]{"/foo", "/bar", "/baz"}, ctx.getPaths(),
			"A comma-delimited String getter return should comma-split with whitespace trimmed");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a04 — getPaths() returns String[] → backwards-compatible behavior
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(paths={"/from-annotation"})
	public static class A04_ReturnsStringArray extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Override public Object getPaths() { return new String[]{"/a", "/b"}; }
	}

	@Test
	void a04_returnsStringArray() throws Exception {
		var args = new RestContext.Args(A04_ReturnsStringArray.class, null, null, A04_ReturnsStringArray::new, "", null, null, null, RestContext.ContextKind.ROOT);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertArrayEquals(new String[]{"/a", "/b"}, ctx.getPaths(),
			"A String[] getter return should mount each element as a separate path");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a05 — getPaths() returns String[] with a CDL element → comma-split applies per element
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(paths={"/from-annotation"})
	public static class A05_ReturnsStringArrayWithCdl extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Override public Object getPaths() { return new String[]{"/a", "/b,/c"}; }
	}

	@Test
	void a05_returnsStringArray_withCdlElement() throws Exception {
		var args = new RestContext.Args(A05_ReturnsStringArrayWithCdl.class, null, null, A05_ReturnsStringArrayWithCdl::new, "", null, null, null, RestContext.ContextKind.ROOT);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertArrayEquals(new String[]{"/a", "/b", "/c"}, ctx.getPaths(),
			"A String[] element with embedded commas should comma-split into multiple mount paths");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a06 — getPaths() returns List<String> → flattened in iteration order
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(paths={"/from-annotation"})
	public static class A06_ReturnsList extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Override public Object getPaths() { return List.of("/x", "/y"); }
	}

	@Test
	void a06_returnsList() throws Exception {
		var args = new RestContext.Args(A06_ReturnsList.class, null, null, A06_ReturnsList::new, "", null, null, null, RestContext.ContextKind.ROOT);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertArrayEquals(new String[]{"/x", "/y"}, ctx.getPaths(),
			"A List<String> getter return should be flattened in iteration order");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a07 — getPaths() returns LinkedHashSet → mounts in insertion order
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(paths={"/from-annotation"})
	public static class A07_ReturnsSet extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Override public Object getPaths() {
			var s = new LinkedHashSet<String>();
			s.add("/x");
			s.add("/y");
			return s;
		}
	}

	@Test
	void a07_returnsSet() throws Exception {
		var args = new RestContext.Args(A07_ReturnsSet.class, null, null, A07_ReturnsSet::new, "", null, null, null, RestContext.ContextKind.ROOT);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertArrayEquals(new String[]{"/x", "/y"}, ctx.getPaths(),
			"A LinkedHashSet getter return should mount in insertion order");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a08 — getPaths() returns a nested mix: List of (String[], CDL String, List<String>)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(paths={"/from-annotation"})
	public static class A08_ReturnsNestedMix extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Override public Object getPaths() {
			return List.of(new String[]{"/a"}, "/b,/c", List.of("/d"));
		}
	}

	@Test
	void a08_returnsNestedListOfArrays() throws Exception {
		var args = new RestContext.Args(A08_ReturnsNestedMix.class, null, null, A08_ReturnsNestedMix::new, "", null, null, null, RestContext.ContextKind.ROOT);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertArrayEquals(new String[]{"/a", "/b", "/c", "/d"}, ctx.getPaths(),
			"Nested List/array/CDL mixes should recursively flatten and comma-split per leaf");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a09 — getPaths() returns String with SVL ($E{...,/default}) → SVL applied then comma-split
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(paths={"/from-annotation"})
	public static class A09_ReturnsStringWithSvl extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Override public Object getPaths() { return "$E{REST_PATHS_TEST_VAR,/fallback}"; }
	}

	@Test
	void a09_returnsStringWithSvl() throws Exception {
		// REST_PATHS_TEST_VAR is intentionally never set on the test JVM; the SVL fallback default fires.
		var args = new RestContext.Args(A09_ReturnsStringWithSvl.class, null, null, A09_ReturnsStringWithSvl::new, "", null, null, null, RestContext.ContextKind.ROOT);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertArrayEquals(new String[]{"/fallback"}, ctx.getPaths(),
			"A getter returning a $E{...,/default} String should SVL-resolve to the literal default when the env var is unset");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a10 — getPaths() returns List of one SVL String with an env-var default containing commas.
	//       The post-SVL value contains commas; comma-split happens on the SVL-resolved string, not on
	//       the raw element.  This matches the @Rest(paths={"$E{NAME,/x,/y}"}) annotation behavior
	//       documented in RestPathsRuntimeOverride_SVL_Test#e01.
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(paths={"/from-annotation"})
	public static class A10_ReturnsListWithSvlCdl extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Override public Object getPaths() { return List.of("$E{REST_PATHS_TEST_VAR2,/x,/y}"); }
	}

	@Test
	void a10_returnsListWithSvlAndCdl() throws Exception {
		// $E{NAME,a,b,c} concatenates everything after the first comma as the default — SVL returns
		// "/x,/y" here, then the post-SVL comma-split produces two mount paths.
		var args = new RestContext.Args(A10_ReturnsListWithSvlCdl.class, null, null, A10_ReturnsListWithSvlCdl::new, "", null, null, null, RestContext.ContextKind.ROOT);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertArrayEquals(new String[]{"/x", "/y"}, ctx.getPaths(),
			"SVL with embedded commas in the default should resolve first, then the post-SVL value comma-splits — comma-split must NOT fire on the raw $E{...} element");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a11 — getPaths() returns an empty List → zero mounts (matches current String[0] semantics)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(paths={"/from-annotation"})
	public static class A11_ReturnsEmptyList extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Override public Object getPaths() { return List.of(); }
	}

	@Test
	void a11_returnsEmptyList() throws Exception {
		var args = new RestContext.Args(A11_ReturnsEmptyList.class, null, null, A11_ReturnsEmptyList::new, "", null, null, null, RestContext.ContextKind.ROOT);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertEquals(0, ctx.getPaths().length,
			"An empty List getter return should produce zero mounts (same as new String[0])");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a12 — getPaths() returns "" → zero mounts (post-SVL is blank → dropped)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(paths={"/from-annotation"})
	public static class A12_ReturnsEmptyString extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Override public Object getPaths() { return ""; }
	}

	@Test
	void a12_returnsEmptyString() throws Exception {
		var args = new RestContext.Args(A12_ReturnsEmptyString.class, null, null, A12_ReturnsEmptyString::new, "", null, null, null, RestContext.ContextKind.ROOT);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertEquals(0, ctx.getPaths().length,
			"An empty-String getter return should resolve to zero mounts (post-SVL blank → dropped)");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a13 — getPaths() returns "   " → zero mounts (blank → dropped)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(paths={"/from-annotation"})
	public static class A13_ReturnsBlankString extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Override public Object getPaths() { return "   "; }
	}

	@Test
	void a13_returnsBlankString() throws Exception {
		var args = new RestContext.Args(A13_ReturnsBlankString.class, null, null, A13_ReturnsBlankString::new, "", null, null, null, RestContext.ContextKind.ROOT);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertEquals(0, ctx.getPaths().length,
			"A blank-String getter return should resolve to zero mounts (post-SVL blank → dropped)");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a14 — getPaths() returns an Integer → coerced via String.valueOf() (loose, permissive contract).
	//
	// Design decision: rather than throwing on non-String leaves, the contract is to use
	// String.valueOf(leaf).  Integer.valueOf(42) becomes the literal mount path "42" — caller's
	// responsibility to return paths that look like paths.  This matches the symmetry the
	// normalizePaths() adapter establishes for non-String leaves (Path, URI, etc.) and avoids an
	// instanceof ladder in the adapter.
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A14_ReturnsInteger extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Override public Object getPaths() { return Integer.valueOf(42); }
	}

	@Test
	void a14_returnsInteger_treatedAsString() throws Exception {
		var args = new RestContext.Args(A14_ReturnsInteger.class, null, null, A14_ReturnsInteger::new, "", null, null, null, RestContext.ContextKind.ROOT);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertArrayEquals(new String[]{"42"}, ctx.getPaths(),
			"A non-String leaf should be coerced via String.valueOf() — Integer 42 mounts on \"42\" (loose, permissive contract)");
	}
}
