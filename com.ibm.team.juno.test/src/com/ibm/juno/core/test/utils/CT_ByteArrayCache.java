/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.utils;

import static com.ibm.juno.core.test.TestUtils.*;
import static org.junit.Assert.*;

import java.io.*;

import org.junit.*;

import com.ibm.juno.core.utils.*;

public class CT_ByteArrayCache {

	//====================================================================================================
	// testBasic
	//====================================================================================================
	@Test
	public void testBasic() throws Exception {
		ByteArrayCache bac = new ByteArrayCache();
		byte[] b;

		b = bac.cache(new byte[]{1,2,3});
		assertObjectEquals("[1,2,3]", b);
		assertEquals(1, bac.size());

		b = bac.cache(new byte[]{1,2,3});
		assertObjectEquals("[1,2,3]", b);
		assertEquals(1, bac.size());

		b = bac.cache(new byte[]{1,2,3,4});
		assertObjectEquals("[1,2,3,4]", b);
		b = bac.cache(new byte[]{1,2});
		assertObjectEquals("[1,2]", b);
		assertEquals(3, bac.size());

		b = bac.cache(new byte[]{});
		assertObjectEquals("[]", b);

		b = bac.cache((byte[])null);
		assertNull(b);

		b = bac.cache((InputStream)null);
		assertNull(b);

		b = bac.cache(new ByteArrayInputStream(new byte[]{1,2,3}));
		assertObjectEquals("[1,2,3]", b);
		assertEquals(4, bac.size());
	}
}
