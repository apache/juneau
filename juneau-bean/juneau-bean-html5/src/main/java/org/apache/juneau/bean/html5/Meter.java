/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.bean.html5;

import org.apache.juneau.marshall.*;

/**
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#the-meter-element">&lt;meter&gt;</a>
 * element.
 *
 * <p>
 * The meter element represents a scalar measurement within a known range, or a fractional value.
 * It is used to display a gauge or meter showing a value within a defined range, such as disk
 * usage, memory usage, or progress. The meter element is not suitable for representing a range
 * of values (use the input element with type="range" for that). It is typically rendered as a
 * visual gauge or bar that shows the current value relative to the minimum and maximum values.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple meter</jc>
 * 	Meter <jv>simple</jv> = <jsm>meter</jsm>()
 * 		.value(50)
 * 		.min(0)
 * 		.max(100);
 *
 * 	<jc>// Meter with styling</jc>
 * 	Meter <jv>styled</jv> = <jsm>meter</jsm>()
 * 		.class_(<js>"progress-meter"</js>)
 * 		.value(75)
 * 		.min(0)
 * 		.max(100);
 *
 * 	<jc>// Meter with complex content</jc>
 * 	Meter <jv>complex</jv> = <jsm>meter</jsm>()
 * 		.value(60)
 * 		.min(0)
 * 		.max(100)
 * 		.low(25)
 * 		.high(75)
 * 		.optimum(50);
 *
 * 	<jc>// Meter with ID</jc>
 * 	Meter <jv>withId</jv> = <jsm>meter</jsm>()
 * 		.id(<js>"disk-usage"</js>)
 * 		.value(80)
 * 		.min(0)
 * 		.max(100);
 *
 * 	<jc>// Meter with styling</jc>
 * 	Meter <jv>styled2</jv> = <jsm>meter</jsm>()
 * 		.style(<js>"width: 200px; height: 20px;"</js>)
 * 		.value(40)
 * 		.min(0)
 * 		.max(100);
 *
 * 	<jc>// Meter with multiple attributes</jc>
 * 	Meter <jv>multiple</jv> = <jsm>meter</jsm>()
 * 		.value(85)
 * 		.min(0)
 * 		.max(100)
 * 		.low(20)
 * 		.high(80)
 * 		.optimum(50)
 * 		.title(<js>"Disk Usage: 85%"</js>);
 *
 * 	// Meter with form
 * 	Meter withForm = new Meter()
 * 		.form("usage-form")
 * 		.value(30)
 * 		.min(0)
 * 		.max(100);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "meter")
public class Meter extends HtmlElementMixed<Meter> {

	/**
	 * Creates an empty {@link Meter} element.
	 */
	public Meter() {}

	/**
	 * Creates a {@link Meter} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Meter(Object...children) {
		children(children);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-meter-high">high</a> attribute.
	 *
	 * <p>
	 * Low limit of high range.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Meter high(Object value) {
		attr("high", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-meter-low">low</a> attribute.
	 *
	 * <p>
	 * High limit of low range.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Meter low(Object value) {
		attr("low", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-meter-max">max</a> attribute.
	 *
	 * <p>
	 * Upper bound of range.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Meter max(Object value) {
		attr("max", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-meter-min">min</a> attribute.
	 *
	 * <p>
	 * Lower bound of range.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Meter min(Object value) {
		attr("min", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-meter-optimum">optimum</a> attribute.
	 *
	 * <p>
	 * Optimum value in gauge.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Meter optimum(Object value) {
		attr("optimum", value);
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
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Meter value(Object value) {
		attr("value", value);
		return this;
	}
}