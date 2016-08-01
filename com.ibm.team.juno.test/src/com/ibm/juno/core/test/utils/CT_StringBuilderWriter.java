/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.utils;

import static org.junit.Assert.*;

import org.junit.*;

import com.ibm.juno.core.utils.*;

public class CT_StringBuilderWriter {

	//====================================================================================================
	// Basic tests
	//====================================================================================================
	@Test
	public void test() throws Exception {
		StringBuilderWriter sbw = new StringBuilderWriter();
		sbw.write("abc");
		assertEquals("abc", sbw.toString());
		sbw.append("abc");
		assertEquals("abcabc", sbw.toString());
		sbw.write("abc", 1, 1);
		assertEquals("abcabcb", sbw.toString());
		sbw.append("abc", 1, 2);
		assertEquals("abcabcbb", sbw.toString());
		sbw.write((String)null);
		assertEquals("abcabcbbnull", sbw.toString());
		sbw.append((String)null);
		assertEquals("abcabcbbnullnull", sbw.toString());
		sbw.append((String)null,0,4);
		assertEquals("abcabcbbnullnullnull", sbw.toString());

		char[] buff = "abc".toCharArray();
		sbw = new StringBuilderWriter();
		sbw.write(buff, 0, buff.length);
		assertEquals("abc", sbw.toString());
		sbw.write(buff, 0, 0);
		assertEquals("abc", sbw.toString());

		try { sbw.write(buff, -1, buff.length); fail(); } catch (IndexOutOfBoundsException e) {}
		try { sbw.write(buff, buff.length+1, 0); fail(); } catch (IndexOutOfBoundsException e) {}
		try { sbw.write(buff, buff.length-1, 2); fail(); } catch (IndexOutOfBoundsException e) {}
		try { sbw.write(buff, 0, buff.length+1); fail(); } catch (IndexOutOfBoundsException e) {}
		try { sbw.write(buff, 0, -1); fail(); } catch (IndexOutOfBoundsException e) {}

		sbw.flush();
		sbw.close();
	}
}
