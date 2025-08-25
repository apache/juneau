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
import static org.apache.juneau.TestUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.junit.jupiter.api.*;

class ItemsAnnotation_Test extends SimpleTestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	Items a1 = ItemsAnnotation.create()
		.$ref("$ref")
		._default("default")
		._enum("enum")
		.cf("cf")
		.collectionFormat("collectionFormat")
        .description("description")
		.df("df")
		.e("e")
		.emax(true)
		.emin(true)
		.exclusiveMaximum(true)
		.exclusiveMinimum(true)
		.f("f")
		.format("format")
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
		.p("p")
		.pattern("pattern")
		.t("t")
		.type("type")
		.ui(true)
		.uniqueItems(true)
		.build();

	Items a2 = ItemsAnnotation.create()
		.$ref("$ref")
		._default("default")
		._enum("enum")
		.cf("cf")
		.collectionFormat("collectionFormat")
        .description("description")
		.df("df")
		.e("e")
		.emax(true)
		.emin(true)
		.exclusiveMaximum(true)
		.exclusiveMinimum(true)
		.f("f")
		.format("format")
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
		.p("p")
		.pattern("pattern")
		.t("t")
		.type("type")
		.ui(true)
		.uniqueItems(true)
		.build();

	@Test void a01_basic() {
		assertJson(a1, ""
			+ "{"
				+ "'$ref':'$ref',"
				+ "_default:['default'],"
				+ "_enum:['enum'],"
				+ "cf:'cf',"
				+ "collectionFormat:'collectionFormat',"
                + "description:['description'],"
				+ "df:['df'],"
				+ "e:['e'],"
				+ "emax:true,"
				+ "emin:true,"
				+ "exclusiveMaximum:true,"
				+ "exclusiveMinimum:true,"
				+ "f:'f',"
				+ "format:'format',"
				+ "items:{'$ref':'',_default:[],_enum:[],cf:'',collectionFormat:'',description:[],df:[],e:[],emax:false,emin:false,exclusiveMaximum:false,exclusiveMinimum:false,f:'',format:'',items:[],max:'',maxItems:-1,maxLength:-1,maxi:-1,maximum:'',maxl:-1,min:'',minItems:-1,minLength:-1,mini:-1,minimum:'',minl:-1,mo:'',multipleOf:'',p:'',pattern:'',t:'',type:'',ui:false,uniqueItems:false},"
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
				+ "p:'p',"
				+ "pattern:'pattern',"
				+ "t:'t',"
				+ "type:'type',"
				+ "ui:true,"
				+ "uniqueItems:true"
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

	@Items(
		$ref="$ref",
		_default="default",
		_enum="enum",
		cf="cf",
		collectionFormat="collectionFormat",
        description={ "description" },
		df="df",
		e="e",
		emax=true,
		emin=true,
		exclusiveMaximum=true,
		exclusiveMinimum=true,
		f="f",
		format="format",
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
		p="p",
		pattern="pattern",
		t="t",
		type="type",
		ui=true,
		uniqueItems=true
	)
	public static class D1 {}
	Items d1 = D1.class.getAnnotationsByType(Items.class)[0];

	@Items(
		$ref="$ref",
		_default="default",
		_enum="enum",
		cf="cf",
		collectionFormat="collectionFormat",
        description={ "description" },
		df="df",
		e="e",
		emax=true,
		emin=true,
		exclusiveMaximum=true,
		exclusiveMinimum=true,
		f="f",
		format="format",
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
		p="p",
		pattern="pattern",
		t="t",
		type="type",
		ui=true,
		uniqueItems=true
	)
	public static class D2 {}
	Items d2 = D2.class.getAnnotationsByType(Items.class)[0];

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(a1, d1, d2);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEqualsAll(a1.hashCode(), d1.hashCode(), d2.hashCode());
	}
}