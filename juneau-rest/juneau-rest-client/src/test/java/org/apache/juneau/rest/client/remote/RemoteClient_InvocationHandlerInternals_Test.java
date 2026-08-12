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
package org.apache.juneau.rest.client.remote;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.util.Date;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.httppart.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.httppart.*;
import org.junit.jupiter.api.*;

/**
 * Direct unit tests for {@code RemoteClient$RemoteInvocationHandler}'s private static pure-logic helpers.
 *
 * <p>
 * These helpers (URL-scheme parsing, part expansion, part-emptiness checks, retry-mode/status classification, etc.)
 * take no {@code RestRequest}/{@code RestResponse}/live-network dependency, so invoking them directly via reflection
 * is far more targeted than driving every branch through a full {@code @Remote}-proxy HTTP round-trip (covered
 * separately in {@link RemoteClient_Test} for the request/response-integrated behavior).
 */
class RemoteClient_InvocationHandlerInternals_Test extends TestBase {

	private static final Class<?> HANDLER;
	static {
		try {
			HANDLER = Class.forName("org.apache.juneau.rest.client.remote.RemoteClient$RemoteInvocationHandler");
		} catch (ClassNotFoundException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private static Object invokeStatic(String name, Class<?>[] paramTypes, Object... args) throws Exception {
		var m = HANDLER.getDeclaredMethod(name, paramTypes);
		m.setAccessible(true);
		try {
			return m.invoke(null, args);
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof RuntimeException re)
				throw re;
			if (e.getCause() instanceof Exception ex)
				throw ex;
			throw e;
		}
	}

	// ==========================================================================
	// a — schemeOf(String)
	// ==========================================================================

	@Test void a01_schemeOf_emptyString_isNull() throws Exception {
		assertNull(invokeStatic("schemeOf", new Class<?>[]{String.class}, ""));
	}

	@Test void a02_schemeOf_leadingNonLetter_isNull() throws Exception {
		assertNull(invokeStatic("schemeOf", new Class<?>[]{String.class}, "1abc/path"));
		assertNull(invokeStatic("schemeOf", new Class<?>[]{String.class}, "{var}/path"));
	}

	@Test void a03_schemeOf_colonBeforeSlash_returnsScheme() throws Exception {
		assertEquals("http", invokeStatic("schemeOf", new Class<?>[]{String.class}, "http://host/path"));
		assertEquals("https", invokeStatic("schemeOf", new Class<?>[]{String.class}, "https://host/path"));
	}

	@Test void a05_schemeOf_slashBeforeColon_isNull() throws Exception {
		// Must start with a letter (else the first-char check at line ~350 already returns null before the loop even
		// starts) so this test genuinely exercises the loop's '/' disjunct rather than the earlier guard.
		assertNull(invokeStatic("schemeOf", new Class<?>[]{String.class}, "ab/cd:ef"));
		assertNull(invokeStatic("schemeOf", new Class<?>[]{String.class}, "/no-scheme:here"));
	}

	@Test void a06_schemeOf_questionOrHashBeforeColon_isNull() throws Exception {
		assertNull(invokeStatic("schemeOf", new Class<?>[]{String.class}, "a?b:c"));
		assertNull(invokeStatic("schemeOf", new Class<?>[]{String.class}, "a#b:c"));
	}

	@Test void a07_schemeOf_invalidSchemeChar_isNull() throws Exception {
		// '_' is not letter/digit/+/-/. -- not a valid scheme character, so this is treated as scheme-less.
		assertNull(invokeStatic("schemeOf", new Class<?>[]{String.class}, "a_b:rest"));
	}

	@Test void a08_schemeOf_noColonAnywhere_isNull() throws Exception {
		assertNull(invokeStatic("schemeOf", new Class<?>[]{String.class}, "plain-relative-path"));
	}

	@Test void a09_schemeOf_validSchemeChars_plusDashDot() throws Exception {
		assertEquals("a+b-c.d", invokeStatic("schemeOf", new Class<?>[]{String.class}, "a+b-c.d://host"));
	}

	// ==========================================================================
	// b — requireHttpScheme(String)
	// ==========================================================================

	@Test void b01_requireHttpScheme_http_accepted() throws Exception {
		assertEquals("http://x", invokeStatic("requireHttpScheme", new Class<?>[]{String.class}, "http://x"));
	}

	@Test void b02_requireHttpScheme_https_accepted() throws Exception {
		assertEquals("HTTPS://x", invokeStatic("requireHttpScheme", new Class<?>[]{String.class}, "HTTPS://x"));
	}

	@Test void b03_requireHttpScheme_noScheme_passesThrough() throws Exception {
		assertEquals("/relative/path", invokeStatic("requireHttpScheme", new Class<?>[]{String.class}, "/relative/path"));
	}

	@Test void b04_requireHttpScheme_otherScheme_rejected() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> invokeStatic("requireHttpScheme", new Class<?>[]{String.class}, "ftp://evil/x"));
	}

	// ==========================================================================
	// c — combinePaths(String, String)
	// ==========================================================================

	@Test void c01_combinePaths_bothEmpty() throws Exception {
		assertEquals("", invokeStatic("combinePaths", new Class<?>[]{String.class, String.class}, "", ""));
	}

	@Test void c02_combinePaths_baseEmpty_methodNonEmpty() throws Exception {
		assertEquals("/m", invokeStatic("combinePaths", new Class<?>[]{String.class, String.class}, "", "/m"));
	}

	@Test void c03_combinePaths_methodEmpty_baseNonEmpty() throws Exception {
		assertEquals("/b", invokeStatic("combinePaths", new Class<?>[]{String.class, String.class}, "/b", ""));
	}

	@Test void c04_combinePaths_bothSlashed_avoidsDoubleSlash() throws Exception {
		assertEquals("/b/m", invokeStatic("combinePaths", new Class<?>[]{String.class, String.class}, "/b/", "/m"));
	}

	@Test void c05_combinePaths_neitherSlashed_insertsSlash() throws Exception {
		assertEquals("/b/m", invokeStatic("combinePaths", new Class<?>[]{String.class, String.class}, "/b", "m"));
	}

	@Test void c06_combinePaths_oneSlashed_concatenatesDirectly() throws Exception {
		assertEquals("/b/m", invokeStatic("combinePaths", new Class<?>[]{String.class, String.class}, "/b", "/m"));
		assertEquals("/b/m", invokeStatic("combinePaths", new Class<?>[]{String.class, String.class}, "/b/", "m"));
	}

	// ==========================================================================
	// d — isEmptyArg(Object)
	// ==========================================================================

	@Test void d01_isEmptyArg_null_isTrue() throws Exception {
		assertEquals(true, invokeStatic("isEmptyArg", new Class<?>[]{Object.class}, new Object[]{null}));
	}

	@Test void d02_isEmptyArg_emptyCharSequence_isTrue() throws Exception {
		assertEquals(true, invokeStatic("isEmptyArg", new Class<?>[]{Object.class}, ""));
	}

	@Test void d03_isEmptyArg_nonEmptyCharSequence_isFalse() throws Exception {
		assertEquals(false, invokeStatic("isEmptyArg", new Class<?>[]{Object.class}, "x"));
	}

	@Test void d04_isEmptyArg_emptyMap_isTrue() throws Exception {
		assertEquals(true, invokeStatic("isEmptyArg", new Class<?>[]{Object.class}, Map.of()));
	}

	@Test void d05_isEmptyArg_nonEmptyMap_isFalse() throws Exception {
		assertEquals(false, invokeStatic("isEmptyArg", new Class<?>[]{Object.class}, Map.of("k", "v")));
	}

	@Test void d06_isEmptyArg_emptyCollection_isTrue() throws Exception {
		assertEquals(true, invokeStatic("isEmptyArg", new Class<?>[]{Object.class}, List.of()));
	}

	@Test void d07_isEmptyArg_nonEmptyCollection_isFalse() throws Exception {
		assertEquals(false, invokeStatic("isEmptyArg", new Class<?>[]{Object.class}, List.of("x")));
	}

	@Test void d08_isEmptyArg_emptyArray_isTrue() throws Exception {
		assertEquals(true, invokeStatic("isEmptyArg", new Class<?>[]{Object.class}, (Object) new String[0]));
	}

	@Test void d09_isEmptyArg_nonEmptyArray_isFalse() throws Exception {
		assertEquals(false, invokeStatic("isEmptyArg", new Class<?>[]{Object.class}, (Object) new String[]{"x"}));
	}

	@Test void d10_isEmptyArg_otherType_isFalse() throws Exception {
		assertEquals(false, invokeStatic("isEmptyArg", new Class<?>[]{Object.class}, 42));
	}

	// ==========================================================================
	// e — isExpandable(Object) / isBean(Object)
	// ==========================================================================

	@Test void e01_isExpandable_map_true() throws Exception {
		assertEquals(true, invokeStatic("isExpandable", new Class<?>[]{Object.class}, Map.of("a", "1")));
	}

	@Test void e02_isExpandable_partList_true() throws Exception {
		assertEquals(true, invokeStatic("isExpandable", new Class<?>[]{Object.class}, PartList.ofPairs("a", "1")));
	}

	@Test void e03_isExpandable_httpHeaderList_true() throws Exception {
		assertEquals(true, invokeStatic("isExpandable", new Class<?>[]{Object.class}, HttpHeaderList.ofPairs("a", "1")));
	}

	@Test void e04_isExpandable_bean_true() throws Exception {
		assertEquals(true, invokeStatic("isExpandable", new Class<?>[]{Object.class}, new Object() {
			@SuppressWarnings("unused") public String getName() { return "x"; }
		}));
	}

	@Test void e05_isExpandable_scalarString_false() throws Exception {
		assertEquals(false, invokeStatic("isExpandable", new Class<?>[]{Object.class}, "plain"));
	}

	@Test void e06_isBean_scalarTypes_allFalse() throws Exception {
		assertEquals(false, invokeStatic("isBean", new Class<?>[]{Object.class}, "s"));
		assertEquals(false, invokeStatic("isBean", new Class<?>[]{Object.class}, 1));
		assertEquals(false, invokeStatic("isBean", new Class<?>[]{Object.class}, true));
		assertEquals(false, invokeStatic("isBean", new Class<?>[]{Object.class}, 'c'));
		assertEquals(false, invokeStatic("isBean", new Class<?>[]{Object.class}, TimeUnit()));
		assertEquals(false, invokeStatic("isBean", new Class<?>[]{Object.class}, new Date()));
		assertEquals(false, invokeStatic("isBean", new Class<?>[]{Object.class}, List.of("x")));
		assertEquals(false, invokeStatic("isBean", new Class<?>[]{Object.class}, new int[]{1}));
	}

	private static Object TimeUnit() { return java.util.concurrent.TimeUnit.SECONDS; }

	@Test void e07_isBean_plainObject_true() throws Exception {
		assertEquals(true, invokeStatic("isBean", new Class<?>[]{Object.class}, new Object()));
	}

	// ==========================================================================
	// f — propertyName(String)
	// ==========================================================================

	@Test void f01_propertyName_getPrefix() throws Exception {
		assertEquals("foo", invokeStatic("propertyName", new Class<?>[]{String.class}, "getFoo"));
	}

	@Test void f02_propertyName_isPrefix() throws Exception {
		assertEquals("foo", invokeStatic("propertyName", new Class<?>[]{String.class}, "isFoo"));
	}

	@Test void f03_propertyName_neitherPrefix_returnsAsIs() throws Exception {
		assertEquals("foo", invokeStatic("propertyName", new Class<?>[]{String.class}, "foo"));
	}

	@Test void f04_propertyName_bareGetOrIs_returnsAsIs() throws Exception {
		// "getFoo" requires length() > 3 after "get"; a bare "get"/"is" has nothing to lowercase-and-append.
		assertEquals("get", invokeStatic("propertyName", new Class<?>[]{String.class}, "get"));
		assertEquals("is", invokeStatic("propertyName", new Class<?>[]{String.class}, "is"));
	}

	// ==========================================================================
	// g — isRetryableMode(RemoteReturn, Class<?>) / isRetryableStatus(int)
	// ==========================================================================

	@Test void g01_isRetryableMode_response_false() throws Exception {
		assertEquals(false, invokeStatic("isRetryableMode", new Class<?>[]{RemoteReturn.class, Class.class}, RemoteReturn.RESPONSE, String.class));
	}

	@Test void g02_isRetryableMode_streamingOrWrapperReturnTypes_false() throws Exception {
		for (var t : List.of(java.io.InputStream.class, java.io.Reader.class, Optional.class,
				java.util.concurrent.CompletableFuture.class, java.util.concurrent.Future.class))
			assertEquals(false, invokeStatic("isRetryableMode", new Class<?>[]{RemoteReturn.class, Class.class}, RemoteReturn.BODY, t), t.getName());
	}

	@Test void g03_isRetryableMode_ordinaryBodyType_true() throws Exception {
		assertEquals(true, invokeStatic("isRetryableMode", new Class<?>[]{RemoteReturn.class, Class.class}, RemoteReturn.BODY, String.class));
	}

	@Test void g04_isRetryableStatus_429and5xx_true() throws Exception {
		assertEquals(true, invokeStatic("isRetryableStatus", new Class<?>[]{int.class}, 429));
		assertEquals(true, invokeStatic("isRetryableStatus", new Class<?>[]{int.class}, 500));
		assertEquals(true, invokeStatic("isRetryableStatus", new Class<?>[]{int.class}, 503));
	}

	@Test void g05_isRetryableStatus_otherCodes_false() throws Exception {
		assertEquals(false, invokeStatic("isRetryableStatus", new Class<?>[]{int.class}, 200));
		assertEquals(false, invokeStatic("isRetryableStatus", new Class<?>[]{int.class}, 400));
		assertEquals(false, invokeStatic("isRetryableStatus", new Class<?>[]{int.class}, 404));
	}

	// ==========================================================================
	// h — httpStatusCode(Class<?>) / newHttpTypeInstance(Class<?>, String) / instantiateHttpType(Class<?>, String)
	// ==========================================================================

	@Test void h01_httpStatusCode_knownType() throws Exception {
		assertEquals(404, invokeStatic("httpStatusCode", new Class<?>[]{Class.class}, NotFound.class));
	}

	@Test void h02_httpStatusCode_typeWithoutField_negativeOne() throws Exception {
		assertEquals(-1, invokeStatic("httpStatusCode", new Class<?>[]{Class.class}, String.class));
	}

	@Test void h03_newHttpTypeInstance_stringCtor_used() throws Exception {
		var o = invokeStatic("newHttpTypeInstance", new Class<?>[]{Class.class, String.class}, Ok.class, "body-text");
		assertInstanceOf(Ok.class, o);
	}

	@Test void h04_newHttpTypeInstance_fallsBackToNoArgCtor() throws Exception {
		// NotFound's only (String,...) constructor is a varargs (String, Object...) overload, which does NOT match
		// getConstructor(String.class)'s exact single-Class lookup -- so this falls back to the no-arg constructor.
		var o = invokeStatic("newHttpTypeInstance", new Class<?>[]{Class.class, String.class}, NotFound.class, "ignored-body");
		assertInstanceOf(NotFound.class, o);
	}

	@Test void h05_instantiateHttpType_delegatesAndWrapsFailure() throws Exception {
		var o = invokeStatic("instantiateHttpType", new Class<?>[]{Class.class, String.class}, Ok.class, "hi");
		assertInstanceOf(Ok.class, o);
	}

	/** A type with neither a (String) nor a no-arg public constructor, to force instantiateHttpType's failure-wrapping path. */
	public static final class NoUsableCtor {
		private NoUsableCtor() {}
	}

	@Test void h06_instantiateHttpType_noUsableCtor_throwsWrapped() throws Exception {
		var m = HANDLER.getDeclaredMethod("instantiateHttpType", Class.class, String.class);
		m.setAccessible(true);
		var ex = assertThrows(InvocationTargetException.class, () -> m.invoke(null, NoUsableCtor.class, "x"));
		assertNotNull(ex.getCause());
	}

	// ==========================================================================
	// i — rawClass(Type) / innerType(Type)
	// ==========================================================================

	@SuppressWarnings("unused")
	private Optional<String> optionalStringField;
	@SuppressWarnings("unused")
	private List<String> rawListField;

	@Test void i01_rawClass_plainClass_returnsItself() throws Exception {
		assertEquals(String.class, invokeStatic("rawClass", new Class<?>[]{Type.class}, String.class));
	}

	@Test void i02_rawClass_parameterizedType_returnsRawType() throws Exception {
		var genericType = getClass().getDeclaredField("optionalStringField").getGenericType();
		assertEquals(Optional.class, invokeStatic("rawClass", new Class<?>[]{Type.class}, genericType));
	}

	@Test void i03_rawClass_otherTypeKind_defaultsToObject() throws Exception {
		var typeVar = List.class.getTypeParameters()[0]; // a TypeVariable<?>, neither Class nor ParameterizedType
		assertEquals(Object.class, invokeStatic("rawClass", new Class<?>[]{Type.class}, typeVar));
	}

	@Test void i04_innerType_parameterizedType_returnsFirstArg() throws Exception {
		var genericType = getClass().getDeclaredField("optionalStringField").getGenericType();
		assertEquals(String.class, invokeStatic("innerType", new Class<?>[]{Type.class}, genericType));
	}

	@Test void i05_innerType_nonParameterizedType_defaultsToObject() throws Exception {
		assertEquals(Object.class, invokeStatic("innerType", new Class<?>[]{Type.class}, String.class));
	}

	// ==========================================================================
	// j — defaultFileName(Object) / isScalarPart(Object)
	// ==========================================================================

	@Test void j01_defaultFileName_file_usesItsName() throws Exception {
		var f = new java.io.File("some-report.pdf");
		assertEquals("some-report.pdf", invokeStatic("defaultFileName", new Class<?>[]{Object.class}, f));
	}

	@Test void j02_defaultFileName_nonFile_null() throws Exception {
		assertNull(invokeStatic("defaultFileName", new Class<?>[]{Object.class}, "not-a-file"));
	}

	@Test void j03_isScalarPart_variousScalars_true() throws Exception {
		assertEquals(true, invokeStatic("isScalarPart", new Class<?>[]{Object.class}, "s"));
		assertEquals(true, invokeStatic("isScalarPart", new Class<?>[]{Object.class}, 1));
		assertEquals(true, invokeStatic("isScalarPart", new Class<?>[]{Object.class}, true));
		assertEquals(true, invokeStatic("isScalarPart", new Class<?>[]{Object.class}, 'c'));
		assertEquals(true, invokeStatic("isScalarPart", new Class<?>[]{Object.class}, TimeUnit()));
		assertEquals(true, invokeStatic("isScalarPart", new Class<?>[]{Object.class}, new Date()));
	}

	@Test void j04_isScalarPart_nonScalar_false() throws Exception {
		assertEquals(false, invokeStatic("isScalarPart", new Class<?>[]{Object.class}, List.of("x")));
		assertEquals(false, invokeStatic("isScalarPart", new Class<?>[]{Object.class}, new Object()));
	}

	// ==========================================================================
	// k — hasBodyParam(Method) / hasPartAnnotation(Parameter)
	// ==========================================================================

	interface BodyParamProbe {
		void soleUnannotated(String body);
		void withContentParam(@org.apache.juneau.http.Content String body);
		void withPathParam(@org.apache.juneau.http.Path("id") String id);
		void multiParamNoContent(@org.apache.juneau.http.Path("id") String id, @org.apache.juneau.http.Query("q") String q);
		void withPartParam(@org.apache.juneau.http.Part("p") String p);
	}

	@Test void k01_hasBodyParam_soleUnannotatedParam_true() throws Exception {
		var method = BodyParamProbe.class.getMethod("soleUnannotated", String.class);
		assertEquals(true, invokeStatic("hasBodyParam", new Class<?>[]{Method.class}, method));
	}

	@Test void k02_hasBodyParam_contentAnnotated_true() throws Exception {
		var method = BodyParamProbe.class.getMethod("withContentParam", String.class);
		assertEquals(true, invokeStatic("hasBodyParam", new Class<?>[]{Method.class}, method));
	}

	@Test void k03_hasBodyParam_solePartAnnotated_false() throws Exception {
		// A sole parameter WITH a recognized part annotation (e.g. @Path) is not treated as an implicit body.
		var method = BodyParamProbe.class.getMethod("withPathParam", String.class);
		assertEquals(false, invokeStatic("hasBodyParam", new Class<?>[]{Method.class}, method));
	}

	@Test void k04_hasBodyParam_multiParamNoContentAnnotation_false() throws Exception {
		var method = BodyParamProbe.class.getMethod("multiParamNoContent", String.class, String.class);
		assertEquals(false, invokeStatic("hasBodyParam", new Class<?>[]{Method.class}, method));
	}

	@Test void k05_hasPartAnnotation_partAnnotation_true() throws Exception {
		var method = BodyParamProbe.class.getMethod("withPartParam", String.class);
		assertEquals(true, invokeStatic("hasPartAnnotation", new Class<?>[]{Parameter.class}, method.getParameters()[0]));
	}

	@Test void k06_hasPartAnnotation_none_false() throws Exception {
		var method = BodyParamProbe.class.getMethod("soleUnannotated", String.class);
		assertEquals(false, invokeStatic("hasPartAnnotation", new Class<?>[]{Parameter.class}, method.getParameters()[0]));
	}

	// ==========================================================================
	// l — applyConstants(BiConsumer, List, List)
	// ==========================================================================

	@Test void l01_applyConstants_bothEmpty_noop() throws Exception {
		var seen = new ArrayList<String>();
		BiConsumer<String,String> adder = (k, v) -> seen.add(k + "=" + v);
		invokeStatic("applyConstants", new Class<?>[]{BiConsumer.class, List.class, List.class}, adder, List.of(), List.of());
		assertEquals(List.of(), seen);
	}

	@Test void l02_applyConstants_methodLevelOverridesInterfaceLevel_sameName() throws Exception {
		var seen = new LinkedHashMap<String,String>();
		BiConsumer<String,String> adder = seen::put;
		var interfaceLevel = List.of(Map.entry("a", "iface-a"), Map.entry("b", "iface-b"));
		var methodLevel = List.<Map.Entry<String,String>>of(Map.entry("a", "method-a"));
		invokeStatic("applyConstants", new Class<?>[]{BiConsumer.class, List.class, List.class}, adder, interfaceLevel, methodLevel);
		assertEquals("method-a", seen.get("a"));
		assertEquals("iface-b", seen.get("b"));
	}

	// ==========================================================================
	// m — bindParts(...) -- static pure part-binding logic
	// ==========================================================================

	private static void bindParts(String annValue, String annName, String def, Object arg, String fallbackName, List<String> seen) throws Exception {
		bindParts(annValue, annName, def, arg, fallbackName, null, null, seen);
	}

	private static void bindParts(String annValue, String annName, String def, Object arg, String fallbackName,
			HttpPartSchema schema, HttpPartSerializer serializer, List<String> seen) throws Exception {
		var m = HANDLER.getDeclaredMethod("bindParts", HttpPartType.class, HttpPartSchema.class, String.class, String.class,
			String.class, Object.class, String.class, HttpPartSerializer.class, BiConsumer.class);
		m.setAccessible(true);
		BiConsumer<String,String> adder = (k, v) -> seen.add(k + "=" + v);
		m.invoke(null, HttpPartType.QUERY, schema, annValue, annName, def, arg, fallbackName, serializer, adder);
	}

	/** A trivial {@link HttpPartSerializer} whose session returns a fixed (possibly <jk>null</jk>/empty) string, for exercising bindParts' post-serialization branches without depending on OpenAPI serialization internals. */
	private static HttpPartSerializer fixedResultSerializer(String result) {
		return () -> (partType, schema, value) -> result;
	}

	@Test void m01_bindParts_nullArg_withDef_usesDef() throws Exception {
		var seen = new ArrayList<String>();
		bindParts(null, null, "default-val", null, "q", seen);
		assertEquals(List.of("q=default-val"), seen);
	}

	@Test void m02_bindParts_nullArg_withDef_wildcardExplicit_usesFallbackName() throws Exception {
		var seen = new ArrayList<String>();
		bindParts("*", null, "default-val", null, "fallback", seen);
		assertEquals(List.of("fallback=default-val"), seen);
	}

	@Test void m03_bindParts_nullArg_noDef_emitsNothing() throws Exception {
		var seen = new ArrayList<String>();
		bindParts(null, null, null, null, "q", seen);
		assertEquals(List.of(), seen);
	}

	@Test void m04_bindParts_nullArg_blankDef_emitsNothing() throws Exception {
		var seen = new ArrayList<String>();
		bindParts(null, null, "", null, "q", seen);
		assertEquals(List.of(), seen);
	}

	@Test void m05_bindParts_wildcardExplicit_mapArg_expands() throws Exception {
		var seen = new ArrayList<String>();
		bindParts("*", null, null, new LinkedHashMap<>(Map.of("a", "1")), "q", seen);
		assertEquals(List.of("a=1"), seen);
	}

	@Test void m06_bindParts_noExplicitName_expandableArg_expands() throws Exception {
		var seen = new ArrayList<String>();
		bindParts(null, null, null, new LinkedHashMap<>(Map.of("a", "1")), "q", seen);
		assertEquals(List.of("a=1"), seen);
	}

	@Test void m07_bindParts_scalarArg_serializedAndEmitted() throws Exception {
		var seen = new ArrayList<String>();
		bindParts(null, "myname", null, "hello", "q", seen);
		assertEquals(List.of("myname=hello"), seen);
	}

	@Test void m07b_bindParts_noExplicitName_scalarArg_evaluatesIsExpandableThenSerializes() throws Exception {
		// Distinct from m07 (which short-circuits the "*"/isExpandable check on a non-null explicit name):
		// here explicit is null, so isExpandable(arg) is actually evaluated (and returns false for a scalar),
		// exercising the second operand's false outcome of the "*".equals(explicit) || (explicit==null && isExpandable(arg)) check.
		var seen = new ArrayList<String>();
		bindParts(null, null, null, "hello", "q", seen);
		assertEquals(List.of("q=hello"), seen);
	}

	@Test void m09_bindParts_explicitOverFallbackName() throws Exception {
		var seen = new ArrayList<String>();
		bindParts("explicit-name", null, null, "v", "fallback", seen);
		assertEquals(List.of("explicit-name=v"), seen);
	}

	@Test void m10_bindParts_skipIfEmptySchema_emptyArg_suppressed() throws Exception {
		var seen = new ArrayList<String>();
		var schema = HttpPartSchema.create().skipIfEmpty(true).build();
		bindParts(null, "q", null, "", "q", schema, null, seen);
		assertEquals(List.of(), seen);
	}

	@Test void m11_bindParts_skipIfEmptySchema_nonEmptyArg_notSuppressed() throws Exception {
		var seen = new ArrayList<String>();
		var schema = HttpPartSchema.create().skipIfEmpty(true).build();
		bindParts(null, "q", null, "x", "q", schema, null, seen);
		assertEquals(List.of("q=x"), seen);
	}

	@Test void m12_bindParts_nonSkipIfEmptySchema_emptyArg_stillSerialized() throws Exception {
		var seen = new ArrayList<String>();
		var schema = HttpPartSchema.create().skipIfEmpty(false).allowEmptyValue(true).build();
		bindParts(null, "q", null, "", "q", schema, null, seen);
		assertEquals(List.of("q="), seen);
	}

	@Test void m13_bindParts_serializerReturnsNull_emitsNothing() throws Exception {
		var seen = new ArrayList<String>();
		bindParts(null, "q", null, "x", "q", null, fixedResultSerializer(null), seen);
		assertEquals(List.of(), seen);
	}

	@Test void m14_bindParts_serializerReturnsEmpty_skipIfEmptySchema_suppressed() throws Exception {
		var seen = new ArrayList<String>();
		var schema = HttpPartSchema.create().skipIfEmpty(true).build();
		bindParts(null, "q", null, "x", "q", schema, fixedResultSerializer(""), seen);
		assertEquals(List.of(), seen);
	}

	@Test void m15_bindParts_serializerReturnsEmpty_noSkipIfEmptySchema_stillEmitted() throws Exception {
		var seen = new ArrayList<String>();
		bindParts(null, "q", null, "x", "q", null, fixedResultSerializer(""), seen);
		assertEquals(List.of("q="), seen);
	}

	// ==========================================================================
	// n — expandPairs(HttpPartType, Object, HttpPartSerializer, BiConsumer) -- static pure
	// ==========================================================================

	private static List<String> expandPairs(Object arg) throws Exception {
		var m = HANDLER.getDeclaredMethod("expandPairs", HttpPartType.class, Object.class, HttpPartSerializer.class, BiConsumer.class);
		m.setAccessible(true);
		var seen = new ArrayList<String>();
		BiConsumer<String,String> adder = (k, v) -> seen.add(k + "=" + v);
		m.invoke(null, HttpPartType.QUERY, arg, null, adder);
		return seen;
	}

	@Test void n01_expandPairs_map_skipsNullValues() throws Exception {
		var m = new LinkedHashMap<String,String>();
		m.put("a", "1");
		m.put("b", null);
		assertEquals(List.of("a=1"), expandPairs(m));
	}

	@Test void n01b_expandPairs_map_skipsNullKeys() throws Exception {
		var m = new HashMap<Object,Object>();
		m.put(null, "orphan-value");
		assertEquals(List.of(), expandPairs(m));
	}

	@Test void n02_expandPairs_partList_skipsNullValues() throws Exception {
		assertEquals(List.of("a=1"), expandPairs(PartList.of(HttpPartBean.of("a", "1"), HttpPartBean.of("b", (String) null))));
	}

	@Test void n03_expandPairs_httpHeaderList_skipsNullValues() throws Exception {
		assertEquals(List.of("a=1"), expandPairs(HttpHeaderList.of(HttpHeaderBean.of("a", "1"), HttpHeaderBean.of("b", (String) null))));
	}

	/** A public bean type, needed because {@code MarshallingContext#toBeanMap} rejects non-public classes. */
	public static final class NameBean {
		public String getName() { return "bob"; }
		public String getNickname() { return null; }
	}

	@Test void n04_expandPairs_bean_serializesNonNullProperties() throws Exception {
		var result = expandPairs(new NameBean());
		assertTrue(result.contains("name=bob"), "Expected name=bob in: " + result);
		assertFalse(result.stream().anyMatch(s -> s.startsWith("nickname=")), "null-valued bean property must be skipped: " + result);
	}

	// ==========================================================================
	// o — serializePart(HttpPartType, HttpPartSchema, Object, HttpPartSerializer) -- static pure
	// ==========================================================================

	@Test void o01_serializePart_defaultSerializer_scalar() throws Exception {
		var m = HANDLER.getDeclaredMethod("serializePart", HttpPartType.class, HttpPartSchema.class, Object.class, HttpPartSerializer.class);
		m.setAccessible(true);
		assertEquals("42", m.invoke(null, HttpPartType.QUERY, null, 42, null));
	}

	@Test void o02_serializePart_serializationFailure_wrapped() throws Exception {
		var m = HANDLER.getDeclaredMethod("serializePart", HttpPartType.class, HttpPartSchema.class, Object.class, HttpPartSerializer.class);
		m.setAccessible(true);
		// A non-null value with a serializer whose session.write(...) always throws reliably exercises the
		// catch-and-wrap with the value != null (cn(value)) message-formatting branch. (A cyclic self-referencing
		// array was tried first to force OpenApiSerializer to fail, but it instead overflows the stack -- a
		// StackOverflowError, which isn't an Exception and so bypasses this catch block entirely.)
		var ex = assertThrows(InvocationTargetException.class, () -> m.invoke(null, HttpPartType.QUERY, null, "abc", throwingSerializer()));
		assertNotNull(ex.getCause());
		assertTrue(ex.getCause().getMessage().contains("String"), ex.getCause().getMessage());
	}

	/** A serializer session whose write(...) always throws, to reach the catch's message-formatting branches. */
	private static HttpPartSerializer throwingSerializer() {
		return () -> (partType, schema, value) -> { throw new org.apache.juneau.marshall.serializer.SerializeException("boom"); };
	}

	@Test void o03_serializePart_serializationFailure_withNullValue_messageSaysNull() throws Exception {
		var m = HANDLER.getDeclaredMethod("serializePart", HttpPartType.class, HttpPartSchema.class, Object.class, HttpPartSerializer.class);
		m.setAccessible(true);
		var ex = assertThrows(InvocationTargetException.class, () -> m.invoke(null, HttpPartType.QUERY, null, null, throwingSerializer()));
		assertNotNull(ex.getCause());
		assertTrue(ex.getCause().getMessage().contains("null"), ex.getCause().getMessage());
	}
}
