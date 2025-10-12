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
 * Identifies the software agent used to generate an Atom feed.
 *
 * <p>
 * The generator element provides information about the software that created the feed. This is 
 * useful for debugging, analytics, and understanding the tools used in feed creation.
 *
 * <p>
 * The generator has three components:
 * <ul class='spaced-list'>
 * 	<li><b>Text content</b> (required) - Human-readable name of the generating agent
 * 	<li><b>uri attribute</b> (optional) - URI identifying or describing the generating agent
 * 	<li><b>version attribute</b> (optional) - Version of the generating agent
 * </ul>
 *
 * <h5 class='figure'>Schema</h5>
 * <p class='bschema'>
 * 	atomGenerator = element atom:generator {
 * 		atomCommonAttributes,
 * 		attribute uri { atomUri }?,
 * 		attribute version { text }?,
 * 		text
 * 	}
 * </p>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	Generator <jv>gen</jv> = <jk>new</jk> Generator(<js>"My Blog Software"</js>)
 * 		.setUri(<js>"http://www.myblogsoftware.com"</js>)
 * 		.setVersion(<js>"2.0"</js>);
 *
 * 	Feed <jv>feed</jv> = <jk>new</jk> Feed(...)
 * 		.setGenerator(<jv>gen</jv>);
 * </p>
 *
 * <h5 class='section'>Specification:</h5>
 * <p>
 * Represents an <c>atomGenerator</c> construct in the 
 * <a class="doclink" href="https://tools.ietf.org/html/rfc4287#section-4.2.4">RFC 4287 - Section 4.2.4</a> specification.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanAtom">juneau-bean-atom</a>
 * 	<li class='extlink'><a class="doclink" href="https://tools.ietf.org/html/rfc4287">RFC 4287 - The Atom Syndication Format</a>
 * </ul>
 */
@Bean(typeName="generator")
public class Generator extends Common {

	private URI uri;
	private String version;
	private String text;


	/**
	 * Normal constructor.
	 *
	 * @param text The generator statement content.
	 */
	public Generator(String text) {
		this.text = text;
	}

	/** Bean constructor. */
	public Generator() {}


	//-----------------------------------------------------------------------------------------------------------------
	// Bean properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>uri</property>.
	 *
	 * <p>
	 * The URI of this generator statement.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format=ATTR)
	public URI getUri() {
		return uri;
	}

	/**
	 * Bean property setter:  <property>uri</property>.
	 *
	 * <p>
	 * The URI of this generator statement.
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
	public Generator setUri(Object value) {
		this.uri = toURI(value);
		return this;
	}

	/**
	 * Bean property getter:  <property>version</property>.
	 *
	 * <p>
	 * The version of this generator statement.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format=ATTR)
	public String getVersion() {
		return version;
	}

	/**
	 * Bean property setter:  <property>version</property>.
	 *
	 * <p>
	 * The version of this generator statement.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Generator setVersion(String value) {
		this.version = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>text</property>.
	 *
	 * <p>
	 * The content of this generator statement.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format=TEXT)
	public String getText() {
		return text;
	}

	/**
	 * Bean property setter:  <property>text</property>.
	 *
	 * <p>
	 * The content of this generator statement.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Generator setText(String value) {
		this.text = value;
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Overridden from Common */
	public Generator setBase(Object value) {
		super.setBase(value);
		return this;
	}

	@Override /* Overridden from Common */
	public Generator setLang(String value) {
		super.setLang(value);
		return this;
	}
}