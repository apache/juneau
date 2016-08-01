/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule 
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.csv;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import com.ibm.juno.core.csv.*;
import com.ibm.juno.core.serializer.*;

public class CT_Csv {

	//====================================================================================================
	// testBasic
	//====================================================================================================
	@Test
	public void testBasic() throws Exception {
		List<A> l = new LinkedList<A>();
		l.add(new A("b1",1));
		l.add(new A("b2",2));
		
		WriterSerializer s = new CsvSerializer();
		String r;
		
		r = s.serialize(l);
		
		assertEquals("b,c\nb1,1\nb2,2\n", r);
	}

	public static class A {
		public String b;
		public int c;
		
		public A(String b, int c) {
			this.b = b;
			this.c = c;
		}
	}
}