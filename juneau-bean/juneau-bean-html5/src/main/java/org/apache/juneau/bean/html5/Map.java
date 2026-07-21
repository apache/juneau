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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#the-map-element">&lt;map&gt;</a>
 * element.
 *
 * <p>
 * The map element defines an image map, which is an image with clickable areas. It contains area
 * elements that define the clickable regions within the image. The map element is referenced by
 * img elements using the usemap attribute to create interactive images.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple image map with rectangular areas</jc>
 * 	Map <jv>map1</jv> = <jsm>map</jsm>(<js>"navigation"</js>,
 * 		<jsm>area</jsm>(<js>"rect"</js>, <js>"0,0,100,50"</js>, <js>"https://example.com/home"</js>),
 * 		<jsm>area</jsm>(<js>"rect"</js>, <js>"100,0,200,50"</js>, <js>"https://example.com/about"</js>),
 * 		<jsm>area</jsm>(<js>"rect"</js>, <js>"200,0,300,50"</js>, <js>"https://example.com/contact"</js>)
 * 	);
 *
 * 	<jc>// Image map with different area shapes</jc>
 * 	Map <jv>map2</jv> = <jsm>map</jsm>(<js>"shapes"</js>,
 * 		<jsm>area</jsm>(<js>"circle"</js>, <js>"150,75,50"</js>, <js>"https://example.com/circle"</js>),
 * 		<jsm>area</jsm>(<js>"poly"</js>, <js>"0,0,100,0,50,100"</js>, <js>"https://example.com/triangle"</js>),
 * 		<jsm>area</jsm>(<js>"default"</js>, <jk>null</jk>, <js>"https://example.com/default"</js>)
 * 	);
 *
 * 	<jc>// Image map with accessibility</jc>
 * 	Map <jv>map3</jv> = <jsm>map</jsm>(<js>"accessible"</js>,
 * 		<jsm>area</jsm>(<js>"rect"</js>, <js>"0,0,100,100"</js>, <js>"https://example.com/region1"</js>)
 * 			.alt(<js>"Click here for region 1"</js>),
 * 		<jsm>area</jsm>(<js>"rect"</js>, <js>"100,0,200,100"</js>, <js>"https://example.com/region2"</js>)
 * 			.alt(<js>"Click here for region 2"</js>)
 * 	);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "map")
public class Map extends HtmlElementContainer<Map> {

	/**
	 * Creates an empty {@link Map} element.
	 */
	public Map() {}

	/**
	 * Creates a {@link Map} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Map(Object...children) {
		children(children);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-map-name">name</a> attribute.
	 *
	 * <p>
	 * Specifies the name of the image map. This name is used by img elements with the usemap attribute
	 * to reference this map for defining clickable areas.
	 *
	 * <p>
	 * The name should be unique within the document and should not contain spaces or special characters.
	 *
	 * @param value The name of the image map for referencing from img elements. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Map name(String value) {
		attr("name", value);
		return this;
	}

}