import { test, expect } from "@playwright/test";

const ADMIN_KEY = process.env.E2E_ADMIN_KEY || "test-admin-key";

test.describe("Admin Upload Flow", () => {
  test("should navigate to admin page and show login form", async ({ page }) => {
    // Navigate directly to admin route
    await page.goto("/admin");

    // Should show admin login form
    await expect(page.getByText("Panel Admin KP")).toBeVisible();
    await expect(page.getByText("Masukkan kunci admin untuk melanjutkan")).toBeVisible();
  });

  test("should login with admin key and see upload page", async ({ page }) => {
    // Skip if no valid admin key is configured
    if (ADMIN_KEY === "test-admin-key") {
      test.skip(true, "Requires valid E2E_ADMIN_KEY environment variable");
    }

    await page.goto("/admin");

    // Enter admin key
    const keyInput = page.getByLabel("Input kunci admin");
    await keyInput.fill(ADMIN_KEY);
    const loginButton = page.getByLabel("Tombol masuk admin");
    await loginButton.click();

    // Wait for upload page content
    await expect(page.getByText("Unggah Dokumen Baru")).toBeVisible();
    await expect(page.getByText("Dokumen Teringest")).toBeVisible();
    await expect(page.getByLabel("Tombol keluar admin")).toBeVisible();

    // Verify "Kembali ke Chat" link exists
    await expect(page.getByLabel("Kembali ke chat")).toBeVisible();
  });

  test("should show upload button disabled when no file selected", async ({ page }) => {
    // Skip if no valid admin key is configured
    if (ADMIN_KEY === "test-admin-key") {
      test.skip(true, "Requires valid E2E_ADMIN_KEY environment variable");
    }

    await page.goto("/admin");

    // Login first
    const keyInput = page.getByLabel("Input kunci admin");
    await keyInput.fill(ADMIN_KEY);
    await page.getByLabel("Tombol masuk admin").click();

    // Wait for upload page
    await expect(page.getByText("Unggah Dokumen Baru")).toBeVisible();

    // Verify upload button is disabled when no file selected
    const uploadButton = page.getByLabel("Tombol unggah dokumen");
    await expect(uploadButton).toBeDisabled();
  });

  test("should logout and return to login form", async ({ page }) => {
    // Skip if no valid admin key is configured
    if (ADMIN_KEY === "test-admin-key") {
      test.skip(true, "Requires valid E2E_ADMIN_KEY environment variable");
    }

    await page.goto("/admin");

    // Login first
    const keyInput = page.getByLabel("Input kunci admin");
    await keyInput.fill(ADMIN_KEY);
    await page.getByLabel("Tombol masuk admin").click();

    // Wait for upload page
    await expect(page.getByText("Panel Admin KP")).toBeVisible();

    // Click logout
    const logoutButton = page.getByLabel("Tombol keluar admin");
    await logoutButton.click();

    // Should return to login page
    await expect(page.getByText("Panel Admin KP")).toBeVisible();
    await expect(keyInput).toBeVisible();
  });

  test("should navigate back to chat from admin", async ({ page }) => {
    // Skip if no valid admin key is configured
    if (ADMIN_KEY === "test-admin-key") {
      test.skip(true, "Requires valid E2E_ADMIN_KEY environment variable");
    }

    await page.goto("/admin");

    // Login first
    const keyInput = page.getByLabel("Input kunci admin");
    await keyInput.fill(ADMIN_KEY);
    await page.getByLabel("Tombol masuk admin").click();

    // Wait for upload page
    await expect(page.getByText("Unggah Dokumen Baru")).toBeVisible();

    // Click "Kembali ke Chat" link
    await page.getByLabel("Kembali ke chat").click();

    // Should navigate to chat page
    await expect(page.getByText("Chat dengan Kredit Pintar")).toBeVisible();
  });
});