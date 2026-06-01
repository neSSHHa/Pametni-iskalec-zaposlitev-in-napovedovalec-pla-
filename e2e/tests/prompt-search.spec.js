import { expect, test } from "@playwright/test";

/**
 * Full E2E test za AI prompt search.
 * Test uporablja Thinking AI mode in preveri realen tok:
 * frontend -> backend -> AI service/OpenRouter -> job matching -> results UI.
 */
test("user can search jobs with a prompt using AI", async ({ page }) => {
  test.setTimeout(120_000);

  await page.goto("/motion");

  await expect(
    page.getByRole("button", { name: /thinking ai/i })
  ).toBeVisible();

  await page
    .getByPlaceholder(/i am looking for a junior/i)
    .fill("I am looking for a junior Java developer role in Maribor, hybrid, with Java, Spring Boot and SQL.");

  await page.getByRole("button", { name: /search jobs/i }).click();

  await expect(page).toHaveURL(/\/motion-prompt/);

  await expect(page.getByRole("heading", { name: /found/i })).toBeVisible({ timeout: 120_000 });
  await page.locator(".job-stream").scrollIntoViewIfNeeded();
  await expect(page.locator(".motion-job").first()).toBeVisible({ timeout: 120_000 });
});
