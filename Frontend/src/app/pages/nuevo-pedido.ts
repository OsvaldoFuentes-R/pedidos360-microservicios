import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, Cliente, Producto, mensajeError } from '../api.service';

@Component({
  selector: 'app-nuevo-pedido',
  imports: [FormsModule, DecimalPipe],
  template: `
    <h2>Nuevo pedido</h2>
    <p class="sub">El pedido se publica en RabbitMQ (<code>pedido.registro</code>) y ms-pedidos lo registra de forma asíncrona.</p>
    @if (error()) { <div class="alerta error">{{ error() }}</div> }
    @if (ok()) { <div class="alerta ok">{{ ok() }}</div> }

    <label class="campo">Cliente
      <select [ngModel]="clienteId()" (ngModelChange)="clienteId.set(+$event)">
        <option [ngValue]="0" disabled>Selecciona un cliente…</option>
        @for (c of clientes(); track c.id) { <option [value]="c.id">{{ c.nombre }} — {{ c.email }}</option> }
      </select>
    </label>

    <div class="tabla-wrap">
      <table>
        <thead><tr><th>Producto</th><th>Precio</th><th>Stock</th><th>Cantidad</th></tr></thead>
        <tbody>
          @for (p of productos(); track p.id) {
            <tr>
              <td>{{ p.nombre }}<div class="peq">{{ p.categoria }}</div></td>
              <td>\${{ p.precio | number:'1.0-0' }}</td>
              <td [class.bajo]="p.stock <= 10">{{ p.stock }}</td>
              <td><input type="number" min="0" [max]="p.stock" class="cant"
                         [ngModel]="cantidades()[p.id] || 0" (ngModelChange)="poner(p.id, +$event)"></td>
            </tr>
          }
        </tbody>
      </table>
    </div>

    <div class="pie">
      <strong>Total estimado: \${{ total() | number:'1.0-0' }}</strong>
      <button class="btn primario" [disabled]="!puedeEnviar() || enviando()" (click)="enviar()">
        {{ enviando() ? 'Enviando…' : 'Crear pedido' }}
      </button>
    </div>
  `,
})
export class NuevoPedidoPage implements OnInit {
  private api = inject(ApiService);
  clientes = signal<Cliente[]>([]);
  productos = signal<Producto[]>([]);
  clienteId = signal(0);
  cantidades = signal<Record<number, number>>({});
  enviando = signal(false);
  error = signal('');
  ok = signal('');

  total = computed(() => this.productos().reduce((s, p) => s + p.precio * (this.cantidades()[p.id] || 0), 0));
  items = computed(() => Object.entries(this.cantidades())
    .filter(([, c]) => c > 0).map(([id, cantidad]) => ({ productoId: +id, cantidad })));
  puedeEnviar = computed(() => this.clienteId() > 0 && this.items().length > 0);

  ngOnInit() { this.cargarCatalogo(); }

  cargarCatalogo() {
    this.api.clientes().subscribe({ next: (d) => this.clientes.set(d), error: (e) => this.error.set(mensajeError(e)) });
    this.api.productos().subscribe({ next: (d) => this.productos.set(d), error: (e) => this.error.set(mensajeError(e)) });
  }
  poner(id: number, cantidad: number) {
    this.cantidades.update((c) => ({ ...c, [id]: Math.max(0, Math.floor(cantidad || 0)) }));
  }
  enviar() {
    this.enviando.set(true); this.error.set(''); this.ok.set('');
    this.api.crearPedido({ clienteId: this.clienteId(), items: this.items() }).subscribe({
      next: (r) => {
        this.ok.set(`${r.estado}: ${r.mensaje}`);
        this.cantidades.set({}); this.enviando.set(false);
        setTimeout(() => this.cargarCatalogo(), 2500); // el stock baja cuando ms-pedidos procesa el mensaje
      },
      error: (e) => { this.error.set(mensajeError(e)); this.enviando.set(false); },
    });
  }
}
