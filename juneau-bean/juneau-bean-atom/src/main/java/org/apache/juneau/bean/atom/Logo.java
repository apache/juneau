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

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Represents a larger logo image for visual identification of a feed.
 *
 * <p>
 * The logo element contains a URI reference to an image that provides visual identification 
 * for the feed. Logos are typically larger than icons and are suitable for display in feed 
 * readers, aggregators, and feed directories.
 *
 * <p>
 * Per RFC 4287 recommendations:
 * <ul class='spaced-list'>
 * 	<li>Should have a 2:1 aspect ratio (twice as wide as tall)
 * 	<li>Common formats: PNG, JPEG, GIF, SVG
 * 	<li>Suitable for prominent display in feed readers
 * </ul>
 *
 * <h5 class='figure'>Schema</h5>
 * <p class='bschema'>
 * 	atomLogo = element atom:logo {
 * 		atomCommonAttributes,
 * 		(atomUri)
 * 	}
 * </p>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	Logo <jv>logo</jv> = <jk>new</jk> Logo(<js>"http://example.org/logo.png"</js>);
 *
 * 	Feed <jv>feed</jv> = <jk>new</jk> Feed(...)
 * 		.setLogo(<jv>logo</jv>);
 * </p>
 *
 * <h5 class='section'>Specification:</h5>
 * <p>
 * Represents an <c>atomLogo</c> construct in the 
 * <a class="doclink" href="https://tools.ietf.org/html/rfc4287#section-4.2.8">RFC 4287 - Section 4.2.8</a> specification.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanAtom">juneau-bean-atom</a>
 * 	<li class='extlink'><a class="doclink" href="https://tools.ietf.org/html/rfc4287">RFC 4287 - The Atom Syndication Format</a>
 * </ul>
 */
@Bean(typeName="logo")
public class Logo extends Common {

	private URI uri;


	/**
	 * Normal constructor.
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * <br>Strings must be valid URIs.
	 *
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 *
	 * @param uri The URI of the logo.
	 */
	public Logo(Object uri) {
		setUri(uri);
	}

	/** Bean constructor. */
	public Logo() {}


	//-----------------------------------------------------------------------------------------------------------------
	// Bean properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>uri</property>.
	 *
	 * <p>
	 * The URI of the logo.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format=TEXT)
	public URI getUri() {
		return uri;
	}

	/**
	 * Bean property setter:  <property>uri</property>.
	 *
	 * <p>
	 * The URI of the logo.
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * <br>Strings must be valid URIs.
	 *
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Logo setUri(Object value) {
		this.uri = toURI(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Overridden from Common */
	public Logo setBase(Object value) {
		super.setBase(value);
		return this;
	}

	@Override /* Overridden from Common */
	public Logo setLang(String value) {
		super.setLang(value);
		return this;
	}
}