// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              * 
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.rest;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Encapsulates class-level properties.
 * 
 * <p>
 * These are properties specified on a REST resource class through the following:
 * <ul>
 * 	<li class='ja'>{@link RestResource#properties()}
 * 	<li class='jm'>{@link RestContextBuilder#set(String, Object)}
 * 	<li class='jm'>{@link RestContextBuilder#set(Map)}
 * </ul>
 */
@SuppressWarnings("serial")
public class RestContextProperties extends ObjectMap {}
