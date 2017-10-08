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

import static org.apache.juneau.rest.RestUtils.*;

import java.util.*;

import org.apache.juneau.html.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.widget.*;

/**
 * Programmatic interface for set properties used by the HtmlDoc serializer.
 */
public final class HtmlDocContext {

	final String header, nav, aside, style, stylesheet, script, footer, noResultsMessage;
	final String[] navlinks, head;
	final boolean nowrap;
	final HtmlDocTemplate template;
	final Map<String,Widget> widgets;

	HtmlDocContext(Object resource, RestConfig config) throws RestServletException {
		try {
			Builder b = new Builder(resource, config);

			this.header = b.header;
			this.nav = b.nav;
			this.aside = b.aside;
			this.style = b.style;
			this.stylesheet = b.stylesheet;
			this.script = b.script;
			this.footer = b.footer;
			this.noResultsMessage = b.noResultsMessage;
			this.navlinks = b.navlinks;
			this.head = b.head;
			this.nowrap = b.nowrap;
			this.widgets = Collections.unmodifiableMap(b.widgets);
			this.template = b.template;
		} catch (RestServletException e) {
			throw e;
		} catch (Exception e) {
			throw new RestServletException("Exception occurred while initializing resource ''{0}''", resource.getClass().getSimpleName()).initCause(e);
		}
	}

	HtmlDocContext(java.lang.reflect.Method method, HtmlDocContext pc) throws RestServletException {
		try {
			Builder b = new Builder(method, pc);
			this.header = b.header;
			this.nav = b.nav;
			this.aside = b.aside;
			this.style = b.style;
			this.stylesheet = b.stylesheet;
			this.script = b.script;
			this.footer = b.footer;
			this.noResultsMessage = b.noResultsMessage;
			this.navlinks = b.navlinks;
			this.head = b.head;
			this.nowrap = b.nowrap;
			this.widgets = Collections.unmodifiableMap(b.widgets);
			this.template = b.template;
		} catch (RestServletException e) {
			throw e;
		} catch (Exception e) {
			String sig = method.getDeclaringClass().getName() + '.' + method.getName();
			throw new RestServletException("Exception occurred while initializing method ''{0}''", sig).initCause(e);
		}
	}


	static class Builder {

		String header, nav, aside, style, stylesheet, script, footer, noResultsMessage;
		String[] navlinks, head;
		boolean nowrap;
		HtmlDocTemplate template;
		Map<String,Widget> widgets;


		Builder(java.lang.reflect.Method method, HtmlDocContext pc) throws Exception {
			String sig = method.getDeclaringClass().getName() + '.' + method.getName();

			try {
				RestMethod m = method.getAnnotation(RestMethod.class);
				if (m == null)
					throw new RestServletException("@RestMethod annotation not found on method ''{0}''", sig);

					HtmlDoc hd = m.htmldoc();

					widgets = new HashMap<String,Widget>(pc.widgets);
					for (Class<? extends Widget> wc : hd.widgets()) {
						Widget w = ClassUtils.newInstance(Widget.class, wc);
						widgets.put(w.getName(), w);
					}

					header = resolveNewlineSeparatedAnnotation(hd.header(), pc.header);
					nav = resolveNewlineSeparatedAnnotation(hd.nav(), pc.nav);
					aside = resolveNewlineSeparatedAnnotation(hd.aside(), pc.aside);
					footer = resolveNewlineSeparatedAnnotation(hd.footer(), pc.footer);
					style = resolveNewlineSeparatedAnnotation(hd.style(), pc.style);
					script = resolveNewlineSeparatedAnnotation(hd.script(), pc.script);
					head = resolveContent(hd.head(), pc.head);
					navlinks = resolveLinks(hd.navlinks(), pc.navlinks);
					stylesheet = hd.stylesheet().isEmpty() ? pc.stylesheet : hd.stylesheet();
					nowrap = hd.nowrap() ? hd.nowrap() : pc.nowrap;
					noResultsMessage = hd.noResultsMessage().isEmpty() ? pc.noResultsMessage : hd.noResultsMessage();
					template =
						hd.template() == HtmlDocTemplate.class
						? pc.template
						: ClassUtils.newInstance(HtmlDocTemplate.class, hd.template());
			} catch (RestServletException e) {
				throw e;
			} catch (Exception e) {
				throw new RestServletException("Exception occurred while initializing method ''{0}''", sig).initCause(e);
			}
		}


		Builder(Object resource, RestConfig sc) throws Exception {

			HtmlDocConfig hdc = sc.htmlDocConfig;

			this.widgets = new LinkedHashMap<String,Widget>();
			for (Class<? extends Widget> wc : hdc.widgets) {
				Widget w = resolve(resource, Widget.class, wc);
				this.widgets.put(w.getName(), w);
			}

			header = hdc.header;
			navlinks = hdc.navlinks;
			nav = hdc.nav;
			aside = hdc.aside;
			style = hdc.style;
			stylesheet = hdc.stylesheet;
			script = hdc.script;
			head = hdc.head;
			footer = hdc.footer;
			nowrap = hdc.nowrap;
			noResultsMessage = hdc.noResultsMessage;
			template = resolve(resource, HtmlDocTemplate.class, hdc.template);
		}
	}

	//----------------------------------------------------------------------------------------------------
	// Utility methods
	//----------------------------------------------------------------------------------------------------

	/**
	 * Takes in an object of type T or a Class<T> and either casts or constructs a T.
	 */
	private static <T> T resolve(Object outer, Class<T> c, Object o, Object...cArgs) throws RestServletException {
		try {
			return ClassUtils.newInstanceFromOuter(outer, c, o, cArgs);
		} catch (Exception e) {
			throw new RestServletException("Exception occurred while constructing class ''{0}''", c).initCause(e);
		}
	}
}
