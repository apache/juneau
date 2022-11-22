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

import org.apache.juneau.svl.*;

/**
 * Defines an interface for resolvers of <js>"$W{...}"</js> string variables.
 *
 * <p>
 * Widgets must provide one of the following public constructors:
 * <ul>
 * 	<li><code><jk>public</jk> Widget();</code>
 * </ul>
 *
 * <p>
 * Widgets can be defined as inner classes of REST resource classes.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jrs.HtmlWidgets">Widgets</a>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.HtmlDetails">HTML Details</a>
 * </ul>
 */
public interface HtmlWidget {

	/**
	 * The name for this widget.
	 *
	 * @return A unique identifying name for this widget.
	 */
	public String getName();

	/**
	 * Resolves the HTML content for this widget.
	 *
	 * <p>
	 * A returned value of <jk>null</jk> will cause nothing to be added to the page.
	 *
	 * @param session The current serializer session.
	 * @return The HTML content of this widget.
	 */
	public String getHtml(VarResolverSession session);

	/**
	 * Resolves any Javascript that should be added to the <xt>&lt;head&gt;/&lt;script&gt;</xt> element.
	 *
	 * <p>
	 * A returned value of <jk>null</jk> will cause nothing to be added to the page.
	 *
	 * @param session The current serializer session.
	 * @return The Javascript needed by this widget.
	 */
	public String getScript(VarResolverSession session);

	/**
	 * Resolves any CSS styles that should be added to the <xt>&lt;head&gt;/&lt;style&gt;</xt> element.
	 *
	 * <p>
	 * A returned value of <jk>null</jk> will cause nothing to be added to the page.
	 *
	 * @param session The current serializer session.
	 * @return The CSS styles needed by this widget.
	 */
	public String getStyle(VarResolverSession session);
}
