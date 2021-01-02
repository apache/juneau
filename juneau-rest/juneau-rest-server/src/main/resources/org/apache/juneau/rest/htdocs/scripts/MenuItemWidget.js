/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 *
 ***************************************************************************************************************************/
var popupItem;

function closePopup() {
	if (popupItem != null) {
		popupItem.nextElementSibling.classList.remove('popup-show');
	}
}

function menuClick(item) {
	closePopup();
	item.nextElementSibling.classList.add('popup-show');
	popupItem = item;
}

window.onclick = function(event) {
	if (popupItem != null && popupItem != event.target && ! popupItem.nextElementSibling.contains(event.target)) {
		closePopup();
	}
};