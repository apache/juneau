/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule 
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.testbeans;

import java.net.*;
import java.net.URI;

import com.ibm.juno.core.annotation.*;

public class TestURI {
	@BeanProperty(beanUri=true)
	public String f0 = "foo/bar";
	
	public URI f1, f2, f3;
	
	@com.ibm.juno.core.annotation.URI
	public String f4, f5, f6;
	
	public URL f7;
	
	public TestURIb f8;
	
	
	@com.ibm.juno.core.annotation.URI
	public String getF9() {
		return "foo/bar";
	}

	public TestURI() throws Exception {
		f1 = new URI("foo/bar");
		f2 = new URI("/foo/bar");
		f3 = new URI("http://www.ibm.com/foo/bar");
		f4 = "foo/bar";
		f5 = "/foo/bar";
		f6 = "http://www.ibm.com/foo/bar";
		f7 = new URL("http://www.ibm.com/foo/bar");
		f8 = new TestURIb();
	}

	@com.ibm.juno.core.annotation.URI
	public static class TestURIb {
		@Override /* Object */
		public String toString() {
			return "foo/bar";
		}
	}
}

