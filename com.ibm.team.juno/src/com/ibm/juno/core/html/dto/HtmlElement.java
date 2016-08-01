/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.html.dto;

import com.ibm.juno.core.html.*;
import com.ibm.juno.core.html.annotation.*;

/**
 * Superclass for all HTML elements.
 * <p>
 * These are beans that when serialized using {@link HtmlSerializer} generate
 * valid XHTML elements.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Html(asXml=true)
public abstract class HtmlElement {}
