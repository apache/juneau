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

import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Lists the required security schemes for this operation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.OpenApi">Overview &gt; juneau-rest-server &gt; OpenAPI</a>
 * </ul>
 */
@Bean(properties="*")
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
	 * @param schemeName The security scheme name.
	 * @param scopes The required scopes.
	 * @return This object.
	 */
	public SecurityRequirement addRequirement(String schemeName, String... scopes) {
		if (requirements == null)
			requirements = new LinkedHashMap<>();
		requirements.put(schemeName, Arrays.asList(scopes));
		return this;
	}
}
