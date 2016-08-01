/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.tests;

import java.util.*;

/**
 * A large POJO object.
 */
@SuppressWarnings("serial")
public class LargePojo {
	public A1Map a1Map;
	public A1List a1List;
	public A1[] a1Array;

	public static LargePojo create() {
		LargePojo a = new LargePojo();
		a.a1Map = new A1Map();
		a.a1List = new A1List();
		for (int i = 0; i < 20000; i++) {
			a.a1Map.put(String.valueOf(i), new A1());
			a.a1List.add(new A1());
		}
		a.a1Array = a.a1List.toArray(new A1[0]);
		return a;
	}

	public static class A1 {
		public String f1 = "a123456789b123456789c123456789d123456789e123456789f123456789g123456789h123456789i123456789j123456789";
	}

	public static class A1Map extends LinkedHashMap<String,A1> {}

	public static class A1List extends LinkedList<A1> {}
}
