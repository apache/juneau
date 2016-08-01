/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.ini;

import java.io.*;

/**
 * Valid formats that can be passed to the {@link ConfigFile#serializeTo(Writer, ConfigFileFormat)} method.
 */
public enum ConfigFileFormat {
	/** Normal INI file format*/
	INI,

	/** Batch file with "set X" commands */
	BATCH,

	/** Shell script file with "export X" commands */
	SHELL;
}