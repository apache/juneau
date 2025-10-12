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
package org.apache.juneau.bean.atom;

import static org.apache.juneau.common.utils.StringUtils.*;
import static org.apache.juneau.xml.annotation.XmlFormat.*;

import java.net.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Represents a category or tag associated with a feed or entry.
 *
 * <p>
 * Categories provide a way to classify or tag feeds and entries, enabling better organization 
 * and discovery of content. Each category has a term (required) and optionally a scheme (for 
 * namespacing) and a human-readable label.
 *
 * <p>
 * Categories are commonly used for:
 * <ul class='spaced-list'>
 * 	<li>Tagging entries by topic (e.g., "technology", "sports")
 * 	<li>Classifying content by category schemes (e.g., subject taxonomies)
 * 	<li>Enabling feed filtering and organization
 * </ul>
 *
 * <h5 class='figure'>Schema</h5>
 * <p class='bschema'>
 * 	atomCategory =
 * 		element atom:category {
 * 			atomCommonAttributes,
 * 			attribute term { text },
 * 			attribute scheme { atomUri }?,
 * 			attribute label { text }?,
 * 			undefinedContent
 * 		}
 * </p>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Simple category</jc>
 * 	Category <jv>cat1</jv> = <jk>new</jk> Category(<js>"technology"</js>);
 *
 * 	<jc>// Category with scheme and label</jc>
 * 	Category <jv>cat2</jv> = <jk>new</jk> Category(<js>"tech"</js>)
 * 		.setScheme(<js>"http://example.org/categories"</js>)
 * 		.setLabel(<js>"Technology"</js>);
 *
 * 	<jc>// Add to entry</jc>
 * 	Entry <jv>entry</jv> = <jk>new</jk> Entry(...)
 * 		.setCategories(<jv>cat1</jv>, <jv>cat2</jv>);
 * </p>
 *
 * <h5 class='section'>Specification:</h5>
 * <p>
 * Represents an <c>atomCategory</c> construct in the 
 * <a class="doclink" href="https://tools.ietf.org/html/rfc4287#section-4.2.2">RFC 4287 - Section 4.2.2</a> specification.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanAtom">juneau-bean-atom</a>
 * 	<li class='extlink'><a class="doclink" href="https://tools.ietf.org/html/rfc4287">RFC 4287 - The Atom Syndication Format</a>
 * </ul>
 */
@Bean(typeName="category")
public class Category extends Common {

	private String term;
	private URI scheme;
	private String label;

	/**
	 * Normal constructor.
	 *
	 * @param term The category term.
	 */
	public Category(String term) {
		setTerm(term);
	}

	/** Bean constructor. */
	public Category() {}


	//-----------------------------------------------------------------------------------------------------------------
	// Bean properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>term</property>.
	 *
	 * <p>
	 * The category term.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format=ATTR)
	public String getTerm() {
		return term;
	}

	/**
	 * Bean property setter:  <property>term</property>.
	 *
	 * <p>
	 * The category term.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	@Xml(format=ATTR)
	public Category setTerm(String value) {
		this.term = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>scheme</property>.
	 *
	 * <p>
	 * The category scheme.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format=ATTR)
	public URI getScheme() {
		return scheme;
	}

	/**
	 * Bean property setter:  <property>scheme</property>.
	 *
	 * <p>
	 * The category scheme.
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Category setScheme(Object value) {
		this.scheme = toURI(value);
		return this;
	}

	/**
	 * Bean property getter:  <property>label</property>.
	 *
	 * <p>
	 * The category label.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format=ATTR)
	public String getLabel() {
		return label;
	}

	/**
	 * Bean property setter:  <property>scheme</property>.
	 *
	 * <p>
	 * The category label.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Category setLabel(String value) {
		this.label = value;
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Overridden from Common */
	public Category setBase(Object value) {
		super.setBase(value);
		return this;
	}

	@Override /* Overridden from Common */
	public Category setLang(String value) {
		super.setLang(value);
		return this;
	}
}