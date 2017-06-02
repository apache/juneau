// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.dto.atom;

import static org.apache.juneau.dto.atom.Utils.*;
import static org.apache.juneau.xml.annotation.XmlFormat.*;

import java.net.URI;

import org.apache.juneau.annotation.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Represents an <code>atomGenerator</code> construct in the RFC4287 specification.
 * <p>
 * <h6 class='figure'>Schema</h6>
 * <p class='bcode'>
 * 	atomGenerator = element atom:generator {
 * 		atomCommonAttributes,
 * 		attribute uri { atomUri }?,
 * 		attribute version { text }?,
 * 		text
 * 	}
 * </p>
 *
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../../overview-summary.html#DTOs'>Juneau Data Transfer Objects (org.apache.juneau.dto)</a>
 * 	<ul>
 * 		<li class='sublink'><a class='doclink' href='../../../../../overview-summary.html#DTOs.Atom'>Atom</a>
 * 	</ul>
 * 	<li class='jp'><a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.dto.atom</a>
 * </ul>
 */
@Bean(typeName="generator")
@SuppressWarnings("hiding")
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


	//--------------------------------------------------------------------------------
	// Bean properties
	//--------------------------------------------------------------------------------

	/**
	 * Returns the URI of this generator statement.
	 *
	 * @return The URI of this generator statement.
	 */
	@Xml(format=ATTR)
	public URI getUri() {
		return uri;
	}

	/**
	 * Sets the URI of this generator statement.
	 *
	 * @param uri The URI of this generator statement.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("uri")
	public Generator uri(URI uri) {
		this.uri = uri;
		return this;
	}

	/**
	 * Sets the URI of this generator statement.
	 *
	 * @param uri The URI of this generator statement.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("uri")
	public Generator uri(String uri) {
		this.uri = toURI(uri);
		return this;
	}

	/**
	 * Returns the version of this generator statement.
	 *
	 * @return The version of this generator statement.
	 */
	@Xml(format=ATTR)
	public String getVersion() {
		return version;
	}

	/**
	 * Sets the version of this generator statement.
	 *
	 * @param version The version of this generator statement.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("version")
	public Generator version(String version) {
		this.version = version;
		return this;
	}

	/**
	 * Returns the content of this generator statement.
	 *
	 * @return The content of this generator statement.
	 */
	@Xml(format=TEXT)
	public String getText() {
		return text;
	}

	/**
	 * Sets the content of this generator statement.
	 *
	 * @param text The content of this generator statement.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("text")
	public Generator text(String text) {
		this.text = text;
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//--------------------------------------------------------------------------------

	@Override /* Common */
	public Generator base(URI base) {
		super.base(base);
		return this;
	}

	@Override /* Common */
	public Generator lang(String lang) {
		super.lang(lang);
		return this;
	}
}
