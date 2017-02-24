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

/**
 * Various useful static methods for creating Swagger elements.
 * <p>
 * Refer to <a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.dto.swagger</a> for usage information.
 */
public class SwaggerBuilder {

	/**
	 * Creates an empty {@link Contact} element.
	 * @return The new element.
	 */
	public static final Contact contact() {
		return new Contact();
	}

	/**
	 * Creates an {@link Contact} element with the specified {@link Contact#name(String)} attribute.
	 * @param name The {@link Contact#name(String)} attribute.
	 * @return The new element.
	 */
	public static final Contact contact(String name) {
		return contact().name(name);
	}

	/**
	 * Creates an {@link Contact} element with the specified {@link Contact#name(String)}, {@link Contact#url(String)}, and {@link Contact#email(String)}, attributes.
	 * @param name The {@link Contact#name(String)} attribute.
	 * @param url The {@link Contact#url(String)} attribute.
	 * @param email The {@link Contact#email(String)} attribute.
	 * @return The new element.
	 */
	public static final Contact contact(String name, String url, String email) {
		return contact().name(name).url(url).email(email);
	}

	/**
	 * Creates an empty {@link ExternalDocumentation} element.
	 * @return The new element.
	 */
	public static final ExternalDocumentation externalDocumentation() {
		return new ExternalDocumentation();
	}

	/**
	 * Creates an {@link ExternalDocumentation} element with the specified {@link ExternalDocumentation#url(String)} attribute.
	 * @param url The {@link ExternalDocumentation#url(String)} attribute.
	 * @return The new element.
	 */
	public static final ExternalDocumentation externalDocumentation(String url) {
		return externalDocumentation().url(url);
	}

	/**
	 * Creates an {@link ExternalDocumentation} element with the specified {@link ExternalDocumentation#url(String)} and {@link ExternalDocumentation#description(String)} attributes.
	 * @param url The {@link ExternalDocumentation#url(String)} attribute.
	 * @param description The {@link ExternalDocumentation#description(String)} attribute.
	 * @return The new element.
	 */
	public static final ExternalDocumentation externalDocumentation(String url, String description) {
		return externalDocumentation().url(url).description(description);
	}

	/**
	 * Creates an empty {@link HeaderInfo} element.
	 * @return The new element.
	 */
	public static final HeaderInfo headerInfo() {
		return new HeaderInfo();
	}

	/**
	 * Creates an {@link HeaderInfo} element with the specified {@link HeaderInfo#type(String)} attribute.
	 * @param type The {@link HeaderInfo#type(String)} attribute.
	 * @return The new element.
	 */
	public static final HeaderInfo headerInfo(String type) {
		return headerInfo().type(type);
	}

	/**
	 * Creates an {@link HeaderInfo} element with the specified {@link HeaderInfo#type(String)} attribute.
	 * @param type The {@link HeaderInfo#type(String)} attribute.
	 * @return The new element.
	 */
	public static final HeaderInfo headerInfoStrict(String type) {
		return headerInfo().strict().type(type);
	}

	/**
	 * Creates an empty {@link Info} element.
	 * @return The new element.
	 */
	public static final Info info() {
		return new Info();
	}

	/**
	 * Creates an {@link Info} element with the specified {@link Info#title(String)} and {@link Info#version(String)} attributes.
	 * @param title The {@link Info#title(String)} attribute.
	 * @param version The {@link Info#version(String)} attribute.
	 * @return The new element.
	 */
	public static final Info info(String title, String version) {
		return info().title(title).version(version);
	}

	/**
	 * Creates an empty {@link Items} element.
	 * @return The new element.
	 */
	public static final Items items() {
		return new Items();
	}

	/**
	 * Creates an {@link Items} element with the specified {@link Items#type(String)} attribute.
	 * @param type The {@link Items#type(String)} attribute.
	 * @return The new element.
	 */
	public static final Items items(String type) {
		return items().type(type);
	}

	/**
	 * Creates an {@link Items} element with the specified {@link Items#type(String)} attribute.
	 * @param type The {@link Items#type(String)} attribute.
	 * @return The new element.
	 */
	public static final Items itemsStrict(String type) {
		return items().strict().type(type);
	}

