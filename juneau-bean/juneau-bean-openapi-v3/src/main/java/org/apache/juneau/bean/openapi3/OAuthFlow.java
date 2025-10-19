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

import org.apache.juneau.common.utils.*;
import org.apache.juneau.internal.*;

/**
 * Configuration details for a supported OAuth Flow.
 *
 * <p>
 * The OAuthFlow Object provides configuration details for a supported OAuth Flow. This object contains the URLs and
 * scopes needed to configure a specific OAuth 2.0 flow. Different flows require different combinations of URLs and
 * have different security characteristics.
 *
 * <h5 class='section'>OpenAPI Specification:</h5>
 * <p>
 * The OAuthFlow Object is composed of the following fields:
 * <ul class='spaced-list'>
 * 	<li><c>authorizationUrl</c> (string) - The authorization URL to be used for this flow. This MUST be in the form of a URL
 * 	<li><c>tokenUrl</c> (string) - The token URL to be used for this flow. This MUST be in the form of a URL
 * 	<li><c>refreshUrl</c> (string) - The URL to be used for obtaining refresh tokens. This MUST be in the form of a URL
 * 	<li><c>scopes</c> (map of strings) - The available scopes for the OAuth2 security scheme. A map between the scope name and a short description for it
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	OAuthFlow <jv>x</jv> = <jsm>oauthFlow</jsm>()
 * 		.setAuthorizationUrl(<js>"https://example.com/oauth/authorize"</js>)
 * 		.setTokenUrl(<js>"https://example.com/oauth/token"</js>)
 * 		.setScopes(<jsm>map</jsm>(<js>"read"</js>, <js>"Read access"</js>, <js>"write"</js>, <js>"Write access"</js>));
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
 * 		<js>"authorizationUrl"</js>: <js>"https://example.com/oauth/authorize"</js>,
 * 		<js>"tokenUrl"</js>: <js>"https://example.com/oauth/token"</js>,
 * 		<js>"scopes"</js>: {
 * 			<js>"read"</js>: <js>"Read access"</js>,
 * 			<js>"write"</js>: <js>"Write access"</js>
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://spec.openapis.org/oas/v3.0.0#oauth-flow-object">OpenAPI Specification &gt; OAuth Flow Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/authentication/oauth2/">OpenAPI OAuth2 Authentication</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanOpenApi3">juneau-bean-openapi-v3</a>
 * </ul>
 */
public class OAuthFlow extends OpenApiElement {

	private String authorizationUrl;
	private String tokenUrl;
	private String refreshUrl;
	private Map<String,String> scopes;

	/**
	 * Default constructor.
	 */
	public OAuthFlow() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public OAuthFlow(OAuthFlow copyFrom) {
		super(copyFrom);

		this.authorizationUrl = copyFrom.authorizationUrl;
		this.tokenUrl = copyFrom.tokenUrl;
		this.refreshUrl = copyFrom.refreshUrl;
		this.scopes = copyOf(copyFrom.scopes);
	}

	/**
	 * Adds a single value to the <property>examples</property> property.
	 *
	 * @param name The mime-type string.  Must not be <jk>null</jk>.
	 * @param description The example.  Must not be <jk>null</jk>.
	 * @return This object
	 */
	public OAuthFlow addScope(String name, String description) {
		assertArgNotNull("name", name);
		assertArgNotNull("description", description);
		scopes = mapBuilder(scopes).sparse().add(name, description).build();
		return this;
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public OAuthFlow copy() {
		return new OAuthFlow(this);
	}

	@Override /* Overridden from OpenApiElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "refreshUrl" -> toType(getRefreshUrl(), type);
			case "tokenUrl" -> toType(getTokenUrl(), type);
			case "authorizationUrl" -> toType(getAuthorizationUrl(), type);
			case "scopes" -> toType(getScopes(), type);
			default -> super.get(property, type);
		};
	}

	/**
	 * Bean property getter:  <property>operationRef</property>.
	 *
	 * <p>
	 * The identifying name of the contact person/organization.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getAuthorizationUrl() { return authorizationUrl; }

	/**
	 * Bean property getter:  <property>externalValue</property>.
	 *
	 * <p>
	 * The email address of the contact person/organization.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getRefreshUrl() { return refreshUrl; }

	/**
	 * Bean property getter:  <property>examples</property>.
	 *
	 * <p>
	 * An example of the response message.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,String> getScopes() { return scopes; }

	/**
	 * Bean property getter:  <property>description</property>.
	 *
	 * <p>
	 * The URL pointing to the contact information.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getTokenUrl() { return tokenUrl; }

	@Override /* Overridden from OpenApiElement */
	public Set<String> keySet() {
		var s = setBuilder(String.class)
			.addIf(authorizationUrl != null, "authorizationUrl")
			.addIf(refreshUrl != null, "refreshUrl")
			.addIf(scopes != null, "scopes")
			.addIf(tokenUrl != null, "tokenUrl")
			.build();
		return new MultiSet<>(s, super.keySet());
	}

	@Override /* Overridden from OpenApiElement */
	public OAuthFlow set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "authorizationUrl" -> setAuthorizationUrl(Utils.s(value));
			case "refreshUrl" -> setRefreshUrl(Utils.s(value));
			case "scopes" -> setScopes(mapBuilder(String.class, String.class).sparse().addAny(value).build());
			case "tokenUrl" -> setTokenUrl(Utils.s(value));
			default -> {
				super.set(property, value);
				yield this;
			}
		};
	}

	/**
	 * Bean property setter:  <property>operationRef</property>.
	 *
	 * <p>
	 * The identifying name of the contact person/organization.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public OAuthFlow setAuthorizationUrl(String value) {
		authorizationUrl = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>externalValue</property>.
	 *
	 * <p>
	 * The email address of the contact person/organization.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>MUST be in the format of an email address.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public OAuthFlow setRefreshUrl(String value) {
		refreshUrl = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>examples</property>.
	 *
	 * <p>
	 * An example of the response message.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Keys must be MIME-type strings.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public OAuthFlow setScopes(Map<String,String> value) {
		scopes = copyOf(value);
		return this;
	}

	/**
	 * Bean property setter:  <property>description</property>.
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public OAuthFlow setTokenUrl(String value) {
		tokenUrl = value;
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	public OAuthFlow strict() {
		super.strict();
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	public OAuthFlow strict(Object value) {
		super.strict(value);
		return this;
	}
}