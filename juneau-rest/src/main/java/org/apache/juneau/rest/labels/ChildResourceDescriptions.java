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
package org.apache.juneau.rest.labels;

import java.util.*;

import org.apache.juneau.rest.*;

/**
 * A POJO structure that describes the list of child resources associated with a resource.
 * <p>
 * Typically used in top-level GET methods of router resources to render a list of available child resources.
 */
public class ChildResourceDescriptions extends LinkedList<ResourceDescription> {

	private static final long serialVersionUID = 1L;

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
	 * @param sort If <jk>true</jk>, list will be ordered by name alphabetically.
	 * Default is to maintain the order as specified in the annotation.
	 */
	public ChildResourceDescriptions(RestContext context, RestRequest req, boolean sort) {
		for (Map.Entry<String,RestContext> e : context.getChildResources().entrySet())
			add(new ResourceDescription(e.getKey(), e.getValue().getInfoProvider().getTitle(req)));
		if (sort)
			Collections.sort(this);
	}

	/**
	 * Bean constructor.
	 */
	public ChildResourceDescriptions() {
		super();
	}
}
