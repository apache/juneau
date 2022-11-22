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

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;

import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.http.message.*;
import org.apache.juneau.annotation.*;

/**
 * Represents a single value in a comma-delimited header value that optionally contains a quality metric for
 * comparison and extension parameters.
 *
 * <p>
 * Similar in concept to {@link MediaRanges} except instead of media types (e.g. <js>"text/json"</js>),
 * it's a simple type (e.g. <js>"iso-8601"</js>).
 *
 * <p>
 * An example of a type range is a value in an <c>Accept-Encoding</c> header.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 */
@BeanIgnore
public class StringRange {

	private final NameValuePair[] extensions;
	private final Float qValue;
	private final String name;
	private final String string;

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The raw string range string.
	 * 	<br>A value of <jk>null</jk> gets interpreted as matching anything (e.g. <js>"*"</js>).
	 */
	public StringRange(String value) {
		this(parse(value));
	}

	/**
	 * Constructor.
	 *
	 * @param e The parsed string range element.
	 */
	public StringRange(HeaderElement e) {
		Float qValue = 1f;

		// The media type consists of everything up to the q parameter.
		// The q parameter and stuff after is part of the range.
		List<NameValuePair> extensions = list();
		for (NameValuePair p : e.getParameters()) {
			if (p.getName().equals("q")) {
				qValue = Float.parseFloat(p.getValue());
			} else {
				extensions.add(new BasicNameValuePair(p.getName(), p.getValue()));
			}
		}

		this.qValue = qValue;
		this.extensions = extensions.toArray(new NameValuePair[extensions.size()]);
		this.name = e.getName();

		StringBuffer sb = new StringBuffer();
		sb.append(name);

		// '1' is equivalent to specifying no qValue. If there's no extensions, then we won't include a qValue.
		if (Float.compare(qValue.floatValue(), 1f) == 0) {
			if (this.extensions.length > 0) {
				sb.append(";q=").append(qValue);
				extensions.forEach(x -> sb.append(';').append(x.getName()).append('=').append(x.getValue()));
			}
		} else {
			sb.append(";q=").append(qValue);
			extensions.forEach(x -> sb.append(';').append(x.getName()).append('=').append(x.getValue()));
		}
		string = sb.toString();
	}

	/**
	 * Returns the name of this string range.
	 *
	 * <p>
	 * This is the primary value minus the quality or other parameters.
	 *
	 * @return The name of this string range.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the <js>'q'</js> (quality) value for this type, as described in Section 3.9 of RFC2616.
	 *
	 * <p>
	 * The quality value is a float between <c>0.0</c> (unacceptable) and <c>1.0</c> (most acceptable).
	 *
	 * <p>
	 * If 'q' value doesn't make sense for the context (e.g. this range was extracted from a <js>"content-*"</js>
	 * header, as opposed to <js>"accept-*"</js> header, its value will always be <js>"1"</js>.
	 *
	 * @return The 'q' value for this type, never <jk>null</jk>.
	 */
	public Float getQValue() {
		return qValue;
	}

	/**
	 * Returns the optional set of custom extensions defined for this type.
	 *
	 * <p>
	 * Values are lowercase and never <jk>null</jk>.
	 *
	 * @return The optional list of extensions, never <jk>null</jk>.
	 */
	public List<NameValuePair> getExtensions() {
		return ulist(extensions);
	}

	/**
	 * Performs an action on the optional set of custom extensions defined for this type.
	 *
	 * @param action The action to perform.
	 * @return This object.
	 */
	public StringRange forEachExtension(Consumer<NameValuePair> action) {
		for (NameValuePair p : extensions)
			action.accept(p);
		return this;
	}

	/**
	 * Returns <jk>true</jk> if the specified object is also a <c>StringRange</c>, and has the same qValue, type,
	 * parameters, and extensions.
	 *
	 * @return <jk>true</jk> if object is equivalent.
	 */
	@Override /* Object */
	public boolean equals(Object o) {
		return (o instanceof StringRange) && eq(this, (StringRange)o, (x,y)->eq(x.string, y.string));
	}

	/**
	 * Returns a hash based on this instance's <c>media-type</c>.
	 *
	 * @return A hash based on this instance's <c>media-type</c>.
	 */
	@Override /* Object */
	public int hashCode() {
		return string.hashCode();
	}

	/**
	 * Performs a match of this string range against the specified name.
	 *
	 * @param name The name being compared against.
	 * @return
	 * 	0 = no match, 100 = perfect match, 50 = meta-match.
	 */
	public int match(String name) {
		if (qValue == 0)
			return 0;
		if (eq(this.name, name))
			return 100;
		if (eq(this.name, "*"))
			return 50;
		return 0;
	}

	private static HeaderElement parse(String value) {
		HeaderElement[] elements = BasicHeaderValueParser.parseElements(emptyIfNull(trim(value)), null);
		return (elements.length > 0 ? elements[0] : new BasicHeaderElement("*", ""));
	}

	@Override /* Object */
	public String toString() {
		return string;
	}
}
