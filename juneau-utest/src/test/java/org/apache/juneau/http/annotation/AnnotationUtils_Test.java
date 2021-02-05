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
		assertObject(body().build().annotationType()).asJson().contains("Body");

		assertTrue(BodyAnnotation.empty(A1.class.getAnnotation(Body.class)));
		assertTrue(BodyAnnotation.empty(A2.class.getAnnotation(Body.class)));
		assertTrue(BodyAnnotation.empty(body().build()));
		assertTrue(BodyAnnotation.empty((Body)null));

		assertFalse(BodyAnnotation.empty(body().api(a("foo")).build()));
		assertFalse(BodyAnnotation.empty(body().d(a("foo")).build()));
		assertFalse(BodyAnnotation.empty(body().description(a("foo")).build()));
		assertFalse(BodyAnnotation.empty(body().ex(a("foo")).build()));
		assertFalse(BodyAnnotation.empty(body().example(a("foo")).build()));
		assertFalse(BodyAnnotation.empty(body().examples(a("foo")).build()));
		assertFalse(BodyAnnotation.empty(body().exs(a("foo")).build()));
		assertFalse(BodyAnnotation.empty(body().required(true).build()));
		assertFalse(BodyAnnotation.empty(body().r(true).build()));
		assertFalse(BodyAnnotation.empty(body().schema(schema().$ref("foo").build()).build()));
		assertFalse(BodyAnnotation.empty(body().value(a("foo")).build()));
	}

	@Test
	public void a02_Contact() throws Exception {
		X1 x1 = A1.class.getAnnotation(X1.class);

		assertObject(contact().build().annotationType()).asJson().contains("Contact");

		assertTrue(ContactAnnotation.empty(x1.contact()));
		assertTrue(ContactAnnotation.empty(contact().build()));
		assertTrue(ContactAnnotation.empty((Contact)null));

		assertFalse(ContactAnnotation.empty(contact().email("foo").build()));
		assertFalse(ContactAnnotation.empty(contact().name("foo").build()));
		assertFalse(ContactAnnotation.empty(contact().url("foo").build()));
		assertFalse(ContactAnnotation.empty(contact().value(a("foo")).build()));
	}

	@Test
	public void a03_FormData() throws Exception {
		Field f1 = A1.class.getField("f1");
		Field f2 = A2.class.getField("f1");

		assertObject(formData().build().annotationType()).asJson().contains("FormData");

		assertTrue(FormDataAnnotation.empty(f1.getAnnotation(FormData.class)));
		assertTrue(FormDataAnnotation.empty(f2.getAnnotation(FormData.class)));
		assertTrue(FormDataAnnotation.empty((FormData)null));
		assertTrue(FormDataAnnotation.empty(formData().build()));

		assertFalse(FormDataAnnotation.empty(formData()._default(a("foo")).build()));
		assertFalse(FormDataAnnotation.empty(formData()._enum(a("foo")).build()));
		assertFalse(FormDataAnnotation.empty(formData().aev(true).build()));
		assertFalse(FormDataAnnotation.empty(formData().allowEmptyValue(true).build()));
		assertFalse(FormDataAnnotation.empty(formData().api(a("foo")).build()));
		assertFalse(FormDataAnnotation.empty(formData().cf("foo").build()));
		assertFalse(FormDataAnnotation.empty(formData().collectionFormat("foo").build()));
		assertFalse(FormDataAnnotation.empty(formData().d(a("foo")).build()));
		assertFalse(FormDataAnnotation.empty(formData().description(a("foo")).build()));
		assertFalse(FormDataAnnotation.empty(formData().df(a("foo")).build()));
		assertFalse(FormDataAnnotation.empty(formData().e(a("foo")).build()));
		assertFalse(FormDataAnnotation.empty(formData().emax(true).build()));
		assertFalse(FormDataAnnotation.empty(formData().emin(true).build()));
		assertFalse(FormDataAnnotation.empty(formData().ex(a("foo")).build()));
		assertFalse(FormDataAnnotation.empty(formData().example(a("foo")).build()));
		assertFalse(FormDataAnnotation.empty(formData().exclusiveMaximum(true).build()));
		assertFalse(FormDataAnnotation.empty(formData().exclusiveMinimum(true).build()));
		assertFalse(FormDataAnnotation.empty(formData().f("foo").build()));
		assertFalse(FormDataAnnotation.empty(formData().format("foo").build()));
		assertFalse(FormDataAnnotation.empty(formData().items(items().$ref("foo").build()).build()));
		assertFalse(FormDataAnnotation.empty(formData().max("foo").build()));
		assertFalse(FormDataAnnotation.empty(formData().maxi(0).build()));
		assertFalse(FormDataAnnotation.empty(formData().maximum("foo").build()));
		assertFalse(FormDataAnnotation.empty(formData().maxItems(0).build()));
		assertFalse(FormDataAnnotation.empty(formData().maxl(0).build()));
		assertFalse(FormDataAnnotation.empty(formData().maxLength(0).build()));
		assertFalse(FormDataAnnotation.empty(formData().min("foo").build()));
		assertFalse(FormDataAnnotation.empty(formData().mini(0).build()));
		assertFalse(FormDataAnnotation.empty(formData().minimum("foo").build()));
		assertFalse(FormDataAnnotation.empty(formData().minItems(0).build()));
		assertFalse(FormDataAnnotation.empty(formData().minl(0).build()));
		assertFalse(FormDataAnnotation.empty(formData().minLength(0).build()));
		assertFalse(FormDataAnnotation.empty(formData().mo("foo").build()));
		assertFalse(FormDataAnnotation.empty(formData().multi(true).build()));
		assertFalse(FormDataAnnotation.empty(formData().multipleOf("foo").build()));
		assertFalse(FormDataAnnotation.empty(formData().n("foo").build()));
		assertFalse(FormDataAnnotation.empty(formData().name("foo").build()));
		assertFalse(FormDataAnnotation.empty(formData().p("foo").build()));
		assertFalse(FormDataAnnotation.empty(formData().parser(OpenApiParser.class).build()));
		assertFalse(FormDataAnnotation.empty(formData().pattern("foo").build()));
		assertFalse(FormDataAnnotation.empty(formData().r(true).build()));
		assertFalse(FormDataAnnotation.empty(formData().required(true).build()));
		assertFalse(FormDataAnnotation.empty(formData().serializer(OpenApiSerializer.class).build()));
		assertFalse(FormDataAnnotation.empty(formData().sie(true).build()));
		assertFalse(FormDataAnnotation.empty(formData().skipIfEmpty(true).build()));
		assertFalse(FormDataAnnotation.empty(formData().t("foo").build()));
		assertFalse(FormDataAnnotation.empty(formData().type("foo").build()));
		assertFalse(FormDataAnnotation.empty(formData().ui(true).build()));
		assertFalse(FormDataAnnotation.empty(formData().uniqueItems(true).build()));
		assertFalse(FormDataAnnotation.empty(formData().value("foo").build()));
	}

	@Test
	public void a04_HasFormData() throws Exception {
		assertObject(hasFormData().build().annotationType()).asJson().contains("HasFormData");

		assertObject(hasFormData().n("foo").build().n()).asJson().is("'foo'");
		assertObject(hasFormData().name("foo").build().name()).asJson().is("'foo'");
		assertObject(hasFormData().value("foo").build().value()).asJson().is("'foo'");
	}

	@Test
	public void a05_Query() throws Exception {
		Field f1 = A1.class.getField("f1");
		Field f2 = A2.class.getField("f1");

		assertObject(query().build().annotationType()).asJson().contains("Query");

		assertTrue(QueryAnnotation.empty(f1.getAnnotation(Query.class)));
		assertTrue(QueryAnnotation.empty(f2.getAnnotation(Query.class)));
		assertTrue(QueryAnnotation.empty((Query)null));
		assertTrue(QueryAnnotation.empty(query().build()));

		assertFalse(QueryAnnotation.empty(query()._default(a("foo")).build()));
		assertFalse(QueryAnnotation.empty(query()._enum(a("foo")).build()));
		assertFalse(QueryAnnotation.empty(query().aev(true).build()));
		assertFalse(QueryAnnotation.empty(query().allowEmptyValue(true).build()));
		assertFalse(QueryAnnotation.empty(query().api(a("foo")).build()));
		assertFalse(QueryAnnotation.empty(query().cf("foo").build()));
		assertFalse(QueryAnnotation.empty(query().collectionFormat("foo").build()));
		assertFalse(QueryAnnotation.empty(query().d(a("foo")).build()));
		assertFalse(QueryAnnotation.empty(query().description(a("foo")).build()));
		assertFalse(QueryAnnotation.empty(query().df(a("foo")).build()));
		assertFalse(QueryAnnotation.empty(query().e(a("foo")).build()));
		assertFalse(QueryAnnotation.empty(query().emax(true).build()));
		assertFalse(QueryAnnotation.empty(query().emin(true).build()));
		assertFalse(QueryAnnotation.empty(query().ex(a("foo")).build()));
		assertFalse(QueryAnnotation.empty(query().example(a("foo")).build()));
		assertFalse(QueryAnnotation.empty(query().exclusiveMaximum(true).build()));
		assertFalse(QueryAnnotation.empty(query().exclusiveMinimum(true).build()));
		assertFalse(QueryAnnotation.empty(query().f("foo").build()));
		assertFalse(QueryAnnotation.empty(query().format("foo").build()));
		assertFalse(QueryAnnotation.empty(query().items(items().$ref("foo").build()).build()));
		assertFalse(QueryAnnotation.empty(query().max("foo").build()));
		assertFalse(QueryAnnotation.empty(query().maxi(0).build()));
		assertFalse(QueryAnnotation.empty(query().maximum("foo").build()));
		assertFalse(QueryAnnotation.empty(query().maxItems(0).build()));
		assertFalse(QueryAnnotation.empty(query().maxl(0).build()));
		assertFalse(QueryAnnotation.empty(query().maxLength(0).build()));
		assertFalse(QueryAnnotation.empty(query().min("foo").build()));
		assertFalse(QueryAnnotation.empty(query().mini(0).build()));
		assertFalse(QueryAnnotation.empty(query().minimum("foo").build()));
		assertFalse(QueryAnnotation.empty(query().minItems(0).build()));
		assertFalse(QueryAnnotation.empty(query().minl(0).build()));
		assertFalse(QueryAnnotation.empty(query().minLength(0).build()));
		assertFalse(QueryAnnotation.empty(query().mo("foo").build()));
		assertFalse(QueryAnnotation.empty(query().multi(true).build()));
		assertFalse(QueryAnnotation.empty(query().multipleOf("foo").build()));
		assertFalse(QueryAnnotation.empty(query().n("foo").build()));
		assertFalse(QueryAnnotation.empty(query().name("foo").build()));
		assertFalse(QueryAnnotation.empty(query().p("foo").build()));
		assertFalse(QueryAnnotation.empty(query().parser(OpenApiParser.class).build()));
		assertFalse(QueryAnnotation.empty(query().pattern("foo").build()));
		assertFalse(QueryAnnotation.empty(query().r(true).build()));
		assertFalse(QueryAnnotation.empty(query().required(true).build()));
		assertFalse(QueryAnnotation.empty(query().serializer(OpenApiSerializer.class).build()));
		assertFalse(QueryAnnotation.empty(query().sie(true).build()));
		assertFalse(QueryAnnotation.empty(query().skipIfEmpty(true).build()));
		assertFalse(QueryAnnotation.empty(query().t("foo").build()));
		assertFalse(QueryAnnotation.empty(query().type("foo").build()));
		assertFalse(QueryAnnotation.empty(query().ui(true).build()));
		assertFalse(QueryAnnotation.empty(query().uniqueItems(true).build()));
		assertFalse(QueryAnnotation.empty(query().value("foo").build()));
	}

	@Test
	public void a06_HasQuery() throws Exception {
		assertObject(hasQuery().build().annotationType()).asJson().contains("HasQuery");

		assertObject(hasQuery().n("foo").build().n()).asJson().is("'foo'");
		assertObject(hasQuery().name("foo").build().name()).asJson().is("'foo'");
		assertObject(hasQuery().value("foo").build().value()).asJson().is("'foo'");
	}

	@Test
	public void a07_Header() throws Exception {
		Field f1 = A1.class.getField("f1");
		Field f2 = A2.class.getField("f1");

		assertObject(header().build().annotationType()).asJson().contains("Header");

		assertTrue(HeaderAnnotation.empty(f1.getAnnotation(Header.class)));
		assertTrue(HeaderAnnotation.empty(f2.getAnnotation(Header.class)));
		assertTrue(HeaderAnnotation.empty((Header)null));
		assertTrue(HeaderAnnotation.empty(header().build()));

		assertFalse(HeaderAnnotation.empty(header()._default(a("foo")).build()));
		assertFalse(HeaderAnnotation.empty(header()._enum(a("foo")).build()));
		assertFalse(HeaderAnnotation.empty(header().aev(true).build()));
		assertFalse(HeaderAnnotation.empty(header().allowEmptyValue(true).build()));
		assertFalse(HeaderAnnotation.empty(header().api(a("foo")).build()));
		assertFalse(HeaderAnnotation.empty(header().cf("foo").build()));
		assertFalse(HeaderAnnotation.empty(header().collectionFormat("foo").build()));
		assertFalse(HeaderAnnotation.empty(header().d(a("foo")).build()));
		assertFalse(HeaderAnnotation.empty(header().description(a("foo")).build()));
		assertFalse(HeaderAnnotation.empty(header().df(a("foo")).build()));
		assertFalse(HeaderAnnotation.empty(header().e(a("foo")).build()));
		assertFalse(HeaderAnnotation.empty(header().emax(true).build()));
		assertFalse(HeaderAnnotation.empty(header().emin(true).build()));
		assertFalse(HeaderAnnotation.empty(header().ex(a("foo")).build()));
		assertFalse(HeaderAnnotation.empty(header().example(a("foo")).build()));
		assertFalse(HeaderAnnotation.empty(header().exclusiveMaximum(true).build()));
		assertFalse(HeaderAnnotation.empty(header().exclusiveMinimum(true).build()));
		assertFalse(HeaderAnnotation.empty(header().f("foo").build()));
		assertFalse(HeaderAnnotation.empty(header().format("foo").build()));
		assertFalse(HeaderAnnotation.empty(header().items(items().$ref("foo").build()).build()));
		assertFalse(HeaderAnnotation.empty(header().max("foo").build()));
		assertFalse(HeaderAnnotation.empty(header().maxi(0).build()));
		assertFalse(HeaderAnnotation.empty(header().maximum("foo").build()));
		assertFalse(HeaderAnnotation.empty(header().maxItems(0).build()));
		assertFalse(HeaderAnnotation.empty(header().maxl(0).build()));
		assertFalse(HeaderAnnotation.empty(header().maxLength(0).build()));
		assertFalse(HeaderAnnotation.empty(header().min("foo").build()));
		assertFalse(HeaderAnnotation.empty(header().mini(0).build()));
		assertFalse(HeaderAnnotation.empty(header().minimum("foo").build()));
		assertFalse(HeaderAnnotation.empty(header().minItems(0).build()));
		assertFalse(HeaderAnnotation.empty(header().minl(0).build()));
		assertFalse(HeaderAnnotation.empty(header().minLength(0).build()));
		assertFalse(HeaderAnnotation.empty(header().mo("foo").build()));
		assertFalse(HeaderAnnotation.empty(header().multi(true).build()));
		assertFalse(HeaderAnnotation.empty(header().multipleOf("foo").build()));
		assertFalse(HeaderAnnotation.empty(header().n("foo").build()));
		assertFalse(HeaderAnnotation.empty(header().name("foo").build()));
		assertFalse(HeaderAnnotation.empty(header().p("foo").build()));
		assertFalse(HeaderAnnotation.empty(header().parser(OpenApiParser.class).build()));
		assertFalse(HeaderAnnotation.empty(header().pattern("foo").build()));
		assertFalse(HeaderAnnotation.empty(header().r(true).build()));
		assertFalse(HeaderAnnotation.empty(header().required(true).build()));
		assertFalse(HeaderAnnotation.empty(header().serializer(OpenApiSerializer.class).build()));
		assertFalse(HeaderAnnotation.empty(header().sie(true).build()));
		assertFalse(HeaderAnnotation.empty(header().skipIfEmpty(true).build()));
		assertFalse(HeaderAnnotation.empty(header().t("foo").build()));
		assertFalse(HeaderAnnotation.empty(header().type("foo").build()));
		assertFalse(HeaderAnnotation.empty(header().ui(true).build()));
		assertFalse(HeaderAnnotation.empty(header().uniqueItems(true).build()));
		assertFalse(HeaderAnnotation.empty(header().value("foo").build()));
	}

	@Test
	public void a08_License() throws Exception {
		X1 x = A1.class.getAnnotation(X1.class);

		assertObject(license().build().annotationType()).asJson().contains("License");

		assertTrue(LicenseAnnotation.empty(x.license()));
		assertTrue(LicenseAnnotation.empty((License)null));
		assertTrue(LicenseAnnotation.empty(license().build()));

		assertFalse(LicenseAnnotation.empty(license().name("foo").build()));
		assertFalse(LicenseAnnotation.empty(license().url("foo").build()));
		assertFalse(LicenseAnnotation.empty(license().value(a("foo")).build()));
	}

	@Test
	public void a09_Path() throws Exception {
		Field f1 = A1.class.getField("f1");
		Field f2 = A2.class.getField("f1");

		assertObject(path().build().annotationType()).asJson().contains("Path");

		assertTrue(PathAnnotation.empty(f1.getAnnotation(Path.class)));
		assertTrue(PathAnnotation.empty(f2.getAnnotation(Path.class)));
		assertTrue(PathAnnotation.empty((Path)null));
		assertTrue(PathAnnotation.empty(path().build()));

		assertFalse(PathAnnotation.empty(path()._enum(a("foo")).build()));
		assertFalse(PathAnnotation.empty(path().aev(true).build()));
		assertFalse(PathAnnotation.empty(path().allowEmptyValue(true).build()));
		assertFalse(PathAnnotation.empty(path().api(a("foo")).build()));
		assertFalse(PathAnnotation.empty(path().cf("foo").build()));
		assertFalse(PathAnnotation.empty(path().collectionFormat("foo").build()));
		assertFalse(PathAnnotation.empty(path().d(a("foo")).build()));
		assertFalse(PathAnnotation.empty(path().description(a("foo")).build()));
		assertFalse(PathAnnotation.empty(path().e(a("foo")).build()));
		assertFalse(PathAnnotation.empty(path().emax(true).build()));
		assertFalse(PathAnnotation.empty(path().emin(true).build()));
		assertFalse(PathAnnotation.empty(path().ex(a("foo")).build()));
		assertFalse(PathAnnotation.empty(path().example(a("foo")).build()));
		assertFalse(PathAnnotation.empty(path().exclusiveMaximum(true).build()));
		assertFalse(PathAnnotation.empty(path().exclusiveMinimum(true).build()));
		assertFalse(PathAnnotation.empty(path().f("foo").build()));
		assertFalse(PathAnnotation.empty(path().format("foo").build()));
		assertFalse(PathAnnotation.empty(path().items(items().$ref("foo").build()).build()));
		assertFalse(PathAnnotation.empty(path().max("foo").build()));
		assertFalse(PathAnnotation.empty(path().maxi(0).build()));
		assertFalse(PathAnnotation.empty(path().maximum("foo").build()));
		assertFalse(PathAnnotation.empty(path().maxItems(0).build()));
		assertFalse(PathAnnotation.empty(path().maxl(0).build()));
		assertFalse(PathAnnotation.empty(path().maxLength(0).build()));
		assertFalse(PathAnnotation.empty(path().min("foo").build()));
		assertFalse(PathAnnotation.empty(path().mini(0).build()));
		assertFalse(PathAnnotation.empty(path().minimum("foo").build()));
		assertFalse(PathAnnotation.empty(path().minItems(0).build()));
		assertFalse(PathAnnotation.empty(path().minl(0).build()));
		assertFalse(PathAnnotation.empty(path().minLength(0).build()));
		assertFalse(PathAnnotation.empty(path().mo("foo").build()));
		assertFalse(PathAnnotation.empty(path().multipleOf("foo").build()));
		assertFalse(PathAnnotation.empty(path().n("foo").build()));
		assertFalse(PathAnnotation.empty(path().name("foo").build()));
		assertFalse(PathAnnotation.empty(path().p("foo").build()));
		assertFalse(PathAnnotation.empty(path().parser(OpenApiParser.class).build()));
		assertFalse(PathAnnotation.empty(path().pattern("foo").build()));
		assertFalse(PathAnnotation.empty(path().r(false).build()));
		assertFalse(PathAnnotation.empty(path().required(false).build()));
		assertFalse(PathAnnotation.empty(path().serializer(OpenApiSerializer.class).build()));
		assertFalse(PathAnnotation.empty(path().t("foo").build()));
		assertFalse(PathAnnotation.empty(path().type("foo").build()));
		assertFalse(PathAnnotation.empty(path().ui(true).build()));
		assertFalse(PathAnnotation.empty(path().uniqueItems(true).build()));
		assertFalse(PathAnnotation.empty(path().value("foo").build()));
	}

	@Test
	public void a10_Request() throws Exception {
		assertObject(request().build().annotationType()).asJson().contains("Request");

		assertObject(request().parser(OpenApiParser.class).build().parser()).asJson().is("'org.apache.juneau.oapi.OpenApiParser'");
		assertObject(request().serializer(OpenApiSerializer.class).build().serializer()).asJson().is("'org.apache.juneau.oapi.OpenApiSerializer'");
	}

	@Test
	public void a11_Response() throws Exception {
		assertObject(response().build().annotationType()).asJson().contains("Response");

		assertTrue(ResponseAnnotation.empty(A1.class.getAnnotation(Response.class)));
		assertTrue(ResponseAnnotation.empty(A2.class.getAnnotation(Response.class)));
		assertTrue(ResponseAnnotation.empty(response().build()));
		assertTrue(ResponseAnnotation.empty((Response)null));

		assertFalse(ResponseAnnotation.empty(response().api(a("foo")).build()));
		assertFalse(ResponseAnnotation.empty(response().code(a(0)).build()));
		assertFalse(ResponseAnnotation.empty(response().d(a("foo")).build()));
		assertFalse(ResponseAnnotation.empty(response().description(a("foo")).build()));
		assertFalse(ResponseAnnotation.empty(response().ex(a("foo")).build()));
		assertFalse(ResponseAnnotation.empty(response().example(a("foo")).build()));
		assertFalse(ResponseAnnotation.empty(response().examples(a("foo")).build()));
		assertFalse(ResponseAnnotation.empty(response().exs(a("foo")).build()));
		assertFalse(ResponseAnnotation.empty(response().headers(new ResponseHeader[]{responseHeader().$ref("foo").build()}).build()));
		assertFalse(ResponseAnnotation.empty(response().parser(OpenApiParser.class).build()));
		assertFalse(ResponseAnnotation.empty(response().schema(schema().$ref("foo").build()).build()));
		assertFalse(ResponseAnnotation.empty(response().serializer(OpenApiSerializer.class).build()));
		assertFalse(ResponseAnnotation.empty(response().value(a(0)).build()));
	}

	@Test
	public void a12_ResponseBody() throws Exception {
		assertObject(responseBody().build().annotationType()).asJson().contains("ResponseBody");
	}

	@Test
	public void a13_ResponseHeader() throws Exception {
		assertObject(responseHeader().build().annotationType()).asJson().contains("ResponseHeader");

		assertTrue(ResponseHeaderAnnotation.empty(A1.class.getAnnotation(ResponseHeader.class)));
		assertTrue(ResponseHeaderAnnotation.empty(A2.class.getAnnotation(ResponseHeader.class)));

		assertFalse(ResponseHeaderAnnotation.empty(responseHeader()._default(a("foo")).build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader()._enum(a("foo")).build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().api(a("foo")).build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().code(a(0)).build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().cf("foo").build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().collectionFormat("foo").build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().d(a("foo")).build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().description(a("foo")).build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().df(a("foo")).build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().e(a("foo")).build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().emax(true).build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().emin(true).build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().ex(a("foo")).build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().example(a("foo")).build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().exclusiveMaximum(true).build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().exclusiveMinimum(true).build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().f("foo").build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().format("foo").build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().items(items().$ref("foo").build()).build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().max("foo").build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().maxi(0).build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().maximum("foo").build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().maxItems(0).build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().maxl(0).build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().maxLength(0).build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().min("foo").build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().mini(0).build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().minimum("foo").build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().minItems(0).build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().minl(0).build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().minLength(0).build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().mo("foo").build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().multipleOf("foo").build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().n("foo").build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().name("foo").build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().p("foo").build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().pattern("foo").build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().serializer(OpenApiSerializer.class).build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().t("foo").build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().type("foo").build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().ui(true).build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().uniqueItems(true).build()));
		assertFalse(ResponseHeaderAnnotation.empty(responseHeader().value("foo").build()));
	}

	@Test
	public void a14_ResponseStatus() throws Exception {
		assertObject(responseStatus().build().annotationType()).asJson().contains("ResponseStatus");
	}

	@Test
	public void a15_Tag() throws Exception {
		assertObject(tag().build().annotationType()).asJson().contains("Tag");

		assertObject(tag().description(a("foo")).build().description()).asJson().is("['foo']");
		assertObject(tag().externalDocs(externalDocs().url("foo").build()).build().externalDocs().url()).asJson().is("'foo'");
		assertObject(tag().name("foo").build().name()).asJson().is("'foo'");
		assertObject(tag().value(a("foo")).build().value()).asJson().is("['foo']");
	}

	@Test
	public void a16_ExternalDocs() throws Exception {
		X1 x = A1.class.getAnnotation(X1.class);

		assertObject(externalDocs().build().annotationType()).asJson().contains("ExternalDocs");

		assertTrue(ExternalDocsAnnotation.empty(x.externalDocs()));
		assertTrue(ExternalDocsAnnotation.empty((ExternalDocs)null));

		assertFalse(ExternalDocsAnnotation.empty(externalDocs().description(a("foo")).build()));
		assertFalse(ExternalDocsAnnotation.empty(externalDocs().url("foo").build()));
		assertFalse(ExternalDocsAnnotation.empty(externalDocs().value(a("foo")).build()));
	}

	@Test
	public void a17_Schema() throws Exception {
		X1 x = A1.class.getAnnotation(X1.class);

		assertObject(schema().build().annotationType()).asJson().contains("Schema");

		assertTrue(SchemaAnnotation.empty(x.schema()));
		assertTrue(SchemaAnnotation.empty((Schema)null));

		assertFalse(SchemaAnnotation.empty(schema()._default(a("foo")).build()));
		assertFalse(SchemaAnnotation.empty(schema()._enum(a("foo")).build()));
		assertFalse(SchemaAnnotation.empty(schema().$ref("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().additionalProperties(a("foo")).build()));
		assertFalse(SchemaAnnotation.empty(schema().allOf(a("foo")).build()));
		assertFalse(SchemaAnnotation.empty(schema().cf("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().collectionFormat("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().d(a("foo")).build()));
		assertFalse(SchemaAnnotation.empty(schema().description(a("foo")).build()));
		assertFalse(SchemaAnnotation.empty(schema().df(a("foo")).build()));
		assertFalse(SchemaAnnotation.empty(schema().discriminator("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().e(a("foo")).build()));
		assertFalse(SchemaAnnotation.empty(schema().emax(true).build()));
		assertFalse(SchemaAnnotation.empty(schema().emin(true).build()));
		assertFalse(SchemaAnnotation.empty(schema().ex(a("foo")).build()));
		assertFalse(SchemaAnnotation.empty(schema().example(a("foo")).build()));
		assertFalse(SchemaAnnotation.empty(schema().examples(a("foo")).build()));
		assertFalse(SchemaAnnotation.empty(schema().exclusiveMaximum(true).build()));
		assertFalse(SchemaAnnotation.empty(schema().exclusiveMinimum(true).build()));
		assertFalse(SchemaAnnotation.empty(schema().exs(a("foo")).build()));
		assertFalse(SchemaAnnotation.empty(schema().externalDocs(externalDocs().url("foo").build()).build()));
		assertFalse(SchemaAnnotation.empty(schema().f("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().format("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().ignore(true).build()));
		assertFalse(SchemaAnnotation.empty(schema().items(items().$ref("foo").build()).build()));
		assertFalse(SchemaAnnotation.empty(schema().max("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().maxi(0).build()));
		assertFalse(SchemaAnnotation.empty(schema().maximum("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().maxItems(0).build()));
		assertFalse(SchemaAnnotation.empty(schema().maxl(0).build()));
		assertFalse(SchemaAnnotation.empty(schema().maxLength(0).build()));
		assertFalse(SchemaAnnotation.empty(schema().maxp(0).build()));
		assertFalse(SchemaAnnotation.empty(schema().maxProperties(0).build()));
		assertFalse(SchemaAnnotation.empty(schema().min("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().mini(0).build()));
		assertFalse(SchemaAnnotation.empty(schema().minimum("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().minItems(0).build()));
		assertFalse(SchemaAnnotation.empty(schema().minl(0).build()));
		assertFalse(SchemaAnnotation.empty(schema().minLength(0).build()));
		assertFalse(SchemaAnnotation.empty(schema().minp(0).build()));
		assertFalse(SchemaAnnotation.empty(schema().minProperties(0).build()));
		assertFalse(SchemaAnnotation.empty(schema().mo("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().multipleOf("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().p("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().pattern("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().properties(a("foo")).build()));
		assertFalse(SchemaAnnotation.empty(schema().r(true).build()));
		assertFalse(SchemaAnnotation.empty(schema().readOnly(true).build()));
		assertFalse(SchemaAnnotation.empty(schema().required(true).build()));
		assertFalse(SchemaAnnotation.empty(schema().ro(true).build()));
		assertFalse(SchemaAnnotation.empty(schema().t("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().title("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().type("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().ui(true).build()));
		assertFalse(SchemaAnnotation.empty(schema().uniqueItems(true).build()));
		assertFalse(SchemaAnnotation.empty(schema().value(a("foo")).build()));
		assertFalse(SchemaAnnotation.empty(schema().xml(a("foo")).build()));
	}

	@Test
	public void a18_SubItems() throws Exception {
		X1 x = A1.class.getAnnotation(X1.class);

		assertObject(subItems().build().annotationType()).asJson().contains("SubItems");

		assertTrue(SubItemsAnnotation.empty(x.subItems()));
		assertTrue(SubItemsAnnotation.empty((SubItems)null));

		assertFalse(SubItemsAnnotation.empty(subItems()._default(a("foo")).build()));
		assertFalse(SubItemsAnnotation.empty(subItems()._enum(a("foo")).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().$ref("foo").build()));
		assertFalse(SubItemsAnnotation.empty(subItems().cf("foo").build()));
		assertFalse(SubItemsAnnotation.empty(subItems().collectionFormat("foo").build()));
		assertFalse(SubItemsAnnotation.empty(subItems().df(a("foo")).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().e(a("foo")).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().emax(true).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().emin(true).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().exclusiveMaximum(true).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().exclusiveMinimum(true).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().f("foo").build()));
		assertFalse(SubItemsAnnotation.empty(subItems().format("foo").build()));
		assertFalse(SubItemsAnnotation.empty(subItems().items(a("foo")).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().max("foo").build()));
		assertFalse(SubItemsAnnotation.empty(subItems().maxi(0).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().maximum("foo").build()));
		assertFalse(SubItemsAnnotation.empty(subItems().maxItems(0).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().maxl(0).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().maxLength(0).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().min("foo").build()));
		assertFalse(SubItemsAnnotation.empty(subItems().mini(0).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().minimum("foo").build()));
		assertFalse(SubItemsAnnotation.empty(subItems().minItems(0).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().minl(0).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().minLength(0).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().mo("foo").build()));
		assertFalse(SubItemsAnnotation.empty(subItems().multipleOf("foo").build()));
		assertFalse(SubItemsAnnotation.empty(subItems().p("foo").build()));
		assertFalse(SubItemsAnnotation.empty(subItems().pattern("foo").build()));
		assertFalse(SubItemsAnnotation.empty(subItems().t("foo").build()));
		assertFalse(SubItemsAnnotation.empty(subItems().type("foo").build()));
		assertFalse(SubItemsAnnotation.empty(subItems().ui(true).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().uniqueItems(true).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().value(a("foo")).build()));
	}

	@Test
	public void a19_Items() throws Exception {
		X1 x = A1.class.getAnnotation(X1.class);

		assertObject(items().build().annotationType()).asJson().contains("Items");

		assertTrue(ItemsAnnotation.empty(x.items()));
		assertTrue(ItemsAnnotation.empty((Items)null));
		assertFalse(ItemsAnnotation.empty(items()._default(a("foo")).build()));
		assertFalse(ItemsAnnotation.empty(items()._enum(a("foo")).build()));
		assertFalse(ItemsAnnotation.empty(items().$ref("foo").build()));
		assertFalse(ItemsAnnotation.empty(items().cf("foo").build()));
		assertFalse(ItemsAnnotation.empty(items().collectionFormat("foo").build()));
		assertFalse(ItemsAnnotation.empty(items().df(a("foo")).build()));
		assertFalse(ItemsAnnotation.empty(items().e(a("foo")).build()));
		assertFalse(ItemsAnnotation.empty(items().emax(true).build()));
		assertFalse(ItemsAnnotation.empty(items().emin(true).build()));
		assertFalse(ItemsAnnotation.empty(items().exclusiveMaximum(true).build()));
		assertFalse(ItemsAnnotation.empty(items().exclusiveMinimum(true).build()));
		assertFalse(ItemsAnnotation.empty(items().f("foo").build()));
		assertFalse(ItemsAnnotation.empty(items().format("foo").build()));
		assertFalse(ItemsAnnotation.empty(items().items(subItems().$ref("foo").build()).build()));
		assertFalse(ItemsAnnotation.empty(items().max("foo").build()));
		assertFalse(ItemsAnnotation.empty(items().maxi(0).build()));
		assertFalse(ItemsAnnotation.empty(items().maximum("foo").build()));
		assertFalse(ItemsAnnotation.empty(items().maxItems(0).build()));
		assertFalse(ItemsAnnotation.empty(items().maxl(0).build()));
		assertFalse(ItemsAnnotation.empty(items().maxLength(0).build()));
		assertFalse(ItemsAnnotation.empty(items().min("foo").build()));
		assertFalse(ItemsAnnotation.empty(items().mini(0).build()));
		assertFalse(ItemsAnnotation.empty(items().minimum("foo").build()));
		assertFalse(ItemsAnnotation.empty(items().minItems(0).build()));
		assertFalse(ItemsAnnotation.empty(items().minl(0).build()));
		assertFalse(ItemsAnnotation.empty(items().minLength(0).build()));
		assertFalse(ItemsAnnotation.empty(items().mo("foo").build()));
		assertFalse(ItemsAnnotation.empty(items().multipleOf("foo").build()));
		assertFalse(ItemsAnnotation.empty(items().p("foo").build()));
		assertFalse(ItemsAnnotation.empty(items().pattern("foo").build()));
		assertFalse(ItemsAnnotation.empty(items().t("foo").build()));
		assertFalse(ItemsAnnotation.empty(items().type("foo").build()));
		assertFalse(ItemsAnnotation.empty(items().ui(true).build()));
		assertFalse(ItemsAnnotation.empty(items().uniqueItems(true).build()));
		assertFalse(ItemsAnnotation.empty(items().value(a("foo")).build()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//-----------------------------------------------------------------------------------------------------------------

	private static BodyAnnotation.Builder body() {
		return BodyAnnotation.create();
	}

	private static ContactAnnotation.Builder contact() {
		return ContactAnnotation.create();
	}

	private static FormDataAnnotation.Builder formData() {
		return FormDataAnnotation.create();
	}

	private static HasFormDataAnnotation.Builder hasFormData() {
		return HasFormDataAnnotation.create();
	}

	private static QueryAnnotation.Builder query() {
		return QueryAnnotation.create();
	}

	private static HasQueryAnnotation.Builder hasQuery() {
		return HasQueryAnnotation.create();
	}

	private static HeaderAnnotation.Builder header() {
		return HeaderAnnotation.create();
	}

	private static LicenseAnnotation.Builder license() {
		return LicenseAnnotation.create();
	}

	private static PathAnnotation.Builder path() {
		return PathAnnotation.create();
	}

	private static RequestAnnotation.Builder request() {
		return RequestAnnotation.create();
	}

	private static ResponseAnnotation.Builder response() {
		return ResponseAnnotation.create();
	}

	private static ResponseBodyAnnotation.Builder responseBody() {
		return ResponseBodyAnnotation.create();
	}

	private static ResponseHeaderAnnotation.Builder responseHeader() {
		return ResponseHeaderAnnotation.create();
	}

	private static ResponseStatusAnnotation.Builder responseStatus() {
		return ResponseStatusAnnotation.create();
	}

	private static TagAnnotation.Builder tag() {
		return TagAnnotation.create();
	}

	private static SchemaAnnotation.Builder schema() {
		return SchemaAnnotation.create();
	}

	private static ItemsAnnotation.Builder items() {
		return ItemsAnnotation.create();
	}

	private static SubItemsAnnotation.Builder subItems() {
		return SubItemsAnnotation.create();
	}

	private static ExternalDocsAnnotation.Builder externalDocs() {
		return ExternalDocsAnnotation.create();
	}

	private static String[] a(String...s) {
		return s;
	}

	private static int[] a(int...i) {
		return i;
	}
}