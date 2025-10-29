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
package org.apache.juneau.bean.swagger;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.bean.swagger.SwaggerBuilder.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link SwaggerBuilder}.
 * Ensures Swagger beans are serialized to proper JSON.
 */
class SwaggerBuilder_Test extends TestBase {

	@Test void a01_contact() {
		var t = contact();
		assertJson("{}", t);

		t = contact("foo");
		assertJson("{name:'foo'}", t);

		t = contact("foo", "bar", "baz");
		assertJson("{email:'baz',name:'foo',url:'bar'}", t);
	}

	@Test void a02_externalDocumentation() {
		var t = externalDocumentation();
		assertJson("{}", t);

		t = externalDocumentation("foo");
		assertJson("{url:'foo'}", t);

		t = externalDocumentation("foo", "bar");
		assertJson("{description:'bar',url:'foo'}", t);
	}

	@Test void a03_headerInfo() {
		var t = headerInfo();
		assertJson("{}", t);

		t = headerInfo("foo");
		assertJson("{type:'foo'}", t);

		t = headerInfoStrict("string");
		assertJson("{type:'string'}", t);
		assertThrowsWithMessage(RuntimeException.class, "Invalid value passed in to setType(String).  Value='foo', valid values=['string','number','integer','boolean','array']", ()->headerInfoStrict("foo"));
	}

	@Test void a04_info() {
		var t = info();
		assertJson("{}", t);

		t = info("foo", "bar");
		assertJson("{title:'foo',version:'bar'}", t);
	}

	@Test void a05_items() {
		var t = items();
		assertJson("{}", t);

		t = items("foo");
		assertJson("{type:'foo'}", t);

		t = itemsStrict("string");
		assertJson("{type:'string'}", t);
		assertThrowsWithMessage(RuntimeException.class, "Invalid value passed in to setType(String).  Value='foo', valid values=['string','number','integer','boolean','array']", ()->itemsStrict("foo"));
	}

	@Test void a06_license() {
		var t = license();
		assertJson("{}", t);

		t = license("foo");
		assertJson("{name:'foo'}", t);
	}

	@Test void a07_operation() {
		var t = operation();
		assertJson("{}", t);
	}

	@Test void a08_parameterInfo() {
		var t = parameterInfo();
		assertJson("{}", t);

		t = parameterInfo("foo", "bar");
		assertJson("{'in':'foo',name:'bar'}", t);

		t = parameterInfoStrict("query", "bar");
		assertJson("{'in':'query',name:'bar'}", t);
		assertThrowsWithMessage(RuntimeException.class, "Invalid value passed in to setIn(String).  Value='foo', valid values=['query','header','path','formData','body']", ()->parameterInfoStrict("foo", "bar"));
	}

	@Test void a09_responseInfo() {
		var t = responseInfo();
		assertJson("{}", t);

		t = responseInfo("foo");
		assertJson("{description:'foo'}", t);
	}

	@Test void a10_schemaInfo() {
		var t = schemaInfo();
		assertJson("{}", t);
	}

	@Test void a11_securityScheme() {
		var t = securityScheme();
		assertJson("{}", t);

		t = securityScheme("foo");
		assertJson("{type:'foo'}", t);

		t = securityScheme("foo");
		assertJson("{type:'foo'}", t);
	}

	@Test void a12_swagger() {
		var t = swagger();
		assertJson("{swagger:'2.0'}", t);

		t = swagger(info());
		assertJson("{info:{},swagger:'2.0'}", t);
	}

	@Test void a13_tag() {
		var t = tag();
		assertJson("{}", t);

		t = tag("foo");
		assertJson("{name:'foo'}", t);
	}

	@Test void a14_xml() {
		var t = xml();
		assertJson("{}", t);
	}

	@Test void a15_license() {
		var t = license();
		assertJson("{}", t);

		t = license("MIT");
		assertJson("{name:'MIT'}", t);

		t = license("MIT", java.net.URI.create("https://opensource.org/licenses/MIT"));
		assertJson("{name:'MIT',url:'https://opensource.org/licenses/MIT'}", t);
	}

	@Test void a16_operation() {
		var t = operation();
		assertJson("{}", t);
	}

	@Test void a17_operationMap() {
		var t = operationMap();
		assertJson("{}", t);
	}

	@Test void a18_itemsStrict() {
		var t = itemsStrict("string");
		assertJson("{type:'string'}", t);
		assertThrowsWithMessage(RuntimeException.class, "Invalid value passed in to setType(String).  Value='foo', valid values=['string','number','integer','boolean','array']", ()->itemsStrict("foo"));
	}

	@Test void a19_securitySchemeStrict() {
		var t = securitySchemeStrict("basic");
		assertJson("{type:'basic'}", t);
		assertThrowsWithMessage(RuntimeException.class, "Invalid value passed in to setType(String).  Value='foo', valid values=['basic','apiKey','oauth2']", ()->securitySchemeStrict("foo"));
	}
}