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
package org.apache.juneau.httppart;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.httppart.*;
import org.apache.juneau.commons.httppart.SchemaValidationException;
import org.junit.jupiter.api.*;

/**
 * Tests covering OpenAPI/JSON Schema format validation paths in HttpPartSchema
 * (email, hostname, ipv4, ipv6, uri, iri, uuid, json-pointer, regex, duration,
 * time, date, date-time, date-time-zone, etc.) plus numeric edge cases (Short, Byte,
 * Long, Float, BigDecimal, BigInteger, AtomicInteger, AtomicLong) for min/max/multipleOf,
 * Builder synonyms/string overloads, and getProperty/getFormat/getType ClassInfo paths.
 */
@SuppressWarnings({
	"java:S5961" // High assertion count acceptable in comprehensive test
})
class HttpPartSchema_Validation_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// EMAIL format validation
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_format_email_valid() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("email").build();
		s.validateInput("user@example.com");
		s.validateInput("foo.bar+tag@sub.domain.com");
		s.validateInput(null);
	}

	@Test void a02_format_email_invalid() {
		var s = HttpPartSchema.create().tString().noValidate().format("email").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("not-an-email"));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("@nodomain.com"));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("noatsign.com"));
	}

	@Test void a03_format_email_literal_null() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("email").build();
		s.validateInput("null");  // Literal "null" string skips format validation
	}

	//-----------------------------------------------------------------------------------------------------------------
	// IDN_EMAIL format validation
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a04_format_idnEmail_valid() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("idn-email").build();
		s.validateInput("user@example.com");
		s.validateInput("ünïcödé@münchen.de");
	}

	@Test void a05_format_idnEmail_invalid() {
		var s = HttpPartSchema.create().tString().noValidate().format("idn-email").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("no at sign"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// HOSTNAME format validation
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a06_format_hostname_valid() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("hostname").build();
		s.validateInput("example.com");
		s.validateInput("sub.example.com");
		s.validateInput("a");
	}

	@Test void a07_format_hostname_invalid() {
		var s = HttpPartSchema.create().tString().noValidate().aev().format("hostname").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput(""));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("-bad.com"));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("bad-.com"));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("has space.com"));
	}

	@Test void a08_format_hostname_label_too_long() {
		var s = HttpPartSchema.create().tString().noValidate().format("hostname").build();
		var longLabel = "a".repeat(64);
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput(longLabel + ".com"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// IDN_HOSTNAME format validation
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a09_format_idnHostname_valid() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("idn-hostname").build();
		s.validateInput("example.com");
		s.validateInput("münchen.de");
	}

	@Test void a10_format_idnHostname_invalid() {
		var s = HttpPartSchema.create().tString().noValidate().aev().format("idn-hostname").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput(""));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("has space"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// IPV4 format validation
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a11_format_ipv4_valid() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("ipv4").build();
		s.validateInput("0.0.0.0");
		s.validateInput("127.0.0.1");
		s.validateInput("255.255.255.255");
	}

	@Test void a12_format_ipv4_invalid() {
		var s = HttpPartSchema.create().tString().noValidate().format("ipv4").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("256.0.0.0"));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("1.2.3"));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("1.2.3.4.5"));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("a.b.c.d"));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("-1.0.0.0"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// IPV6 format validation
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a13_format_ipv6_valid() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("ipv6").build();
		s.validateInput("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
		s.validateInput("::1");
	}

	@Test void a14_format_ipv6_invalid() {
		var s = HttpPartSchema.create().tString().noValidate().format("ipv6").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("not-an-ipv6"));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("1.2.3.4"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// URI / URI_REFERENCE format validation
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a15_format_uri_valid() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("uri").build();
		s.validateInput("https://example.com/path");
		s.validateInput("ftp://files.example.com");
	}

	@Test void a16_format_uriReference_valid() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("uri-reference").build();
		s.validateInput("https://example.com");
		s.validateInput("/relative/path");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// IRI / IRI_REFERENCE format validation
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a17_format_iri_valid() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("iri").build();
		s.validateInput("https://example.com/path");
	}

	@Test void a18_format_iri_invalid() {
		var s = HttpPartSchema.create().tString().noValidate().format("iri").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("notascheme"));
	}

	@Test void a19_format_iriReference_valid() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("iri-reference").build();
		s.validateInput("https://example.com");
		s.validateInput("/path");
	}

	@Test void a20_format_iriReference_invalid() {
		var s = HttpPartSchema.create().tString().noValidate().aev().format("iri-reference").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput(""));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UUID format validation
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a21_format_uuid_valid() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("uuid").build();
		s.validateInput("12345678-1234-1234-1234-123456789abc");
		s.validateInput("00000000-0000-0000-0000-000000000000");
	}

	@Test void a22_format_uuid_invalid() {
		var s = HttpPartSchema.create().tString().noValidate().format("uuid").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("not-a-uuid"));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("12345678123412341234123456789abc"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// URI_TEMPLATE format validation
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a23_format_uriTemplate_valid() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("uri-template").build();
		s.validateInput("/users/{id}");
		s.validateInput("https://example.com/{path}");
	}

	@Test void a24_format_uriTemplate_invalid() {
		var s = HttpPartSchema.create().tString().noValidate().format("uri-template").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("has space"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// JSON_POINTER / RELATIVE_JSON_POINTER format validation
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a25_format_jsonPointer_valid() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().aev().format("json-pointer").build();
		s.validateInput("");
		s.validateInput("/foo/bar");
	}

	@Test void a26_format_jsonPointer_invalid() {
		var s = HttpPartSchema.create().tString().noValidate().format("json-pointer").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("nopath"));
	}

	@Test void a27_format_relativeJsonPointer_valid() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("relative-json-pointer").build();
		s.validateInput("0");
		s.validateInput("1/foo");
		s.validateInput("3#");
		s.validateInput("123");
	}

	@Test void a28_format_relativeJsonPointer_invalid() {
		var s = HttpPartSchema.create().tString().noValidate().aev().format("relative-json-pointer").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput(""));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("abc"));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("1#extra"));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("1z"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// REGEX format validation
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a29_format_regex_valid() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("regex").build();
		s.validateInput("[a-z]+");
		s.validateInput(".*");
	}

	@Test void a30_format_regex_invalid() {
		var s = HttpPartSchema.create().tString().noValidate().format("regex").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("[unclosed"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// DATE / DATE_TIME / DATE_TIME_ZONE format validation
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a31_format_date_valid() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("date").build();
		s.validateInput("2024-01-15");
		s.validateInput("2024/01/15");
	}

	@Test void a32_format_date_invalid() {
		var s = HttpPartSchema.create().tString().noValidate().format("date").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("not-a-date"));
	}

	@Test void a33_format_dateTime_valid() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("date-time").build();
		s.validateInput("2024-01-15T10:30:45");
		s.validateInput("2024-01-15 10:30");
	}

	@Test void a34_format_dateTime_invalid() {
		var s = HttpPartSchema.create().tString().noValidate().format("date-time").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("2024-01-15"));
	}

	@Test void a35_format_dateTimeZone_valid() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("date-time-zone").build();
		s.validateInput("2024-01-15T10:30:45+05:00");
		s.validateInput("2024-01-15T10:30:45.123-08:00");
	}

	@Test void a36_format_dateTimeZone_invalid() {
		var s = HttpPartSchema.create().tString().noValidate().format("date-time-zone").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("2024-01-15"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// TIME format validation
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a37_format_time_valid() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("time").build();
		s.validateInput("10:30:45");
		s.validateInput("10:30:45.123");
		s.validateInput("10:30:45Z");
		s.validateInput("10:30:45+05:00");
	}

	@Test void a38_format_time_invalid() {
		var s = HttpPartSchema.create().tString().noValidate().format("time").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("not-a-time"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// DURATION format validation
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a39_format_duration_valid() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("duration").build();
		s.validateInput("P1Y");
		s.validateInput("P1Y2M3D");
		s.validateInput("PT1H30M");
		s.validateInput("P1Y2M3DT4H5M6S");
	}

	@Test void a40_format_duration_invalid() {
		var s = HttpPartSchema.create().tString().noValidate().format("duration").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("not-a-duration"));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("1Y"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Format pass-through validation (BYTE, BINARY, BINARY_SPACED, PASSWORD, INT32, INT64, FLOAT, DOUBLE, UON)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a41_format_passThrough_byte() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("byte").build();
		s.validateInput("anyValue");  // BYTE/BINARY/BINARY_SPACED are transformation formats, not validation
	}

	@Test void a42_format_passThrough_binary() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("binary").build();
		s.validateInput("anyValue");
	}

	@Test void a43_format_passThrough_binarySpaced() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("binary-spaced").build();
		s.validateInput("anyValue");
	}

	@Test void a44_format_passThrough_password() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("password").build();
		s.validateInput("anyValue");  // PASSWORD format is just a UI hint
	}

	@Test void a45_format_passThrough_uon() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("uon").build();
		s.validateInput("(foo=bar)");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// validateOutput - Number type variants for min/max/multipleOf (Short, Byte, Long, Float, BigDecimal, BigInteger, AtomicInteger, AtomicLong)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_validateOutput_minimum_short() throws Exception {
		var s = HttpPartSchema.create().tInteger().minimum(10).build();
		s.validateOutput((short)10);
		s.validateOutput((short)20);
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput((short)5));
	}

	@Test void b02_validateOutput_minimum_byte() throws Exception {
		var s = HttpPartSchema.create().tInteger().minimum(10).build();
		s.validateOutput((byte)10);
		s.validateOutput((byte)20);
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput((byte)5));
	}

	@Test void b03_validateOutput_minimum_long() throws Exception {
		var s = HttpPartSchema.create().tInteger().minimum(10).build();
		s.validateOutput(10L);
		s.validateOutput(20L);
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput(5L));
	}

	@Test void b04_validateOutput_minimum_atomicLong() throws Exception {
		var s = HttpPartSchema.create().tInteger().minimum(10).build();
		s.validateOutput(new AtomicLong(10));
		s.validateOutput(new AtomicLong(20));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput(new AtomicLong(5)));
	}

	@Test void b05_validateOutput_minimum_bigInteger() throws Exception {
		var s = HttpPartSchema.create().tInteger().minimum(10).build();
		s.validateOutput(BigInteger.valueOf(10));
		s.validateOutput(BigInteger.valueOf(20));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput(BigInteger.valueOf(5)));
	}

	@Test void b06_validateOutput_minimum_atomicInteger() throws Exception {
		var s = HttpPartSchema.create().tInteger().minimum(10).build();
		s.validateOutput(new AtomicInteger(10));
		s.validateOutput(new AtomicInteger(20));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput(new AtomicInteger(5)));
	}

	@Test void b07_validateOutput_minimum_float() throws Exception {
		var s = HttpPartSchema.create().tNumber().minimum(10.0).build();
		s.validateOutput(10.0f);
		s.validateOutput(20.0f);
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput(5.0f));
	}

	@Test void b08_validateOutput_minimum_bigDecimal() throws Exception {
		var s = HttpPartSchema.create().tNumber().minimum(10.0).build();
		s.validateOutput(BigDecimal.valueOf(10.0));
		s.validateOutput(BigDecimal.valueOf(20.0));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput(BigDecimal.valueOf(5.0)));
	}

	@Test void b09_validateOutput_maximum_short() throws Exception {
		var s = HttpPartSchema.create().tInteger().maximum(100).build();
		s.validateOutput((short)10);
		s.validateOutput((short)100);
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput((short)200));
	}

	@Test void b10_validateOutput_maximum_byte() throws Exception {
		var s = HttpPartSchema.create().tInteger().maximum(100).build();
		s.validateOutput((byte)10);
		s.validateOutput((byte)100);
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput((byte)127));
	}

	@Test void b11_validateOutput_maximum_long() throws Exception {
		var s = HttpPartSchema.create().tInteger().maximum(100).build();
		s.validateOutput(50L);
		s.validateOutput(100L);
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput(200L));
	}

	@Test void b12_validateOutput_maximum_bigInteger() throws Exception {
		var s = HttpPartSchema.create().tInteger().maximum(100).build();
		s.validateOutput(BigInteger.valueOf(50));
		s.validateOutput(BigInteger.valueOf(100));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput(BigInteger.valueOf(200)));
	}

	@Test void b13_validateOutput_maximum_float() throws Exception {
		var s = HttpPartSchema.create().tNumber().maximum(100.0).build();
		s.validateOutput(50.0f);
		s.validateOutput(100.0f);
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput(200.0f));
	}

	@Test void b14_validateOutput_maximum_bigDecimal() throws Exception {
		var s = HttpPartSchema.create().tNumber().maximum(100.0).build();
		s.validateOutput(BigDecimal.valueOf(50.0));
		s.validateOutput(BigDecimal.valueOf(100.0));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput(BigDecimal.valueOf(200.0)));
	}

	@Test void b15_validateOutput_multipleOf_short() throws Exception {
		var s = HttpPartSchema.create().tInteger().multipleOf(5).build();
		s.validateOutput((short)10);
		s.validateOutput((short)15);
		assertThrowsWithMessage(SchemaValidationException.class, "Multiple-of not met.", ()->s.validateOutput((short)7));
	}

	@Test void b16_validateOutput_multipleOf_byte() throws Exception {
		var s = HttpPartSchema.create().tInteger().multipleOf(5).build();
		s.validateOutput((byte)10);
		s.validateOutput((byte)15);
		assertThrowsWithMessage(SchemaValidationException.class, "Multiple-of not met.", ()->s.validateOutput((byte)7));
	}

	@Test void b17_validateOutput_multipleOf_long() throws Exception {
		var s = HttpPartSchema.create().tInteger().multipleOf(5).build();
		s.validateOutput(10L);
		s.validateOutput(15L);
		assertThrowsWithMessage(SchemaValidationException.class, "Multiple-of not met.", ()->s.validateOutput(7L));
	}

	@Test void b18_validateOutput_multipleOf_bigInteger() throws Exception {
		var s = HttpPartSchema.create().tInteger().multipleOf(5).build();
		s.validateOutput(BigInteger.valueOf(10));
		s.validateOutput(BigInteger.valueOf(15));
		assertThrowsWithMessage(SchemaValidationException.class, "Multiple-of not met.", ()->s.validateOutput(BigInteger.valueOf(7)));
	}

	@Test void b19_validateOutput_multipleOf_float() throws Exception {
		var s = HttpPartSchema.create().tNumber().multipleOf(2.5).build();
		s.validateOutput(5.0f);
		s.validateOutput(7.5f);
		assertThrowsWithMessage(SchemaValidationException.class, "Multiple-of not met.", ()->s.validateOutput(3.0f));
	}

	@Test void b20_validateOutput_multipleOf_bigDecimal() throws Exception {
		var s = HttpPartSchema.create().tNumber().multipleOf(2.5).build();
		s.validateOutput(BigDecimal.valueOf(5.0));
		s.validateOutput(BigDecimal.valueOf(7.5));
		assertThrowsWithMessage(SchemaValidationException.class, "Multiple-of not met.", ()->s.validateOutput(BigDecimal.valueOf(3.0)));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// validateOutput - exclusiveMinimumValue/exclusiveMaximumValue Number variants
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_validateOutput_exclusiveMin_short() throws Exception {
		var s = HttpPartSchema.create().tInteger().exclusiveMinimumValue(10).build();
		s.validateOutput((short)11);
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput((short)10));
	}

	@Test void c02_validateOutput_exclusiveMin_byte() throws Exception {
		var s = HttpPartSchema.create().tInteger().exclusiveMinimumValue(10).build();
		s.validateOutput((byte)11);
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput((byte)10));
	}

	@Test void c03_validateOutput_exclusiveMin_long() throws Exception {
		var s = HttpPartSchema.create().tInteger().exclusiveMinimumValue(10).build();
		s.validateOutput(11L);
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput(10L));
	}

	@Test void c04_validateOutput_exclusiveMin_bigInteger() throws Exception {
		var s = HttpPartSchema.create().tInteger().exclusiveMinimumValue(10).build();
		s.validateOutput(BigInteger.valueOf(11));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput(BigInteger.valueOf(10)));
	}

	@Test void c05_validateOutput_exclusiveMin_atomicLong() throws Exception {
		var s = HttpPartSchema.create().tInteger().exclusiveMinimumValue(10).build();
		s.validateOutput(new AtomicLong(11));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput(new AtomicLong(10)));
	}

	@Test void c06_validateOutput_exclusiveMin_atomicInteger() throws Exception {
		var s = HttpPartSchema.create().tInteger().exclusiveMinimumValue(10).build();
		s.validateOutput(new AtomicInteger(11));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput(new AtomicInteger(10)));
	}

	@Test void c07_validateOutput_exclusiveMin_float() throws Exception {
		var s = HttpPartSchema.create().tNumber().exclusiveMinimumValue(10.0).build();
		s.validateOutput(11.0f);
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput(10.0f));
	}

	@Test void c08_validateOutput_exclusiveMin_bigDecimal() throws Exception {
		var s = HttpPartSchema.create().tNumber().exclusiveMinimumValue(10.0).build();
		s.validateOutput(BigDecimal.valueOf(11.0));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput(BigDecimal.valueOf(10.0)));
	}

	@Test void c09_validateOutput_exclusiveMax_short() throws Exception {
		var s = HttpPartSchema.create().tInteger().exclusiveMaximumValue(100).build();
		s.validateOutput((short)99);
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput((short)100));
	}

	@Test void c10_validateOutput_exclusiveMax_byte() throws Exception {
		var s = HttpPartSchema.create().tInteger().exclusiveMaximumValue(100).build();
		s.validateOutput((byte)99);
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput((byte)100));
	}

	@Test void c11_validateOutput_exclusiveMax_long() throws Exception {
		var s = HttpPartSchema.create().tInteger().exclusiveMaximumValue(100).build();
		s.validateOutput(99L);
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput(100L));
	}

	@Test void c12_validateOutput_exclusiveMax_bigInteger() throws Exception {
		var s = HttpPartSchema.create().tInteger().exclusiveMaximumValue(100).build();
		s.validateOutput(BigInteger.valueOf(99));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput(BigInteger.valueOf(100)));
	}

	@Test void c13_validateOutput_exclusiveMax_atomicInteger() throws Exception {
		var s = HttpPartSchema.create().tInteger().exclusiveMaximumValue(100).build();
		s.validateOutput(new AtomicInteger(99));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput(new AtomicInteger(100)));
	}

	@Test void c14_validateOutput_exclusiveMax_atomicLong() throws Exception {
		var s = HttpPartSchema.create().tInteger().exclusiveMaximumValue(100).build();
		s.validateOutput(new AtomicLong(99));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput(new AtomicLong(100)));
	}

	@Test void c15_validateOutput_exclusiveMax_float() throws Exception {
		var s = HttpPartSchema.create().tNumber().exclusiveMaximumValue(100.0).build();
		s.validateOutput(99.0f);
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput(100.0f));
	}

	@Test void c16_validateOutput_exclusiveMax_bigDecimal() throws Exception {
		var s = HttpPartSchema.create().tNumber().exclusiveMaximumValue(100.0).build();
		s.validateOutput(BigDecimal.valueOf(99.0));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput(BigDecimal.valueOf(100.0)));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// validateOutput - Object/Map (minProperties, maxProperties, getProperty)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_validateOutput_object_minMaxProperties() throws Exception {
		var s = HttpPartSchema.create().tObject().minProperties(2L).maxProperties(4L).noValidate().build();
		s.validateOutput(map("a", 1, "b", 2));
		s.validateOutput(map("a", 1, "b", 2, "c", 3, "d", 4));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum number of properties not met.", ()->s.validateOutput(map("a", 1)));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum number of properties exceeded.", ()->s.validateOutput(map("a", 1, "b", 2, "c", 3, "d", 4, "e", 5)));
	}

	@Test void d02_validateOutput_object_property_validation() throws Exception {
		var nestedSchema = HttpPartSchema.create().tString().minLength(3L).build();
		var s = HttpPartSchema.create().tObject().property("foo", nestedSchema).build();
		s.validateOutput(map("foo", "abc"));
		s.validateOutput(map("foo", "abcdef"));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum length of value not met.", ()->s.validateOutput(map("foo", "ab")));
	}

	@Test void d03_getProperty_with_properties() throws Exception {
		var nested = HttpPartSchema.create().tString().build();
		var s = HttpPartSchema.create().tObject().property("foo", nested).build();
		assertNotNull(s.getProperty("foo"));
		assertNull(s.getProperty("bar"));
	}

	@Test void d04_getProperty_falls_back_to_additionalProperties() throws Exception {
		var addl = HttpPartSchema.create().tString().build();
		var s = HttpPartSchema.create().tObject().additionalProperties(addl).build();
		// "anyKey" is not in properties, but additionalProperties returns the schema
		assertNotNull(s.getProperty("anyKey"));
	}

	@Test void d05_getProperty_no_properties_no_additional() throws Exception {
		var s = HttpPartSchema.create().tObject().build();
		assertNull(s.getProperty("anyKey"));
	}

	@Test void d06_hasProperties() throws Exception {
		var s1 = HttpPartSchema.create().tObject().build();
		assertFalse(s1.hasProperties());

		var s2 = HttpPartSchema.create().tObject().property("foo", HttpPartSchema.create().tString().build()).build();
		assertTrue(s2.hasProperties());

		var s3 = HttpPartSchema.create().tObject().additionalProperties(HttpPartSchema.create().tString().build()).build();
		assertTrue(s3.hasProperties());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// validateOutput - String length and format
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_validateOutput_string_minMaxLength() throws Exception {
		var s = HttpPartSchema.create().tString().minLength(3L).maxLength(10L).build();
		s.validateOutput("abc");
		s.validateOutput("abcdefghij");
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum length of value not met.", ()->s.validateOutput("ab"));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum length of value exceeded.", ()->s.validateOutput("abcdefghijk"));
	}

	@Test void e02_validateOutput_string_pattern() throws Exception {
		var s = HttpPartSchema.create().tString().pattern("^[a-z]+$").build();
		s.validateOutput("abc");
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected pattern", ()->s.validateOutput("ABC123"));
	}

	@Test void e03_validateOutput_string_format() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("uuid").build();
		s.validateOutput("12345678-1234-1234-1234-123456789abc");
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateOutput("not-a-uuid"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// validateOutput - non-matching types do nothing
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_validateOutput_nonString_for_stringType() throws Exception {
		var s = HttpPartSchema.create().tString().minLength(5L).build();
		// Acceptable string passes validation
		s.validateOutput("hello with sufficient length to pass minLength validation");
	}

	@Test void f02_validateOutput_nonNumber_for_integerType() throws Exception {
		var s = HttpPartSchema.create().tInteger().minimum(100).noValidate().build();
		// String passed but type is INTEGER; not a number, validation skipped on non-Number
		// Note: actual invocation goes through getType(ClassInfo) which may infer differently.
		// The key path is the inner check: ci.isNumber() returns false, validation is skipped.
		// Just make sure no exception when passing valid value
		s.validateOutput(200);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// validateOutput - required null check
	//-----------------------------------------------------------------------------------------------------------------

	@Test void g01_validateOutput_required_null() {
		var s = HttpPartSchema.create().tString().required(true).build();
		assertThrowsWithMessage(SchemaValidationException.class, "Required value not provided.", ()->s.validateOutput(null));
	}

	@Test void g02_validateOutput_optional_null() throws Exception {
		var s = HttpPartSchema.create().tString().required(false).build();
		assertNull(s.validateOutput(null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder synonym methods (cf, cfXxx, fXxx, tXxx, df, e, emax, emin, p, r, sie, ui, ap, aev, i, max, maxX, min, minX, mo, n)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void h01_builder_synonyms_collectionFormat() {
		assertEquals(HttpPartCollectionFormat.CSV, HttpPartSchema.create().cf("csv").build().getCollectionFormat());
		assertEquals(HttpPartCollectionFormat.PIPES, HttpPartSchema.create().cf(HttpPartCollectionFormat.PIPES).build().getCollectionFormat());
		assertEquals(HttpPartCollectionFormat.CSV, HttpPartSchema.create().cfCsv().build().getCollectionFormat());
		assertEquals(HttpPartCollectionFormat.PIPES, HttpPartSchema.create().cfPipes().build().getCollectionFormat());
		assertEquals(HttpPartCollectionFormat.SSV, HttpPartSchema.create().cfSsv().build().getCollectionFormat());
		assertEquals(HttpPartCollectionFormat.TSV, HttpPartSchema.create().cfTsv().build().getCollectionFormat());
		assertEquals(HttpPartCollectionFormat.MULTI, HttpPartSchema.create().cfMulti().build().getCollectionFormat());
		assertEquals(HttpPartCollectionFormat.UONC, HttpPartSchema.create().cfUon().build().getCollectionFormat());
		assertEquals(HttpPartCollectionFormat.NO_COLLECTION_FORMAT, HttpPartSchema.create().cfCsv().cfNone().build().getCollectionFormat());
	}

	@Test void h02_builder_synonyms_format() {
		assertEquals(HttpPartFormat.INT32, HttpPartSchema.create().f("int32").build().getFormat());
		assertEquals(HttpPartFormat.INT64, HttpPartSchema.create().f(HttpPartFormat.INT64).build().getFormat());
		assertEquals(HttpPartFormat.INT32, HttpPartSchema.create().fInt32().build().getFormat());
		assertEquals(HttpPartFormat.INT64, HttpPartSchema.create().fInt64().build().getFormat());
		assertEquals(HttpPartFormat.FLOAT, HttpPartSchema.create().fFloat().build().getFormat());
		assertEquals(HttpPartFormat.DOUBLE, HttpPartSchema.create().fDouble().build().getFormat());
		assertEquals(HttpPartFormat.BYTE, HttpPartSchema.create().fByte().build().getFormat());
		assertEquals(HttpPartFormat.BINARY, HttpPartSchema.create().fBinary().build().getFormat());
		assertEquals(HttpPartFormat.BINARY_SPACED, HttpPartSchema.create().fBinarySpaced().build().getFormat());
		assertEquals(HttpPartFormat.DATE, HttpPartSchema.create().fDate().build().getFormat());
		assertEquals(HttpPartFormat.DATE_TIME, HttpPartSchema.create().fDateTime().build().getFormat());
		assertEquals(HttpPartFormat.PASSWORD, HttpPartSchema.create().fPassword().build().getFormat());
		assertEquals(HttpPartFormat.UON, HttpPartSchema.create().fUon().build().getFormat());
		assertEquals(HttpPartFormat.NO_FORMAT, HttpPartSchema.create().fInt32().fNone().build().getFormat());
	}

	@Test void h03_builder_synonyms_type() {
		assertEquals(HttpPartDataType.STRING, HttpPartSchema.create().t("string").build().getType());
		assertEquals(HttpPartDataType.INTEGER, HttpPartSchema.create().t(HttpPartDataType.INTEGER).build().getType());
		assertEquals(HttpPartDataType.STRING, HttpPartSchema.create().tString().build().getType());
		assertEquals(HttpPartDataType.INTEGER, HttpPartSchema.create().tInteger().build().getType());
		assertEquals(HttpPartDataType.NUMBER, HttpPartSchema.create().tNumber().build().getType());
		assertEquals(HttpPartDataType.BOOLEAN, HttpPartSchema.create().tBoolean().build().getType());
		assertEquals(HttpPartDataType.ARRAY, HttpPartSchema.create().tArray().noValidate().build().getType());
		assertEquals(HttpPartDataType.OBJECT, HttpPartSchema.create().tObject().build().getType());
		assertEquals(HttpPartDataType.FILE, HttpPartSchema.create().tFile().build().getType());
		assertEquals(HttpPartDataType.NO_TYPE, HttpPartSchema.create().tString().tNone().build().getType());
	}

	@Test void h04_builder_synonyms_default() {
		assertEquals("hello", HttpPartSchema.create().df("hello").build().getDefault());
	}

	@Test void h05_builder_synonyms_enum() {
		assertEquals(set("a", "b", "c"), HttpPartSchema.create().e("a", "b", "c").build().getEnum());
		assertEquals(set("x", "y"), HttpPartSchema.create().e(set("x", "y")).build().getEnum());
	}

	@Test void h06_builder_synonyms_exclusiveMaxMin_string() {
		var s1 = HttpPartSchema.create().tNumber().min(0).max(100).emax("true").emin("true").build();
		assertTrue(s1.isExclusiveMaximum());
		assertTrue(s1.isExclusiveMinimum());

		var s2 = HttpPartSchema.create().tNumber().min(0).max(100).emax(Boolean.TRUE).emin(Boolean.TRUE).build();
		assertTrue(s2.isExclusiveMaximum());
		assertTrue(s2.isExclusiveMinimum());

		var s3 = HttpPartSchema.create().tNumber().min(0).max(100).emax().emin().build();
		assertTrue(s3.isExclusiveMaximum());
		assertTrue(s3.isExclusiveMinimum());
	}

	@Test void h07_builder_synonyms_pattern() {
		assertEquals("^x.*", HttpPartSchema.create().tString().p("^x.*").build().getPattern().pattern());
	}

	@Test void h08_builder_synonyms_required() {
		assertTrue(HttpPartSchema.create().r().build().isRequired());
		assertTrue(HttpPartSchema.create().r(true).build().isRequired());
		assertTrue(HttpPartSchema.create().r("true").build().isRequired());
	}

	@Test void h09_builder_synonyms_skipIfEmpty() {
		assertTrue(HttpPartSchema.create().sie().build().isSkipIfEmpty());
		assertTrue(HttpPartSchema.create().sie(true).build().isSkipIfEmpty());
		assertTrue(HttpPartSchema.create().sie("true").build().isSkipIfEmpty());
	}

	@Test void h10_builder_synonyms_uniqueItems() {
		var s1 = HttpPartSchema.create().tArray().items(HttpPartSchema.create().tString()).ui().build();
		assertTrue(s1.isUniqueItems());
		var s2 = HttpPartSchema.create().tArray().items(HttpPartSchema.create().tString()).ui(true).build();
		assertTrue(s2.isUniqueItems());
		var s3 = HttpPartSchema.create().tArray().items(HttpPartSchema.create().tString()).ui("true").build();
		assertTrue(s3.isUniqueItems());
	}

	@Test void h11_builder_synonyms_allowEmptyValue() {
		assertTrue(HttpPartSchema.create().aev().build().isAllowEmptyValue());
		assertTrue(HttpPartSchema.create().aev(true).build().isAllowEmptyValue());
		assertTrue(HttpPartSchema.create().aev("true").build().isAllowEmptyValue());
	}

	@Test void h12_builder_synonyms_minMaxLengthItemsProperties() {
		var s = HttpPartSchema.create().tString().maxl(10L).minl(2L).build();
		assertEquals(10L, s.getMaxLength());
		assertEquals(2L, s.getMinLength());

		var s2 = HttpPartSchema.create().tString().maxl("10").minl("2").build();
		assertEquals(10L, s2.getMaxLength());
		assertEquals(2L, s2.getMinLength());

		var s3 = HttpPartSchema.create().tArray().items(HttpPartSchema.create().tString()).maxi(10L).mini(2L).build();
		assertEquals(10L, s3.getMaxItems());
		assertEquals(2L, s3.getMinItems());

		var s4 = HttpPartSchema.create().tArray().items(HttpPartSchema.create().tString()).maxi("10").mini("2").build();
		assertEquals(10L, s4.getMaxItems());
		assertEquals(2L, s4.getMinItems());

		var s5 = HttpPartSchema.create().tObject().maxp(10L).minp(2L).build();
		assertEquals(10L, s5.getMaxProperties());
		assertEquals(2L, s5.getMinProperties());

		var s6 = HttpPartSchema.create().tObject().maxp("10").minp("2").build();
		assertEquals(10L, s6.getMaxProperties());
		assertEquals(2L, s6.getMinProperties());
	}

	@Test void h13_builder_synonyms_minMaxMultipleOf() {
		var s = HttpPartSchema.create().tNumber().min(1).max(100).mo(2).build();
		assertEquals(1, s.getMinimum());
		assertEquals(100, s.getMaximum());
		assertEquals(2, s.getMultipleOf());
	}

	@Test void h14_builder_synonyms_name() {
		assertEquals("foo", HttpPartSchema.create().n("foo").build().getName());
	}

	@Test void h15_builder_synonyms_items_additionalProperties() {
		var inner = HttpPartSchema.create().tString();
		var s1 = HttpPartSchema.create().tArray().i(inner).build();
		assertNotNull(s1.getItems());

		var inner2 = HttpPartSchema.create().tString().build();
		var s2 = HttpPartSchema.create().tArray().i(inner2).build();
		assertNotNull(s2.getItems());

		var addl = HttpPartSchema.create().tString();
		var s3 = HttpPartSchema.create().tObject().ap(addl).build();
		assertNotNull(s3.getProperty("anyKey"));

		var addl2 = HttpPartSchema.create().tString().build();
		var s4 = HttpPartSchema.create().tObject().ap(addl2).build();
		assertNotNull(s4.getProperty("anyKey"));
	}

	@Test void h16_builder_p_property() {
		var inner = HttpPartSchema.create().tString();
		var s = HttpPartSchema.create().tObject().p("foo", inner).build();
		assertNotNull(s.getProperty("foo"));

		var inner2 = HttpPartSchema.create().tString().build();
		var s2 = HttpPartSchema.create().tObject().p("foo", inner2).build();
		assertNotNull(s2.getProperty("foo"));
	}

	@Test void h17_builder_code_codes() {
		var s = HttpPartSchema.create().code(200).build();
		assertNotNull(s);

		var s2 = HttpPartSchema.create().codes(new int[]{200, 201, 204}).build();
		assertNotNull(s2);

		// Zero is ignored
		var s3 = HttpPartSchema.create().code(0).build();
		assertNotNull(s3);

		// Null and empty arrays are ignored
		var s4 = HttpPartSchema.create().codes(null).build();
		assertNotNull(s4);
		var s5 = HttpPartSchema.create().codes(new int[0]).build();
		assertNotNull(s5);
	}

	@Test void h18_builder_const_examples_deprecated() {
		var s = HttpPartSchema.create().tString().const_("CONST").examples("a", "b").deprecated(true).build();
		assertNotNull(s);
	}

	@Test void h19_builder_exclusiveValueSetters() {
		var s = HttpPartSchema.create().tInteger().exclusiveMaximumValue(100).exclusiveMinimumValue(0).build();
		// These should take precedence over the boolean flags
		s.validateOutput(50);
	}

	@Test void h20_builder_noValidate() {
		// Schema with conflicting properties should throw without noValidate
		assertThrowsWithMessage(BasicRuntimeException.class, "Schema specification errors", ()->HttpPartSchema.create().tString().minimum(1).build());
		// Same with noValidate succeeds
		var s = HttpPartSchema.create().tString().minimum(1).noValidate().build();
		assertNotNull(s);
	}

	@Test void h21_builder_noValidate_synonyms() {
		var s1 = HttpPartSchema.create().tString().minimum(1).noValidate(true).build();
		assertNotNull(s1);
		var s2 = HttpPartSchema.create().tString().minimum(1).noValidate(Boolean.TRUE).build();
		assertNotNull(s2);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder.format(String) and type(String) error paths
	//-----------------------------------------------------------------------------------------------------------------

	@Test void i01_builder_format_invalid() {
		assertThrowsWithMessage(BasicRuntimeException.class, "Invalid value", ()->HttpPartSchema.create().format("not-a-format").build());
	}

	@Test void i02_builder_format_empty_ignored() throws Exception {
		var s = HttpPartSchema.create().tString().fByte().format("").build();
		// Empty value is ignored, so format remains BYTE
		assertEquals(HttpPartFormat.BYTE, s.getFormat());
	}

	@Test void i03_builder_format_null_ignored() throws Exception {
		var s = HttpPartSchema.create().tString().fByte().format((String)null).build();
		assertEquals(HttpPartFormat.BYTE, s.getFormat());
	}

	@Test void i04_builder_type_invalid() {
		assertThrowsWithMessage(BasicRuntimeException.class, "Invalid value", ()->HttpPartSchema.create().type("not-a-type").build());
	}

	@Test void i05_builder_type_empty_ignored() throws Exception {
		var s = HttpPartSchema.create().tInteger().type("").build();
		assertEquals(HttpPartDataType.INTEGER, s.getType());
	}

	@Test void i06_builder_collectionFormat_invalid() {
		assertThrowsWithMessage(BasicRuntimeException.class, "Invalid value", ()->HttpPartSchema.create().collectionFormat("not-a-format").build());
	}

	@Test void i07_builder_pattern_invalid() {
		assertThrowsWithMessage(BasicRuntimeException.class, "Invalid value", ()->HttpPartSchema.create().tString().pattern("[unclosed").build());
	}

	@Test void i08_builder_pattern_empty_ignored() throws Exception {
		var s = HttpPartSchema.create().tString().pattern("^x.*").pattern("").build();
		// Empty pattern doesn't override existing
		assertEquals("^x.*", s.getPattern().pattern());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder.parser/serializer
	//-----------------------------------------------------------------------------------------------------------------

	@Test void j01_builder_parser_serializer_void_ignored() throws Exception {
		var s = HttpPartSchema.create().parser(Void.class).serializer(Void.class).build();
		assertNull(s.getParser());
		assertNull(s.getSerializer());
	}

	@Test void j02_builder_parser_serializer_set() throws Exception {
		var s = HttpPartSchema.create().parser(String.class).serializer(Integer.class).build();
		assertEquals(String.class, s.getParser());
		assertEquals(Integer.class, s.getSerializer());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Statics: HttpPartSchema.create(String type), create(String, String), getProperty constants
	//-----------------------------------------------------------------------------------------------------------------

	@Test void k01_create_with_type() throws Exception {
		var s = HttpPartSchema.create("string").build();
		assertEquals(HttpPartDataType.STRING, s.getType());
	}

	@Test void k02_create_with_type_format() throws Exception {
		var s = HttpPartSchema.create("integer", "int32").build();
		assertEquals(HttpPartDataType.INTEGER, s.getType());
		assertEquals(HttpPartFormat.INT32, s.getFormat());
	}

	@Test void k03_static_T_constants() throws Exception {
		assertNotNull(HttpPartSchema.DEFAULT);
		assertEquals(HttpPartDataType.BOOLEAN, HttpPartSchema.T_BOOLEAN.getType());
		assertEquals(HttpPartDataType.STRING, HttpPartSchema.T_STRING.getType());
		assertEquals(HttpPartDataType.INTEGER, HttpPartSchema.T_INTEGER.getType());
		assertEquals(HttpPartDataType.NUMBER, HttpPartSchema.T_NUMBER.getType());
		assertEquals(HttpPartDataType.OBJECT, HttpPartSchema.T_OBJECT.getType());
		assertEquals(HttpPartDataType.FILE, HttpPartSchema.T_FILE.getType());
		assertEquals(HttpPartFormat.INT32, HttpPartSchema.T_INT32.getFormat());
		assertEquals(HttpPartFormat.INT64, HttpPartSchema.T_INT64.getFormat());
		assertEquals(HttpPartFormat.FLOAT, HttpPartSchema.T_FLOAT.getFormat());
		assertEquals(HttpPartFormat.DOUBLE, HttpPartSchema.T_DOUBLE.getFormat());
		assertEquals(HttpPartFormat.BYTE, HttpPartSchema.T_BYTE.getFormat());
		assertEquals(HttpPartFormat.BINARY, HttpPartSchema.T_BINARY.getFormat());
		assertEquals(HttpPartFormat.BINARY_SPACED, HttpPartSchema.T_BINARY_SPACED.getFormat());
		assertEquals(HttpPartFormat.DATE, HttpPartSchema.T_DATE.getFormat());
		assertEquals(HttpPartFormat.DATE_TIME, HttpPartSchema.T_DATETIME.getFormat());
		assertEquals(HttpPartFormat.UON, HttpPartSchema.T_UON.getFormat());
		assertEquals(HttpPartCollectionFormat.CSV, HttpPartSchema.T_ARRAY_CSV.getCollectionFormat());
		assertEquals(HttpPartCollectionFormat.PIPES, HttpPartSchema.T_ARRAY_PIPES.getCollectionFormat());
		assertEquals(HttpPartCollectionFormat.SSV, HttpPartSchema.T_ARRAY_SSV.getCollectionFormat());
		assertEquals(HttpPartCollectionFormat.TSV, HttpPartSchema.T_ARRAY_TSV.getCollectionFormat());
		assertEquals(HttpPartCollectionFormat.UONC, HttpPartSchema.T_ARRAY_UON.getCollectionFormat());
		assertEquals(HttpPartCollectionFormat.MULTI, HttpPartSchema.T_ARRAY_MULTI.getCollectionFormat());
	}

	@Test void k04_static_tArray_with_items() throws Exception {
		var inner = HttpPartSchema.create().tString();
		var s = HttpPartSchema.tArray(inner).build();
		assertNotNull(s.getItems());
	}

	@Test void k05_static_tArrayXxx_with_items() throws Exception {
		var inner = HttpPartSchema.create().tString();
		assertNotNull(HttpPartSchema.tArrayCsv(inner).build().getItems());
		assertNotNull(HttpPartSchema.tArrayPipes(inner).build().getItems());
		assertNotNull(HttpPartSchema.tArraySsv(inner).build().getItems());
		assertNotNull(HttpPartSchema.tArrayTsv(inner).build().getItems());
		assertNotNull(HttpPartSchema.tArrayUon(inner).build().getItems());
		assertNotNull(HttpPartSchema.tArrayMulti(inner).build().getItems());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Static toSet helper paths
	//-----------------------------------------------------------------------------------------------------------------

	@Test void l01_enum_empty_handling() throws Exception {
		// Empty Set is filtered by builder - getEnum() returns empty
		var s2 = HttpPartSchema.create().tString().enum_(set()).build();
		assertTrue(s2.getEnum().isEmpty());

		// Empty varargs equivalent - no enum set
		var s3 = HttpPartSchema.create().tString().enum_().build();
		assertTrue(s3.getEnum().isEmpty());

		// Null Set is filtered by builder
		var s4 = HttpPartSchema.create().tString().enum_((java.util.Set<String>)null).build();
		assertTrue(s4.getEnum().isEmpty());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Schema specification error paths
	//-----------------------------------------------------------------------------------------------------------------

	@Test void m01_validation_string_with_collectionFormat() {
		assertThrowsWithMessage(BasicRuntimeException.class, "Schema specification errors", ()->HttpPartSchema.create().tString().cfCsv().build());
	}

	@Test void m02_validation_string_with_uniqueItems() {
		assertThrowsWithMessage(BasicRuntimeException.class, "Schema specification errors", ()->HttpPartSchema.create().tString().ui(true).build());
	}

	@Test void m03_validation_string_with_maximum() {
		assertThrowsWithMessage(BasicRuntimeException.class, "Schema specification errors", ()->HttpPartSchema.create().tString().maximum(100).build());
	}

	@Test void m04_validation_array_with_pattern() {
		assertThrowsWithMessage(BasicRuntimeException.class, "Schema specification errors", ()->HttpPartSchema.create().tArray().items(HttpPartSchema.create().tString()).pattern("x.*").build());
	}

	@Test void m05_validation_boolean_with_pattern() {
		assertThrowsWithMessage(BasicRuntimeException.class, "Schema specification errors", ()->HttpPartSchema.create().tBoolean().pattern("x.*").build());
	}

	@Test void m06_validation_integer_with_pattern() {
		assertThrowsWithMessage(BasicRuntimeException.class, "Schema specification errors", ()->HttpPartSchema.create().tInteger().pattern("x.*").build());
	}

	@Test void m07_validation_number_with_pattern() {
		assertThrowsWithMessage(BasicRuntimeException.class, "Schema specification errors", ()->HttpPartSchema.create().tNumber().pattern("x.*").build());
	}

	@Test void m08_validation_object_with_pattern() {
		assertThrowsWithMessage(BasicRuntimeException.class, "Schema specification errors", ()->HttpPartSchema.create().tObject().pattern("x.*").build());
	}

	@Test void m09_validation_exclusiveMax_without_max() {
		assertThrowsWithMessage(BasicRuntimeException.class, "Cannot specify exclusiveMaximum with maximum", ()->HttpPartSchema.create().tInteger().emax(true).build());
	}

	@Test void m10_validation_exclusiveMin_without_min() {
		assertThrowsWithMessage(BasicRuntimeException.class, "Cannot specify exclusiveMinimum with minimum", ()->HttpPartSchema.create().tInteger().emin(true).build());
	}

	@Test void m11_validation_required_with_default() {
		assertThrowsWithMessage(BasicRuntimeException.class, "Cannot specify a default value on a required value", ()->HttpPartSchema.create().tString().required(true).default_("def").build());
	}

	@Test void m12_validation_maxLength_lt_minLength() {
		assertThrowsWithMessage(BasicRuntimeException.class, "maxLength cannot be less than minLength", ()->HttpPartSchema.create().tString().minLength(10L).maxLength(5L).build());
	}

	@Test void m13_validation_max_lt_min() {
		assertThrowsWithMessage(BasicRuntimeException.class, "maximum cannot be less than minimum", ()->HttpPartSchema.create().tInteger().minimum(10).maximum(5).build());
	}

	@Test void m14_validation_maxItems_lt_minItems() {
		assertThrowsWithMessage(BasicRuntimeException.class, "maxItems cannot be less than minItems", ()->HttpPartSchema.create().tArray().items(HttpPartSchema.create().tString()).minItems(10L).maxItems(5L).build());
	}

	@Test void m15_validation_maxProperties_lt_minProperties() {
		assertThrowsWithMessage(BasicRuntimeException.class, "maxProperties cannot be less than minProperties", ()->HttpPartSchema.create().tObject().minProperties(10L).maxProperties(5L).build());
	}

	@Test void m16_validation_negative_lengths() {
		assertThrowsWithMessage(BasicRuntimeException.class, "minLength cannot be less than zero", ()->HttpPartSchema.create().tString().minLength(-2L).build());
		assertThrowsWithMessage(BasicRuntimeException.class, "maxLength cannot be less than zero", ()->HttpPartSchema.create().tString().maxLength(-2L).build());
	}

	@Test void m17_validation_negative_items() {
		assertThrowsWithMessage(BasicRuntimeException.class, "minItems cannot be less than zero", ()->HttpPartSchema.create().tArray().items(HttpPartSchema.create().tString()).minItems(-2L).build());
		assertThrowsWithMessage(BasicRuntimeException.class, "maxItems cannot be less than zero", ()->HttpPartSchema.create().tArray().items(HttpPartSchema.create().tString()).maxItems(-2L).build());
	}

	@Test void m18_validation_negative_properties() {
		assertThrowsWithMessage(BasicRuntimeException.class, "minProperties cannot be less than zero", ()->HttpPartSchema.create().tObject().minProperties(-2L).build());
		assertThrowsWithMessage(BasicRuntimeException.class, "maxProperties cannot be less than zero", ()->HttpPartSchema.create().tObject().maxProperties(-2L).build());
	}

	@Test void m20_validation_array_of_objects_uon_ok() throws Exception {
		var inner = HttpPartSchema.create().tObject();
		// UON format is allowed for array-of-objects
		var s = HttpPartSchema.create().tArray().items(inner).fUon().build();
		assertNotNull(s);
	}

	@Test void m21_validation_invalid_format_string_type() {
		// INT32 is not valid for STRING
		assertThrowsWithMessage(BasicRuntimeException.class, "Invalid format for type", ()->HttpPartSchema.create().tString().fInt32().build());
	}

	@Test void m22_validation_invalid_format_array_type() {
		// INT32 is not valid for ARRAY
		assertThrowsWithMessage(BasicRuntimeException.class, "Invalid format for type", ()->HttpPartSchema.create().tArray().items(HttpPartSchema.create().tString()).fInt32().build());
	}

	@Test void m23_validation_invalid_format_integer_type() {
		// FLOAT is not valid for INTEGER
		assertThrowsWithMessage(BasicRuntimeException.class, "Invalid format for type", ()->HttpPartSchema.create().tInteger().fFloat().build());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// validateInput edge cases
	//-----------------------------------------------------------------------------------------------------------------

	@Test void n01_validateInput_allowEmptyValue() throws Exception {
		var s = HttpPartSchema.create().tString().allowEmptyValue(true).build();
		s.validateInput("");
		s.validateInput(null);
	}

	@Test void n02_validateInput_required_null_throws() {
		var s = HttpPartSchema.create().tString().required(true).build();
		assertThrowsWithMessage(SchemaValidationException.class, "No value specified.", ()->s.validateInput(null));
	}

	@Test void n03_validateInput_pattern() throws Exception {
		var s = HttpPartSchema.create().tString().pattern("^[a-z]+$").build();
		s.validateInput("abc");
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected pattern", ()->s.validateInput("ABC"));
	}

	@Test void n04_validateInput_enum() throws Exception {
		var s = HttpPartSchema.create().tString().enum_("a", "b", "c").build();
		s.validateInput("a");
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match one of the expected values", ()->s.validateInput("z"));
	}

	@Test void n05_validateInput_maxLength() {
		var s = HttpPartSchema.create().tString().maxLength(5L).build();
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum length of value exceeded.", ()->s.validateInput("toolong"));
	}

	@Test void n06_validateInput_minLength() {
		var s = HttpPartSchema.create().tString().minLength(5L).build();
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum length of value not met.", ()->s.validateInput("ab"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// toString and properties()
	//-----------------------------------------------------------------------------------------------------------------

	@Test void o01_toString_basic() throws Exception {
		var s = HttpPartSchema.create().tString().build();
		var str = s.toString();
		assertNotNull(str);
		assertFalse(str.isEmpty());
	}

	@Test void o02_toString_with_properties() throws Exception {
		var s = HttpPartSchema.create().tString().minLength(3L).maxLength(10L).pattern("^x.*").required(true).build();
		var str = s.toString();
		assertNotNull(str);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getFormat(ClassInfo) and getType(ClassInfo) auto-detection paths
	//-----------------------------------------------------------------------------------------------------------------

	@Test void p01_getFormat_classInfo_double() throws Exception {
		var s = HttpPartSchema.create().build();
		var ci = org.apache.juneau.commons.reflect.ClassInfo.of(Double.class);
		assertEquals(HttpPartFormat.DOUBLE, s.getFormat(ci));
	}

	@Test void p02_getFormat_classInfo_float() throws Exception {
		var s = HttpPartSchema.create().build();
		var ci = org.apache.juneau.commons.reflect.ClassInfo.of(Float.class);
		assertEquals(HttpPartFormat.FLOAT, s.getFormat(ci));
	}

	@Test void p03_getFormat_classInfo_long() throws Exception {
		var s = HttpPartSchema.create().build();
		var ci = org.apache.juneau.commons.reflect.ClassInfo.of(Long.class);
		assertEquals(HttpPartFormat.INT64, s.getFormat(ci));
	}

	@Test void p04_getFormat_classInfo_int() throws Exception {
		var s = HttpPartSchema.create().build();
		var ci = org.apache.juneau.commons.reflect.ClassInfo.of(Integer.class);
		assertEquals(HttpPartFormat.INT32, s.getFormat(ci));
	}

	@Test void p05_getFormat_classInfo_nonNumber() throws Exception {
		var s = HttpPartSchema.create().build();
		var ci = org.apache.juneau.commons.reflect.ClassInfo.of(String.class);
		assertEquals(HttpPartFormat.NO_FORMAT, s.getFormat(ci));
	}

	@Test void p06_getFormat_classInfo_explicit_format_overrides() throws Exception {
		var s = HttpPartSchema.create().fInt32().tInteger().build();
		var ci = org.apache.juneau.commons.reflect.ClassInfo.of(Long.class);
		// Explicit format wins
		assertEquals(HttpPartFormat.INT32, s.getFormat(ci));
	}

	@Test void p07_getType_classInfo_string() throws Exception {
		var s = HttpPartSchema.create().build();
		var ci = org.apache.juneau.commons.reflect.ClassInfo.of(String.class);
		assertEquals(HttpPartDataType.STRING, s.getType(ci));
	}

	@Test void p08_getType_classInfo_number() throws Exception {
		var s = HttpPartSchema.create().build();
		var ci = org.apache.juneau.commons.reflect.ClassInfo.of(Double.class);
		assertEquals(HttpPartDataType.NUMBER, s.getType(ci));
	}

	@Test void p09_getType_classInfo_integer() throws Exception {
		var s = HttpPartSchema.create().build();
		var ci = org.apache.juneau.commons.reflect.ClassInfo.of(Integer.class);
		assertEquals(HttpPartDataType.INTEGER, s.getType(ci));
	}

	@Test void p10_getType_classInfo_boolean() throws Exception {
		var s = HttpPartSchema.create().build();
		var ci = org.apache.juneau.commons.reflect.ClassInfo.of(Boolean.class);
		assertEquals(HttpPartDataType.BOOLEAN, s.getType(ci));
	}

	@Test void p11_getType_classInfo_collection() throws Exception {
		var s = HttpPartSchema.create().build();
		var ci = org.apache.juneau.commons.reflect.ClassInfo.of(java.util.List.class);
		assertEquals(HttpPartDataType.ARRAY, s.getType(ci));
	}

	@Test void p12_getType_classInfo_map() throws Exception {
		var s = HttpPartSchema.create().build();
		var ci = org.apache.juneau.commons.reflect.ClassInfo.of(java.util.Map.class);
		assertEquals(HttpPartDataType.OBJECT, s.getType(ci));
	}

	@Test void p13_getType_classInfo_explicit_overrides() throws Exception {
		var s = HttpPartSchema.create().tString().build();
		var ci = org.apache.juneau.commons.reflect.ClassInfo.of(Integer.class);
		// Explicit type wins
		assertEquals(HttpPartDataType.STRING, s.getType(ci));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// validateOutput - non-numeric for INTEGER/NUMBER skips validation
	//-----------------------------------------------------------------------------------------------------------------

	@Test void q01_validateOutput_integer_string_skips() throws Exception {
		var s = HttpPartSchema.create().tInteger().minimum(100).noValidate().build();
		// For INTEGER type with non-Number value, validation is skipped
		s.validateOutput("not-a-number");  // schema.getType(ci) for String is STRING, skips
	}

	@Test void q02_validateOutput_array_collection() throws Exception {
		var s = HttpPartSchema.create().tArray().minItems(2L).maxItems(4L).items(HttpPartSchema.create().tString()).build();
		s.validateOutput(l("a", "b"));
		s.validateOutput(l("a", "b", "c", "d"));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum number of items not met.", ()->s.validateOutput(l("a")));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum number of items exceeded.", ()->s.validateOutput(l("a", "b", "c", "d", "e")));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Inclusive/Exclusive boundary edges with maximum + boolean exclusiveMaximum (Short, Byte, Long, BigInteger, etc.)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void r01_validateOutput_short_boundary_inclusive() throws Exception {
		var s = HttpPartSchema.create().tInteger().maximum(100).minimum(0).build();
		// At boundary - inclusive
		s.validateOutput((short)0);
		s.validateOutput((short)100);
	}

	@Test void r02_validateOutput_long_boundary_inclusive() throws Exception {
		var s = HttpPartSchema.create().tInteger().maximum(100L).minimum(0L).build();
		s.validateOutput(0L);
		s.validateOutput(100L);
	}

	@Test void r03_validateOutput_float_boundary_inclusive() throws Exception {
		var s = HttpPartSchema.create().tNumber().maximum(100.0).minimum(0.0).build();
		s.validateOutput(0.0f);
		s.validateOutput(100.0f);
	}

	@Test void r04_validateOutput_bigInteger_boundary_inclusive() throws Exception {
		var s = HttpPartSchema.create().tInteger().maximum(100).minimum(0).build();
		s.validateOutput(BigInteger.valueOf(0));
		s.validateOutput(BigInteger.valueOf(100));
	}

	@Test void r05_validateOutput_bigDecimal_boundary_inclusive() throws Exception {
		var s = HttpPartSchema.create().tNumber().maximum(100.0).minimum(0.0).build();
		s.validateOutput(BigDecimal.valueOf(0.0));
		s.validateOutput(BigDecimal.valueOf(100.0));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// validateOutput - array with items schema validation for each element
	//-----------------------------------------------------------------------------------------------------------------

	@Test void s01_validateOutput_array_items_validate_array() throws Exception {
		var inner = HttpPartSchema.create().tString().minLength(2L);
		var s = HttpPartSchema.create().tArray().items(inner).build();
		s.validateOutput(new String[]{"abc", "defghi"});
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum length of value not met.", ()->s.validateOutput(new String[]{"a", "b"}));
	}

	@Test void s02_validateOutput_array_items_validate_collection() throws Exception {
		var inner = HttpPartSchema.create().tString().minLength(2L);
		var s = HttpPartSchema.create().tArray().items(inner).build();
		s.validateOutput(l("abc", "defghi"));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum length of value not met.", ()->s.validateOutput(l("a", "b")));
	}
}
