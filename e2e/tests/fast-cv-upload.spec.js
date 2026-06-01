import { expect, test } from "@playwright/test";

/**
 * E2E test za CV upload v Instant fast načinu.
 * Preveri, da backend lahko iz CV-ja z lokalnim parserjem izdela filter
 * in vrne priporočene službe brez zunanjega AI/OpenRouter klica.
 */
test("user can upload CV with instant fast mode", async ({ page }) => {
  test.setTimeout(120_000);

  await page.goto("/motion");

  await page.getByRole("button", { name: /instant fast/i }).click();

  await page.setInputFiles('input[type="file"]', "fixtures/test-cv.pdf");

  await expect(page).toHaveURL(/\/motion-cv/, { timeout: 120_000 });

  await expect(page.getByRole("heading", { name: /found/i })).toBeVisible({
    timeout: 120_000,
  });

  await page.locator(".job-stream").scrollIntoViewIfNeeded();

  await expect(page.locator(".motion-job").first()).toBeVisible({
    timeout: 120_000,
  });
});
