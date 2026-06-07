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

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.json.*;

/**
 * Represents a JSON:API Resource Identifier Object as defined by
 * <a href="https://jsonapi.org/format/#document-resource-identifier-objects">JSON:API v1.1 &#167; Resource Identifier Objects</a>.
 *
 * <p>
 * A slim form of {@link JsonApiResource} used inside relationship {@code data} linkage. Carries only {@code type},
 * {@code id}, and an optional {@code meta} bag. {@code type} is a plain {@code String} for the same reasons
 * documented on {@link JsonApiResource#getType()}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 *   <li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanJsonApi">juneau-bean-jsonapi</a>
 * </ul>
 */
@Marshalled
public class JsonApiResourceIdentifier {

	private String type;
	private String id;
	private Map<String,Object> meta;

	/**
	 * Default constructor.
	 */
	public JsonApiResourceIdentifier() {}

	/**
	 * Convenience constructor.
	 *
	 * @param type The entity-type name.
	 * @param id The identifier value.
	 */
	public JsonApiResourceIdentifier(String type, String id) {
		this.type = type;
		this.id = id;
	}

	/**
	 * Bean property getter:  <property>type</property>.
	 *
	 * @return The value of the <property>type</property> property, or <jk>null</jk> if it is not set.
	 */
	public String getType() { return type; }

	/**
	 * Bean property setter:  <property>type</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public JsonApiResourceIdentifier setType(String value) {
		type = value;
		return this;
	}

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
	public JsonApiResourceIdentifier setId(String value) {
		id = value;
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
	public JsonApiResourceIdentifier setMeta(Map<String,Object> value) {
		meta = value;
		return this;
	}

	@Override /* Overridden from Object */
	public String toString() {
		return JsonSerializer.DEFAULT.toString(this);
	}
}
