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
import java.net.URI;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;

/**
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#the-form-element">&lt;form&gt;</a>
 * element.
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'>
 * 		<a class='doclink' href='../../../../../overview-summary.html#DTOs'>Juneau Data Transfer Objects
 * 		(org.apache.juneau.dto)</a>
 * 		<ul>
 * 			<li class='sublink'>
 * 				<a class='doclink' href='../../../../../overview-summary.html#DTOs.HTML5'>HTML5</a>
 * 		</ul>
 * 	</li>
 * </ul>
 */
@Bean(typeName="form")
public class Form extends HtmlElementMixed {

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-form-accept-charset">accept-charset</a>
	 * attribute.
	 * 
	 * <p>
	 * Character encodings to use for form submission.
	 * 
	 * @param acceptcharset The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Form acceptcharset(String acceptcharset) {
		attr("accept-charset", acceptcharset);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fs-action">action</a> attribute.
	 * 
	 * <p>
	 * URL to use for form submission.
	 * 
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 * 
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 * 
	 * @param action The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Form action(String action) {
		attrUri("action", action);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-form-autocomplete">autocomplete</a>
	 * attribute.
	 * 
	 * <p>
	 * Default setting for auto-fill feature for controls in the form.
	 * 
	 * @param autocomplete The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Form autocomplete(String autocomplete) {
		attr("autocomplete", autocomplete);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fs-enctype">enctype</a> attribute.
	 * 
	 * <p>
	 * Form data set encoding type to use for form submission.
	 * 
	 * @param enctype The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Form enctype(String enctype) {
		attr("enctype", enctype);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fs-method">method</a> attribute.
	 * 
	 * <p>
	 * HTTP method to use for form submission.
	 * 
	 * @param method The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Form method(String method) {
		attr("method", method);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-form-name">name</a> attribute.
	 * 
	 * <p>
	 * Name of form to use in the document.forms API.
	 * 
	 * @param name The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Form name(String name) {
		attr("name", name);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fs-novalidate">novalidate</a> attribute.
	 * 
	 * <p>
	 * Bypass form control validation for form submission.
	 * 
	 * @param novalidate The new value for this attribute.
	 * Typically a {@link Boolean} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Form novalidate(Boolean novalidate) {
		attr("novalidate", novalidate);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fs-target">target</a> attribute.
	 * 
	 * <p>
	 * Browsing context for form submission.
	 * 
	 * @param target The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Form target(String target) {
		attr("target", target);
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------


	@Override /* HtmlElement */
	public final Form _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* HtmlElement */
	public final Form id(String id) {
		super.id(id);
		return this;
	}

	@Override /* HtmlElement */
	public final Form style(String style) {
		super.style(style);
		return this;
	}

	@Override /* HtmlElementMixed */
	public Form children(Object...children) {
		super.children(children);
		return this;
	}

	@Override /* HtmlElementMixed */
	public Form child(Object child) {
		super.child(child);
		return this;
	}
}
