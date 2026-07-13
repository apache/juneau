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

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.json.*;

/**
 * Represents the top-level JSON:API document as defined by
 * <a href="https://jsonapi.org/format/#document-top-level">JSON:API v1.1 &#167; Document Structure</a>.
 *
 * <p>
 * Carries the standard six top-level members: {@code data}, {@code errors}, {@code meta}, {@code jsonapi},
 * {@code links}, {@code included}.
 *
 * <h5 class='topic'>Mutual exclusion</h5>
 *
 * <p>
 * Per spec, a document MUST contain at least one of {@code data}/{@code errors}/{@code meta}, and {@code data}
 * and {@code errors} MUST NOT coexist. The {@link #validate()} helper checks both rules and throws
 * {@link IllegalStateException} on violation.
 *
 * <h5 class='topic'>The polymorphic {@code data} member</h5>
 *
 * <p>
 * The top-level {@code data} can be {@code null}, a single {@link JsonApiResource}, or an array of them. We
 * model {@code data} as a plain {@code Object} property; the writer chooses single-vs-array shape based on
 * which value was set. Typed setters {@link #setData(JsonApiResource)} and {@link #setData(List)} are provided
 * for convenience.
 *
 * <h5 class='section'>See Also:</h5><ul>
 *   <li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanJsonApi">juneau-bean-jsonapi</a>
 * </ul>
 */
@Marshalled
public class JsonApiDocument {

	private Object data;
	private List<JsonApiError> errors;
	private Map<String,Object> meta;
	private JsonApiVersion jsonapi;
	private Map<String,Object> links;
	private List<JsonApiResource> included;

	/**
	 * Default constructor.
	 */
	public JsonApiDocument() { /* intentionally empty */ }

	/**
	 * Bean property getter:  <property>data</property>.
	 *
	 * <p>
	 * Top-level resource linkage. May be {@code null}, a single {@link JsonApiResource}, or a
	 * {@code List&lt;JsonApiResource&gt;}.
	 *
	 * @return The value of the <property>data</property> property, or <jk>null</jk> if it is not set.
	 */
	public Object getData() { return data; }

	/**
	 * Bean property setter:  <property>data</property>.
	 *
	 * @param value The new value (single resource, list of resources, or {@code null} to unset).
	 * @return This object.
	 */
	public JsonApiDocument setData(Object value) {
		data = value;
		return this;
	}

	/**
	 * Typed setter for single-resource primary data.
	 *
	 * @param value The resource.
	 * @return This object.
	 */
	public JsonApiDocument setData(JsonApiResource value) {
		data = value;
		return this;
	}

	/**
	 * Typed setter for to-many primary data.
	 *
	 * @param value The list of resources.
	 * @return This object.
	 */
	public JsonApiDocument setData(List<JsonApiResource> value) {
		data = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>errors</property>.
	 *
	 * @return The value of the <property>errors</property> property, or <jk>null</jk> if it is not set.
	 */
	public List<JsonApiError> getErrors() { return errors; }

	/**
	 * Bean property setter:  <property>errors</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public JsonApiDocument setErrors(List<JsonApiError> value) {
		errors = value;
		return this;
	}

	/**
	 * Bean property appender:  <property>errors</property>.
	 *
	 * @param value The error objects to append.
	 * @return This object.
	 */
	public JsonApiDocument addErrors(JsonApiError...value) {
		if (errors == null)
			errors = list();
		Collections.addAll(errors, value);
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
	public JsonApiDocument setMeta(Map<String,Object> value) {
		meta = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>jsonapi</property>.
	 *
	 * @return The value of the <property>jsonapi</property> property, or <jk>null</jk> if it is not set.
	 */
	public JsonApiVersion getJsonapi() { return jsonapi; }

	/**
	 * Bean property setter:  <property>jsonapi</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public JsonApiDocument setJsonapi(JsonApiVersion value) {
		jsonapi = value;
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
	public JsonApiDocument setLinks(Map<String,Object> value) {
		links = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>included</property>.
	 *
	 * @return The value of the <property>included</property> property, or <jk>null</jk> if it is not set.
	 */
	public List<JsonApiResource> getIncluded() { return included; }

	/**
	 * Bean property setter:  <property>included</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public JsonApiDocument setIncluded(List<JsonApiResource> value) {
		included = value;
		return this;
	}

	/**
	 * Bean property appender:  <property>included</property>.
	 *
	 * @param value The resource objects to append.
	 * @return This object.
	 */
	public JsonApiDocument addIncluded(JsonApiResource...value) {
		if (included == null)
			included = list();
		Collections.addAll(included, value);
		return this;
	}

	/**
	 * Validates document-level invariants per the JSON:API spec.
	 *
	 * <p>
	 * Throws if {@code data} and {@code errors} are both present, or if none of {@code data}/{@code errors}/{@code meta}
	 * is present.
	 *
	 * @return This object.
	 * @throws IllegalStateException If the document violates a top-level invariant.
	 */
	public JsonApiDocument validate() {
		if (data != null && errors != null)
			throw isex("JSON:API document must not contain both 'data' and 'errors'.");
		if (data == null && errors == null && meta == null)
			throw isex("JSON:API document must contain at least one of 'data', 'errors', or 'meta'.");
		return this;
	}

	@Override /* Overridden from Object */
	public String toString() {
		return JsonSerializer.DEFAULT.toString(this);
	}
}
