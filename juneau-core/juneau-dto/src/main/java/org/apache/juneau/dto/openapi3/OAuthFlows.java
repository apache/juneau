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
package org.apache.juneau.dto.openapi3;

import static org.apache.juneau.internal.ConverterUtils.*;

import org.apache.juneau.annotation.Bean;
import org.apache.juneau.internal.*;

import java.util.Set;

import static org.apache.juneau.internal.CollectionUtils.*;

/**
 * Describes a single operation parameter.
 *
 * <p>
 * A unique parameter is defined by a combination of a name and location.
 *
 * <p>
 * There are five possible parameter types.
 * <ul class='spaced-list'>
 * 	<li><js>"path"</js> - Used together with Path Templating, where the parameter value is actually part of the
 * 		operation's URL.
 * 		This does not include the host or base path of the API.
 * 		For example, in <code>/items/{itemId}</code>, the path parameter is <code>itemId</code>.
 * 	<li><js>"query"</js> - Parameters that are appended to the URL.
 * 		For example, in <code>/items?id=###</code>, the query parameter is <code>id</code>.
 * 	<li><js>"header"</js> - Custom headers that are expected as part of the request.
 * 	<li><js>"body"</js> - The payload that's appended to the HTTP request.
 * 		Since there can only be one payload, there can only be one body parameter.
 * 		The name of the body parameter has no effect on the parameter itself and is used for documentation purposes
 * 		only.
 * 		Since Form parameters are also in the payload, body and form parameters cannot exist together for the same
 * 		operation.
 * 	<li><js>"formData"</js> - Used to describe the payload of an HTTP request when either
 * 		<code>application/x-www-form-urlencoded</code>, <code>multipart/form-data</code> or both are used as the
 * 		content type of the request (in Swagger's definition, the consumes property of an operation).
 * 		This is the only parameter type that can be used to send files, thus supporting the file type.
 * 		Since form parameters are sent in the payload, they cannot be declared together with a body parameter for the
 * 		same operation.
 * 		Form parameters have a different format based on the content-type used (for further details, consult
 * 		<code>http://www.w3.org/TR/html401/interact/forms.html#h-17.13.4</code>):
 * 		<ul>
 * 			<li><js>"application/x-www-form-urlencoded"</js> - Similar to the format of Query parameters but as a
 * 				payload.
 * 				For example, <code>foo=1&amp;bar=swagger</code> - both <code>foo</code> and <code>bar</code> are form
 * 				parameters.
 * 				This is normally used for simple parameters that are being transferred.
 * 			<li><js>"multipart/form-data"</js> - each parameter takes a section in the payload with an internal header.
 * 				For example, for the header <code>Content-Disposition: form-data; name="submit-name"</code> the name of
 * 				the parameter is <code>submit-name</code>.
 * 				This type of form parameters is more commonly used for file transfers.
 * 		</ul>
 * 	</li>
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	ParameterInfo x = <jsm>parameterInfo</jsm>(<js>"query"</js>, <js>"foo"</js>);
 *
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String json = JsonSerializer.<jsf>DEFAULT</jsf>.toString(x);
 *
 * 	<jc>// Or just use toString() which does the same as above.</jc>
 * 	String json = x.toString();
 * </p>
 * <p class='bcode'>
 * 	<jc>// Output</jc>
 * 	{
 * 		<js>"in"</js>: <js>"query"</js>,
 * 		<js>"name"</js>: <js>"foo"</js>
 * 	}
 * </p>
 */
@Bean(properties="implicit,password,clientCredentials,authorizationCode,*")
@FluentSetters
public class OAuthFlows extends OpenApiElement {

	private OAuthFlow implicit,
			password,
			clientCredentials,
			authorizationCode;

	/**
	 * Default constructor.
	 */
	public OAuthFlows() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public OAuthFlows(OAuthFlows copyFrom) {
		super(copyFrom);

		this.implicit = copyFrom.implicit;
		this.password = copyFrom.password;
		this.clientCredentials = copyFrom.clientCredentials;
		this.authorizationCode = copyFrom.authorizationCode;
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public OAuthFlows copy() {
		return new OAuthFlows(this);
	}

	@Override /* SwaggerElement */
	protected OAuthFlows strict() {
		super.strict();
		return this;
	}

	/**
	 * Bean property getter:  <property>implicit</property>.
	 *
	 * <p>
	 * Describes the type of items in the array.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public OAuthFlow getImplicit() {
		return implicit;
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
	public OAuthFlows setImplicit(OAuthFlow value) {
		implicit = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>password</property>.
	 *
	 * <p>
	 * Describes the type of items in the array.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public OAuthFlow getPassword() {
		return password;
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
	public OAuthFlows setPassword(OAuthFlow value) {
		password = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>clientCredentials</property>.
	 *
	 * <p>
	 * Describes the type of items in the array.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public OAuthFlow getClientCredentials() {
		return clientCredentials;
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
	public OAuthFlows setClientCredentials(OAuthFlow value) {
		clientCredentials = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>authorizationCode</property>.
	 *
	 * <p>
	 * Describes the type of items in the array.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public OAuthFlow getAuthorizationCode() {
		return authorizationCode;
	}

	/**
	 * Bean property setter:  <property>authorizationCode</property>.
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
	public OAuthFlows setAuthorizationCode(OAuthFlow value) {
		authorizationCode = value;
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>

	@Override /* SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "implicit": return toType(getImplicit(), type);
			case "password": return toType(getPassword(), type);
			case "clientCredentials": return toType(getClientCredentials(), type);
			case "authorizationCode": return toType(getAuthorizationCode(), type);
			default: return super.get(property, type);
		}
	}

	@Override /* SwaggerElement */
	public OAuthFlows set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "implicit": return setImplicit(toType(value, OAuthFlow.class));
			case "password": return setPassword(toType(value, OAuthFlow.class));
			case "clientCredentials": return setClientCredentials(toType(value, OAuthFlow.class));
			case "authorizationCode": return setAuthorizationCode(toType(value, OAuthFlow.class));
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* SwaggerElement */
	public Set<String> keySet() {
		Set<String> s = setBuilder(String.class)
			.addIf(implicit != null, "implicit")
			.addIf(password != null, "password")
			.addIf(clientCredentials != null, "clientCredentials")
			.addIf(authorizationCode != null, "authorizationCode")
			.build();
		return new MultiSet<>(s, super.keySet());
	}

}
