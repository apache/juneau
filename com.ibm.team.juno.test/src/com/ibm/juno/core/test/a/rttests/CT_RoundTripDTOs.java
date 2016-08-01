/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.a.rttests;

import static com.ibm.juno.core.test.TestUtils.*;

import org.junit.*;

import com.ibm.juno.core.dto.jsonschema.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.test.dto.jsonschema.*;


/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
@SuppressWarnings("hiding")
public class CT_RoundTripDTOs extends RoundTripTest {

	public CT_RoundTripDTOs(String label, WriterSerializer s, ReaderParser p, int flags) throws Exception {
		super(label, s, p, flags);
	}

	//====================================================================================================
	// com.ibm.juno.core.test.dto.jsonschema
	//====================================================================================================
	@Test
	public void testJsonSchema1() throws Exception {
		Schema s = CT_JsonSchema.getTest1();
		Schema s2 = roundTrip(s, Schema.class);
		assertEqualObjects(s, s2);
	}

	@Test
	public void testJsonSchema2() throws Exception {
		Schema s = CT_JsonSchema.getTest2();
		Schema s2 = roundTrip(s, Schema.class);
		assertEqualObjects(s, s2);
	}
}
