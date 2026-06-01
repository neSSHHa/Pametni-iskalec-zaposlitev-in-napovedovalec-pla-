import { expect, test } from "@playwright/test";

/**
 * E2E test za prehod iz rezultatov na statistiko.
 * Uporabnik naredi iskanje, nato odpre statistiko za rezultate in se vrne nazaj.
 * To preverja, da aplikacija ohrani kontekst rezultatov med navigacijo.
 */
test("user can view statistics from search results and return to jobs", async ({ page }) => {
  test.setTimeout(120_000);

  await page.goto("/motion");

  await page
    .getByPlaceholder(/i am looking for a junior/i)
    .fill("I am looking for a junior Java developer role in Maribor, hybrid.");

  await page.getByRole("button", { name: /search jobs/i }).click();

  await page.locator(".job-stream").scrollIntoViewIfNeeded();
  await expect(page.locator(".motion-job").first()).toBeVisible({ timeout: 120_000 });

  await page.getByRole("button", { name: /view statistics/i }).click();

  await expect(page).toHaveURL(/\/analytics/);
  await expect(page.locator(".analytics-section")).toBeVisible();

  await page.getByRole("button", { name: /back to jobs/i }).click();

  await expect(page).toHaveURL(/\/motion-prompt/);
  await page.locator(".job-stream").scrollIntoViewIfNeeded();
  await expect(page.locator(".motion-job").first()).toBeVisible();
});
