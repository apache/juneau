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

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.commons.collections.*;

/**
 * A map of possible out-of-band callbacks related to the parent operation.
 *
 * <p>
 * The Callback Object is a map of possible out-of-band callbacks related to the parent operation. Each value in the
 * map is a Path Item Object that describes a set of requests that may be initiated by the API provider and the expected
 * responses. The key value used to identify the callback object is an expression, evaluated at runtime, that identifies
 * a URL to use for the callback operation.
 *
 * <h5 class='section'>OpenAPI Specification:</h5>
 * <p>
 * The Callback Object is composed of the following fields:
 * <ul class='spaced-list'>
 * 	<li><c>callbacks</c> (map of {@link PathItem}) - A map of possible out-of-band callbacks related to the parent operation
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	Callback <jv>x</jv> = <jsm>callback</jsm>()
 * 		.setCallbacks(<jsm>map</jsm>(<js>"myCallback"</js>, <jsm>pathItem</jsm>().setPost(<jsm>operation</jsm>().setSummary(<js>"Callback"</js>))));
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
 * 		<js>"callbacks"</js>: {
 * 			<js>"myCallback"</js>: {
 * 				<js>"post"</js>: { <js>"summary"</js>: <js>"Callback"</js> }
 * 			}
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://spec.openapis.org/oas/v3.0.0#callback-object">OpenAPI Specification &gt; Callback Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/callbacks/">OpenAPI Callbacks</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanOpenApi3">juneau-bean-openapi-v3</a>
 * </ul>
 */
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
	 * Adds a callback.
	 *
	 * @param expression The callback expression.  Must not be <jk>null</jk>.
	 * @param pathItem The path item for the callback.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public Callback addCallback(String expression, PathItem pathItem) {
		assertArgNotNull("expression", expression);
		assertArgNotNull("pathItem", pathItem);
		if (callbacks == null)
			callbacks = new LinkedHashMap<>();
		callbacks.put(expression, pathItem);
		return this;
	}

	/**
	 * Creates a copy of this object.
	 *
	 * @return A copy of this object.
	 */
	public Callback copy() {
		return new Callback(this);
	}

	@Override /* Overridden from OpenApiElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "callbacks" -> toType(getCallbacks(), type);
			default -> super.get(property, type);
		};
	}

	/**
	 * Returns the callbacks map.
	 *
	 * @return The callbacks map.
	 */
	public Map<String,PathItem> getCallbacks() { return callbacks; }

	@Override /* Overridden from OpenApiElement */
	public Set<String> keySet() {
		// @formatter:off
		var s = setb(String.class)
			.addIf(nn(callbacks), "callbacks")
			.build();
		// @formatter:on
		return new MultiSet<>(s, super.keySet());
	}

	@Override /* Overridden from OpenApiElement */
	public Callback set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "callbacks" -> setCallbacks(toMapBuilder(value, String.class, PathItem.class).sparse().build());
			default -> {
				super.set(property, value);
				yield this;
			}
		};
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

	@Override /* Overridden from OpenApiElement */
	public Callback strict() {
		super.strict();
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	public Callback strict(Object value) {
		super.strict(value);
		return this;
	}
}