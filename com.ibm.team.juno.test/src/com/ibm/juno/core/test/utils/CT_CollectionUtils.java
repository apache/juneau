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
import static com.ibm.juno.core.utils.CollectionUtils.*;
import static org.junit.Assert.*;

import org.junit.*;

import com.ibm.juno.core.*;

public class CT_CollectionUtils {

	//====================================================================================================
	// reverse(LinkedHashMap)
	//====================================================================================================
	@Test
	public void testReverse() throws Exception {
		assertNull(reverse(null));

		assertObjectEquals("{b:2,a:1}", reverse(new ObjectMap("{a:1,b:2}")));
		assertObjectEquals("{}", reverse(new ObjectMap("{}")));
	}
}
