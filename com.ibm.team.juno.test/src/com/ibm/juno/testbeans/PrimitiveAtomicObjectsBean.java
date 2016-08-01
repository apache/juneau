/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.testbeans;

import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * Test bean fields of type AtomicInteger and AtomicLong.
 * Note that Jena parsers cannot handle these types, so we only test non-Jena parsers.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@SuppressWarnings({"serial"})
public class PrimitiveAtomicObjectsBean {

	// primitive objects
	public AtomicInteger poAtomicInteger;
	public AtomicLong poAtomicLong;

	// uninitialized primitive objects
	public AtomicInteger pouAtomicInteger;
	public AtomicLong pouAtomicLong;

	// primitive object arrays
	public AtomicInteger[][] poaAtomicInteger;
	public AtomicLong[][] poaAtomicLong;

	// primitive object arrays
	public AtomicInteger[][] poauAtomicInteger;
	public AtomicLong[][] poauAtomicLong;

	// Anonymous list of primitives (types not erased on objects
	public List<AtomicInteger[]> poalAtomicInteger;
	public List<AtomicLong[]> poalAtomicLong;

	// Regular list of primitives (types erased on objects)
	public List<AtomicInteger[]> polAtomicInteger;
	public List<AtomicLong[]> polAtomicLong;

	public PrimitiveAtomicObjectsBean init() {
		// primitive objects
		poAtomicInteger = new AtomicInteger(1);
		poAtomicLong = new AtomicLong(2);

		// primitive object arrays
		poaAtomicInteger = new AtomicInteger[][]{{new AtomicInteger(1)}, {new AtomicInteger(2)}, null};
		poaAtomicLong = new AtomicLong[][]{{new AtomicLong(1)}, {new AtomicLong(2)}, null};

		// Anonymous list of primitives
		poalAtomicInteger = new ArrayList<AtomicInteger[]>() {{
			add(new AtomicInteger[]{new AtomicInteger(1)}); add(null);
		}};
		poalAtomicLong = new ArrayList<AtomicLong[]>() {{
			add(new AtomicLong[]{new AtomicLong(1)}); add(null);
		}};

		// Regular list of primitives
		polAtomicInteger = new ArrayList<AtomicInteger[]>();
		polAtomicInteger.add(new AtomicInteger[]{new AtomicInteger(1)});
		polAtomicInteger.add(null);
		polAtomicLong = new ArrayList<AtomicLong[]>();
		polAtomicLong.add(new AtomicLong[]{new AtomicLong(1)});
		polAtomicLong.add(null);

		return this;
	}
}