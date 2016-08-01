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

import java.util.*;

import org.junit.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.utils.*;

public class CT_FilteredMap {

	Map<?,?> m3;

	//====================================================================================================
	// testBasic
	//====================================================================================================
	@Test
	public void testBasic() throws Exception {
		ObjectMap m = new ObjectMap("{a:'1',b:'2'}");
		FilteredMap<String,Object> m2 = new FilteredMap<String,Object>(m, new String[]{"a"});

		assertObjectEquals("{a:'1'}", m2);

		m2.entrySet().iterator().next().setValue("3");
		assertObjectEquals("{a:'3'}", m2);

		try { m3 = new FilteredMap<String,String>(null, new String[0]); fail(); } catch (IllegalArgumentException e) {}
		try { m3 = new FilteredMap<String,Object>(m, null); fail(); } catch (IllegalArgumentException e) {}
	}
}
