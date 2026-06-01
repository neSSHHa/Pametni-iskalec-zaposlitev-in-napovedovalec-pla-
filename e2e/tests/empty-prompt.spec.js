import { expect, test } from "@playwright/test";

/**
 * E2E test za osnovno validacijo prompt iskanja.
 * Uporabnik ne more poslati praznega prompta.
 * To je majhen, stabilen test, ki preverja uporabniško zaščito pred napačnim vnosom.
 */
test("search button is disabled when prompt is empty", async ({ page }) => {
  await page.goto("/motion");

  const textbox = page.getByPlaceholder(/i am looking for a junior/i);
  const searchButton = page.getByRole("button", { name: /search jobs/i });

  await expect(searchButton).toBeDisabled();

  await textbox.fill("Junior developer in Slovenia");
  await expect(searchButton).toBeEnabled();

  await textbox.fill("");
  await expect(searchButton).toBeDisabled();
});
