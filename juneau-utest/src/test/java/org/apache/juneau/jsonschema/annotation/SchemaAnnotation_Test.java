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
package org.apache.juneau.jsonschema.annotation;

import static org.junit.Assert.*;
import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.junit.jupiter.api.*;

public class SchemaAnnotation_Test extends SimpleTestBase {

	private static final String CNAME = SchemaAnnotation_Test.class.getName();

	private static class X1 {}

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	Schema a1 = SchemaAnnotation.create()
		._default("default")
		._enum("enum")
		.$ref("$ref")
		.additionalProperties("additionalProperties")
		.allOf("allOf")
		.cf("cf")
		.collectionFormat("collectionFormat")
		.d("d")
		.description("description")
		.df("df")
		.discriminator("discriminator")
		.e("e")
		.emax(true)
		.emin(true)
		.exclusiveMaximum(true)
		.exclusiveMinimum(true)
		.externalDocs(ExternalDocsAnnotation.DEFAULT)
		.f("f")
		.format("format")
		.ignore(true)
		.items(ItemsAnnotation.DEFAULT)
		.max("max")
		.maxi(1)
		.maximum("maximum")
		.maxItems(2)
		.maxl(3)
		.maxLength(4)
		.maxp(5)
		.maxProperties(6)
		.min("min")
		.mini(7)
		.minimum("minimum")
		.minItems(8)
		.minl(9)
		.minLength(10)
		.minp(11)
		.minProperties(12)
		.mo("mo")
		.multipleOf("multipleOf")
		.on("on")
		.onClass(X1.class)
		.p("p")
		.pattern("pattern")
		.properties("properties")
		.r(true)
		.readOnly(true)
		.required(true)
		.ro(true)
		.t("t")
		.title("title")
		.type("type")
		.ui(true)
		.uniqueItems(true)
		.xml("xml")
		.build();

	Schema a2 = SchemaAnnotation.create()
		._default("default")
		._enum("enum")
		.$ref("$ref")
		.additionalProperties("additionalProperties")
		.allOf("allOf")
		.cf("cf")
		.collectionFormat("collectionFormat")
		.d("d")
		.description("description")
		.df("df")
		.discriminator("discriminator")
		.e("e")
		.emax(true)
		.emin(true)
		.exclusiveMaximum(true)
		.exclusiveMinimum(true)
		.externalDocs(ExternalDocsAnnotation.DEFAULT)
		.f("f")
		.format("format")
		.ignore(true)
		.items(ItemsAnnotation.DEFAULT)
		.max("max")
		.maxi(1)
		.maximum("maximum")
		.maxItems(2)
		.maxl(3)
		.maxLength(4)
		.maxp(5)
		.maxProperties(6)
		.min("min")
		.mini(7)
		.minimum("minimum")
		.minItems(8)
		.minl(9)
		.minLength(10)
		.minp(11)
		.minProperties(12)
		.mo("mo")
		.multipleOf("multipleOf")
		.on("on")
		.onClass(X1.class)
		.p("p")
		.pattern("pattern")
		.properties("properties")
		.r(true)
		.readOnly(true)
		.required(true)
		.ro(true)
		.t("t")
		.title("title")
		.type("type")
		.ui(true)
		.uniqueItems(true)
		.xml("xml")
		.build();

	@Test void a01_basic() {
		assertJson(a1, ""
			+ "{"
				+ "'$ref':'$ref',"
				+ "_default:['default'],"
				+ "_enum:['enum'],"
				+ "additionalProperties:['additionalProperties'],"
				+ "aev:false,"
				+ "allOf:['allOf'],"
				+ "allowEmptyValue:false,"
				+ "cf:'cf',"
				+ "collectionFormat:'collectionFormat',"
				+ "d:['d'],"
				+ "description:['description'],"
				+ "df:['df'],"
				+ "discriminator:'discriminator',"
				+ "e:['e'],"
				+ "emax:true,"
				+ "emin:true,"
				+ "exclusiveMaximum:true,"
				+ "exclusiveMinimum:true,"
				+ "externalDocs:{description:[],url:''},"
				+ "f:'f',"
				+ "format:'format',"
				+ "ignore:true,"
				+ "items:{'$ref':'',_default:[],_enum:[],cf:'',collectionFormat:'',description:[],df:[],e:[],emax:false,emin:false,exclusiveMaximum:false,exclusiveMinimum:false,f:'',format:'',items:{'$ref':'',_default:[],_enum:[],cf:'',collectionFormat:'',description:[],df:[],e:[],emax:false,emin:false,exclusiveMaximum:false,exclusiveMinimum:false,f:'',format:'',items:[],max:'',maxItems:-1,maxLength:-1,maxi:-1,maximum:'',maxl:-1,min:'',minItems:-1,minLength:-1,mini:-1,minimum:'',minl:-1,mo:'',multipleOf:'',p:'',pattern:'',t:'',type:'',ui:false,uniqueItems:false},max:'',maxItems:-1,maxLength:-1,maxi:-1,maximum:'',maxl:-1,min:'',minItems:-1,minLength:-1,mini:-1,minimum:'',minl:-1,mo:'',multipleOf:'',p:'',pattern:'',t:'',type:'',ui:false,uniqueItems:false},"
				+ "max:'max',"
				+ "maxItems:2,"
				+ "maxLength:4,"
				+ "maxProperties:6,"
				+ "maxi:1,"
				+ "maximum:'maximum',"
				+ "maxl:3,"
				+ "maxp:5,"
				+ "min:'min',"
				+ "minItems:8,"
				+ "minLength:10,"
				+ "minProperties:12,"
				+ "mini:7,"
				+ "minimum:'minimum',"
				+ "minl:9,"
				+ "minp:11,"
				+ "mo:'mo',"
				+ "multipleOf:'multipleOf',"
				+ "on:['on'],"
				+ "onClass:['"+CNAME+"$X1'],"
				+ "p:'p',"
				+ "pattern:'pattern',"
				+ "properties:['properties'],"
				+ "r:true,"
				+ "readOnly:true,"
				+ "required:true,"
				+ "ro:true,"
				+ "sie:false,skipIfEmpty:false,"
				+ "t:'t',"
				+ "title:'title',"
				+ "type:'type',"
				+ "ui:true,"
				+ "uniqueItems:true,"
				+ "xml:['xml']"
			+ "}"
		);
	}

