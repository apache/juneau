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
 * Defines the interface for rendering the contents of an HTML page produced by the {@link HtmlDocSerializer}
 * serializer.
 *
 * <p>
 * The HTML doc serializer produces the following document structure with the typical contents:
 * <p class='bxml'>
 * 	<xt>&lt;html&gt;
 * 		&lt;head&gt;
 * 			&lt;style&gt;
 * 				<xv>CSS styles and links to stylesheets</xv>
 * 			&lt;/style&gt;
 * 			&lt;script&gt;
 * 				<xv>Javascript</xv>
 * 			&lt;/script&gt;
 * 		&lt;/head&gt;
 * 		&lt;body&gt;
 * 			&lt;header&gt;
 * 				&lt;h1&gt;<xv>Page title</xv>&lt;/h1&gt;
 * 				&lt;h2&gt;<xv>Page description</xv>&lt;/h2&gt;
 * 				<xv>Arbitrary page branding</xv>
 * 			&lt;/header&gt;
 * 			&lt;nav&gt;
 * 				<xv>Page links</xv>
 * 			&lt;/nav&gt;
 * 			&lt;aside&gt;
 * 				<xv>Side-bar page links</xv>
 * 			&lt;/aside&gt;
 * 			&lt;article&gt;
 * 				<xv>Contents of serialized object</xv>
 * 			&lt;/article&gt;
 * 			&lt;footer&gt;
 * 				<xv>Footer message</xv>
 * 			&lt;/footer&gt;
 * 		&lt;/body&gt;
 * 	&lt;/html&gt;</xt>
 * </p>
 *
 * <p>
 * This interface allows you to control how these sections get rendered.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.HtmlDetails">HTML Details</a>
 * </ul>
 */
public interface HtmlDocTemplate {

	/**
	 * Represents a non-existent doc template.
	 */
	public interface Void extends HtmlDocTemplate {}

	/**
	 * Renders the contents of the <code><xt>&lt;head&gt;</xt></code> element.
	 *
	 * @param session The current serializer session.
	 * @param w The writer being written to.
	 * @param o The object being serialized.
	 * @throws Exception Any exception can be thrown.
	 */
	public void writeTo(HtmlDocSerializerSession session, HtmlWriter w, Object o) throws Exception;
}
