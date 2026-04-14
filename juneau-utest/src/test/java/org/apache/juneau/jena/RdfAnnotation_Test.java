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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.jena.annotation.*;
import org.junit.jupiter.api.*;

class RdfAnnotation_Test extends TestBase {

	@Test void a01_defaults() {
		var x = RdfAnnotation.create().build();
		assertFalse(x.beanUri());
		assertEquals(RdfCollectionFormat.DEFAULT, x.collectionFormat());
		assertEquals("", x.prefix());
		assertEquals("", x.namespace());
	}

	@Test void a02_beanUri() {
		var x = RdfAnnotation.create().beanUri(true).build();
		assertTrue(x.beanUri());
	}

	@Test void a03_beanUri_false() {
		var x = RdfAnnotation.create().beanUri(false).build();
		assertFalse(x.beanUri());
	}

	@Test void a04_collectionFormat_bag() {
		var x = RdfAnnotation.create().collectionFormat(RdfCollectionFormat.BAG).build();
		assertEquals(RdfCollectionFormat.BAG, x.collectionFormat());
	}

	@Test void a05_collectionFormat_list() {
		var x = RdfAnnotation.create().collectionFormat(RdfCollectionFormat.LIST).build();
		assertEquals(RdfCollectionFormat.LIST, x.collectionFormat());
	}

	@Test void a06_collectionFormat_seq() {
		var x = RdfAnnotation.create().collectionFormat(RdfCollectionFormat.SEQ).build();
		assertEquals(RdfCollectionFormat.SEQ, x.collectionFormat());
	}

	@Test void a07_namespace() {
		var x = RdfAnnotation.create().namespace("http://foo/").build();
		assertEquals("http://foo/", x.namespace());
	}

	@Test void a08_prefix() {
		var x = RdfAnnotation.create().prefix("foo").build();
		assertEquals("foo", x.prefix());
	}

	@Test void a13_prefix_and_namespace() {
		var x = RdfAnnotation.create().prefix("foo").namespace("http://foo/").build();
		assertEquals("foo", x.prefix());
		assertEquals("http://foo/", x.namespace());
	}

	@Test void a16_default_constant() {
		assertNotNull(RdfAnnotation.DEFAULT);
		assertFalse(RdfAnnotation.DEFAULT.beanUri());
		assertEquals(RdfCollectionFormat.DEFAULT, RdfAnnotation.DEFAULT.collectionFormat());
	}
}
