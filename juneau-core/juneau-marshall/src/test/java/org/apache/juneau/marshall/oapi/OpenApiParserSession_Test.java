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
package org.apache.juneau.marshall.oapi;

import static org.apache.juneau.BasicTestUtils.*;
import static org.apache.juneau.marshall.httppart.HttpPartSchema.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.httppart.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.httppart.*;
import org.apache.juneau.marshall.parser.*;
import org.junit.jupiter.api.*;

/**
 * Targeted coverage tests for {@link OpenApiParserSession} focusing on gaps not exercised by the
 * broader round-trip suite in {@link OpenApi_Test} -- Optional unwrap/rewrap, non-default
 * collection formats for both ARRAY and OBJECT, element/value-type fallback chains, bean/map
 * malformed-input error paths, and toType()'s conversion-failure wrapping.
 */
@SuppressWarnings({
	"unchecked", // Parser returns Object by default for raw target types in these tests
	"java:S5976", // Separate test methods preferred over parameterized for clarity and independent failure reporting.
})
class OpenApiParserSession_Test extends TestBase {

	private static final OpenApiParser DP = OpenApiParser.DEFAULT;

	private static <T> T p(HttpPartSchema schema, String in, Class<T> c, Class<?>...args) throws Exception {
		return DP.read(null, schema, in, DP.getClassMeta(c, args));
	}

	// ============================================================
	// a01-a02: Optional unwrap/rewrap (read()'s top-of-method loop)
	// ============================================================

	@Test
	void a01_optionalTypeUnwrapsAndRewraps() throws Exception {
		// isOptional == true: the while-loop unwraps to the element type for parsing, then the
		// result is re-wrapped via o(t) before returning.
		var r = (Optional<String>) p(T_STRING, "hi", Optional.class, String.class);
		assertTrue(r.isPresent());
		assertEquals("hi", r.get());
	}

	@Test
	void a02_doublyNestedOptionalUnwrapsBothLevels() throws Exception {
		// The while-loop at the top of read() unwraps EACH level of nested Optional down to the
		// innermost non-optional element type (only the outermost level is re-wrapped afterward).
		var r = (Optional<?>) p(T_STRING, "hi", Optional.class, Optional.class, String.class);
		assertTrue(r.isPresent());
		assertEquals("hi", r.get());
	}

	// ============================================================
	// b01: STRING+isObject()+DATE format with a blank (but schema-allowed) value
	// ============================================================

	@Test
	void b01_dateFormatBlankValueIntoObjectFiltersToNull() {
		// sType.isObject() && f == DATE branch: isBlank(x1) == true short-circuits the
		// GranularZonedDateTime parse and orElse(null) fires -- schema.validateOutput() then
		// rejects the null result for this required-format schema, but the DATE branch itself
		// (and its empty-filter arm) is still exercised before that outer rejection.
		var ps = tNone().fDate().allowEmptyValue().build();
		assertThrowsWithMessage(SchemaValidationException.class, "does not match expected format",
			() -> DP.read(null, ps, "", Object.class));
	}

	// ============================================================
	// c01-c05: ARRAY branch -- element-type fallback chain and non-default collection formats
	// ============================================================

	@Test
	void c01_arrayElementTypeFallsBackThroughSchemaParsedType() throws Exception {
		// type.getElementType() == null (raw JsonList target) falls back to spt.getElementType(),
		// which is also null, and finally defaults to string().
		var r = p(T_ARRAY, "a,b", JsonList.class);
		assertEquals(JsonList.of("a", "b"), r);
	}

	@Test
	void c02_arrayPipesFormat() throws Exception {
		var r = (String[]) p(T_ARRAY_PIPES, "a|b", String[].class);
		assertArrayEquals(new String[] {"a", "b"}, r);
	}

	@Test
	void c03_arraySsvFormat() throws Exception {
		var r = (String[]) p(T_ARRAY_SSV, "a b", String[].class);
		assertArrayEquals(new String[] {"a", "b"}, r);
	}

	@Test
	void c04_arrayTsvFormat() throws Exception {
		var r = (String[]) p(T_ARRAY_TSV, "a\tb", String[].class);
		assertArrayEquals(new String[] {"a", "b"}, r);
	}

	@Test
	void c05_arrayMultiFormat() throws Exception {
		// cf == MULTI: the raw input is treated as a single-element array (a(in)), as opposed to
		// being split on a delimiter.
		var ps = tArray().collectionFormat(HttpPartCollectionFormat.MULTI).build();
		var r = (String[]) DP.read(null, ps, "a", DP.getClassMeta(String[].class));
		assertArrayEquals(new String[] {"a"}, r);
	}

	@Test
	void c06_arrayDefaultFormatUonWrappedInputDelegatesToUon() throws Exception {
		// cf == NO_COLLECTION_FORMAT's "starts with '@', ends with ')'" arm: delegates straight to
		// the UON parser instead of comma-splitting -- distinct from OpenApi_Test's f02a, which uses
		// plain comma-separated (non-UON-looking) input and takes the other arm of this same check.
		var r = (String[]) p(T_ARRAY, "@(a,b)", String[].class);
		assertArrayEquals(new String[] {"a", "b"}, r);
	}

