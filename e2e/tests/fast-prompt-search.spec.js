import { expect, test } from "@playwright/test";

/**
 * E2E test za Instant fast mode.
 * Preveri, da aplikacija lahko izvede iskanje brez AI/OpenRouter klica,
 * z uporabo lokalnega fast parserja na backendu.
 */
test("user can search jobs with instant fast mode", async ({ page }) => {
  test.setTimeout(120_000);

  await page.goto("/motion");

  await page.getByRole("button", { name: /instant fast/i }).click();

  await page
    .getByPlaceholder(/i am looking for a junior/i)
    .fill("Software developer jobs in Slovenia remote");

  await page.getByRole("button", { name: /search jobs/i }).click();

  await expect(page).toHaveURL(/\/motion-prompt/, { timeout: 120_000 });

  await expect(page.getByRole("heading", { name: /found/i })).toBeVisible({
    timeout: 120_000,
  });

  await page.locator(".job-stream").scrollIntoViewIfNeeded();

  await expect(page.locator(".motion-job").first()).toBeVisible({
    timeout: 120_000,
  });
});
