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

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.html.*;
import org.apache.juneau.http.*;

/**
 * Describes a single route (an {@code @RestOp}-annotated method) in a route-index listing.
 *
 * <p>
 * Companion of {@link ResourceDescription} (which describes a child resource); a {@code RouteDescription}
 * describes an operation mounted on a resource.  Returned (as a {@link RouteDescriptions} list) by the
 * route-index op so the response is content-negotiated through the configured serializers &mdash; an
 * {@code Accept: text/html} request renders a browsable table with the {@linkplain #getPath() path} as a
 * clickable link (via {@link Html#link() @Html(link)}), while {@code application/json} / {@code text/xml}
 * clients receive the same entries in their requested format.
 *
 * <p>
 * The five serialized properties &mdash; {@code path}, {@code methods}, {@code summary}, {@code description},
 * and {@code deprecated} &mdash; preserve the JSON shape emitted by earlier route-index snapshots so existing
 * API clients see an equivalent payload.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link RouteDescriptions}
 * 	<li class='jc'>{@link ResourceDescription}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/UtilityBeans">Utility Beans</a>
 * </ul>
 *
 * @serial exclude
 * @since 10.0.0
 */
@BeanType(properties = "path,methods,summary,description,deprecated", findFluentSetters = true)
@Response(schema = @Schema(ignore = true))
public class RouteDescription {

	private String path;
	private List<String> methods;
	private String summary;
	private String description;
	private boolean deprecated;

	/** No-arg bean constructor. */
	public RouteDescription() {}

	/**
	 * Constructor.
	 *
	 * @param path The mount path of the operation (e.g. {@code "/items/{id}"}).
	 * @param methods The HTTP methods the operation responds to (e.g. {@code ["GET"]}).
	 * @param summary The operation summary, or an empty string when none is declared.
	 * @param description The operation description, or an empty string when none is declared.
	 * @param deprecated Whether the operation (or its declaring class) is {@link Deprecated @Deprecated}.
	 */
	public RouteDescription(String path, List<String> methods, String summary, String description, boolean deprecated) {
		this.path = path;
		this.methods = methods;
		this.summary = summary;
		this.description = description;
		this.deprecated = deprecated;
	}

	/**
	 * Returns the mount path of the operation.
	 *
	 * <p>
	 * Rendered as a clickable link in HTML output via {@link Html#link() @Html(link)}.
	 *
	 * @return The mount path.
	 */
	@Html(link = "servlet:{path}")
	public String getPath() { return path; }

	/**
	 * Sets the mount path of the operation.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public RouteDescription path(String value) {
		path = value;
		return this;
	}

	/**
	 * Returns the HTTP methods the operation responds to.
	 *
	 * @return The HTTP methods.
	 */
	public List<String> getMethods() { return methods; }

	/**
	 * Sets the HTTP methods the operation responds to.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public RouteDescription methods(List<String> value) {
		methods = value;
		return this;
	}

	/**
	 * Returns the operation summary.
	 *
	 * @return The summary, or an empty string when none is declared.
	 */
	public String getSummary() { return summary; }

	/**
	 * Sets the operation summary.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public RouteDescription summary(String value) {
		summary = value;
		return this;
	}

	/**
	 * Returns the operation description.
	 *
	 * @return The description, or an empty string when none is declared.
	 */
	public String getDescription() { return description; }

	/**
	 * Sets the operation description.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public RouteDescription description(String value) {
		description = value;
		return this;
	}

	/**
	 * Returns whether the operation is deprecated.
	 *
	 * @return <jk>true</jk> if the operation (or its declaring class) is {@link Deprecated @Deprecated}.
	 */
	public boolean isDeprecated() { return deprecated; }

	/**
	 * Sets whether the operation is deprecated.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public RouteDescription deprecated(boolean value) {
		deprecated = value;
		return this;
	}
}
