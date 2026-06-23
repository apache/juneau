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
package org.apache.juneau.rest.client;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;

import org.apache.juneau.rest.client.assertion.*;
import org.junit.jupiter.api.*;

/**
 * Branch-coverage tests for {@link ResponseAssertion}, {@link ResponseHeaderAssertion},
 * and {@link ResponseBodyAssertion}.
 *
 * <p>
 * All three classes are accessed through the package-private {@link RestResponse} constructor,
 * so this test lives in the {@code org.apache.juneau.rest.client} package.
 */
@SuppressWarnings({
	"resource",   // RestResponse is created for assertion testing; closed inline or not needed.
	"java:S5778", // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice.
})
class ResponseAssertion_Test {

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static RestResponse response(int statusCode) {
		return response(statusCode, null, "");
	}

	private static RestResponse response(int statusCode, String body) {
		return response(statusCode, null, body);
	}

	private static RestResponse response(int statusCode, String headerName, String body) {
		var b = TransportResponse.builder().statusCode(statusCode);
		if (headerName != null)
			b.header(headerName, "header-value");
		if (body != null && !body.isEmpty())
			b.body(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
		return new RestResponse(b.build(), RestClient.create());
	}

	private static RestResponse responseWithHeader(int statusCode, String name, String value) {
		var b = TransportResponse.builder()
			.statusCode(statusCode)
			.header(name, value);
		return new RestResponse(b.build(), RestClient.create());
	}

	// -----------------------------------------------------------------------
	// a — ResponseAssertion.statusCode()
	// -----------------------------------------------------------------------

	@Test void a01_statusCode_passes_when_matches() {
		var ra = new ResponseAssertion(response(200));
		assertDoesNotThrow(() -> ra.statusCode(200));
	}

	@Test void a02_statusCode_throws_when_mismatch() {
		var ra = new ResponseAssertion(response(404));
		assertThrows(AssertionError.class, () -> ra.statusCode(200));
	}

	// -----------------------------------------------------------------------
	// b — ResponseAssertion.isOk()
	// -----------------------------------------------------------------------

	@Test void b01_isOk_passes_for_200() {
		assertDoesNotThrow(() -> new ResponseAssertion(response(200)).isOk());
	}

	@Test void b02_isOk_passes_for_299() {
		assertDoesNotThrow(() -> new ResponseAssertion(response(299)).isOk());
	}

	@Test void b03_isOk_throws_for_199() {
		assertThrows(AssertionError.class, () -> new ResponseAssertion(response(199)).isOk());
	}

	@Test void b04_isOk_throws_for_300() {
		assertThrows(AssertionError.class, () -> new ResponseAssertion(response(300)).isOk());
	}

	// -----------------------------------------------------------------------
	// c — ResponseAssertion.isClientError()
	// -----------------------------------------------------------------------

	@Test void c01_isClientError_passes_for_400() {
		assertDoesNotThrow(() -> new ResponseAssertion(response(400)).isClientError());
	}

	@Test void c02_isClientError_passes_for_499() {
		assertDoesNotThrow(() -> new ResponseAssertion(response(499)).isClientError());
	}

	@Test void c03_isClientError_throws_for_399() {
		assertThrows(AssertionError.class, () -> new ResponseAssertion(response(399)).isClientError());
	}

	@Test void c04_isClientError_throws_for_500() {
		assertThrows(AssertionError.class, () -> new ResponseAssertion(response(500)).isClientError());
	}

	// -----------------------------------------------------------------------
	// d — ResponseAssertion.isServerError()
	// -----------------------------------------------------------------------

	@Test void d01_isServerError_passes_for_500() {
		assertDoesNotThrow(() -> new ResponseAssertion(response(500)).isServerError());
	}

	@Test void d02_isServerError_passes_for_599() {
		assertDoesNotThrow(() -> new ResponseAssertion(response(599)).isServerError());
	}

	@Test void d03_isServerError_throws_for_499() {
		assertThrows(AssertionError.class, () -> new ResponseAssertion(response(499)).isServerError());
	}

	@Test void d04_isServerError_throws_for_600() {
		assertThrows(AssertionError.class, () -> new ResponseAssertion(response(600)).isServerError());
	}

	// -----------------------------------------------------------------------
	// e — ResponseAssertion.header() / body() / and()
	// -----------------------------------------------------------------------

	@Test void e01_header_returnsNonNull() {
		var ra = new ResponseAssertion(response(200));
		assertNotNull(ra.header("X-Foo"));
	}

	@Test void e02_body_returnsNonNull() {
		var ra = new ResponseAssertion(response(200, "hello"));
		assertNotNull(ra.body());
	}

	@Test void e03_and_returnsResponse() {
		var resp = response(200);
		var ra = new ResponseAssertion(resp);
		assertNotNull(ra.and());
		assertEquals(200, ra.and().getStatusCode());
	}

	// -----------------------------------------------------------------------
	// f — ResponseHeaderAssertion.isPresent() / isAbsent()
	// -----------------------------------------------------------------------

	@Test void f01_isPresent_passes_when_header_present() {
		var resp = responseWithHeader(200, "X-Foo", "bar");
		assertDoesNotThrow(() -> new ResponseAssertion(resp).header("X-Foo").isPresent());
	}

	@Test void f02_isPresent_throws_when_header_absent() {
		var resp = response(200);
		assertThrows(AssertionError.class, () -> new ResponseAssertion(resp).header("X-Missing").isPresent());
	}

	@Test void f03_isAbsent_passes_when_header_absent() {
		var resp = response(200);
		assertDoesNotThrow(() -> new ResponseAssertion(resp).header("X-Missing").isAbsent());
	}

	@Test void f04_isAbsent_throws_when_header_present() {
		var resp = responseWithHeader(200, "X-Foo", "bar");
		assertThrows(AssertionError.class, () -> new ResponseAssertion(resp).header("X-Foo").isAbsent());
	}

	// -----------------------------------------------------------------------
	// g — ResponseHeaderAssertion.equals()
	// -----------------------------------------------------------------------

	@Test void g01_equals_passes_when_value_matches() {
		var resp = responseWithHeader(200, "X-Foo", "bar");
		assertDoesNotThrow(() -> new ResponseAssertion(resp).header("X-Foo").equals("bar"));
	}

	@Test void g02_equals_throws_when_value_mismatch() {
		var resp = responseWithHeader(200, "X-Foo", "bar");
		assertThrows(AssertionError.class, () -> new ResponseAssertion(resp).header("X-Foo").equals("other"));
	}

	@Test void g03_equals_throws_when_header_absent() {
		var resp = response(200);
		assertThrows(AssertionError.class, () -> new ResponseAssertion(resp).header("X-Missing").equals("bar"));
	}

	// -----------------------------------------------------------------------
	// h — ResponseHeaderAssertion.contains()
	// -----------------------------------------------------------------------

	@Test void h01_contains_passes_when_substring_present() {
		var resp = responseWithHeader(200, "X-Foo", "hello-world");
		assertDoesNotThrow(() -> new ResponseAssertion(resp).header("X-Foo").contains("world"));
	}

	@Test void h02_contains_throws_when_substring_absent() {
		var resp = responseWithHeader(200, "X-Foo", "hello-world");
		assertThrows(AssertionError.class, () -> new ResponseAssertion(resp).header("X-Foo").contains("missing"));
	}

	@Test void h03_contains_throws_when_header_absent() {
		var resp = response(200);
		assertThrows(AssertionError.class, () -> new ResponseAssertion(resp).header("X-Missing").contains("any"));
	}

	// -----------------------------------------------------------------------
	// i — ResponseHeaderAssertion.startsWith()
	// -----------------------------------------------------------------------

	@Test void i01_startsWith_passes_when_prefix_matches() {
		var resp = responseWithHeader(200, "X-Foo", "Bearer token123");
		assertDoesNotThrow(() -> new ResponseAssertion(resp).header("X-Foo").startsWith("Bearer"));
	}

	@Test void i02_startsWith_throws_when_prefix_mismatch() {
		var resp = responseWithHeader(200, "X-Foo", "Bearer token123");
		assertThrows(AssertionError.class, () -> new ResponseAssertion(resp).header("X-Foo").startsWith("Basic"));
	}

	@Test void i03_startsWith_throws_when_header_absent() {
		var resp = response(200);
		assertThrows(AssertionError.class, () -> new ResponseAssertion(resp).header("X-Missing").startsWith("any"));
	}

	// -----------------------------------------------------------------------
	// j — ResponseHeaderAssertion.integerEquals()
	// -----------------------------------------------------------------------

	@Test void j01_integerEquals_passes_when_value_matches() {
		var resp = responseWithHeader(200, "X-Count", "42");
		assertDoesNotThrow(() -> new ResponseAssertion(resp).header("X-Count").integerEquals(42));
	}

	@Test void j02_integerEquals_throws_when_value_mismatch() {
		var resp = responseWithHeader(200, "X-Count", "42");
		assertThrows(AssertionError.class, () -> new ResponseAssertion(resp).header("X-Count").integerEquals(99));
	}

	@Test void j03_integerEquals_throws_when_not_a_number() {
		var resp = responseWithHeader(200, "X-Count", "notanumber");
		assertThrows(AssertionError.class, () -> new ResponseAssertion(resp).header("X-Count").integerEquals(42));
	}

	@Test void j04_integerEquals_throws_when_header_absent() {
		var resp = response(200);
		assertThrows(AssertionError.class, () -> new ResponseAssertion(resp).header("X-Missing").integerEquals(0));
	}

	// -----------------------------------------------------------------------
	// k — ResponseHeaderAssertion.and()
	// -----------------------------------------------------------------------

	@Test void k01_headerAnd_returnsParentAssertion() {
		var resp = responseWithHeader(200, "X-Foo", "bar");
		var ra = new ResponseAssertion(resp);
		var ha = ra.header("X-Foo");
		assertSame(ra, ha.and());
	}

	// -----------------------------------------------------------------------
	// l — ResponseBodyAssertion.equals()
	// -----------------------------------------------------------------------

	@Test void l01_bodyEquals_passes_when_matches() {
		assertDoesNotThrow(() -> new ResponseAssertion(response(200, "hello")).body().equals("hello"));
	}

	@Test void l02_bodyEquals_throws_when_mismatch() {
		assertThrows(AssertionError.class, () -> new ResponseAssertion(response(200, "hello")).body().equals("world"));
	}

	@Test void l03_bodyEquals_passes_when_both_null() {
		// expected=null, actual=null (no body) — the null==null branch passes
		assertDoesNotThrow(() -> new ResponseAssertion(response(200)).body().equals(null));
	}

	@Test void l04_bodyEquals_throws_when_expected_null_actual_present() {
		// expected=null, actual has content — the null!=value branch throws
		assertThrows(AssertionError.class, () -> new ResponseAssertion(response(200, "text")).body().equals(null));
	}

	// -----------------------------------------------------------------------
	// m — ResponseBodyAssertion.contains()
	// -----------------------------------------------------------------------

	@Test void m01_bodyContains_passes_when_substring_present() {
		assertDoesNotThrow(() -> new ResponseAssertion(response(200, "hello world")).body().contains("world"));
	}

	@Test void m02_bodyContains_throws_when_substring_absent() {
		assertThrows(AssertionError.class, () -> new ResponseAssertion(response(200, "hello")).body().contains("missing"));
	}

	@Test void m03_bodyContains_throws_when_body_null() {
		// actual == null branch in contains()
		assertThrows(AssertionError.class, () -> new ResponseAssertion(response(200)).body().contains("any"));
	}

	// -----------------------------------------------------------------------
	// n — ResponseBodyAssertion.isEmpty() / isNotEmpty()
	// -----------------------------------------------------------------------

	@Test void n01_bodyIsEmpty_passes_for_empty_body() {
		assertDoesNotThrow(() -> new ResponseAssertion(response(200)).body().isEmpty());
	}

	@Test void n02_bodyIsEmpty_throws_for_non_empty_body() {
		assertThrows(AssertionError.class, () -> new ResponseAssertion(response(200, "text")).body().isEmpty());
	}

	@Test void n03_bodyIsNotEmpty_passes_for_non_empty_body() {
		assertDoesNotThrow(() -> new ResponseAssertion(response(200, "text")).body().isNotEmpty());
	}

	@Test void n04_bodyIsNotEmpty_throws_for_empty_body() {
		assertThrows(AssertionError.class, () -> new ResponseAssertion(response(200)).body().isNotEmpty());
	}

	// -----------------------------------------------------------------------
	// o — ResponseBodyAssertion.startsWith() / and()
	// -----------------------------------------------------------------------

	@Test void o01_bodyStartsWith_passes_when_prefix_matches() {
		assertDoesNotThrow(() -> new ResponseAssertion(response(200, "hello world")).body().startsWith("hello"));
	}

	@Test void o02_bodyStartsWith_throws_when_prefix_mismatch() {
		assertThrows(AssertionError.class, () -> new ResponseAssertion(response(200, "hello world")).body().startsWith("world"));
	}

	@Test void o03_bodyStartsWith_throws_when_body_absent() {
		assertThrows(AssertionError.class, () -> new ResponseAssertion(response(200)).body().startsWith("any"));
	}

	@Test void o04_bodyAnd_returnsParentAssertion() {
		var resp = response(200, "text");
		var ra = new ResponseAssertion(resp);
		assertSame(ra, ra.body().and());
	}
}
