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
package org.apache.juneau.bean.openapi3;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.bean.openapi3.OpenApiBuilder.*;
import java.net.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link OpenApiBuilder}.
 */
class OpenApiBuilder_Test extends TestBase {

	/**
	 * Test method for builder methods and marshalling.
	 */
	@Test void a01_builderMethodsAndMarshalling() {
		// Test contact builder
		var contact = contact("a1", "a2", "a3");
		assertJson("{email:'a3',name:'a1',url:'a2'}", contact);

		// Test external documentation builder
		var externalDocs = externalDocumentation("a1", "a2");
		assertJson("{description:'a2',url:'a1'}", externalDocs);

		// Test header info builder
		var headerInfo = headerInfo(schemaInfo("a1"));
		assertJson("{schema:{type:'a1'}}", headerInfo);

		// Test info builder
		var info = info("a1", "a2");
		assertJson("{title:'a1',version:'a2'}", info);

		// Test license builder
		var license = license("a1", URI.create("a2"));
		assertJson("{name:'a1',url:'a2'}", license);

		// Test operation builder
		var operation = operation().setSummary("a1");
		assertJson("{summary:'a1'}", operation);

		// Test parameter builder
		var parameter = parameter("a1", "a2");
		assertJson("{'in':'a1',name:'a2'}", parameter);

		// Test path item builder
		var pathItem = pathItem().setGet(operation().setSummary("a1"));
		assertJson("{get:{summary:'a1'}}", pathItem);

		// Test response builder
		var response = response("a1");
		assertJson("{description:'a1'}", response);

		// Test schema info builder
		var schemaInfo = schemaInfo("a1");
		assertJson("{type:'a1'}", schemaInfo);

		// Test security scheme info builder
		var securitySchemeInfo = securitySchemeInfo("a1");
		assertJson("{type:'a1'}", securitySchemeInfo);

		// Test server builder
		var server = server(URI.create("a1"));
		assertJson("{url:'a1'}", server);

		// Test tag builder
		var tag = tag("a1");
		assertJson("{name:'a1'}", tag);

		// Test openApi builder
		var openApi = openApi().setInfo(info("a1", "a2"));
		assertJson("{info:{title:'a1',version:'a2'},openapi:'3.0.0'}", openApi);

		// Test components builder
		var components = components().setSchemas(map("a1", schemaInfo("a2")));
		assertJson("{schemas:{a1:{type:'a2'}}}", components);
	}

	/**
	 * Test default values.
	 */
	@Test void a02_defaultValues() {
		assertJson("{}", contact());
		assertJson("{}", externalDocumentation());
		assertJson("{}", headerInfo());
		assertJson("{}", info());
		assertJson("{}", license());
		assertJson("{}", operation());
		assertJson("{}", parameter());
		assertJson("{}", pathItem());
		assertJson("{}", response());
		assertJson("{}", schemaInfo());
		assertJson("{}", securityRequirement());
		assertJson("{}", securitySchemeInfo());
		assertJson("{}", server());
		assertJson("{}", tag());
		assertJson("{openapi:'3.0.0'}", openApi());
		assertJson("{}", components());
	}
}