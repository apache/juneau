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
package org.apache.juneau.bean.openapi3;

import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.internal.*;

/**
 * Allows configuration of the supported OAuth Flows.
 *
 * <p>
 * The OAuthFlows Object allows configuration of the supported OAuth Flows. This object contains the configuration
 * for different OAuth 2.0 flows that can be used to secure the API. Each flow type has its own specific configuration
 * requirements and use cases.
 *
 * <h5 class='section'>OpenAPI Specification:</h5>
 * <p>
 * The OAuthFlows Object is composed of the following fields:
 * <ul class='spaced-list'>
 * 	<li><c>implicit</c> ({@link OAuthFlow}) - Configuration for the OAuth Implicit flow
 * 	<li><c>password</c> ({@link OAuthFlow}) - Configuration for the OAuth Resource Owner Password flow
 * 	<li><c>clientCredentials</c> ({@link OAuthFlow}) - Configuration for the OAuth Client Credentials flow
 * 	<li><c>authorizationCode</c> ({@link OAuthFlow}) - Configuration for the OAuth Authorization Code flow
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	OAuthFlows <jv>x</jv> = <jsm>oauthFlows</jsm>()
 * 		.setAuthorizationCode(<jsm>oauthFlow</jsm>()
 * 			.setAuthorizationUrl(<js>"https://example.com/oauth/authorize"</js>)
 * 			.setTokenUrl(<js>"https://example.com/oauth/token"</js>));
 *
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String <jv>json</jv> = Json.<jsm>from</jsm>(<jv>x</jv>);
 *
 * 	<jc>// Or just use toString() which does the same as above.</jc>
 * 	<jv>json</jv> = <jv>x</jv>.toString();
 * </p>
 * <p class='bcode'>
 * 	<jc>// Output</jc>
 * 	{
 * 		<js>"authorizationCode"</js>: {
 * 			<js>"authorizationUrl"</js>: <js>"https://example.com/oauth/authorize"</js>,
 * 			<js>"tokenUrl"</js>: <js>"https://example.com/oauth/token"</js>
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://spec.openapis.org/oas/v3.0.0#oauth-flows-object">OpenAPI Specification &gt; OAuth Flows Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/authentication/oauth2/">OpenAPI OAuth2 Authentication</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanOpenApi3">juneau-bean-openapi-v3</a>
 * </ul>
 */
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

	@Override /* Overridden from SwaggerElement */
	protected OAuthFlows strict() {
		super.strict();
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	public OAuthFlows strict(Object value) {
		super.strict(value);
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

	@Override /* Overridden from SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "implicit" -> toType(getImplicit(), type);
			case "password" -> toType(getPassword(), type);
			case "clientCredentials" -> toType(getClientCredentials(), type);
			case "authorizationCode" -> toType(getAuthorizationCode(), type);
			default -> super.get(property, type);
		};
	}

	@Override /* Overridden from SwaggerElement */
	public OAuthFlows set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "authorizationCode" -> setAuthorizationCode(toType(value, OAuthFlow.class));
			case "clientCredentials" -> setClientCredentials(toType(value, OAuthFlow.class));
			case "implicit" -> setImplicit(toType(value, OAuthFlow.class));
			case "password" -> setPassword(toType(value, OAuthFlow.class));
			default -> {
				super.set(property, value);
				yield this;
			}
		};
	}

	@Override /* Overridden from SwaggerElement */
	public Set<String> keySet() {
		var s = setBuilder(String.class)
			.addIf(authorizationCode != null, "authorizationCode")
			.addIf(clientCredentials != null, "clientCredentials")
			.addIf(implicit != null, "implicit")
			.addIf(password != null, "password")
			.build();
		return new MultiSet<>(s, super.keySet());
	}
}