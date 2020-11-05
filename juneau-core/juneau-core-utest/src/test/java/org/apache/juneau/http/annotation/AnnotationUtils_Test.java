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
package org.apache.juneau.http.annotation;

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.http.annotation.AnnotationUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;

import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.oapi.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class AnnotationUtils_Test {

	//-----------------------------------------------------------------------------------------------------------------
	// Test empty checks
	//-----------------------------------------------------------------------------------------------------------------

	@Target(TYPE)
	@Retention(RUNTIME)
	public @interface X1 {
		Contact contact() default @Contact;
		ExternalDocs externalDocs() default @ExternalDocs;
		License license() default @License;
		Schema schema() default @Schema;
		SubItems subItems() default @SubItems;
		Items items() default @Items;
	}

	@Body
	@Response
	@ResponseHeader
	@X1
	public static class A1 {
		@Query @Header @FormData @Path @Schema
		public int f1;
	}

	public static class A2 {
		public int f1;
	}

	@Test
	public void a01_Body() throws Exception {
		assertObject(body().build().annotationType()).json().contains("Body");

		assertTrue(empty(A1.class.getAnnotation(Body.class)));
		assertTrue(empty(A2.class.getAnnotation(Body.class)));
		assertTrue(empty(body().build()));
		assertTrue(empty((Body)null));

		assertFalse(empty(body().api(a("foo")).build()));
		assertFalse(empty(body().d(a("foo")).build()));
		assertFalse(empty(body().description(a("foo")).build()));
		assertFalse(empty(body().ex(a("foo")).build()));
		assertFalse(empty(body().example(a("foo")).build()));
		assertFalse(empty(body().examples(a("foo")).build()));
		assertFalse(empty(body().exs(a("foo")).build()));
		assertFalse(empty(body().required(true).build()));
		assertFalse(empty(body().r(true).build()));
		assertFalse(empty(body().schema(schema().$ref("foo").build()).build()));
		assertFalse(empty(body().value(a("foo")).build()));
	}

	@Test
	public void a02_Contact() throws Exception {
		X1 x1 = A1.class.getAnnotation(X1.class);

		assertObject(contact().build().annotationType()).json().contains("Contact");

		assertTrue(empty(x1.contact()));
		assertTrue(empty(contact().build()));
		assertTrue(empty((Contact)null));

		assertFalse(empty(contact().email("foo").build()));
		assertFalse(empty(contact().name("foo").build()));
		assertFalse(empty(contact().url("foo").build()));
		assertFalse(empty(contact().value(a("foo")).build()));
	}

	@Test
	public void a03_FormData() throws Exception {
		Field f1 = A1.class.getField("f1");
		Field f2 = A2.class.getField("f1");

		assertObject(formData().build().annotationType()).json().contains("FormData");

		assertTrue(empty(f1.getAnnotation(FormData.class)));
		assertTrue(empty(f2.getAnnotation(FormData.class)));
		assertTrue(empty((FormData)null));
		assertTrue(empty(formData().build()));

		assertFalse(empty(formData()._default(a("foo")).build()));
		assertFalse(empty(formData()._enum(a("foo")).build()));
		assertFalse(empty(formData().aev(true).build()));
		assertFalse(empty(formData().allowEmptyValue(true).build()));
		assertFalse(empty(formData().api(a("foo")).build()));
		assertFalse(empty(formData().cf("foo").build()));
		assertFalse(empty(formData().collectionFormat("foo").build()));
		assertFalse(empty(formData().d(a("foo")).build()));
		assertFalse(empty(formData().description(a("foo")).build()));
		assertFalse(empty(formData().df(a("foo")).build()));
		assertFalse(empty(formData().e(a("foo")).build()));
		assertFalse(empty(formData().emax(true).build()));
		assertFalse(empty(formData().emin(true).build()));
		assertFalse(empty(formData().ex(a("foo")).build()));
		assertFalse(empty(formData().example(a("foo")).build()));
		assertFalse(empty(formData().exclusiveMaximum(true).build()));
		assertFalse(empty(formData().exclusiveMinimum(true).build()));
		assertFalse(empty(formData().f("foo").build()));
		assertFalse(empty(formData().format("foo").build()));
		assertFalse(empty(formData().items(items().$ref("foo").build()).build()));
		assertFalse(empty(formData().max("foo").build()));
		assertFalse(empty(formData().maxi(0).build()));
		assertFalse(empty(formData().maximum("foo").build()));
		assertFalse(empty(formData().maxItems(0).build()));
		assertFalse(empty(formData().maxl(0).build()));
		assertFalse(empty(formData().maxLength(0).build()));
		assertFalse(empty(formData().min("foo").build()));
		assertFalse(empty(formData().mini(0).build()));
		assertFalse(empty(formData().minimum("foo").build()));
		assertFalse(empty(formData().minItems(0).build()));
		assertFalse(empty(formData().minl(0).build()));
		assertFalse(empty(formData().minLength(0).build()));
		assertFalse(empty(formData().mo("foo").build()));
		assertFalse(empty(formData().multi(true).build()));
		assertFalse(empty(formData().multipleOf("foo").build()));
		assertFalse(empty(formData().n("foo").build()));
		assertFalse(empty(formData().name("foo").build()));
		assertFalse(empty(formData().p("foo").build()));
		assertFalse(empty(formData().parser(OpenApiParser.class).build()));
		assertFalse(empty(formData().pattern("foo").build()));
		assertFalse(empty(formData().r(true).build()));
		assertFalse(empty(formData().required(true).build()));
		assertFalse(empty(formData().serializer(OpenApiSerializer.class).build()));
		assertFalse(empty(formData().sie(true).build()));
		assertFalse(empty(formData().skipIfEmpty(true).build()));
		assertFalse(empty(formData().t("foo").build()));
		assertFalse(empty(formData().type("foo").build()));
		assertFalse(empty(formData().ui(true).build()));
		assertFalse(empty(formData().uniqueItems(true).build()));
		assertFalse(empty(formData().value("foo").build()));
	}

	@Test
	public void a04_HasFormData() throws Exception {
		assertObject(hasFormData().build().annotationType()).json().contains("HasFormData");

		assertObject(hasFormData().n("foo").build().n()).json().is("'foo'");
		assertObject(hasFormData().name("foo").build().name()).json().is("'foo'");
		assertObject(hasFormData().value("foo").build().value()).json().is("'foo'");
	}

	@Test
	public void a05_Query() throws Exception {
		Field f1 = A1.class.getField("f1");
		Field f2 = A2.class.getField("f1");

		assertObject(query().build().annotationType()).json().contains("Query");

		assertTrue(empty(f1.getAnnotation(Query.class)));
		assertTrue(empty(f2.getAnnotation(Query.class)));
		assertTrue(empty((Query)null));
		assertTrue(empty(query().build()));

		assertFalse(empty(query()._default(a("foo")).build()));
		assertFalse(empty(query()._enum(a("foo")).build()));
		assertFalse(empty(query().aev(true).build()));
		assertFalse(empty(query().allowEmptyValue(true).build()));
		assertFalse(empty(query().api(a("foo")).build()));
		assertFalse(empty(query().cf("foo").build()));
		assertFalse(empty(query().collectionFormat("foo").build()));
		assertFalse(empty(query().d(a("foo")).build()));
		assertFalse(empty(query().description(a("foo")).build()));
		assertFalse(empty(query().df(a("foo")).build()));
		assertFalse(empty(query().e(a("foo")).build()));
		assertFalse(empty(query().emax(true).build()));
		assertFalse(empty(query().emin(true).build()));
		assertFalse(empty(query().ex(a("foo")).build()));
		assertFalse(empty(query().example(a("foo")).build()));
		assertFalse(empty(query().exclusiveMaximum(true).build()));
		assertFalse(empty(query().exclusiveMinimum(true).build()));
		assertFalse(empty(query().f("foo").build()));
		assertFalse(empty(query().format("foo").build()));
		assertFalse(empty(query().items(items().$ref("foo").build()).build()));
		assertFalse(empty(query().max("foo").build()));
		assertFalse(empty(query().maxi(0).build()));
		assertFalse(empty(query().maximum("foo").build()));
		assertFalse(empty(query().maxItems(0).build()));
		assertFalse(empty(query().maxl(0).build()));
		assertFalse(empty(query().maxLength(0).build()));
		assertFalse(empty(query().min("foo").build()));
		assertFalse(empty(query().mini(0).build()));
		assertFalse(empty(query().minimum("foo").build()));
		assertFalse(empty(query().minItems(0).build()));
		assertFalse(empty(query().minl(0).build()));
		assertFalse(empty(query().minLength(0).build()));
		assertFalse(empty(query().mo("foo").build()));
		assertFalse(empty(query().multi(true).build()));
		assertFalse(empty(query().multipleOf("foo").build()));
		assertFalse(empty(query().n("foo").build()));
		assertFalse(empty(query().name("foo").build()));
		assertFalse(empty(query().p("foo").build()));
		assertFalse(empty(query().parser(OpenApiParser.class).build()));
		assertFalse(empty(query().pattern("foo").build()));
		assertFalse(empty(query().r(true).build()));
		assertFalse(empty(query().required(true).build()));
		assertFalse(empty(query().serializer(OpenApiSerializer.class).build()));
		assertFalse(empty(query().sie(true).build()));
		assertFalse(empty(query().skipIfEmpty(true).build()));
		assertFalse(empty(query().t("foo").build()));
		assertFalse(empty(query().type("foo").build()));
		assertFalse(empty(query().ui(true).build()));
		assertFalse(empty(query().uniqueItems(true).build()));
		assertFalse(empty(query().value("foo").build()));
	}

	@Test
	public void a06_HasQuery() throws Exception {
		assertObject(hasQuery().build().annotationType()).json().contains("HasQuery");

		assertObject(hasQuery().n("foo").build().n()).json().is("'foo'");
		assertObject(hasQuery().name("foo").build().name()).json().is("'foo'");
		assertObject(hasQuery().value("foo").build().value()).json().is("'foo'");
	}

	@Test
	public void a07_Header() throws Exception {
		Field f1 = A1.class.getField("f1");
		Field f2 = A2.class.getField("f1");

		assertObject(header().build().annotationType()).json().contains("Header");

		assertTrue(empty(f1.getAnnotation(Header.class)));
		assertTrue(empty(f2.getAnnotation(Header.class)));
		assertTrue(empty((Header)null));
		assertTrue(empty(header().build()));

		assertFalse(empty(header()._default(a("foo")).build()));
		assertFalse(empty(header()._enum(a("foo")).build()));
		assertFalse(empty(header().aev(true).build()));
		assertFalse(empty(header().allowEmptyValue(true).build()));
		assertFalse(empty(header().api(a("foo")).build()));
		assertFalse(empty(header().cf("foo").build()));
		assertFalse(empty(header().collectionFormat("foo").build()));
		assertFalse(empty(header().d(a("foo")).build()));
		assertFalse(empty(header().description(a("foo")).build()));
		assertFalse(empty(header().df(a("foo")).build()));
		assertFalse(empty(header().e(a("foo")).build()));
		assertFalse(empty(header().emax(true).build()));
		assertFalse(empty(header().emin(true).build()));
		assertFalse(empty(header().ex(a("foo")).build()));
		assertFalse(empty(header().example(a("foo")).build()));
		assertFalse(empty(header().exclusiveMaximum(true).build()));
		assertFalse(empty(header().exclusiveMinimum(true).build()));
		assertFalse(empty(header().f("foo").build()));
		assertFalse(empty(header().format("foo").build()));
		assertFalse(empty(header().items(items().$ref("foo").build()).build()));
		assertFalse(empty(header().max("foo").build()));
		assertFalse(empty(header().maxi(0).build()));
		assertFalse(empty(header().maximum("foo").build()));
		assertFalse(empty(header().maxItems(0).build()));
		assertFalse(empty(header().maxl(0).build()));
		assertFalse(empty(header().maxLength(0).build()));
		assertFalse(empty(header().min("foo").build()));
		assertFalse(empty(header().mini(0).build()));
		assertFalse(empty(header().minimum("foo").build()));
		assertFalse(empty(header().minItems(0).build()));
		assertFalse(empty(header().minl(0).build()));
		assertFalse(empty(header().minLength(0).build()));
		assertFalse(empty(header().mo("foo").build()));
		assertFalse(empty(header().multi(true).build()));
		assertFalse(empty(header().multipleOf("foo").build()));
		assertFalse(empty(header().n("foo").build()));
		assertFalse(empty(header().name("foo").build()));
		assertFalse(empty(header().p("foo").build()));
		assertFalse(empty(header().parser(OpenApiParser.class).build()));
		assertFalse(empty(header().pattern("foo").build()));
		assertFalse(empty(header().r(true).build()));
		assertFalse(empty(header().required(true).build()));
		assertFalse(empty(header().serializer(OpenApiSerializer.class).build()));
		assertFalse(empty(header().sie(true).build()));
		assertFalse(empty(header().skipIfEmpty(true).build()));
		assertFalse(empty(header().t("foo").build()));
		assertFalse(empty(header().type("foo").build()));
		assertFalse(empty(header().ui(true).build()));
		assertFalse(empty(header().uniqueItems(true).build()));
		assertFalse(empty(header().value("foo").build()));
	}

	@Test
	public void a08_License() throws Exception {
		X1 x = A1.class.getAnnotation(X1.class);

		assertObject(license().build().annotationType()).json().contains("License");

		assertTrue(empty(x.license()));
		assertTrue(empty((License)null));
		assertTrue(empty(license().build()));

		assertFalse(empty(license().name("foo").build()));
		assertFalse(empty(license().url("foo").build()));
		assertFalse(empty(license().value(a("foo")).build()));
	}

	@Test
	public void a09_Path() throws Exception {
		Field f1 = A1.class.getField("f1");
		Field f2 = A2.class.getField("f1");

		assertObject(path().build().annotationType()).json().contains("Path");

		assertTrue(empty(f1.getAnnotation(Path.class)));
		assertTrue(empty(f2.getAnnotation(Path.class)));
		assertTrue(empty((Path)null));
		assertTrue(empty(path().build()));

		assertFalse(empty(path()._enum(a("foo")).build()));
		assertFalse(empty(path().aev(true).build()));
		assertFalse(empty(path().allowEmptyValue(true).build()));
		assertFalse(empty(path().api(a("foo")).build()));
		assertFalse(empty(path().cf("foo").build()));
		assertFalse(empty(path().collectionFormat("foo").build()));
		assertFalse(empty(path().d(a("foo")).build()));
		assertFalse(empty(path().description(a("foo")).build()));
		assertFalse(empty(path().e(a("foo")).build()));
		assertFalse(empty(path().emax(true).build()));
		assertFalse(empty(path().emin(true).build()));
		assertFalse(empty(path().ex(a("foo")).build()));
		assertFalse(empty(path().example(a("foo")).build()));
		assertFalse(empty(path().exclusiveMaximum(true).build()));
		assertFalse(empty(path().exclusiveMinimum(true).build()));
		assertFalse(empty(path().f("foo").build()));
		assertFalse(empty(path().format("foo").build()));
		assertFalse(empty(path().items(items().$ref("foo").build()).build()));
		assertFalse(empty(path().max("foo").build()));
		assertFalse(empty(path().maxi(0).build()));
		assertFalse(empty(path().maximum("foo").build()));
		assertFalse(empty(path().maxItems(0).build()));
		assertFalse(empty(path().maxl(0).build()));
		assertFalse(empty(path().maxLength(0).build()));
		assertFalse(empty(path().min("foo").build()));
		assertFalse(empty(path().mini(0).build()));
		assertFalse(empty(path().minimum("foo").build()));
		assertFalse(empty(path().minItems(0).build()));
		assertFalse(empty(path().minl(0).build()));
		assertFalse(empty(path().minLength(0).build()));
		assertFalse(empty(path().mo("foo").build()));
		assertFalse(empty(path().multipleOf("foo").build()));
		assertFalse(empty(path().n("foo").build()));
		assertFalse(empty(path().name("foo").build()));
		assertFalse(empty(path().p("foo").build()));
		assertFalse(empty(path().parser(OpenApiParser.class).build()));
		assertFalse(empty(path().pattern("foo").build()));
		assertFalse(empty(path().r(false).build()));
		assertFalse(empty(path().required(false).build()));
		assertFalse(empty(path().serializer(OpenApiSerializer.class).build()));
		assertFalse(empty(path().t("foo").build()));
		assertFalse(empty(path().type("foo").build()));
		assertFalse(empty(path().ui(true).build()));
		assertFalse(empty(path().uniqueItems(true).build()));
		assertFalse(empty(path().value("foo").build()));
	}

	@Test
	public void a10_Request() throws Exception {
		assertObject(request().build().annotationType()).json().contains("Request");

		assertObject(request().parser(OpenApiParser.class).build().parser()).json().is("'org.apache.juneau.oapi.OpenApiParser'");
		assertObject(request().serializer(OpenApiSerializer.class).build().serializer()).json().is("'org.apache.juneau.oapi.OpenApiSerializer'");
	}

	@Test
	public void a11_Response() throws Exception {
		assertObject(response().build().annotationType()).json().contains("Response");

		assertTrue(empty(A1.class.getAnnotation(Response.class)));
		assertTrue(empty(A2.class.getAnnotation(Response.class)));
		assertTrue(empty(response().build()));
		assertTrue(empty((Response)null));

		assertFalse(empty(response().api(a("foo")).build()));
		assertFalse(empty(response().code(a(0)).build()));
		assertFalse(empty(response().d(a("foo")).build()));
		assertFalse(empty(response().description(a("foo")).build()));
		assertFalse(empty(response().ex(a("foo")).build()));
		assertFalse(empty(response().example(a("foo")).build()));
		assertFalse(empty(response().examples(a("foo")).build()));
		assertFalse(empty(response().exs(a("foo")).build()));
		assertFalse(empty(response().headers(new ResponseHeader[]{responseHeader().$ref("foo").build()}).build()));
		assertFalse(empty(response().parser(OpenApiParser.class).build()));
		assertFalse(empty(response().schema(schema().$ref("foo").build()).build()));
		assertFalse(empty(response().serializer(OpenApiSerializer.class).build()));
		assertFalse(empty(response().value(a(0)).build()));
	}

	@Test
	public void a12_ResponseBody() throws Exception {
		assertObject(responseBody().build().annotationType()).json().contains("ResponseBody");
	}

	@Test
	public void a13_ResponseHeader() throws Exception {
		assertObject(responseHeader().build().annotationType()).json().contains("ResponseHeader");

		assertTrue(empty(A1.class.getAnnotation(ResponseHeader.class)));
		assertTrue(empty(A2.class.getAnnotation(ResponseHeader.class)));

		assertFalse(empty(responseHeader()._default(a("foo")).build()));
		assertFalse(empty(responseHeader()._enum(a("foo")).build()));
		assertFalse(empty(responseHeader().api(a("foo")).build()));
		assertFalse(empty(responseHeader().code(a(0)).build()));
		assertFalse(empty(responseHeader().cf("foo").build()));
		assertFalse(empty(responseHeader().collectionFormat("foo").build()));
		assertFalse(empty(responseHeader().d(a("foo")).build()));
		assertFalse(empty(responseHeader().description(a("foo")).build()));
		assertFalse(empty(responseHeader().df(a("foo")).build()));
		assertFalse(empty(responseHeader().e(a("foo")).build()));
		assertFalse(empty(responseHeader().emax(true).build()));
		assertFalse(empty(responseHeader().emin(true).build()));
		assertFalse(empty(responseHeader().ex(a("foo")).build()));
		assertFalse(empty(responseHeader().example(a("foo")).build()));
		assertFalse(empty(responseHeader().exclusiveMaximum(true).build()));
		assertFalse(empty(responseHeader().exclusiveMinimum(true).build()));
		assertFalse(empty(responseHeader().f("foo").build()));
		assertFalse(empty(responseHeader().format("foo").build()));
		assertFalse(empty(responseHeader().items(items().$ref("foo").build()).build()));
		assertFalse(empty(responseHeader().max("foo").build()));
		assertFalse(empty(responseHeader().maxi(0).build()));
		assertFalse(empty(responseHeader().maximum("foo").build()));
		assertFalse(empty(responseHeader().maxItems(0).build()));
		assertFalse(empty(responseHeader().maxl(0).build()));
		assertFalse(empty(responseHeader().maxLength(0).build()));
		assertFalse(empty(responseHeader().min("foo").build()));
		assertFalse(empty(responseHeader().mini(0).build()));
		assertFalse(empty(responseHeader().minimum("foo").build()));
		assertFalse(empty(responseHeader().minItems(0).build()));
		assertFalse(empty(responseHeader().minl(0).build()));
		assertFalse(empty(responseHeader().minLength(0).build()));
		assertFalse(empty(responseHeader().mo("foo").build()));
		assertFalse(empty(responseHeader().multipleOf("foo").build()));
		assertFalse(empty(responseHeader().n("foo").build()));
		assertFalse(empty(responseHeader().name("foo").build()));
		assertFalse(empty(responseHeader().p("foo").build()));
		assertFalse(empty(responseHeader().pattern("foo").build()));
		assertFalse(empty(responseHeader().serializer(OpenApiSerializer.class).build()));
		assertFalse(empty(responseHeader().t("foo").build()));
		assertFalse(empty(responseHeader().type("foo").build()));
		assertFalse(empty(responseHeader().ui(true).build()));
		assertFalse(empty(responseHeader().uniqueItems(true).build()));
		assertFalse(empty(responseHeader().value("foo").build()));
	}

	@Test
	public void a14_ResponseStatus() throws Exception {
		assertObject(responseStatus().build().annotationType()).json().contains("ResponseStatus");
	}

	@Test
	public void a15_Tag() throws Exception {
		assertObject(tag().build().annotationType()).json().contains("Tag");

		assertObject(tag().description(a("foo")).build().description()).json().is("['foo']");
		assertObject(tag().externalDocs(externalDocs().url("foo").build()).build().externalDocs().url()).json().is("'foo'");
		assertObject(tag().name("foo").build().name()).json().is("'foo'");
		assertObject(tag().value(a("foo")).build().value()).json().is("['foo']");
	}

	@Test
	public void a16_ExternalDocs() throws Exception {
		X1 x = A1.class.getAnnotation(X1.class);

		assertObject(externalDocs().build().annotationType()).json().contains("ExternalDocs");

		assertTrue(empty(x.externalDocs()));
		assertTrue(empty((ExternalDocs)null));

		assertFalse(empty(externalDocs().description(a("foo")).build()));
		assertFalse(empty(externalDocs().url("foo").build()));
		assertFalse(empty(externalDocs().value(a("foo")).build()));
	}

	@Test
	public void a17_Schema() throws Exception {
		X1 x = A1.class.getAnnotation(X1.class);

		assertObject(schema().build().annotationType()).json().contains("Schema");

		assertTrue(empty(x.schema()));
		assertTrue(empty((Schema)null));

		assertFalse(empty(schema()._default(a("foo")).build()));
		assertFalse(empty(schema()._enum(a("foo")).build()));
		assertFalse(empty(schema().$ref("foo").build()));
		assertFalse(empty(schema().additionalProperties(a("foo")).build()));
		assertFalse(empty(schema().allOf(a("foo")).build()));
		assertFalse(empty(schema().cf("foo").build()));
		assertFalse(empty(schema().collectionFormat("foo").build()));
		assertFalse(empty(schema().d(a("foo")).build()));
		assertFalse(empty(schema().description(a("foo")).build()));
		assertFalse(empty(schema().df(a("foo")).build()));
		assertFalse(empty(schema().discriminator("foo").build()));
		assertFalse(empty(schema().e(a("foo")).build()));
		assertFalse(empty(schema().emax(true).build()));
		assertFalse(empty(schema().emin(true).build()));
		assertFalse(empty(schema().ex(a("foo")).build()));
		assertFalse(empty(schema().example(a("foo")).build()));
		assertFalse(empty(schema().examples(a("foo")).build()));
		assertFalse(empty(schema().exclusiveMaximum(true).build()));
		assertFalse(empty(schema().exclusiveMinimum(true).build()));
		assertFalse(empty(schema().exs(a("foo")).build()));
		assertFalse(empty(schema().externalDocs(externalDocs().url("foo").build()).build()));
		assertFalse(empty(schema().f("foo").build()));
		assertFalse(empty(schema().format("foo").build()));
		assertFalse(empty(schema().ignore(true).build()));
		assertFalse(empty(schema().items(items().$ref("foo").build()).build()));
		assertFalse(empty(schema().max("foo").build()));
		assertFalse(empty(schema().maxi(0).build()));
		assertFalse(empty(schema().maximum("foo").build()));
		assertFalse(empty(schema().maxItems(0).build()));
		assertFalse(empty(schema().maxl(0).build()));
		assertFalse(empty(schema().maxLength(0).build()));
		assertFalse(empty(schema().maxp(0).build()));
		assertFalse(empty(schema().maxProperties(0).build()));
		assertFalse(empty(schema().min("foo").build()));
		assertFalse(empty(schema().mini(0).build()));
		assertFalse(empty(schema().minimum("foo").build()));
		assertFalse(empty(schema().minItems(0).build()));
		assertFalse(empty(schema().minl(0).build()));
		assertFalse(empty(schema().minLength(0).build()));
		assertFalse(empty(schema().minp(0).build()));
		assertFalse(empty(schema().minProperties(0).build()));
		assertFalse(empty(schema().mo("foo").build()));
		assertFalse(empty(schema().multipleOf("foo").build()));
		assertFalse(empty(schema().p("foo").build()));
		assertFalse(empty(schema().pattern("foo").build()));
		assertFalse(empty(schema().properties(a("foo")).build()));
		assertFalse(empty(schema().r(true).build()));
		assertFalse(empty(schema().readOnly(true).build()));
		assertFalse(empty(schema().required(true).build()));
		assertFalse(empty(schema().ro(true).build()));
		assertFalse(empty(schema().t("foo").build()));
		assertFalse(empty(schema().title("foo").build()));
		assertFalse(empty(schema().type("foo").build()));
		assertFalse(empty(schema().ui(true).build()));
		assertFalse(empty(schema().uniqueItems(true).build()));
		assertFalse(empty(schema().value(a("foo")).build()));
		assertFalse(empty(schema().xml(a("foo")).build()));
	}

	@Test
	public void a18_SubItems() throws Exception {
		X1 x = A1.class.getAnnotation(X1.class);

		assertObject(subItems().build().annotationType()).json().contains("SubItems");

		assertTrue(empty(x.subItems()));
		assertTrue(empty((SubItems)null));

		assertFalse(empty(subItems()._default(a("foo")).build()));
		assertFalse(empty(subItems()._enum(a("foo")).build()));
		assertFalse(empty(subItems().$ref("foo").build()));
		assertFalse(empty(subItems().cf("foo").build()));
		assertFalse(empty(subItems().collectionFormat("foo").build()));
		assertFalse(empty(subItems().df(a("foo")).build()));
		assertFalse(empty(subItems().e(a("foo")).build()));
		assertFalse(empty(subItems().emax(true).build()));
		assertFalse(empty(subItems().emin(true).build()));
		assertFalse(empty(subItems().exclusiveMaximum(true).build()));
		assertFalse(empty(subItems().exclusiveMinimum(true).build()));
		assertFalse(empty(subItems().f("foo").build()));
		assertFalse(empty(subItems().format("foo").build()));
		assertFalse(empty(subItems().items(a("foo")).build()));
		assertFalse(empty(subItems().max("foo").build()));
		assertFalse(empty(subItems().maxi(0).build()));
		assertFalse(empty(subItems().maximum("foo").build()));
		assertFalse(empty(subItems().maxItems(0).build()));
		assertFalse(empty(subItems().maxl(0).build()));
		assertFalse(empty(subItems().maxLength(0).build()));
		assertFalse(empty(subItems().min("foo").build()));
		assertFalse(empty(subItems().mini(0).build()));
		assertFalse(empty(subItems().minimum("foo").build()));
		assertFalse(empty(subItems().minItems(0).build()));
		assertFalse(empty(subItems().minl(0).build()));
		assertFalse(empty(subItems().minLength(0).build()));
		assertFalse(empty(subItems().mo("foo").build()));
		assertFalse(empty(subItems().multipleOf("foo").build()));
		assertFalse(empty(subItems().p("foo").build()));
		assertFalse(empty(subItems().pattern("foo").build()));
		assertFalse(empty(subItems().t("foo").build()));
		assertFalse(empty(subItems().type("foo").build()));
		assertFalse(empty(subItems().ui(true).build()));
		assertFalse(empty(subItems().uniqueItems(true).build()));
		assertFalse(empty(subItems().value(a("foo")).build()));
	}

	@Test
	public void a19_Items() throws Exception {
		X1 x = A1.class.getAnnotation(X1.class);

		assertObject(items().build().annotationType()).json().contains("Items");

		assertTrue(empty(x.items()));
		assertTrue(empty((Items)null));
		assertFalse(empty(items()._default(a("foo")).build()));
		assertFalse(empty(items()._enum(a("foo")).build()));
		assertFalse(empty(items().$ref("foo").build()));
		assertFalse(empty(items().cf("foo").build()));
		assertFalse(empty(items().collectionFormat("foo").build()));
		assertFalse(empty(items().df(a("foo")).build()));
		assertFalse(empty(items().e(a("foo")).build()));
		assertFalse(empty(items().emax(true).build()));
		assertFalse(empty(items().emin(true).build()));
		assertFalse(empty(items().exclusiveMaximum(true).build()));
		assertFalse(empty(items().exclusiveMinimum(true).build()));
		assertFalse(empty(items().f("foo").build()));
		assertFalse(empty(items().format("foo").build()));
		assertFalse(empty(items().items(subItems().$ref("foo").build()).build()));
		assertFalse(empty(items().max("foo").build()));
		assertFalse(empty(items().maxi(0).build()));
		assertFalse(empty(items().maximum("foo").build()));
		assertFalse(empty(items().maxItems(0).build()));
		assertFalse(empty(items().maxl(0).build()));
		assertFalse(empty(items().maxLength(0).build()));
		assertFalse(empty(items().min("foo").build()));
		assertFalse(empty(items().mini(0).build()));
		assertFalse(empty(items().minimum("foo").build()));
		assertFalse(empty(items().minItems(0).build()));
		assertFalse(empty(items().minl(0).build()));
		assertFalse(empty(items().minLength(0).build()));
		assertFalse(empty(items().mo("foo").build()));
		assertFalse(empty(items().multipleOf("foo").build()));
		assertFalse(empty(items().p("foo").build()));
		assertFalse(empty(items().pattern("foo").build()));
		assertFalse(empty(items().t("foo").build()));
		assertFalse(empty(items().type("foo").build()));
		assertFalse(empty(items().ui(true).build()));
		assertFalse(empty(items().uniqueItems(true).build()));
		assertFalse(empty(items().value(a("foo")).build()));
	}


	@Test
	public void b01_allEmpty() {
		assertTrue(allEmpty(new String[0]));
		assertTrue(allEmpty(""));
		assertTrue(allEmpty(null,""));
		assertFalse(allEmpty(null,"","x"));
		assertTrue(allEmpty(new String[0],new String[0]));
		assertTrue(allEmpty(null,new String[0]));
		assertFalse(allEmpty(null,new String[]{""}));
		assertFalse(allEmpty(null,new String[]{"x"}));
	}

	@Test
	public void b02_allTrue() {
		assertTrue(allTrue());
		assertTrue(allTrue(true));
		assertTrue(allTrue(true,true));
		assertFalse(allTrue(false,true));
		assertFalse(allTrue(false));
	}

	@Test
	public void b03_allFalse() {
		assertTrue(allFalse());
		assertTrue(allFalse(false));
		assertTrue(allFalse(false,false));
		assertFalse(allFalse(false,true));
		assertFalse(allFalse(true));
	}

	@Test
	public void b04_allMinusOne() {
		assertTrue(allMinusOne());
		assertTrue(allMinusOne(-1));
		assertTrue(allMinusOne(-1,-1));
		assertFalse(allMinusOne(-1,0));
		assertFalse(allMinusOne(0));
	}

	@Test
	public void b05_allMinusOneLongs() {
		assertTrue(allMinusOne(-1l));
		assertTrue(allMinusOne(-1l,-1l));
		assertFalse(allMinusOne(-1l,0l));
		assertFalse(allMinusOne(0l));
	}

	@Test
	public void b06_other() throws Exception {
		new AnnotationUtils();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//-----------------------------------------------------------------------------------------------------------------

	private static BodyBuilder body() {
		return BodyBuilder.create();
	}

	private static ContactBuilder contact() {
		return ContactBuilder.create();
	}

	private static FormDataBuilder formData() {
		return FormDataBuilder.create();
	}

	private static HasFormDataBuilder hasFormData() {
		return HasFormDataBuilder.create();
	}

	private static QueryBuilder query() {
		return QueryBuilder.create();
	}

	private static HasQueryBuilder hasQuery() {
		return HasQueryBuilder.create();
	}

	private static HeaderBuilder header() {
		return HeaderBuilder.create();
	}

	private static LicenseBuilder license() {
		return LicenseBuilder.create();
	}

	private static PathBuilder path() {
		return PathBuilder.create();
	}

	private static RequestBuilder request() {
		return RequestBuilder.create();
	}

	private static ResponseBuilder response() {
		return ResponseBuilder.create();
	}

	private static ResponseBodyBuilder responseBody() {
		return ResponseBodyBuilder.create();
	}

	private static ResponseHeaderBuilder responseHeader() {
		return ResponseHeaderBuilder.create();
	}

	private static ResponseStatusBuilder responseStatus() {
		return ResponseStatusBuilder.create();
	}

	private static TagBuilder tag() {
		return TagBuilder.create();
	}

	private static SchemaBuilder schema() {
		return SchemaBuilder.create();
	}

	private static ItemsBuilder items() {
		return ItemsBuilder.create();
	}

	private static SubItemsBuilder subItems() {
		return SubItemsBuilder.create();
	}

	private static ExternalDocsBuilder externalDocs() {
		return ExternalDocsBuilder.create();
	}

	private static String[] a(String...s) {
		return s;
	}

	private static int[] a(int...i) {
		return i;
	}
}