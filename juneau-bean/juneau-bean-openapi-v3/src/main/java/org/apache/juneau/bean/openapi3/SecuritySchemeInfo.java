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

import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.internal.ArrayUtils.contains;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.internal.*;

/**
 * Defines a security scheme that can be used by the operations.
 *
 * <p>
 * The Security Scheme Object defines a security scheme that can be used by the operations. Supported schemes are 
 * HTTP authentication, an API key (either as a header or as a query parameter), OAuth2's common flows (implicit, 
 * password, client credentials and authorization code) as defined in RFC6749, and OpenID Connect Discovery.
 *
 * <h5 class='section'>OpenAPI Specification:</h5>
 * <p>
 * The Security Scheme Object is composed of the following fields:
 * <ul class='spaced-list'>
 * 	<li><c>type</c> (string, REQUIRED) - The type of the security scheme. Values: <js>"apiKey"</js>, <js>"http"</js>, <js>"oauth2"</js>, <js>"openIdConnect"</js>
 * 	<li><c>description</c> (string) - A short description for security scheme (CommonMark syntax may be used)
 * 	<li><c>name</c> (string) - The name of the header, query or cookie parameter to be used (for <js>"apiKey"</js> type)
 * 	<li><c>in</c> (string) - The location of the API key (for <js>"apiKey"</js> type). Values: <js>"query"</js>, <js>"header"</js>, <js>"cookie"</js>
 * 	<li><c>scheme</c> (string) - The name of the HTTP Authorization scheme to be used in the Authorization header (for <js>"http"</js> type)
 * 	<li><c>bearerFormat</c> (string) - A hint to the client to identify how the bearer token is formatted (for <js>"http"</js> type with <js>"bearer"</js> scheme)
 * 	<li><c>flows</c> ({@link OAuthFlows}) - An object containing configuration information for the flow types supported (for <js>"oauth2"</js> type)
 * 	<li><c>openIdConnectUrl</c> (string) - OpenId Connect URL to discover OAuth2 configuration values (for <js>"openIdConnect"</js> type)
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create an API key security scheme</jc>
 * 	SecuritySchemeInfo <jv>scheme</jv> = <jk>new</jk> SecuritySchemeInfo()
 * 		.setType(<js>"apiKey"</js>)
 * 		.setDescription(<js>"API key authentication"</js>)
 * 		.setName(<js>"X-API-Key"</js>)
 * 		.setIn(<js>"header"</js>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Create an OAuth2 security scheme</jc>
 * 	SecuritySchemeInfo <jv>oauthScheme</jv> = <jk>new</jk> SecuritySchemeInfo()
 * 		.setType(<js>"oauth2"</js>)
 * 		.setDescription(<js>"OAuth2 authentication"</js>)
 * 		.setFlows(
 * 			<jk>new</jk> OAuthFlows()
 * 				.setAuthorizationCode(
 * 					<jk>new</jk> OAuthFlow()
 * 						.setAuthorizationUrl(<js>"https://example.com/oauth/authorize"</js>)
 * 						.setTokenUrl(<js>"https://example.com/oauth/token"</js>)
 * 						.setScopes(
 * 							JsonMap.<jsm>of</jsm>(
 * 								<js>"read"</js>, <js>"Read access to resources"</js>,
 * 								<js>"write"</js>, <js>"Write access to resources"</js>
 * 							)
 * 						)
 * 				)
 * 		);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://spec.openapis.org/oas/v3.0.0#security-scheme-object">OpenAPI Specification &gt; Security Scheme Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/authentication/">OpenAPI Authentication</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanOpenApi3">juneau-bean-openapi-v3</a>
 * </ul>
 */
public class SecuritySchemeInfo extends OpenApiElement {

	private static final String[] VALID_IN = {"query", "header", "cookie"};
	private static final String[] VALID_TYPES = {"apiKey", "http", "oauth2", "openIdConnect"};

	private String
		type,
		description,
		name,
		in,
		scheme,
		bearerFormat,
		openIdConnectUrl;

	private OAuthFlow flows;