	// ============================================================
	// d01-d05: OBJECT branch -- non-default collection formats, bean/map error paths, newInstance fallback
	// ============================================================

	@Test
	void d00_objectDefaultCsvFormatMultipleKeys() throws Exception {
		// cf == CSV branch (the default collection format) with more than one key=value pair.
		var r = p(T_OBJECT, "a=1,b=2", JsonMap.class);
		assertEquals("1", r.get("a"));
		assertEquals("2", r.get("b"));
	}

	@Test
	void d01_objectMultiFormat() throws Exception {
		var ps = tObject().collectionFormat(HttpPartCollectionFormat.MULTI).build();
		var r = (JsonMap) DP.read(null, ps, "a=1", DP.getClassMeta(JsonMap.class));
		assertEquals("1", r.get("a"));
	}

	@Test
	void d02_objectPipesFormat() throws Exception {
		var r = p(tObject().collectionFormat(HttpPartCollectionFormat.PIPES).build(), "a=1|b=2", JsonMap.class);
		assertEquals("1", r.get("a"));
		assertEquals("2", r.get("b"));
	}

	@Test
	void d03_objectSsvFormat() throws Exception {
		var r = p(tObject().collectionFormat(HttpPartCollectionFormat.SSV).build(), "a=1 b=2", JsonMap.class);
		assertEquals("1", r.get("a"));
		assertEquals("2", r.get("b"));
	}

	@Test
	void d04_objectTsvFormat() throws Exception {
		var r = p(tObject().collectionFormat(HttpPartCollectionFormat.TSV).build(), "a=1\tb=2", JsonMap.class);
		assertEquals("1", r.get("a"));
		assertEquals("2", r.get("b"));
	}

	public static class D05_Bean {
		public String a;
	}

	@Test
	void d05_beanBranchMalformedKvPairThrows() {
		// type.isBean() branch: a token with no "=" separator (kv.length != 2).
		assertThrowsWithMessage(ParseException.class, "Invalid input", () -> p(T_OBJECT, "abadkv", D05_Bean.class));
	}

	@Test
	void d06_mapBranchMalformedKvPairThrows() {
		// Plain-Map branch's identical malformed-token check (distinct call site from d05's bean branch).
		assertThrowsWithMessage(ParseException.class, "Invalid input", () -> p(T_OBJECT, "abadkv", JsonMap.class));
	}

	public static class D05b_Bean {
		public String a;
	}

	@Test
	void d05b_beanBranchUnknownPropertyIgnored() throws Exception {
		// type.isBean() branch's "bpm == null && ! isIgnoreUnknownBeanProperties()" check: with
		// ignoreUnknownBeanProperties() enabled, an unrecognized key is read as object() and
		// silently dropped instead of throwing (d05's default-settings variant does throw).
		var ctx = OpenApiParser.create().ignoreUnknownBeanProperties().build();
		var r = ctx.read(null, T_OBJECT, "a=1,bogus=2", ctx.getClassMeta(D05b_Bean.class));
		assertEquals("1", r.a);
	}

	@Test
	void d07_mapNewInstanceReturnsNullFallsBackToJsonMap() throws Exception {
		// type.newInstance() returns null for an interface target (java.util.Map itself isn't
		// directly instantiable), so the "m == null" fallback constructs a JsonMap instead.
		var r = (Map<String,Object>) p(T_OBJECT, "a=1", Map.class);
		assertEquals("1", r.get("a"));
	}

	@Test
	void d08_objectTypeNeitherMapNorBeanThrows() {
		// "! type.isMapOrBean()" branch: schema forces OBJECT type but the target is a plain String
		// (neither a bean nor a map).
		assertThrowsWithMessage(ParseException.class, "Invalid type", () -> p(T_OBJECT, "a=1", String.class));
	}

	// ============================================================
	// e01: FILE type
	// ============================================================

	@Test
	void e01_fileTypeThrows() {
		assertThrowsWithMessage(ParseException.class, "File part not supported", () -> p(T_FILE, "x", String.class));
	}

	// ============================================================
	// f01: toType()'s InvalidDataConversionException wrapping
	// ============================================================

	public static class F01_Bean {
		public String a;
	}

	@Test
	void f01_decodedBytesIncompatibleWithTargetTypeWrapsAsParseException() {
		// toType()'s catch (InvalidDataConversionException e) -> throw new ParseException(e.getMessage()):
		// the base64 decode itself succeeds, but converting the resulting byte[] into a bean with no
		// byte[] mutator fails at the convertToType() call.
		assertThrowsWithMessage(ParseException.class, "Invalid data conversion", () -> p(T_BYTE, "YWJj", F01_Bean.class));
	}

