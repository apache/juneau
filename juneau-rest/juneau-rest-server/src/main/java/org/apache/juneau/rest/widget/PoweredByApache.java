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
package org.apache.juneau.rest.widget;

import org.apache.juneau.*;
import org.apache.juneau.rest.*;

/**
 * Widget that places a powered-by-Apache message on the page.
 *
 * <p>
 * The variable it resolves is <js>"$W{PoweredByApache}"</js>.
 *
 * <p>
 * It produces a simple Apache icon floating on the right.
 * Typically it's used in the footer of the page, as shown below in the <c>RootResources</c> from the examples:
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(
 * 		path=<js>"/"</js>,
 * 		title=<js>"Root resources"</js>,
 * 		description=<js>"Example of a router resource page."</js>
 * 	)
 *  <ja>@HtmlDocConfig</ja>(
 * 		widgets={
 * 			PoweredByApache.<jk>class</jk>
 * 		},
 * 		footer=<js>"$W{PoweredByApache}"</js>
 * 	)
 * </p>
 *
 * <p>
 * It renders the following image:
 * <img class='bordered' src='doc-files/PoweredByApacheWidget.png'>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.HtmlPredefinedWidgets">Predefined Widgets</a>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.HtmlWidgets">Widgets</a>
 * </ul>
 */
public class PoweredByApache extends Widget {

	/**
	 * Returns an Apache image tag hyperlinked to <js>"http://apache.org"</js>
	 */
	@Override /* Widget */
	public String getHtml(RestRequest req, RestResponse res) {
		UriResolver r = req.getUriResolver();
		return "<a href='http://apache.org'><img style='float:right;padding-right:20px;height:32px' src='"+r.resolve("servlet:/htdocs/asf.png")+"'>";
	}
}


