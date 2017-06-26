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
 * <p>
 * The HTML doc serializer produces the following document structure with the typical contents:
 * <p class='bcode'>
 * 	<xt>&lt;html&gt;
 * 		&lt;head&gt;
 * 			&lt;style <xa>type</xa>=<xs>'text/css'</xs>&gt;
 * 				<xv>CSS styles and links to stylesheets</xv>
 * 			&lt;/style&gt;
 * 		&lt;/head&gt;
 * 		&lt;body&gt;
 * 			&lt;header&gt;
 * 				&lt;h3 <xa>class</xa>=<xs>'title'</xs>&gt;<xv>Page title</xv>&lt;/h3&gt;
 * 				&lt;h5 <xa>class</xa>=<xs>'description'</xs>&gt;<xv>Page description</xv>&lt;/h5&gt;
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
 * <p>
 * This interface allows you to control how these sections get rendered.
 */
public interface HtmlDocTemplate {

	/**
	 * Renders the contents of the <code><xt>&lt;head&gt;</xt></code> element.
	 *
	 * @param session The current serializer session.
	 * @param w The writer being written to.
	 * @param s The serializer calling this method.
	 * @param o The object being serialized.
	 * @throws Exception Any exception can be thrown.
	 */
	public void head(HtmlDocSerializerSession session, HtmlWriter w, HtmlDocSerializer s, Object o) throws Exception;

	/**
	 * Renders the contents of the <code><xt>&lt;head&gt;</xt>/<xt>&lt;style</xt>
	 * <xa>type</xa>=<xs>"text/css"</xs><xt>&gt;</xt></code> element.
	 *
	 * @param session The current serializer session.
	 * @param w The writer being written to.
	 * @param s The serializer calling this method.
	 * @param o The object being serialized.
	 * @throws Exception Any exception can be thrown.
	 */
	public void css(HtmlDocSerializerSession session, HtmlWriter w, HtmlDocSerializer s, Object o) throws Exception;

	/**
	 * Renders the contents of the <code><xt>&lt;body&gt;</xt></code> element.
	 *
	 * @param session The current serializer session.
	 * @param w The writer being written to.
	 * @param s The serializer calling this method.
	 * @param o The object being serialized.
	 * @throws Exception Any exception can be thrown.
	 */
	public void body(HtmlDocSerializerSession session, HtmlWriter w, HtmlDocSerializer s, Object o) throws Exception;

	/**
	 * Renders the contents of the <code><xt>&lt;body&gt;</xt>/<xt>&lt;header&gt;</xt></code> element.
	 *
	 * @param session The current serializer session.
	 * @param w The writer being written to.
	 * @param s The serializer calling this method.
	 * @param o The object being serialized.
	 * @throws Exception Any exception can be thrown.
	 */
	public void header(HtmlDocSerializerSession session, HtmlWriter w, HtmlDocSerializer s, Object o) throws Exception;

	/**
	 * Renders the contents of the <code><xt>&lt;body&gt;</xt>/<xt>&lt;nav&gt;</xt></code> element.
	 *
	 * @param session The current serializer session.
	 * @param w The writer being written to.
	 * @param s The serializer calling this method.
	 * @param o The object being serialized.
	 * @throws Exception Any exception can be thrown.
	 */
	public void nav(HtmlDocSerializerSession session, HtmlWriter w, HtmlDocSerializer s, Object o) throws Exception;

	/**
	 * Renders the contents of the <code><xt>&lt;body&gt;</xt>/<xt>&lt;article&gt;</xt></code> element.
	 *
	 * @param session The current serializer session.
	 * @param w The writer being written to.
	 * @param s The serializer calling this method.
	 * @param o The object being serialized.
	 * @throws Exception Any exception can be thrown.
	 */
	public void article(HtmlDocSerializerSession session, HtmlWriter w, HtmlDocSerializer s, Object o) throws Exception;

	/**
	 * Renders the contents of the <code><xt>&lt;body&gt;</xt>/<xt>&lt;aside&gt;</xt></code> element.
	 *
	 * @param session The current serializer session.
	 * @param w The writer being written to.
	 * @param s The serializer calling this method.
	 * @param o The object being serialized.
	 * @throws Exception Any exception can be thrown.
	 */
	public void aside(HtmlDocSerializerSession session, HtmlWriter w, HtmlDocSerializer s, Object o) throws Exception;

	/**
	 * Renders the contents of the <code><xt>&lt;body&gt;</xt>/<xt>&lt;footer&gt;</xt></code> element.
	 *
	 * @param session The current serializer session.
	 * @param w The writer being written to.
	 * @param s The serializer calling this method.
	 * @param o The object being serialized.
	 * @throws Exception Any exception can be thrown.
	 */
	public void footer(HtmlDocSerializerSession session, HtmlWriter w, HtmlDocSerializer s, Object o) throws Exception;

	/**
	 * Returns <jk>true</jk> if this page should render a <code><xt>&lt;head&gt;</xt>/<xt>&lt;style</xt>
	 * <xa>type</xa>=<xs>"text/css"</xs><xt>&gt;</xt></code> element.
	 *
	 * @param session The current serializer session.
	 * @return A boolean flag.
	 */
	public boolean hasCss(HtmlDocSerializerSession session);

	/**
	 * Returns <jk>true</jk> if this page should render a <code><xt>&lt;body&gt;</xt>/<xt>&lt;header&gt;</xt></code>
	 * element.
	 *
	 * @param session The current serializer session.
	 * @return A boolean flag.
	 */
	public boolean hasHeader(HtmlDocSerializerSession session);

	/**
	 * Returns <jk>true</jk> if this page should render a <code><xt>&lt;body&gt;</xt>/<xt>&lt;nav&gt;</xt></code>
	 * element.
	 *
	 * @param session The current serializer session.
	 * @return A boolean flag.
	 */
	public boolean hasNav(HtmlDocSerializerSession session);

	/**
	 * Returns <jk>true</jk> if this page should render a <code><xt>&lt;body&gt;</xt>/<xt>&lt;aside&gt;</xt></code>
	 * element.
	 *
	 * @param session The current serializer session.
	 * @return A boolean flag.
	 */
	public boolean hasAside(HtmlDocSerializerSession session);

	/**
	 * Returns <jk>true</jk> if this page should render a <code><xt>&lt;body&gt;</xt>/<xt>&lt;footer&gt;</xt></code>
	 * element.
	 *
	 * @param session The current serializer session.
	 * @return A boolean flag.
	 */
	public boolean hasFooter(HtmlDocSerializerSession session);
}
