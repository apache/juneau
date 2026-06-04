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
package org.apache.juneau.rest.beans;

import java.util.*;

/**
 * A list of {@link RouteDescription} objects.
 *
 * <p>
 * Typically returned by a route-index op so the listing is content-negotiated through the configured
 * serializers (browsable HTML table, JSON, or XML based on the {@code Accept} header), mirroring the way
 * {@link ChildResourceDescriptions} backs the child-resource navigation page.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link RouteDescription}
 * 	<li class='jc'>{@link ResourceDescriptions}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/UtilityBeans">Utility Beans</a>
 * </ul>
 *
 * @serial exclude
 * @since 10.0.0
 */
public class RouteDescriptions extends ArrayList<RouteDescription> {
	private static final long serialVersionUID = 1L;

	/**
	 * Static creator.
	 *
	 * @return A new {@link RouteDescriptions} object.
	 */
	public static RouteDescriptions create() {
		return new RouteDescriptions();
	}

	/**
	 * Adds a new {@link RouteDescription} to this list.
	 *
	 * @param path The mount path of the operation.
	 * @param methods The HTTP methods the operation responds to.
	 * @param summary The operation summary.
	 * @param description The operation description.
	 * @param deprecated Whether the operation is deprecated.
	 * @return This object.
	 */
	public RouteDescriptions append(String path, List<String> methods, String summary, String description, boolean deprecated) {
		super.add(new RouteDescription(path, methods, summary, description, deprecated));
		return this;
	}
}
