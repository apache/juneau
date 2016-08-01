/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp. 
 *******************************************************************************/
package com.ibm.juno.core.test.utils;

import static org.junit.Assert.*;

import java.io.*;

import org.junit.*;

import com.ibm.juno.core.parser.*;

public class CT_ParserReader {

	//====================================================================================================
	// test 
	//====================================================================================================
	@Test
	public void test() throws Exception {		
		ParserReader r = new ParserReader(new StringReader("abc123"), 128);
		try {
			assertEquals('a', r.read());
			r.unread();
			assertEquals('a', r.read());
			assertEquals('b', r.read());
			r.unread();
			assertEquals("bc", r.read(2));
			assertEquals('1', r.read());
			r.unread();
			r.read();
			assertEquals('2', r.peek());
			assertEquals('2', r.peek());
			assertEquals('2', r.read());
			assertEquals('3', r.read());
			assertEquals(-1, r.read());
			assertEquals(-1, r.read());
		} finally {
			r.close();
		}
	}
}