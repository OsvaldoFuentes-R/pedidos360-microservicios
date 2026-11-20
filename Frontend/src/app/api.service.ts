import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

export interface LineaPedido { producto: string; cantidad: number; precioUnitario: number; }
export interface Pedido {
  id: number; producto: string; estado: string; cliente: string;
  total: number; fecha: string; ticket: string | null; items: LineaPedido[];
}
export interface Producto { id: number; nombre: string; descripcion: string; precio: number; stock: number; categoria: string; }
export interface Cliente { id: number; nombre: string; email: string; direccion: string; }
export interface ItemSolicitud { productoId: number; cantidad: number; }
export interface SolicitudPedido { clienteId: number; items: ItemSolicitud[]; }
export interface RespuestaRecepcion { estado: string; mensaje: string; }
export interface Kpis { pedidos: number; ingresos: number; ticketPromedio: number; unidadesVendidas: number; }
export interface ProductoTop { productoId: number; nombre: string; unidades: number; }
export interface StockCritico { productoId: number; nombre: string; stockActual: number; }
export interface EventoAuditoria {
  id: number; topico: string; particion: number; offsetKafka: number;
  clave: string; tipo: string; payload: string; recibidoEn: string;
}

/** Todas las llamadas van al BFF; el interceptor de MSAL agrega el token Bearer. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  pedidos(): Observable<Pedido[]> { return this.http.get<Pedido[]>(`${this.base}/pedidos`); }
  crearPedido(s: SolicitudPedido): Observable<RespuestaRecepcion> {
    return this.http.post<RespuestaRecepcion>(`${this.base}/pedidos`, s);
  }
  productos(): Observable<Producto[]> { return this.http.get<Producto[]>(`${this.base}/productos`); }
  clientes(): Observable<Cliente[]> { return this.http.get<Cliente[]>(`${this.base}/clientes`); }
  kpis(): Observable<Kpis> { return this.http.get<Kpis>(`${this.base}/analitica/kpis`); }
  topProductos(limite = 5): Observable<ProductoTop[]> {
    return this.http.get<ProductoTop[]>(`${this.base}/analitica/top-productos`, { params: { limite } });
  }
  stockCritico(umbral = 10): Observable<StockCritico[]> {
    return this.http.get<StockCritico[]>(`${this.base}/analitica/stock-critico`, { params: { umbral } });
  }
  auditoria(topico: string, limite = 50): Observable<EventoAuditoria[]> {
    let params = new HttpParams().set('limite', limite);
    if (topico) params = params.set('topico', topico);
    return this.http.get<EventoAuditoria[]>(`${this.base}/analitica/auditoria`, { params });
  }
}

export function mensajeError(e: any): string {
  if (e?.status === 0) return 'No se pudo conectar con el BFF. ¿Está corriendo en el puerto 8080?';
  if (e?.status === 401) return 'El BFF rechazó el token (401). Cierra sesión y vuelve a entrar.';
  if (e?.status === 400) return e?.error?.mensaje || e?.error?.message || 'Datos inválidos (400).';
  return e?.error?.mensaje || e?.error?.message || `Error ${e?.status ?? ''} al llamar al BFF.`;
}
