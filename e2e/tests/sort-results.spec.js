import { expect, test } from "@playwright/test";

/**
 * E2E test za sortiranje rezultatov.
 * Uporabnik naredi iskanje in preklopi sortiranje med Date posted in Compatibility.
 */
test("user can sort search results", async ({ page }) => {
  test.setTimeout(120_000);

  await page.goto("/motion");

  await page
    .getByPlaceholder(/i am looking for a junior/i)
    .fill("I am looking for a junior Java developer role in Maribor, hybrid.");

  await page.getByRole("button", { name: /search jobs/i }).click();

  await page.locator(".job-stream").scrollIntoViewIfNeeded();
  await expect(page.locator(".motion-job").first()).toBeVisible({
    timeout: 120_000,
  });

  const datePostedButton = page.getByRole("button", { name: /date posted/i });
  const compatibilityButton = page.getByRole("button", { name: /compatibility/i });

  await datePostedButton.click();
  await expect(datePostedButton).toHaveClass(/active/);

  await compatibilityButton.click();
  await expect(compatibilityButton).toHaveClass(/active/);
});
