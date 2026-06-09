import { Before, After, Given, When, Then } from '@cucumber/cucumber';
import { chromium } from '@playwright/test';
import assert from 'node:assert/strict';

const baseUrl = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173';

Before(async function () {
  this.browser = await chromium.launch({ headless: true });
  this.context = await this.browser.newContext();
  this.page = await this.context.newPage();
  this.hostName = `Host-${Date.now()}`;
});

After(async function () {
  await this.context?.close();
  await this.browser?.close();
});

Given(/I open the home ?page/, async function () {
  await this.page.goto(baseUrl, { waitUntil: 'networkidle' });
  await this.page.getByText('ENTRA EN LA ARENA').waitFor({ state: 'visible' });
});

When('I go to multiplayer mode', async function () {
  await this.page.locator('article:has-text("MULTIJUGADOR")').getByRole('button', { name: 'INICIAR PARTIDA' }).click();
  await this.page.getByText('SELECCION DE PARTIDA').waitFor({ state: 'visible' });
});

When('I create a room with my host name', async function () {
  await this.page.locator('#host-name').fill(this.hostName);
  await this.page.getByRole('button', { name: 'CREAR NUEVA SALA' }).click();
});

Then('I should see the lobby with a room code', async function () {
  const roomCodeButton = this.page.locator('.multiplayer-lobby-code');
  await roomCodeButton.waitFor({ state: 'visible' });

  const roomCodeText = await roomCodeButton.textContent();
  assert.ok(roomCodeText, 'Expected room code text to exist');
  assert.match(roomCodeText.trim(), /^SALA\s+[A-Z0-9]{4,}$/);
});

Then('I should see myself as host in the room', async function () {
  const summary = this.page.locator('.multiplayer-lobby-summary');
  await summary.waitFor({ state: 'visible' });

  await this.page.getByText(`Host: ${this.hostName}`).waitFor({ state: 'visible' });
});
