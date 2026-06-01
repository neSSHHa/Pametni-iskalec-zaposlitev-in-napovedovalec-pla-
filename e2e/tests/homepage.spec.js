import { expect, test } from "@playwright/test";

/**
 * Smoke E2E test.
 * Preveri, da se aplikacija odpre in da je začetna stran uporabniku pravilno prikazana.
 * To ni poslovni flow, ampak osnovna potrditev, da frontend deluje v browserju.
 */
test("homepage loads with the main search UI", async ({ page }) => {
  await page.goto("/motion");

  await expect(page.getByRole("link", { name: /job radar/i })).toBeVisible();
  await expect(page.getByRole("link", { name: /home/i })).toBeVisible();
  await expect(page.getByRole("link", { name: /statistics/i })).toBeVisible();

  await expect(
    page.getByRole("heading", { name: /upload your cv or write a prompt/i })
  ).toBeVisible();

  await expect(page.getByText("Upload CV").first()).toBeVisible();
  await expect(page.getByText(/prompt search/i)).toBeVisible();
  await expect(page.getByPlaceholder(/i am looking for a junior/i)).toBeVisible();
  await expect(page.getByRole("button", { name: /search jobs/i })).toBeDisabled();

  await expect(page.getByRole("heading", { name: /how it works/i })).toBeVisible();
});
