/*
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
 */

import {useEffect, useState, type ReactNode} from 'react';
import Link from '@docusaurus/Link';
import BrowserOnly from '@docusaurus/BrowserOnly';
import {useHistory} from '@docusaurus/router';
import {GROUPS, PROJECTS} from '@site/src/data/projects';
import styles from './styles.module.css';

type Props = {current?: string};

function MobileSelect({current}: Props): ReactNode {
  const history = useHistory();
  return (
    <label className={styles.selectLabel}>
      <span className={styles.selectText}>Project</span>
      <select
        className={styles.select}
        value={current ?? ''}
        onChange={(e) => history.push(e.target.value ? `/about/${e.target.value}/` : '/')}>
        <option value="">Overview</option>
        {GROUPS.map((g) => (
          <optgroup key={g.id} label={g.label}>
            {PROJECTS.filter((p) => p.group === g.id).map((p) => (
              <option key={p.slug} value={p.slug}>{p.name}</option>
            ))}
          </optgroup>
        ))}
      </select>
    </label>
  );
}

export default function ProjectRail({current}: Props): ReactNode {
  const [hydrated, setHydrated] = useState(false);
  // Hide the list on mobile only once JavaScript is running, so without it the list stays usable at every width.
  useEffect(() => setHydrated(true), []);
  return (
    <nav
      aria-label="Juneau projects"
      className={`juneau-project-rail ${styles.rail}`}
      data-hydrated={hydrated ? 'true' : undefined}>
      <BrowserOnly>{() => <MobileSelect current={current} />}</BrowserOnly>
      <div className={styles.list}>
        <Link to="/" className={styles.link}>
          Overview
        </Link>
        {GROUPS.map((g) => (
          <div key={g.id}>
            <div className={styles.groupLabel}>{g.label}</div>
            <ul className={styles.items}>
              {PROJECTS.filter((p) => p.group === g.id).map((p) => (
                <li key={p.slug}>
                  <Link
                    to={`/about/${p.slug}/`}
                    className={styles.link}
                    aria-current={p.slug === current ? 'page' : undefined}>
                    {p.name}
                  </Link>
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>
    </nav>
  );
}
