import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach, beforeAll } from "vitest";

beforeAll(() => {
  Element.prototype.scrollTo = Element.prototype.scrollTo || function scrollTo() {};
});

afterEach(() => {
  cleanup();
});