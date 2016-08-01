/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp. 
 *******************************************************************************/
package com.ibm.juno.core.test;

import static org.junit.Assert.*;

import java.io.*;

import org.junit.*;

import com.ibm.juno.core.parser.*;

public class CT_ParserReader {
	
	//====================================================================================================
	// testBasic
	//====================================================================================================
	@Test
	public void testBasic() throws Exception {
		String t = "01234567890123456789012345678901234567890123456789";
		
		// Min buff size is 20.
		ParserReader pr = new ParserReader(new StringReader(t), 1);
		String r = read(pr);
		assertEquals(t, r);
		pr.close();

		pr = new ParserReader(new StringReader(t), 1);
		pr.read();
		pr.unread();
		r = read(pr);
		assertEquals(t, r);
		pr.close();

		pr = new ParserReader(new StringReader(t), 1);
		assertEquals('0', (char)pr.peek());
		assertEquals('0', (char)pr.peek());
		r = read(pr);
		assertEquals(t, r);
		
		pr = new ParserReader(new StringReader(t), 1);
		pr.read();
		pr.unread();
		try {
			pr.unread();
			fail("Exception expected");
		} catch (IOException e) {
			// Good
		}
	}

	//====================================================================================================
	// testMarking
	//====================================================================================================
	@Test
	public void testMarking() throws Exception {
		String t = "a123456789b123456789c123456789d123456789e123456789f123456789g123456789h123456789i123456789j123456789";
		String r = null;
		
		// Min buff size is 20.
		ParserReader pr = new ParserReader(new StringReader(t), 1);
		read(pr, 5);
		pr.mark();
		read(pr, 10);
		r = pr.getMarked();
		assertEquals("56789b1234", r);
		r = read(pr);
		assertEquals("56789c123456789d123456789e123456789f123456789g123456789h123456789i123456789j123456789", r);

		// Force doubling of buffer size
		pr = new ParserReader(new StringReader(t), 1);
		read(pr, 5);
		pr.mark();
		read(pr, 20);
		r = pr.getMarked();
		assertEquals("56789b123456789c1234", r);
		r = read(pr);
		assertEquals("56789d123456789e123456789f123456789g123456789h123456789i123456789j123456789", r);
	}
	
	//====================================================================================================
	// testReadStrings
	//====================================================================================================
	@Test
	public void testReadStrings() throws Exception {
		String t = "a123456789b123456789c123456789d123456789e123456789f123456789g123456789h123456789i123456789j123456789";
		
		// Min buff size is 20.
		ParserReader pr = new ParserReader(new StringReader(t), 1);
		assertEquals("a123456789", pr.read(10));
		pr.mark();
		assertEquals("b123456789c123456789", pr.read(20));
		assertEquals("d123456789e123456789f123456789", pr.read(30));
		assertEquals("123456789c123456789d123456789e123456789f12345678", pr.getMarked(1, -1));
		assertEquals("g123456789h123456789i123456789j123456789", pr.read(100));
		assertEquals("", pr.read(100));
		pr.close();
	}

	//====================================================================================================
	// testReplace
	//====================================================================================================
	@Test
	public void testReplace() throws Exception {
		String t = "a123456789b123456789c123456789d123456789e123456789f123456789g123456789h123456789i123456789j123456789";
		
		// Min buff size is 20.
		ParserReader pr = new ParserReader(new StringReader(t), 1);
		assertEquals("a123456789", pr.read(10));
		pr.mark();
		assertEquals("b123456789", pr.read(10));
		pr.replace('x');
		assertEquals("c123456789", pr.read(10));
		assertEquals("b12345678xc123456789", pr.getMarked());
		pr.close();

		pr = new ParserReader(new StringReader(t), 1);
		assertEquals("a123456789", pr.read(10));
		pr.mark();
		assertEquals("b123456789", pr.read(10));
		pr.replace('x', 5);
		assertEquals("c123456789", pr.read(10));
		assertEquals("b1234xc123456789", pr.getMarked());
		pr.close();
	}

	//====================================================================================================
	// testDelete
	//====================================================================================================
	@Test
	public void testDelete() throws Exception {
		String t = "a123456789b123456789c123456789d123456789e123456789f123456789g123456789h123456789i123456789j123456789";
		
		// Min buff size is 20.
		ParserReader pr = new ParserReader(new StringReader(t), 1);
		assertEquals("a123456789", pr.read(10));
		pr.mark();
		assertEquals("b123456789", pr.read(10));
		pr.delete();
		assertEquals("c123456789", pr.read(10));
		assertEquals("b12345678c123456789", pr.getMarked());
		pr.close();

		pr = new ParserReader(new StringReader(t), 1);
		assertEquals("a123456789", pr.read(10));
		pr.mark();
		assertEquals("b123456789", pr.read(10));
		pr.delete(5);
		assertEquals("c123456789", pr.read(10));
		assertEquals("b1234c123456789", pr.getMarked());
		pr.close();
	}

	//====================================================================================================
	// Utility methods
	//====================================================================================================

	private String read(ParserReader r) throws IOException {
		return read(r, Integer.MAX_VALUE);
	}
	
	private String read(ParserReader r, int length) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			int c = r.read();
			if (c == -1)
				return sb.toString();
			sb.append((char)c);
		}
		return sb.toString();
	}
}
