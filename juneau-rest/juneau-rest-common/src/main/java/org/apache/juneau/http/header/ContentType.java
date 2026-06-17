/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at
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

import java.util.function.*;

import org.apache.juneau.commons.http.*;

/**
 * Represents an HTTP <c>Content-Type</c> header.
 *
 * @since 9.2.1
 */
public class ContentType extends HttpMediaTypeHeader {

	public static final String NAME = "Content-Type";

	// application/*
	/** Content-Type for {@code application/atom+xml}. */
	public static final ContentType APPLICATION_ATOM_XML = new ContentType("application/atom+xml");
	/** Content-Type for {@code application/bson} (Juneau {@code BsonSerializer}/{@code BsonParser}). */
	public static final ContentType APPLICATION_BSON = new ContentType("application/bson");
	/** Content-Type for {@code application/cbor} (Juneau {@code CborSerializer}/{@code CborParser}). */
	public static final ContentType APPLICATION_CBOR = new ContentType("application/cbor");
	/** Content-Type for {@code application/x-www-form-urlencoded}. */
	public static final ContentType APPLICATION_FORM_URLENCODED = new ContentType("application/x-www-form-urlencoded");
	/** Content-Type for {@code application/gzip}. */
	public static final ContentType APPLICATION_GZIP = new ContentType("application/gzip");
	/** Content-Type for {@code application/hal+json} (HAL hypermedia). */
	public static final ContentType APPLICATION_HAL_JSON = new ContentType("application/hal+json");
	/** Content-Type for {@code application/hjson} (Juneau {@code HjsonSerializer}/{@code HjsonParser}). */
	public static final ContentType APPLICATION_HJSON = new ContentType("application/hjson");
	/** Content-Type for {@code application/hocon} (Juneau {@code HoconSerializer}/{@code HoconParser}). */
	public static final ContentType APPLICATION_HOCON = new ContentType("application/hocon");
	/** Content-Type for {@code application/javascript}. */
	public static final ContentType APPLICATION_JAVASCRIPT = new ContentType("application/javascript");
	/** Content-Type for {@code application/jcs+json} (Juneau {@code JcsSerializer}). */
	public static final ContentType APPLICATION_JCS_JSON = new ContentType("application/jcs+json");
	/** Content-Type for {@code application/json}. */
	public static final ContentType APPLICATION_JSON = new ContentType("application/json");
	/** Content-Type for {@code application/json5} (Juneau {@code Json5Serializer}/{@code Json5Parser}). */
	public static final ContentType APPLICATION_JSON5 = new ContentType("application/json5");
	/** Content-Type for {@code application/json5l} (Juneau {@code Json5lSerializer}/{@code Json5lParser}). */
	public static final ContentType APPLICATION_JSON5L = new ContentType("application/json5l");
	/** Content-Type for {@code application/jsonl} (Juneau {@code JsonlSerializer}/{@code JsonlParser}). */
	public static final ContentType APPLICATION_JSONL = new ContentType("application/jsonl");
	/** Content-Type for {@code application/json-patch+json} (RFC 6902). */
	public static final ContentType APPLICATION_JSON_PATCH = new ContentType("application/json-patch+json");
	/** Content-Type for {@code application/ld+json} (JSON-LD). */
	public static final ContentType APPLICATION_LD_JSON = new ContentType("application/ld+json");
	/** Content-Type for {@code application/manifest+json} (W3C web app manifest). */
	public static final ContentType APPLICATION_MANIFEST_JSON = new ContentType("application/manifest+json");
	/** Content-Type for {@code application/merge-patch+json} (RFC 7396). */
	public static final ContentType APPLICATION_MERGE_PATCH = new ContentType("application/merge-patch+json");
	/** Content-Type for {@code application/msgpack} (RFC-style MessagePack). */
	public static final ContentType APPLICATION_MSGPACK = new ContentType("application/msgpack");
	/** Content-Type for {@code application/n-quads} (RDF N-Quads). */
	public static final ContentType APPLICATION_N_QUADS = new ContentType("application/n-quads");
	/** Content-Type for {@code application/x-ndjson} (newline-delimited JSON; alias for JSONL). */
	public static final ContentType APPLICATION_NDJSON = new ContentType("application/x-ndjson");
	/** Content-Type for {@code application/octet-stream}. */
	public static final ContentType APPLICATION_OCTET_STREAM = new ContentType("application/octet-stream");
	/** Content-Type for {@code application/vnd.apache.parquet} (Juneau {@code ParquetSerializer}/{@code ParquetParser}). */
	public static final ContentType APPLICATION_PARQUET = new ContentType("application/vnd.apache.parquet");
	/** Content-Type for {@code application/pdf}. */
	public static final ContentType APPLICATION_PDF = new ContentType("application/pdf");
	/** Content-Type for {@code application/problem+json} (RFC 7807 problem details). */
	public static final ContentType APPLICATION_PROBLEM_JSON = new ContentType("application/problem+json");
	/** Content-Type for {@code application/problem+xml} (RFC 7807 problem details). */
	public static final ContentType APPLICATION_PROBLEM_XML = new ContentType("application/problem+xml");
	/** Content-Type for {@code application/vnd.apache.protobuf} (Juneau {@code RdfProtoSerializer}/{@code RdfProtoParser}). */
	public static final ContentType APPLICATION_PROTOBUF_BINARY = new ContentType("application/vnd.apache.protobuf");
	/** Content-Type for {@code application/rdf+json}. */
	public static final ContentType APPLICATION_RDF_JSON = new ContentType("application/rdf+json");
	/** Content-Type for {@code application/soap+xml}. */
	public static final ContentType APPLICATION_SOAP_XML = new ContentType("application/soap+xml");
	/** Content-Type for {@code application/svg+xml}. */
	public static final ContentType APPLICATION_SVG_XML = new ContentType("application/svg+xml");
	/** Content-Type for {@code application/vnd.apache.thrift.binary} (Juneau {@code RdfThriftSerializer}/{@code RdfThriftParser}). */
	public static final ContentType APPLICATION_THRIFT_BINARY = new ContentType("application/vnd.apache.thrift.binary");
	/** Content-Type for {@code application/toml} (Juneau {@code TomlSerializer}/{@code TomlParser}). */
	public static final ContentType APPLICATION_TOML = new ContentType("application/toml");
	/** Content-Type for {@code application/trig} (RDF TriG). */
	public static final ContentType APPLICATION_TRIG = new ContentType("application/trig");
	/** Content-Type for {@code application/trix+xml} (RDF TriX). */
	public static final ContentType APPLICATION_TRIX_XML = new ContentType("application/trix+xml");
	/** Content-Type for {@code application/vnd.api+json} (JSON:API). */
	public static final ContentType APPLICATION_VND_API_JSON = new ContentType("application/vnd.api+json");
	/** Content-Type for {@code application/wasm} (WebAssembly). */
	public static final ContentType APPLICATION_WASM = new ContentType("application/wasm");
	/** Content-Type for {@code application/xhtml+xml}. */
	public static final ContentType APPLICATION_XHTML_XML = new ContentType("application/xhtml+xml");
	/** Content-Type for {@code application/xml}. */
	public static final ContentType APPLICATION_XML = new ContentType("application/xml");
	/** Content-Type for {@code application/yaml} (Juneau {@code YamlSerializer}/{@code YamlParser}). */
	public static final ContentType APPLICATION_YAML = new ContentType("application/yaml");
	/** Content-Type for {@code application/zip}. */
	public static final ContentType APPLICATION_ZIP = new ContentType("application/zip");

