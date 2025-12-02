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

import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.util.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.servlet.*;

/**
 * A POJO structure that describes the list of child resources associated with a resource.
 *
 * <p>
 * Typically used in top-level GET methods of router resources to render a list of available child resources.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/UtilityBeans">Utility Beans</a>
 * 	<li class='jic'>{@link BasicGroupOperations}

 * </ul>
 *
 * @serial exclude
 */
public class ChildResourceDescriptions extends ResourceDescriptions {
	private static final long serialVersionUID = 1L;

	/**
	 * Static creator.
	 *
	 * @param req The HTTP servlet request.
	 * @return A new {@link ChildResourceDescriptions} bean.
	 */
	public static ChildResourceDescriptions of(RestRequest req) {
		return new ChildResourceDescriptions(req);
	}

	/**
	 * Bean constructor.
	 */
	public ChildResourceDescriptions() {}

	/**
	 * Constructor.
	 *
	 * @param context The servlet context that this bean describes.
	 * @param req The HTTP servlet request.
	 */
	public ChildResourceDescriptions(RestContext context, RestRequest req) {
		this(context, req, false);
	}

	/**
	 * Constructor.
	 *
	 * @param context The servlet context that this bean describes.
	 * @param req The HTTP servlet request.
	 * @param sort
	 * 	If <jk>true</jk>, list will be ordered by name alphabetically.
	 * 	Default is to maintain the order as specified in the annotation.
	 */
	public ChildResourceDescriptions(RestContext context, RestRequest req, boolean sort) {
		for (var e : context.getRestChildren().asMap().entrySet()) {
			var title = (String)null;
			try {
				title = e.getValue().getSwagger(req.getLocale()).map(x -> x == null ? null : x.getInfo()).map(x -> x == null ? null : x.getTitle()).orElse(null);
			} catch (Exception e1) {
				title = lm(e1);
			}
			add(new ResourceDescription(e.getKey(), title));
		}
		if (sort)
			Collections.sort(this);
	}

	/**
	 * Constructor.
	 *
	 * @param req The HTTP servlet request.
	 */
	public ChildResourceDescriptions(RestRequest req) {
		this(req.getContext(), req, false);
	}

	@Override /* Overridden from ResourceDescriptions */
	public ChildResourceDescriptions append(String name, String description) {
		super.append(name, description);
		return this;
	}

	@Override /* Overridden from ResourceDescriptions */
	public ChildResourceDescriptions append(String name, String uri, String description) {
		super.append(name, uri, description);
		return this;
	}
}