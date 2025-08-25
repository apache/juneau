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

import static org.apache.juneau.bean.swagger.SwaggerBuilder.*;
import static org.apache.juneau.TestUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.swagger.*;
import org.apache.juneau.bean.swagger.Tag;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link SwaggerBuilder}.
 */
class SwaggerBuilder_Test extends SimpleTestBase {

	@Test void a01_contact() {
		Contact t = contact();
		assertJson(t, "{}");

		t = contact("foo");
		assertJson(t, "{name:'foo'}");

		t = contact("foo", "bar", "baz");
		assertJson(t, "{name:'foo',url:'bar',email:'baz'}");
	}

	@Test void a02_externalDocumentation() {
		ExternalDocumentation t = externalDocumentation();
		assertJson(t, "{}");

		t = externalDocumentation("foo");
		assertJson(t, "{url:'foo'}");

		t = externalDocumentation("foo", "bar");
		assertJson(t, "{description:'bar',url:'foo'}");
	}

	@Test void a03_headerInfo() {
		HeaderInfo t = headerInfo();
		assertJson(t, "{}");

		t = headerInfo("foo");
		assertJson(t, "{type:'foo'}");

		t = headerInfoStrict("string");
		assertJson(t, "{type:'string'}");
		assertThrowsWithMessage(BasicRuntimeException.class, "Invalid value passed in to setType(String).  Value='foo', valid values=['string','number','integer','boolean','array']", ()->headerInfoStrict("foo"));
	}

	@Test void a04_info() {
		Info t = info();
		assertJson(t, "{}");

		t = info("foo", "bar");
		assertJson(t, "{title:'foo',version:'bar'}");
	}

	@Test void a05_items() {
		Items t = items();
		assertJson(t, "{}");

		t = items("foo");
		assertJson(t, "{type:'foo'}");

		t = itemsStrict("string");
		assertJson(t, "{type:'string'}");
		assertThrowsWithMessage(BasicRuntimeException.class, "Invalid value passed in to setType(String).  Value='foo', valid values=['string','number','integer','boolean','array']", ()->itemsStrict("foo"));
	}

	@Test void a06_license() {
		License t = license();
		assertJson(t, "{}");

		t = license("foo");
		assertJson(t, "{name:'foo'}");
	}

	@Test void a07_operation() {
		Operation t = operation();
		assertJson(t, "{}");
	}

	@Test void a08_parameterInfo() {
		ParameterInfo t = parameterInfo();
		assertJson(t, "{}");

		t = parameterInfo("foo", "bar");
		assertJson(t, "{'in':'foo',name:'bar'}");

		t = parameterInfoStrict("query", "bar");
		assertJson(t, "{'in':'query',name:'bar'}");
		assertThrowsWithMessage(BasicRuntimeException.class, "Invalid value passed in to setIn(String).  Value='foo', valid values=['query','header','path','formData','body']", ()->parameterInfoStrict("foo", "bar"));
	}

	@Test void a09_responseInfo() {
		ResponseInfo t = responseInfo();
		assertJson(t, "{}");

		t = responseInfo("foo");
		assertJson(t, "{description:'foo'}");
	}

	@Test void a10_schemaInfo() {
		SchemaInfo t = schemaInfo();
		assertJson(t, "{}");
	}

	@Test void a11_securityScheme() {
		SecurityScheme t = securityScheme();
		assertJson(t, "{}");

		t = securityScheme("foo");
		assertJson(t, "{type:'foo'}");

		t = securityScheme("foo");
		assertJson(t, "{type:'foo'}");
	}

	@Test void a12_swagger() {
		Swagger t = swagger();
		assertJson(t, "{swagger:'2.0'}");

		t = swagger(info());
		assertJson(t, "{swagger:'2.0',info:{}}");
	}

	@Test void a13_tag() {
		Tag t = tag();
		assertJson(t, "{}");

		t = tag("foo");
		assertJson(t, "{name:'foo'}");
	}

	@Test void a14_xml() {
		Xml t = xml();
		assertJson(t, "{}");
	}
}