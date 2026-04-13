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
package org.apache.juneau.bean.openapi3.ui;

import static org.apache.juneau.bean.openapi3.OpenApiBuilder.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.html5.*;
import org.apache.juneau.TestBase;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link OpenApiUI}.
 */
class OpenApiUI_Test extends TestBase {

	private static final OpenApiUI UI = new OpenApiUI();
	private static final BeanSession SESSION = BeanContext.DEFAULT_SESSION;

	// Calls swap() with a minimal OpenApi document (no info, no paths, no tags).
	@Test void a01_swap_minimal() throws Exception {
		var doc = openApi().setOpenapi("3.0.0");
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
		assertInstanceOf(Div.class, result);
	}

	// Calls swap() with a fully-populated OpenApi document to cover all branches.
	@Test void a02_swap_fullDocument() throws Exception {
		var doc = openApi()
			.setInfo(
				info()
					.setTitle("Test API")
					.setVersion("1.0")
					.setDescription("This is a test API.\nWith multiple lines.")
					.setTermsOfService("https://example.com/tos")
					.setContact(
						contact()
							.setName("John Doe")
							.setEmail("john@example.com")
							.setUrl(URI.create("https://example.com"))
					)
					.setLicense(
						license()
							.setName("Apache 2.0")
							.setUrl(URI.create("https://www.apache.org/licenses/LICENSE-2.0"))
					)
			)
			.setExternalDocs(
				externalDocumentation()
					.setUrl(URI.create("https://docs.example.com"))
					.setDescription("API Documentation")
			)
			.setTags(
				tag()
					.setName("pets")
					.setDescription("Everything about pets")
					.setExternalDocs(
						externalDocumentation()
							.setUrl(URI.create("https://example.com/pets"))
							.setDescription("More about pets")
					),
				tag()
					.setName("store")
					.setDescription("Store operations")
			)
			.setPaths(m(
				"/pets", pathItem()
					.setGet(
						operation()
							.setOperationId("listPets")
							.setSummary("List all pets")
							.setDescription("Returns all pets from the system.")
							.setTags("pets")
							.setParameters(
								parameter()
									.setIn("query")
									.setName("limit")
									.setRequired(true)
									.setSchema(schemaInfo().setType("integer"))
									.setDescription("How many items to return")
							)
							.setResponses(m(
								"200", response()
									.setDescription("A list of pets")
									.setContent(m(
										"application/json", mediaType()
											.setSchema(schemaInfo().setType("array"))
									))
									.setHeaders(m(
										"x-next", headerInfo()
											.setDescription("A link to the next page")
									))
							))
					)
					.setPost(
						operation()
							.setOperationId("createPet")
							.setSummary("Create a pet")
							.setTags("pets")
							.setDeprecated(true)
							.setResponses(m(
								"201", response().setDescription("Null response")
							))
					),
				"/pets/{petId}", pathItem()
					.setGet(
						operation()
							.setOperationId("showPetById")
							.setTags("pets")
							.setParameters(
								parameter()
									.setIn("path")
									.setName("petId")
									.setRequired(true)
									.setDescription("The id of the pet")
							)
							.setResponses(m(
								"200", response()
									.setDescription("Expected response"),
								"default", response()
									.setDescription("Unexpected error")
									.setContent(m(
										"application/json", mediaType()
											.setSchema(schemaInfo().setRef("#/components/schemas/Error"))
									))
							))
					)
					.setPut(operation().setOperationId("updatePet").setTags("store"))
					.setDelete(operation().setOperationId("deletePet"))
					.setOptions(operation().setOperationId("optionsPet"))
					.setHead(operation().setOperationId("headPet"))
					.setPatch(operation().setOperationId("patchPet"))
					.setTrace(operation().setOperationId("tracePet"))
			))
			.setComponents(
				components()
					.setSchemas(m(
						"Pet", schemaInfo()
							.setType("object")
							.setDescription("A pet object"),
						"Error", schemaInfo()
							.setType("object")
							.setTitle("Error schema")
					))
			);

		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
		assertInstanceOf(Div.class, result);
		// Just check it produces output containing key HTML elements
		var html = result.toString();
		assertNotNull(html);
		assertTrue(html.contains("openapi-ui"));
	}

	// Tests forMediaTypes() to cover the method branch
	@Test void a03_forMediaTypes() {
		var mediaTypes = UI.forMediaTypes();
		assertNotNull(mediaTypes);
		assertEquals(1, mediaTypes.length);
		assertEquals(org.apache.juneau.commons.http.MediaType.HTML, mediaTypes[0]);
	}

