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

import java.net.*;

import org.apache.juneau.*;
import org.apache.juneau.common.internal.*;

/**
 * Various useful static methods for creating OpenAPI elements.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanOpenApi3">juneau-bean-openapi3</a>
 * </ul>
 */
public class OpenApiBuilder {

	/**
	 * Constructor.
	 */
	protected OpenApiBuilder() {}

	/**
	 * Creates an empty {@link Contact} element.
	 *
	 * @return The new element.
	 */
	public static final Contact contact() {
		return new Contact();
	}

	/**
	 * Creates an {@link Contact} element with the specified {@link Contact#setName(String) name} attribute.
	 *
	 * @param name The {@link Contact#setName(String) name} attribute.
	 * @return The new element.
	 */
	public static final Contact contact(String name) {
		return contact().setName(name);
	}

	/**
	 * Creates an {@link Contact} element with the specified {@link Contact#setName(String) name}, {@link Contact#setUrl(URI) url},
	 * and {@link Contact#setEmail(String) email} attributes.
	 *
	 * @param name The {@link Contact#setName(String) name} attribute.
	 * @param url
	 * 	The {@link Contact#setUrl(URI) url} attribute.
	 * 	<br>The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * 	<br>Strings must be valid URIs.
	 * 	<br>URIs defined by {@link UriResolver} can be used for values.
	 * @param email The {@link Contact#setEmail(String) email} attribute.
	 * @return The new element.
	 */
	public static final Contact contact(String name, Object url, String email) {
		return contact().setName(name).setUrl(StringUtils.toURI(url)).setEmail(email);
	}

	/**
	 * Creates an empty {@link ExternalDocumentation} element.
	 *
	 * @return The new element.
	 */
	public static final ExternalDocumentation externalDocumentation() {
		return new ExternalDocumentation();
	}

	/**
	 * Creates an {@link ExternalDocumentation} element with the specified {@link ExternalDocumentation#setUrl(URI) url}
	 * attribute.
	 *
	 * @param url
	 * 	The {@link ExternalDocumentation#setUrl(URI) url} attribute.
	 * 	<br>The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * 	<br>Strings must be valid URIs.
	 * 	<br>URIs defined by {@link UriResolver} can be used for values.
	 * @return The new element.
	 */
	public static final ExternalDocumentation externalDocumentation(Object url) {
		return externalDocumentation().setUrl(StringUtils.toURI(url));
	}

	/**
	 * Creates an {@link ExternalDocumentation} element with the specified {@link ExternalDocumentation#setUrl(URI) url}
	 * and {@link ExternalDocumentation#setDescription(String) description} attributes.
	 *
	 * @param url
	 * 	The {@link ExternalDocumentation#setUrl(URI) url} attribute.
	 * 	<br>The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * 	<br>Strings must be valid URIs.
	 * 	<br>URIs defined by {@link UriResolver} can be used for values.
	 * @param description The {@link ExternalDocumentation#setDescription(String) description} attribute.
	 * @return The new element.
	 */
	public static final ExternalDocumentation externalDocumentation(Object url, String description) {
		return externalDocumentation().setUrl(StringUtils.toURI(url)).setDescription(description);
	}

	/**
	 * Creates an empty {@link HeaderInfo} element.
	 *
	 * @return The new element.
	 */
	public static final HeaderInfo headerInfo() {
		return new HeaderInfo();
	}

	/**
	 * Creates an {@link HeaderInfo} element with the specified {@link HeaderInfo#setSchema(SchemaInfo) schema} attribute.
	 *
	 * @param schema The {@link HeaderInfo#setSchema(SchemaInfo) schema} attribute.
	 * @return The new element.
	 */
	public static final HeaderInfo headerInfo(SchemaInfo schema) {
		return headerInfo().setSchema(schema);
	}

	/**
	 * Creates an empty {@link Info} element.
	 *
	 * @return The new element.
	 */
	public static final Info info() {
		return new Info();
	}

	/**
	 * Creates an {@link Info} element with the specified {@link Info#setTitle(String) title} and {@link Info#setVersion(String) version}
	 * attributes.
	 *
	 * @param title The {@link Info#setTitle(String) title} attribute.
	 * @param version The {@link Info#setVersion(String) version} attribute.
	 * @return The new element.
	 */
	public static final Info info(String title, String version) {
		return info().setTitle(title).setVersion(version);
	}

