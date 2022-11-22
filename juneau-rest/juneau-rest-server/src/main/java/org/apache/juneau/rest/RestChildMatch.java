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
package org.apache.juneau.rest;

import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.util.*;

/**
 * Represents a matched {@link Rest}-annotated child on an HTTP request.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jrs.AnnotatedClasses">@Rest-Annotated Classes</a>
 * </ul>
 */
public class RestChildMatch {

	private UrlPathMatch pathMatch;
	private RestContext childContext;

	/**
	 * Creator.
	 *
	 * @param pathMatch The path matching results.
	 * @param childContext The child context.
	 * @return A new {@link RestChildMatch} object.
	 */
	public static RestChildMatch create(UrlPathMatch pathMatch, RestContext childContext) {
		return new RestChildMatch(pathMatch, childContext);
	}

	/**
	 * Constructor.
	 *
	 * @param pathMatch The path matching results.
	 * @param childContext The child context.
	 */
	protected RestChildMatch(UrlPathMatch pathMatch, RestContext childContext) {
		this.pathMatch = pathMatch;
		this.childContext = childContext;
	}

	/**
	 * Returns the path matching results of the REST child match.
	 *
	 * @return The path matching results of the REST child match.
	 */
	public UrlPathMatch getPathMatch() {
		return pathMatch;
	}

	/**
	 * Returns the child context of the REST child match.
	 *
	 * @return The child context of the REST child match.
	 */
	public RestContext getChildContext() {
		return childContext;
	}
}
