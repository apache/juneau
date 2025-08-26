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

import static org.apache.juneau.Visibility.*;
import static org.junit.Assert.*;

import org.apache.juneau.a.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

class VisibilityTest extends SimpleTestBase {

	//====================================================================================================
	// testVisibility
	//====================================================================================================
	@Test void a01_classDefault() throws Exception {
		JsonSerializer.Builder s1 = JsonSerializer.create().json5().sortProperties().disableBeansRequireSomeProperties();
		JsonSerializer.Builder s2 = JsonSerializer.create().json5().sortProperties().disableBeansRequireSomeProperties().beanClassVisibility(PROTECTED);
		JsonSerializer.Builder s3 = JsonSerializer.create().json5().sortProperties().disableBeansRequireSomeProperties().beanClassVisibility(Visibility.DEFAULT);
		JsonSerializer.Builder s4 = JsonSerializer.create().json5().sortProperties().disableBeansRequireSomeProperties().beanClassVisibility(PRIVATE);

		var a1 = A1.create();
		String r;

		s1.beanFieldVisibility(NONE);
		s2.beanFieldVisibility(NONE);
		s3.beanFieldVisibility(NONE);
		s4.beanFieldVisibility(NONE);

		r = s1.build().serialize(a1);
		assertEquals("{f5:5}", r);

		r = s2.build().serialize(a1);
		assertEquals("{f5:5}", r);

		r = s3.build().serialize(a1);
		assertEquals("{f5:5}", r);

		r = s4.build().serialize(a1);
		assertEquals("{f5:5}", r);

		s1.beanFieldVisibility(PUBLIC);
		s2.beanFieldVisibility(PUBLIC);
		s3.beanFieldVisibility(PUBLIC);
		s4.beanFieldVisibility(PUBLIC);

		r = s1.build().serialize(a1);
		assertEquals("{f1:1,f5:5,g2:{f1:1,f5:5},g3:'A3',g4:'A4',g5:'A5'}", r);

		r = s2.build().serialize(a1);
		assertEquals("{f1:1,f5:5,g2:{f1:1,f5:5},g3:{f1:1,f5:5},g4:'A4',g5:'A5'}", r);

		r = s3.build().serialize(a1);
		assertEquals("{f1:1,f5:5,g2:{f1:1,f5:5},g3:{f1:1,f5:5},g4:{f1:1,f5:5},g5:'A5'}", r);

		r = s4.build().serialize(a1);
		assertEquals("{f1:1,f5:5,g2:{f1:1,f5:5},g3:{f1:1,f5:5},g4:{f1:1,f5:5},g5:{f1:1,f5:5}}", r);

		s1.beanFieldVisibility(PROTECTED);
		s2.beanFieldVisibility(PROTECTED);
		s3.beanFieldVisibility(PROTECTED);
		s4.beanFieldVisibility(PROTECTED);

		r = s1.build().serialize(a1);
		assertEquals("{f1:1,f2:2,f5:5,g2:{f1:1,f2:2,f5:5},g3:'A3',g4:'A4',g5:'A5'}", r);

		r = s2.build().serialize(a1);
		assertEquals("{f1:1,f2:2,f5:5,g2:{f1:1,f2:2,f5:5},g3:{f1:1,f2:2,f5:5},g4:'A4',g5:'A5'}", r);

		r = s3.build().serialize(a1);
		assertEquals("{f1:1,f2:2,f5:5,g2:{f1:1,f2:2,f5:5},g3:{f1:1,f2:2,f5:5},g4:{f1:1,f2:2,f5:5},g5:'A5'}", r);

		r = s4.build().serialize(a1);
		assertEquals("{f1:1,f2:2,f5:5,g2:{f1:1,f2:2,f5:5},g3:{f1:1,f2:2,f5:5},g4:{f1:1,f2:2,f5:5},g5:{f1:1,f2:2,f5:5}}", r);

		s1.beanFieldVisibility(Visibility.DEFAULT);
		s2.beanFieldVisibility(Visibility.DEFAULT);
		s3.beanFieldVisibility(Visibility.DEFAULT);
		s4.beanFieldVisibility(Visibility.DEFAULT);

		r = s1.build().serialize(a1);
		assertEquals("{f1:1,f2:2,f3:3,f5:5,g2:{f1:1,f2:2,f3:3,f5:5},g3:'A3',g4:'A4',g5:'A5'}", r);

		r = s2.build().serialize(a1);
		assertEquals("{f1:1,f2:2,f3:3,f5:5,g2:{f1:1,f2:2,f3:3,f5:5},g3:{f1:1,f2:2,f3:3,f5:5},g4:'A4',g5:'A5'}", r);

		r = s3.build().serialize(a1);
		assertEquals("{f1:1,f2:2,f3:3,f5:5,g2:{f1:1,f2:2,f3:3,f5:5},g3:{f1:1,f2:2,f3:3,f5:5},g4:{f1:1,f2:2,f3:3,f5:5},g5:'A5'}", r);

		r = s4.build().serialize(a1);
		assertEquals("{f1:1,f2:2,f3:3,f5:5,g2:{f1:1,f2:2,f3:3,f5:5},g3:{f1:1,f2:2,f3:3,f5:5},g4:{f1:1,f2:2,f3:3,f5:5},g5:{f1:1,f2:2,f3:3,f5:5}}", r);

		s1.beanFieldVisibility(PRIVATE);
		s2.beanFieldVisibility(PRIVATE);
		s3.beanFieldVisibility(PRIVATE);
		s4.beanFieldVisibility(PRIVATE);

		r = s1.build().serialize(a1);
		assertEquals("{f1:1,f2:2,f3:3,f4:4,f5:5,g2:{f1:1,f2:2,f3:3,f4:4,f5:5},g3:'A3',g4:'A4',g5:'A5'}", r);

		r = s2.build().serialize(a1);
		assertEquals("{f1:1,f2:2,f3:3,f4:4,f5:5,g2:{f1:1,f2:2,f3:3,f4:4,f5:5},g3:{f1:1,f2:2,f3:3,f4:4,f5:5},g4:'A4',g5:'A5'}", r);

		r = s3.build().serialize(a1);
		assertEquals("{f1:1,f2:2,f3:3,f4:4,f5:5,g2:{f1:1,f2:2,f3:3,f4:4,f5:5},g3:{f1:1,f2:2,f3:3,f4:4,f5:5},g4:{f1:1,f2:2,f3:3,f4:4,f5:5},g5:'A5'}", r);

		r = s4.build().serialize(a1);
		assertEquals("{f1:1,f2:2,f3:3,f4:4,f5:5,g2:{f1:1,f2:2,f3:3,f4:4,f5:5},g3:{f1:1,f2:2,f3:3,f4:4,f5:5},g4:{f1:1,f2:2,f3:3,f4:4,f5:5},g5:{f1:1,f2:2,f3:3,f4:4,f5:5}}", r);

		s1.beanMethodVisibility(NONE);
		s2.beanMethodVisibility(NONE);
		s3.beanMethodVisibility(NONE);
		s4.beanMethodVisibility(NONE);

		r = s1.build().serialize(a1);
		assertEquals("{f1:1,f2:2,f3:3,f4:4,g2:{f1:1,f2:2,f3:3,f4:4},g3:'A3',g4:'A4',g5:'A5'}", r);

		r = s2.build().serialize(a1);
		assertEquals("{f1:1,f2:2,f3:3,f4:4,g2:{f1:1,f2:2,f3:3,f4:4},g3:{f1:1,f2:2,f3:3,f4:4},g4:'A4',g5:'A5'}", r);

		r = s3.build().serialize(a1);
		assertEquals("{f1:1,f2:2,f3:3,f4:4,g2:{f1:1,f2:2,f3:3,f4:4},g3:{f1:1,f2:2,f3:3,f4:4},g4:{f1:1,f2:2,f3:3,f4:4},g5:'A5'}", r);

		r = s4.build().serialize(a1);
		assertEquals("{f1:1,f2:2,f3:3,f4:4,g2:{f1:1,f2:2,f3:3,f4:4},g3:{f1:1,f2:2,f3:3,f4:4},g4:{f1:1,f2:2,f3:3,f4:4},g5:{f1:1,f2:2,f3:3,f4:4}}", r);

		s1.beanMethodVisibility(PROTECTED);
		s2.beanMethodVisibility(PROTECTED);
		s3.beanMethodVisibility(PROTECTED);
		s4.beanMethodVisibility(PROTECTED);

		r = s1.build().serialize(a1);
		assertEquals("{f1:1,f2:2,f3:3,f4:4,f5:5,f6:6,g2:{f1:1,f2:2,f3:3,f4:4,f5:5,f6:6},g3:'A3',g4:'A4',g5:'A5'}", r);

		r = s2.build().serialize(a1);
		assertEquals("{f1:1,f2:2,f3:3,f4:4,f5:5,f6:6,g2:{f1:1,f2:2,f3:3,f4:4,f5:5,f6:6},g3:{f1:1,f2:2,f3:3,f4:4,f5:5,f6:6},g4:'A4',g5:'A5'}", r);

		r = s3.build().serialize(a1);
		assertEquals("{f1:1,f2:2,f3:3,f4:4,f5:5,f6:6,g2:{f1:1,f2:2,f3:3,f4:4,f5:5,f6:6},g3:{f1:1,f2:2,f3:3,f4:4,f5:5,f6:6},g4:{f1:1,f2:2,f3:3,f4:4,f5:5,f6:6},g5:'A5'}", r);

		r = s4.build().serialize(a1);
		assertEquals("{f1:1,f2:2,f3:3,f4:4,f5:5,f6:6,g2:{f1:1,f2:2,f3:3,f4:4,f5:5,f6:6},g3:{f1:1,f2:2,f3:3,f4:4,f5:5,f6:6},g4:{f1:1,f2:2,f3:3,f4:4,f5:5,f6:6},g5:{f1:1,f2:2,f3:3,f4:4,f5:5,f6:6}}", r);

	}

	static class A {
		public int f1;
		public A(){/* no-op */}

		static A create() {
			var x = new A();
			x.f1 = 1;
			return x;
		}
	}
}