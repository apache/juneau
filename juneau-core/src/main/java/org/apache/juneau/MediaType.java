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
package org.apache.juneau;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.annotation.*;


/**
 * Describes a single media type used in content negotiation between an HTTP client and server, as described in
 * Section 14.1 and 14.7 of RFC2616 (the HTTP/1.1 specification).
 */
@BeanIgnore
public final class MediaType {

	private static final ConcurrentHashMap<String,MediaType> cache = new ConcurrentHashMap<String,MediaType>();

	/** Reusable predefined media type */
	@SuppressWarnings("javadoc")
	public static final MediaType
		CSV = forString("text/csv"),
		HTML = forString("text/html"),
		JSON = forString("application/json"),
		MSGPACK = forString("octal/msgpack"),
		PLAIN = forString("text/plain"),
		UON = forString("text/uon"),
		URLENCODING = forString("application/x-www-form-urlencoded"),
		XML = forString("text/xml"),
		XMLSOAP = forString("text/xml+soap"),

		RDF = forString("text/xml+rdf"),
		RDFABBREV = forString("text/xml+rdf+abbrev"),
		NTRIPLE = forString("text/n-triple"),
		TURTLE = forString("text/turtle"),
		N3 = forString("text/n3")
	;

	private final String mediaType;
	private final String type;								// The media type (e.g. "text" for Accept, "utf-8" for Accept-Charset)
	private final String subType;                   // The media sub-type (e.g. "json" for Accept, not used for Accept-Charset)

	/**
	 * Returns the media type for the specified string.
	 * The same media type strings always return the same objects so that these objects
	 * can be compared for equality using '=='.
	 * <p>
	 * Note:  Spaces are replaced with <js>'+'</js> characters.
	 * This gets around the issue where passing media type strings with <js>'+'</js> as HTTP GET parameters
	 * 	get replaced with spaces by your browser.  Since spaces aren't supported by the spec, this
	 * 	is doesn't break anything.
	 * <p>
	 * Anything including and following the <js>';'</js> character is ignored (e.g. <js>";charset=X"</js>).
	 *
	 * @param s - The media type string.  Will be lowercased.
	 * 	Returns <jk>null</jk> if input is null.
	 * @return A cached media type object.
	 */
	public static MediaType forString(String s) {
		if (s == null)
			return null;
		MediaType mt = cache.get(s);
		if (mt == null) {
			mt = new MediaType(s);
			cache.putIfAbsent(s, mt);
		}
		return cache.get(s);
	}

	private MediaType(String mt) {
		int i = mt.indexOf(';');
		if (i != -1)
			mt = mt.substring(0, i);

		mt = mt.toLowerCase(Locale.ENGLISH);
		this.mediaType = mt;
		String _type = null, _subType = null;
		if (mt != null) {
			mt = mt.replace(' ', '+');
			i = mt.indexOf('/');
			_type = (i == -1 ? mt : mt.substring(0, i));
			_subType = (i == -1 ? "*" : mt.substring(i+1));
		}
		this.type = _type;
		this.subType = _subType;
	}

	/**
	 * Returns the <js>'type'</js> fragment of the <js>'type/subType'</js> string.
	 *
	 * @return The media type.
	 */
	public String getType() {
		return type;
	}

	/**
	 * Returns the <js>'subType'</js> fragment of the <js>'type/subType'</js> string.
	 *
	 * @return The media subtype.
	 */
	public String getSubType() {
		return subType;
	}

	/**
	 * Returns <jk>true</jk> if this media type is a match for the specified media type.
	 * <p>
	 * Matches if any of the following is true:
	 * <ul>
	 * 	<li>Both type and subtype are the same.
	 * 	<li>One or both types are <js>'*'</js> and the subtypes are the same.
	 * 	<li>One or both subtypes are <js>'*'</js> and the types are the same.
	 * 	<li>Either is <js>'*\/*'</js>.
	 *
	 * @param o The media type to compare with.
	 * @return <jk>true</jk> if the media types match.
	 */
	public final boolean matches(MediaType o) {
		if (this == o)
			return true;

		if (type.equals(o.type) || (type.equals("*")) || (o.type.equals("*")))
			if (subType.equals(o.subType) || subType.equals("*") || o.subType.equals("*"))
				return true;

		return false;
	}

	@Override /* Object */
	public String toString() {
		return mediaType;
	}

	@Override /* Object */
	public int hashCode() {
		return mediaType.hashCode();
	}

	@Override /* Object */
	public boolean equals(Object o) {
		return this == o;
	}
}
