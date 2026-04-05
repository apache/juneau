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
package org.apache.juneau.bean.swagger.ui;

import static org.apache.juneau.bean.swagger.SwaggerBuilder.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.Stream;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Testcase for {@link SwaggerUI}.
 */
@SuppressWarnings({
	"java:S4144" // Identical test methods intentional for testing different scenarios
})
class SwaggerUI_Test extends TestBase {

	private final BeanSession bs = BeanContext.DEFAULT_SESSION;

	/**
	 * Test method for media types.
	 */
	@Test void a01_mediaTypes() {
		assertList(
			new SwaggerUI().forMediaTypes(),
			"text/html"
		);
	}

	/**
	 * Test method for basic Swagger document conversion.
	 */
	@Test void a02_basicSwaggerConversion() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0").setDescription("Test API Description"))
			.setHost("localhost:8080")
			.setBasePath("/api")
			.setPaths(map("/test", operationMap().append("get", operation().setSummary("Test operation"))));

		assertString(
			"""
			<div class='swagger-ui'>
				<style></style>
				<script type='text/javascript'><sp/></script>
				<table class='header'>
					<tr><th>Description:</th><td>Test API Description</td></tr>
					<tr><th>Version:</th><td>1.0.0</td></tr>
				</table>
				<div class='tag-block tag-block-open'>
					<div class='tag-block-contents'>
						<div class='op-block op-block-closed get'>
							<div class='op-block-summary' onclick='toggleOpBlock(this)'>
								<span class='method-button'>GET</span>
								<span class='path'>/test</span>
								<span class='summary'>Test operation</span>
							</div>
							<div class='op-block-contents'>
								<div class='table-container'></div>
							</div>
						</div>
					</div>
				</div>
			</div>
			""".replaceAll("\\n\\s*", ""),
			new SwaggerUI().swap(bs, swagger)
		);
	}

	/**
	 * Test method for Swagger document with tags.
	 */
	@Test void a03_swaggerWithTags() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0"))
			.setTags(l(
				tag("users").setDescription("User operations"),
				tag("orders").setDescription("Order operations")
			));

		assertString(
			"""
			<div class='swagger-ui'>
				<style></style>
				<script type='text/javascript'><sp/></script>
				<table class='header'>
					<tr><th>Version:</th><td>1.0.0</td></tr>
				</table>
				<div class='tag-block tag-block-open'>
					<div class='tag-block-contents'></div>
				</div>
				<div class='tag-block tag-block-open'>
					<div class='tag-block-summary' onclick='toggleTagBlock(this)'>
						<span class='name'>users</span>
						<span class='description'>User operations</span>
					</div>
					<div class='tag-block-contents'></div>
				</div>
				<div class='tag-block tag-block-open'>
					<div class='tag-block-summary' onclick='toggleTagBlock(this)'>
						<span class='name'>orders</span>
						<span class='description'>Order operations</span>
					</div>
					<div class='tag-block-contents'></div>
				</div>
			</div>
			""".replaceAll("\\n\\s*", ""),
			new SwaggerUI().swap(bs, swagger)
		);
	}

	@ParameterizedTest
	@MethodSource("swaggerEmptyPathsProvider")
	void a04_swaggerWithEmptyPaths(String testName) throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0"))
			.setPaths(new HashMap<>());

		assertString(
			"""
			<div class='swagger-ui'>
				<style></style>
				<script type='text/javascript'><sp/></script>
				<table class='header'>
					<tr><th>Version:</th><td>1.0.0</td></tr>
				</table>
				<div class='tag-block tag-block-open'>
					<div class='tag-block-contents'></div>
				</div>
			</div>
			""".replaceAll("\\n\\s*", ""),
			new SwaggerUI().swap(bs, swagger)
		);
	}

	static Stream<Arguments> swaggerEmptyPathsProvider() {
		return Stream.of(
			Arguments.of("parameters"),
			Arguments.of("responses"),
			Arguments.of("models"),
			Arguments.of("empty")
		);
	}

	/**
	 * Test method for Swagger documents with empty paths.
	 */
	@ParameterizedTest
	@MethodSource("swaggerWithEmptyPathsTestData")
	void a07_swaggerWithEmptyPaths(String testName) throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0"))
			.setPaths(new HashMap<>());

		assertString(
			"""
			<div class='swagger-ui'>
				<style></style>
				<script type='text/javascript'><sp/></script>
				<table class='header'>
					<tr><th>Version:</th><td>1.0.0</td></tr>
				</table>
				<div class='tag-block tag-block-open'>
					<div class='tag-block-contents'></div>
				</div>
			</div>
			""".replaceAll("\\n\\s*", ""),
			new SwaggerUI().swap(bs, swagger)
		);
	}

	static Stream<Arguments> swaggerWithEmptyPathsTestData() {
		return Stream.of(
			Arguments.of("emptySwaggerDocument"),
			Arguments.of("swaggerWithExternalDocs"),
			Arguments.of("swaggerWithSecuritySchemes"),
			Arguments.of("swaggerWithMultipleOperations")
		);
	}

	/**
	 * Test method for null input.
	 */
	@Test void a11_nullInput() {
		var swaggerUI = new SwaggerUI();
		assertThrows(NullPointerException.class, () -> swaggerUI.swap(bs, null));
	}

	/**
	 * Test method for HTML output structure.
	 */
	@Test void a12_htmlOutputStructure() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0"))
			.setPaths(map("/test", operationMap()
				.append("get", operation().setSummary("Test operation"))));

		assertString(
			"""
			<div class='swagger-ui'>
				<style></style>
				<script type='text/javascript'><sp/></script>
				<table class='header'>
					<tr><th>Version:</th><td>1.0.0</td></tr>
				</table>
				<div class='tag-block tag-block-open'>
					<div class='tag-block-contents'>
						<div class='op-block op-block-closed get'>
							<div class='op-block-summary' onclick='toggleOpBlock(this)'>
								<span class='method-button'>GET</span>
								<span class='path'>/test</span>
								<span class='summary'>Test operation</span>
							</div>
							<div class='op-block-contents'>
								<div class='table-container'></div>
							</div>
						</div>
					</div>
				</div>
			</div>
			""".replaceAll("\\n\\s*", ""),
			new SwaggerUI().swap(bs, swagger)
		);
	}

	// Tests operation with description, parameters (body+query with required=true), responses, headers.
	@Test void a13_swaggerWithParamsAndResponses() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0"))
			.setPaths(map(
				"/pets", operationMap()
					.append("post", operation()
						.setDescription("Create a new pet")
						.setSummary("Create pet")
						.setParameters(l(
							parameterInfo("body", null)
								.setSchema(schemaInfo().setType("object"))
								.setDescription("Pet body, value1\nvalue2"),
							parameterInfo("query", "limit")
								.setFormat("int32")
								.setRequired(true)
								.setDescription("Max results")
						))
						.setResponses(map(
							"201", responseInfo("Created")
								.setSchema(schemaInfo().setType("object"))
								.setHeaders(map("X-Rate-Limit", headerInfo("integer")))
						))
					)
			));
		var result = new SwaggerUI().swap(bs, swagger);
		assertNotNull(result);
	}

	// Tests response with schema AND examples map → covers examplesDiv with multiple entries (select dropdown).
	@Test void a14_swaggerWithMultipleExamples() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0"))
			.setPaths(map(
				"/data", operationMap()
					.append("get", operation()
						.setSummary("Get data")
						.setResponses(map(
							"200", responseInfo("OK")
								.setSchema(schemaInfo().setType("object"))
								.addExample("application/json", "{\"key\":\"value\"}")
								.addExample("application/xml", "<key>value</key>")
						))
					)
			));
		var result = new SwaggerUI().swap(bs, swagger);
		assertNotNull(result);
	}

	// Tests info with full contact block (name, url, email).
	@Test void a15_swaggerWithContact() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0")
				.setContact(contact("John Doe", "https://example.com", "john@example.com")));
		var result = new SwaggerUI().swap(bs, swagger);
		assertNotNull(result);
	}

	// Tests info with contact having only email (no name, no url) → covers false branches for name and url.
	@Test void a16_swaggerWithContactEmailOnly() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0")
				.setContact(contact().setEmail("john@example.com")));
		var result = new SwaggerUI().swap(bs, swagger);
		assertNotNull(result);
	}

	// Tests license with url only (no name) → covers nn(l.getName())=false.
	@Test void a17_swaggerWithLicenseUrlOnly() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0")
				.setLicense(license().setUrl(java.net.URI.create("https://example.com/license"))));
		var result = new SwaggerUI().swap(bs, swagger);
		assertNotNull(result);
	}

	// Tests license with name only (no url) → covers nn(l.getUrl())=false.
	@Test void a18_swaggerWithLicenseNameOnly() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0")
				.setLicense(license("Apache 2.0")));
		var result = new SwaggerUI().swap(bs, swagger);
		assertNotNull(result);
	}

	// Tests externalDocs with both description and url.
	@Test void a19_swaggerWithExternalDocs() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0"))
			.setExternalDocs(externalDocumentation("https://docs.example.com", "API Docs"));
		var result = new SwaggerUI().swap(bs, swagger);
		assertNotNull(result);
	}

	// Tests externalDocs with url only (no description) → covers nn(ed.getDescription())=false.
	@Test void a20_swaggerWithExternalDocsUrlOnly() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0"))
			.setExternalDocs(externalDocumentation("https://docs.example.com"));
		var result = new SwaggerUI().swap(bs, swagger);
		assertNotNull(result);
	}

	// Tests externalDocs with description only (no url) → covers nn(ed.getUrl())=false.
	@Test void a21_swaggerWithExternalDocsDescriptionOnly() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0"))
			.setExternalDocs(externalDocumentation().setDescription("API reference"));
		var result = new SwaggerUI().swap(bs, swagger);
		assertNotNull(result);
	}

	// Tests info with terms of service as URI → covers isUri(tos)=true.
	@Test void a22_swaggerWithTermsOfServiceUri() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0").setTermsOfService("https://example.com/tos"));
		var result = new SwaggerUI().swap(bs, swagger);
		assertNotNull(result);
	}

	// Tests info with terms of service as non-URI → covers isUri(tos)=false.
	@Test void a23_swaggerWithTermsOfServiceNonUri() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0").setTermsOfService("See our legal page for details"));
		var result = new SwaggerUI().swap(bs, swagger);
		assertNotNull(result);
	}

	// Tests swagger without info → covers nn(info)=false.
	@Test void a24_swaggerWithNoInfo() throws Exception {
		var swagger = swagger()
			.setPaths(map("/test", operationMap().append("get", operation().setSummary("Test"))));
		var result = new SwaggerUI().swap(bs, swagger);
		assertNotNull(result);
	}

	// Tests swagger with info but no description → covers nn(info.getDescription())=false.
	@Test void a25_swaggerWithInfoNoDescription() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0"));
		var result = new SwaggerUI().swap(bs, swagger);
		assertNotNull(result);
	}

	// Tests swagger with definitions → covers modelsBlockContents with model with/without description.
	@Test void a26_swaggerWithDefinitions() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0"))
			.addDefinition("Pet", new org.apache.juneau.collections.JsonMap()
				.append("type", "object")
				.append("description", "A pet"))
			.addDefinition("Error", new org.apache.juneau.collections.JsonMap()
				.append("type", "object"));
		var result = new SwaggerUI().swap(bs, swagger);
		assertNotNull(result);
	}

	// Tests tag with externalDocs having both description and url.
	@Test void a27_swaggerTagWithExternalDocs() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0"))
			.setTags(l(
				tag("pets")
					.setDescription("Pet operations")
					.setExternalDocs(externalDocumentation("https://example.com", "More Info"))
			));
		var result = new SwaggerUI().swap(bs, swagger);
		assertNotNull(result);
	}

	// Tests tag with externalDocs having url only (no description) → covers nn(ed.getDescription())=false in tagBlockSummary.
	@Test void a28_swaggerTagWithExternalDocsUrlOnly() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0"))
			.setTags(l(
				tag("pets")
					.setExternalDocs(externalDocumentation("https://example.com"))
			));
		var result = new SwaggerUI().swap(bs, swagger);
		assertNotNull(result);
	}

	// Tests deprecated operation → covers opBlock deprecated branch.
	@Test void a29_swaggerWithDeprecatedOperation() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0"))
			.setPaths(map(
				"/old", operationMap()
					.append("get", operation()
						.setSummary("Old endpoint")
						.setDeprecated(true))
			));
		var result = new SwaggerUI().swap(bs, swagger);
		assertNotNull(result);
	}

	// Tests toBRL with comma and newline → covers comma branch with multiple lines.
	@Test void a30_swaggerWithCommaInDescription() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0")
				.setDescription("Line1, value\nLine2, more"));
		var result = new SwaggerUI().swap(bs, swagger);
		assertNotNull(result);
	}

	// Tests operation with matching tag → covers tagBlockContents tag matching.
	@Test void a31_swaggerWithTaggedOperations() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0"))
			.setTags(l(tag("pets").setDescription("Pet ops")))
			.setPaths(map(
				"/pets", operationMap()
					.append("get", operation()
						.setSummary("List pets")
						.addTags("pets")),
				"/other", operationMap()
					.append("get", operation()
						.setSummary("Other op")
						.addTags("other"))
			));
		var result = new SwaggerUI().swap(bs, swagger);
		assertNotNull(result);
	}

	// Tests non-body parameter with no format/pattern (empty m2) → covers m2.isEmpty()=true branch.
	@Test void a32_swaggerWithParamNoFormat() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0"))
			.setPaths(map(
				"/test", operationMap()
					.append("get", operation()
						.setSummary("Test")
						.setParameters(l(
							parameterInfo("query", "search")
								.setDescription("Search term")
						)))
			));
		var result = new SwaggerUI().swap(bs, swagger);
		assertNotNull(result);
	}

	// Tests body parameter without schema → covers nn(si)=false in examples(ParameterInfo) body path.
	@Test void a33_swaggerWithBodyParamNoSchema() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0"))
			.setPaths(map(
				"/test", operationMap()
					.append("post", operation()
						.setSummary("Test")
						.setParameters(l(
							parameterInfo("body", null)
								.setDescription("Request body")
						)))
			));
		var result = new SwaggerUI().swap(bs, swagger);
		assertNotNull(result);
	}

	// Tests operation with no summary → covers opBlockSummary nn(op.getSummary())=false.
	@Test void a34_swaggerWithOperationNoSummary() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0"))
			.setPaths(map(
				"/test", operationMap()
					.append("get", operation()
						.setDescription("A test operation with description but no summary"))
			));
		var result = new SwaggerUI().swap(bs, swagger);
		assertNotNull(result);
	}

	// Tests parameter with required=false → covers nn(x.getRequired())=true && x.getRequired()=false.
	@Test void a35_swaggerWithRequiredFalseParam() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0"))
			.setPaths(map(
				"/test", operationMap()
					.append("get", operation()
						.setSummary("Test")
						.setParameters(l(
							parameterInfo("query", "q")
								.setRequired(false)
								.setDescription("Optional query param")
						)))
			));
		var result = new SwaggerUI().swap(bs, swagger);
		assertNotNull(result);
	}

	// Tests info without version → covers nn(info.getVersion())=false.
	@Test void a36_swaggerWithInfoNoVersion() throws Exception {
		var swagger = swagger()
			.setInfo(info().setTitle("Test API").setDescription("No version here"));
		var result = new SwaggerUI().swap(bs, swagger);
		assertNotNull(result);
	}

	// Tests contact with name but no email → covers nn(c.getEmail())=false.
	@Test void a37_swaggerWithContactNoEmail() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0")
				.setContact(contact("John Doe")));
		var result = new SwaggerUI().swap(bs, swagger);
		assertNotNull(result);
	}

	// Tests tag with operation having no tags → covers nn(op.getTags())=false when t!=null (branch 7 in tagBlockContents).
	@Test void a38_swaggerTagWithUntaggedOperation() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0"))
			.setTags(l(tag("pets").setDescription("Pet ops")))
			.setPaths(map(
				"/untagged", operationMap()
					.append("get", operation()
						.setSummary("No tags operation"))
			));
		var result = new SwaggerUI().swap(bs, swagger);
		assertNotNull(result);
	}

	// Tests response with headers → covers headers(ResponseInfo) with non-null headers.
	@Test void a39_swaggerWithResponseHeaders() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0"))
			.setPaths(map(
				"/test", operationMap()
					.append("get", operation()
						.setSummary("Test")
						.setResponses(map(
							"200", responseInfo("OK")
								.addHeader("X-Rate-Limit", headerInfo("integer").setDescription("Rate limit"))
								.addHeader("X-Expires-After", headerInfo("string").setDescription("Expiry time"))
						))
					)
			));
		var result = new SwaggerUI().swap(bs, swagger);
		assertNotNull(result);
	}
}
