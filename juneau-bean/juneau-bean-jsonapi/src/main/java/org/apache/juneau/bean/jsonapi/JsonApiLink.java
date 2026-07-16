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
import org.apache.juneau.marshall.marshaller.*;

/**
 * Represents a JSON:API Link Object as defined by
 * <a href="https://jsonapi.org/format/#document-links">JSON:API v1.1 &#167; Links</a>.
 *
 * <p>
 * Per the spec, each {@code links} entry may be either a JSON string URL or a JSON object with these members:
 * {@code href} (required URI), {@code rel}, {@code describedby}, {@code title}, {@code type} (media-type hint),
 * {@code hreflang} (language tag), and {@code meta}. The single-vs-object union is handled by
 * {@link JsonApiLinkOrStringSwap}, applied via field-level {@code @BeanProp("links") @Swap(...)} on every type
 * carrying a {@code links} map.
 *
 * <h5 class='section'>See Also:</h5><ul>
 *   <li class='link'><a href="https://jsonapi.org/format/#document-links">JSON:API v1.1 &gt; Links</a>
 *   <li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanJsonApi">juneau-bean-jsonapi</a>
 * </ul>
 */
@Marshalled
public class JsonApiLink {

	private String href;
	private String rel;
	private String describedby;
	private String title;
	private String type;
	private String hreflang;
	private Map<String,Object> meta;

	/**
	 * Default constructor.
	 */
	public JsonApiLink() {}

	/**
	 * Convenience constructor with the required {@code href} field.
	 *
	 * @param href The link target URI.
	 */
	public JsonApiLink(String href) {
		this.href = href;
	}

	/**
	 * Bean property getter:  <property>href</property>.
	 *
	 * @return The value of the <property>href</property> property, or <jk>null</jk> if it is not set.
	 */
	public String getHref() { return href; }

	/**
	 * Bean property setter:  <property>href</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public JsonApiLink setHref(String value) {
		href = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>rel</property>.
	 *
	 * @return The value of the <property>rel</property> property, or <jk>null</jk> if it is not set.
	 */
	public String getRel() { return rel; }

	/**
	 * Bean property setter:  <property>rel</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public JsonApiLink setRel(String value) {
		rel = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>describedby</property>.
	 *
	 * @return The value of the <property>describedby</property> property, or <jk>null</jk> if it is not set.
	 */
	public String getDescribedby() { return describedby; }

	/**
	 * Bean property setter:  <property>describedby</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public JsonApiLink setDescribedby(String value) {
		describedby = value;
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
	public JsonApiLink setTitle(String value) {
		title = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>type</property>.
	 *
	 * <p>
	 * Media-type hint for the target resource.
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
	public JsonApiLink setType(String value) {
		type = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>hreflang</property>.
	 *
	 * @return The value of the <property>hreflang</property> property, or <jk>null</jk> if it is not set.
	 */
	public String getHreflang() { return hreflang; }

	/**
	 * Bean property setter:  <property>hreflang</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public JsonApiLink setHreflang(String value) {
		hreflang = value;
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
	public JsonApiLink setMeta(Map<String,Object> value) {
		meta = value;
		return this;
	}

	@Override /* Overridden from Object */
	public String toString() {
		return Json.of(this);
	}
}
