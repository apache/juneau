// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file		*
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance		    *
// * with the License.  You may obtain a copy of the License at														      *
// *																														 *
// *  http://www.apache.org/licenses/LICENSE-2.0																		     *
// *																														 *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the		*
// * specific language governing permissions and limitations under the License.										      *
// ***************************************************************************************************************************
package org.apache.juneau.jsonschema.annotation;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.junit.jupiter.api.*;

class SubItemsAnnotation_Test extends SimpleTestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	SubItems a1 = SubItemsAnnotation.create()
		.$ref("a")
		._default("b")
		._enum("c")
		.cf("d")
		.collectionFormat("e")
		.description("f")
		.df("g")
		.e("h")
		.emax(true)
		.emin(true)
		.exclusiveMaximum(true)
		.exclusiveMinimum(true)
		.f("i")
		.format("j")
		.max("k")
		.maxi(1)
		.maximum("l")
		.maxItems(2)
		.maxl(3)
		.maxLength(4)
		.min("m")
		.mini(5)
		.minimum("n")
		.minItems(6)
		.minl(7)
		.minLength(8)
		.mo("o")
		.multipleOf("p")
		.p("q")
		.pattern("r")
		.t("s")
		.type("t")
		.ui(true)
		.uniqueItems(true)
		.build();

	SubItems a2 = SubItemsAnnotation.create()
		.$ref("a")
		._default("b")
		._enum("c")
		.cf("d")
		.collectionFormat("e")
		.description("f")
		.df("g")
		.e("h")
		.emax(true)
		.emin(true)
		.exclusiveMaximum(true)
		.exclusiveMinimum(true)
		.f("i")
		.format("j")
		.max("k")
		.maxi(1)
		.maximum("l")
		.maxItems(2)
		.maxl(3)
		.maxLength(4)
		.min("m")
		.mini(5)
		.minimum("n")
		.minItems(6)
		.minl(7)
		.minLength(8)
		.mo("o")
		.multipleOf("p")
		.p("q")
		.pattern("r")
		.t("s")
		.type("t")
		.ui(true)
		.uniqueItems(true)
		.build();

	@Test void a01_basic() {
		assertBean(a1, "$ref,_default,_enum,cf,collectionFormat,description,df,e,emax,emin,exclusiveMaximum,exclusiveMinimum,f,format,items,max,maxItems,maxLength,maxi,maximum,maxl,min,minItems,minLength,mini,minimum,minl,mo,multipleOf,p,pattern,t,type,ui,uniqueItems", "a,[b],[c],d,e,[f],[g],[h],true,true,true,true,i,j,[],k,2,4,1,l,3,m,6,8,5,n,7,o,p,q,r,s,t,true,true");
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

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@SubItems(
		$ref="a",
		_default="b",
		_enum="c",
		cf="d",
		collectionFormat="e",
		description={ "f" },
		df="g",
		e="h",
		emax=true,
		emin=true,
		exclusiveMaximum=true,
		exclusiveMinimum=true,
		f="i",
		format="j",
		max="k",
		maxi=1,
		maximum="l",
		maxItems=2,
		maxl=3,
		maxLength=4,
		min="m",
		mini=5,
		minimum="n",
		minItems=6,
		minl=7,
		minLength=8,
		mo="o",
		multipleOf="p",
		p="q",
		pattern="r",
		t="s",
		type="t",
		ui=true,
		uniqueItems=true
	)
	public static class D1 {}
	SubItems d1 = D1.class.getAnnotationsByType(SubItems.class)[0];

	@SubItems(
		$ref="a",
		_default="b",
		_enum="c",
		cf="d",
		collectionFormat="e",
		description={ "f" },
		df="g",
		e="h",
		emax=true,
		emin=true,
		exclusiveMaximum=true,
		exclusiveMinimum=true,
		f="i",
		format="j",
		max="k",
		maxi=1,
		maximum="l",
		maxItems=2,
		maxl=3,
		maxLength=4,
		min="m",
		mini=5,
		minimum="n",
		minItems=6,
		minl=7,
		minLength=8,
		mo="o",
		multipleOf="p",
		p="q",
		pattern="r",
		t="s",
		type="t",
		ui=true,
		uniqueItems=true
	)
	public static class D2 {}
	SubItems d2 = D2.class.getAnnotationsByType(SubItems.class)[0];

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(a1, d1, d2);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEqualsAll(a1.hashCode(), d1.hashCode(), d2.hashCode());
	}
}