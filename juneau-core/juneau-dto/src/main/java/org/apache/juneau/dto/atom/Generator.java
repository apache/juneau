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

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.xml.annotation.XmlFormat.*;

import java.net.*;
import java.net.URI;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Represents an <c>atomGenerator</c> construct in the RFC4287 specification.
 *
 * <h5 class='figure'>Schema</h5>
 * <p class='bcode w800'>
 * 	atomGenerator = element atom:generator {
 * 		atomCommonAttributes,
 * 		attribute uri { atomUri }?,
 * 		attribute version { text }?,
 * 		text
 * 	}
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc DtoAtom}
 * 	<li class='jp'>{@doc package-summary.html#TOC}
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
	 */
	public void setUri(Object value) {
		this.uri = toURI(value);
	}

	/**
	 * Bean property fluent getter:  <property>uri</property>.
	 *
	 * <p>
	 * The URI of this generator statement.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<URI> uri() {
		return Optional.ofNullable(uri);
	}

	/**
	 * Bean property fluent setter:  <property>uri</property>.
	 *
	 * <p>
	 * The URI of this generator statement.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Generator uri(Object value) {
		setUri(value);
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
	 */
	public void setVersion(String value) {
		this.version = value;
	}

	/**
	 * Bean property fluent getter:  <property>version</property>.
	 *
	 * <p>
	 * The version of this generator statement.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> version() {
		return Optional.ofNullable(version);
	}

	/**
	 * Bean property fluent setter:  <property>version</property>.
	 *
	 * <p>
	 * The version of this generator statement.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Generator version(String value) {
		setVersion(value);
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
	 */
	public void setText(String value) {
		this.text = value;
	}

	/**
	 * Bean property fluent getter:  <property>text</property>.
	 *
	 * <p>
	 * The content of this generator statement.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> text() {
		return Optional.ofNullable(text);
	}

	/**
	 * Bean property fluent setter:  <property>text</property>.
	 *
	 * <p>
	 * The content of this generator statement.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Generator text(String value) {
		setText(value);
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Common */
	public Generator base(Object base) {
		super.base(base);
		return this;
	}

	@Override /* Common */
	public Generator lang(String lang) {
		super.lang(lang);
		return this;
	}
}
