export const environment = {
  production: true,
  // In Docker, the frontend runs in the browser, so it still needs to reach
  // the API gateway via the host-mapped port (8062).
  // Detect current hostname dynamically so it works on both localhost and AWS/Cloud
  apiBaseUrl: `http://${window.location.hostname}:8062`
};
