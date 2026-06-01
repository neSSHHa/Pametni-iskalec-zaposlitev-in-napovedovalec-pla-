import { expect, test } from "@playwright/test";

/**
 * E2E test za pregled podrobnosti službe.
 * Uporabnik najprej naredi iskanje, nato klikne prvo službo in preveri,
 * da se odpre dialog s podrobnostmi.
 */
test("user can open job details from search results", async ({ page }) => {
  test.setTimeout(120_000);

  await page.goto("/motion");

  await page
    .getByPlaceholder(/i am looking for a junior/i)
    .fill("I am looking for a junior Java developer role in Maribor, hybrid.");

  await page.getByRole("button", { name: /search jobs/i }).click();

  const firstJob = page.locator(".motion-job").first();
  await page.locator(".job-stream").scrollIntoViewIfNeeded();
  await expect(firstJob).toBeVisible({ timeout: 120_000 });

  await firstJob.click();

  const dialog = page.locator(".job-details-page");
  await expect(dialog).toBeVisible();

  await expect(dialog.locator("section").filter({ hasText: /job description/i })).toBeVisible();
  await expect(dialog.locator("dt").filter({ hasText: /^Salary$/ })).toBeVisible();
  await expect(dialog.locator("dt").filter({ hasText: /^Education$/ })).toBeVisible();

  await dialog.getByRole("button", { name: /close job details/i }).click();
  await expect(dialog).not.toBeVisible();
});
