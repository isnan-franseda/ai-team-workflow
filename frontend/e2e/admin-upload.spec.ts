import { test, expect } from "@playwright/test";

const ADMIN_KEY = process.env.E2E_ADMIN_KEY || "test-admin-key";

test.describe("Admin Upload Flow", () => {
  test("should upload document and see it in list", async ({ page }) => {
    // Skip if no valid admin key is configured
    if (ADMIN_KEY === "test-admin-key") {
      test.skip(true, "Requires valid E2E_ADMIN_KEY environment variable");
    }

    await page.goto("/");

    // Enter admin key
    const keyInput = page.getByLabel("Admin Key Input");
    await keyInput.fill(ADMIN_KEY);
    const loginButton = page.getByRole("button", { name: "Masuk" });
    await loginButton.click();

    // Wait for upload page
    await expect(page.getByText("Unggah Dokumen Baru")).toBeVisible();

    // Upload file
    const dropZone = page.getByLabel("Upload File Drop Zone");
    const file = "./e2e/fixtures/sample.pdf";
    await dropZone.setInputFiles(file);

    // Wait for file to appear in selected list
    await expect(page.getByText("sample.pdf")).toBeVisible();

    // Upload
    const uploadButton = page.getByLabel("Tombol Unggah Dokumen");
    await uploadButton.click();

    // Wait for result - mock returns success after ~1-2s
    await expect(page.getByText(/Berhasil|SUCCESS/i)).toBeVisible({ timeout: 10000 });

    // Verify document in list appears
    const docList = page.getByText("Dokumen Teringest");
    await expect(docList).toBeVisible();
  });

  test("should show error when no file selected", async ({ page }) => {
    await page.goto("/");

    const keyInput = page.getByLabel("Admin Key Input");
    await keyInput.fill(ADMIN_KEY);
    await page.getByRole("button", { name: "Masuk" }).click();

    // Verify upload button is disabled when no file selected
    const uploadButton = page.getByLabel("Tombol Unggah Dokumen");
    await expect(uploadButton).toBeDisabled();
  });

  test("should logout and return to login", async ({ page }) => {
    await page.goto("/");

    const keyInput = page.getByLabel("Admin Key Input");
    await keyInput.fill(ADMIN_KEY);
    await page.getByRole("button", { name: "Masuk" }).click();

    // Wait for upload page
    await expect(page.getByText("Panel Admin KP")).toBeVisible();

    // Click logout
    const logoutButton = page.getByLabel("Tombol Keluar");
    await logoutButton.click();

    // Should return to login page
    await expect(page.getByText("Panel Admin KP")).toBeVisible();
    await expect(keyInput).toBeVisible();
  });
});
