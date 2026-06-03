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

import org.apache.juneau.annotation.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.json.*;

/**
 * Represents a JSON:API Error Object as defined by
 * <a href="https://jsonapi.org/format/#error-objects">JSON:API v1.1 &#167; Error Objects</a>.
 *
 * <h5 class='section'>See Also:</h5><ul>
 *   <li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanJsonApi">juneau-bean-jsonapi</a>
 * </ul>
 */
@Marshalled
public class JsonApiError {

	private String id;
	private Map<String,Object> links;
	private String status;
	private String code;
	private String title;
	private String detail;
	private JsonApiErrorSource source;
	private Map<String,Object> meta;

	/**
	 * Default constructor.
	 */
	public JsonApiError() { /* intentionally empty */ }

	/**
	 * Bean property getter:  <property>id</property>.
	 *
	 * @return The value of the <property>id</property> property, or <jk>null</jk> if it is not set.
	 */
	public String getId() { return id; }

	/**
	 * Bean property setter:  <property>id</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public JsonApiError setId(String value) {
		id = value;
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
	public JsonApiError setLinks(Map<String,Object> value) {
		links = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>status</property>.
	 *
	 * <p>
	 * Per spec, the HTTP status code as a string.
	 *
	 * @return The value of the <property>status</property> property, or <jk>null</jk> if it is not set.
	 */
	public String getStatus() { return status; }

	/**
	 * Bean property setter:  <property>status</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public JsonApiError setStatus(String value) {
		status = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>code</property>.
	 *
	 * <p>
	 * An application-specific error code.
	 *
	 * @return The value of the <property>code</property> property, or <jk>null</jk> if it is not set.
	 */
	public String getCode() { return code; }

	/**
	 * Bean property setter:  <property>code</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public JsonApiError setCode(String value) {
		code = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>title</property>.
	 *
	 * @return The value of the <property>title</property> property, or <jk>null</jk> if it is not set.
	 */
	public String getTitle() { return title; }

	/**
	 * Bean property setter:  <property>title</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public JsonApiError setTitle(String value) {
		title = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>detail</property>.
	 *
	 * @return The value of the <property>detail</property> property, or <jk>null</jk> if it is not set.
	 */
	public String getDetail() { return detail; }

	/**
	 * Bean property setter:  <property>detail</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public JsonApiError setDetail(String value) {
		detail = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>source</property>.
	 *
	 * @return The value of the <property>source</property> property, or <jk>null</jk> if it is not set.
	 */
	public JsonApiErrorSource getSource() { return source; }

	/**
	 * Bean property setter:  <property>source</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public JsonApiError setSource(JsonApiErrorSource value) {
		source = value;
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
	public JsonApiError setMeta(Map<String,Object> value) {
		meta = value;
		return this;
	}

	@Override /* Overridden from Object */
	public String toString() {
		return JsonSerializer.DEFAULT.toString(this);
	}
}
