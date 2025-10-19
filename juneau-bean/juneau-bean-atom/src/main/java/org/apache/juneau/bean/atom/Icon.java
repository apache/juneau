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
 * Represents a small icon image for visual identification of a feed.
 *
 * <p>
 * The icon element contains a URI reference to a small image that provides iconic visual
 * identification for the feed. Icons are typically small, square images suitable for display
 * in feed readers and aggregators.
 *
 * <p>
 * Per RFC 4287 recommendations:
 * <ul class='spaced-list'>
 * 	<li>Should be square (aspect ratio of 1:1)
 * 	<li>Should be small (e.g., 16x16, 32x32 pixels)
 * 	<li>Common formats: PNG, ICO, GIF
 * </ul>
 *
 * <h5 class='figure'>Schema</h5>
 * <p class='bschema'>
 * 	atomIcon = element atom:icon {
 * 		atomCommonAttributes,
 * 		(atomUri)
 * 	}
 * </p>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	Icon <jv>icon</jv> = <jk>new</jk> Icon(<js>"http://example.org/icon.png"</js>);
 *
 * 	Feed <jv>feed</jv> = <jk>new</jk> Feed(...)
 * 		.setIcon(<jv>icon</jv>);
 * </p>
 *
 * <h5 class='section'>Specification:</h5>
 * <p>
 * Represents an <c>atomIcon</c> construct in the
 * <a class="doclink" href="https://tools.ietf.org/html/rfc4287#section-4.2.5">RFC 4287 - Section 4.2.5</a> specification.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanAtom">juneau-bean-atom</a>
 * 	<li class='extlink'><a class="doclink" href="https://tools.ietf.org/html/rfc4287">RFC 4287 - The Atom Syndication Format</a>
 * </ul>
 */
@Bean(typeName = "icon")
public class Icon extends Common {

	private URI uri;

	/** Bean constructor. */
	public Icon() {}

	/**
	 * Normal constructor.
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 *
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 *
	 * @param uri The URI of the icon.
	 */
	public Icon(Object uri) {
		setUri(uri);
	}

	/**
	 * Bean property getter:  <property>uri</property>.
	 *
	 * <p>
	 * The URI of this icon.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format = TEXT)
	public URI getUri() { return uri; }

	@Override /* Overridden from Common */
	public Icon setBase(Object value) {
		super.setBase(value);
		return this;
	}

	@Override /* Overridden from Common */
	public Icon setLang(String value) {
		super.setLang(value);
		return this;
	}

	/**
	 * Bean property setter:  <property>uri</property>.
	 *
	 * <p>
	 * The URI of this icon.
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
	public Icon setUri(Object value) {
		this.uri = toURI(value);
		return this;
	}
}