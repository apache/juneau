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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#the-iframe-element">&lt;iframe&gt;</a> element.
 * <p>
 */
@Bean(typeName="iframe")
public class Iframe extends HtmlElementMixed {

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-dim-height">height</a> attribute.
	 * Vertical dimension.
	 * @param height - The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Iframe height(Object height) {
		attr("height", height);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-iframe-name">name</a> attribute.
	 * Name of nested browsing context.
	 * @param name - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Iframe name(String name) {
		attr("name", name);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-iframe-sandbox">sandbox</a> attribute.
	 * Security rules for nested content.
	 * @param sandbox - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Iframe sandbox(String sandbox) {
		attr("sandbox", sandbox);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-iframe-src">src</a> attribute.
	 * Address of the resource.
	 * @param src - The new value for this attribute.
	 * 	Typically a {@link URL} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Iframe src(Object src) {
		attr("src", src);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-iframe-srcdoc">srcdoc</a> attribute.
	 * A document to render in the iframe.
	 * @param srcdoc - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Iframe srcdoc(String srcdoc) {
		attr("srcdoc", srcdoc);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-dim-width">width</a> attribute.
	 * Horizontal dimension.
	 * @param width - The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Iframe width(Object width) {
		attr("width", width);
		return this;
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* HtmlElement */
	public final Iframe _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* HtmlElement */
	public final Iframe id(String id) {
		super.id(id);
		return this;
	}

	@Override /* HtmlElementMixed */
	public Iframe children(Object...children) {
		super.children(children);
		return this;
	}

	@Override /* HtmlElementMixed */
	public Iframe child(Object child) {
		super.child(child);
		return this;
	}
}
