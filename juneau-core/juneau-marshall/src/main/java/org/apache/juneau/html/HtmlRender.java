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

import org.apache.juneau.html.annotation.*;
import org.apache.juneau.serializer.*;

/**
 * Allows custom rendering of bean property values when serialized as HTML.
 *
 * <p>
 * Associated with bean properties using the {@link Html#render() @Html(render)} annotation.
 *
 * <p>
 * Using this class, you can alter the CSS style and HTML content of the bean property.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.HtmlRenderAnnotation">@Html(render) Annotation</a>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.HtmlDetails">HTML Details</a>
 * </ul>
 *
 * @param <T> The bean property type.
 */
public abstract class HtmlRender<T> {

	/**
	 * Returns the CSS style of the element containing the bean property value.
	 *
	 * @param session
	 * 	The current serializer session.
	 * 	Can be used to retrieve properties and session-level information.
	 * @param value The bean property value.
	 * @return The CSS style string, or <jk>null</jk> if no style should be added.
	 */
	public String getStyle(SerializerSession session, T value) {
		return null;
	}

	/**
	 * Returns the delegate value for the specified bean property value.
	 *
	 * <p>
	 * The default implementation simply returns the same value.
	 * A typical use is to return an HTML element using one of the HTML5 DOM beans.
	 *
	 * @param session
	 * 	The current serializer session.
	 * 	Can be used to retrieve properties and session-level information.
	 * @param value The bean property value.
	 * @return The new bean property value.
	 */
	public Object getContent(SerializerSession session, T value) {
		return value;
	}
}
