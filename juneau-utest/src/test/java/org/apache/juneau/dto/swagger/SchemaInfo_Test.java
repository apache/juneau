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
package org.apache.juneau.dto.swagger;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.bean.swagger.SwaggerBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.swagger.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link SchemaInfo}.
 */
class SchemaInfo_Test extends SimpleTestBase {

	/**
	 * Test method for getters and setters.
	 */
	@Test void a01_gettersAndSetters() {
		var t = new SchemaInfo();

		// General - Combined assertBean test
		assertBean(
			t.setAdditionalProperties(new SchemaInfo().setType("a")).setAllOf(set("b1","b2")).setDefault("c").setDescription("d")
				.setDiscriminator("e").setEnum(set("f1","f2")).setExample("g").setExclusiveMaximum(true).setExclusiveMinimum(true)
				.setExternalDocs(externalDocumentation("h")).setFormat("i").setItems(items("j")).setMaxItems(1).setMaxLength(2)
				.setMaxProperties(3).setMaximum(4).setMinItems(5).setMinLength(6).setMinProperties(7).setMinimum(8)
				.setMultipleOf(9).setPattern("k").setProperties(map("l",new SchemaInfo().setType("m"))).setReadOnly(true)
				.setRequiredProperties("n").setTitle("o").setType("p").setUniqueItems(true).setXml(xml().setName("q")),
			"additionalProperties{type},allOf,default,description,discriminator,enum,example,exclusiveMaximum,exclusiveMinimum,externalDocs{url},format,items{type},maxItems,maxLength,maxProperties,maximum,minItems,minLength,minProperties,minimum,multipleOf,pattern,properties{l{type}},readOnly,requiredProperties,title,type,uniqueItems,xml{name}",
			"{a},[b1,b2],c,d,e,[f1,f2],g,true,true,{h},i,{j},1,2,3,4,5,6,7,8,9,k,{{m}},true,[n],o,p,true,{q}"
		);

		// Null cases
		assertBean(
			t.setAdditionalProperties((SchemaInfo)null).setAllOf((Collection<Object>)null).setDefault(null).setDescription(null)
				.setDiscriminator(null).setEnum((Collection<Object>)null).setExample(null).setFormat(null)
				.setPattern(null).setProperties((Map<String,SchemaInfo>)null).setRequiredProperties((Collection<String>)null)
				.setTitle(null).setType(null),
			"additionalProperties,allOf,default,description,discriminator,enum,example,format,pattern,properties,requiredProperties,title,type",
			"null,null,null,null,null,null,null,null,null,null,null,null,null"
		);

		// Empty collections
		assertBean(
			t.setAllOf(set()).setProperties(map()),
			"allOf,properties",
			"[],{}"
		);

		// Other
		assertBean(
			t.setDefault(Utils.sb("a")).setExample(1).setMaximum(2f).setMinimum(3f).setMultipleOf(4f),
			"default,example,maximum,minimum,multipleOf", "a,1,2.0,3.0,4.0");
		assertJson("{}", t.setAdditionalProperties(new SchemaInfo()).getAdditionalProperties());

		// addEnum and addAllOf
		assertList(t.addEnum("a","b").getEnum(), "a", "b");
		assertList(t.addEnum("c").getEnum(), "a", "b", "c");
		assertList(t.addAllOf("d","e").getAllOf(), "d", "e");
	}

