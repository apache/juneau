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
 * DTO for an HTML <a class="doclink"
 * href="https://www.w3.org/TR/html5/sections.html#the-body-element">&lt;body&gt;</a> element.
 * <p>
 *
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../../overview-summary.html#DTOs'>Juneau Data Transfer Objects
 * 		(org.apache.juneau.dto)</a>
 * 	<ul>
 * 		<li class='link'><a class='doclink' href='../../../../../overview-summary.html#DTOs.HTML5'>HTML5</a>
 * 	</ul>
 * </ul>
 */
@Bean(typeName="body")
public class Body extends HtmlElementMixed {

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-window-onafterprint">onafterprint</a>
	 * attribute.
	 *
	 * @param onafterprint The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Body onafterprint(String onafterprint) {
		attr("onafterprint", onafterprint);
		return this;
	}

	/**
	 * <a class="doclink"
	 * href="https://www.w3.org/TR/html5/webappapis.html#handler-window-onbeforeunload">onbeforeunload</a> attribute.
	 *
	 * @param onbeforeunload The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Body onbeforeunload(String onbeforeunload) {
		attr("onbeforeunload", onbeforeunload);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-window-onmessage">onmessage</a>
	 * attribute.
	 *
	 * @param onmessage The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Body onmessage(String onmessage) {
		attr("onmessage", onmessage);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-window-ononline">ononline</a>
	 * attribute.
	 *
	 * @param ononline The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Body ononline(String ononline) {
		attr("ononline", ononline);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-window-onpageshow">onpageshow</a>
	 * attribute.
	 *
	 * @param onpageshow The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Body onpageshow(String onpageshow) {
		attr("onpageshow", onpageshow);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-window-onstorage">onstorage</a>
	 * attribute.
	 *
	 * @param onstorage The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Body onstorage(String onstorage) {
		attr("onstorage", onstorage);
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

	@Override /* HtmlElement */
	public final Body style(String style) {
		super.style(style);
		return this;
	}

	@Override /* HtmlElementMixed */
	public Body children(Object...children) {
		super.children(children);
		return this;
	}

	@Override /* HtmlElementMixed */
	public Body child(Object child) {
		super.child(child);
		return this;
	}
}
