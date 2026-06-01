import { expect, test } from "@playwright/test";

/**
 * E2E test za primerjavo služb.
 * Uporabnik poišče službe, doda dve v primerjavo in odpre Compare stran.
 * To preverja realen flow čez rezultate, state management in navigacijo.
 */
test("user can compare two jobs", async ({ page }) => {
  test.setTimeout(120_000);

  await page.goto("/motion");

  await page
    .getByPlaceholder(/i am looking for a junior/i)
    .fill("I am looking for a software developer role in Slovenia, remote or hybrid.");

  await page.getByRole("button", { name: /search jobs/i }).click();

  await page.locator(".job-stream").scrollIntoViewIfNeeded();
  await expect(page.locator(".motion-job").first()).toBeVisible({ timeout: 120_000 });

  const jobs = page.locator(".motion-job");
  await expect(jobs.nth(0)).toBeVisible({ timeout: 120_000 });
  await expect(jobs.nth(1)).toBeVisible({ timeout: 120_000 });

  await jobs.nth(0).getByRole("button", { name: /add job to comparison/i }).click();
  await expect(page.getByRole("button", { name: /compare jobs 1 of 2/i })).toBeVisible();

  await jobs.nth(1).getByRole("button", { name: /add job to comparison/i }).click();
  await expect(page.getByRole("button", { name: /compare jobs 2 of 2/i })).toBeVisible();

  await page.getByRole("button", { name: /compare jobs 2 of 2/i }).click();

  await expect(page).toHaveURL(/\/compare/);
  await expect(page.getByRole("heading", { name: /compare jobs/i })).toBeVisible();

  await expect(page.locator(".compare-section h2").filter({ hasText: /key differences/i })).toBeVisible();
  await expect(page.locator(".compare-section h2").filter({ hasText: /^Skills$/i })).toBeVisible();
  await expect(page.locator(".compare-section h2").filter({ hasText: /^Details$/i })).toBeVisible();
});
