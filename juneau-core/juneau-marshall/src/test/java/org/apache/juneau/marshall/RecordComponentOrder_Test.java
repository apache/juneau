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
package org.apache.juneau.marshall;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.csv.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.json5.*;
import org.junit.jupiter.api.*;

/**
 * Tests the {@code recordComponentOrder} setting.
 */
class RecordComponentOrder_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// a - setting plumbing
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_beanConfigContext() {
		assertBean(BeanConfigContext.DEFAULT, "recordComponentOrder", "false");
		var ctx = BeanConfigContext.create().recordComponentOrder(true).build();
		assertBean(ctx, "recordComponentOrder,unsortedProperties", "true,false");
		assertBean(ctx.copy().build(), "recordComponentOrder", "true");
	}

	@Test void a02_marshallingContext() {
		assertFalse(MarshallingContext.create().build().isRecordComponentOrder());
		var mc = MarshallingContext.create().recordComponentOrder().build();
		assertTrue(mc.isRecordComponentOrder());
		assertTrue(mc.copy().build().isRecordComponentOrder());
		assertFalse(MarshallingContext.create().recordComponentOrder(false).build().isRecordComponentOrder());
		assertTrue(mc.getBeanConfigContext().isRecordComponentOrder());
	}

	@Test void a03_hashKeyDistinguishesSetting() {
		assertNotSame(MarshallingContext.create().build(), MarshallingContext.create().recordComponentOrder().build());
	}

	@Test void a04_contextableAndSession() {
		var s = JsonSerializer.create().recordComponentOrder().build();
		assertTrue(s.getSession().isRecordComponentOrder());
		assertFalse(JsonSerializer.DEFAULT.getSession().isRecordComponentOrder());
	}

	@Test void a05_propertiesDump() {
		assertTrue(MarshallingContext.create().recordComponentOrder().build().toString().contains("recordComponentOrder"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - serializer output
	//-----------------------------------------------------------------------------------------------------------------

	public record B1(String zeta, int alpha, boolean mid) {}

	public static class B2 { public String zeta = "z"; public String alpha = "a"; }

	@BeanType(unsorted=true)
	public static class B3 { public String zeta = "z"; public String alpha = "a"; }

	@Test void b01_recordDefaultSorted_regression() {
		assertEquals("{alpha:1,mid:true,zeta:'z'}", Json5Serializer.DEFAULT.write(new B1("z", 1, true)));
	}

	@Test void b02_recordComponentOrder() {
		var s = Json5Serializer.create().recordComponentOrder().build();
		assertEquals("{zeta:'z',alpha:1,mid:true}", s.write(new B1("z", 1, true)));
		assertEquals("{alpha:'a',zeta:'z'}", s.write(new B2()));  // classes unaffected
	}

	@Test void b03_unsortedProperties_declarationOrder() {
		var s = Json5Serializer.create().unsortedProperties().build();
		assertEquals("{zeta:'z',alpha:1,mid:true}", s.write(new B1("z", 1, true)));
		assertEquals("{zeta:'z',alpha:'a'}", s.write(new B2()));
	}

	@Test void b04_beanTypeUnsorted() {
		assertList(names(MarshallingContext.create().build(), B3.class), "zeta", "alpha");
		assertEquals("{zeta:'z',alpha:'a'}", Json5Serializer.DEFAULT.write(new B3()));
	}

	@Test void b05_unsortedPropertiesOnClass() {
		var s = Json5Serializer.create().unsortedProperties(B2.class).build();
		assertEquals("{zeta:'z',alpha:'a'}", s.write(new B2()));
	}

	@Test void b06_csvColumnOrderFollowsBeanOrder() {
		var unsorted = CsvSerializer.create().unsortedProperties().build();
		assertEquals("zeta,alpha", unsorted.write(List.of(new B2())).toString().lines().findFirst().orElse(""));
		assertEquals("alpha,zeta", CsvSerializer.DEFAULT.write(List.of(new B2())).toString().lines().findFirst().orElse(""));
	}

	@Test void b07_parseRoundTripUnaffected() {
		var s = Json5Serializer.create().recordComponentOrder().build();
		var p = Json5Parser.create().recordComponentOrder().build();
		assertBean(p.read(s.write(new B1("z", 1, true)), B1.class), "zeta,alpha,mid", "z,1,true");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d - precedence (moved from commons BeanMeta_DeclarationOrder_Test d01: commons BeanMeta.of ignores @BeanType;
	// the d02 @BeanType(unsorted) case is covered by b04)
	//-----------------------------------------------------------------------------------------------------------------

	@BeanType(properties="mid,zeta")
	public static class D1 { public String zeta = "z"; public String alpha = "a"; public String mid = "m"; }

	private static List<String> names(MarshallingContext ctx, Class<?> c) {
		return new ArrayList<>(ctx.getBeanMeta(c).getProperties().keySet());
	}

	@Test void d01_fixedPropertiesListWins() {
		var unsorted = MarshallingContext.create().unsortedProperties().build();
		var sorted = MarshallingContext.create().build();
		assertList(names(unsorted, D1.class), "mid", "zeta");
		assertList(names(sorted, D1.class), "mid", "zeta");
		assertEquals("{mid:'m',zeta:'z'}", Json5Serializer.create().unsortedProperties().build().write(new D1()));
	}
}