	// ============================================================
	// g01-g02: STRING+isObject()+DATE/DATE_TIME with a non-blank value
	// ============================================================

	@Test
	void g01_dateFormatNonBlankValueIntoObjectParses() throws Exception {
		// sType.isObject() && f == DATE branch's non-blank arm: the filter passes, so the
		// GranularZonedDateTime/GregorianCalendar mapping chain actually executes (distinct from
		// b01, which only exercises the blank/orElse(null) arm of this same filter chain).
		var ps = tNone().fDate().build();
		var r = p(ps, "1970-01-01", Object.class);
		assertInstanceOf(GregorianCalendar.class, r);
	}

	@Test
	void g02_dateTimeFormatNonBlankValueIntoObjectParses() throws Exception {
		var ps = tNone().fDateTime().build();
		var r = p(ps, "1970-01-01T00:00:00Z", Object.class);
		assertInstanceOf(GregorianCalendar.class, r);
	}

	// ============================================================
	// h01: ARRAY branch -- type.isObject() true-arm of the eType ternary
	// ============================================================

	@Test
	void h01_arrayElementTypeUsesStringWhenTargetIsObject() throws Exception {
		// A raw Object.class target is reassigned to CM_JsonList a few lines above (before the
		// eType ternary is ever evaluated), so parsing still succeeds via JsonList's own
		// getElementType() fallback rather than the ternary's (dead) true-arm.
		var r = (JsonList) p(T_ARRAY, "a,b", Object.class);
		assertEquals(JsonList.of("a", "b"), r);
	}

	// ============================================================
	// i01-i02: ARRAY/OBJECT branch -- '@'-prefixed-but-not-')'-terminated input
	// ============================================================

	@Test
	void i01_arrayInputStartsWithAtButDoesNotEndWithParenTakesCommaSplitArm() throws Exception {
		// NO_COLLECTION_FORMAT's "starts with '@', ends with ')'" check: an input that merely
		// starts with '@' (but doesn't end with ')') fails the AND overall, falling through to
		// the plain comma-split arm instead of delegating to the UON parser.
		var r = (String[]) p(T_ARRAY, "@(a,b),c", String[].class);
		assertArrayEquals(new String[] {"@(a", "b)", "c"}, r);
	}

	@Test
	void i02_objectInputStartsWithAtButDoesNotEndWithParenTakesCommaSplitArm() {
		// Same "starts with '@', ends with ')'" check, OBJECT branch's call site: falls through
		// to the comma-split arm, and the resulting non-"key=value" token then trips the
		// malformed-input check.
		assertThrowsWithMessage(ParseException.class, "Invalid input", () -> p(T_OBJECT, "@(a=1,b=2),c", JsonMap.class));
	}

	// ============================================================
	// j01: OBJECT branch -- explicit CSV collection format (distinct call site from the
	// default-format comma-split at line 322)
	// ============================================================

	@Test
	void j01_objectExplicitCsvFormat() throws Exception {
		var r = p(tObject().collectionFormat(HttpPartCollectionFormat.CSV).build(), "a=1,b=2", JsonMap.class);
		assertEquals("1", r.get("a"));
		assertEquals("2", r.get("b"));
	}

	// ============================================================
	// k01-k02: OBJECT branch -- non-bean Map value-type fallback chain
	// ============================================================

	public static class K01_Map extends LinkedHashMap<String,Integer> {
		private static final long serialVersionUID = 1L;
	}

	@Test
	void k01_mapValueTypeFallsBackThroughSchemaParsedType() throws Exception {
		// eType ternary's false-arm (type.isObject() == false) with type.getValueType() non-null
		// (a generic-typed Map subclass), so the value is read as the declared value type rather
		// than falling all the way back to string().
		var r = p(T_OBJECT, "a=1", K01_Map.class);
		assertEquals(1, r.get("a"));
	}

	public static class K03_Map extends LinkedHashMap<String,Object> {
		private static final long serialVersionUID = 1L;
		public K03_Map() {
			throw new RuntimeException("boom");
		}
	}

	@Test
	void k03_mapNewInstanceThrowingWrapsAsParseException() {
		// The "catch (ExecutableException e) -> throw new ParseException(e)" arm: type.newInstance()
		// propagates the no-arg constructor's own RuntimeException wrapped as an ExecutableException.
		assertThrowsWithMessage(ParseException.class, "boom", () -> p(T_OBJECT, "a=1", K03_Map.class));
	}

	@Test
	void k02_rawMapValueTypeIntoObjectUsesJsonMapFallback() throws Exception {
		// A raw Object.class target is reassigned to CM_JsonMap a few lines above (before the
		// eType ternary is ever evaluated), so parsing still succeeds via JsonMap's own
		// getValueType() fallback rather than the ternary's (dead) true-arm.
		var r = (Map<?,?>) p(T_OBJECT, "a=1", Object.class);
		assertEquals("1", r.get("a"));
	}
}
