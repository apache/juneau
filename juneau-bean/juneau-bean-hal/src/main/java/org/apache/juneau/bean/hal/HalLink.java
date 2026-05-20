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
package org.apache.juneau.bean.hal;

import org.apache.juneau.annotation.*;
import org.apache.juneau.json.*;

/**
 * Represents a HAL Link Object as defined by
 * <a href="https://datatracker.ietf.org/doc/html/draft-kelly-json-hal-08#section-5">draft-kelly-json-hal-08 &#167; 5</a>.
 *
 * <p>
 * A Link Object describes a hyperlink from the containing resource to another resource. The {@code href} property is
 * the only required field; all other fields are optional.
 *
 * <h5 class='section'>See Also:</h5><ul>
 *   <li class='link'><a href="https://datatracker.ietf.org/doc/html/draft-kelly-json-hal-08#section-5">HAL Link Object</a>
 *   <li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHal">juneau-bean-hal</a>
 * </ul>
 */
@Marshalled
@SuppressWarnings({
	"java:S116" // Field names mirror HAL spec.
})
public class HalLink {

	private String href;
	private Boolean templated;
	private String type;
	private String deprecation;
	private String name;
	private String profile;
	private String title;
	private String hreflang;

	/**
	 * Default constructor.
	 */
	public HalLink() {}

	/**
	 * Convenience constructor with the required {@code href} field.
	 *
	 * @param href The link URI or URI template.
	 */
	public HalLink(String href) {
		this.href = href;
	}

	/**
	 * Bean property getter:  <property>href</property>.
	 *
	 * <p>
	 * The link's target URI or URI template (per RFC 6570). This is the only required HAL Link field.
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
	public HalLink setHref(String value) {
		href = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>templated</property>.
	 *
	 * <p>
	 * Indicates whether {@link #getHref() href} is a URI template per RFC 6570.
	 *
	 * @return The value of the <property>templated</property> property, or <jk>null</jk> if it is not set.
	 */
	public Boolean getTemplated() { return templated; }

	/**
	 * Bean property setter:  <property>templated</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public HalLink setTemplated(Boolean value) {
		templated = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>type</property>.
	 *
	 * <p>
	 * A hint about the media type expected when dereferencing the target resource.
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
	public HalLink setType(String value) {
		type = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>deprecation</property>.
	 *
	 * <p>
	 * A URL providing information about deprecation of the link.
	 *
	 * @return The value of the <property>deprecation</property> property, or <jk>null</jk> if it is not set.
	 */
	public String getDeprecation() { return deprecation; }

	/**
	 * Bean property setter:  <property>deprecation</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public HalLink setDeprecation(String value) {
		deprecation = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>name</property>.
	 *
	 * <p>
	 * A secondary key for selecting a link from a set sharing the same relation.
	 *
	 * @return The value of the <property>name</property> property, or <jk>null</jk> if it is not set.
	 */
	public String getName() { return name; }

	/**
	 * Bean property setter:  <property>name</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public HalLink setName(String value) {
		name = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>profile</property>.
	 *
	 * <p>
	 * A URI hint about the profile (additional semantics) of the target resource.
	 *
	 * @return The value of the <property>profile</property> property, or <jk>null</jk> if it is not set.
	 */
	public String getProfile() { return profile; }

	/**
	 * Bean property setter:  <property>profile</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public HalLink setProfile(String value) {
		profile = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>title</property>.
	 *
	 * <p>
	 * A human-readable identifier for the link, suitable for display.
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
	public HalLink setTitle(String value) {
		title = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>hreflang</property>.
	 *
	 * <p>
	 * The language of the target resource (RFC 5646 language tag).
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
	public HalLink setHreflang(String value) {
		hreflang = value;
		return this;
	}

	@Override /* Overridden from Object */
	public String toString() {
		return JsonSerializer.DEFAULT.toString(this);
	}
}
