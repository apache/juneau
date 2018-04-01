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
import static org.apache.juneau.dto.swagger.SwaggerBuilder.*;
import static org.junit.Assert.*;

import org.junit.*;

/**
 * Testcase for {@link SwaggerBuilder}.
 */
public class SwaggerBuilderTest {

	/**
	 * Test method for {@link SwaggerBuilder#contact()}.
	 */
	@Test
	public void testContact() {
		Contact t = contact();
		assertObjectEquals("{}", t);
	}

	/**
	 * Test method for {@link SwaggerBuilder#contact(java.lang.String)}.
	 */
	@Test
	public void testContactString() {
		Contact t = contact("foo");
		assertObjectEquals("{name:'foo'}", t);
	}

	/**
	 * Test method for {@link SwaggerBuilder#contact(java.lang.String, java.lang.Object, java.lang.String)}.
	 */
	@Test
	public void testContactStringObjectString() {
		Contact t = contact("foo", "bar", "baz");
		assertObjectEquals("{name:'foo',url:'bar',email:'baz'}", t);
	}

	/**
	 * Test method for {@link SwaggerBuilder#externalDocumentation()}.
	 */
	@Test
	public void testExternalDocumentation() {
		ExternalDocumentation t = externalDocumentation();
		assertObjectEquals("{}", t);
	}

	/**
	 * Test method for {@link SwaggerBuilder#externalDocumentation(java.lang.Object)}.
	 */
	@Test
	public void testExternalDocumentationObject() {
		ExternalDocumentation t = externalDocumentation("foo");
		assertObjectEquals("{url:'foo'}", t);
	}

	/**
	 * Test method for {@link SwaggerBuilder#externalDocumentation(java.lang.Object, java.lang.String)}.
	 */
	@Test
	public void testExternalDocumentationObjectString() {
		ExternalDocumentation t = externalDocumentation("foo", "bar");
		assertObjectEquals("{description:'bar',url:'foo'}", t);
	}

	/**
	 * Test method for {@link SwaggerBuilder#headerInfo()}.
	 */
	@Test
	public void testHeaderInfo() {
		HeaderInfo t = headerInfo();
		assertObjectEquals("{}", t);
	}

	/**
	 * Test method for {@link SwaggerBuilder#headerInfo(java.lang.String)}.
	 */
	@Test
	public void testHeaderInfoString() {
		HeaderInfo t = headerInfo("foo");
		assertObjectEquals("{type:'foo'}", t);
	}

	/**
	 * Test method for {@link SwaggerBuilder#headerInfoStrict(java.lang.String)}.
	 */
	@Test
	public void testHeaderInfoStrict() {
		HeaderInfo t = headerInfoStrict("string");
		assertObjectEquals("{type:'string'}", t);
		
		try {
			headerInfoStrict("foo");
		} catch (Exception e) {
			assertEquals("Invalid value passed in to setType(String).  Value='foo', valid values=['string','number','integer','boolean','array']", e.getLocalizedMessage());
		}
	}

	/**
	 * Test method for {@link SwaggerBuilder#info()}.
	 */
	@Test
	public void testInfo() {
		Info t = info();
		assertObjectEquals("{}", t);
	}

	/**
	 * Test method for {@link SwaggerBuilder#info(java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testInfoStringString() {
		Info t = info("foo", "bar");
		assertObjectEquals("{title:'foo',version:'bar'}", t);
	}

	/**
	 * Test method for {@link SwaggerBuilder#items()}.
	 */
	@Test
	public void testItems() {
		Items t = items();
		assertObjectEquals("{}", t);
	}

	/**
	 * Test method for {@link SwaggerBuilder#items(java.lang.String)}.
	 */
	@Test
	public void testItemsString() {
		Items t = items("foo");
		assertObjectEquals("{type:'foo'}", t);
	}

	/**
	 * Test method for {@link SwaggerBuilder#itemsStrict(java.lang.String)}.
	 */
	@Test
	public void testItemsStrict() {
		Items t = itemsStrict("string");
		assertObjectEquals("{type:'string'}", t);
		
		try {
			itemsStrict("foo");
		} catch (Exception e) {
			assertEquals("Invalid value passed in to setType(String).  Value='foo', valid values=['string','number','integer','boolean','array']", e.getLocalizedMessage());
		}
	}

