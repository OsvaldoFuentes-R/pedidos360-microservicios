import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ApiService, Pedido, mensajeError } from '../api.service';

@Component({
  selector: 'app-pedidos',
  imports: [DatePipe, DecimalPipe],
  template: `
    <div class="barra">
      <h2>Pedidos</h2>
      <label class="auto"><input type="checkbox" [checked]="auto()" (change)="alternarAuto()"> Actualizar cada 5 s</label>
      <button class="btn" (click)="cargar()">🔄 Actualizar</button>
    </div>
    @if (error()) { <div class="alerta error">{{ error() }}</div> }
    @if (pedidos().length === 0 && !error()) {
      <div class="vacio">Aún no hay pedidos. Crea uno en la pestaña <strong>Nuevo pedido</strong>.</div>
    } @else {
      <div class="tabla-wrap">
        <table>
          <thead><tr><th>#</th><th>Cliente</th><th>Detalle</th><th>Total</th><th>Fecha</th><th>Ticket</th><th>Estado</th></tr></thead>
          <tbody>
            @for (p of pedidos(); track p.id) {
              <tr>
                <td class="id">#{{ p.id }}</td>
                <td>{{ p.cliente }}</td>
                <td>{{ p.producto }}</td>
                <td>\${{ p.total | number:'1.0-0' }}</td>
                <td>{{ p.fecha | date:'dd/MM/yy HH:mm' }}</td>
                <td>{{ p.ticket || '—' }}</td>
                <td><span class="chip" [class]="clase(p.estado)">{{ p.estado }}</span></td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }
  `,
})
export class PedidosPage implements OnInit, OnDestroy {
  private api = inject(ApiService);
  pedidos = signal<Pedido[]>([]);
  error = signal('');
  auto = signal(true);
  private timer?: any;

  ngOnInit() { this.cargar(); this.programar(); }
  ngOnDestroy() { clearInterval(this.timer); }

  cargar() {
    this.api.pedidos().subscribe({
      next: (d) => { this.pedidos.set([...d].sort((a, b) => b.id - a.id)); this.error.set(''); },
      error: (e) => this.error.set(mensajeError(e)),
    });
  }
  alternarAuto() { this.auto.update((v) => !v); this.programar(); }
  private programar() {
    clearInterval(this.timer);
    if (this.auto()) this.timer = setInterval(() => this.cargar(), 5000);
  }
  clase(estado: string) {
    const e = (estado || '').toUpperCase();
    if (e.includes('ENTREG')) return 'chip ok';
    if (e.includes('RUTA') || e.includes('TRANSITO') || e.includes('TRÁNSITO')) return 'chip warn';
    return 'chip info';
  }
}
