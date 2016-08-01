/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule 
 * Contract with IBM Corp.
 *******************************************************************************/

@XmlSchema(
	prefix="p1",
	xmlNs={
		@XmlNs(prefix="p1",namespaceURI="http://p1"),	
		@XmlNs(prefix="p2",namespaceURI="http://p2"),	
		@XmlNs(prefix="p3",namespaceURI="http://p3(unused)"),	
		@XmlNs(prefix="c1",namespaceURI="http://c1"),
		@XmlNs(prefix="f1",namespaceURI="http://f1")
	}
)
package com.ibm.juno.core.test.xml.xml1c;
import com.ibm.juno.core.xml.annotation.*;

