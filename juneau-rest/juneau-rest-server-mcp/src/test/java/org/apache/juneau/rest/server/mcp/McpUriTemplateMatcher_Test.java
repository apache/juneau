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
package org.apache.juneau.rest.server.mcp;

import static org.apache.juneau.BasicTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;

import org.junit.jupiter.api.*;

/**
 * Coverage for the hand-rolled RFC 6570 reverse matcher {@link McpUriTemplateMatcher}.
 */
@SuppressWarnings({
	"java:S5976" // Each aNN/bNN/... test pins a distinct named RFC 6570 form/scenario as its own discoverable, individually-runnable test (per project SSLLC convention); collapsing similar-shaped groups into @ParameterizedTest would trade per-scenario failure clarity for a marginal LOC reduction.
})
class McpUriTemplateMatcher_Test {

	//-----------------------------------------------------------------------------------------------------------------
	// A: reversible forms and basic captures
	//-----------------------------------------------------------------------------------------------------------------

	@Nested class A_reversibleForms {

		@Test void a01_simpleScalar() {
			var a = McpUriTemplateMatcher.compile("file:///{name}");
			assertTrue(a.isReverseMatchable());
			assertEquals(Map.of("name", "report"), a.match("file:///report"));
			assertNull(a.match("file:///one/two"));
		}

		@Test void a02_reservedScalar() {
			var a = McpUriTemplateMatcher.compile("file:///{+name}");
			assertTrue(a.isReverseMatchable());
			assertEquals(Map.of("name", "one/two"), a.match("file:///one/two"));
		}

		@Test void a03_fragmentScalar() {
			var a = McpUriTemplateMatcher.compile("file:///doc{#name}");
			assertTrue(a.isReverseMatchable());
			assertEquals(Map.of("name", "sec/tion"), a.match("file:///doc#sec/tion"));
			assertNull(a.match("file:///docsec"), "fragment operator requires the leading '#'");
		}

		@Test void a04_pathSegmentsSingleAndMultiple() {
			var a = McpUriTemplateMatcher.compile("r{/x}");
			assertTrue(a.isReverseMatchable());
			assertEquals(Map.of("x", "foo"), a.match("r/foo"));

			var b = McpUriTemplateMatcher.compile("r{/x,y}");
			assertTrue(b.isReverseMatchable());
			assertEquals(Map.of("x", "foo", "y", "bar"), b.match("r/foo/bar"));
			assertEquals(List.of("x", "y"), List.copyOf(b.match("r/foo/bar").keySet()));
		}

		@Test void a05_labelSegmentsSingleAndMultiple() {
			var a = McpUriTemplateMatcher.compile("r{.x}");
			assertTrue(a.isReverseMatchable());
			assertEquals(Map.of("x", "val"), a.match("r.val"));

			var b = McpUriTemplateMatcher.compile("r{.x,y}");
			assertTrue(b.isReverseMatchable());
			assertEquals(Map.of("x", "foo", "y", "bar"), b.match("r.foo.bar"));
		}

		@Test void a06_queryStartSingleAndMultiple() {
			var a = McpUriTemplateMatcher.compile("r{?x}");
			assertTrue(a.isReverseMatchable());
			assertEquals(Map.of("x", "1"), a.match("r?x=1"));

			var b = McpUriTemplateMatcher.compile("r{?x,y}");
			assertTrue(b.isReverseMatchable());
			assertEquals(Map.of("x", "1", "y", "2"), b.match("r?x=1&y=2"));
		}

		@Test void a07_queryContinuationSingleAndMultiple() {
			var a = McpUriTemplateMatcher.compile("r?a=1{&x}");
			assertTrue(a.isReverseMatchable());
			assertEquals(Map.of("x", "1"), a.match("r?a=1&x=1"));

			var b = McpUriTemplateMatcher.compile("r?a=1{&x,y}");
			assertTrue(b.isReverseMatchable());
			assertEquals(Map.of("x", "1", "y", "2"), b.match("r?a=1&x=1&y=2"));
		}

