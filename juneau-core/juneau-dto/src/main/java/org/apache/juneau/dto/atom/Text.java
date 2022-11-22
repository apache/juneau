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

import static org.apache.juneau.xml.annotation.XmlFormat.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Represents an <c>atomTextConstruct</c> construct in the RFC4287 specification.
 *
 * <h5 class='figure'>Schema</h5>
 * <p class='bschema'>
 * 	atomTextConstruct = atomPlainTextConstruct | atomXHTMLTextConstruct
 *
 * 	atomPlainTextConstruct =
 * 		atomCommonAttributes,
 * 		attribute type { "text" | "html" }?,
 * 		text
 *
 * 	atomXHTMLTextConstruct =
 * 		atomCommonAttributes,
 * 		attribute type { "xhtml" },
 * 		xhtmlDiv
 *
 * 	xhtmlDiv = element xhtml:div {
 * 		(attribute * { text }
 * 		| text
 * 		| anyXHTML)*
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jd.Atom">Overview &gt; juneau-dto &gt; Atom</a>
 * </ul>
 */
@FluentSetters
public class Text extends Common {

	private String type;
	private String text;

	/**
	 * Normal content.
	 *
	 * @param type The content type of this content.
	 */
	public Text(String type) {
		setType(type);
	}

	/** Bean constructor. */
	public Text() {}


	//-----------------------------------------------------------------------------------------------------------------
	// Bean properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>type</property>.
	 *
	 * <p>
	 * The content type of this content.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format=ATTR)
	public String getType() {
		return type;
	}

	/**
	 * Bean property setter:  <property>type</property>.
	 *
	 * <p>
	 * The content type of this content.
	 *
	 * <p>
	 * Must be one of the following:
	 * <ul>
	 * 	<li><js>"text"</js>
	 * 	<li><js>"html"</js>
	 * 	<li><js>"xhtml"</js>
	 * 	<li><jk>null</jk> (defaults to <js>"text"</js>)
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Text setType(String value) {
		this.type = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>text</property>.
	 *
	 * <p>
	 * The content of this content.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format=XMLTEXT)
	public String getText() {
		return text;
	}

	/**
	 * Bean property setter:  <property>text</property>.
	 *
	 * <p>
	 * The content of this content.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Text setText(String value) {
		this.text = value;
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.dto.atom.Common */
	public Text setBase(Object value) {
		super.setBase(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.atom.Common */
	public Text setLang(String value) {
		super.setLang(value);
		return this;
	}

	// </FluentSetters>
}
