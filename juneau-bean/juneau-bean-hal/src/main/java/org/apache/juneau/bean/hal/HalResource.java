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

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.marshaller.*;

/**
 * Represents a HAL Resource Object as defined by
 * <a href="https://datatracker.ietf.org/doc/html/draft-kelly-json-hal-08#section-4">draft-kelly-json-hal-08 &#167; 4</a>.
 *
 * <p>
 * A HAL Resource carries three pieces of state:
 * <ul class='spaced-list'>
 *   <li>The reserved {@code _links} member - a map of relation name to either a single {@link HalLink}
 *     or a {@link HalLinkArray}.
 *   <li>The reserved {@code _embedded} member - a map of relation name to either a single {@link HalResource}
 *     or a {@link HalResourceArray} (sub-resources). HAL embeddings are conceptually a <i>tree</i>; cyclic
 *     {@code _embedded} graphs are rejected by Juneau's {@code BeanTraverseContext} by default.
 *   <li>Arbitrary payload fields, set via the dynamic {@link #set(String,Object)} method (uses the
 *     {@code @BeanProp("*")} extra-properties triplet, the same pattern used by {@code OpenApiElement} and
 *     {@code SwaggerElement}).
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	HalResource <jv>order</jv> = <jk>new</jk> HalResource()
 * 		.addLink(<js>"self"</js>, <jk>new</jk> HalLink().setHref(<js>"/orders/123"</js>))
 * 		.addLinks(<js>"curies"</js>,
 * 			<jk>new</jk> HalLink().setName(<js>"acme"</js>).setHref(<js>"https://acme.example/{rel}"</js>).setTemplated(<jk>true</jk>))
 * 		.set(<js>"total"</js>, 99.50);
 *
 * 	String <jv>json</jv> = JsonSerializer.<jsf>DEFAULT</jsf>.write(<jv>order</jv>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 *   <li class='link'><a href="https://stateless.group/hal_specification.html">HAL Specification</a>
 *   <li class='link'><a href="https://datatracker.ietf.org/doc/html/draft-kelly-json-hal-08">draft-kelly-json-hal-08</a>
 *   <li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHal">juneau-bean-hal</a>
 * </ul>
 */
@Marshalled
@SuppressWarnings({
	"java:S116", // Field names mirror HAL spec.
	"java:S115" // Constant names intentionally mirror argument identifiers.
})
public class HalResource {

	private static final String ARG_property = "property";
	private static final String ARG_relation = "relation";

	private Map<String,Object> _links;
	private Map<String,Object> _embedded;
	private Map<String,Object> properties;

	/**
	 * Default constructor.
	 */
	public HalResource() {
		/* intentionally empty — required public no-arg constructor for bean creation/decoding */
	}

	/**
	 * Bean property getter:  <property>_links</property>.
	 *
	 * <p>
	 * The reserved {@code _links} member. Each map value is either a single {@link HalLink} (rendered as a JSON
	 * object on the wire) or a {@link HalLinkArray} (rendered as a JSON array).
	 *
	 * @return The value of the <property>_links</property> property, or <jk>null</jk> if it is not set.
	 */
	@BeanProp("_links")
	@Swap(HalLinkOrArraySwap.class)
	public Map<String,Object> getLinks() {
		return u(_links);
	}

	/**
	 * Bean property setter:  <property>_links</property>.
	 *
	 * @param value The new value for this property. Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	@BeanProp("_links")
	@Swap(HalLinkOrArraySwap.class)
	public HalResource setLinks(Map<String,Object> value) {
		_links = value;
		return this;
	}

	/**
	 * Convenience method that adds a single link under the given relation.
	 *
	 * <p>
	 * If a value already exists for {@code relation}, it is replaced with the supplied single link. To accumulate
	 * multiple links under the same relation use {@link #addLinks(String, HalLink...)} instead.
	 *
	 * @param relation The link relation (e.g. {@code "self"}, {@code "next"}, {@code "curies"}).  Must not be <jk>null</jk>.
	 * @param value The link to set.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public HalResource addLink(String relation, HalLink value) {
		assertArgNotNull(ARG_relation, relation);
		if (_links == null)
			_links = map();
		_links.put(relation, value);
		return this;
	}

	/**
	 * Convenience method that appends one or more links to a {@link HalLinkArray} stored under {@code relation}.
	 *
	 * <p>
	 * If the current value at {@code relation} is a single {@link HalLink}, it is promoted to a {@link HalLinkArray}
	 * containing the original link and the appended ones.
	 *
	 * @param relation The link relation.  Must not be <jk>null</jk>.
	 * @param values The links to append.
	 * @return This object.
	 */
	public HalResource addLinks(String relation, HalLink...values) {
		assertArgNotNull(ARG_relation, relation);
		if (_links == null)
			_links = map();
		var existing = _links.get(relation);
		HalLinkArray arr;
		if (existing instanceof HalLinkArray existing2) {
			arr = existing2;
		} else if (existing instanceof HalLink existing2) {
			arr = new HalLinkArray(existing2);
			_links.put(relation, arr);
		} else {
			arr = new HalLinkArray();
			_links.put(relation, arr);
		}
		arr.addAll(values);
		return this;
	}

