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

import static org.apache.juneau.assertions.ObjectAssertion.*;
import static org.apache.juneau.dto.swagger.SwaggerBuilder.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import org.junit.*;

/**
 * Testcase for {@link SwaggerBuilder}.
 */
@FixMethodOrder(NAME_ASCENDING)
public class SwaggerBuilderTest {

	/**
	 * Test method for {@link SwaggerBuilder#contact()}.
	 */
	@Test
	public void testContact() {
		Contact t = contact();
		assertObject(t).json().is("{}");
	}

	/**
	 * Test method for {@link SwaggerBuilder#contact(java.lang.String)}.
	 */
	@Test
	public void testContactString() {
		Contact t = contact("foo");
		assertObject(t).json().is("{name:'foo'}");
	}

	/**
	 * Test method for {@link SwaggerBuilder#contact(java.lang.String, java.lang.Object, java.lang.String)}.
	 */
	@Test
	public void testContactStringObjectString() {
		Contact t = contact("foo", "bar", "baz");
		assertObject(t).json().is("{name:'foo',url:'bar',email:'baz'}");
	}

	/**
	 * Test method for {@link SwaggerBuilder#externalDocumentation()}.
	 */
	@Test
	public void testExternalDocumentation() {
		ExternalDocumentation t = externalDocumentation();
		assertObject(t).json().is("{}");
	}

	/**
	 * Test method for {@link SwaggerBuilder#externalDocumentation(java.lang.Object)}.
	 */
	@Test
	public void testExternalDocumentationObject() {
		ExternalDocumentation t = externalDocumentation("foo");
		assertObject(t).json().is("{url:'foo'}");
	}

	/**
	 * Test method for {@link SwaggerBuilder#externalDocumentation(java.lang.Object, java.lang.String)}.
	 */
	@Test
	public void testExternalDocumentationObjectString() {
		ExternalDocumentation t = externalDocumentation("foo", "bar");
		assertObject(t).json().is("{description:'bar',url:'foo'}");
	}

	/**
	 * Test method for {@link SwaggerBuilder#headerInfo()}.
	 */
	@Test
	public void testHeaderInfo() {
		HeaderInfo t = headerInfo();
		assertObject(t).json().is("{}");
	}

	/**
	 * Test method for {@link SwaggerBuilder#headerInfo(java.lang.String)}.
	 */
	@Test
	public void testHeaderInfoString() {
		HeaderInfo t = headerInfo("foo");
		assertObject(t).json().is("{type:'foo'}");
	}

	/**
	 * Test method for {@link SwaggerBuilder#headerInfoStrict(java.lang.String)}.
	 */
	@Test
	public void testHeaderInfoStrict() {
		HeaderInfo t = headerInfoStrict("string");
		assertObject(t).json().is("{type:'string'}");

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
		assertObject(t).json().is("{}");
	}

	/**
	 * Test method for {@link SwaggerBuilder#info(java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testInfoStringString() {
		Info t = info("foo", "bar");
		assertObject(t).json().is("{title:'foo',version:'bar'}");
	}

	/**
	 * Test method for {@link SwaggerBuilder#items()}.
	 */
	@Test
	public void testItems() {
		Items t = items();
		assertObject(t).json().is("{}");
	}

	/**
	 * Test method for {@link SwaggerBuilder#items(java.lang.String)}.
	 */
	@Test
	public void testItemsString() {
		Items t = items("foo");
		assertObject(t).json().is("{type:'foo'}");
	}

	/**
	 * Test method for {@link SwaggerBuilder#itemsStrict(java.lang.String)}.
	 */
	@Test
	public void testItemsStrict() {
		Items t = itemsStrict("string");
		assertObject(t).json().is("{type:'string'}");

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
		assertObject(t).json().is("{}");
	}

	/**
	 * Test method for {@link SwaggerBuilder#license(java.lang.String)}.
	 */
	@Test
	public void testLicenseString() {
		License t = license("foo");
		assertObject(t).json().is("{name:'foo'}");
	}

	/**
	 * Test method for {@link SwaggerBuilder#operation()}.
	 */
	@Test
	public void testOperation() {
		Operation t = operation();
		assertObject(t).json().is("{}");
	}

	/**
	 * Test method for {@link SwaggerBuilder#parameterInfo()}.
	 */
	@Test
	public void testParameterInfo() {
		ParameterInfo t = parameterInfo();
		assertObject(t).json().is("{}");
	}

	/**
	 * Test method for {@link SwaggerBuilder#parameterInfo(java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testParameterInfoStringString() {
		ParameterInfo t = parameterInfo("foo", "bar");
		assertObject(t).json().is("{'in':'foo',name:'bar'}");
	}

	/**
	 * Test method for {@link SwaggerBuilder#parameterInfoStrict(java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testParameterInfoStrict() {
		ParameterInfo t = parameterInfoStrict("query", "bar");
		assertObject(t).json().is("{'in':'query',name:'bar'}");

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
		assertObject(t).json().is("{}");
	}

	/**
	 * Test method for {@link SwaggerBuilder#responseInfo(java.lang.String)}.
	 */
	@Test
	public void testResponseInfoString() {
		ResponseInfo t = responseInfo("foo");
		assertObject(t).json().is("{description:'foo'}");
	}

	/**
	 * Test method for {@link SwaggerBuilder#schemaInfo()}.
	 */
	@Test
	public void testSchemaInfo() {
		SchemaInfo t = schemaInfo();
		assertObject(t).json().is("{}");
	}

	/**
	 * Test method for {@link SwaggerBuilder#securityScheme()}.
	 */
	@Test
	public void testSecurityScheme() {
		SecurityScheme t = securityScheme();
		assertObject(t).json().is("{}");
	}

	/**
	 * Test method for {@link SwaggerBuilder#securityScheme(java.lang.String)}.
	 */
	@Test
	public void testSecuritySchemeString() {
		SecurityScheme t = securityScheme("foo");
		assertObject(t).json().is("{type:'foo'}");
	}

	/**
	 * Test method for {@link SwaggerBuilder#securitySchemeStrict(java.lang.String)}.
	 */
	@Test
	public void testSecuritySchemeStrict() {
		SecurityScheme t = securityScheme("foo");
		assertObject(t).json().is("{type:'foo'}");
	}

	/**
	 * Test method for {@link SwaggerBuilder#swagger()}.
	 */
	@Test
	public void testSwagger() {
		Swagger t = swagger();
		assertObject(t).json().is("{swagger:'2.0'}");
	}

	/**
	 * Test method for {@link SwaggerBuilder#swagger(org.apache.juneau.dto.swagger.Info)}.
	 */
	@Test
	public void testSwaggerInfo() {
		Swagger t = swagger(info());
		assertObject(t).json().is("{swagger:'2.0',info:{}}");
	}

	/**
	 * Test method for {@link SwaggerBuilder#tag()}.
	 */
	@Test
	public void testTag() {
		Tag t = tag();
		assertObject(t).json().is("{}");
	}

	/**
	 * Test method for {@link SwaggerBuilder#tag(java.lang.String)}.
	 */
	@Test
	public void testTagString() {
		Tag t = tag("foo");
		assertObject(t).json().is("{name:'foo'}");
	}

	/**
	 * Test method for {@link SwaggerBuilder#xml()}.
	 */
	@Test
	public void testXml() {
		Xml t = xml();
		assertObject(t).json().is("{}");
	}
}
