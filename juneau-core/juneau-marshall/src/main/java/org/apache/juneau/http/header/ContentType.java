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

import static org.apache.juneau.http.header.Constants.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.internal.*;

/**
 * Represents a parsed <l>Content-Type</l> HTTP request/response header.
 *
 * <p>
 * The MIME type of this content.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	Content-Type: text/html; charset=utf-8
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The Content-Type entity-header field indicates the media type of the entity-body sent to the recipient or, in the
 * case of the HEAD method, the media type that would have been sent had the request been a GET.
 * <p class='bcode w800'>
 * 	Content-Type   = "Content-Type" ":" media-type
 * </p>
 *
 * <p>
 * Media types are defined in section 3.7.
 * An example of the field is...
 * <p class='bcode w800'>
 * 	Content-Type: text/html; charset=ISO-8859-4
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc RFC2616}
 * </ul>
 */
@Header("Content-Type")
@BeanIgnore
public class ContentType extends BasicParameterizedHeader {

	private static final long serialVersionUID = 1L;

	private static Cache<String,ContentType> CACHE = new Cache<>(NOCACHE, CACHE_MAX_SIZE);

	/**
	 * Returns a parsed and cached <c>Content-Type</c> header.
	 *
	 * @param value The <c>Content-Type</c> header string.
	 * @return The parsed <c>Content-Type</c> header, or <jk>null</jk> if the string was null.
	 */
	public static ContentType of(String value) {
		if (value == null)
			return null;
		ContentType ct = CACHE.get(value);
		if (ct == null)
			ct = CACHE.put(value, new ContentType(value));
		return ct;
	}

	private final MediaType mediaType;

	/**
	 * Constructor.
	 *
	 * @param value The value for this header.
	 */
	public ContentType(String value) {
		super("Content-Type", value);
		this.mediaType = new MediaType(value);
	}

	/**
	 * Given a list of media types, returns the best match for this <c>Content-Type</c> header.
	 *
	 * <p>
	 * Note that fuzzy matching is allowed on the media types where the <c>Content-Types</c> header may
	 * contain additional subtype parts.
	 * <br>For example, given a <c>Content-Type</c> value of <js>"text/json+activity"</js>,
	 * the media type <js>"text/json"</js> will match if <js>"text/json+activity"</js> or <js>"text/activity+json"</js>
	 * isn't found.
	 * <br>The purpose for this is to allow parsers to match when artifacts such as <c>id</c> properties are
	 * present in the header.
	 *
	 * @param mediaTypes The media types to match against.
	 * @return The index into the array of the best match, or <c>-1</c> if no suitable matches could be found.
	 */
	public int findMatch(MediaType[] mediaTypes) {
		int matchQuant = 0, matchIndex = -1;

		for (int i = 0; i < mediaTypes.length; i++) {
			MediaType mt = mediaTypes[i];
			int matchQuant2 = mt.match(mediaType, true);
			if (matchQuant2 > matchQuant) {
				matchQuant = matchQuant2;
				matchIndex = i;
			}
		}
		return matchIndex;
	}

	/**
	 * Returns this header as a {@link MediaType} object.
	 *
	 * @return This header as a {@link MediaType} object.
	 */
	public MediaType asMediaType() {
		return mediaType;
	}
}