	/**
	 * Creates an empty {@link Items} element.
	 *
	 * @return The new element.
	 */
	public static final Items items() {
		return new Items();
	}

	/**
	 * Creates an {@link Items} element with the specified {@link Items#setType(String) type} attribute.
	 *
	 * @param type The {@link Items#setType(String) type} attribute.
	 * @return The new element.
	 */
	public static final Items items(String type) {
		return items().setType(type);
	}

	/**
	 * Creates an empty {@link License} element.
	 *
	 * @return The new element.
	 */
	public static final License license() {
		return new License();
	}

	/**
	 * Creates an {@link License} element with the specified {@link License#setName(String) name} attribute.
	 *
	 * @param name The {@link License#setName(String) name} attribute.
	 * @return The new element.
	 */
	public static final License license(String name) {
		return license().setName(name);
	}

	/**
	 * Creates an {@link License} element with the specified {@link License#setName(String) name} and {@link License#setUrl(URI) url} attributes.
	 *
	 * @param name The {@link License#setName(String) name} attribute.
	 * @param url The {@link License#setUrl(URI) url} attribute.
	 * @return The new element.
	 */
	public static final License license(String name, URI url) {
		return license().setName(name).setUrl(url);
	}

	/**
	 * Creates an empty {@link OpenApi} element.
	 *
	 * @return The new element.
	 */
	public static final OpenApi openApi() {
		return new OpenApi();
	}

	/**
	 * Creates an {@link OpenApi} element with the specified {@link OpenApi#setInfo(Info) info} attribute.
	 *
	 * @param info The {@link OpenApi#setInfo(Info) info} attribute.
	 * @return The new element.
	 */
	public static final OpenApi openApi(Info info) {
		return openApi().setInfo(info);
	}

	/**
	 * Creates an empty {@link SchemaInfo} element.
	 *
	 * @return The new element.
	 */
	public static final SchemaInfo schemaInfo() {
		return new SchemaInfo();
	}

	/**
	 * Creates an {@link SchemaInfo} element with the specified {@link SchemaInfo#setType(String) type} attribute.
	 *
	 * @param type The {@link SchemaInfo#setType(String) type} attribute.
	 * @return The new element.
	 */
	public static final SchemaInfo schemaInfo(String type) {
		return schemaInfo().setType(type);
	}

	/**
	 * Creates an empty {@link SecuritySchemeInfo} element.
	 *
	 * @return The new element.
	 */
	public static final SecuritySchemeInfo securitySchemeInfo() {
		return new SecuritySchemeInfo();
	}

	/**
	 * Creates an {@link SecuritySchemeInfo} element with the specified {@link SecuritySchemeInfo#setType(String) type} attribute.
	 *
	 * @param type The {@link SecuritySchemeInfo#setType(String) type} attribute.
	 * @return The new element.
	 */
	public static final SecuritySchemeInfo securitySchemeInfo(String type) {
		return securitySchemeInfo().setType(type);
	}

	/**
	 * Creates an empty {@link Server} element.
	 *
	 * @return The new element.
	 */
	public static final Server server() {
		return new Server();
	}

	/**
	 * Creates an {@link Server} element with the specified {@link Server#setUrl(URI) url} attribute.
	 *
	 * @param url The {@link Server#setUrl(URI) url} attribute.
	 * @return The new element.
	 */
	public static final Server server(URI url) {
		return server().setUrl(url);
	}

	/**
	 * Creates an empty {@link Tag} element.
	 *
	 * @return The new element.
	 */
	public static final Tag tag() {
		return new Tag();
	}

	/**
	 * Creates an {@link Tag} element with the specified {@link Tag#setName(String) name} attribute.
	 *
	 * @param name The {@link Tag#setName(String) name} attribute.
	 * @return The new element.
	 */
	public static final Tag tag(String name) {
		return tag().setName(name);
	}

	/**
	 * Creates an empty {@link Xml} element.
	 *
	 * @return The new element.
	 */
	public static final Xml xml() {
		return new Xml();
	}

	/**
	 * Creates an empty {@link Operation} element.
	 *
	 * @return The new element.
	 */
	public static final Operation operation() {
		return new Operation();
	}

	/**
	 * Creates an empty {@link Parameter} element.
	 *
	 * @return The new element.
	 */
	public static final Parameter parameter() {
		return new Parameter();
	}

