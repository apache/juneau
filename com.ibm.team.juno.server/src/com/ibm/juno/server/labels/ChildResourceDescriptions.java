/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.labels;

import java.util.*;

import com.ibm.juno.server.*;

/**
 * A POJO structure that describes the list of child resources associated with a resource.
 * <p>
 * Typically used in top-level GET methods of router resources to render a list of
 * 	available child resources.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class ChildResourceDescriptions extends LinkedList<ResourceDescription> {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param servlet The servlet that this bean describes.
	 * @param req The HTTP servlet request.
	 */
	public ChildResourceDescriptions(RestServlet servlet, RestRequest req) {
		this(servlet, req, false);
	}

	/**
	 * Constructor.
	 *
	 * @param servlet The servlet that this bean describes.
	 * @param req The HTTP servlet request.
	 * @param sort If <jk>true</jk>, list will be ordered by name alphabetically.
	 * 	Default is to maintain the order as specified in the annotation.
	 */
	public ChildResourceDescriptions(RestServlet servlet, RestRequest req, boolean sort) {
		String uri = req.getTrimmedRequestURI();
		for (Map.Entry<String,RestServlet> e : servlet.getChildResources().entrySet())
			add(new ResourceDescription(uri, e.getKey(), e.getValue().getLabel(req)));
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
