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
package org.apache.juneau.jena;

import static java.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.xml.*;
import org.junit.jupiter.api.*;

class RdfUtils_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Helper factory methods
	//-----------------------------------------------------------------------------------------------------------------

	private static Rdf rdf(String prefix, String namespace) {
		return RdfAnnotation.create().prefix(prefix).namespace(namespace).build();
	}

	private static Rdf rdfPrefix(String prefix) {
		return RdfAnnotation.create().prefix(prefix).build();
	}

	private static Rdf rdfNamespace(String namespace) {
		return RdfAnnotation.create().namespace(namespace).build();
	}

	private static RdfSchema schema(String prefix, String namespace, RdfNs... rdfNs) {
		return (RdfSchema) Proxy.newProxyInstance(
			RdfSchema.class.getClassLoader(),
			new Class<?>[]{ RdfSchema.class },
			(proxy, method, args) -> switch (method.getName()) {
				case "prefix" -> prefix;
				case "namespace" -> namespace;
				case "rdfNs" -> rdfNs;
				case "annotationType" -> RdfSchema.class;
				default -> null;
			}
		);
	}

	private static RdfNs rdfNs(String prefix, String namespaceURI) {
		return (RdfNs) Proxy.newProxyInstance(
			RdfNs.class.getClassLoader(),
			new Class<?>[]{ RdfNs.class },
			(proxy, method, args) -> switch (method.getName()) {
				case "prefix" -> prefix;
				case "namespaceURI" -> namespaceURI;
				case "annotationType" -> RdfNs.class;
				default -> null;
			}
		);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_emptyLists_returnsNull() {
		assertNull(RdfUtils.findNamespace(emptyList(), emptyList()));
	}

	@Test void a02_rdf_bothPrefixAndNamespace() {
		var result = RdfUtils.findNamespace(
			List.of(rdf("foo", "http://foo/")),
			emptyList()
		);
		assertEquals(Namespace.of("foo", "http://foo/"), result);
	}

	@Test void a03_rdf_onlyPrefix_foundInOtherRdf() {
		var result = RdfUtils.findNamespace(
			List.of(rdfPrefix("foo"), rdf("foo", "http://foo/")),
			emptyList()
		);
		assertEquals(Namespace.of("foo", "http://foo/"), result);
	}

	@Test void a04_rdf_onlyPrefix_foundInSchema() {
		var result = RdfUtils.findNamespace(
			List.of(rdfPrefix("foo")),
			List.of(schema("foo", "http://foo/"))
		);
		assertEquals(Namespace.of("foo", "http://foo/"), result);
	}

	@Test void a05_rdf_onlyPrefix_foundViaRdfNs() {
		var result = RdfUtils.findNamespace(
			List.of(rdfPrefix("foo")),
			List.of(schema("", "", rdfNs("foo", "http://foo/")))
		);
		assertEquals(Namespace.of("foo", "http://foo/"), result);
	}

	@Test void a06_rdf_onlyPrefix_notFound_throws() {
		assertThrows(Exception.class, () ->
			RdfUtils.findNamespace(
				List.of(rdfPrefix("foo")),
				emptyList()
			)
		);
	}

	@Test void a07_rdf_onlyNamespace_foundInOtherRdf() {
		var result = RdfUtils.findNamespace(
			List.of(rdfNamespace("http://foo/"), rdf("bar", "http://foo/")),
			emptyList()
		);
		assertEquals(Namespace.of("bar", "http://foo/"), result);
	}

	@Test void a08_rdf_onlyNamespace_foundInSchema() {
		var result = RdfUtils.findNamespace(
			List.of(rdfNamespace("http://foo/")),
			List.of(schema("bar", "http://foo/"))
		);
		assertEquals(Namespace.of("bar", "http://foo/"), result);
	}

	@Test void a09_rdf_onlyNamespace_foundViaRdfNs() {
		var result = RdfUtils.findNamespace(
			List.of(rdfNamespace("http://foo/")),
			List.of(schema("", "", rdfNs("baz", "http://foo/")))
		);
		assertEquals(Namespace.of("baz", "http://foo/"), result);
	}

	@Test void a10_rdf_onlyNamespace_notFound_returnsNull() {
		var result = RdfUtils.findNamespace(
			List.of(rdfNamespace("http://foo/")),
			emptyList()
		);
		assertNull(result);
	}

	@Test void a11_schema_bothPrefixAndNamespace() {
		var result = RdfUtils.findNamespace(
			emptyList(),
			List.of(schema("s", "http://s/"))
		);
		assertEquals(Namespace.of("s", "http://s/"), result);
	}

	@Test void a12_schema_onlyPrefix_foundViaRdfNs() {
		var result = RdfUtils.findNamespace(
			emptyList(),
			List.of(schema("s", "", rdfNs("s", "http://s/")))
		);
		assertEquals(Namespace.of("s", "http://s/"), result);
	}

	@Test void a13_schema_neitherSpecified_returnsNull() {
		var result = RdfUtils.findNamespace(
			emptyList(),
			List.of(schema("", ""))
		);
		assertNull(result);
	}

	@Test void a14_rdf_onlyPrefix_nonMatchingRdfScanned() {
		// Covers the A=false branch at line ~74: when prefix doesn't match another rdf2
		var result = RdfUtils.findNamespace(
			List.of(rdfPrefix("foo"), rdfPrefix("bar")),
			List.of(schema("foo", "http://foo/"))
		);
		assertEquals(Namespace.of("foo", "http://foo/"), result);
	}

	@Test void a15_rdf_onlyPrefix_foundViaRdfNs_nonMatchingFirst() {
		// Covers the false branch when rdfNs.prefix() doesn't match before finding one that does
		var result = RdfUtils.findNamespace(
			List.of(rdfPrefix("foo")),
			List.of(schema("", "", rdfNs("other", "http://other/"), rdfNs("foo", "http://foo/")))
		);
		assertEquals(Namespace.of("foo", "http://foo/"), result);
	}

	@Test void a16_rdf_onlyNamespace_nonMatchingRdfScanned() {
		// Covers the A=false branch at line ~90: when namespace doesn't match another rdf2
		var result = RdfUtils.findNamespace(
			List.of(rdfNamespace("http://foo/"), rdf("baz", "http://baz/")),
			List.of(schema("bar", "http://foo/"))
		);
		assertEquals(Namespace.of("bar", "http://foo/"), result);
	}

	@Test void a17_rdf_onlyNamespace_nonMatchingSchemaScanned() {
		// Covers the false branch at line ~93: when schema namespace doesn't match
		var result = RdfUtils.findNamespace(
			List.of(rdfNamespace("http://foo/")),
			List.of(schema("other", "http://other/"), schema("bar", "http://foo/"))
		);
		assertEquals(Namespace.of("bar", "http://foo/"), result);
	}

	@Test void a18_rdf_onlyNamespace_nonMatchingRdfNsScanned() {
		// Covers the false branch at line ~96: when rdfNs.namespaceURI() doesn't match
		var result = RdfUtils.findNamespace(
			List.of(rdfNamespace("http://foo/")),
			List.of(schema("", "", rdfNs("other", "http://other/"), rdfNs("baz", "http://foo/")))
		);
		assertEquals(Namespace.of("baz", "http://foo/"), result);
	}
}
