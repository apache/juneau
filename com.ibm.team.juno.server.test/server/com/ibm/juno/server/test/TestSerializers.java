/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.test;

import static com.ibm.juno.server.annotation.Inherit.*;

import java.io.*;

import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testSerializers",
	serializers=TestSerializers.TestSerializerA.class
)
public class TestSerializers extends RestServletDefault {
	private static final long serialVersionUID = 1L;

	@Produces("text/a")
	public static class TestSerializerA extends WriterSerializer {
		@Override /* Serializer */
		protected void doSerialize(Object o, Writer out, SerializerContext ctx) throws IOException, SerializeException {
			out.write("text/a - " + o);
		}
	}

	@Produces("text/b")
	public static class TestSerializerB extends WriterSerializer {
		@Override /* Serializer */
		protected void doSerialize(Object o, Writer out, SerializerContext ctx) throws IOException, SerializeException {
			out.write("text/b - " + o);
		}
	}

	//====================================================================================================
	// Serializer defined on class.
	//====================================================================================================
	@RestMethod(name="GET", path="/testSerializerOnClass")
	public String testSerializerOnClass() {
		return "test1";
	}

	//====================================================================================================
	// Serializer defined on method.
	//====================================================================================================
	@RestMethod(name="GET", path="/testSerializerOnMethod", serializers=TestSerializerB.class)
	public String testSerializerOnMethod() {
		return "test2";
	}

	//====================================================================================================
	// Serializer overridden on method.
	//====================================================================================================
	@RestMethod(name="GET", path="/testSerializerOverriddenOnMethod", serializers={TestSerializerB.class,TestSerializerC.class}, serializersInherit=SERIALIZERS)
	public String testSerializerOverriddenOnMethod() {
		return "test3";
	}

	@Produces("text/a")
	public static class TestSerializerC extends WriterSerializer {
		@Override /* Serializer */
		protected void doSerialize(Object o, Writer out, SerializerContext ctx) throws IOException, SerializeException {
			out.write("text/c - " + o);
		}
	}

	//====================================================================================================
	// Serializer with different Accept than Content-Type.
	//====================================================================================================
	@RestMethod(name="GET", path="/testSerializerWithDifferentMediaTypes", serializers={TestSerializerD.class}, serializersInherit=SERIALIZERS)
	public String testSerializerWithDifferentMediaTypes() {
		return "test4";
	}

	@Produces(value={"text/a","text/d"},contentType="text/d")
	public static class TestSerializerD extends WriterSerializer {
		@Override /* Serializer */
		protected void doSerialize(Object o, Writer out, SerializerContext ctx) throws IOException, SerializeException {
			out.write("text/d - " + o);
		}
	}

	//====================================================================================================
	// Check for valid 406 error response.
	//====================================================================================================
	@RestMethod(name="GET", path="/test406")
	public String test406() {
		return "test406";
	}
}