	/**
	 * Test method for {@link SwaggerBuilder#license()}.
	 */
	@Test
	public void testLicense() {
		License t = license();
		assertObjectEquals("{}", t);
	}

	/**
	 * Test method for {@link SwaggerBuilder#license(java.lang.String)}.
	 */
	@Test
	public void testLicenseString() {
		License t = license("foo");
		assertObjectEquals("{name:'foo'}", t);
	}

	/**
	 * Test method for {@link SwaggerBuilder#operation()}.
	 */
	@Test
	public void testOperation() {
		Operation t = operation();
		assertObjectEquals("{}", t);
	}

	/**
	 * Test method for {@link SwaggerBuilder#parameterInfo()}.
	 */
	@Test
	public void testParameterInfo() {
		ParameterInfo t = parameterInfo();
		assertObjectEquals("{}", t);
	}

	/**
	 * Test method for {@link SwaggerBuilder#parameterInfo(java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testParameterInfoStringString() {
		ParameterInfo t = parameterInfo("foo", "bar");
		assertObjectEquals("{'in':'foo',name:'bar'}", t);
	}

	/**
	 * Test method for {@link SwaggerBuilder#parameterInfoStrict(java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testParameterInfoStrict() {
		ParameterInfo t = parameterInfoStrict("query", "bar");
		assertObjectEquals("{'in':'query',name:'bar'}", t);
		
		try {
			parameterInfoStrict("foo", "bar");
		} catch (Exception e) {
			assertEquals("Invalid value passed in to setIn(String).  Value='foo', valid values=['query','header','path','formData','body']", e.getLocalizedMessage());
		}
	}

	/**
	 * Test method for {@link SwaggerBuilder#responseInfo()}.
	 */
	@Test
	public void testResponseInfo() {
		ResponseInfo t = responseInfo();
		assertObjectEquals("{}", t);
	}

	/**
	 * Test method for {@link SwaggerBuilder#responseInfo(java.lang.String)}.
	 */
	@Test
	public void testResponseInfoString() {
		ResponseInfo t = responseInfo("foo");
		assertObjectEquals("{description:'foo'}", t);
	}

	/**
	 * Test method for {@link SwaggerBuilder#schemaInfo()}.
	 */
	@Test
	public void testSchemaInfo() {
		SchemaInfo t = schemaInfo();
		assertObjectEquals("{}", t);
	}

	/**
	 * Test method for {@link SwaggerBuilder#securityScheme()}.
	 */
	@Test
	public void testSecurityScheme() {
		SecurityScheme t = securityScheme();
		assertObjectEquals("{}", t);
	}

	/**
	 * Test method for {@link SwaggerBuilder#securityScheme(java.lang.String)}.
	 */
	@Test
	public void testSecuritySchemeString() {
		SecurityScheme t = securityScheme("foo");
		assertObjectEquals("{type:'foo'}", t);
	}

	/**
	 * Test method for {@link SwaggerBuilder#securitySchemeStrict(java.lang.String)}.
	 */
	@Test
	public void testSecuritySchemeStrict() {
		SecurityScheme t = securityScheme("foo");
		assertObjectEquals("{type:'foo'}", t);
	}

	/**
	 * Test method for {@link SwaggerBuilder#swagger()}.
	 */
	@Test
	public void testSwagger() {
		Swagger t = swagger();
		assertObjectEquals("{swagger:'2.0'}", t);
	}

	/**
	 * Test method for {@link SwaggerBuilder#swagger(org.apache.juneau.dto.swagger.Info)}.
	 */
	@Test
	public void testSwaggerInfo() {
		Swagger t = swagger(info());
		assertObjectEquals("{swagger:'2.0',info:{}}", t);
	}

	/**
	 * Test method for {@link SwaggerBuilder#tag()}.
	 */
	@Test
	public void testTag() {
		Tag t = tag();
		assertObjectEquals("{}", t);
	}

	/**
	 * Test method for {@link SwaggerBuilder#tag(java.lang.String)}.
	 */
	@Test
	public void testTagString() {
		Tag t = tag("foo");
		assertObjectEquals("{name:'foo'}", t);
	}

	/**
	 * Test method for {@link SwaggerBuilder#xml()}.
	 */
	@Test
	public void testXml() {
		Xml t = xml();
		assertObjectEquals("{}", t);
	}
}
