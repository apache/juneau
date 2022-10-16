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

import org.apache.juneau.internal.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Represents an <c>atomContent</c> construct in the RFC4287 specification.
 *
 * <h5 class='figure'>Schema</h5>
 * <p class='bschema'>
 * 	atomContent = atomInlineTextContent
 * 		| atomInlineXHTMLContent
 * 		| atomInlineOtherContent
 * 		| atomOutOfLineContent
 *
 * 	atomInlineTextContent =
 * 		element atom:content {
 * 			atomCommonAttributes,
 * 			attribute type { "text" | "html" }?,
 * 			(text)*
 * 		}
 *
 * 	atomInlineXHTMLContent =
 * 		element atom:content {
 * 			atomCommonAttributes,
 * 			attribute type { "xhtml" },
 * 			xhtmlDiv
 * 		}
 *
 * 	atomInlineOtherContent =
 * 		element atom:content {
 * 			atomCommonAttributes,
 * 			attribute type { atomMediaType }?,
 * 			(text|anyElement)*
 * 	}
 *
 * 	atomOutOfLineContent =
 * 		element atom:content {
 * 			atomCommonAttributes,
 * 			attribute type { atomMediaType }?,
 * 			attribute src { atomUri },
 * 			empty
 * 	}
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jd.Atom}
 * 	<li class='jp'>{@doc package-summary.html#TOC}
 * </ul>
 */
@FluentSetters
public class Content extends Text {

	private URI src;


	/**
	 * Normal content.
	 *
	 * @param type The content type of this content.
	 */
	public Content(String type) {
		super(type);
	}

	/**
	 * Normal content.
	 */
	public Content() {
		super();
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Bean properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>src</property>.
	 *
	 * <p>
	 * The source URI.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format=ATTR)
	public URI getSrc() {
		return src;
	}

	/**
	 * Bean property setter:  <property>src</property>.
	 *
	 * <p>
	 * The source URI.
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
	public Content setSrc(Object value) {
		this.src = toURI(value);
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Text */
	public Content setText(String text) {
		super.setText(text);
		return this;
	}

	@Override /* Text */
	public Content setType(String type) {
		super.setType(type);
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.dto.atom.Common */
	public Content setBase(Object value) {
		super.setBase(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.atom.Common */
	public Content setLang(String value) {
		super.setLang(value);
		return this;
	}

	// </FluentSetters>
}