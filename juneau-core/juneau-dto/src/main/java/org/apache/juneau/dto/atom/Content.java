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

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Represents an <code>atomContent</code> construct in the RFC4287 specification.
 *
 * <h5 class='figure'>Schema</h5>
 * <p class='bcode w800'>
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
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'>{@doc juneau-dto.Atom}
 * 	<li class='jp'><a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.dto.atom</a>
 * </ul>
 */
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


	//--------------------------------------------------------------------------------
	// Bean properties
	//--------------------------------------------------------------------------------

	/**
	 * Returns the source URI.
	 *
	 * @return the source URI.
	 */
	@Xml(format=ATTR)
	public URI getSrc() {
		return src;
	}

	/**
	 * Sets the source URI.
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 *
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 *
	 * @param src The source URI.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("src")
	public Content src(Object src) {
		this.src = toURI(src);
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//--------------------------------------------------------------------------------

	@Override /* Text */
	public Content text(String text) {
		super.text(text);
		return this;
	}

	@Override /* Text */
	public Content type(String type) {
		super.type(type);
		return this;
	}

	@Override /* Common */
	public Content base(Object base) {
		super.base(base);
		return this;
	}
	@Override /* Common */
	public Content lang(String lang) {
		super.lang(lang);
		return this;
	}
}