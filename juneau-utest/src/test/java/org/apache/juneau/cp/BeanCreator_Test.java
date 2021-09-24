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
package org.apache.juneau.cp;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import javax.inject.*;

import static org.apache.juneau.cp.BeanCreator.*;

import org.apache.juneau.annotation.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class BeanCreator_Test {

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests.
	//-----------------------------------------------------------------------------------------------------------------

	public static class A1 {}

	@Test
	public void a01_basic() {
		assertObject(of(A1.class).run()).isNotNull();
		assertObject(of(null).run()).isNull();
	}

	public class A2 {}

	@Test
	public void a02_outer() {
		BeanCreator_Test outer = new BeanCreator_Test();
		assertObject(of(A2.class).outer(outer).run()).isNotNull();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Static creators.
	//-----------------------------------------------------------------------------------------------------------------

	public static class B1 {
		public String a;
		public static B1 create() {
			B1 x = new B1();
			x.a = "foo";
			return x;
		}
	}

	@Test
	public void b01_staticCreator_create() {
		assertString(of(B1.class).run().a).is("foo");

	}

	public static abstract class B2 {
		public String a;
		public static B2 getInstance() {
			B2 x = new B2(){};
			x.a = "foo";
			return x;
		}
	}

	@Test
	public void b02_staticCreator_getInstance() {
		assertString(of(B2.class).run().a).is("foo");
	}

	public static class B3a {
		public String a;
		public static B3a getFoo() {
			B3a x = new B3a();
			x.a = "foo";
			return x;
		}
		protected static B3a create() {
			B3a x = new B3a();
			x.a = "foo";
			return x;
		}
		public static B3a create(String foo) {
			B3a x = new B3a();
			x.a = "foo";
			return x;
		}
		@Deprecated protected static B3a getInstance() {
			B3a x = new B3a();
			x.a = "foo";
			return x;
		}
		protected B3a() {
			a = "bar";
		}
	}

	public static class B3b {
		public String a;
		public B3b create() {
			B3b x = new B3b();
			x.a = "foo";
			return x;
		}
		@BeanIgnore public static B3b getInstance() {
			B3b x = new B3b();
			x.a = "foo";
			return x;
		}
		protected B3b() {
			a = "bar";
		}
	}

	public static class B3c {
		public String a;
		public static String create() {
			return null;
		}
		protected B3c() {
			a = "bar";
		}
	}

	@Test
	public void b03_staticCreator_invalidSignatures() {
		BeanStore bs = BeanStore.INSTANCE;
		assertString(bs.creator(B3a.class).run().a).is("bar");
		assertString(bs.creator(B3b.class).run().a).is("bar");
		assertString(bs.creator(B3c.class).run().a).is("bar");
	}

	public static class B4 {
		public String a;
		public static B4 create() {
			B4 x = new B4();
			x.a = "foo";
			return x;
		}
		public static B4 create(Integer i, String s) {
			B4 x = new B4();
			x.a = i.toString() + s;
			return x;
		}
		public static B4 create(Integer i) {
			B4 x = new B4();
			x.a = i.toString();
			return x;
		}
		protected B4() {
			a = "bar";
		}
	}

	@Test
	public void b04_staticCreator_withBeans() {
		BeanStore bs = BeanStore.create().build();
		assertString(bs.creator(B4.class).run().a).is("foo");
		bs.add(Integer.class, 1);
		assertString(bs.creator(B4.class).run().a).is("1");
		bs.add(String.class, "x");
		assertString(bs.creator(B4.class).run().a).is("1x");
		bs.add(Integer.class, null);
		assertString(bs.creator(B4.class).run().a).is("foo");
	}

	public static class B5 {
		public String a;
		public static B5 create() {
			return new B5("foo");
		}
		protected B5(String s) {
			a = s;
		}
	}

	@Test
	public void b05_staticCreator_ignoredWithBuilder() {
		assertString(of(B5.class).builder("bar").run().a).is("bar");
	}

	public static class B6 {
		public String a = "foo";
		public static B6 create(Optional<String> s) {
			B6 x = new B6();
			x.a = s.orElse(null);
			return x;
		}
	}

	@Test
	public void b06_staticCreator_withOptional() {
		BeanStore bs = BeanStore.create().build();
		assertString(bs.creator(B6.class).run().a).isNull();
		bs.add(String.class, "bar");
		assertString(bs.creator(B6.class).run().a).is("bar");
	}

	public static class B7 {
		public String a = "foo";
		public static B7 create(Optional<String> s, Integer i) {
			B7 x = new B7();
			x.a = s.orElse(null) + "," + i;
			return x;
		}
		private B7() {}
	}

	@Test
	public void b07_staticCreator_missingPrereqs() {
		BeanStore bs = BeanStore.create().build();
		assertThrown(()->bs.creator(B7.class).run()).message().is("Could not instantiate class org.apache.juneau.cp.BeanCreator_Test$B7: Static creator found but could not find prerequisites: Integer.");
		bs.add(Integer.class, 1);
		assertString(bs.creator(B7.class).run().a).is("null,1");
		bs.add(String.class, "bar");
		assertString(bs.creator(B7.class).run().a).is("bar,1");
	}
	//-----------------------------------------------------------------------------------------------------------------
	// Invalid types.
	//-----------------------------------------------------------------------------------------------------------------

	public static abstract class C1 {
		public C1() {}
	}

	public interface C2 {}

	@Test
	public void c01_staticCreator_withBeans() {
		assertThrown(()->of(C1.class).run()).message().is("Could not instantiate class "+C1.class.getName()+": Class is abstract.");
		assertThrown(()->of(C2.class).run()).message().is("Could not instantiate class "+C2.class.getName()+": Class is an interface.");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Constructors.
	//-----------------------------------------------------------------------------------------------------------------

	public static class D1 {
		public String a;
		public D1(String s) {
			a = "s="+s;
		}
		public D1(Integer i) {
			a = "i="+i;
		}
		public D1(String s, Integer i) {
			a = "s="+s+",i="+i;
		}
	}

	@Test
	public void d01_constructors_public() {
		BeanStore bs = BeanStore.create().build();
		assertThrown(()->bs.creator(D1.class).run()).message().is("Could not instantiate class "+D1.class.getName()+": Public constructor found but could not find prerequisites: Integer or Integer,String or String.");
		bs.add(String.class, "foo");
		assertString(bs.creator(D1.class).run().a).is("s=foo");
		bs.add(Integer.class, 1);
		assertString(bs.creator(D1.class).run().a).is("s=foo,i=1");
		bs.removeBean(String.class);
		assertString(bs.creator(D1.class).run().a).is("i=1");
	}

	public static class D2 {
		public String a;
		protected D2(String s) {
			a = "s="+s;
		}
		protected D2(Integer i) {
			a = "i="+i;
		}
		protected D2(String s, Integer i) {
			a = "s="+s+",i="+i;
		}
	}

	@Test
	public void d02_constructors_protected() {
		BeanStore bs = BeanStore.create().build();
		assertThrown(()->bs.creator(D2.class).run()).message().is("Could not instantiate class "+D2.class.getName()+": Protected constructor found but could not find prerequisites: Integer or Integer,String or String.");
		bs.add(String.class, "foo");
		assertString(bs.creator(D2.class).run().a).is("s=foo");
		bs.add(Integer.class, 1);
		assertString(bs.creator(D2.class).run().a).is("s=foo,i=1");
		bs.removeBean(String.class);
		assertString(bs.creator(D2.class).run().a).is("i=1");
	}

	public static class D3 {
		public String a;
		public D3(String s) {
			a = "s="+s;
		}
		protected D3(String s, Integer i) {
			a = "s="+s+",i="+i;
		}
	}

	@Test
	public void d03_constructors_publicOverProtected() {
		BeanStore bs = BeanStore.create().build();
		assertThrown(()->bs.creator(D3.class).run()).message().is("Could not instantiate class "+D3.class.getName()+": Public constructor found but could not find prerequisites: String.");
		bs.add(String.class, "foo");
		bs.add(Integer.class, 1);
		assertString(bs.creator(D3.class).run().a).is("s=foo");
	}

	public static class D4 {
		private D4() {}
	}

	@Test
	public void d04_constructors_private() {
		assertThrown(()->of(D4.class).run()).message().is("Could not instantiate class "+D4.class.getName()+": No public/protected constructors found.");
	}

	public static class D5 {
		public String a;
		public D5(@Named("foo") Object o) {
			a = o.toString();
		}
		public D5(@Named("foo") Object o, Integer i) {
			a = o.toString() + "," + i;
		}
	}

	@Test
	public void d05_constructors_namedBean() {
		BeanStore bs = BeanStore.create().build();
		assertThrown(()->bs.creator(D5.class).run()).message().is("Could not instantiate class "+D5.class.getName()+": Public constructor found but could not find prerequisites: Integer,foo or foo.");
		bs.add("foo", "bar");
		assertString(bs.creator(D5.class).run().a).is("bar");
	}

	public class D6 {
		public String a;
		public D6(@Named("foo") Object o) {
			a = o.toString();
		}
		public D6(@Named("foo") Object o, Integer i) {
			a = o.toString() + "," + i;
		}
	}

	@Test
	public void d06_constructors_namedBean_withOuter() {
		BeanStore bs = BeanStore.create().build();
		Object outer = new BeanCreator_Test();
		assertThrown(()->bs.creator(D6.class).outer(outer).run()).message().is("Could not instantiate class "+D6.class.getName()+": Public constructor found but could not find prerequisites: Integer,foo or foo.");
		bs.add("foo", "bar");
		assertString(bs.creator(D6.class).outer(outer).run().a).is("bar");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builders.
	//-----------------------------------------------------------------------------------------------------------------

	public static class E1 {
		public String a;

		public static Builder create() {
			return new Builder();
		}

		public static class Builder {
			public String b;
		}

		protected E1(Builder b) {
			a = b.b;
		}
	}

	@Test
	public void e01_builders() {
		BeanStore bs = BeanStore.create().build();
		E1.Builder b = E1.create();
		b.b = "foo";
		assertString(bs.creator(E1.class).builder(b).run().a).is("foo");
	}

	public static class E2 {
		public String a;
		public static Builder create() {
			return new Builder();
		}
		public static class Builder {
			public String b;
		}
		protected E2(Builder b, Integer i) {
			a = b.b;
		}
		protected E2(Integer i) {
		}
		E2(String s) {
		}
		protected E2(Builder b) {
			a = b.b;
		}
	}

	@Test
	public void e02_builders_inherent() {
		BeanStore bs = BeanStore.create().build();
		assertString(bs.creator(E2.class).run().a).isNull();
		assertThrown(()->bs.creator(E2.class).builder(true).run()).message().is("Could not instantiate class "+E2.class.getName()+": Protected constructor found but could not find prerequisites: Builder or Builder,Integer or Integer.");
	}
}
