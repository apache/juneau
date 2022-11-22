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
package org.apache.juneau.html.annotation;

/**
 * Identifies possible values for the {@link Html#format()} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.HtmlDetails">HTML Details</a>
 * </ul>
 */
public enum HtmlFormat {

	/**
	 * Object is serialized to a String using the <c>toString()</c> method and written directly to output.
	 * <br>Useful when you want to serialize custom HTML.
	 */
	PLAIN_TEXT,

	/**
	 * Object is serialized to HTML.
	 * <br>This is the default value for serialization.
	 */
	HTML,

	/**
	 * Object is serialized to HTML, use comma-delimited format instead of list.
	 */
	HTML_CDC,

	/**
	 * Object is serialized to HTML, use space-delimited format instead of list.
	 */
	HTML_SDC,

	/**
	 * Object is serialized to XML.
	 * <br>Useful when creating beans that model HTML elements.
	 */
	XML
}