	/**
	 * Bean property getter:  <property>_embedded</property>.
	 *
	 * <p>
	 * The reserved {@code _embedded} member. Each map value is either a single {@link HalResource} or a
	 * {@link HalResourceArray}.
	 *
	 * @return The value of the <property>_embedded</property> property, or <jk>null</jk> if it is not set.
	 */
	@BeanProp("_embedded")
	@Swap(HalResourceOrArraySwap.class)
	public Map<String,Object> getEmbedded() {
		return u(_embedded);
	}

	/**
	 * Bean property setter:  <property>_embedded</property>.
	 *
	 * <p>
	 * Note: HAL embeddings are conceptually a tree, not a graph. Juneau's {@code BeanTraverseContext} throws on
	 * cyclic references by default, which matches the spec.
	 *
	 * @param value The new value for this property. Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	@BeanProp("_embedded")
	@Swap(HalResourceOrArraySwap.class)
	public HalResource setEmbedded(Map<String,Object> value) {
		_embedded = value;
		return this;
	}

	/**
	 * Convenience method that embeds a single sub-resource under the given relation.
	 *
	 * @param relation The relation name.  Must not be <jk>null</jk>.
	 * @param value The sub-resource.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public HalResource addEmbedded(String relation, HalResource value) {
		assertArgNotNull(ARG_relation, relation);
		if (_embedded == null)
			_embedded = map();
		_embedded.put(relation, value);
		return this;
	}

	/**
	 * Convenience method that appends one or more sub-resources to a {@link HalResourceArray} stored under
	 * {@code relation}.
	 *
	 * @param relation The relation name.  Must not be <jk>null</jk>.
	 * @param values The sub-resources to append.
	 * @return This object.
	 */
	public HalResource addEmbedded(String relation, HalResource...values) {
		assertArgNotNull(ARG_relation, relation);
		if (_embedded == null)
			_embedded = map();
		var existing = _embedded.get(relation);
		HalResourceArray arr;
		if (existing instanceof HalResourceArray existing2) {
			arr = existing2;
		} else if (existing instanceof HalResource existing2) {
			arr = new HalResourceArray(existing2);
			_embedded.put(relation, arr);
		} else {
			arr = new HalResourceArray();
			_embedded.put(relation, arr);
		}
		arr.addAll(values);
		return this;
	}

	/**
	 * Generic property keyset.
	 *
	 * @return All the payload (non-{@code _links}, non-{@code _embedded}) property names on this resource. Never <jk>null</jk>.
	 */
	@BeanProp("*")
	public Set<String> extraKeys() {
		return properties == null ? Collections.emptySet() : u(properties.keySet());
	}

	/**
	 * Generic property getter.
	 *
	 * @param property The property name. Must not be <jk>null</jk>.
	 * @return The property value, or <jk>null</jk> if the property is not set.
	 */
	@BeanProp("*")
	public Object get(String property) {
		assertArgNotNull(ARG_property, property);
		return o(properties).map(x -> x.get(property)).orElse(null);
	}

	/**
	 * Generic property setter.
	 *
	 * <p>
	 * Use this to set arbitrary payload fields alongside {@code _links} and {@code _embedded}.
	 *
	 * @param property The property name. Must not be <jk>null</jk>.
	 * @param value The new value for the property. Can be <jk>null</jk>.
	 * @return This object.
	 */
	@BeanProp("*")
	public HalResource set(String property, Object value) {
		assertArgNotNull(ARG_property, property);
		if (properties == null)
			properties = map();
		properties.put(property, value);
		return this;
	}

	@Override /* Overridden from Object */
	public String toString() {
		return Json.of(this);
	}
}
