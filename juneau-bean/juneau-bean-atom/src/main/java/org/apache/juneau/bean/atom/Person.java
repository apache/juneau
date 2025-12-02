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

import static org.apache.juneau.commons.utils.StringUtils.*;

import java.net.*;

/**
 * Represents a person, corporation, or similar entity in an Atom document.
 *
 * <p>
 * Person constructs are used to describe authors, contributors, and other people or entities
 * associated with a feed or entry. Each person construct contains a name and optionally a URI
 * and email address.
 *
 * <p>
 * Person constructs appear in several places:
 * <ul class='spaced-list'>
 * 	<li><c>atom:author</c> - Indicates the author(s) of a feed or entry
 * 	<li><c>atom:contributor</c> - Indicates those who contributed to a feed or entry
 * </ul>
 *
 * <h5 class='figure'>Schema</h5>
 * <p class='bschema'>
 * 	atomPersonConstruct =
 * 		atomCommonAttributes,
 * 		(element atom:name { text }
 * 		&amp; element atom:uri { atomUri }?
 * 		&amp; element atom:email { atomEmailAddress }?
 * 		&amp; extensionElement*)
 * </p>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create an author</jc>
 * 	Person <jv>author</jv> = <jk>new</jk> Person(<js>"Jane Doe"</js>)
 * 		.setEmail(<js>"jane@example.org"</js>)
 * 		.setUri(<js>"http://example.org/~jane"</js>);
 *
 * 	<jc>// Add to entry</jc>
 * 	Entry <jv>entry</jv> = <jk>new</jk> Entry(...)
 * 		.setAuthors(<jv>author</jv>);
 * </p>
 *
 * <h5 class='section'>Specification:</h5>
 * <p>
 * Represents an <c>atomPersonConstruct</c> in the
 * <a class="doclink" href="https://tools.ietf.org/html/rfc4287#section-3.2">RFC 4287 - Section 3.2</a> specification.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanAtom">juneau-bean-atom</a>
 * 	<li class='extlink'><a class="doclink" href="https://tools.ietf.org/html/rfc4287">RFC 4287 - The Atom Syndication Format</a>
 * </ul>
 */
public class Person extends Common {

	private String name;
	private URI uri;
	private String email;

	/** Bean constructor. */
	public Person() {}

	/**
	 * Normal constructor.
	 *
	 * @param name The name of the person.
	 */
	public Person(String name) {
		setName(name);
	}

	/**
	 * Bean property getter:  <property>email</property>.
	 *
	 * <p>
	 * Returns the email address associated with the person.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getEmail() { return email; }

	/**
	 * Bean property getter:  <property>name</property>.
	 *
	 * <p>
	 * Returns the human-readable name for the person (required).
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getName() { return name; }

	/**
	 * Bean property getter:  <property>uri</property>.
	 *
	 * <p>
	 * Returns a URI associated with the person.
	 *
	 * <p>
	 * Typically this is a personal website or profile page.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public URI getUri() { return uri; }

	@Override /* Overridden from Common */
	public Person setBase(Object value) {
		super.setBase(value);
		return this;
	}

	/**
	 * Bean property setter:  <property>email</property>.
	 *
	 * <p>
	 * Sets the email address associated with the person.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Person <jv>person</jv> = <jk>new</jk> Person(<js>"Jane Doe"</js>)
	 * 		.setEmail(<js>"jane@example.org"</js>);
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Person setEmail(String value) {
		this.email = value;
		return this;
	}

	@Override /* Overridden from Common */
	public Person setLang(String value) {
		super.setLang(value);
		return this;
	}

	/**
	 * Bean property setter:  <property>name</property>.
	 *
	 * <p>
	 * Sets the human-readable name for the person (required).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Person <jv>person</jv> = <jk>new</jk> Person(<js>"Jane Doe"</js>);
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Person setName(String value) {
		this.name = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>uri</property>.
	 *
	 * <p>
	 * Sets a URI associated with the person (typically a website or profile page).
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Person <jv>person</jv> = <jk>new</jk> Person(<js>"Jane Doe"</js>)
	 * 		.setUri(<js>"http://example.org/~jane"</js>);
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Person setUri(Object value) {
		this.uri = toURI(value);
		return this;
	}
}