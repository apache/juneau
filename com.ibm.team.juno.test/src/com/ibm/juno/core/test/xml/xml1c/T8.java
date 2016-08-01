/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule 
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.xml.xml1c;

import com.ibm.juno.core.xml.annotation.*;

@Xml(prefix="p2")
public class T8 {
	
	public int f1 = 1;
	
	@Xml(prefix="p1") public int f2 = 2;
	
	@Xml(prefix="c1") public int f3 = 3;
	
	@Xml(prefix="f1") 
	public int f4 = 4;

	public boolean equals(T8 x) {
		return x.f1 == f1 && x.f2 == f2 && x.f3 == f3 && x.f4 == f4;
	}
}
