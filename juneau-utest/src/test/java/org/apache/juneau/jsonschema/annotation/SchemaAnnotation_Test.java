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

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.junit.jupiter.api.*;

class SchemaAnnotation_Test extends TestBase {

	private static final String CNAME = SchemaAnnotation_Test.class.getName();

	private static class X1 {}

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	Schema a1 = SchemaAnnotation.create()
		._default("a")
		._enum("b")
		.$ref("c")
		.additionalProperties("d")
		.allOf("e")
		.cf("f")
		.collectionFormat("g")
		.d("h")
		.description("i")
		.df("j")
		.discriminator("k")
		.e("l")
		.emax(true)
		.emin(true)
		.exclusiveMaximum(true)
		.exclusiveMinimum(true)
		.externalDocs(ExternalDocsAnnotation.DEFAULT)
		.f("m")
		.format("n")
		.ignore(true)
		.items(ItemsAnnotation.DEFAULT)
		.max("o")
		.maxi(1)
		.maximum("p")
		.maxItems(2)
		.maxl(3)
		.maxLength(4)
		.maxp(5)
		.maxProperties(6)
		.min("q")
		.mini(7)
		.minimum("r")
		.minItems(8)
		.minl(9)
		.minLength(10)
		.minp(11)
		.minProperties(12)
		.mo("s")
		.multipleOf("t")
		.on("u")
		.onClass(X1.class)
		.p("v")
		.pattern("w")
		.properties("x")
		.r(true)
		.readOnly(true)
		.required(true)
		.ro(true)
		.t("z")
		.title("aa")
		.type("bb")
		.ui(true)
		.uniqueItems(true)
		.xml("cc")
		.build();

	Schema a2 = SchemaAnnotation.create()
		._default("a")
		._enum("b")
		.$ref("c")
		.additionalProperties("d")
		.allOf("e")
		.cf("f")
		.collectionFormat("g")
		.d("h")
		.description("i")
		.df("j")
		.discriminator("k")
		.e("l")
		.emax(true)
		.emin(true)
		.exclusiveMaximum(true)
		.exclusiveMinimum(true)
		.externalDocs(ExternalDocsAnnotation.DEFAULT)
		.f("m")
		.format("n")
		.ignore(true)
		.items(ItemsAnnotation.DEFAULT)
		.max("o")
		.maxi(1)
		.maximum("p")
		.maxItems(2)
		.maxl(3)
		.maxLength(4)
		.maxp(5)
		.maxProperties(6)
		.min("q")
		.mini(7)
		.minimum("r")
		.minItems(8)
		.minl(9)
		.minLength(10)
		.minp(11)
		.minProperties(12)
		.mo("s")
		.multipleOf("t")
		.on("u")
		.onClass(X1.class)
		.p("v")
		.pattern("w")
		.properties("x")
		.r(true)
		.readOnly(true)
		.required(true)
		.ro(true)
		.t("z")
		.title("aa")
		.type("bb")
		.ui(true)
		.uniqueItems(true)
		.xml("cc")
		.build();

