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
package org.apache.juneau.rest.server.widget;

import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.converter.*;

/**
 * Widget that returns a menu-item drop-down form for entering BeanQuery search/view/sort/paging arguments
 * (<c>search</c>/<c>view</c>/<c>sort</c>/<c>position</c>/<c>limit</c> &mdash; see {@link Queryable} and
 * {@link org.apache.juneau.http.BeanQueryRequest}).
 *
 * <p>
 * The variable it resolves is <js>"$W{QueryMenuItem}"</js>.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(widgets=QueryMenuItem.<jk>class</jk>)
 * 	<ja>@HtmlDocConfig</ja>(navlinks={<js>"$W{QueryMenuItem}"</js>})
 * 	<jk>public class</jk> MyResource <jk>extends</jk> BasicRestServlet {}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link Queryable}
 * 	<li class='jc'>{@link org.apache.juneau.http.BeanQueryRequest}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HtmlPredefinedWidgets">Predefined Widgets</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HtmlWidgets">Widgets</a>
 * </ul>
 */
public class QueryMenuItem extends MenuItemWidget {

	@Override /* Overridden from MenuItemWidget */
	public String getContent(RestRequest req, RestResponse res) {
		return loadHtml(req, "QueryMenuItem.html");
	}

	@Override /* Overridden from MenuItemWidget */
	public String getLabel(RestRequest req, RestResponse res) {
		return "query";
	}

	/**
	 * Returns CSS for the tooltips.
	 */
	@Override
	public String getStyle(RestRequest req, RestResponse res) {
		return super.getStyle(req, res) + "\n" + loadStyle(req, "styles/QueryMenuItem.css");
	}
}