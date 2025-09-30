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
 * A map of possible out-of-band callbacks related to the parent operation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.OpenApi">Overview &gt; juneau-rest-server &gt; OpenAPI</a>
 * </ul>
 */
@Bean(properties="*")
@FluentSetters
public class Callback extends OpenApiElement {

	private Map<String,PathItem> callbacks;

	/**
	 * Default constructor.
	 */
	public Callback() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public Callback(Callback copyFrom) {
		super(copyFrom);
		this.callbacks = copyOf(copyFrom.callbacks);
	}

	/**
	 * Returns the callbacks map.
	 *
	 * @return The callbacks map.
	 */
	public Map<String,PathItem> getCallbacks() {
		return callbacks;
	}

	/**
	 * Sets the callbacks map.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Callback setCallbacks(Map<String,PathItem> value) {
		this.callbacks = value;
		return this;
	}

	/**
	 * Adds a callback.
	 *
	 * @param expression The callback expression.
	 * @param pathItem The path item for the callback.
	 * @return This object.
	 */
	public Callback addCallback(String expression, PathItem pathItem) {
		if (callbacks == null)
			callbacks = new LinkedHashMap<>();
		callbacks.put(expression, pathItem);
		return this;
	}
}
