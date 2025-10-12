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

import static org.apache.juneau.xml.annotation.XmlFormat.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Represents a permanent, universally unique identifier for a feed or entry.
 *
 * <p>
 * The ID element provides a permanent, globally unique identifier for feeds and entries. 
 * IDs must never change, even if the resource is moved or its content is modified.
 *
 * <p>
 * Requirements for IDs per RFC 4287:
 * <ul class='spaced-list'>
 * 	<li>Must be a valid IRI (Internationalized Resource Identifier)
 * 	<li>Must be permanent - never changes
 * 	<li>Must be unique - no two feeds or entries should share the same ID
 * 	<li>Should be dereferenceable when possible (but not required)
 * </ul>
 *
 * <p>
 * Common ID formats:
 * <ul class='spaced-list'>
 * 	<li><b>Tag URI</b> - <c>tag:example.org,2024:entry-123</c>
 * 	<li><b>HTTP URL</b> - <c>http://example.org/posts/123</c>
 * 	<li><b>UUID URN</b> - <c>urn:uuid:1225c695-cfb8-4ebb-aaaa-80da344efa6a</c>
 * </ul>
 *
 * <h5 class='figure'>Schema</h5>
 * <p class='bschema'>
 * 	atomId = element atom:id {
 * 		atomCommonAttributes,
 * 		(atomUri)
 * 	}
 * </p>
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Tag URI (recommended)</jc>
 * 	Id <jv>id1</jv> = <jk>new</jk> Id(<js>"tag:example.org,2024:feed"</js>);
 *
 * 	<jc>// HTTP URL</jc>
 * 	Id <jv>id2</jv> = <jk>new</jk> Id(<js>"http://example.org/posts/123"</js>);
 *
 * 	<jc>// UUID URN</jc>
 * 	Id <jv>id3</jv> = <jk>new</jk> Id(<js>"urn:uuid:1225c695-cfb8-4ebb-aaaa-80da344efa6a"</js>);
 * </p>
 *
 * <h5 class='section'>Specification:</h5>
 * <p>
 * Represents an <c>atomId</c> construct in the 
 * <a class="doclink" href="https://tools.ietf.org/html/rfc4287#section-4.2.6">RFC 4287 - Section 4.2.6</a> specification.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanAtom">juneau-bean-atom</a>
 * 	<li class='extlink'><a class="doclink" href="https://tools.ietf.org/html/rfc4287">RFC 4287 - The Atom Syndication Format</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.ietf.org/rfc/rfc4151.txt">RFC 4151 - Tag URI Scheme</a>
 * </ul>
 */
@Bean(typeName="id")
public class Id extends Common {

	private String text;

	/**
	 * Normal constructor.
	 *
	 * @param text The id element contents.
	 */
	public Id(String text) {
		setText(text);
	}

	/** Bean constructor. */
	public Id() {}


	//-----------------------------------------------------------------------------------------------------------------
	// Bean properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>text</property>.
	 *
	 * <p>
	 * The content of this identifier.
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
	 * The content of this identifier.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Id setText(String value) {
		this.text = value;
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Overridden from Common */
	public Id setBase(Object value) {
		super.setBase(value);
		return this;
	}

	@Override /* Overridden from Common */
	public Id setLang(String value) {
		super.setLang(value);
		return this;
	}
}