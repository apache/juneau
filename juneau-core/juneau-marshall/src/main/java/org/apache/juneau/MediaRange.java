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

import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.http.message.*;


/**
 * Describes a single type used in content negotiation between an HTTP client and server, as described in
 * Section 14.1 and 14.7 of RFC2616 (the HTTP/1.1 specification).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 */
public class MediaRange extends MediaType {

	private final NameValuePair[] extensions;
	private final Float qValue;
	private final String string;

	/**
	 * Constructor.
	 *
	 * @param e The parsed media range element.
	 */
	public MediaRange(HeaderElement e) {
		super(e);

		Float qValue = 1f;

		// The media type consists of everything up to the q parameter.
		// The q parameter and stuff after is part of the range.
		List<NameValuePair> extensions = list();
		boolean foundQ = false;
		for (NameValuePair p : e.getParameters()) {
			if (p.getName().equals("q")) {
				qValue = Float.parseFloat(p.getValue());
				foundQ = true;
			} else if (foundQ) {
				extensions.add(new BasicNameValuePair(p.getName(), p.getValue()));
			}
		}

		this.qValue = qValue;
		this.extensions = extensions.toArray(new NameValuePair[extensions.size()]);

		StringBuffer sb = new StringBuffer().append(super.toString());

		// '1' is equivalent to specifying no qValue. If there's no extensions, then we won't include a qValue.
		if (qValue.floatValue() == 1.0) {
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
	 * <p>
	 * Values are lowercase and never <jk>null</jk>.
	 *
	 * @param action The action to perform.
	 * @return This object.
	 */
	public MediaRange forEachExtension(Consumer<NameValuePair> action) {
		for (NameValuePair r : extensions)
			action.accept(r);
		return this;
	}

	@Override /* Object */
	public String toString() {
		return string;
	}
}
