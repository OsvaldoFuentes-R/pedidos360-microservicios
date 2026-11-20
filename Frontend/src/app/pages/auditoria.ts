import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ApiService, EventoAuditoria, mensajeError } from '../api.service';

@Component({
  selector: 'app-auditoria',
  imports: [DatePipe],
  template: `
    <div class="barra">
      <h2>Auditoría de eventos Kafka</h2>
      <select (change)="filtrar($any($event.target).value)">
        <option value="">Todos los tópicos</option>
        <option value="pedidos.eventos">pedidos.eventos</option>
        <option value="inventario.movimientos">inventario.movimientos</option>
      </select>
      <button class="btn" (click)="cargar()">🔄 Actualizar</button>
    </div>
    @if (error()) { <div class="alerta error">{{ error() }}</div> }
    @if (eventos().length === 0 && !error()) { <div class="vacio">Sin eventos registrados.</div> } @else {
      <div class="tabla-wrap">
        <table>
          <thead><tr><th>Recibido</th><th>Tópico</th><th>Part.</th><th>Offset</th><th>Clave</th><th>Tipo</th><th>Contenido</th></tr></thead>
          <tbody>
            @for (e of eventos(); track e.id) {
              <tr>
                <td>{{ e.recibidoEn | date:'dd/MM HH:mm:ss' }}</td>
                <td>{{ e.topico }}</td><td>{{ e.particion }}</td><td>{{ e.offsetKafka }}</td>
                <td>{{ e.clave }}</td><td><span class="chip info">{{ e.tipo }}</span></td>
                <td class="payload">{{ e.payload }}</td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }
  `,
})
export class AuditoriaPage implements OnInit {
  private api = inject(ApiService);
  eventos = signal<EventoAuditoria[]>([]);
  error = signal('');
  private topico = '';

  ngOnInit() { this.cargar(); }
  filtrar(t: string) { this.topico = t; this.cargar(); }
  cargar() {
    this.api.auditoria(this.topico, 50).subscribe({
      next: (d) => { this.eventos.set(d); this.error.set(''); },
      error: (e) => this.error.set(mensajeError(e)),
    });
  }
}
