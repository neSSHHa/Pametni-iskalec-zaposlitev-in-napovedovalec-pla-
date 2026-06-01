import { expect, test } from "@playwright/test";

/**
 * E2E test za salary prediction.
 * Po iskanju preveri, da se prikaže kartica z oceno plače.
 * Ta test uporabljaj samo, če salary-service zanesljivo teče in vrača rezultat.
 */
test("salary prediction appears after search", async ({ page }) => {
  test.setTimeout(120_000);

  await page.goto("/motion");

  await page
    .getByPlaceholder(/i am looking for a junior/i)
    .fill("I am looking for a junior Java developer role in Austria, hybrid, salary from 1800 EUR, Java and SQL.");

  await page.getByRole("button", { name: /search jobs/i }).click();

  await page.locator(".job-stream").scrollIntoViewIfNeeded();
  await expect(page.locator(".motion-job").first()).toBeVisible({
    timeout: 120_000,
  });

  await expect(page.locator('[aria-label="Predicted salary range"]')).toBeVisible({ timeout: 120_000 });
});
