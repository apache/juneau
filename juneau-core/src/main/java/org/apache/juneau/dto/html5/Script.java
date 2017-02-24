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

import java.net.*;

import org.apache.juneau.annotation.*;

/**
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/scripting-1.html#the-script-element">&lt;script&gt;</a> element.
 * <p>
 */
@Bean(typeName="script")
public class Script extends HtmlElementRawText {

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/scripting-1.html#attr-script-async">async</a> attribute.
	 * Execute script asynchronously.
	 * @param async The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Script async(Object async) {
		attr("async", async);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/scripting-1.html#attr-script-charset">charset</a> attribute.
	 * Character encoding of the external script resource.
	 * @param charset The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Script charset(String charset) {
		attr("charset", charset);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/scripting-1.html#attr-script-crossorigin">crossorigin</a> attribute.
	 * How the element handles crossorigin requests.
	 * @param crossorigin The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Script crossorigin(String crossorigin) {
		attr("crossorigin", crossorigin);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/scripting-1.html#attr-script-defer">defer</a> attribute.
	 * Defer script execution.
	 * @param defer The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Script defer(Object defer) {
		attr("defer", defer);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/scripting-1.html#attr-script-src">src</a> attribute.
	 * Address of the resource.
	 * @param src The new value for this attribute.
	 * 	Typically a {@link URL} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Script src(Object src) {
		attr("src", src);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/scripting-1.html#attr-script-type">type</a> attribute.
	 * Type of embedded resource.
	 * @param type The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Script type(String type) {
		attr("type", type);
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* HtmlElement */
	public final Script _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* HtmlElement */
	public final Script id(String id) {
		super.id(id);
		return this;
	}

	@Override /* HtmlElementText */
	public Script text(Object text) {
		super.text(text);
		return this;
	}
}
