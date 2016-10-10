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
package org.apache.juneau.server.labels;

import org.apache.juneau.dto.*;
import org.apache.juneau.server.*;

/**
 * Shortcut label for child resources.  Typically used in router resources.
 *
 * <h6 class='topic'>Example:</h6>
 * <p class='bcode'>
 * 	<jc>// Instead of this...</jc>
 * 	<jk>new</jk> NameDescription(<jk>new</jk> Link(<js>"httpTool"</js>, uri + <js>"/httpTool"</js>), <js>"HTTP request test client"</js>);
 *
 * 	<jc>// ...use this simpler equivalent...</jc>
 * 	<jk>new</jk> ResourceLink(uri, <js>"httpTool"</js>, <js>"HTTP request test client"</js>);
 * </p>
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public final class ResourceDescription extends NameDescription implements Comparable<ResourceDescription> {

	/**
	 * Constructor.
	 *
	 * @param rootUrl The root URI of the child resource (e.g. the URI of the parent resource).
	 * 		Must not end with <js>'/'</js>.
	 * 		Must be URL-Encoded.
	 * @param name The name of the child resource.
	 * 		This will be URL-encoded and appended onto the root URL to create the hyperlink for the resource.
	 * @param description The description of the child resource.
	 */
	public ResourceDescription(String rootUrl, String name, String description) {
		super(new Link(name, (rootUrl.equals("/") || rootUrl.isEmpty() ? "/" : rootUrl + "/") + RestUtils.encode(name)), description);
	}

	/**
	 * Constructor for resources that are children of a REST resource.
	 *
	 * @param req The HTTP request.
	 * @param childPath The childPath The path of the child resource relative to the servlet.
	 * @param description The description of the child resource.
	 */
	public ResourceDescription(RestRequest req, String childPath, String description) {
		super(new Link(calcName(childPath), calcHref(req, childPath)), description);
	}

	private static String calcName(String childPath) {
		return RestUtils.decode(childPath.indexOf('/') == -1 ? childPath : childPath.substring(childPath.lastIndexOf('/')+1));
	}

	private static String calcHref(RestRequest req, String childPath) {
		return req.getServletURIBuilder().append('/').append(childPath).toString();
	}

	/**
	 * Constructor.
	 *
	 * @param name The name of the child resource.
	 * @param description The description of the child resource.
	 */
	public ResourceDescription(String name, String description) {
		super(new Link(name, name), description);
	}

	/** No-arg constructor.  Used for JUnit testing of OPTIONS pages. */
	public ResourceDescription() {}

	@Override /* NameDescription */
	public Link getName() {
		return (Link)super.getName();
	}

	/**
	 * Overridden setter.
	 *
	 * @param name The new name.
	 */
	public void setName(Link name) {
		super.setName(name);
	}

	@Override /* Comparable */
	public int compareTo(ResourceDescription o) {
		return getName().compareTo(o.getName());
	}

	@Override /* Object */
	public boolean equals(Object o) {
		return (o instanceof ResourceDescription) && ((ResourceDescription)o).getName().equals(getName());
	}

	@Override /* Object */
	public int hashCode() {
		return getName().hashCode();
	}
}
