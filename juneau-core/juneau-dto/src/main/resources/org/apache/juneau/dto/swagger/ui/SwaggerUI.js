/*
 ***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
 * with the License.  You may obtain a copy of the License at                                                              *
 *                                                                                                                         *
 *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
 *                                                                                                                         *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
 * specific language governing permissions and limitations under the License.                                              *
 ***************************************************************************************************************************
*/

/* Toggles visibility of tag-group block */
function toggleTagBlock(e) {
	e = e.parentNode;
	var isOpen = e.classList.contains("tag-block-open");
	if (isOpen) {
		e.classList.add("tag-block-closed");
		e.classList.remove("tag-block-open");
	} else {
		e.classList.add("tag-block-open");
		e.classList.remove("tag-block-closed");
	}
}

/* Toggles visibility of operation block */
function toggleOpBlock(e) {
	e = e.parentNode;
	var isOpen = e.classList.contains("op-block-open");
	if (isOpen) {
		e.classList.add("op-block-closed");
		e.classList.remove("op-block-open");
	} else {
		e.classList.add("op-block-open");
		e.classList.remove("op-block-closed");
	}
}

/* Shows an example */
function selectExample(e) {
	var dataName = e.getAttribute("data-name");
	var examplesNode = e.parentNode.parentNode;
	var lis = examplesNode.getElementsByTagName("li");
	var divs = examplesNode.getElementsByTagName("div");
	
	for (var i in lis) {
		var li = lis[i], div = divs[i];
		if (li.getAttribute("data-name") == dataName) {
			li.classList.add("active");
			div.classList.add("active");
		} else {
			li.classList.remove("active");
			div.classList.remove("active");
		}
	}
}
