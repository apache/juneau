/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2016. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test;

import static org.junit.Assert.*;

import org.junit.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.json.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;

public class CT_PojoFilter {

	//====================================================================================================
	// Test same type
	// If you define a PojoFilter<String,String> filter, then it should be invoked on all strings.
	//====================================================================================================
	@Test
	public void testSameType() throws Exception {
		JsonSerializer s = JsonSerializer.DEFAULT_LAX.clone().addFilters(AFilter.class);
		JsonParser p = JsonParser.DEFAULT.clone().addFilters(AFilter.class);
		String r;

		r = s.serialize("foobar");
		assertEquals("'xfoobarx'", r);
		r = p.parse(r, String.class);
		assertEquals("foobar", r);

		ObjectMap m = new ObjectMap("{foo:'bar'}");
		r = s.serialize(m);
		assertEquals("{xfoox:'xbarx'}", r);
	}

	public static class AFilter extends PojoFilter<String,String> {
		@Override
		public String filter(String o) throws SerializeException {
			return "x" + o + "x";
		}

		@Override
		public String unfilter(String f, ClassMeta<?> hint) throws ParseException {
			return f.substring(1, f.length()-1);
		}
	}
}
