export const environment = {
  production: false,
  // Use gateway-exposed local port inside the required 8050-8059 range.
  // Detect current hostname so it works locally and on IP addresses
  apiBaseUrl: `http://${window.location.hostname}:8062`
};