	/**
	 * Creates a {@link Parameter} element with the specified {@link Parameter#setIn(String) in} and {@link Parameter#setName(String) name} attributes.
	 *
	 * @param in The {@link Parameter#setIn(String) in} attribute.
	 * @param name The {@link Parameter#setName(String) name} attribute.
	 * @return The new element.
	 */
	public static final Parameter parameter(String in, String name) {
		return parameter().setIn(in).setName(name);
	}

	/**
	 * Creates an empty {@link PathItem} element.
	 *
	 * @return The new element.
	 */
	public static final PathItem pathItem() {
		return new PathItem();
	}

	/**
	 * Creates an empty {@link Response} element.
	 *
	 * @return The new element.
	 */
	public static final Response response() {
		return new Response();
	}

	/**
	 * Creates a {@link Response} element with the specified {@link Response#setDescription(String) description} attribute.
	 *
	 * @param description The {@link Response#setDescription(String) description} attribute.
	 * @return The new element.
	 */
	public static final Response response(String description) {
		return response().setDescription(description);
	}

	/**
	 * Creates an empty {@link Components} element.
	 *
	 * @return The new element.
	 */
	public static final Components components() {
		return new Components();
	}

	/**
	 * Creates an empty {@link SecurityRequirement} element.
	 *
	 * @return The new element.
	 */
	public static final SecurityRequirement securityRequirement() {
		return new SecurityRequirement();
	}

	/**
	 * Creates an empty {@link RequestBodyInfo} element.
	 *
	 * @return The new element.
	 */
	public static final RequestBodyInfo requestBodyInfo() {
		return new RequestBodyInfo();
	}

	/**
	 * Creates an empty {@link Example} element.
	 *
	 * @return The new element.
	 */
	public static final Example example() {
		return new Example();
	}

	/**
	 * Creates an empty {@link Link} element.
	 *
	 * @return The new element.
	 */
	public static final Link link() {
		return new Link();
	}

	/**
	 * Creates an empty {@link Callback} element.
	 *
	 * @return The new element.
	 */
	public static final Callback callback() {
		return new Callback();
	}

	/**
	 * Creates an empty {@link Discriminator} element.
	 *
	 * @return The new element.
	 */
	public static final Discriminator discriminator() {
		return new Discriminator();
	}

	/**
	 * Creates a {@link Discriminator} element with the specified {@link Discriminator#setPropertyName(String) propertyName} attribute.
	 *
	 * @param propertyName The {@link Discriminator#setPropertyName(String) propertyName} attribute.
	 * @return The new element.
	 */
	public static final Discriminator discriminator(String propertyName) {
		return discriminator().setPropertyName(propertyName);
	}

	/**
	 * Creates an empty {@link Encoding} element.
	 *
	 * @return The new element.
	 */
	public static final Encoding encoding() {
		return new Encoding();
	}

	/**
	 * Creates an {@link Encoding} element with the specified {@link Encoding#setContentType(String) contentType} attribute.
	 *
	 * @param contentType The {@link Encoding#setContentType(String) contentType} attribute.
	 * @return The new element.
	 */
	public static final Encoding encoding(String contentType) {
		return encoding().setContentType(contentType);
	}

	/**
	 * Creates an empty {@link MediaType} element.
	 *
	 * @return The new element.
	 */
	public static final MediaType mediaType() {
		return new MediaType();
	}

	/**
	 * Creates an empty {@link OAuthFlow} element.
	 *
	 * @return The new element.
	 */
	public static final OAuthFlow oAuthFlow() {
		return new OAuthFlow();
	}

	/**
	 * Creates an empty {@link OAuthFlows} element.
	 *
	 * @return The new element.
	 */
	public static final OAuthFlows oAuthFlows() {
		return new OAuthFlows();
	}

	/**
	 * Creates an empty {@link ServerVariable} element.
	 *
	 * @return The new element.
	 */
	public static final ServerVariable serverVariable() {
		return new ServerVariable();
	}

	/**
	 * Creates a {@link ServerVariable} element with the specified {@link ServerVariable#setDefault(String) default} attribute.
	 *
	 * @param defaultValue The {@link ServerVariable#setDefault(String) default} attribute.
	 * @return The new element.
	 */
	public static final ServerVariable serverVariable(String defaultValue) {
		return serverVariable().setDefault(defaultValue);
	}
}