	/**
	 * Test method for {@link SchemaInfo#set(java.lang.String, java.lang.Object)}.
	 */
	@Test void b01_set() {
		var t = new SchemaInfo();

		t
			.set("additionalProperties", map("c",alist("c1")))
			.set("allOf", set("d"))
			.set("default", "a")
			.set("description", "e")
			.set("discriminator", "f")
			.set("enum", set("b"))
			.set("example", "g")
			.set("exclusiveMaximum", true)
			.set("exclusiveMinimum", true)
			.set("externalDocs", externalDocumentation("h"))
			.set("format", "i")
			.set("items", items("j"))
			.set("maxItems", 2)
			.set("maxLength", 3)
			.set("maxProperties", 4)
			.set("maximum", 1f)
			.set("minItems", 6)
			.set("minLength", 7)
			.set("minProperties", 8)
			.set("minimum", 5f)
			.set("multipleOf", 9f)
			.set("pattern", "k")
			.set("properties", map("l",map("l1", 1)))
			.set("readOnly", true)
			.set("$ref", "p")
			.set("requiredProperties", set("x"))
			.set("title", "m")
			.set("type", "n")
			.set("uniqueItems", true)
			.set("xml", xml().setName("o"));

		assertBean(t,
			"additionalProperties{c},allOf,default,description,discriminator,enum,example,exclusiveMaximum,exclusiveMinimum,externalDocs{url},format,items{type},maxItems,maxLength,maxProperties,maximum,minItems,minLength,minProperties,minimum,multipleOf,pattern,properties{l{l1}},readOnly,ref,requiredProperties,title,type,uniqueItems,xml{name}",
			"{[c1]},[d],a,e,f,[b],g,true,true,{h},i,{j},2,3,4,1.0,6,7,8,5.0,9.0,k,{{1}},true,p,[x],m,n,true,{o}");

		t
			.set("default", "a")
			.set("enum", "['b']")
			.set("additionalProperties", "{c:['c1']}")
			.set("allOf", "['d']")
			.set("description", "e")
			.set("discriminator", "f")
			.set("example", "g")
			.set("exclusiveMaximum", "true")
			.set("exclusiveMinimum", "true")
			.set("externalDocs", "{url:'h'}")
			.set("format", "i")
			.set("items", "{type:'j'}")
			.set("maximum", "1.0")
			.set("maxItems", "2")
			.set("maxLength", "3")
			.set("maxProperties", "4")
			.set("minimum", "5.0")
			.set("minItems", "6")
			.set("minLength", "7")
			.set("minProperties", "8")
			.set("multipleOf", "9.0")
			.set("pattern", "k")
			.set("properties", "{l:{l1:1}}")
			.set("readOnly", "true")
			.set("requiredProperties", "['x']")
			.set("title", "m")
			.set("type", "n")
			.set("uniqueItems", "true")
			.set("xml", "{name:'o'}")
			.set("$ref", "p");

		assertBean(t,
			"format,title,description,default,multipleOf,maximum,exclusiveMaximum,minimum,exclusiveMinimum,maxLength,minLength,pattern,maxItems,minItems,uniqueItems,maxProperties,minProperties,requiredProperties,enum,type,items{type},allOf,properties{l{l1}},additionalProperties{c},discriminator,readOnly,xml{name},externalDocs{url},example,ref",
			"i,m,e,a,9.0,1.0,true,5.0,true,3,7,k,2,6,true,4,8,[x],[b],n,{j},[d],{{1}},{[c1]},f,true,{o},{h},g,p");

		t
			.set("default", Utils.sb("a"))
			.set("enum", Utils.sb("['b']"))
			.set("additionalProperties", Utils.sb("{c:['c1']}"))
			.set("allOf", Utils.sb("['d']"))
			.set("description", Utils.sb("e"))
			.set("discriminator", Utils.sb("f"))
			.set("example", Utils.sb("g"))
			.set("exclusiveMaximum", Utils.sb("true"))
			.set("exclusiveMinimum", Utils.sb("true"))
			.set("externalDocs", Utils.sb("{url:'h'}"))
			.set("format", Utils.sb("i"))
			.set("items", Utils.sb("{type:'j'}"))
			.set("maximum", Utils.sb("1.0"))
			.set("maxItems", Utils.sb("2"))
			.set("maxLength", Utils.sb("3"))
			.set("maxProperties", Utils.sb("4"))
			.set("minimum", Utils.sb("5.0"))
			.set("minItems", Utils.sb("6"))
			.set("minLength", Utils.sb("7"))
			.set("minProperties", Utils.sb("8"))
			.set("multipleOf", Utils.sb("9.0"))
			.set("pattern", Utils.sb("k"))
			.set("properties", Utils.sb("{l:{l1:1}}"))
			.set("readOnly", Utils.sb("true"))
			.set("requiredProperties", Utils.sb("['x']"))
			.set("title", Utils.sb("m"))
			.set("type", Utils.sb("n"))
			.set("uniqueItems", Utils.sb("true"))
			.set("xml", Utils.sb("{name:'o'}"))
			.set("$ref", Utils.sb("p"));

		assertBean(t,
			"format,title,description,default,multipleOf,maximum,exclusiveMaximum,minimum,exclusiveMinimum,maxLength,minLength,pattern,maxItems,minItems,uniqueItems,maxProperties,minProperties,requiredProperties,enum,type,items{type},allOf,properties{l{l1}},additionalProperties{c},discriminator,readOnly,xml{name},externalDocs{url},example,ref",
			"i,m,e,a,9.0,1.0,true,5.0,true,3,7,k,2,6,true,4,8,[x],[b],n,{j},[d],{{1}},{[c1]},f,true,{o},{h},g,p");

		assertMapped(t, (obj,prop) -> obj.get(prop, String.class),
			"default,enum,additionalProperties,allOf,description,discriminator,example,exclusiveMaximum,exclusiveMinimum,externalDocs,format,items,maximum,maxItems,maxLength,maxProperties,minimum,minItems,minLength,minProperties,multipleOf,pattern,properties,readOnly,requiredProperties,title,type,uniqueItems,xml,$ref",
			"a,['b'],{c:['c1']},['d'],e,f,g,true,true,{url:'h'},i,{type:'j'},1.0,2,3,4,5.0,6,7,8,9.0,k,{l:{l1:1}},true,['x'],m,n,true,{name:'o'},p");

		assertMapped(t, (obj,prop) -> obj.get(prop, Object.class).getClass().getSimpleName(),
			"default,enum,additionalProperties,allOf,description,discriminator,example,exclusiveMaximum,exclusiveMinimum,externalDocs,format,items,maximum,maxItems,maxLength,maxProperties,minimum,minItems,minLength,minProperties,multipleOf,pattern,properties,readOnly,requiredProperties,title,type,uniqueItems,xml,$ref",
			"StringBuilder,LinkedHashSet,SchemaInfo,LinkedHashSet,String,String,StringBuilder,Boolean,Boolean,ExternalDocumentation,String,Items,Float,Integer,Integer,Integer,Float,Integer,Integer,Integer,Float,String,LinkedHashMap,Boolean,LinkedHashSet,String,String,Boolean,Xml,String");

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));
	}

	@Test void b02_roundTripJson() {
		var s = "{format:'i',title:'m',description:'e','default':'a',multipleOf:9.0,maximum:1.0,exclusiveMaximum:true,minimum:5.0,exclusiveMinimum:true,maxLength:3,minLength:7,pattern:'k',maxItems:2,minItems:6,uniqueItems:true,maxProperties:4,minProperties:8,requiredProperties:['x'],'enum':['b'],type:'n',items:{type:'j'},allOf:['d'],properties:{l:{l1:1}},additionalProperties:{c:['c1']},discriminator:'f',readOnly:true,xml:{name:'o'},externalDocs:{url:'h'},example:'g','$ref':'p'}";
		assertJson(s, JsonParser.DEFAULT.parse(s, SchemaInfo.class));
	}

	@Test void b03_copy() {
		var t = new SchemaInfo();

		t = t.copy();

		assertJson("{}", t);

		t
			.set("default", "a")
			.set("enum", set("b"))
			.set("additionalProperties", map("c",alist("c1")))
			.set("allOf", set("d"))
			.set("description", "e")
			.set("discriminator", "f")
			.set("example", "g")
			.set("exclusiveMaximum", true)
			.set("exclusiveMinimum", true)
			.set("externalDocs", externalDocumentation("h"))
			.set("format", "i")
			.set("items", items("j"))
			.set("maximum", 1f)
			.set("maxItems", 2)
			.set("maxLength", 3)
			.set("maxProperties", 4)
			.set("minimum", 5f)
			.set("minItems", 6)
			.set("minLength", 7)
			.set("minProperties", 8)
			.set("multipleOf", 9f)
			.set("pattern", "k")
			.set("properties", map("l",map("l1", 1)))
			.set("readOnly", true)
			.set("requiredProperties", set("x"))
			.set("title", "m")
			.set("type", "n")
			.set("uniqueItems", true)
			.set("xml", xml().setName("o"))
			.set("$ref", "p")
			.copy();

		assertBean(t,
			"format,title,description,default,multipleOf,maximum,exclusiveMaximum,minimum,exclusiveMinimum,maxLength,minLength,pattern,maxItems,minItems,uniqueItems,maxProperties,minProperties,requiredProperties,enum,type,items{type},allOf,properties{l{l1}},additionalProperties{c},discriminator,readOnly,xml{name},externalDocs{url},example,ref",
			"i,m,e,a,9.0,1.0,true,5.0,true,3,7,k,2,6,true,4,8,[x],[b],n,{j},[d],{{1}},{[c1]},f,true,{o},{h},g,p");
	}

	@Test void b04_keySet() {
		var t = new SchemaInfo();

		assertEmpty(t.keySet());

		t
			.set("additionalProperties", map("c",alist("c1")))
			.set("allOf", set("d"))
			.set("default", "a")
			.set("description", "e")
			.set("discriminator", "f")
			.set("enum", set("b"))
			.set("example", "g")
			.set("exclusiveMaximum", true)
			.set("exclusiveMinimum", true)
			.set("externalDocs", externalDocumentation("h"))
			.set("format", "i")
			.set("items", items("j"))
			.set("maximum", 1f)
			.set("maxItems", 2)
			.set("maxLength", 3)
			.set("maxProperties", 4)
			.set("minimum", 5f)
			.set("minItems", 6)
			.set("minLength", 7)
			.set("minProperties", 8)
			.set("multipleOf", 9f)
			.set("pattern", "k")
			.set("properties", map("l",map("l1", 1)))
			.set("readOnly", true)
			.set("$ref", "p")
			.set("requiredProperties", set("x"))
			.set("title", "m")
			.set("type", "n")
			.set("uniqueItems", true)
			.set("xml", xml().setName("o"));

		assertList(t.keySet(), "additionalProperties", "allOf", "default", "description", "discriminator", "enum", "example", "exclusiveMaximum", "exclusiveMinimum", "externalDocs", "format", "items", "maximum", "maxItems", "maxLength", "maxProperties", "minimum", "minItems", "minLength", "minProperties", "multipleOf", "pattern", "properties", "readOnly", "$ref", "requiredProperties", "title", "type", "uniqueItems", "xml");
	}
}