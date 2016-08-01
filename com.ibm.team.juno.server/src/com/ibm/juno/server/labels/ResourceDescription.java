/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.labels;

import com.ibm.juno.core.dto.*;
import com.ibm.juno.server.*;

/**
 * Shortcut label for child resources.  Typically used in router resources.
 *
 * <h6 class='topic'>Examples</h6>
 * <p class='bcode'>
 * 	<jc>// Instead of this...</jc>
 * 	<jk>new</jk> NameDescription(<jk>new</jk> Link(<js>"httpTool"</js>, uri + <js>"/httpTool"</js>), <js>"HTTP request test client"</js>);
 *
 * 	<jc>// ...use this simpler equivalent...</jc>
 * 	<jk>new</jk> ResourceLink(uri, <js>"httpTool"</js>, <js>"HTTP request test client"</js>);
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
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