	// image/*
	/** Content-Type for {@code image/avif}. */
	public static final ContentType IMAGE_AVIF = new ContentType("image/avif");
	/** Content-Type for {@code image/bmp}. */
	public static final ContentType IMAGE_BMP = new ContentType("image/bmp");
	/** Content-Type for {@code image/gif}. */
	public static final ContentType IMAGE_GIF = new ContentType("image/gif");
	/** Content-Type for {@code image/heic}. */
	public static final ContentType IMAGE_HEIC = new ContentType("image/heic");
	/** Content-Type for {@code image/heif}. */
	public static final ContentType IMAGE_HEIF = new ContentType("image/heif");
	/** Content-Type for {@code image/x-icon} (favicons). */
	public static final ContentType IMAGE_ICON = new ContentType("image/x-icon");
	/** Content-Type for {@code image/jpeg}. */
	public static final ContentType IMAGE_JPEG = new ContentType("image/jpeg");
	/** Content-Type for {@code image/png}. */
	public static final ContentType IMAGE_PNG = new ContentType("image/png");
	/** Content-Type for {@code image/svg+xml}. */
	public static final ContentType IMAGE_SVG = new ContentType("image/svg+xml");
	/** Content-Type for {@code image/tiff}. */
	public static final ContentType IMAGE_TIFF = new ContentType("image/tiff");
	/** Content-Type for {@code image/webp}. */
	public static final ContentType IMAGE_WEBP = new ContentType("image/webp");

