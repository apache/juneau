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

import java.io.*;

import org.junit.*;

import com.ibm.juno.core.utils.*;

public class CT_ByteArrayInOutStream {

	//====================================================================================================
	// testBasic
	//====================================================================================================
	@Test
	public void testBasic() throws Exception {
		InputStream is = new ByteArrayInputStream("foobar".getBytes(IOUtils.UTF8));
		ByteArrayInOutStream baios = new ByteArrayInOutStream();
		IOPipe.create(is, baios).run();
		assertEquals("foobar", IOUtils.read(baios.getInputStream()));
	}
}
