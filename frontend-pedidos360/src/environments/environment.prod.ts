export const environment = {
  production: true,
  msalConfig: {
    clientId: '8a1f476e-1fc8-4240-aaf0-0af6b309712e',
    authority: 'https://login.microsoftonline.com/dafcc960-7ca2-4ecb-a012-71e2e1929fa5',
    redirectUri: 'https://thankful-river-06284c40f.5.azurestaticapps.net/auth/callback',
    postLogoutRedirectUri: 'https://thankful-river-06284c40f.5.azurestaticapps.net/login'
  },
  apiConfig: {
    scopes: ['api://8a1f476e-1fc8-4240-aaf0-0af6b309712e/access_as_user'],
    bffEndpoint: 'https://apim-pedidos360.azure-api.net'
  }
};