	// multipart/*
	/** Content-Type for {@code multipart/alternative} (RFC 2046). */
	public static final ContentType MULTIPART_ALTERNATIVE = new ContentType("multipart/alternative");
	/** Content-Type for {@code multipart/byteranges} (RFC 7233). */
	public static final ContentType MULTIPART_BYTERANGES = new ContentType("multipart/byteranges");
	/** Content-Type for {@code multipart/form-data}. */
	public static final ContentType MULTIPART_FORM_DATA = new ContentType("multipart/form-data");
	/** Content-Type for {@code multipart/mixed} (RFC 2046). */
	public static final ContentType MULTIPART_MIXED = new ContentType("multipart/mixed");
	/** Content-Type for {@code multipart/related} (RFC 2387). */
	public static final ContentType MULTIPART_RELATED = new ContentType("multipart/related");

	// text/*
	/** Content-Type for {@code text/calendar} (iCalendar). */
	public static final ContentType TEXT_CALENDAR = new ContentType("text/calendar");
	/** Content-Type for {@code text/css}. */
	public static final ContentType TEXT_CSS = new ContentType("text/css");
	/** Content-Type for {@code text/csv} (Juneau {@code CsvSerializer}/{@code CsvParser}). */
	public static final ContentType TEXT_CSV = new ContentType("text/csv");
	/** Content-Type for {@code text/event-stream} (Server-Sent Events). */
	public static final ContentType TEXT_EVENT_STREAM = new ContentType("text/event-stream");
	/** Content-Type for {@code text/html}. */
	public static final ContentType TEXT_HTML = new ContentType("text/html");
	/** Content-Type for {@code text/html+stripped} (Juneau {@code HtmlStrippedDocSerializer}). */
	public static final ContentType TEXT_HTML_STRIPPED = new ContentType("text/html+stripped");
	/** Content-Type for {@code text/ini} (Juneau {@code IniSerializer}/{@code IniParser}). */
	public static final ContentType TEXT_INI = new ContentType("text/ini");
	/** Content-Type for {@code text/javascript} (preferred per RFC 9239). */
	public static final ContentType TEXT_JAVASCRIPT = new ContentType("text/javascript");
	/** Content-Type for {@code text/json} (Juneau {@code JsonParser} alias). */
	public static final ContentType TEXT_JSON = new ContentType("text/json");
	/** Content-Type for {@code text/markdown} (Juneau {@code MarkdownSerializer}). */
	public static final ContentType TEXT_MARKDOWN = new ContentType("text/markdown");
	/** Content-Type for {@code text/n3} (RDF N3). */
	public static final ContentType TEXT_N3 = new ContentType("text/n3");
	/** Content-Type for {@code text/n-triple} (RDF N-Triples). */
	public static final ContentType TEXT_N_TRIPLE = new ContentType("text/n-triple");
	/** Content-Type for {@code text/openapi}. */
	public static final ContentType TEXT_OPENAPI = new ContentType("text/openapi");
	/** Content-Type for {@code text/plain}. */
	public static final ContentType TEXT_PLAIN = new ContentType("text/plain");
	/** Content-Type for {@code text/protobuf} (Juneau {@code PrototextSerializer}/{@code PrototextParser}). */
	public static final ContentType TEXT_PROTOBUF = new ContentType("text/protobuf");
	/** Content-Type for {@code text/turtle} (RDF Turtle). */
	public static final ContentType TEXT_TURTLE = new ContentType("text/turtle");
	/** Content-Type for {@code text/uon} (Juneau {@code UonSerializer}/{@code UonParser}). */
	public static final ContentType TEXT_UON = new ContentType("text/uon");
	/** Content-Type for {@code text/xml}. */
	public static final ContentType TEXT_XML = new ContentType("text/xml");
	/** Content-Type for {@code text/xml+rdf} (RDF/XML). */
	public static final ContentType TEXT_XML_RDF = new ContentType("text/xml+rdf");
	/** Content-Type for {@code text/yaml} (Juneau {@code YamlParser} alias). */
	public static final ContentType TEXT_YAML = new ContentType("text/yaml");

	// Special
	/** Content-Type for <c>*&#47;*</c> (all media types). */
	public static final ContentType WILDCARD = new ContentType("*/*");
	/** Reusable {@code Content-Type} header with a {@code null} value. */
	public static final ContentType NULL = new ContentType((String)null);

	public ContentType(String value) {
		super(NAME, value);
	}

	public ContentType(MediaType value) {
		super(NAME, value);
	}

	private ContentType(Supplier<?> supplier, int lazyMode) {
		super(NAME, supplier, lazyMode);
	}

	public static ContentType of(String value) {
		return new ContentType(value);
	}

	public static ContentType of(MediaType value) {
		return new ContentType(value);
	}

	public static ContentType ofLazyWire(Supplier<String> supplier) {
		return new ContentType(supplier, LAZY_WIRE_STRING);
	}

	public static ContentType ofLazyParsed(Supplier<MediaType> supplier) {
		return new ContentType(supplier, LAZY_MEDIA_TYPE);
	}
}