	/**
	 * Creates an empty {@link License} element.
	 * @return The new element.
	 */
	public static final License license() {
		return new License();
	}

	/**
	 * Creates an {@link License} element with the specified {@link License#name(String)} attribute.
	 * @param name The {@link License#name(String)} attribute.
	 * @return The new element.
	 */
	public static final License license(String name) {
		return license().name(name);
	}

	/**
	 * Creates an empty {@link Operation} element.
	 * @return The new element.
	 */
	public static final Operation operation() {
		return new Operation();
	}

	/**
	 * Creates an empty {@link ParameterInfo} element.
	 * @return The new element.
	 */
	public static final ParameterInfo parameterInfo() {
		return new ParameterInfo();
	}

	/**
	 * Creates an {@link ParameterInfo} element with the specified {@link ParameterInfo#in(String)} and {@link ParameterInfo#name(String)} attributes.
	 * @param in The {@link ParameterInfo#in(String)} attribute.
	 * @param name The {@link ParameterInfo#name(String)} attribute.
	 * @return The new element.
	 */
	public static final ParameterInfo parameterInfo(String in, String name) {
		return parameterInfo().in(in).name(name);
	}

	/**
	 * Creates an {@link ParameterInfo} element with the specified {@link ParameterInfo#in(String)} and {@link ParameterInfo#name(String)} attributes.
	 * @param in The {@link ParameterInfo#in(String)} attribute.
	 * @param name The {@link ParameterInfo#name(String)} attribute.
	 * @return The new element.
	 */
	public static final ParameterInfo parameterInfoStrict(String in, String name) {
		return parameterInfo().strict().in(in).name(name);
	}

	/**
	 * Creates an empty {@link ResponseInfo} element.
	 * @return The new element.
	 */
	public static final ResponseInfo responseInfo() {
		return new ResponseInfo();
	}

	/**
	 * Creates an {@link ResponseInfo} element with the specified {@link ResponseInfo#description(String)} attribute.
	 * @param description The {@link ResponseInfo#description(String)} attribute.
	 * @return The new element.
	 */
	public static final ResponseInfo responseInfo(String description) {
		return responseInfo().description(description);
	}

	/**
	 * Creates an empty {@link SchemaInfo} element.
	 * @return The new element.
	 */
	public static final SchemaInfo schemaInfo() {
		return new SchemaInfo();
	}

	/**
	 * Creates an empty {@link SecurityScheme} element.
	 * @return The new element.
	 */
	public static final SecurityScheme securityScheme() {
		return new SecurityScheme();
	}

	/**
	 * Creates an {@link SecurityScheme} element with the specified {@link SecurityScheme#type(String)} attribute.
	 * @param type The {@link SecurityScheme#type(String)} attribute.
	 * @return The new element.
	 */
	public static final SecurityScheme securityScheme(String type) {
		return securityScheme().type(type);
	}

	/**
	 * Creates an {@link SecurityScheme} element with the specified {@link SecurityScheme#type(String)} attribute.
	 * @param type The {@link SecurityScheme#type(String)} attribute.
	 * @return The new element.
	 */
	public static final SecurityScheme securitySchemeStrict(String type) {
		return securityScheme().strict().type(type);
	}

	/**
	 * Creates an empty {@link Swagger} element.
	 * @return The new element.
	 */
	public static final Swagger swagger() {
		return new Swagger();
	}

	/**
	 * Creates an {@link Swagger} element with the specified {@link Swagger#info(Info)} attribute.
	 * @param info The {@link Swagger#info(Info)} attribute.
	 * @return The new element.
	 */
	public static final Swagger swagger(Info info) {
		return swagger().info(info);
	}

	/**
	 * Creates an empty {@link Tag} element.
	 * @return The new element.
	 */
	public static final Tag tag() {
		return new Tag();
	}

	/**
	 * Creates an {@link Tag} element with the specified {@link Tag#name(String)} attribute.
	 * @param name The {@link Tag#name(String)} attribute.
	 * @return The new element.
	 */
	public static final Tag tag(String name) {
		return tag().name(name);
	}

	/**
	 * Creates an empty {@link Xml} element.
	 * @return The new element.
	 */
	public static final Xml xml() {
		return new Xml();
	}
}
