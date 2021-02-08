// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.junit.*;

/**
 * Tests the ContextCache class.
 */
@FixMethodOrder(NAME_ASCENDING)
public class ContextCacheTest {

	//-------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-------------------------------------------------------------------------------------------------------------------

	@Test
	public void testBasic() {

		ContextPropertiesBuilder cpb = ContextProperties.create();
		ContextProperties cp = cpb.build();

		A a = ContextCache.INSTANCE.create(A.class, cp);
		B b = ContextCache.INSTANCE.create(B.class, cp);
		C c = ContextCache.INSTANCE.create(C.class, cp);

		assertObject(a).asJson().is("{f1:'xxx'}");
		assertObject(b).asJson().is("{f1:'xxx',f2:-1}");
		assertObject(c).asJson().is("{f1:'xxx',f2:-1,f3:false}");

		A a2 = ContextCache.INSTANCE.create(A.class, cp);
		B b2 = ContextCache.INSTANCE.create(B.class, cp);
		C c2 = ContextCache.INSTANCE.create(C.class, cp);

		assertTrue(a == a2);
		assertTrue(b == b2);
		assertTrue(c == c2);

		cpb.set("A.f1", "foo");
		cp = cpb.build();

		a2 = ContextCache.INSTANCE.create(A.class, cp);
		b2 = ContextCache.INSTANCE.create(B.class, cp);
		c2 = ContextCache.INSTANCE.create(C.class, cp);

		assertObject(a2).asJson().is("{f1:'foo'}");
		assertObject(b2).asJson().is("{f1:'foo',f2:-1}");
		assertObject(c2).asJson().is("{f1:'foo',f2:-1,f3:false}");

		assertTrue(a != a2);
		assertTrue(b != b2);
		assertTrue(c != c2);

		a = a2; b = b2; c = c2;

		cp = cpb.set("B.f2.i", 123).build();

		a2 = ContextCache.INSTANCE.create(A.class, cp);
		b2 = ContextCache.INSTANCE.create(B.class, cp);
		c2 = ContextCache.INSTANCE.create(C.class, cp);

		assertObject(a2).asJson().is("{f1:'foo'}");
		assertObject(b2).asJson().is("{f1:'foo',f2:123}");
		assertObject(c2).asJson().is("{f1:'foo',f2:123,f3:false}");

		assertTrue(a == a2);
		assertTrue(b != b2);
		assertTrue(c != c2);

		a = a2; b = b2; c = c2;

		cp = cpb.set("C.f3.b").build();

		a2 = ContextCache.INSTANCE.create(A.class, cp);
		b2 = ContextCache.INSTANCE.create(B.class, cp);
		c2 = ContextCache.INSTANCE.create(C.class, cp);

		assertObject(a2).asJson().is("{f1:'foo'}");
		assertObject(b2).asJson().is("{f1:'foo',f2:123}");
		assertObject(c2).asJson().is("{f1:'foo',f2:123,f3:true}");

		assertTrue(a == a2);
		assertTrue(b == b2);
		assertTrue(c != c2);

		a = a2; b = b2; c = c2;

		cp = cpb.set("D.bad.o", "xxx").build();

		a2 = ContextCache.INSTANCE.create(A.class, cp);
		b2 = ContextCache.INSTANCE.create(B.class, cp);
		c2 = ContextCache.INSTANCE.create(C.class, cp);

		assertObject(a2).asJson().is("{f1:'foo'}");
		assertObject(b2).asJson().is("{f1:'foo',f2:123}");
		assertObject(c2).asJson().is("{f1:'foo',f2:123,f3:true}");

		assertTrue(a == a2);
		assertTrue(b == b2);
		assertTrue(c == c2);

		assertTrue(a.getContextProperties() == a2.getContextProperties());
		assertTrue(b.getContextProperties() == b2.getContextProperties());
		assertTrue(c.getContextProperties() == c2.getContextProperties());

		a2 = ContextCache.INSTANCE.create(A.class, a.getContextProperties().builder().set("A.f1", "foo").build());
		assertTrue(a == a2);

		a2 = ContextCache.INSTANCE.create(A.class, a.getContextProperties().builder().set("A.f1", "bar").build());
		assertTrue(a != a2);
	}

	@ConfigurableContext
	public static class A extends Context {
		public final String f1;

		public A(ContextProperties cp) {
			super(cp, true);
			f1 = getContextProperties().getString("A.f1").orElse("xxx");
		}

		@Override
		public Session createSession(SessionArgs args) {
			return null;
		}

		@Override
		public SessionArgs createDefaultSessionArgs() {
			return null;
		}

		@Override
		public OMap toMap() {
			return OMap.of("f1", f1);
		}
	}

	@ConfigurableContext
	public static class B extends A {
		public int f2;

		public B(ContextProperties cp) {
			super(cp);
			f2 = getContextProperties().getInteger("B.f2.i").orElse(-1);

		}

		@Override
		public OMap toMap() {
			return super.toMap().a("f2", f2);
		}
	}

	@ConfigurableContext
	public static class C extends B {
		public boolean f3;
		public C(ContextProperties cp) {
			super(cp);
			f3 = getContextProperties().getBoolean("C.f3.b").orElse(false);
		}

		@Override
		public OMap toMap() {
			return super.toMap().a("f3", f3);
		}
	}

	@Test
	public void testBadConstructor() {
		ContextPropertiesBuilder cpb = ContextProperties.create();
		ContextProperties cp = cpb.build();
		assertThrown(()->ContextCache.INSTANCE.create(D1.class, cp)).is("Could not create instance of class 'org.apache.juneau.ContextCacheTest$D1'");
		assertThrown(()->ContextCache.INSTANCE.create(D2.class, cp)).is("Could not create instance of class 'org.apache.juneau.ContextCacheTest$D2'");
	}

	public static class D1 extends A {
		protected D1(ContextProperties cp) {
			super(cp);
		}
	}

	public static class D2 extends A {
		public D2(ContextProperties cp) {
			super(cp);
			throw new RuntimeException("Error!");
		}
	}
}
