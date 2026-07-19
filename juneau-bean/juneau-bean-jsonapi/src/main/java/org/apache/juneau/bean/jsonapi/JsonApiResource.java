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
import org.apache.juneau.marshall.marshaller.*;

/**
 * Represents a JSON:API Resource Object as defined by
 * <a href="https://jsonapi.org/format/#document-resource-objects">JSON:API v1.1 &#167; Resource Objects</a>.
 *
 * <p>
 * Carries the standard six members: {@code type}, {@code id}, {@code attributes}, {@code relationships},
 * {@code links}, {@code meta}.
 *
 * <h5 class='topic'>Why {@code type} is a plain String</h5>
 *
 * <p>
 * JSON:API uses {@code type} as an open-ended string field on resource objects (the entity-type name, e.g.
 * {@code "articles"}); the value is chosen by the API and is <b>not</b> a closed Java class hierarchy.
 * Juneau's {@code @Marshalled(typePropertyName=...)} discriminator defaults to {@code _type} (so there is also
 * no literal name clash). <b>Do NOT</b> annotate {@code JsonApiResource} with
 * {@code @Marshalled(typePropertyName="type", dictionary=...)} — that would conflate JSON:API resource typing
 * with Juneau bean dispatch, break round-tripping of unknown {@code type} values, and force every API to
 * enumerate its resource types in a Java dictionary up front.
 *
 * <h5 class='section'>See Also:</h5><ul>
 *   <li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanJsonApi">juneau-bean-jsonapi</a>
 * </ul>
 */
@Marshalled
public class JsonApiResource {

	private String type;
	private String id;
	private Map<String,Object> attributes;
	private Map<String,JsonApiRelationship> relationships;
	private Map<String,Object> links;
	private Map<String,Object> meta;

	/**
	 * Default constructor.
	 */
	public JsonApiResource() {}

	/**
	 * Convenience constructor.
	 *
	 * @param type The entity-type name.  Can be <jk>null</jk>.
	 * @param id The identifier value.  Can be <jk>null</jk>.
	 */
	public JsonApiResource(String type, String id) {
		this.type = type;
		this.id = id;
	}

	/**
	 * Bean property getter:  <property>type</property>.
	 *
	 * <p>
	 * Plain {@code String} entity-type name. See class-level Javadoc for why this is not a Juneau polymorphic
	 * discriminator.
	 *
	 * @return The value of the <property>type</property> property, or <jk>null</jk> if it is not set.
	 */
	public String getType() { return type; }

	/**
	 * Bean property setter:  <property>type</property>.
	 *
	 * @param value The new value for this property.  Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public JsonApiResource setType(String value) {
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
	 * @param value The new value for this property.  Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public JsonApiResource setId(String value) {
		id = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>attributes</property>.
	 *
	 * <p>
	 * The spec lets {@code attributes} be any JSON object. Modeled as {@code Map&lt;String,Object&gt;} (a nested
	 * object on the wire — not flattened).
	 *
	 * @return The value of the <property>attributes</property> property, or <jk>null</jk> if it is not set.
	 */
	public Map<String,Object> getAttributes() { return u(attributes); }

	/**
	 * Bean property setter:  <property>attributes</property>.
	 *
	 * @param value The new value for this property.  Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public JsonApiResource setAttributes(Map<String,Object> value) {
		attributes = value;
		return this;
	}

	/**
	 * Convenience method to add a single attribute.
	 *
	 * @param name The attribute name.  Can be <jk>null</jk>.
	 * @param value The attribute value.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public JsonApiResource putAttribute(String name, Object value) {
		if (attributes == null)
			attributes = map();
		attributes.put(name, value);
		return this;
	}

	/**
	 * Bean property getter:  <property>relationships</property>.
	 *
	 * @return The value of the <property>relationships</property> property, or <jk>null</jk> if it is not set.
	 */
	public Map<String,JsonApiRelationship> getRelationships() { return u(relationships); }

	/**
	 * Bean property setter:  <property>relationships</property>.
	 *
	 * @param value The new value for this property.  Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public JsonApiResource setRelationships(Map<String,JsonApiRelationship> value) {
		relationships = value;
		return this;
	}

	/**
	 * Convenience method to add a single named relationship.
	 *
	 * @param name The relationship name.  Can be <jk>null</jk>.
	 * @param value The relationship object.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public JsonApiResource putRelationship(String name, JsonApiRelationship value) {
		if (relationships == null)
			relationships = map();
		relationships.put(name, value);
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
	public Map<String,Object> getLinks() { return u(links); }

	/**
	 * Bean property setter:  <property>links</property>.
	 *
	 * @param value The new value for this property.  Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	@BeanProp(value="links")
	@Swap(JsonApiLinkOrStringSwap.class)
	public JsonApiResource setLinks(Map<String,Object> value) {
		links = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>meta</property>.
	 *
	 * @return The value of the <property>meta</property> property, or <jk>null</jk> if it is not set.
	 */
	public Map<String,Object> getMeta() { return u(meta); }

	/**
	 * Bean property setter:  <property>meta</property>.
	 *
	 * @param value The new value for this property.  Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public JsonApiResource setMeta(Map<String,Object> value) {
		meta = value;
		return this;
	}

	@Override /* Overridden from Object */
	public String toString() {
		return Json.of(this);
	}
}
