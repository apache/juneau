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
package org.apache.juneau.bean.html5;

import java.net.*;

import org.apache.juneau.marshall.*;

/**
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/document-metadata.html#the-base-element">&lt;base&gt;</a>
 * element.
 *
 * <p>
 * The base element specifies the base URL for all relative URLs in a document. It also specifies
 * the default target for all links and forms in the document. Only one base element is allowed
 * per document and it must be placed in the head section.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Set base URL for all relative links</jc>
 * 	Base <jv>base1</jv> = <jsm>base</jsm>().href(<js>"https://example.com/docs/"</js>);
 *
 * 	<jc>// Set default target for all links</jc>
 * 	Base <jv>base2</jv> = <jsm>base</jsm>().target(<js>"_blank"</js>);
 *
 * 	<jc>// Set both base URL and default target</jc>
 * 	Base <jv>base3</jv> = <jsm>base</jsm>().href(<js>"https://example.com/"</js>).target(<js>"_self"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#base() base()}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "base")
public class Base extends HtmlElementVoid<Base> {

	/**
	 * Creates an empty {@link Base} element.
	 */
	public Base() {}

	/**
	 * Creates a {@link Base} element with the specified {@link Base#href(Object)} attribute.
	 *
	 * @param value The {@link Base#href(Object)} attribute. Can be <jk>null</jk>.
	 */
	public Base(Object value) {
		href(value);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/document-metadata.html#attr-base-href">href</a> attribute.
	 *
	 * <p>
	 * Document base URL.
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 *
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link URL} or {@link String}.
	 * 	Can be <jk>null</jk>.
	 * @return This object.
	 */
	public Base href(Object value) {
		attrUri("href", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/document-metadata.html#attr-base-target">target</a>
	 * attribute.
	 *
	 * <p>
	 * Specifies the default target for all links and forms in the document.
	 *
	 * <p>
	 * Common values:
	 * <ul>
	 * 	<li><js>"_blank"</js> - Open in a new window/tab</li>
	 * 	<li><js>"_self"</js> - Open in the same frame (default)</li>
	 * 	<li><js>"_parent"</js> - Open in the parent frame</li>
	 * 	<li><js>"_top"</js> - Open in the full body of the window</li>
	 * 	<li><js>"framename"</js> - Open in a named frame</li>
	 * </ul>
	 *
	 * @param value The default target for links and forms. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Base target(String value) {
		attr("target", value);
		return this;
	}

}