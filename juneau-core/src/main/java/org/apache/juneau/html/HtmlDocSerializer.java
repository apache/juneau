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

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.serializer.*;

/**
 * Serializes POJOs to HTTP responses as HTML documents.
 *
 * <h5 class='section'>Media types:</h5>
 *
 * Handles <code>Accept</code> types: <code>text/html</code>
 * <p>
 * Produces <code>Content-Type</code> types: <code>text/html</code>
 *
 * <h5 class='section'>Description:</h5>
 *
 * Same as {@link HtmlSerializer}, except wraps the response in <code><xt>&lt;html&gt;</code>,
 * <code><xt>&lt;head&gt;</code>, and <code><xt>&lt;body&gt;</code> tags so that it can be rendered in a browser.
 *
 * <h5 class='section'>Configurable properties:</h5>
 *
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
	 *
	 * @param propertyStore The property store containing all the settings for this object.
	 */
	public HtmlDocSerializer(PropertyStore propertyStore) {
		super(propertyStore);
		this.ctx = createContext(HtmlDocSerializerContext.class);
	}

	@Override /* Serializer */
	public HtmlDocSerializerSession createSession(SerializerSessionArgs args) {
		return new HtmlDocSerializerSession(ctx, args);
	}
}
