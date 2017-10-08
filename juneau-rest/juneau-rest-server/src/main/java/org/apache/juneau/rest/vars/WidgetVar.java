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
package org.apache.juneau.rest.vars;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.widget.*;
import org.apache.juneau.svl.*;

/**
 * HTML widget variable resolver.
 *
 * <p>
 * The format for this var is <js>"$W{widgetName}"</js>.
 *
 * <p>
 * Widgets are simple class that produce some sort of string based on a passed-in HTTP request.
 *
 * <p>
 * They're registered via the following mechanisms:
 * <ul>
 * 	<li>{@link HtmlDoc#widgets() @HtmlDoc.widgets()}
 * 	<li>{@link HtmlDocConfig#widget(Class)}
 * </ul>
 *
 * @see org.apache.juneau.svl
 */
public class WidgetVar extends SimpleVar {

	/**
	 * The name of the session or context object that identifies the {@link RestRequest} object.
	 */
	private static final String SESSION_req = "req";

	/**
	 * The name of this variable.
	 */
	public static final String NAME = "W";

	/**
	 * Constructor.
	 */
	public WidgetVar() {
		super(NAME);
	}

	@Override /* Parameter */
	public String resolve(VarResolverSession session, String key) throws Exception {
		RestRequest req = session.getSessionObject(RestRequest.class, SESSION_req);
		Widget w = req.getWidgets().get(key);
		if (w == null)
			return "unknown-widget-"+key;
		return w.getHtml(req);
	}
}