	/**
	 * Default constructor.
	 */
	public SecuritySchemeInfo() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public SecuritySchemeInfo(SecuritySchemeInfo copyFrom) {
		super(copyFrom);

		this.name = copyFrom.name;
		this.in = copyFrom.in;
		this.description = copyFrom.description;
		this.type = copyFrom.type;
		this.scheme = copyFrom.scheme;
		this.bearerFormat = copyFrom.bearerFormat;
		this.openIdConnectUrl = copyFrom.openIdConnectUrl;
		this.flows = copyFrom.flows;
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public SecuritySchemeInfo copy() {
		return new SecuritySchemeInfo(this);
	}

	@Override /* SwaggerElement */
	protected SecuritySchemeInfo strict() {
		super.strict();
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	public SecuritySchemeInfo strict(Object value) {
		super.strict(value);
		return this;
	}

	/**
	 * Bean property getter:  <property>name</property>.
	 *
	 * <p>
	 * The name of the parameter.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Parameter names are case sensitive.
	 * 	<li>
	 * 		If <code>in</code> is <js>"path"</js>, the <code>name</code> field MUST correspond to the associated path segment
	 * 		from the <code>path</code> field in the paths object.
	 * 	<li>
	 * 		For all other cases, the name corresponds to the parameter name used based on the <code>in</code> property.
	 * </ul>
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Bean property setter:  <property>name</property>.
	 *
	 * <p>
	 * The name of the parameter.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Parameter names are case sensitive.
	 * 	<li>
	 * 		If <code>in</code> is <js>"path"</js>, the <code>name</code> field MUST correspond to the associated path segment
	 * 		from the <code>path</code> field in the paths object.
	 * 	<li>
	 * 		For all other cases, the name corresponds to the parameter name used based on the <code>in</code> property.
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Property value is required.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public SecuritySchemeInfo setName(String value) {
		name = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>in</property>.
	 *
	 * <p>
	 * The location of the parameter.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getIn() {
		return in;
	}

	/**
	 * Bean property setter:  <property>in</property>.
	 *
	 * <p>
	 * The location of the parameter.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"query"</js>
	 * 		<li><js>"header"</js>
	 * 		<li><js>"path"</js>
	 * 		<li><js>"formData"</js>
	 * 		<li><js>"body"</js>
	 * 	</ul>
	 * 	<br>Property value is required.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public SecuritySchemeInfo setIn(String value) {
		if (isStrict() && ! contains(value, VALID_IN))
			throw new BasicRuntimeException(
				"Invalid value passed in to setIn(String).  Value=''{0}'', valid values={1}",
				value, VALID_IN
			);
		in = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>description</property>.
	 *
	 * <p>
	 * A brief description of the parameter.
	 * <br>This could contain examples of use.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Bean property setter:  <property>description</property>.
	 *
	 * <p>
	 * A brief description of the parameter.
	 * <br>This could contain examples of use.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public SecuritySchemeInfo setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>schema</property>.
	 *
	 * <p>
	 * The schema defining the type used for the body parameter.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getScheme() {
		return scheme;
	}

	/**
	 * Bean property setter:  <property>schema</property>.
	 *
	 * <p>
	 * The schema defining the type used for the body parameter.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Property value is required.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public SecuritySchemeInfo setScheme(String value) {
		scheme = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>type</property>.
	 *
	 * <p>
	 * The type of the parameter.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getType() {
		return type;
	}

	/**
	 * Bean property setter:  <property>type</property>.
	 *
	 * <p>
	 * The type of the parameter.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"string"</js>
	 * 		<li><js>"number"</js>
	 * 		<li><js>"integer"</js>
	 * 		<li><js>"boolean"</js>
	 * 		<li><js>"array"</js>
	 * 		<li><js>"file"</js>
	 * 	</ul>
	 * 	<br>If type is <js>"file"</js>, the <code>consumes</code> MUST be either <js>"multipart/form-data"</js>, <js>"application/x-www-form-urlencoded"</js>
	 * 		or both and the parameter MUST be <code>in</code> <js>"formData"</js>.
	 * 	<br>Property value is required.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public SecuritySchemeInfo setType(String value) {
		if (isStrict() && ! contains(value, VALID_TYPES))
			throw new BasicRuntimeException(
				"Invalid value passed in to setType(String).  Value=''{0}'', valid values={1}",
				value, VALID_TYPES
			);
		type = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>format</property>.
	 *
	 * <p>
	 * The extending format for the previously mentioned type.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getBearerFormat() {
		return bearerFormat;
	}

	/**
	 * Bean property setter:  <property>format</property>.
	 *
	 * <p>
	 * The extending format for the previously mentioned type.
	 *
	 * @param value The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public SecuritySchemeInfo setBearerFormat(String value) {
		bearerFormat = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>items</property>.
	 *
	 * <p>
	 * Describes the type of items in the array.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public OAuthFlow getFlows() {
		return flows;
	}

	/**
	 * Bean property setter:  <property>items</property>.
	 *
	 * <p>
	 * Describes the type of items in the array.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Property value is required if <code>type</code> is <js>"array"</js>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public SecuritySchemeInfo setFlows(OAuthFlow value) {
		flows = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>collectionFormat</property>.
	 *
	 * <p>
	 * Determines the format of the array if type array is used.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getOpenIdConnectUrl() {
		return openIdConnectUrl;
	}

	/**
	 * Bean property setter:  <property>collectionFormat</property>.
	 *
	 * <p>
	 * Determines the format of the array if type array is used.
	 *
	 * @param value The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public SecuritySchemeInfo setOpenIdConnectUrl(String value) {
		openIdConnectUrl = value;
		return this;
	}

	@Override /* SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "name" -> toType(getName(), type);
			case "in" -> toType(getIn(), type);
			case "description" -> toType(getDescription(), type);
			case "scheme" -> toType(getScheme(), type);
			case "flows" -> toType(getFlows(), type);
			case "bearerFormat" -> toType(getBearerFormat(), type);
			case "openIdConnectUrl" -> toType(getOpenIdConnectUrl(), type);
			case "type" -> toType(getType(), type);
			default -> super.get(property, type);
		};
	}

	@Override /* SwaggerElement */
	public SecuritySchemeInfo set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "bearerFormat" -> setBearerFormat(Utils.s(value));
			case "description" -> setDescription(Utils.s(value));
			case "flows" -> setFlows(toType(value, OAuthFlow.class));
			case "in" -> setIn(Utils.s(value));
			case "name" -> setName(Utils.s(value));
			case "openIdConnectUrl" -> setOpenIdConnectUrl(Utils.s(value));
			case "scheme" -> setScheme(Utils.s(value));
			case "type" -> setType(Utils.s(value));
			default -> {
				super.set(property, value);
				yield this;
			}
		};
	}

	@Override /* SwaggerElement */
	public Set<String> keySet() {
		var s = setBuilder(String.class)
			.addIf(bearerFormat != null, "bearerFormat")
			.addIf(description != null, "description")
			.addIf(flows != null, "flows")
			.addIf(in != null, "in")
			.addIf(name != null, "name")
			.addIf(openIdConnectUrl != null, "openIdConnectUrl")
			.addIf(scheme != null, "scheme")
			.addIf(type != null, "type")
			.build();
		return new MultiSet<>(s, super.keySet());
	}
}