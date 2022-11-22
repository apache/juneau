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

import java.net.*;

import org.apache.juneau.*;
import org.apache.juneau.common.internal.*;

/**
 * Various useful static methods for creating Swagger elements.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Swagger">Overview &gt; juneau-rest-server &gt; Swagger</a>
 * </ul>
 */
public class SwaggerBuilder {

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
	 * Creates an {@link HeaderInfo} element with the specified {@link HeaderInfo#setType(String) type} attribute.
	 *
	 * @param type The {@link HeaderInfo#setType(String) type} attribute.
	 * @return The new element.
	 */
	public static final HeaderInfo headerInfo(String type) {
		return headerInfo().setType(type);
	}

	/**
	 * Creates an {@link HeaderInfo} element with the specified {@link HeaderInfo#setType(String) type} attribute.
	 *
	 * <p>
	 * Throws a runtime exception if the type is not valid.
	 *
	 * @param type
	 * 	The {@link HeaderInfo#setType(String) type} attribute.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"string"</js>
	 * 		<li><js>"number"</js>
	 * 		<li><js>"integer"</js>
	 * 		<li><js>"boolean"</js>
	 * 		<li><js>"array"</js>
	 * 	</ul>
	 * @return The new element.
	 */
	public static final HeaderInfo headerInfoStrict(String type) {
		return headerInfo().strict().setType(type);
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
	 * Creates an {@link Items} element with the specified {@link Items#setType(String) type} attribute.
	 *
	 * <p>
	 * Throws a runtime exception if the type is not valid.
	 *
	 * @param type
	 * 	The {@link Items#setType(String) type} attribute.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"string"</js>
	 * 		<li><js>"number"</js>
	 * 		<li><js>"integer"</js>
	 * 		<li><js>"boolean"</js>
	 * 		<li><js>"array"</js>
	 * 	</ul>
	 * @return The new element.
	 */
	public static final Items itemsStrict(String type) {
		return items().strict().setType(type);
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
	 * Creates an empty {@link Operation} element.
	 *
	 * @return The new element.
	 */
	public static final Operation operation() {
		return new Operation();
	}

	/**
	 * Creates an empty {@link ParameterInfo} element.
	 *
	 * @return The new element.
	 */
	public static final ParameterInfo parameterInfo() {
		return new ParameterInfo();
	}

	/**
	 * Creates an {@link ParameterInfo} element with the specified {@link ParameterInfo#setIn(String) in} and
	 * {@link ParameterInfo#setName(String) name} attributes.
	 *
	 * @param in The {@link ParameterInfo#setIn(String) in} attribute.
	 * @param name The {@link ParameterInfo#setName(String) name} attribute.
	 * @return The new element.
	 */
	public static final ParameterInfo parameterInfo(String in, String name) {
		return parameterInfo().setIn(in).setName(name);
	}

	/**
	 * Creates an {@link ParameterInfo} element with the specified {@link ParameterInfo#setIn(String) in} and
	 * {@link ParameterInfo#setName(String) name} attributes.
	 *
	 * <p>
	 * Throws a runtime exception if the type is not valid.
	 *
	 * @param in
	 * 	The {@link ParameterInfo#setIn(String) in} attribute.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"query"</js>
	 * 		<li><js>"header"</js>
	 * 		<li><js>"path"</js>
	 * 		<li><js>"formData"</js>
	 * 		<li><js>"body"</js>
	 * 	</ul>
	 * @param name The {@link ParameterInfo#setName(String) name} attribute.
	 * @return The new element.
	 */
	public static final ParameterInfo parameterInfoStrict(String in, String name) {
		return parameterInfo().strict().setIn(in).setName(name);
	}

	/**
	 * Creates an empty {@link ResponseInfo} element.
	 *
	 * @return The new element.
	 */
	public static final ResponseInfo responseInfo() {
		return new ResponseInfo();
	}

	/**
	 * Creates an {@link ResponseInfo} element with the specified {@link ResponseInfo#setDescription(String) description} attribute.
	 *
	 * @param description The {@link ResponseInfo#setDescription(String) description} attribute.
	 * @return The new element.
	 */
	public static final ResponseInfo responseInfo(String description) {
		return responseInfo().setDescription(description);
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
	 * Creates an empty {@link SecurityScheme} element.
	 *
	 * @return The new element.
	 */
	public static final SecurityScheme securityScheme() {
		return new SecurityScheme();
	}

	/**
	 * Creates an {@link SecurityScheme} element with the specified {@link SecurityScheme#setType(String) type} attribute.
	 *
	 * @param type The {@link SecurityScheme#setType(String) type} attribute.
	 * @return The new element.
	 */
	public static final SecurityScheme securityScheme(String type) {
		return securityScheme().setType(type);
	}

	/**
	 * Creates an {@link SecurityScheme} element with the specified {@link SecurityScheme#setType(String) type} attribute.
	 *
	 * <p>
	 * Throws a runtime exception if the type is not valid.
	 *
	 * @param type
	 * 	The {@link SecurityScheme#setType(String) type} attribute.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"basic"</js>
	 * 		<li><js>"apiKey"</js>
	 * 		<li><js>"oauth2"</js>
	 * 	</ul>
	 * @return The new element.
	 */
	public static final SecurityScheme securitySchemeStrict(String type) {
		return securityScheme().strict().setType(type);
	}

	/**
	 * Creates an empty {@link Swagger} element.
	 *
	 * @return The new element.
	 */
	public static final Swagger swagger() {
		return new Swagger();
	}

	/**
	 * Creates an {@link Swagger} element with the specified {@link Swagger#setInfo(Info) info} attribute.
	 *
	 * @param info The {@link Swagger#setInfo(Info) info} attribute.
	 * @return The new element.
	 */
	public static final Swagger swagger(Info info) {
		return swagger().setInfo(info);
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
}
