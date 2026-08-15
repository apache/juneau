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
package org.apache.juneau.rest.server;

import static org.apache.juneau.BasicTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.uon.*;
import org.apache.juneau.rest.server.auth.*;
import org.apache.juneau.rest.server.converter.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the {@link ChildAnnotation} companion (programmatic builder + synthetic impl) and equivalency with
 * the declarative {@link Child @Child} annotation form.
 */
@SuppressWarnings({
	"unchecked" // Generic Class<? extends X>[] varargs in the builder slot setters are populated with single literals in tests.
})
class ChildAnnotation_Test extends TestBase {

	public static class FooChild {}

	private static ChildAnnotation.Builder fullBuilder() {
		return ChildAnnotation.create()
			.type(FooChild.class)
			.guards(BearerTokenGuard.class)
			.roleGuard("admin")
			.rolesDeclared("admin,user")
			.converters(Traversable.class)
			.partSerializer(UonSerializer.class)
			.partParser(UonParser.class)
			.defaultCharset("utf-8")
			.maxInput("1M");
	}

	Child a1 = fullBuilder().build();
	Child a2 = fullBuilder().build();

	@Test void a01_stringAccessors() {
		assertEquals(FooChild.class, a1.type());
		assertEquals("admin", a1.roleGuard());
		assertEquals("admin,user", a1.rolesDeclared());
		assertEquals("utf-8", a1.defaultCharset());
		assertEquals("1M", a1.maxInput());
	}

	@Test void a01b_classAndAnnotationAccessors() {
		assertEquals(BearerTokenGuard.class, a1.guards()[0]);
		assertEquals(Traversable.class, a1.converters()[0]);
		assertEquals(UonSerializer.class, a1.partSerializer());
		assertEquals(UonParser.class, a1.partParser());
	}

	@Test void a02_testEquivalency() {
		assertEquals(a2, a1);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEquals(a1.hashCode(), a2.hashCode());
	}

	@Test void a03_defaultInstance() {
		var d = ChildAnnotation.DEFAULT;
		assertEquals("", d.roleGuard());
		assertEquals(0, d.guards().length);
	}

	// Comparison with the declarative @Child form.

	@Rest(childrenDefs=@Child(
		type=FooChild.class,
		guards=BearerTokenGuard.class,
		roleGuard="admin",
		rolesDeclared="admin,user",
		converters=Traversable.class,
		partSerializer=UonSerializer.class,
		partParser=UonParser.class,
		defaultCharset="utf-8",
		maxInput="1M"
	))
	public static class D1 {}

	@Test void d01_comparisonWithDeclarativeAnnotation() {
		// The builder-produced a1 must be equal+hashCode-equal to the declarative @Child form (same slots).
		var d1 = D1.class.getAnnotationsByType(Rest.class)[0].childrenDefs()[0];
		assertEquals(a1, d1);
		assertEquals(a1.hashCode(), d1.hashCode());
	}
}
