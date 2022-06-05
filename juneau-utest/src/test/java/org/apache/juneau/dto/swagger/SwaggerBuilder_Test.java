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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.dto.swagger.SwaggerBuilder.*;
import static org.junit.runners.MethodSorters.*;

import org.junit.*;

/**
 * Testcase for {@link SwaggerBuilder}.
 */
@FixMethodOrder(NAME_ASCENDING)
public class SwaggerBuilder_Test {

	@Test
	public void a01_contact() {
		Contact t = contact();
		assertObject(t).asJson().is("{}");

		t = contact("foo");
		assertObject(t).asJson().is("{name:'foo'}");

		t = contact("foo", "bar", "baz");
		assertObject(t).asJson().is("{name:'foo',url:'bar',email:'baz'}");
	}

	@Test
	public void a02_externalDocumentation() {
		ExternalDocumentation t = externalDocumentation();
		assertObject(t).asJson().is("{}");

		t = externalDocumentation("foo");
		assertObject(t).asJson().is("{url:'foo'}");

		t = externalDocumentation("foo", "bar");
		assertObject(t).asJson().is("{description:'bar',url:'foo'}");
	}

	@Test
	public void a03_headerInfo() {
		HeaderInfo t = headerInfo();
		assertObject(t).asJson().is("{}");

		t = headerInfo("foo");
		assertObject(t).asJson().is("{type:'foo'}");

		t = headerInfoStrict("string");
		assertObject(t).asJson().is("{type:'string'}");
		assertThrown(()->headerInfoStrict("foo")).asMessage().is("Invalid value passed in to setType(String).  Value='foo', valid values=['string','number','integer','boolean','array']");
	}

	@Test
	public void a04_info() {
		Info t = info();
		assertObject(t).asJson().is("{}");

		t = info("foo", "bar");
		assertObject(t).asJson().is("{title:'foo',version:'bar'}");
	}

	@Test
	public void a05_items() {
		Items t = items();
		assertObject(t).asJson().is("{}");

		t = items("foo");
		assertObject(t).asJson().is("{type:'foo'}");

		t = itemsStrict("string");
		assertObject(t).asJson().is("{type:'string'}");
		assertThrown(()->itemsStrict("foo")).asMessage().is("Invalid value passed in to setType(String).  Value='foo', valid values=['string','number','integer','boolean','array']");
	}

	@Test
	public void a06_license() {
		License t = license();
		assertObject(t).asJson().is("{}");

		t = license("foo");
		assertObject(t).asJson().is("{name:'foo'}");
	}

	@Test
	public void a07_operation() {
		Operation t = operation();
		assertObject(t).asJson().is("{}");
	}

	@Test
	public void a08_parameterInfo() {
		ParameterInfo t = parameterInfo();
		assertObject(t).asJson().is("{}");

		t = parameterInfo("foo", "bar");
		assertObject(t).asJson().is("{'in':'foo',name:'bar'}");

		t = parameterInfoStrict("query", "bar");
		assertObject(t).asJson().is("{'in':'query',name:'bar'}");
		assertThrown(()->parameterInfoStrict("foo", "bar")).asMessage().is("Invalid value passed in to setIn(String).  Value='foo', valid values=['query','header','path','formData','body']");
	}

	@Test
	public void a09_responseInfo() {
		ResponseInfo t = responseInfo();
		assertObject(t).asJson().is("{}");

		t = responseInfo("foo");
		assertObject(t).asJson().is("{description:'foo'}");
	}

	@Test
	public void a10_schemaInfo() {
		SchemaInfo t = schemaInfo();
		assertObject(t).asJson().is("{}");
	}

	@Test
	public void a11_securityScheme() {
		SecurityScheme t = securityScheme();
		assertObject(t).asJson().is("{}");

		t = securityScheme("foo");
		assertObject(t).asJson().is("{type:'foo'}");

		t = securityScheme("foo");
		assertObject(t).asJson().is("{type:'foo'}");
	}

	@Test
	public void a12_swagger() {
		Swagger t = swagger();
		assertObject(t).asJson().is("{swagger:'2.0'}");

		t = swagger(info());
		assertObject(t).asJson().is("{swagger:'2.0',info:{}}");
	}

	@Test
	public void a13_tag() {
		Tag t = tag();
		assertObject(t).asJson().is("{}");

		t = tag("foo");
		assertObject(t).asJson().is("{name:'foo'}");
	}

	@Test
	public void a14_xml() {
		Xml t = xml();
		assertObject(t).asJson().is("{}");
	}
}
