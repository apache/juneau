/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.test;

import java.io.*;

import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testNlsProperty",
	serializers={TestNlsProperty.TestSerializer.class},
	properties={
		@Property(name="TestProperty",value="$L{key1}")
	},
	messages="TestNlsProperty"
)
public class TestNlsProperty extends RestServlet {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Test getting an NLS property defined on a class.
	//====================================================================================================
	@RestMethod(name="GET", path="/testInheritedFromClass")
	public String testInheritedFromClass() {
		return null;
	}

	//====================================================================================================
	// Test getting an NLS property defined on a method.
	//====================================================================================================
	@RestMethod(name="GET", path="/testInheritedFromMethod",
		properties={
			@Property(name="TestProperty",value="$L{key2}")
		}
	)
	public String testInheritedFromMethod() {
		return null;
	}

	@Produces("text/plain")
	public static class TestSerializer extends WriterSerializer {
		@Override /* Serializer */
		protected void doSerialize(Object o, Writer out, SerializerContext ctx) throws IOException, SerializeException {
			out.write(ctx.getProperties().getString("TestProperty"));
		}
	}
}
