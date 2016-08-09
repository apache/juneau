/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.dto.atom;

import static org.apache.juneau.xml.annotation.XmlFormat.*;

import java.net.*;

import org.apache.juneau.xml.annotation.*;

/**
 * Represents an <code>atomContent</code> construct in the RFC4287 specification.
 * <p>
 *
 * <h6 class='figure'>Schema</h6>
 * <p class='bcode'>
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
 * <p>
 * 	Refer to {@link org.apache.juneau.dto.atom} for further information about ATOM support.
 * </p>
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class Content extends Text {

	private URI src;


	/**
	 * Normal content.
	 *
	 * @param type The content type of this content.
	 * @param content The content of this content.
	 */
	public Content(String type, String content) {
		super(type, content);
	}

	/**
	 * Normal content.
	 *
	 * @param content The content of this content.
	 */
	public Content(String content) {
		super(content);
	}

	/** Bean constructor. */
	public Content() {}


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
	 * @param src The source URI.
	 * @return This object (for method chaining).
	 */
	public Content setSrc(URI src) {
		this.src = src;
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//--------------------------------------------------------------------------------

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

	@Override /* Common */
	public Content setBase(URI base) {
		super.setBase(base);
		return this;
	}

	@Override /* Common */
	public Content setLang(String lang) {
		super.setLang(lang);
		return this;
	}
}