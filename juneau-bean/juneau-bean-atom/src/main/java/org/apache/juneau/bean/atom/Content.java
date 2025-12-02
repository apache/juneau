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
import static org.apache.juneau.xml.annotation.XmlFormat.*;

import java.net.*;

import org.apache.juneau.xml.annotation.*;

/**
 * Represents the content of an Atom entry.
 *
 * <p>
 * The content element contains or links to the complete content of an entry. It supports multiple
 * content types and delivery methods:
 *
 * <ul class='spaced-list'>
 * 	<li><b>Inline text content</b> - Plain text or HTML content embedded in the feed
 * 	<li><b>Inline XHTML content</b> - Well-formed XHTML embedded in the feed
 * 	<li><b>Inline other content</b> - Other media types (XML, base64-encoded binary, etc.)
 * 	<li><b>Out-of-line content</b> - Link to external content via the <c>src</c> attribute
 * </ul>
 *
 * <p>
 * The <c>type</c> attribute indicates the media type of the content. Common values:
 * <ul class='spaced-list'>
 * 	<li><c>"text"</c> - Plain text (default)
 * 	<li><c>"html"</c> - HTML, entity-escaped
 * 	<li><c>"xhtml"</c> - XHTML wrapped in a div element
 * 	<li>Other MIME types - For multimedia content or base64-encoded data
 * </ul>
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
 * <h5 class='section'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Plain text content</jc>
 * 	Content <jv>c1</jv> = <jk>new</jk> Content(<js>"text"</js>)
 * 		.setText(<js>"This is plain text content"</js>);
 *
 * 	<jc>// HTML content</jc>
 * 	Content <jv>c2</jv> = <jk>new</jk> Content(<js>"html"</js>)
 * 		.setText(<js>"&lt;p&gt;This is &lt;strong&gt;HTML&lt;/strong&gt; content&lt;/p&gt;"</js>);
 *
 * 	<jc>// XHTML content</jc>
 * 	Content <jv>c3</jv> = <jk>new</jk> Content(<js>"xhtml"</js>)
 * 		.setText(<js>"&lt;div xmlns='http://www.w3.org/1999/xhtml'&gt;&lt;p&gt;XHTML content&lt;/p&gt;&lt;/div&gt;"</js>);
 *
 * 	<jc>// External content (out-of-line)</jc>
 * 	Content <jv>c4</jv> = <jk>new</jk> Content()
 * 		.setType(<js>"video/mp4"</js>)
 * 		.setSrc(<js>"http://example.org/movie.mp4"</js>);
 * </p>
 *
 * <h5 class='section'>Specification:</h5>
 * <p>
 * Represents an <c>atomContent</c> construct in the
 * <a class="doclink" href="https://tools.ietf.org/html/rfc4287#section-4.1.3">RFC 4287 - Section 4.1.3</a> specification.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanAtom">juneau-bean-atom</a>
 * 	<li class='extlink'><a class="doclink" href="https://tools.ietf.org/html/rfc4287">RFC 4287 - The Atom Syndication Format</a>
 * </ul>
 */
public class Content extends Text {

	private URI src;

	/**
	 * Normal content.
	 */
	public Content() {}

	/**
	 * Normal content.
	 *
	 * @param type The content type of this content.
	 */
	public Content(String type) {
		super(type);
	}

	/**
	 * Bean property getter:  <property>src</property>.
	 *
	 * <p>
	 * Returns the URI of externally-hosted content (out-of-line content).
	 *
	 * <p>
	 * When <c>src</c> is present, the content is not embedded in the feed but is instead
	 * referenced by URI. This is useful for large media files or content hosted elsewhere.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format = ATTR)
	public URI getSrc() { return src; }

	@Override /* Overridden from Common */
	public Content setBase(Object value) {
		super.setBase(value);
		return this;
	}

	@Override /* Overridden from Common */
	public Content setLang(String value) {
		super.setLang(value);
		return this;
	}

	/**
	 * Bean property setter:  <property>src</property>.
	 *
	 * <p>
	 * Sets the URI of externally-hosted content (out-of-line content).
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Link to external video</jc>
	 * 	Content <jv>content</jv> = <jk>new</jk> Content()
	 * 		.setType(<js>"video/mp4"</js>)
	 * 		.setSrc(<js>"http://example.org/videos/intro.mp4"</js>);
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Content setSrc(Object value) {
		this.src = toURI(value);
		return this;
	}

	@Override /* Overridden from Text */
	public Content setText(String value) {
		super.setText(value);
		return this;
	}

	@Override /* Overridden from Text */
	public Content setType(String value) {
		super.setType(value);
		return this;
	}
}