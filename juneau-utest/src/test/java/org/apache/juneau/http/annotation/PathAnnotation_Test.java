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
package org.apache.juneau.http.annotation;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.*;
import org.apache.juneau.oapi.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class PathAnnotation_Test {

	private static final String CNAME = PathAnnotation_Test.class.getName();

	public static class X1 {}

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	Path a1 = PathAnnotation.create()
		._enum("enum")
		.aev(true)
		.allowEmptyValue(true)
		.api("api")
		.cf("cf")
		.collectionFormat("collectionFormat")
		.d("d")
		.description("description")
		.e("e")
		.emax(true)
		.emin(true)
		.ex("ex")
		.example("example")
		.exclusiveMaximum(true)
		.exclusiveMinimum(true)
		.f("f")
		.format("format")
		.items(ItemsAnnotation.DEFAULT)
		.max("max")
		.maxi(1)
		.maximum("maximum")
		.maxItems(2)
		.maxl(3)
		.maxLength(4)
		.min("min")
		.mini(5)
		.minimum("minimum")
		.minItems(6)
		.minl(7)
		.minLength(8)
		.mo("mo")
		.multipleOf("multipleOf")
		.n("n")
		.name("name")
		.on("on")
		.onClass(X1.class)
		.p("p")
		.parser(OpenApiParser.class)
		.pattern("pattern")
		.r(true)
		.required(true)
		.serializer(OpenApiSerializer.class)
		.t("t")
		.type("type")
		.ui(true)
		.uniqueItems(true)
		.value("value")
		.build();

	Path a2 = PathAnnotation.create()
		._enum("enum")
		.aev(true)
		.allowEmptyValue(true)
		.api("api")
		.cf("cf")
		.collectionFormat("collectionFormat")
		.d("d")
		.description("description")
		.e("e")
		.emax(true)
		.emin(true)
		.ex("ex")
		.example("example")
		.exclusiveMaximum(true)
		.exclusiveMinimum(true)
		.f("f")
		.format("format")
		.items(ItemsAnnotation.DEFAULT)
		.max("max")
		.maxi(1)
		.maximum("maximum")
		.maxItems(2)
		.maxl(3)
		.maxLength(4)
		.min("min")
		.mini(5)
		.minimum("minimum")
		.minItems(6)
		.minl(7)
		.minLength(8)
		.mo("mo")
		.multipleOf("multipleOf")
		.n("n")
		.name("name")
		.on("on")
		.onClass(X1.class)
		.p("p")
		.parser(OpenApiParser.class)
		.pattern("pattern")
		.r(true)
		.required(true)
		.serializer(OpenApiSerializer.class)
		.t("t")
		.type("type")
		.ui(true)
		.uniqueItems(true)
		.value("value")
		.build();

	@Test
	public void a01_basic() {
		assertObject(a1).asJson().is(""
			+ "{"
				+ "_enum:['enum'],"
				+ "aev:true,"
				+ "allowEmptyValue:true,"
				+ "api:['api'],"
				+ "cf:'cf',"
				+ "collectionFormat:'collectionFormat',"
				+ "d:['d'],"
				+ "description:['description'],"
				+ "e:['e'],"
				+ "emax:true,"
				+ "emin:true,"
				+ "ex:['ex'],"
				+ "example:['example'],"
				+ "exclusiveMaximum:true,"
				+ "exclusiveMinimum:true,"
				+ "f:'f',"
				+ "format:'format',"
				+ "items:{'$ref':'',_default:[],_enum:[],cf:'',collectionFormat:'',df:[],e:[],emax:false,emin:false,exclusiveMaximum:false,exclusiveMinimum:false,f:'',format:'',items:{'$ref':'',_default:[],_enum:[],cf:'',collectionFormat:'',df:[],e:[],emax:false,emin:false,exclusiveMaximum:false,exclusiveMinimum:false,f:'',format:'',items:[],max:'',maxItems:-1,maxLength:-1,maxi:-1,maximum:'',maxl:-1,min:'',minItems:-1,minLength:-1,mini:-1,minimum:'',minl:-1,mo:'',multipleOf:'',p:'',pattern:'',t:'',type:'',ui:false,uniqueItems:false,value:[]},max:'',maxItems:-1,maxLength:-1,maxi:-1,maximum:'',maxl:-1,min:'',minItems:-1,minLength:-1,mini:-1,minimum:'',minl:-1,mo:'',multipleOf:'',p:'',pattern:'',t:'',type:'',ui:false,uniqueItems:false,value:[]},"
				+ "max:'max',"
				+ "maxItems:2,"
				+ "maxLength:4,"
				+ "maxi:1,"
				+ "maximum:'maximum',"
				+ "maxl:3,"
				+ "min:'min',"
				+ "minItems:6,"
				+ "minLength:8,"
				+ "mini:5,"
				+ "minimum:'minimum',"
				+ "minl:7,"
				+ "mo:'mo',"
				+ "multipleOf:'multipleOf',"
				+ "n:'n',"
				+ "name:'name',"
				+ "on:['on'],"
				+ "onClass:['"+CNAME+"$X1'],"
				+ "p:'p',"
				+ "parser:'org.apache.juneau.oapi.OpenApiParser',"
				+ "pattern:'pattern',"
				+ "r:true,"
				+ "required:true,"
				+ "serializer:'org.apache.juneau.oapi.OpenApiSerializer',"
				+ "t:'t',"
				+ "type:'type',"
				+ "ui:true,"
				+ "uniqueItems:true,"
				+ "value:'value'"
			+ "}"
		);
	}

	@Test
	public void a02_testEquivalency() {
		assertObject(a1).is(a2);
		assertInteger(a1.hashCode()).is(a2.hashCode()).isNotAny(0,-1);
	}

	//------------------------------------------------------------------------------------------------------------------
	// PropertyStore equivalency.
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void b01_testEquivalencyInPropertyStores() {
		BeanContext bc1 = BeanContext.create().annotations(a1).build();
		BeanContext bc2 = BeanContext.create().annotations(a2).build();
		assertTrue(bc1 == bc2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other methods.
	//------------------------------------------------------------------------------------------------------------------

	public static class C1 {
		public int f1;
		public void m1() {}
	}
	public static class C2 {
		public int f2;
		public void m2() {}
	}

	@Test
	public void c01_otherMethods() throws Exception {
		Path c1 = PathAnnotation.create(C1.class).on(C2.class).build();
		Path c2 = PathAnnotation.create("a").on("b").build();
		Path c3 = PathAnnotation.create().on(C1.class.getField("f1")).on(C2.class.getField("f2")).build();
		Path c4 = PathAnnotation.create().on(C1.class.getMethod("m1")).on(C2.class.getMethod("m2")).build();

		assertObject(c1).asJson().contains("on:['"+CNAME+"$C1','"+CNAME+"$C2']");
		assertObject(c2).asJson().contains("on:['a','b']");
		assertObject(c3).asJson().contains("on:['"+CNAME+"$C1.f1','"+CNAME+"$C2.f2']");
		assertObject(c4).asJson().contains("on:['"+CNAME+"$C1.m1()','"+CNAME+"$C2.m2()']");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@Path(
		_enum="enum",
		aev=true,
		allowEmptyValue=true,
		api="api",
		cf="cf",
		collectionFormat="collectionFormat",
		d="d",
		description="description",
		e="e",
		emax=true,
		emin=true,
		ex="ex",
		example="example",
		exclusiveMaximum=true,
		exclusiveMinimum=true,
		f="f",
		format="format",
		items=@Items,
		max="max",
		maxi=1,
		maximum="maximum",
		maxItems=2,
		maxl=3,
		maxLength=4,
		min="min",
		mini=5,
		minimum="minimum",
		minItems=6,
		minl=7,
		minLength=8,
		mo="mo",
		multipleOf="multipleOf",
		n="n",
		name="name",
		on="on",
		onClass=X1.class,
		p="p",
		parser=OpenApiParser.class,
		pattern="pattern",
		r=true,
		required=true,
		serializer=OpenApiSerializer.class,
		t="t",
		type="type",
		ui=true,
		uniqueItems=true,
		value="value"
	)
	public static class D1 {}
	Path d1 = D1.class.getAnnotationsByType(Path.class)[0];

	@Path(
		_enum="enum",
		aev=true,
		allowEmptyValue=true,
		api="api",
		cf="cf",
		collectionFormat="collectionFormat",
		d="d",
		description="description",
		e="e",
		emax=true,
		emin=true,
		ex="ex",
		example="example",
		exclusiveMaximum=true,
		exclusiveMinimum=true,
		f="f",
		format="format",
		items=@Items,
		max="max",
		maxi=1,
		maximum="maximum",
		maxItems=2,
		maxl=3,
		maxLength=4,
		min="min",
		mini=5,
		minimum="minimum",
		minItems=6,
		minl=7,
		minLength=8,
		mo="mo",
		multipleOf="multipleOf",
		n="n",
		name="name",
		on="on",
		onClass=X1.class,
		p="p",
		parser=OpenApiParser.class,
		pattern="pattern",
		r=true,
		required=true,
		serializer=OpenApiSerializer.class,
		t="t",
		type="type",
		ui=true,
		uniqueItems=true,
		value="value"
	)
	public static class D2 {}
	Path d2 = D2.class.getAnnotationsByType(Path.class)[0];

	@Test
	public void d01_comparisonWithDeclarativeAnnotations() {
		assertObject(d1).is(d2).is(a1);
		assertInteger(d1.hashCode()).is(d2.hashCode()).is(a1.hashCode()).isNotAny(0,-1);
	}
}
