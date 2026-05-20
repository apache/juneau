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
import org.apache.juneau.json.*;

/**
 * Represents the JSON:API top-level {@code jsonapi} member.
 *
 * <p>
 * Per <a href="https://jsonapi.org/format/#document-jsonapi-object">JSON:API v1.1 &#167; JSON:API Object</a>,
 * this object carries the optional protocol {@code version} string (e.g. {@code "1.1"}) and an optional
 * {@code meta} bag.
 *
 * <h5 class='section'>See Also:</h5><ul>
 *   <li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanJsonApi">juneau-bean-jsonapi</a>
 * </ul>
 */
@Marshalled
public class JsonApiVersion {

	private String version;
	private Map<String,Object> meta;

	/**
	 * Default constructor.
	 */
	public JsonApiVersion() {}

	/**
	 * Convenience constructor.
	 *
	 * @param version The JSON:API protocol version (e.g. {@code "1.1"}).
	 */
	public JsonApiVersion(String version) {
		this.version = version;
	}

	/**
	 * Bean property getter:  <property>version</property>.
	 *
	 * @return The value of the <property>version</property> property, or <jk>null</jk> if it is not set.
	 */
	public String getVersion() { return version; }

	/**
	 * Bean property setter:  <property>version</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public JsonApiVersion setVersion(String value) {
		version = value;
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
	public JsonApiVersion setMeta(Map<String,Object> value) {
		meta = value;
		return this;
	}

	@Override /* Overridden from Object */
	public String toString() {
		return JsonSerializer.DEFAULT.toString(this);
	}
}
