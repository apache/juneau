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

import java.util.*;

import org.apache.juneau.html.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.rest.*;
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
 * 	<li>{@link HtmlDocConfig#widgets() @HtmlDocConfig(widgets)}
 * </ul>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'>{@doc juneau-rest-server.SvlVariables}
 * </ul>
 *
 * @deprecated Use {@link HtmlWidgetVar}
 */
@Deprecated
public class WidgetVar extends SimpleVar {

	private static final String SESSION_req = "req";
	private static final String SESSION_res = "res";

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

	@Override /* Var */
	public String resolve(VarResolverSession session, String key) throws Exception {
		RestRequest req = session.getSessionObject(RestRequest.class, SESSION_req, true);
		RestResponse res = session.getSessionObject(RestResponse.class, SESSION_res, true);

		boolean isScript = false, isStyle = false;

		if (key.endsWith(".script")) {
			key = key.substring(0, key.length() - 7);
			isScript = true;
		}

		if (key.endsWith(".style")) {
			key = key.substring(0, key.length() - 6);
			isStyle = true;
		}

		HtmlWidget w = req.getWidgets().get(key);

		if (w == null) {
			Map<String,Widget> widgetMap = session.getSessionObject(Map.class, "htmlWidgets", false);
			if (widgetMap != null)
				w = widgetMap.get(key);
		}

		if (w == null)
			return "unknown-widget-"+key;

		if (w instanceof Widget) {
			Widget w2 = (Widget)w;
			if (isScript)
				return w2.getScript(req, res);
			if (isStyle)
				return w2.getStyle(req, res);
			return w2.getHtml(req, res);
		}

		return w.getHtml(session);
	}

	@Override /* Var */
	public boolean canResolve(VarResolverSession session) {
		return session.hasSessionObject(SESSION_req) && session.hasSessionObject(SESSION_res);
	}
}