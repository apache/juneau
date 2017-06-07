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

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.serializer.*;

/**
 * Serializes POJOs to HTTP responses as HTML documents.
 *
 * <h5 class='section'>Media types:</h5>
 * <p>
 * Handles <code>Accept</code> types: <code>text/html</code>
 * <p>
 * Produces <code>Content-Type</code> types: <code>text/html</code>
 *
 * <h5 class='section'>Description:</h5>
 * <p>
 * Same as {@link HtmlSerializer}, except wraps the response in <code><xt>&lt;html&gt;</code>, <code><xt>&lt;head&gt;</code>,
 * 	and <code><xt>&lt;body&gt;</code> tags so that it can be rendered in a browser.
 *
 * <h5 class='section'>Configurable properties:</h5>
 * <p>
 * This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link HtmlDocSerializerContext}
 * 	<li>{@link BeanContext}
 * </ul>
 */
@Produces("text/html")
@SuppressWarnings("hiding")
public class HtmlDocSerializer extends HtmlStrippedDocSerializer {

	/** Default serializer, all default settings. */
	public static final HtmlDocSerializer DEFAULT = new HtmlDocSerializer(PropertyStore.create());


	final HtmlDocSerializerContext ctx;

	/**
	 * Constructor.
	 * @param propertyStore The property store containing all the settings for this object.
	 */
	public HtmlDocSerializer(PropertyStore propertyStore) {
		super(propertyStore);
		this.ctx = createContext(HtmlDocSerializerContext.class);
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	public HtmlDocSerializerSession createSession(Object output, ObjectMap op, Method javaMethod, Locale locale, TimeZone timeZone, MediaType mediaType, UriContext uriContext) {
		return new HtmlDocSerializerSession(ctx, op, output, javaMethod, locale, timeZone, mediaType, uriContext);
	}

	@Override /* Serializer */
	protected void doSerialize(SerializerSession session, Object o) throws Exception {

		HtmlDocSerializerSession s = (HtmlDocSerializerSession)session;
		HtmlWriter w = s.getWriter();
		HtmlDocTemplate t = s.getTemplate();

		w.sTag("html").nl();
		w.sTag(1, "head").nl();
		t.head(s, w, this, o);
		w.eTag(1, "head").nl();
		w.sTag(1, "body").nl();
		t.body(s, w, this, o);
		w.eTag(1, "body").nl();
		w.eTag("html").nl();
	}

	/**
	 * Calls the parent {@link #doSerialize(SerializerSession, Object)} method which invokes just the HTML serializer.
	 * @param session The serializer session.
	 * @param o The object being serialized.
	 * @throws Exception
	 */
	public void parentSerialize(SerializerSession session, Object o) throws Exception {
		super.doSerialize(session, o);
	}
}
