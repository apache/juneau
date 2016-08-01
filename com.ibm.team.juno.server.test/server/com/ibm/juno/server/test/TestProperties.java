/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.test;

import static java.lang.String.*;

import java.io.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testProperties",
	properties={
		@Property(name="A1",value="a1"),
		@Property(name="A2",value="a2"),
		@Property(name="foo",value="bar"),
		@Property(name="bar",value="baz"),
		@Property(name="R1a",value="$R{requestURI}"),
		@Property(name="R1b",value="$R{requestParentURI}"),
		@Property(name="R2",value="$R{foo}"),
		@Property(name="R3",value="$R{$R{foo}}"),
		@Property(name="R4",value="$R{A1}"),
		@Property(name="R5",value="$R{A2}"),
		@Property(name="R6",value="$R{C}"),
	}
)
public class TestProperties extends RestServletDefault {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Properties defined on method.
	//====================================================================================================
	@RestMethod(name="GET", path="/testPropertiesDefinedOnMethod",
		properties={
			@Property(name="B1",value="b1"),
			@Property(name="B2",value="b2")
		},
		serializers=PropertySerializer1.class
	)
	public void testPropertiesDefinedOnMethod(RestResponse res) {
		res.setProperty("A2", "c");
		res.setProperty("B2", "c");
		res.setProperty("C", "c");
		res.setOutput(null);
	}

	@Produces({"application/json","text/json"})
	public static class PropertySerializer1 extends WriterSerializer {
		@Override /* Serializer */
		protected void doSerialize(Object output, Writer out, SerializerContext ctx) throws IOException, SerializeException {
			ObjectMap p = ctx.getProperties();
			out.write(format("A1=%s,A2=%s,B1=%s,B2=%s,C=%s,R1a=%s,R1b=%s,R2=%s,R3=%s,R4=%s,R5=%s,R6=%s",
				p.get("A1"), p.get("A2"), p.get("B1"), p.get("B2"), p.get("C"),
				p.get("R1a"), p.get("R1b"), p.get("R2"), p.get("R3"), p.get("R4"), p.get("R5"), p.get("R6")));
		}
	}

	//====================================================================================================
	// Make sure attributes/parameters/headers are available through ctx.getProperties().
	//====================================================================================================
	@RestMethod(name="GET", path="/testProperties/{A}", serializers=PropertySerializer2.class)
	public void testProperties(RestResponse res) {
		res.setOutput(null);
	}

	@Produces({"application/json","text/json"})
	public static class PropertySerializer2 extends WriterSerializer {
		@Override /* Serializer */
		protected void doSerialize(Object output, Writer out, SerializerContext ctx) throws IOException, SerializeException {
			ObjectMap p = ctx.getProperties();
			out.write(format("A=%s,P=%s,H=%s", p.get("A"), p.get("P"), p.get("h")));
		}
	}

}
