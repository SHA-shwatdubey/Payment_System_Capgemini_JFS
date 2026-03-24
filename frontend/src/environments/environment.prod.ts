export const environment = {
  production: true,
  // In Docker, the frontend runs in the browser, so it still needs to reach
  // the API gateway via the host-mapped port (8062).
  apiBaseUrl: 'http://localhost:8062'
};
