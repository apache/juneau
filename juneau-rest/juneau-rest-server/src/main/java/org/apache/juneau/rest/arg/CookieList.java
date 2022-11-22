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
package org.apache.juneau.rest.arg;

import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;

import javax.servlet.http.*;

/**
 * A simple list of {@link Cookie} objects.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.HttpParts">HTTP Parts</a>
 * </ul>
 *
 * @serial exclude
 */
public class CookieList extends ArrayList<Cookie> {

	private static final long serialVersionUID = 1L;

	/**
	 * Static creator.
	 *
	 * @param values The values to set in the cookie list.
	 * @return A new cookie list.
	 */
	public static CookieList of(Cookie[] values) {
		return new CookieList(values);
	}

	/**
	 * Constructor.
	 *
	 * @param values The values to set in the cookie list.
	 */
	public CookieList(Cookie[] values) {
		super(alist(values));
	}
}
