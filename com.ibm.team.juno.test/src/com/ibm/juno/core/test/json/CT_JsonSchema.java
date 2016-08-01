/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule 
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.json;

import static org.junit.Assert.*;

import org.junit.*;

import com.ibm.juno.core.json.*;

public class CT_JsonSchema {

	//====================================================================================================
	// Primitive objects
	//====================================================================================================
	@Test
	public void testBasic() throws Exception {

		JsonSchemaSerializer s = JsonSerializer.DEFAULT_LAX.getSchemaSerializer();

		Object o = new String();
		assertEquals("{type:'string',description:'java.lang.String'}", s.serialize(o));

		o = new Integer(123);
		assertEquals("{type:'number',description:'java.lang.Integer'}", s.serialize(o));
		
		o = new Float(123);
		assertEquals("{type:'number',description:'java.lang.Float'}", s.serialize(o));

		o = new Double(123);
		assertEquals("{type:'number',description:'java.lang.Double'}", s.serialize(o));

		o = Boolean.TRUE;
		assertEquals("{type:'boolean',description:'java.lang.Boolean'}", s.serialize(o));
	}
}