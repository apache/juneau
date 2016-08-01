/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.test;

import java.util.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;

/**
 * JUnit automated testcase resource.
 * Validates that resource bundles can be defined on both parent and child classes.
 */
@RestResource(
	path="/testMessages",
	messages="TestMessages",
	filters={
		TestMessages.ResourceBundleFilter.class
	}
)
public class TestMessages extends RestServletDefault {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Return contents of resource bundle.
	//====================================================================================================
	@RestMethod(name="GET", path="/test")
	public Object test(@Messages ResourceBundle nls) {
		return nls;
	}


	@SuppressWarnings("serial")
	@RestResource(
		path="/testMessages2",
		messages="TestMessages2"
	)
	public static class TestMessages2 extends TestMessages {}

	public static class ResourceBundleFilter extends PojoFilter<ResourceBundle,ObjectMap> {
		@Override /* Filter */
		public ObjectMap filter(ResourceBundle o) throws SerializeException {
			ObjectMap m = new ObjectMap();
			for (String k : o.keySet())
				m.put(k, o.getString(k));
			return m;
		}
	}
}
