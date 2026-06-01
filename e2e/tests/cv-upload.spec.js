import { expect, test } from "@playwright/test";

/**
 * Full E2E test za CV upload flow.
 * Uporabnik naloži CV datoteko, sistem jo obdela in prikaže priporočene službe.
 * Za ta test mora obstajati testni CV v e2e/fixtures/test-cv.pdf.
 */
test("user can upload CV and see job matches", async ({ page }) => {
  test.setTimeout(120_000);

  await page.goto("/motion");

  await page.setInputFiles('input[type="file"]', "fixtures/test-cv.pdf");

  await expect(page).toHaveURL(/\/motion-cv/, { timeout: 120_000 });

  await expect(page.getByRole("heading", { name: /found/i })).toBeVisible({ timeout: 120_000 });
  await page.locator(".job-stream").scrollIntoViewIfNeeded();
  await expect(page.locator(".motion-job").first()).toBeVisible({ timeout: 120_000 });
});