	@Test void a02_testEquivalency() {
		assertEquals(a2, a1);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEquals(a1.hashCode(), a2.hashCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// PropertyStore equivalency.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_testEquivalencyInPropertyStores() {
		BeanContext bc1 = BeanContext.create().annotations(a1).build();
		BeanContext bc2 = BeanContext.create().annotations(a2).build();
		assertSame(bc1, bc2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other methods.
	//------------------------------------------------------------------------------------------------------------------

	public static class C1 {
		public int f1;
		public void m1() {}  // NOSONAR
	}
	public static class C2 {
		public int f2;
		public void m2() {}  // NOSONAR
	}

	@Test void c01_otherMethods() throws Exception {
		Schema c1 = SchemaAnnotation.create(C1.class).on(C2.class).build();
		Schema c2 = SchemaAnnotation.create("a").on("b").build();
		Schema c3 = SchemaAnnotation.create().on(C1.class.getField("f1")).on(C2.class.getField("f2")).build();
		Schema c4 = SchemaAnnotation.create().on(C1.class.getMethod("m1")).on(C2.class.getMethod("m2")).build();

		assertJsonContains(c1, "on:['"+CNAME+"$C1','"+CNAME+"$C2']");
		assertJsonContains(c2, "on:['a','b']");
		assertJsonContains(c3, "on:['"+CNAME+"$C1.f1','"+CNAME+"$C2.f2']");
		assertJsonContains(c4, "on:['"+CNAME+"$C1.m1()','"+CNAME+"$C2.m2()']");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@Schema(
		_default="default",
		_enum="enum",
		$ref="$ref",
		additionalProperties="additionalProperties",
		allOf="allOf",
		cf="cf",
		collectionFormat="collectionFormat",
		d="d",
		description="description",
		df="df",
		discriminator="discriminator",
		e="e",
		emax=true,
		emin=true,
		exclusiveMaximum=true,
		exclusiveMinimum=true,
		externalDocs=@ExternalDocs,
		f="f",
		format="format",
		ignore=true,
		items=@Items,
		max="max",
		maxi=1,
		maximum="maximum",
		maxItems=2,
		maxl=3,
		maxLength=4,
		maxp=5,
		maxProperties=6,
		min="min",
		mini=7,
		minimum="minimum",
		minItems=8,
		minl=9,
		minLength=10,
		minp=11,
		minProperties=12,
		mo="mo",
		multipleOf="multipleOf",
		on="on",
		onClass=X1.class,
		p="p",
		pattern="pattern",
		properties="properties",
		r=true,
		readOnly=true,
		required=true,
		ro=true,
		t="t",
		title="title",
		type="type",
		ui=true,
		uniqueItems=true,
		xml="xml"
	)
	public static class D1 {}
	Schema d1 = D1.class.getAnnotationsByType(Schema.class)[0];

	@Schema(
		_default="default",
		_enum="enum",
		$ref="$ref",
		additionalProperties="additionalProperties",
		allOf="allOf",
		cf="cf",
		collectionFormat="collectionFormat",
		d="d",
		description="description",
		df="df",
		discriminator="discriminator",
		e="e",
		emax=true,
		emin=true,
		exclusiveMaximum=true,
		exclusiveMinimum=true,
		externalDocs=@ExternalDocs,
		f="f",
		format="format",
		ignore=true,
		items=@Items,
		max="max",
		maxi=1,
		maximum="maximum",
		maxItems=2,
		maxl=3,
		maxLength=4,
		maxp=5,
		maxProperties=6,
		min="min",
		mini=7,
		minimum="minimum",
		minItems=8,
		minl=9,
		minLength=10,
		minp=11,
		minProperties=12,
		mo="mo",
		multipleOf="multipleOf",
		on="on",
		onClass=X1.class,
		p="p",
		pattern="pattern",
		properties="properties",
		r=true,
		readOnly=true,
		required=true,
		ro=true,
		t="t",
		title="title",
		type="type",
		ui=true,
		uniqueItems=true,
		xml="xml"
	)
	public static class D2 {}
	Schema d2 = D2.class.getAnnotationsByType(Schema.class)[0];

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(a1, d1, d2);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEqualsAll(a1.hashCode(), d1.hashCode(), d2.hashCode());
	}
}