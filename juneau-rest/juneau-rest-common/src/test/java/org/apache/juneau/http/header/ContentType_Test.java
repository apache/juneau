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
package org.apache.juneau.http.header;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.http.MediaType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Tests for {@link ContentType}.
 *
 * <p>
 * Verifies the well-known {@code Content-Type} constants exposed by the class, including the Juneau marshalling formats
 * (CBOR, BSON, YAML, HJSON, HOCON, JSON5, JSONL, etc.), common web types (CSS, JavaScript, PDF, ZIP, SSE, etc.), REST
 * API conventions (RFC 7807 {@code problem+json}, RFC 6902/7396 patch types, HAL, JSON:API), and multipart variants.
 */
class ContentType_Test extends TestBase {

	@Test void a01_name() {
		assertEquals("Content-Type", ContentType.NAME);
		assertEquals("Content-Type", ContentType.APPLICATION_JSON.getName());
	}

	@Test void a02_nullConstant() {
		assertNull(ContentType.NULL.getValue());
	}

	@Test void a03_factories() {
		assertEquals("application/json", ContentType.of("application/json").getValue());
		assertEquals("application/json", ContentType.of(MediaType.of("application/json")).getValue());
		assertEquals("application/json", ContentType.ofLazyWire(() -> "application/json").getValue());
		assertEquals("application/json", ContentType.ofLazyParsed(() -> MediaType.of("application/json")).getValue());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Constants — parametric value check
	//------------------------------------------------------------------------------------------------------------------

	static Stream<Arguments> b01_constants() {
		return Stream.of(
			// application/*
			Arguments.of("APPLICATION_ATOM_XML", ContentType.APPLICATION_ATOM_XML, "application/atom+xml"),
			Arguments.of("APPLICATION_BSON", ContentType.APPLICATION_BSON, "application/bson"),
			Arguments.of("APPLICATION_CBOR", ContentType.APPLICATION_CBOR, "application/cbor"),
			Arguments.of("APPLICATION_FORM_URLENCODED", ContentType.APPLICATION_FORM_URLENCODED, "application/x-www-form-urlencoded"),
			Arguments.of("APPLICATION_GZIP", ContentType.APPLICATION_GZIP, "application/gzip"),
			Arguments.of("APPLICATION_HAL_JSON", ContentType.APPLICATION_HAL_JSON, "application/hal+json"),
			Arguments.of("APPLICATION_HJSON", ContentType.APPLICATION_HJSON, "application/hjson"),
			Arguments.of("APPLICATION_HOCON", ContentType.APPLICATION_HOCON, "application/hocon"),
			Arguments.of("APPLICATION_JAVASCRIPT", ContentType.APPLICATION_JAVASCRIPT, "application/javascript"),
			Arguments.of("APPLICATION_JCS_JSON", ContentType.APPLICATION_JCS_JSON, "application/jcs+json"),
			Arguments.of("APPLICATION_JSON", ContentType.APPLICATION_JSON, "application/json"),
			Arguments.of("APPLICATION_JSON5", ContentType.APPLICATION_JSON5, "application/json5"),
			Arguments.of("APPLICATION_JSON5L", ContentType.APPLICATION_JSON5L, "application/json5l"),
			Arguments.of("APPLICATION_JSONL", ContentType.APPLICATION_JSONL, "application/jsonl"),
			Arguments.of("APPLICATION_JSON_PATCH", ContentType.APPLICATION_JSON_PATCH, "application/json-patch+json"),
			Arguments.of("APPLICATION_LD_JSON", ContentType.APPLICATION_LD_JSON, "application/ld+json"),
			Arguments.of("APPLICATION_MANIFEST_JSON", ContentType.APPLICATION_MANIFEST_JSON, "application/manifest+json"),
			Arguments.of("APPLICATION_MERGE_PATCH", ContentType.APPLICATION_MERGE_PATCH, "application/merge-patch+json"),
			Arguments.of("APPLICATION_MSGPACK", ContentType.APPLICATION_MSGPACK, "application/msgpack"),
			Arguments.of("APPLICATION_N_QUADS", ContentType.APPLICATION_N_QUADS, "application/n-quads"),
			Arguments.of("APPLICATION_NDJSON", ContentType.APPLICATION_NDJSON, "application/x-ndjson"),
			Arguments.of("APPLICATION_OCTET_STREAM", ContentType.APPLICATION_OCTET_STREAM, "application/octet-stream"),
			Arguments.of("APPLICATION_PARQUET", ContentType.APPLICATION_PARQUET, "application/vnd.apache.parquet"),
			Arguments.of("APPLICATION_PDF", ContentType.APPLICATION_PDF, "application/pdf"),
			Arguments.of("APPLICATION_PROBLEM_JSON", ContentType.APPLICATION_PROBLEM_JSON, "application/problem+json"),
			Arguments.of("APPLICATION_PROBLEM_XML", ContentType.APPLICATION_PROBLEM_XML, "application/problem+xml"),
			Arguments.of("APPLICATION_PROTOBUF_BINARY", ContentType.APPLICATION_PROTOBUF_BINARY, "application/vnd.apache.protobuf"),
			Arguments.of("APPLICATION_RDF_JSON", ContentType.APPLICATION_RDF_JSON, "application/rdf+json"),
			Arguments.of("APPLICATION_SOAP_XML", ContentType.APPLICATION_SOAP_XML, "application/soap+xml"),
			Arguments.of("APPLICATION_SVG_XML", ContentType.APPLICATION_SVG_XML, "application/svg+xml"),
			Arguments.of("APPLICATION_THRIFT_BINARY", ContentType.APPLICATION_THRIFT_BINARY, "application/vnd.apache.thrift.binary"),
			Arguments.of("APPLICATION_TOML", ContentType.APPLICATION_TOML, "application/toml"),
			Arguments.of("APPLICATION_TRIG", ContentType.APPLICATION_TRIG, "application/trig"),
			Arguments.of("APPLICATION_TRIX_XML", ContentType.APPLICATION_TRIX_XML, "application/trix+xml"),
			Arguments.of("APPLICATION_VND_API_JSON", ContentType.APPLICATION_VND_API_JSON, "application/vnd.api+json"),
			Arguments.of("APPLICATION_WASM", ContentType.APPLICATION_WASM, "application/wasm"),
			Arguments.of("APPLICATION_XHTML_XML", ContentType.APPLICATION_XHTML_XML, "application/xhtml+xml"),
			Arguments.of("APPLICATION_XML", ContentType.APPLICATION_XML, "application/xml"),
			Arguments.of("APPLICATION_YAML", ContentType.APPLICATION_YAML, "application/yaml"),
			Arguments.of("APPLICATION_ZIP", ContentType.APPLICATION_ZIP, "application/zip"),

			// image/*
			Arguments.of("IMAGE_AVIF", ContentType.IMAGE_AVIF, "image/avif"),
			Arguments.of("IMAGE_BMP", ContentType.IMAGE_BMP, "image/bmp"),
			Arguments.of("IMAGE_GIF", ContentType.IMAGE_GIF, "image/gif"),
			Arguments.of("IMAGE_HEIC", ContentType.IMAGE_HEIC, "image/heic"),
			Arguments.of("IMAGE_HEIF", ContentType.IMAGE_HEIF, "image/heif"),
			Arguments.of("IMAGE_ICON", ContentType.IMAGE_ICON, "image/x-icon"),
			Arguments.of("IMAGE_JPEG", ContentType.IMAGE_JPEG, "image/jpeg"),
			Arguments.of("IMAGE_PNG", ContentType.IMAGE_PNG, "image/png"),
			Arguments.of("IMAGE_SVG", ContentType.IMAGE_SVG, "image/svg+xml"),
			Arguments.of("IMAGE_TIFF", ContentType.IMAGE_TIFF, "image/tiff"),
			Arguments.of("IMAGE_WEBP", ContentType.IMAGE_WEBP, "image/webp"),

			// multipart/*
			Arguments.of("MULTIPART_ALTERNATIVE", ContentType.MULTIPART_ALTERNATIVE, "multipart/alternative"),
			Arguments.of("MULTIPART_BYTERANGES", ContentType.MULTIPART_BYTERANGES, "multipart/byteranges"),
			Arguments.of("MULTIPART_FORM_DATA", ContentType.MULTIPART_FORM_DATA, "multipart/form-data"),
			Arguments.of("MULTIPART_MIXED", ContentType.MULTIPART_MIXED, "multipart/mixed"),
			Arguments.of("MULTIPART_RELATED", ContentType.MULTIPART_RELATED, "multipart/related"),

			// text/*
			Arguments.of("TEXT_CALENDAR", ContentType.TEXT_CALENDAR, "text/calendar"),
			Arguments.of("TEXT_CSS", ContentType.TEXT_CSS, "text/css"),
			Arguments.of("TEXT_CSV", ContentType.TEXT_CSV, "text/csv"),
			Arguments.of("TEXT_EVENT_STREAM", ContentType.TEXT_EVENT_STREAM, "text/event-stream"),
			Arguments.of("TEXT_HTML", ContentType.TEXT_HTML, "text/html"),
			Arguments.of("TEXT_HTML_STRIPPED", ContentType.TEXT_HTML_STRIPPED, "text/html+stripped"),
			Arguments.of("TEXT_INI", ContentType.TEXT_INI, "text/ini"),
			Arguments.of("TEXT_JAVASCRIPT", ContentType.TEXT_JAVASCRIPT, "text/javascript"),
			Arguments.of("TEXT_JSON", ContentType.TEXT_JSON, "text/json"),
			Arguments.of("TEXT_MARKDOWN", ContentType.TEXT_MARKDOWN, "text/markdown"),
			Arguments.of("TEXT_N3", ContentType.TEXT_N3, "text/n3"),
			Arguments.of("TEXT_N_TRIPLE", ContentType.TEXT_N_TRIPLE, "text/n-triple"),
			Arguments.of("TEXT_OPENAPI", ContentType.TEXT_OPENAPI, "text/openapi"),
			Arguments.of("TEXT_PLAIN", ContentType.TEXT_PLAIN, "text/plain"),
			Arguments.of("TEXT_PROTOBUF", ContentType.TEXT_PROTOBUF, "text/protobuf"),
			Arguments.of("TEXT_TURTLE", ContentType.TEXT_TURTLE, "text/turtle"),
			Arguments.of("TEXT_UON", ContentType.TEXT_UON, "text/uon"),
			Arguments.of("TEXT_XML", ContentType.TEXT_XML, "text/xml"),
			Arguments.of("TEXT_XML_RDF", ContentType.TEXT_XML_RDF, "text/xml+rdf"),
			Arguments.of("TEXT_YAML", ContentType.TEXT_YAML, "text/yaml"),

			// Special
			Arguments.of("WILDCARD", ContentType.WILDCARD, "*/*")
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource
	void b01_constants(String name, ContentType ct, String expected) {
		assertNotNull(ct, name + " is null");
		assertEquals(expected, ct.getValue(), name + " value mismatch");
		assertEquals("Content-Type", ct.getName(), name + " header name mismatch");
	}

	@Test void c01_noDuplicateMimeValues() {
		var seen = new HashSet<String>();
		var dupes = new ArrayList<String>();
		b01_constants().forEach(args -> {
			var v = (String) args.get()[2];
			if (! seen.add(v))
				dupes.add(v);
		});
		assertTrue(dupes.isEmpty(), () -> "Duplicate Content-Type values: " + dupes);
	}
}
