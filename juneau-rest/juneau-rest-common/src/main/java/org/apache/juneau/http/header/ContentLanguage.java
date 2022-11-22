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
package org.apache.juneau.http.header;

import java.util.function.*;

import org.apache.juneau.http.annotation.*;

/**
 * Represents a parsed <l>Content-Language</l> HTTP response header.
 *
 * <p>
 * The natural language or languages of the intended audience for the enclosed content.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	Content-Language: da
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The Content-Language entity-header field describes the natural language(s) of the intended audience for the
 * enclosed entity.
 * Note that this might not be equivalent to all the languages used within the entity-body.
 * <p class='bcode'>
 * 	Content-Language  = "Content-Language" ":" 1#language-tag
 * </p>
 *
 * <p>
 * Language tags are defined in section 3.10.
 * The primary purpose of Content-Language is to allow a user to identify and differentiate entities according to the
 * user's own preferred language.
 * Thus, if the body content is intended only for a Danish-literate audience, the appropriate field is...
 * <p class='bcode'>
 * 	Content-Language: da
 * </p>
 *
 * <p>
 * If no Content-Language is specified, the default is that the content is intended for all language audiences.
 * This might mean that the sender does not consider it to be specific to any natural language, or that the sender
 * does not know for which language it is intended.
 *
 * <p>
 * Multiple languages MAY be listed for content that is intended for multiple audiences.
 * For example, a rendition of the "Treaty of Waitangi," presented simultaneously in the original Maori and English
 * versions, would call for...
 * <p class='bcode'>
 * 	Content-Language: mi, en
 * </p>
 *
 * <p>
 * However, just because multiple languages are present within an entity does not mean that it is intended for
 * multiple linguistic audiences.
 * An example would be a beginner's language primer, such as "A First Lesson in Latin," which is clearly intended to
 * be used by an English-literate audience.
 * In this case, the Content-Language would properly only include "en".
 *
 * <p>
 * Content-Language MAY be applied to any media type -- it is not limited to textual documents.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
@Header("Content-Language")
public class ContentLanguage extends BasicCsvHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;
	private static final String NAME = "Content-Language";

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static ContentLanguage of(String value) {
		return value == null ? null : new ContentLanguage(value);
	}

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static ContentLanguage of(String...value) {
		return value == null ? null : new ContentLanguage(value);
	}

	/**
	 * Static creator with delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static ContentLanguage of(Supplier<String[]> value) {
		return value == null ? null : new ContentLanguage(value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public ContentLanguage(String value) {
		super(NAME, value);
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public ContentLanguage(String...value) {
		super(NAME, value);
	}

	/**
	 * Constructor with delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public ContentLanguage(Supplier<String[]> value) {
		super(NAME, value);
	}
}
