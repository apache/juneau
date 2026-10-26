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
import {GROUPS, PROJECTS} from '@site/src/data/projects';
import {expect, test} from '@playwright/test';

const RAIL = 'Juneau projects';

test.describe('desktop', () => {
  test.use({viewport: {width: 1280, height: 800}});

  test('a project page marks its rail link and keeps one navbar and footer', async ({page}) => {
    await page.goto('/about/marshall/');
    await expect(page.getByRole('navigation', {name: RAIL}).getByRole('link', {name: 'Juneau Marshall'}))
      .toHaveAttribute('aria-current', 'page');
    await expect(page).toHaveTitle(/Juneau Marshall/);
    await expect(page.locator('nav.navbar')).toHaveCount(1);
    await expect(page.locator('footer')).toHaveCount(1);
  });

  test('/about/ redirects to the home page', async ({page}) => {
    await page.goto('/about/');
    await expect(page).toHaveURL(/\/$/);
    await expect(page.locator('#projects')).toBeVisible();
  });

  test('the rail Overview link goes to the home page', async ({page}) => {
    await page.goto('/about/marshall/');
    await page.getByRole('navigation', {name: RAIL}).getByRole('link', {name: 'Overview'}).click();
    await expect(page).toHaveURL(/\/$/);
  });

  for (const [slug, gettingStarted, docs] of [
    ['marshall', 'GettingStartedSerializer', 'JuneauMarshall'],
    ['rest-server', 'GettingStartedRestApp', 'JuneauRestServer'],
    ['rest-client', 'GettingStartedClient', 'NextGenRestClient'],
    ['beans', 'GettingStarted', 'JuneauBean'],
  ]) {
    test(`${slug} has Get started and Documentation buttons`, async ({page}) => {
      await page.goto(`/about/${slug}/`);
      await expect(page.locator('main').getByRole('link', {name: 'Get started'}))
        .toHaveAttribute('href', new RegExp(`/docs/topics/${gettingStarted}/?$`));
      await expect(page.locator('main').getByRole('link', {name: 'Documentation', exact: true}))
        .toHaveAttribute('href', new RegExp(`/docs/topics/${docs}/?$`));
    });
  }

  test('the home page lists a card per project and the footer Projects link scrolls to them', async ({page}) => {
    await page.goto('/');
    await expect(page.locator('.juneau-project-cards a')).toHaveCount(11);
    await page.locator('footer').getByRole('link', {name: 'Projects'}).click();
    await expect(page).toHaveURL(/#projects$/);
    await expect(page.locator('#projects')).toBeInViewport();
  });

  test('the home hero has a Documentation button', async ({page}) => {
    await page.goto('/');
    await expect(page.locator('main').getByRole('link', {name: 'Documentation', exact: true}).first())
      .toHaveAttribute('href', /JuneauEcosystemOverview/);
  });

  test('the rail link color differs between light and dark themes', async ({page}) => {
    const link = () => page.getByRole('navigation', {name: RAIL}).getByRole('link', {name: 'Juneau Marshall'});
    await page.goto('/about/marshall/');
    const light = await link().evaluate((el) => getComputedStyle(el).color);
    await page.addInitScript(() => {
      localStorage.setItem('theme', 'dark');
      document.documentElement.setAttribute('data-theme', 'dark');
    });
    await page.goto('/about/marshall/');
    const dark = await link().evaluate((el) => getComputedStyle(el).color);
    expect(dark).not.toBe(light);
  });
});

test.describe('mobile with JavaScript', () => {
  test.use({viewport: {width: 390, height: 844}});

  test('the select replaces the list and navigates', async ({page}) => {
    await page.goto('/about/rest-server/');
    await expect(page.locator('select')).toHaveValue('rest-server');
    const list = page.getByRole('navigation', {name: RAIL}).locator('ul').first();
    await expect(list).toBeHidden();
    await page.locator('select').selectOption({label: 'Juneau Config'});
    await expect(page).toHaveURL(/\/about\/config\/$/);
  });

  test('the home project cards are stacked', async ({page}) => {
    await page.goto('/');
    const headings = page.locator('.juneau-project-cards h3');
    await expect(headings).toHaveCount(3);
    const xs = await headings.evaluateAll((els) => els.map((el) => el.getBoundingClientRect().x));
    expect(new Set(xs).size).toBe(1);
  });
});

test.describe('mobile without JavaScript', () => {
  test.use({viewport: {width: 390, height: 844}, javaScriptEnabled: false});

  test('the list stays visible and there is no select', async ({page}) => {
    await page.goto('/about/rest-server/');
    await expect(page.getByRole('navigation', {name: RAIL}).locator('ul').first()).toBeVisible();
    await expect(page.locator('select')).toHaveCount(0);
  });

  test('the /about/ stub carries a meta refresh and noindex', async ({request}) => {
    // The raw HTML, because a browser acts on the refresh before the page can be inspected.
    const html = await (await request.get('/about/')).text();
    expect(html).toMatch(/<meta[^>]*http-equiv="refresh"[^>]*content="0; url=\/"/);
    expect(html).toMatch(/<meta[^>]*name="robots"[^>]*content="noindex"/);
  });

  test('the /about/ stub reaches the home page without JavaScript', async ({page}) => {
    await page.goto('/about/');
    await expect(page).toHaveURL(/\/$/);
    await expect(page.locator('#projects')).toBeVisible();
  });
});
