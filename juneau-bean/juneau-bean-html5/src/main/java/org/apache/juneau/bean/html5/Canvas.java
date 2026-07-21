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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/scripting-1.html#the-canvas-element">&lt;canvas&gt;</a>
 * element.
 *
 * <p>
 * The canvas element provides a resolution-dependent bitmap canvas that can be used for rendering
 * graphs, game graphics, or other visual images on the fly. It is used with JavaScript to create
 * dynamic, scriptable rendering of 2D shapes and bitmap images. The canvas element requires both
 * width and height attributes to define the canvas size, and the actual drawing is done through
 * the Canvas API in JavaScript.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Basic canvas</jc>
 * 	Canvas <jv>basic</jv> = <jsm>canvas</jsm>(300, 200);
 *
 * 	<jc>// Canvas with ID for JavaScript access</jc>
 * 	Canvas <jv>withId</jv> = <jsm>canvas</jsm>(400, 300)
 * 		.id(<js>"myCanvas"</js>);
 *
 * 	<jc>// Canvas with styling</jc>
 * 	Canvas <jv>styled</jv> = <jsm>canvas</jsm>(500, 400)
 * 		.id(<js>"drawingCanvas"</js>)
 * 		.class_(<js>"canvas-element"</js>)
 * 		.style(<js>"border: 1px solid #ccc;"</js>);
 *
 * 	<jc>// Canvas with event handlers</jc>
 * 	Canvas <jv>interactive</jv> = <jsm>canvas</jsm>(800, 600)
 * 		.id(<js>"gameCanvas"</js>)
 * 		.onclick(<js>"handleCanvasClick(event)"</js>)
 * 		.onmousemove(<js>"handleMouseMove(event)"</js>);
 *
 * 	<jc>// Canvas with fallback content</jc>
 * 	Canvas <jv>withFallback</jv> = <jsm>canvas</jsm>(600, 400)
 * 		.id(<js>"chartCanvas"</js>)
 * 		.children(<js>"Your browser does not support the canvas element."</js>);
 *
 * 	<jc>// Canvas for data visualization</jc>
 * 	Canvas <jv>chart</jv> = <jsm>canvas</jsm>(800, 500)
 * 		.id(<js>"dataChart"</js>)
 * 		.title(<js>"Interactive Data Chart"</js>);
 *
 * 	<jc>// Canvas with accessibility</jc>
 * 	Canvas <jv>accessible</jv> = <jsm>canvas</jsm>(400, 300)
 * 		.id(<js>"accessibleCanvas"</js>)
 * 		.title(<js>"Interactive drawing canvas"</js>)
 * 		.children(<js>"Use your mouse to draw on this canvas."</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#canvas() canvas()}
 * 		<li class='jm'>{@link HtmlBuilder#canvas(Number, Number) canvas(Number, Number)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "canvas")
public class Canvas extends HtmlElementMixed<Canvas> {

	/**
	 * Creates an empty {@link Canvas} element.
	 */
	public Canvas() {}

	/**
	 * Creates a {@link Canvas} element with the specified {@link Canvas#width(Object)} and
	 * {@link Canvas#height(Object)} attributes.
	 *
	 * @param width The {@link Canvas#width(Object)} attribute. Can be <jk>null</jk>.
	 * @param height The {@link Canvas#height(Object)} attribute. Can be <jk>null</jk>.
	 */
	public Canvas(Number width, Number height) {
		width(width).height(height);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/scripting-1.html#attr-canvas-height">height</a> attribute.
	 *
	 * <p>
	 * Vertical dimension.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Canvas height(Object value) {
		attr("height", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/scripting-1.html#attr-canvas-width">width</a> attribute.
	 *
	 * <p>
	 * Horizontal dimension.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Canvas width(Object value) {
		attr("width", value);
		return this;
	}
}