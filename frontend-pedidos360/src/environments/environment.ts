export const environment = {
  production: false,
  msalConfig: {
    clientId: '8a1f476e-1fc8-4240-aaf0-0af6b309712e',
    authority: 'https://login.microsoftonline.com/dafcc960-7ca2-4ecb-a012-71e2e1929fa5',
    redirectUri: 'http://localhost:4200/auth/callback',
    postLogoutRedirectUri: 'http://localhost:4200/login'
  },
  apiConfig: {
    scopes: ['api://8a1f476e-1fc8-4240-aaf0-0af6b309712e/access_as_user'],
    bffEndpoint: 'http://localhost:8080/api'
  }
};

