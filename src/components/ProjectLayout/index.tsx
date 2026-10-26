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

import type {ReactNode} from 'react';
import Link from '@docusaurus/Link';
import {PageMetadata} from '@docusaurus/theme-common';
import ProjectRail from '@site/src/components/ProjectRail';
import {projectBySlug, topicForModule} from '@site/src/data/projects';
import styles from './styles.module.css';

type Props = {slug: string; children: ReactNode};

// Wraps a project page body under /about/<slug>/: the project rail beside the content, and the project's name,
// tagline, Get started and Documentation buttons, module list (each module links to its topic when MODULE_DOCS has
// one) and Read-more links.  The MDX page wrapper already provides the page container and <main>, so this adds
// neither; every about page sets hide_table_of_contents.
export default function ProjectLayout({slug, children}: Props): ReactNode {
  const project = projectBySlug(slug);
  if (project === undefined) {
    // An error thrown during static generation fails `npm run build`, unlike a broken link.
    throw new Error(`ProjectLayout: no project with slug "${slug}" in src/data/projects.ts`);
  }
  const gettingStarted = project.gettingStarted ?? 'GettingStarted';
  return (
    <div className={styles.layout}>
      <PageMetadata title={project.name} description={project.tagline} />
      <ProjectRail current={slug} />
      <div className={styles.content}>
        <h1>{project.name}</h1>
        <p className={styles.lead}>{project.tagline}</p>
        <div className={styles.buttons}>
          <Link className="button button--primary button--lg" to={`/docs/topics/${gettingStarted}/`}>
            Get started
          </Link>
          <Link className="button button--secondary button--lg" to={`/docs/topics/${project.docs[0]}/`}>
            Documentation
          </Link>
        </div>
        {children}
        <p className={styles.modules}>
          <strong>Modules:</strong>{' '}
          {project.modules.map((m, i) => (
            <span key={m}>
              {i > 0 && ', '}
              {topicForModule(m) === undefined ? (
                <code>{m}</code>
              ) : (
                <Link to={`/docs/topics/${topicForModule(m)}/`}>
                  <code>{m}</code>
                </Link>
              )}
            </span>
          ))}
        </p>
        <p className={styles.readMore}>
          {project.docs.map((d, i) => (
            <span key={d}>
              {i > 0 && ' '}
              <Link to={`/docs/topics/${d}/`}>{i === 0 ? 'Read more →' : `Also: ${d}`}</Link>
            </span>
          ))}
        </p>
      </div>
    </div>
  );
}
