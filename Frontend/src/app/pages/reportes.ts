import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { ApiService, Kpis, ProductoTop, StockCritico, mensajeError } from '../api.service';

@Component({
  selector: 'app-reportes',
  imports: [DecimalPipe],
  template: `
    <div class="barra">
      <h2>Reportes (calculados desde Kafka)</h2>
      <button class="btn" (click)="cargar()">🔄 Actualizar</button>
    </div>
    @if (error()) { <div class="alerta error">{{ error() }}</div> }

    @if (kpis(); as k) {
      <div class="kpis">
        <div class="kpi"><span>Pedidos</span><strong>{{ k.pedidos }}</strong></div>
        <div class="kpi"><span>Ingresos</span><strong>\${{ k.ingresos | number:'1.0-0' }}</strong></div>
        <div class="kpi"><span>Ticket promedio</span><strong>\${{ k.ticketPromedio | number:'1.0-0' }}</strong></div>
        <div class="kpi"><span>Unidades vendidas</span><strong>{{ k.unidadesVendidas }}</strong></div>
      </div>
    }

    <div class="dos">
      <section>
        <h3>Productos más vendidos</h3>
        @if (top().length === 0) { <div class="vacio">Sin ventas todavía.</div> }
        @for (t of top(); track t.productoId) {
          <div class="barra-h">
            <span>{{ t.nombre }}</span>
            <div class="riel"><div class="relleno" [style.width.%]="porcentaje(t.unidades)"></div></div>
            <strong>{{ t.unidades }}</strong>
          </div>
        }
      </section>
      <section>
        <h3>Stock crítico (≤ 10)</h3>
        @if (critico().length === 0) { <div class="vacio">Ningún producto en nivel crítico.</div> }
        @for (s of critico(); track s.productoId) {
          <div class="fila-stock"><span>{{ s.nombre }}</span><span class="chip warn">{{ s.stockActual }} u.</span></div>
        }
      </section>
    </div>
  `,
})
export class ReportesPage implements OnInit {
  private api = inject(ApiService);
  kpis = signal<Kpis | null>(null);
  top = signal<ProductoTop[]>([]);
  critico = signal<StockCritico[]>([]);
  error = signal('');

  ngOnInit() { this.cargar(); }
  cargar() {
    this.error.set('');
    const fallo = (e: any) => this.error.set(mensajeError(e));
    this.api.kpis().subscribe({ next: (d) => this.kpis.set(d), error: fallo });
    this.api.topProductos(5).subscribe({ next: (d) => this.top.set(d), error: fallo });
    this.api.stockCritico(10).subscribe({ next: (d) => this.critico.set(d), error: fallo });
  }
  porcentaje(u: number) {
    const max = Math.max(...this.top().map((t) => t.unidades), 1);
    return Math.round((u / max) * 100);
  }
}