	@Test void a01_basic() {
		assertBean(a1,
			"$ref,_default,_enum,additionalProperties,aev,allOf,allowEmptyValue,cf,collectionFormat,d,description,df,discriminator,e,emax,emin,exclusiveMaximum,exclusiveMinimum,externalDocs{description,url},f,format,ignore,items{$ref,_default,_enum,cf,collectionFormat,description,df,e,emax,emin,exclusiveMaximum,exclusiveMinimum,f,format,items{$ref,_default,_enum,cf,collectionFormat,description,df,e,emax,emin,exclusiveMaximum,exclusiveMinimum,f,format,items,max,maxItems,maxLength,maxi,maximum,maxl,min,minItems,minLength,mini,minimum,minl,mo,multipleOf,p,pattern,t,type,ui,uniqueItems},max,maxItems,maxLength,maxi,maximum,maxl,min,minItems,minLength,mini,minimum,minl,mo,multipleOf,p,pattern,t,type,ui,uniqueItems},max,maxItems,maxLength,maxProperties,maxi,maximum,maxl,maxp,min,minItems,minLength,minProperties,mini,minimum,minl,minp,mo,multipleOf,on,onClass,p,pattern,properties,r,readOnly,required,ro,sie,skipIfEmpty,t,title,type,ui,uniqueItems,xml",
			"c,[a],[b],[d],false,[e],false,f,g,[h],[i],[j],k,[l],true,true,true,true,{[],},m,n,true,{,[],[],,,[],[],[],false,false,false,false,,,{,[],[],,,[],[],[],false,false,false,false,,,[],,-1,-1,-1,,-1,,-1,-1,-1,,-1,,,,,,,false,false},,-1,-1,-1,,-1,,-1,-1,-1,,-1,,,,,,,false,false},o,2,4,6,1,p,3,5,q,8,10,12,7,r,9,11,s,t,[u],[X1],v,w,[x],true,true,true,true,false,false,z,aa,bb,true,true,[cc]");
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
		var bc1 = BeanContext.create().annotations(a1).build();
		var bc2 = BeanContext.create().annotations(a2).build();
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
		var c1 = SchemaAnnotation.create(C1.class).on(C2.class).build();
		var c2 = SchemaAnnotation.create("a").on("b").build();
		var c3 = SchemaAnnotation.create().on(C1.class.getField("f1")).on(C2.class.getField("f2")).build();
		var c4 = SchemaAnnotation.create().on(C1.class.getMethod("m1")).on(C2.class.getMethod("m2")).build();

		assertBean(c1, "on", "["+CNAME+"$C1,"+CNAME+"$C2]");
		assertBean(c2, "on", "[a,b]");
		assertBean(c3, "on", "["+CNAME+"$C1.f1,"+CNAME+"$C2.f2]");
		assertBean(c4, "on", "["+CNAME+"$C1.m1(),"+CNAME+"$C2.m2()]");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@Schema(
		_default="a",
		_enum="b",
		$ref="c",
		additionalProperties="d",
		allOf="e",
		cf="f",
		collectionFormat="g",
		d="h",
		description="i",
		df="j",
		discriminator="k",
		e="l",
		emax=true,
		emin=true,
		exclusiveMaximum=true,
		exclusiveMinimum=true,
		externalDocs=@ExternalDocs,
		f="m",
		format="n",
		ignore=true,
		items=@Items,
		max="o",
		maxi=1,
		maximum="p",
		maxItems=2,
		maxl=3,
		maxLength=4,
		maxp=5,
		maxProperties=6,
		min="q",
		mini=7,
		minimum="r",
		minItems=8,
		minl=9,
		minLength=10,
		minp=11,
		minProperties=12,
		mo="s",
		multipleOf="t",
		on="u",
		onClass=X1.class,
		p="v",
		pattern="w",
		properties="x",
		r=true,
		readOnly=true,
		required=true,
		ro=true,
		t="z",
		title="aa",
		type="bb",
		ui=true,
		uniqueItems=true,
		xml="cc"
	)
	public static class D1 {}
	Schema d1 = D1.class.getAnnotationsByType(Schema.class)[0];

	@Schema(
		_default="a",
		_enum="b",
		$ref="c",
		additionalProperties="d",
		allOf="e",
		cf="f",
		collectionFormat="g",
		d="h",
		description="i",
		df="j",
		discriminator="k",
		e="l",
		emax=true,
		emin=true,
		exclusiveMaximum=true,
		exclusiveMinimum=true,
		externalDocs=@ExternalDocs,
		f="m",
		format="n",
		ignore=true,
		items=@Items,
		max="o",
		maxi=1,
		maximum="p",
		maxItems=2,
		maxl=3,
		maxLength=4,
		maxp=5,
		maxProperties=6,
		min="q",
		mini=7,
		minimum="r",
		minItems=8,
		minl=9,
		minLength=10,
		minp=11,
		minProperties=12,
		mo="s",
		multipleOf="t",
		on="u",
		onClass=X1.class,
		p="v",
		pattern="w",
		properties="x",
		r=true,
		readOnly=true,
		required=true,
		ro=true,
		t="z",
		title="aa",
		type="bb",
		ui=true,
		uniqueItems=true,
		xml="cc"
	)
	public static class D2 {}
	Schema d2 = D2.class.getAnnotationsByType(Schema.class)[0];

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(a1, d1, d2);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEqualsAll(a1.hashCode(), d1.hashCode(), d2.hashCode());
	}
}