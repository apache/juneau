/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule 
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.xml.xml1b;

import com.ibm.juno.core.xml.annotation.*;

public class T3 {
	
	public int f1 = 1;
	
	@Xml(prefix="bar",namespace="http://bar") public int f2 = 2;
	
	private int f3 = 3;
	public int getF3() { return f3; }
	public void setF3(int f3) { this.f3 = f3; }
	
	private int f4 = 4;
	@Xml(prefix="baz",namespace="http://baz") public int getF4() { return f4; }
	public void setF4(int f4) { this.f4 = f4; }
	
	public boolean equals(T3 x) {
		return x.f1 == f1 && x.f2 == f2 && x.f3 == f3 && x.f4 == f4;
	}
}