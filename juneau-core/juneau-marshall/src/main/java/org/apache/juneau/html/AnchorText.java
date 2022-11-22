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
package org.apache.juneau.html;

/**
 * Identifies possible values for the {@link HtmlSerializer.Builder#uriAnchorText(AnchorText)} setting.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.HtmlDetails">HTML Details</a>
 * </ul>
 */
public enum AnchorText {

	/**
	 * Set to whatever is returned by {@link #toString()} on the object.
	 */
	TO_STRING,

	/**
	 * Set to the bean property name.
	 */
	PROPERTY_NAME,

	/**
	 * Set to the URI value.
	 *
	 * <p>
	 * This is the same as {@link #TO_STRING} but strips off the anchor tag if present.
	 */
	URI,

	/**
	 * Set to the last token of the URI value.
	 */
	LAST_TOKEN,

	/**
	 * Set to the anchor of the URL.
	 *
	 * <p>
	 * (e.g. <js>"http://localhost:9080/foobar#anchorTextHere"</js>)
	 */
	URI_ANCHOR,

	/**
	 * Same as {@link #TO_STRING} but assumes it's a context-relative path.
	 */
	CONTEXT_RELATIVE,

	/**
	 * Same as {@link #TO_STRING} but assumes it's a servlet-relative path.
	 */
	SERVLET_RELATIVE,

	/**
	 * Same as {@link #TO_STRING} but assumes it's a path-relative path.
	 */
	PATH_RELATIVE
}