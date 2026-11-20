export const environment = {
  production: false,
  // Local: el BFF corre en Docker en el puerto 8080. Para AWS cambia por 'http://<ip-o-api-gateway>/api'
  apiUrl: 'http://localhost:8080/api',
  msalConfig: {
    auth: {
      clientId: 'd1a052a1-66c7-4847-b5e2-46fdb86c94f6',
      authority: 'https://pedido360seba2026.ciamlogin.com/39eb9615-c754-4ef1-96f8-4f61494df782',
      redirectUri: 'http://localhost:4200'
    }
  }
};
