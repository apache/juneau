/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Zero-pads a number to two digits for the compact Zulu timestamp formatter.
function pad(n) { return n < 10 ? '0' + n : '' + n; }

// A small, framework-agnostic named-renderer registry: any element marked up with
// data-render="<name>" gets its content replaced by the matching renderer in RM_RENDERERS, using its
// data-value as the raw input. New renderers can be added to the map without touching the apply loop.
globalThis.RM_RENDERERS = {
    'ts-zulu': function (el, raw) {
        if (!raw) return;
        const d = new Date(raw);
        if (Number.isNaN(d.getTime())) return;
        el.textContent = d.getUTCFullYear() + '-' + pad(d.getUTCMonth() + 1) + '-' + pad(d.getUTCDate()) + ' '
            + pad(d.getUTCHours()) + ':' + pad(d.getUTCMinutes()) + 'Z';
        el.title = 'Local: ' + d.toLocaleString();
    }
};

// Applies every not-yet-rendered data-render element under root, marking each done so a later call
// (e.g. after a partial DOM update) never re-renders the same element twice.
globalThis.rmApplyRenderers = function (root) {
    const scope = root || document;
    const els = scope.querySelectorAll('[data-render]:not([data-rendered])');
    for (const el of els) {
        const renderer = RM_RENDERERS[el.dataset.render];
        if (renderer) renderer(el, el.dataset.value);
        el.dataset.rendered = 'true';
    }
};

document.addEventListener('DOMContentLoaded', function () {
    rmApplyRenderers(document);
});
