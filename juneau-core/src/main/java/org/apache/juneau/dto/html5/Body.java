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
package org.apache.juneau.dto.html5;

import org.apache.juneau.annotation.*;

/**
 * DTO for an HTML <a href='https://www.w3.org/TR/html5/sections.html#the-body-element'>&lt;body&gt;</a> element.
 * <p>
 */
@Bean(typeName="body")
@SuppressWarnings("hiding")
public class Body extends HtmlElementMixed {

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-window-onafterprint'>onafterprint</a> attribute.
	 * //onbeforeprint https://www.w3.org/TR/html5/webappapis.html#handler-window-onbeforeprint.
	 * @param onafterprint - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Body onafterprint(String onafterprint) {
		attrs.put("onafterprint", onafterprint);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-window-onbeforeunload'>onbeforeunload</a> attribute.
	 * //onhashchange https://www.w3.org/TR/html5/webappapis.html#handler-window-onhashchange.
	 * @param onbeforeunload - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Body onbeforeunload(String onbeforeunload) {
		attrs.put("onbeforeunload", onbeforeunload);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-window-onmessage'>onmessage</a> attribute.
	 * //onoffline https://www.w3.org/TR/html5/webappapis.html#handler-window-onoffline.
	 * @param onmessage - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Body onmessage(String onmessage) {
		attrs.put("onmessage", onmessage);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-window-ononline'>ononline</a> attribute.
	 * //onpagehide https://www.w3.org/TR/html5/webappapis.html#handler-window-onpagehide.
	 * @param ononline - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Body ononline(String ononline) {
		attrs.put("ononline", ononline);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-window-onpageshow'>onpageshow</a> attribute.
	 * //onpopstate https://www.w3.org/TR/html5/webappapis.html#handler-window-onpopstate.
	 * @param onpageshow - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Body onpageshow(String onpageshow) {
		attrs.put("onpageshow", onpageshow);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-window-onstorage'>onstorage</a> attribute.
	 * //onunload https://www.w3.org/TR/html5/webappapis.html#handler-window-onunload.
	 * @param onstorage - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Body onstorage(String onstorage) {
		attrs.put("onstorage", onstorage);
		return this;
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* HtmlElement */
	public final Body _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* HtmlElement */
	public final Body id(String id) {
		super.id(id);
		return this;
	}

	@Override /* HtmlElementMixed */
	public Body children(Object...children) {
		super.children(children);
		return this;
	}

	@Override /* HtmlElementMixed */
	public Body child(Object child) {
		this.children.add(child);
		return this;
	}
}
