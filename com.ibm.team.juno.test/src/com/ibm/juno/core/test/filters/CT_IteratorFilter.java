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

public class CT_IteratorFilter {

	//====================================================================================================
	// test
	//====================================================================================================
	@Test
	public void test() throws Exception {		
		WriterSerializer s = new JsonSerializer.Simple().addFilters(IteratorFilter.class);
		
		// Iterators
		List<String> l = new ArrayList<String>(Arrays.asList(new String[]{"foo","bar","baz"}));
		Iterator<String> i = l.iterator();
		assertEquals("['foo','bar','baz']", s.serialize(i));
	}
}