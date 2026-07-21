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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/grouping-content.html#the-figure-element">&lt;figure&gt;</a>
 * element.
 *
 * <p>
 * The figure element represents self-contained content, potentially with a caption, that is
 * typically referenced as a single unit from the main flow of the document. It is used to
 * group related content such as images, diagrams, code snippets, or other media that can
 * be moved away from the main flow of the document without affecting the document's meaning.
 * The figure element can contain a figcaption element to provide a caption for the content.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple figure with image</jc>
 * 	Figure <jv>simple</jv> = <jsm>figure</jsm>(
 * 		<jsm>img</jsm>(<js>"/images/sunset.jpg"</js>, <js>"Sunset"</js>),
 * 		<jsm>figcaption</jsm>(<js>"A beautiful sunset over the mountains."</js>)
 * 	);
 *
 * 	<jc>// Figure with code snippet</jc>
 * 	Figure <jv>codeFigure</jv> = <jsm>figure</jsm>(
 * 		<jsm>pre</jsm>(<jsm>code</jsm>(<js>"function hello() {\n  return 'Hello World';\n}"</js>)),
 * 		<jsm>figcaption</jsm>(<js>"A simple JavaScript function."</js>)
 * 	);
 *
 * 	<jc>// Figure with styling</jc>
 * 	Figure <jv>styled</jv> = <jsm>figure</jsm>(
 * 		<jsm>img</jsm>(<js>"/charts/sales.png"</js>, <js>"Sales Chart"</js>),
 * 		<jsm>figcaption</jsm>(<js>"Monthly sales data for 2024."</js>)
 * 	).class_(<js>"chart-figure"</js>);
 *
 * 	<jc>// Figure with multiple elements</jc>
 * 	Figure <jv>complex</jv> = <jsm>figure</jsm>(
 * 		<jsm>img</jsm>(<js>"/images/diagram.png"</js>, <js>"Process Diagram"</js>),
 * 		<jsm>p</jsm>(<js>"This diagram shows the complete workflow."</js>),
 * 		<jsm>figcaption</jsm>(<js>"Figure 1: System Architecture Overview."</js>)
 * 	);
 *
 * 	<jc>// Figure with ID</jc>
 * 	Figure <jv>withId</jv> = <jsm>figure</jsm>(
 * 		<jsm>img</jsm>(<js>"/charts/main.png"</js>, <js>"Main Chart"</js>),
 * 		<jsm>figcaption</jsm>(<js>"Primary performance metrics."</js>)
 * 	).id(<js>"main-chart"</js>);
 *
 * 	<jc>// Figure with styling</jc>
 * 	Figure <jv>styled2</jv> = <jsm>figure</jsm>(
 * 		<jsm>img</jsm>(<js>"/images/example.png"</js>, <js>"Example"</js>),
 * 		<jsm>figcaption</jsm>(<js>"An example of the new feature."</js>)
 * 	).style(<js>"border: 1px solid #ccc; padding: 10px; margin: 20px 0;"</js>);
 *
 * 	<jc>// Figure with table</jc>
 * 	Figure <jv>tableFigure</jv> = <jsm>figure</jsm>(
 * 		<jsm>table</jsm>(
 * 			<jsm>tr</jsm>(
 * 				<jsm>th</jsm>(<js>"Name"</js>),
 * 				<jsm>th</jsm>(<js>"Value"</js>)
 * 			),
 * 			<jsm>tr</jsm>(
 * 				<jsm>td</jsm>(<js>"Item 1"</js>),
 * 				<jsm>td</jsm>(<js>"100"</js>)
 * 			)
 * 		),
 * 		<jsm>figcaption</jsm>(<js>"Data summary table."</js>)
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#figure() figure()}
 * 		<li class='jm'>{@link HtmlBuilder#figure(Object...) figure(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "figure")
public class Figure extends HtmlElementContainer<Figure> {

	/**
	 * Creates an empty {@link Figure} element.
	 */
	public Figure() {}

	/**
	 * Creates a {@link Figure} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Figure(Object...children) {
		children(children);
	}

}