	// Tests swap() with externalDocs having URL but no description (tagBlockSummary branch)
	@Test void a04_swap_tagWithExternalDocsNoDescription() throws Exception {
		var doc = openApi()
			.setTags(
				tag()
					.setName("test")
					.setExternalDocs(
						externalDocumentation()
							.setUrl(URI.create("https://example.com"))
						// No description - covers the 'else if (nn(ed))' branch in tagBlockSummary
					)
			);
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests swap() with a tag that has no externalDocs (tagBlockSummary null branch)
	@Test void a05_swap_tagWithNoExternalDocs() throws Exception {
		var doc = openApi()
			.setTags(
				tag()
					.setName("test")
					.setDescription("Simple tag with no external docs")
			);
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests swap() with operation parameters having no schema (covers null schema branch in tableContainer)
	@Test void a06_swap_parametersWithNoSchema() throws Exception {
		var doc = openApi()
			.setPaths(m(
				"/test", pathItem()
					.setGet(
						operation()
							.setParameters(
								parameter()
									.setIn("query")
									.setName("q")
									.setRequired(false)
								// No schema - covers nn(x.getSchema()) false branch
							)
							.setResponses(m(
								"200", response().setDescription("OK")
									.setContent(m("application/json", mediaType()))
								// mediaType with no schema
							))
					)
			));
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests swap() with response that has no content (covers empty examples branch)
	@Test void a07_swap_responseNoContent() throws Exception {
		var doc = openApi()
			.setPaths(m(
				"/test", pathItem()
					.setGet(
						operation()
							.setResponses(m(
								"204", response().setDescription("No Content")
								// No content map - covers null content branch in examples(Response)
							))
					)
			));
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests swap() with no-tag operations (untagged ops rendered first)
	@Test void a08_swap_operationWithNoTags() throws Exception {
		var doc = openApi()
			.setTags(tag().setName("myTag"))
			.setPaths(m(
				"/untagged", pathItem()
					.setGet(operation().setSummary("Untagged operation"))
				// This operation has no tags so it appears in the null-tag block
			));
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests toBRL with null string (covers null branch in toBRL)
	@Test void a09_swap_infoWithNullDescription() throws Exception {
		var doc = openApi()
			.setInfo(info().setTitle("Test").setVersion("1.0"));
		// No description, so toBRL is called with null
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests description with newlines (covers the '\n' split in toBRL)
	@Test void a10_swap_descriptionWithNewlines() throws Exception {
		var doc = openApi()
			.setInfo(info().setTitle("Test").setVersion("1.0").setDescription("Line1\nLine2\nLine3"));
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests license with URL and no name (covers ternary in header() for license)
	@Test void a11_swap_licenseWithUrlNoName() throws Exception {
		var doc = openApi()
			.setInfo(
				info()
					.setTitle("Test").setVersion("1.0")
					.setLicense(license().setUrl(URI.create("https://example.com/license")))
					// No name - covers 'nn(l.getName()) ? l.getName() : l.getUrl()' false branch
			);
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests externalDocs with no description (covers ternary in header() for externalDocs)
	@Test void a12_swap_externalDocsNoDescription() throws Exception {
		var doc = openApi()
			.setExternalDocs(
				externalDocumentation().setUrl(URI.create("https://docs.example.com"))
				// No description - covers 'nn(ed.getDescription()) ? ed.getDescription() : ed.getUrl()' false branch
			);
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests models block with multiple schemas (covers modelsBlockContents)
	@Test void a13_swap_modelsBlock() throws Exception {
		var doc = openApi()
			.setComponents(
				components()
					.setSchemas(m(
						"ModelA", schemaInfo().setType("object").setDescription("Model A description"),
						"ModelB", schemaInfo().setType("string")
						// ModelB has no description - covers null description branch in modelBlockSummary
					))
			);
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests response with multiple content types (examplesDiv with multiple entries)
	@Test void a14_swap_responseWithMultipleContentTypes() throws Exception {
		var doc = openApi()
			.setPaths(m(
				"/test", pathItem()
					.setGet(
						operation()
							.setResponses(m(
								"200", response()
									.setDescription("OK")
									.setContent(m(
										"application/json", mediaType().setSchema(schemaInfo().setType("object")),
										"application/xml", mediaType().setSchema(schemaInfo().setType("object"))
										// Two content types triggers the select dropdown in examplesDiv
									))
							))
					)
			));
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests operation with standard methods not in STANDARD_METHODS (covers 'other' class branch)
	@Test void a15_swap_customHttpMethod() throws Exception {
		var doc = openApi()
			.setPaths(m(
				"/test", pathItem()
					.set("custom", operation().setSummary("Custom method"))
			));
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests info with TermsOfService that is a URI (covers isUri() branch)
	@Test void a16_swap_termsOfServiceAsUri() throws Exception {
		var doc = openApi()
			.setInfo(
				info()
					.setTitle("Test").setVersion("1.0")
					.setTermsOfService("https://example.com/tos")
			);
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests info with TermsOfService that is not a URI (covers !isUri() branch)
	@Test void a17_swap_termsOfServiceNotUri() throws Exception {
		var doc = openApi()
			.setInfo(
				info()
					.setTitle("Test").setVersion("1.0")
					.setTermsOfService("See our website for terms")
			);
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests response with null headers (covers getHeaders() == null branch)
	@Test void a18_swap_responseWithNullHeaders() throws Exception {
		var doc = openApi()
			.setPaths(m(
				"/test", pathItem()
					.setGet(
						operation()
							.setResponses(m(
								"200", response()
									.setDescription("OK")
									// No headers - covers getHeaders() == null branch in headers(Response)
							))
					)
			));
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests info with no version (covers nn(info.getVersion()) false branch)
	@Test void a19_swap_infoWithNoVersion() throws Exception {
		var doc = openApi()
			.setInfo(info().setTitle("Test"));
		// No version - covers false branch of nn(info.getVersion())
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests contact with only email set - no name or url (covers false branches of nn(name) and nn(url))
	@Test void a20_swap_contactWithOnlyEmail() throws Exception {
		var doc = openApi()
			.setInfo(
				info().setTitle("Test").setVersion("1.0")
					.setContact(contact().setEmail("test@example.com"))
				// No name, no url - covers false branches of nn(c.getName()) and nn(c.getUrl())
			);
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests contact with only url - no name or email (covers nn(c.getEmail()) false branch)
	@Test void a21_swap_contactWithOnlyUrl() throws Exception {
		var doc = openApi()
			.setInfo(
				info().setTitle("Test").setVersion("1.0")
					.setContact(contact().setUrl(URI.create("https://example.com")))
				// No name, no email
			);
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests license with name but no URL (covers nn(l.getUrl()) false branch → uses name as child)
	@Test void a22_swap_licenseWithNameNoUrl() throws Exception {
		var doc = openApi()
			.setInfo(
				info().setTitle("Test").setVersion("1.0")
					.setLicense(license().setName("Apache 2.0"))
				// Name only, no URL - covers nn(l.getUrl()) false branch in header()
			);
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests externalDocs with description but no URL (covers nn(ed.getUrl()) false branch)
	@Test void a23_swap_externalDocsWithDescriptionNoUrl() throws Exception {
		var doc = openApi()
			.setExternalDocs(
				externalDocumentation().setDescription("Some docs description")
				// No URL - covers nn(ed.getUrl()) false branch → child = ed.getDescription()
			);
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests components with no schemas (covers modelsBlockContents null-schemas branch)
	@Test void a24_swap_componentsWithNoSchemas() throws Exception {
		var doc = openApi()
			.setComponents(
				components()
				// No schemas set - covers nn(getComponents().getSchemas()) false branch
			);
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests description with comma and newlines (covers toBRL false branch of indexOf(',') == -1)
	@Test void a25_swap_descriptionWithCommaAndNewlines() throws Exception {
		var doc = openApi()
			.setInfo(
				info().setTitle("Test").setVersion("1.0")
					.setDescription("First line,\nSecond line,\nThird line")
				// Has comma and newlines - covers toBRL false branch (indexOf(',') != -1)
			);
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests operation matching tag by name (covers addOperationIfTagMatches branch)
	@Test void a26_swap_operationWithNonMatchingTag() throws Exception {
		var doc = openApi()
			.setTags(
				tag().setName("tagA"),
				tag().setName("tagB")
			)
			.setPaths(m(
				"/test", pathItem()
					.setGet(
						operation()
							.setTags("tagA")
						// tagA matches the first tag block, tagB doesn't match
					)
			));
		// This covers the non-matching tag branch in addOperationIfTagMatches
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests modelsBlockContents with schemas (another path ensuring models block renders)
	@Test void a27_swap_componentsWithNullSchemasMap() throws Exception {
		// A minimal openApi with components but getSchemas() returns null
		var doc = openApi()
			.setComponents(components());
		// Explicitly set no schemas - getSchemas() returns null → nn check fails
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests info + externalDocs with URL but no description (covers line 191 false branch inside info block)
	@Test void a28_swap_infoWithExternalDocsUrlNoDescription() throws Exception {
		var doc = openApi()
			.setInfo(info().setTitle("Test").setVersion("1.0"))
			.setExternalDocs(
				externalDocumentation().setUrl(URI.create("https://docs.example.com"))
				// No description - covers nn(ed.getDescription()) false branch at line 191
			);
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests info + externalDocs with description but no URL (covers line 192 false branch)
	@Test void a29_swap_infoWithExternalDocsDescriptionNoUrl() throws Exception {
		var doc = openApi()
			.setInfo(info().setTitle("Test").setVersion("1.0"))
			.setExternalDocs(
				externalDocumentation().setDescription("API docs")
				// No URL - covers nn(ed.getUrl()) false branch at line 192
			);
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests parameter with required=false (covers nn(x.getRequired()) true branch but value false)
	@Test void a30_swap_parameterWithRequiredFalse() throws Exception {
		var doc = openApi()
			.setPaths(m(
				"/test", pathItem()
					.setGet(
						operation()
							.setParameters(
								parameter()
									.setIn("query")
									.setName("q")
									.setRequired(false)
								// required=false - covers nn(true) && false = false path at line 272
							)
					)
			));
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests tag with externalDocs having description but no URL (covers nn(ed) true but nn(ed.getUrl()) false branch)
	@Test void a31_swap_tagWithExternalDocsDescriptionNoUrl() throws Exception {
		var doc = openApi()
			.setTags(
				tag()
					.setName("test")
					.setExternalDocs(
						externalDocumentation().setDescription("Some tag docs")
						// No URL - covers nn(ed) && nn(ed.getUrl()) false branch at line 348
					)
			);
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}
}
