import { server } from './mocks/server.js';
import { afterAll, afterEach, beforeAll } from 'vitest';
import '@testing-library/jest-dom/vitest'; // Extends Vitest's expect with jest-dom matchers

// Establish API mocking before all tests.
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));

// Reset any request handlers that we may add during the tests,
// so they don't affect other tests.
afterEach(() => server.resetHandlers());

// Clean up after the tests are finished.
afterAll(() => server.close());

// Mock localStorage
const localStorageMock = (() => {
  let store = {};
  return {
    getItem: (key) => store[key] || null,
    setItem: (key, value) => {
      store[key] = value.toString();
    },
    removeItem: (key) => {
      delete store[key];
    },
    clear: () => {
      store = {};
    },
  };
})();

Object.defineProperty(window, 'localStorage', {
  value: localStorageMock,
});

// Mock IntersectionObserver for MUI components that might use it (like virtualized lists, etc.)
// This is a common requirement when testing MUI components.
const mockIntersectionObserver = class {
    constructor(callback, options) {}
    observe(target) {}
    unobserve(target) {}
    disconnect() {}
};

Object.defineProperty(window, 'IntersectionObserver', {
    value: mockIntersectionObserver,
    writable: true,
});

// Clean up localStorage after each test to ensure test isolation
afterEach(() => {
  window.localStorage.clear();
});
