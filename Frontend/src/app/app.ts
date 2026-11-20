import { Component, OnInit, signal } from '@angular/core';
import { MsalService } from '@azure/msal-angular';
import { PedidosPage } from './pages/pedidos';
import { NuevoPedidoPage } from './pages/nuevo-pedido';
import { ReportesPage } from './pages/reportes';
import { AuditoriaPage } from './pages/auditoria';

type Pestana = 'pedidos' | 'nuevo' | 'reportes' | 'auditoria';

@Component({
  selector: 'app-root',
  imports: [PedidosPage, NuevoPedidoPage, ReportesPage, AuditoriaPage],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit {
  protected readonly title = signal('Pedidos360');

  isIframe = false;
  isLoggedIn = signal(false);
  userName = signal('');
  userRoles = signal<string[]>([]);
  pestana = signal<Pestana>('pedidos');

  readonly pestanas: { id: Pestana; texto: string }[] = [
    { id: 'pedidos', texto: '📦 Pedidos' },
    { id: 'nuevo', texto: '➕ Nuevo pedido' },
    { id: 'reportes', texto: '📊 Reportes' },
    { id: 'auditoria', texto: '🧾 Auditoría' },
  ];

  constructor(private msalService: MsalService) {}

  ngOnInit(): void {
    this.isIframe = window !== window.parent && !window.opener;
    this.verificarSesion();
  }

  verificarSesion() {
    const cuentas = this.msalService.instance.getAllAccounts();
    if (cuentas.length > 0) {
      const cuenta = cuentas[0];
      this.msalService.instance.setActiveAccount(cuenta);
      this.isLoggedIn.set(true);
      this.userName.set(cuenta.name || cuenta.username);
      const roles = cuenta.idTokenClaims?.roles;
      if (roles) this.userRoles.set(roles as string[]);
    } else {
      this.isLoggedIn.set(false);
    }
  }

  login() {
    this.msalService.loginRedirect({ scopes: ['d1a052a1-66c7-4847-b5e2-46fdb86c94f6/.default'] });
  }

  logout() {
    this.msalService.logoutRedirect();
  }
}
