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
package org.apache.juneau.http.header;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

/**
 * Represents a validator value.
 *
 * <p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	ETag: "123456789"    – A strong ETag validator
 * 	ETag: W/"123456789"  – A weak ETag validator
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestCommonBasics">juneau-rest-common Basics</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 */
public class EntityTag {
	/**
	 * Static creator.
	 *
	 * @param value The validator string value.
	 * @return A new header bean or <jk>null</jk> if the value was <jk>null</jk>.
	 * @throws IllegalArgumentException If attempting to set an invalid entity tag value.
	 */
	public static EntityTag of(Object value) {
		Object o = unwrap(value);
		return o == null ? null : new EntityTag(o.toString());
	}

	private final String value;

	private final boolean isWeak, isAny;

	/**
	 * Constructor.
	 *
	 * @param value The validator string value.
	 * @throws IllegalArgumentException If attempting to set an invalid entity tag value.
	 */
	public EntityTag(String value) {
		assertArgNotNull("value", value);

		value = trim(emptyIfNull(value));
		isWeak = value.startsWith("W/");
		isAny = "*".equals(value);

		if (! isAny) {
			if (isWeak)
				value = value.substring(2);
			if (value.length() > 1 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"')
				value = value.substring(1, value.length() - 1);
			else
				throw illegalArg("Invalid value for entity-tag: [{0}]", (isWeak ? ("W/" + value) : value));
		}
		this.value = value;

	}

	/**
	 * Returns the validator value stripped of quotes and weak tag.
	 *
	 * @return The validator value.
	 */
	public String getEntityValue() { return value; }

	/**
	 * Returns <jk>true</jk> if the validator string value is <c>*</c>.
	 *
	 * @return <jk>true</jk> if the validator string value is <c>*</c>.
	 */
	public boolean isAny() { return isAny; }

	/**
	 * Returns <jk>true</jk> if the weak flag is present in the value.
	 *
	 * @return <jk>true</jk> if the weak flag is present in the value.
	 */
	public boolean isWeak() { return isWeak; }

	@Override
	public String toString() {
		return (isWeak ? "W/" : "") + (isAny() ? "*" : ('"' + value + '"'));
	}
}