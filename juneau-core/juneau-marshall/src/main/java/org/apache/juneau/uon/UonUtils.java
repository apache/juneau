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
package org.apache.juneau.uon;

import static org.apache.juneau.common.internal.StringUtils.*;

import org.apache.juneau.common.internal.*;

/**
 * Utility methods for the UON and UrlEncoding serializers and parsers.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.UonDetails">UON Details</a>
 * </ul>
 */
public final class UonUtils {

	private static final AsciiSet needsQuoteChars = AsciiSet.create("),=\n\t\r\b\f ");
	private static final AsciiSet maybeNeedsQuotesFirstChar = AsciiSet.create("),=\n\t\r\b\f tfn+-.#0123456789");

	/**
	 * Returns <jk>true</jk> if the specified string needs to be quoted per UON notation.
	 *
	 * <p>
	 * For example, strings that start with '(' or '@' or look like boolean or numeric values need to be quoted.
	 *
	 * @param s The string to test.
	 * @return <jk>true</jk> if the specified string needs to be quoted per UON notation.
	 */
	public static final boolean needsQuotes(String s) {
		char c0 = s.isEmpty() ? 0 : s.charAt(0);
		return (
			s.isEmpty()
			|| c0 == '@'
			|| c0 == '('
			|| needsQuoteChars.contains(s)
			|| (
				maybeNeedsQuotesFirstChar.contains(c0)
				&& (
					"true".equals(s)
					|| "false".equals(s)
					|| "null".equals(s)
					|| isNumeric(s)
				)
			)
		);
	}
}
