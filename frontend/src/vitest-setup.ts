import "@testing-library/jest-dom";
import { beforeEach, beforeAll } from "vitest";

const sessionStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn(),
};

beforeAll(() => {
  Object.defineProperty(globalThis, "sessionStorage", {
    value: sessionStorageMock,
    writable: true,
  });
});

beforeEach(() => {
  vi.clearAllMocks();
  sessionStorageMock.getItem.mockReturnValue(null);
});