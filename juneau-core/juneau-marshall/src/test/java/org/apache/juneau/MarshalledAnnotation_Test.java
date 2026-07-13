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
package org.apache.juneau;

import static org.apache.juneau.BasicTestUtils.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

class MarshalledAnnotation_Test extends TestBase {

	private static class X1 {}
	private  static class X2 extends BeanInterceptor<MarshalledAnnotation_Test> {}

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	Marshalled a1 = MarshalledAnnotation.create()
		.binaryFormat(BinaryFormat.HEX)
		.calendarFormat(CalendarFormat.ISO_INSTANT)
		.dateFormat(DateFormat.ISO_INSTANT)
		.description("a")
		.dictionary(X1.class)
		.durationFormat(DurationFormat.SECONDS)
		.enumFormat(EnumFormat.NAME)
		.example("b")
		.implClass(X1.class)
		.interceptor(X2.class)
		.localeFormat(LocaleFormat.UNDERSCORE)
		.periodFormat(PeriodFormat.DAYS)
		.temporalFormat(TemporalFormat.ISO_INSTANT)
		.timeZoneFormat(TimeZoneFormat.OFFSET)
		.typeName("i")
		.typePropertyName("j")
		.build();

	Marshalled a2 = MarshalledAnnotation.create()
		.binaryFormat(BinaryFormat.HEX)
		.calendarFormat(CalendarFormat.ISO_INSTANT)
		.dateFormat(DateFormat.ISO_INSTANT)
		.description("a")
		.dictionary(X1.class)
		.durationFormat(DurationFormat.SECONDS)
		.enumFormat(EnumFormat.NAME)
		.example("b")
		.implClass(X1.class)
		.interceptor(X2.class)
		.localeFormat(LocaleFormat.UNDERSCORE)
		.periodFormat(PeriodFormat.DAYS)
		.temporalFormat(TemporalFormat.ISO_INSTANT)
		.timeZoneFormat(TimeZoneFormat.OFFSET)
		.typeName("i")
		.typePropertyName("j")
		.build();

	BeanType bt1 = BeanTypeAnnotation.create()
		.description("a")
		.excludeProperties("c")
		.findFluentSetters(true)
		.interfaceClass(X1.class)
		.p("e")
		.properties("f")
		.propertyNamer(BasicPropertyNamer.class)
		.readOnlyProperties("g")
		.ro("h")
		.unsorted(true)
		.stopClass(X1.class)
		.wo("k")
		.writeOnlyProperties("l")
		.xp("m")
		.build();

	BeanType bt2 = BeanTypeAnnotation.create()
		.description("a")
		.excludeProperties("c")
		.findFluentSetters(true)
		.interfaceClass(X1.class)
		.p("e")
		.properties("f")
		.propertyNamer(BasicPropertyNamer.class)
		.readOnlyProperties("g")
		.ro("h")
		.unsorted(true)
		.stopClass(X1.class)
		.wo("k")
		.writeOnlyProperties("l")
		.xp("m")
		.build();

	@Test void a01_basic() {
		assertBean(a1,
			"binaryFormat,calendarFormat,dateFormat,description,dictionary,durationFormat,enumFormat,example,implClass,interceptor,localeFormat,periodFormat,temporalFormat,timeZoneFormat,typeName,typePropertyName",
			"HEX,ISO_INSTANT,ISO_INSTANT,[a],[X1],SECONDS,NAME,b,X1,X2,UNDERSCORE,DAYS,ISO_INSTANT,OFFSET,i,j");
		assertBean(bt1,
			"description,excludeProperties,findFluentSetters,interfaceClass,p,properties,propertyNamer,readOnlyProperties,ro,stopClass,unsorted,wo,writeOnlyProperties,xp",
			"[a],c,true,X1,e,f,BasicPropertyNamer,g,h,X1,true,k,l,m");
	}

	@Test void a02_testEquivalency() {
		assertEquals(a2, a1);
		assertNotEquals(0, a1.hashCode());
		assertNotEquals(-1, a1.hashCode());
		assertEquals(a1.hashCode(), a2.hashCode());

		assertEquals(bt2, bt1);
		assertNotEquals(0, bt1.hashCode());
		assertNotEquals(-1, bt1.hashCode());
		assertEquals(bt1.hashCode(), bt2.hashCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// PropertyStore equivalency.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_testEquivalencyInPropertyStores() {
		var b1 = MarshallingContext.create().annotations(a1, bt1).build();
		var b2 = MarshallingContext.create().annotations(a2, bt2).build();
		assertSame(b1, b2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@Marshalled(binaryFormat=BinaryFormat.HEX, calendarFormat=CalendarFormat.ISO_INSTANT, dateFormat=DateFormat.ISO_INSTANT, description={ "a" }, dictionary=X1.class, durationFormat=DurationFormat.SECONDS, enumFormat=EnumFormat.NAME, example="b", implClass=X1.class, interceptor=X2.class, localeFormat=LocaleFormat.UNDERSCORE, periodFormat=PeriodFormat.DAYS, temporalFormat=TemporalFormat.ISO_INSTANT, timeZoneFormat=TimeZoneFormat.OFFSET, typeName="i", typePropertyName="j")
	@BeanType(description={ "a" }, excludeProperties="c", findFluentSetters=true, interfaceClass=X1.class, p="e", properties="f", propertyNamer=BasicPropertyNamer.class, readOnlyProperties="g", ro="h", unsorted=true, stopClass=X1.class, wo="k", writeOnlyProperties="l", xp="m")
	public static class D1 {}
	Marshalled d1 = D1.class.getAnnotationsByType(Marshalled.class)[0];
	BeanType d1bt = D1.class.getAnnotationsByType(BeanType.class)[0];

	@Marshalled(binaryFormat=BinaryFormat.HEX, calendarFormat=CalendarFormat.ISO_INSTANT, dateFormat=DateFormat.ISO_INSTANT, description={ "a" }, dictionary=X1.class, durationFormat=DurationFormat.SECONDS, enumFormat=EnumFormat.NAME, example="b", implClass=X1.class, interceptor=X2.class, localeFormat=LocaleFormat.UNDERSCORE, periodFormat=PeriodFormat.DAYS, temporalFormat=TemporalFormat.ISO_INSTANT, timeZoneFormat=TimeZoneFormat.OFFSET, typeName="i", typePropertyName="j")
	@BeanType(description={ "a" }, excludeProperties="c", findFluentSetters=true, interfaceClass=X1.class, p="e", properties="f", propertyNamer=BasicPropertyNamer.class, readOnlyProperties="g", ro="h", unsorted=true, stopClass=X1.class, wo="k", writeOnlyProperties="l", xp="m")
	public static class D2 {}
	Marshalled d2 = D2.class.getAnnotationsByType(Marshalled.class)[0];
	BeanType d2bt = D2.class.getAnnotationsByType(BeanType.class)[0];

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(a1, d1, d2);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEqualsAll(a1.hashCode(), d1.hashCode(), d2.hashCode());

		assertEqualsAll(bt1, d1bt, d2bt);
		assertNotEqualsAny(bt1.hashCode(), 0, -1);
		assertEqualsAll(bt1.hashCode(), d1bt.hashCode(), d2bt.hashCode());
	}
}
