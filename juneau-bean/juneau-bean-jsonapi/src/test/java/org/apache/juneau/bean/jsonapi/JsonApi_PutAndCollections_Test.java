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
package org.apache.juneau.bean.jsonapi;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the lazy-initializing {@code putMeta} / {@code putLink} helpers and the varargs / collection
 * overloads on the {@code juneau-bean-jsonapi} beans.
 */
class JsonApi_PutAndCollections_Test extends TestBase {

	@Test
	void a01_documentPutMeta() {
		var d = new JsonApiDocument().putMeta("a", 1).putMeta("b", 2);
		assertEquals(Map.of("a", 1, "b", 2), d.getMeta());
	}

	@Test
	void a02_documentPutLink() {
		var d = new JsonApiDocument().putLink("self", "/x").putLink("next", new JsonApiLink("/y"));
		assertEquals(2, d.getLinks().size());
		assertEquals("/x", d.getLinks().get("self"));
		assertEquals("/y", ((JsonApiLink)d.getLinks().get("next")).getHref());
	}

	@Test
	void a03_documentSetErrorsVarargs() {
		var d = new JsonApiDocument().setErrors(new JsonApiError().setTitle("x"), new JsonApiError().setTitle("y"));
		assertEquals(2, d.getErrors().size());
		assertBean(d.getErrors().get(1), "title", "y");
	}

	@Test
	void a04_documentSetIncludedVarargs() {
		var d = new JsonApiDocument().setIncluded(new JsonApiResource("a", "1"), new JsonApiResource("a", "2"));
		assertEquals(2, d.getIncluded().size());
		assertBean(d.getIncluded().get(1), "type,id", "a,2");
	}

	@Test
	void a05_documentAddErrorsCollectionInitializesAndAppends() {
		var d = new JsonApiDocument().addErrors(List.of(new JsonApiError().setTitle("a")));
		assertEquals(1, d.getErrors().size());
		d.addErrors(List.of(new JsonApiError().setTitle("b")));
		assertEquals(2, d.getErrors().size());
		assertEquals("b", d.getErrors().get(1).getTitle());
	}

	@Test
	void a06_documentAddIncludedCollectionInitializesAndAppends() {
		var d = new JsonApiDocument().addIncluded(List.of(new JsonApiResource("a", "1")));
		assertEquals(1, d.getIncluded().size());
		d.addIncluded(List.of(new JsonApiResource("a", "2")));
		assertEquals(2, d.getIncluded().size());
		assertEquals("2", d.getIncluded().get(1).getId());
	}

	@Test
	void b01_errorPutLinkAndMeta() {
		var e = new JsonApiError().putLink("about", "/e").putLink("more", "/m").putMeta("a", 1).putMeta("b", 2);
		assertEquals(Map.of("about", "/e", "more", "/m"), e.getLinks());
		assertEquals(Map.of("a", 1, "b", 2), e.getMeta());
	}

	@Test
	void c01_linkPutMeta() {
		var l = new JsonApiLink("/u").putMeta("a", 1).putMeta("b", 2);
		assertEquals(Map.of("a", 1, "b", 2), l.getMeta());
	}

	@Test
	void d01_relationshipPutLinkAndMeta() {
		var r = new JsonApiRelationship().putLink("self", "/s").putLink("related", "/r").putMeta("a", 1).putMeta("b", 2);
		assertEquals(Map.of("self", "/s", "related", "/r"), r.getLinks());
		assertEquals(Map.of("a", 1, "b", 2), r.getMeta());
	}

	@Test
	void e01_resourcePutLinkAndMeta() {
		var r = new JsonApiResource("a", "1").putLink("self", "/s").putLink("o", "/o").putMeta("a", 1).putMeta("b", 2);
		assertEquals(Map.of("self", "/s", "o", "/o"), r.getLinks());
		assertEquals(Map.of("a", 1, "b", 2), r.getMeta());
	}

	@Test
	void f01_resourceIdentifierPutMeta() {
		var r = new JsonApiResourceIdentifier("a", "1").putMeta("a", 1).putMeta("b", 2);
		assertEquals(Map.of("a", 1, "b", 2), r.getMeta());
	}

	@Test
	void g01_versionPutMeta() {
		var v = new JsonApiVersion("1.1").putMeta("a", 1).putMeta("b", 2);
		assertEquals(Map.of("a", 1, "b", 2), v.getMeta());
	}
}
