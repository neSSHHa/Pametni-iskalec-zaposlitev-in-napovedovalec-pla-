import { expect, test } from "@playwright/test";

/**
 * E2E test za statistično stran.
 * Preveri, da uporabnik lahko pride na Statistics stran in da se analytics pogled prikaže.
 * Ne preverjamo točnih številk, ker so realni podatki lahko spremenljivi.
 */
test("user can open statistics page", async ({ page }) => {
  test.setTimeout(120_000);

  await page.goto("/motion");

  await page.getByRole("link", { name: /statistics/i }).click();

  await expect(page).toHaveURL(/\/analytics/);
  await expect(page.locator(".analytics-section")).toBeVisible({ timeout: 60_000 });

  await expect(page.locator(".analytics-loading-card")).not.toBeVisible({
    timeout: 120_000,
  });
});
