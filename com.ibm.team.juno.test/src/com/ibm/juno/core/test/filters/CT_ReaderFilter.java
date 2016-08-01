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

import java.io.*;
import java.util.*;

import org.junit.*;

import com.ibm.juno.core.filters.*;
import com.ibm.juno.core.json.*;
import com.ibm.juno.core.serializer.*;

public class CT_ReaderFilter {

	//====================================================================================================
	// test
	//====================================================================================================
	@Test
	public void test() throws Exception {		
		WriterSerializer s = new JsonSerializer.Simple().addFilters(ReaderFilter.Json.class);

		Reader r;
		Map<String,Object> m;
		
		r = new StringReader("{foo:'bar',baz:'quz'}");
		m = new HashMap<String,Object>();
		m.put("X", r);
		assertEquals("{X:{foo:'bar',baz:'quz'}}", s.serialize(m));
		
		s.addFilters(ReaderFilter.Xml.class);
		r = new StringReader("<object><foo type='string'>bar</foo><baz type='string'>quz</baz></object>");
		m = new HashMap<String,Object>();
		m.put("X", r);
		assertEquals("{X:{foo:'bar',baz:'quz'}}", s.serialize(m));
	}
}