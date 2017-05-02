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
package org.apache.juneau.http;

import java.util.concurrent.*;

/**
 * Represents a parsed <code>Content-Type:</code> HTTP header.
 * <p>
 * The formal RFC2616 header field definition is as follows:
 * <p class='bcode'>
 * 	14.17 Content-Type
 *
 * 	The Content-Type entity-header field indicates the media type of the
 * 	entity-body sent to the recipient or, in the case of the HEAD method,
 * 	the media type that would have been sent had the request been a GET.
 *
 * 		Content-Type   = "Content-Type" ":" media-type
 *
 * 	Media types are defined in section 3.7. An example of the field is
 *
 * 	Content-Type: text/html; charset=ISO-8859-4
 * </p>
 */
public class ContentType extends MediaType {

	private static final boolean nocache = Boolean.getBoolean("juneau.http.ContentType.nocache");
	private static final ConcurrentHashMap<String,ContentType> cache = new ConcurrentHashMap<String,ContentType>();

	/**
	 * Returns a parsed <code>Content-Type</code> header.
	 *
	 * @param s The <code>Content-Type</code> header string.
	 * @return The parsed <code>Content-Type</code> header, or <jk>null</jk> if the string was null.
	 */
	public static ContentType forString(String s) {
		if (s == null)
			return null;

		// Prevent OOM in case of DDOS
		if (cache.size() > 1000)
			cache.clear();

		while (true) {
			ContentType mt = cache.get(s);
			if (mt != null)
				return mt;
			mt = new ContentType(s);
			if (nocache)
				return mt;
			cache.putIfAbsent(s, mt);
		}
	}

	private ContentType(String s) {
		super(s);
	}

	/**
	 * Given a list of media types, returns the best match for this <code>Content-Type</code> header.
	 * <p>
	 * Note that fuzzy matching is allowed on the media types where the <code>Content-Types</code> header may
	 * contain additional subtype parts.
	 * <br>For example, given a <code>Content-Type</code> value of <js>"text/json+activity"</js>,
	 * the media type <js>"text/json"</js> will match if <js>"text/json+activity"</js> or <js>"text/activity+json"</js>
	 * isn't found.
	 * <br>The purpose for this is to allow parsers to match when artifacts such as <code>id</code> properties are present
	 * in the header.
	 *
	 * @param mediaTypes The media types to match against.
	 * @return The index into the array of the best match, or <code>-1</code> if no suitable matches could be found.
	 */
	public int findMatch(MediaType[] mediaTypes) {
		int matchQuant = 0, matchIndex = -1;

		for (int i = 0; i < mediaTypes.length; i++) {
			MediaType mt = mediaTypes[i];
			int matchQuant2 = mt.match(this);
			if (matchQuant2 > matchQuant) {
				matchIndex = i;
			}
		}

		return matchIndex;
	}
}
