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
package org.apache.juneau.bean.jsonapi;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.json.*;

/**
 * Represents a JSON:API Relationship Object as defined by
 * <a href="https://jsonapi.org/format/#document-resource-object-relationships">JSON:API v1.1 &#167; Relationships</a>.
 *
 * <p>
 * A relationship carries:
 * <ul class='spaced-list'>
 *   <li>{@code data} - resource linkage. Per spec, this can be {@code null}, a single
 *     {@link JsonApiResourceIdentifier}, or a JSON array of identifiers. Modeled here as {@code Object}.
 *   <li>{@code links} - related URLs. Each value can be either a JSON string URL or a {@link JsonApiLink} object;
 *     the field-level {@link JsonApiLinkOrStringSwap} swap handles the union.
 *   <li>{@code meta} - free-form meta bag.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 *   <li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanJsonApi">juneau-bean-jsonapi</a>
 * </ul>
 */
@Marshalled
public class JsonApiRelationship {

	private Object data;
	private Map<String,Object> links;
	private Map<String,Object> meta;

	/**
	 * Default constructor.
	 */
	public JsonApiRelationship() { /* intentionally empty */ }

	/**
	 * Bean property getter:  <property>data</property>.
	 *
	 * <p>
	 * Resource linkage. Per spec, this may be {@code null}, a single {@link JsonApiResourceIdentifier}, or a
	 * {@code List&lt;JsonApiResourceIdentifier&gt;}.
	 *
	 * @return The value of the <property>data</property> property, or <jk>null</jk> if it is not set.
	 */
	public Object getData() { return data; }

	/**
	 * Bean property setter:  <property>data</property>.
	 *
	 * @param value The new value (single identifier, list of identifiers, or {@code null} to unset).
	 * @return This object.
	 */
	public JsonApiRelationship setData(Object value) {
		data = value;
		return this;
	}

	/**
	 * Typed setter for single-identifier linkage.
	 *
	 * @param value The identifier.
	 * @return This object.
	 */
	public JsonApiRelationship setData(JsonApiResourceIdentifier value) {
		data = value;
		return this;
	}

	/**
	 * Typed setter for to-many linkage.
	 *
	 * @param value The list of identifiers.
	 * @return This object.
	 */
	public JsonApiRelationship setData(List<JsonApiResourceIdentifier> value) {
		data = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>links</property>.
	 *
	 * <p>
	 * Per spec, each {@code links} value can be either a JSON string URL or a {@link JsonApiLink} object.
	 *
	 * @return The value of the <property>links</property> property, or <jk>null</jk> if it is not set.
	 */
	@BeanProp(value="links")
	@Swap(JsonApiLinkOrStringSwap.class)
	public Map<String,Object> getLinks() { return links; }

	/**
	 * Bean property setter:  <property>links</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	@BeanProp(value="links")
	@Swap(JsonApiLinkOrStringSwap.class)
	public JsonApiRelationship setLinks(Map<String,Object> value) {
		links = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>meta</property>.
	 *
	 * @return The value of the <property>meta</property> property, or <jk>null</jk> if it is not set.
	 */
	public Map<String,Object> getMeta() { return meta; }

	/**
	 * Bean property setter:  <property>meta</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public JsonApiRelationship setMeta(Map<String,Object> value) {
		meta = value;
		return this;
	}

	@Override /* Overridden from Object */
	public String toString() {
		return JsonSerializer.DEFAULT.toString(this);
	}
}