		@Test void a08_queryValuesMayBeEmpty() {
			var a = McpUriTemplateMatcher.compile("r{?x}");
			assertEquals(Map.of("x", ""), a.match("r?x="));
		}

		@Test void a09_pureLiteralTemplateMatchesOnlyItself() {
			var a = McpUriTemplateMatcher.compile("file:///static/logo.png");
			assertTrue(a.isReverseMatchable());
			assertEquals(Map.of(), a.match("file:///static/logo.png"));
			assertNull(a.match("file:///static/other.png"));
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// B: pinned examples from the spec
	//-----------------------------------------------------------------------------------------------------------------

	@Nested class B_pinnedExamples {

		@Test void b01_twoSegmentsVersusSingleVersusReserved() {
			var ab = McpUriTemplateMatcher.compile("file:///{a}/{b}");
			assertEquals(Map.of("a", "one", "b", "two"), ab.match("file:///one/two"));

			var name = McpUriTemplateMatcher.compile("file:///{name}");
			assertNull(name.match("file:///one/two"));

			var plusName = McpUriTemplateMatcher.compile("file:///{+name}");
			assertEquals(Map.of("name", "one/two"), plusName.match("file:///one/two"));
		}

		@Test void b02_literalPrefixAndSuffixAroundCapture() {
			var a = McpUriTemplateMatcher.compile("file:///docs/{name}.txt");
			assertEquals(Map.of("name", "report"), a.match("file:///docs/report.txt"));
			assertNull(a.match("file:///docs/report.md"));
			assertNull(a.match("file:///other/report.txt"));
		}

		@Test void b03_longestReservedCaptureThatStillLetsSuffixMatch() {
			var a = McpUriTemplateMatcher.compile("{+path}.json");
			// The literal suffix ".json" also occurs inside the reserved capture's candidate text; the
			// longest capture that still lets ".json" match afterward wins.
			assertEquals(Map.of("path", "a.json"), a.match("a.json.json"));
		}

		@Test void b04_longestCaptureAmbiguityWithRepeatingLiteral() {
			var a = McpUriTemplateMatcher.compile("{+a}x");
			assertEquals(Map.of("a", "xx"), a.match("xxx"));
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// C: immutability, insertion order, and UTF-8 decoding of captures
	//-----------------------------------------------------------------------------------------------------------------

	@Nested class C_captureMapContract {

		@Test void c01_immutable() {
			var a = McpUriTemplateMatcher.compile("file:///{name}");
			var b = a.match("file:///report");
			assertThrows(UnsupportedOperationException.class, () -> b.put("x", "y"));
		}

		@Test void c02_insertionOrderMatchesDeclarationOrder() {
			var a = McpUriTemplateMatcher.compile("r{/first,second,third}");
			var b = a.match("r/1/2/3");
			assertEquals(List.of("first", "second", "third"), List.copyOf(b.keySet()));
		}

		@Test void c03_utf8DecodedCapture() {
			var a = McpUriTemplateMatcher.compile("file:///{name}");
			// %C3%A9 is the UTF-8 encoding of U+00E9 (e-acute).
			assertEquals(Map.of("name", "caf\u00e9"), a.match("file:///caf%C3%A9"));
		}

		@Test void c04_emptyMapWhenNoVariablesDeclared() {
			var a = McpUriTemplateMatcher.compile("file:///static/logo.png");
			assertEquals(Map.of(), a.match("file:///static/logo.png"));
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// D: specificity metadata exposure
	//-----------------------------------------------------------------------------------------------------------------

	@Nested class D_specificityMetadata {

		@Test void d01_literalOctetCountExcludesVariablesAndOperatorSyntax() {
			var a = McpUriTemplateMatcher.compile("file:///{name}");
			assertEquals(8, a.literalOctetCount());
			assertEquals(1, a.variableCount());
			assertEquals(List.of("name"), a.variableNames());
		}

		@Test void d02_normalizedPercentTripletCountsAsOneOctet() {
			var a = McpUriTemplateMatcher.compile("a%2Fb");
			assertEquals(3, a.literalOctetCount());
		}

		@Test void d03_operatorTypeIsNotAHiddenTieBreaker() {
			var simple = McpUriTemplateMatcher.compile("file:///{name}");
			var reserved = McpUriTemplateMatcher.compile("file:///{+name}");
			assertEquals(simple.literalOctetCount(), reserved.literalOctetCount());
			assertEquals(simple.variableCount(), reserved.variableCount());
		}

		@Test void d04_metadataExposedEvenWhenNotReverseMatchable() {
			var a = McpUriTemplateMatcher.compile("file:///{var:3}");
			assertFalse(a.isReverseMatchable());
			assertEquals(8, a.literalOctetCount());
			assertEquals(List.of("var"), a.variableNames());
		}

		@Test void d05_templateAccessor() {
			var a = McpUriTemplateMatcher.compile("file:///{name}");
			assertEquals("file:///{name}", a.template());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// E: fail-fast registration validation
	//-----------------------------------------------------------------------------------------------------------------

	@Nested class E_validation {

		@Test void e01_nullOrBlankTemplate() {
			assertThrowsWithMessage(IllegalArgumentException.class, "must not be null or blank",
				() -> McpUriTemplateMatcher.compile(null));
			assertThrowsWithMessage(IllegalArgumentException.class, "must not be null or blank",
				() -> McpUriTemplateMatcher.compile("   "));
		}

		@Test void e02_unterminatedBrace() {
			assertThrowsWithMessage(IllegalArgumentException.class,
				List.of("Malformed URI template", "file:///{name", "unterminated"),
				() -> McpUriTemplateMatcher.compile("file:///{name"));
		}

		@Test void e03_unmatchedClosingBrace() {
			assertThrowsWithMessage(IllegalArgumentException.class,
				List.of("Malformed URI template", "unmatched '}'"),
				() -> McpUriTemplateMatcher.compile("file:///name}"));
		}

		@Test void e04_emptyExpression() {
			assertThrowsWithMessage(IllegalArgumentException.class, "empty expression",
				() -> McpUriTemplateMatcher.compile("file:///{}"));
		}

		@Test void e05_emptyVariableListAfterOperator() {
			assertThrowsWithMessage(IllegalArgumentException.class, "empty variable list",
				() -> McpUriTemplateMatcher.compile("file:///{+}"));
		}

		@Test void e06_malformedPercentEscapeInLiteral() {
			assertThrowsWithMessage(IllegalArgumentException.class, "malformed percent-escape",
				() -> McpUriTemplateMatcher.compile("file:///100%"));
			assertThrowsWithMessage(IllegalArgumentException.class, "malformed percent-escape",
				() -> McpUriTemplateMatcher.compile("file:///100%zz"));
		}

		@Test void e07_invalidVariableNameGrammar() {
			assertThrowsWithMessage(IllegalArgumentException.class, "invalid variable name",
				() -> McpUriTemplateMatcher.compile("file:///{na@me}"));
		}

		@Test void e08_invalidPrefixLengthGrammar() {
			assertThrowsWithMessage(IllegalArgumentException.class, "invalid prefix-length modifier",
				() -> McpUriTemplateMatcher.compile("file:///{name:abc}"));
		}

		@Test void e09_unsupportedOperator() {
			assertThrowsWithMessage(IllegalArgumentException.class, "unsupported operator",
				() -> McpUriTemplateMatcher.compile("file:///{!name}"));
		}

		@Test void e10_invalidUtf8LiteralEscape() {
			// %80 alone is a lone UTF-8 continuation byte: not valid standalone UTF-8.
			assertThrowsWithMessage(IllegalArgumentException.class, "invalid UTF-8",
				() -> McpUriTemplateMatcher.compile("file:///a%80b"));
		}

		@Test void e11_duplicateVariableNameWithinTemplate() {
			assertThrowsWithMessage(IllegalArgumentException.class,
				List.of("duplicate variable", "''a''"),
				() -> McpUriTemplateMatcher.compile("file:///{a}/{a}"));
		}

		@Test void e12_duplicateVariableAcrossDifferentExpressionForms() {
			assertThrowsWithMessage(IllegalArgumentException.class, "duplicate variable",
				() -> McpUriTemplateMatcher.compile("r{/a}{?a}"));
		}

		@Test void e13_nestedBrace() {
			assertThrowsWithMessage(IllegalArgumentException.class, "nested '{'",
				() -> McpUriTemplateMatcher.compile("file:///{a{b}}"));
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// F: valid-but-not-reverse-matchable forms
	//-----------------------------------------------------------------------------------------------------------------

	@Nested class F_nonMatchableForms {

		@Test void f01_explode() {
			for (var t : List.of("file:///{var*}", "r{/path*}", "r{?tags*}")) {
				var a = McpUriTemplateMatcher.compile(t);
				assertFalse(a.isReverseMatchable(), t);
			}
		}

		@Test void f02_prefix() {
			var a = McpUriTemplateMatcher.compile("file:///{var:3}");
			assertFalse(a.isReverseMatchable());
			assertNull(a.match("abc"));
		}

		@Test void f03_matrix() {
			var a = McpUriTemplateMatcher.compile("r{;x}");
			assertFalse(a.isReverseMatchable());
			assertNull(a.match("r;x=1"));
		}

		@Test void f04_multiVariableSimpleReservedFragment() {
			assertFalse(McpUriTemplateMatcher.compile("file:///{x,y}").isReverseMatchable());
			assertFalse(McpUriTemplateMatcher.compile("file:///{+x,y}").isReverseMatchable());
			assertFalse(McpUriTemplateMatcher.compile("file:///doc{#x,y}").isReverseMatchable());
		}

		@Test void f05_adjacentExpressionsAreAmbiguous() {
			var a = McpUriTemplateMatcher.compile("file:///{a}{b}");
			assertFalse(a.isReverseMatchable());
			assertEquals(List.of("a", "b"), a.variableNames());
		}

		@Test void f06_variablesOfNonMatchableTemplateRemainDeclared() {
			var a = McpUriTemplateMatcher.compile("r{?tags*}");
			assertEquals(List.of("tags"), a.variableNames());
			assertNull(a.match("r?tags=a&tags=b"));
		}

		@Test void f07_queryFormsWithOmittedReorderedDuplicateOrExtraKeysDoNotMatch() {
			var a = McpUriTemplateMatcher.compile("r{?x,y}");
			assertTrue(a.isReverseMatchable(), "the query expression itself remains reverse-matchable");
			assertNull(a.match("r?x=1"), "omitted declared key");
			assertNull(a.match("r?y=2&x=1"), "reordered keys");
			assertNull(a.match("r?x=1&x=2&y=3"), "duplicate key");
			assertNull(a.match("r?x=1&y=2&z=3"), "extra key");
			assertNull(a.match("r?x&y=2"), "key-only parameter, missing '='");
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// G: percent-encoding normalization
	//-----------------------------------------------------------------------------------------------------------------

	@Nested class G_percentNormalization {

		@Test void g01_hexCaseNormalization() {
			var a = McpUriTemplateMatcher.compile("file:///seg%2fend");
			assertEquals(Map.of(), a.match("file:///seg%2Fend"), "lower and upper hex triplets are equivalent");
		}

		@Test void g02_encodedUnreservedEquivalence() {
			var a = McpUriTemplateMatcher.compile("file:///na%6de");
			assertEquals(Map.of(), a.match("file:///name"), "%6D decodes to unreserved 'm' for comparison");
		}

		@Test void g03_encodedSlashStaysSegmentDataAndDecodesToSlash() {
			var a = McpUriTemplateMatcher.compile("file:///{name}");
			assertEquals(Map.of("name", "a/b"), a.match("file:///a%2Fb"));
		}

		@Test void g04_rawSlashIsABoundary() {
			var a = McpUriTemplateMatcher.compile("file:///{name}");
			assertNull(a.match("file:///a/b"));
		}

		@Test void g05_literalPlusIsNeverSpace() {
			var a = McpUriTemplateMatcher.compile("file:///{name}");
			assertEquals(Map.of("name", "a+b"), a.match("file:///a+b"));
		}

		@Test void g06_malformedConcreteEscapeDoesNotMatch() {
			var a = McpUriTemplateMatcher.compile("file:///{name}");
			assertNull(a.match("file:///a%"));
			assertNull(a.match("file:///a%zzb"));
		}

		@Test void g07_invalidUtf8CaptureDoesNotMatch() {
			var a = McpUriTemplateMatcher.compile("file:///{name}");
			assertNull(a.match("file:///%80"));
		}

		@Test void g08_schemeAndHostCaseAreNotFolded() {
			var a = McpUriTemplateMatcher.compile("HTTP://Example.com/{id}");
			assertEquals(Map.of("id", "5"), a.match("HTTP://Example.com/5"));
			assertNull(a.match("http://example.com/5"), "scheme/host comparison is textual, not case-folded");
		}

		@Test void g09_labelCaptureStopsAtSlashAndDot() {
			var a = McpUriTemplateMatcher.compile("a{.x}suffix");
			assertEquals(Map.of("x", "val"), a.match("a.valsuffix"));
			assertNull(a.match("a.foo.barsuffix"), "a label capture cannot itself contain an unencoded '.'");
		}

		@Test void g10_nullUriReturnsNull() {
			var a = McpUriTemplateMatcher.compile("file:///{name}");
			assertNull(a.match(null));
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// H: adversarial inputs must not cause catastrophic/exponential blow-up
	//-----------------------------------------------------------------------------------------------------------------

	@Nested class H_adversarialPerformance {

		@Test void h01_longLiteralMatchIsFast() {
			var literal = "x".repeat(200_000);
			var a = McpUriTemplateMatcher.compile("file:///" + literal);
			assertTimeoutPreemptively(Duration.ofSeconds(2), () -> assertEquals(Map.of(), a.match("file:///" + literal)));
		}

		@Test void h02_longReservedCaptureIsFast() {
			var value = "a".repeat(200_000);
			var a = McpUriTemplateMatcher.compile("file:///{+name}");
			assertTimeoutPreemptively(Duration.ofSeconds(2),
				() -> assertEquals(Map.of("name", value), a.match("file:///" + value)));
		}

		@Test void h03_boundedCaptureWithDistantLiteralSuffixIsFast() {
			var value = "a".repeat(200_000);
			var a = McpUriTemplateMatcher.compile("{name}END");
			assertTimeoutPreemptively(Duration.ofSeconds(2), () -> assertEquals(Map.of("name", value), a.match(value + "END")));
		}

		@Test void h04_repeatingAdjacentLiteralPatternIsFast() {
			var a = McpUriTemplateMatcher.compile("{+a}AB");
			var uri = "AB".repeat(100_000);
			assertTimeoutPreemptively(Duration.ofSeconds(2), () -> assertNotNull(a.match(uri)));
		}

		@Test void h05_noMatchOnLongAdversarialInputIsFast() {
			var a = McpUriTemplateMatcher.compile("file:///{name}/fixed");
			var uri = "file:///" + "a".repeat(200_000);
			assertTimeoutPreemptively(Duration.ofSeconds(2), () -> assertNull(a.match(uri)));
		}
	}
}
