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
package org.apache.juneau.bson.annotation;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.bson.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link BsonAnnotation} and {@link Bson @Bson} annotation.
 */
class BsonAnnotation_Test extends org.apache.juneau.TestBase {

	Bson a1 = BsonAnnotation.create()
		.description("a")
		.build();

	Bson a2 = BsonAnnotation.create()
		.description("a")
		.build();

	@Test
	void a01_basic() {
		assertBean(a1, "description", "[a]");
	}

	@Test
	void a02_equivalency() {
		assertEquals(a2, a1);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEquals(a1.hashCode(), a2.hashCode());
	}

	@Test
	void b01_equivalencyInPropertyStores() {
		var bc1 = BeanContext.create().annotations(a1).build();
		var bc2 = BeanContext.create().annotations(a2).build();
		assertSame(bc1, bc2);
	}

	@org.apache.juneau.bson.annotation.Bson(description = "a")
	public static class D1 {}

	@org.apache.juneau.bson.annotation.Bson(description = "a")
	public static class D2 {}

	@Test
	void d01_comparisonWithDeclarativeAnnotations() {
		var d1 = D1.class.getAnnotationsByType(org.apache.juneau.bson.annotation.Bson.class)[0];
		var d2 = D2.class.getAnnotationsByType(org.apache.juneau.bson.annotation.Bson.class)[0];
		assertEqualsAll(a1, d1, d2);
		assertEqualsAll(a1.hashCode(), d1.hashCode(), d2.hashCode());
	}

	@BsonConfig(addBeanTypes = "true")
	public static class G1_Bean { public String name = "x"; }

	@Test
	void g01_bsonConfigAddBeanTypes() throws Exception {
		var s = BsonSerializer.create().applyAnnotations(G1_Bean.class).keepNullProperties().build();
		var bytes = s.serialize(new G1_Bean());
		var p = BsonParser.create().build();
		var parsed = p.parse(bytes, JsonMap.class);
		assertTrue(parsed.containsKey("_type") || parsed.containsKey("name"));
		assertEquals("x", parsed.get("name"));
	}

	@BsonConfig(writeDatesAsDatetime = "false")
	public static class G2_Bean { public java.util.Date d = new java.util.Date(1700000000000L); }

	@Test
	void g02_writeDatesAsDatetimeFalse() throws Exception {
		var s = BsonSerializer.create().applyAnnotations(G2_Bean.class).keepNullProperties().build();
		var bytes = s.serialize(new G2_Bean());
		var p = BsonParser.create().build();
		var parsed = p.parse(bytes, JsonMap.class);
		var d = parsed.get("d");
		assertTrue(d instanceof String, "Date should be string when writeDatesAsDatetime=false, got: " + d.getClass().getName());
	}
}
