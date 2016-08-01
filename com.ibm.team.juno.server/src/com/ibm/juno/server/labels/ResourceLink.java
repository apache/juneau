/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.labels;

import java.text.*;

import com.ibm.juno.core.dto.*;
import com.ibm.juno.server.*;

/**
 * A simple link to a child of a parent resource.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class ResourceLink extends Link {

	/**
	 * Constructor.
	 *
	 * @param req The HTTP request from the parent resource.
	 * @param childPath The child resource path.
	 * @param args Optional {@link MessageFormat}-style arguments in the child path.
	 */
	public ResourceLink(RestRequest req, String childPath, Object...args) {
		super(getName(getPath(childPath,args)), getHref(req, getPath(childPath,args)));
	}

	/**
	 * Constructor.
	 *
	 * @param label The label for the link.
	 * @param req The HTTP request from the parent resource.
	 * @param childPath The child resource path.
	 * @param args Optional {@link MessageFormat}-style arguments in the child path.
	 */
	public ResourceLink(String label, RestRequest req, String childPath, Object...args) {
		super(label, getHref(req, getPath(childPath,args)));
	}

	private static String getName(String childPath) {
		String s = childPath;
		if (childPath.indexOf('/') == -1)
			s = childPath;
		else
			s = childPath.substring(childPath.lastIndexOf('/')+1);
		return RestUtils.decode(s);
	}

	private static String getHref(RestRequest req, String childPath) {
		return req.getServletURIBuilder().append('/').append(childPath).toString();
	}

	private static String getPath(String childPath, Object...args) {
		if (args.length > 0)
			childPath = MessageFormat.format(childPath, args);
		return childPath;
	}
}
