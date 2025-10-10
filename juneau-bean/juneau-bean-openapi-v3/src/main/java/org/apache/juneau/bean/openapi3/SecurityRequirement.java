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
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.internal.*;

/**
 * Lists the required security schemes for this operation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanOpenApi3">juneau-bean-openapi-v3</a>
 * </ul>
 */
@FluentSetters
public class SecurityRequirement extends OpenApiElement {

	private Map<String,List<String>> requirements;

	/**
	 * Default constructor.
	 */
	public SecurityRequirement() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public SecurityRequirement(SecurityRequirement copyFrom) {
		super(copyFrom);
		this.requirements = copyOf(copyFrom.requirements);
	}

	/**
	 * Makes a copy of this object.
	 *
	 * @return A new copy of this object.
	 */
	public SecurityRequirement copy() {
		return new SecurityRequirement(this);
	}

	/**
	 * Returns the security requirements map.
	 *
	 * @return The security requirements map.
	 */
	public Map<String,List<String>> getRequirements() {
		return requirements;
	}

	/**
	 * Sets the security requirements map.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public SecurityRequirement setRequirements(Map<String,List<String>> value) {
		this.requirements = value;
		return this;
	}

	/**
	 * Adds a security requirement.
	 *
	 * @param schemeName The security scheme name.  Must not be <jk>null</jk>.
	 * @param scopes The required scopes.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public SecurityRequirement addRequirement(String schemeName, String... scopes) {
		assertArgNotNull("schemeName", schemeName);
		assertVarargsNotNull("scopes", scopes);
		if (requirements == null)
			requirements = new LinkedHashMap<>();
		requirements.put(schemeName, Arrays.asList(scopes));
		return this;
	}

	/**
	 * Adds a security requirement for a scheme that doesn't use scopes.
	 *
	 * <p>
	 * This is a convenience method for adding security schemes that don't use scopes, such as API keys, 
	 * HTTP Basic authentication, or HTTP Bearer tokens. According to the OpenAPI specification, security 
	 * schemes that don't use scopes should have an empty array as the value.
	 *
	 * <p>
	 * This method is equivalent to calling <c>addRequirement(schemeName)</c> with no scopes.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Add API key authentication requirement</jc>
	 * 	SecurityRequirement <jv>requirement</jv> = <jk>new</jk> SecurityRequirement()
	 * 		.setApiKeyAuth(<js>"api_key"</js>);
	 * 	<jc>// Results in: { "api_key": [] }</jc>
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="https://spec.openapis.org/oas/v3.0.0#security-requirement-object">OpenAPI Specification &gt; Security Requirement Object</a>
	 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/authentication/">OpenAPI Authentication</a>
	 * </ul>
	 *
	 * @param schemeName The security scheme name.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public SecurityRequirement setApiKeyAuth(String schemeName) {
		return addRequirement(schemeName);
	}

	@Override /* OpenApiElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "requirements" -> toType(getRequirements(), type);
			default -> super.get(property, type);
		};
	}

	@Override /* OpenApiElement */
	public SecurityRequirement set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "requirements" -> setRequirements((Map<String,List<String>>)value);
			default -> {
				super.set(property, value);
				yield this;
			}
		};
	}

	@Override /* OpenApiElement */
	public Set<String> keySet() {
		var s = setBuilder(String.class)
			.addIf(requirements != null, "requirements")
			.build();
		return new MultiSet<>(s, super.keySet());
	}

	// <FluentSetters>

	@Override /* GENERATED - do not modify */
	public SecurityRequirement strict() {
		super.strict();
		return this;
	}

	@Override /* GENERATED - do not modify */
	public SecurityRequirement strict(Object value) {
		super.strict(value);
		return this;
	}

	// </FluentSetters>
}
