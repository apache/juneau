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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#the-meter-element">&lt;meter&gt;</a>
 * element.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../../overview-summary.html#juneau-dto.HTML5'>Overview &gt; juneau-dto &gt; HTML5</a>
 * </ul>
 */
@Bean(typeName="meter")
public class Meter extends HtmlElementMixed {

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-meter-high">high</a> attribute.
	 *
	 * <p>
	 * Low limit of high range.
	 *
	 * @param high
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Meter high(Object high) {
		attr("high", high);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-meter-low">low</a> attribute.
	 *
	 * <p>
	 * High limit of low range.
	 *
	 * @param low
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Meter low(Object low) {
		attr("low", low);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-meter-max">max</a> attribute.
	 *
	 * <p>
	 * Upper bound of range.
	 *
	 * @param max
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Meter max(Object max) {
		attr("max", max);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-meter-min">min</a> attribute.
	 *
	 * <p>
	 * Lower bound of range.
	 *
	 * @param min
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Meter min(Object min) {
		attr("min", min);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-meter-optimum">optimum</a> attribute.
	 *
	 * <p>
	 * Optimum value in gauge.
	 *
	 * @param optimum
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Meter optimum(Object optimum) {
		attr("optimum", optimum);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-meter-value">value</a> attribute.
	 *
	 * <p>
	 * Current value of the element.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Meter value(Object value) {
		attr("value", value);
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* HtmlElement */
	public final Meter _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* HtmlElement */
	public final Meter id(String id) {
		super.id(id);
		return this;
	}

	@Override /* HtmlElement */
	public final Meter style(String style) {
		super.style(style);
		return this;
	}

	@Override /* HtmlElementMixed */
	public Meter children(Object...children) {
		super.children(children);
		return this;
	}

	@Override /* HtmlElementMixed */
	public Meter child(Object child) {
		super.child(child);
		return this;
	}
}
