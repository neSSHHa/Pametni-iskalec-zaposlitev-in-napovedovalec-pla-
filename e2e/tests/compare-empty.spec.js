import { expect, test } from "@playwright/test";

/**
 * E2E test za prazno Compare stran.
 * Preveri, da uporabnik lahko odpre primerjavo brez izbranih služb
 * in dobi jasen empty state.
 */
test("compare page shows empty state when no jobs are selected", async ({ page }) => {
  await page.goto("/compare");

  await expect(page.getByRole("heading", { name: /compare jobs/i })).toBeVisible();
  await expect(page.getByRole("heading", { name: /no jobs selected/i })).toBeVisible();
  await expect(page.getByText(/choose up to two jobs/i)).toBeVisible();
  await expect(page.getByRole("button", { name: /go to results/i })).toBeVisible();
});
