/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule 
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.filters;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import com.ibm.juno.core.filters.*;
import com.ibm.juno.core.json.*;
import com.ibm.juno.core.serializer.*;

public class CT_EnumerationFilter {

	//====================================================================================================
	// test 
	//====================================================================================================
	@Test
	public void test() throws Exception {		
		WriterSerializer s = new JsonSerializer.Simple().addFilters(EnumerationFilter.class);
		Vector<String> v = new Vector<String>(Arrays.asList(new String[]{"foo","bar","baz"}));
		Enumeration<String> e = v.elements();
		assertEquals("['foo','bar','baz']", s.serialize(e));
	}
}