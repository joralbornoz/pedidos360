import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { environment } from '../../environments/environment';
import { Order, OrderStatus } from './models/order.model';

// ── Mock data realista (ready to swap with BFF) ──────────────────────────────
const MOCK_ORDERS: Order[] = [
  {
    id: 1, numero: 'ORD-2024-001',
    clienteId: 'user-001', cliente: 'María González', clienteEmail: 'maria@example.com',
    estado: 'Completado', fecha: '2024-09-01T09:15:00Z', fechaActualizacion: '2024-09-02T14:30:00Z',
    total: 128500,
    items: [
      { productoId: 1, productNombre: 'Laptop Lenovo IdeaPad', cantidad: 1, precioUnitario: 120000, subtotal: 120000 },
      { productoId: 5, productNombre: 'Mouse inalámbrico', cantidad: 2, precioUnitario: 4250, subtotal: 8500 }
    ]
  },
  {
    id: 2, numero: 'ORD-2024-002',
    clienteId: 'user-002', cliente: 'Carlos Rodríguez', clienteEmail: 'carlos@example.com',
    estado: 'En proceso', fecha: '2024-09-05T11:00:00Z', fechaActualizacion: '2024-09-05T15:00:00Z',
    total: 45900,
    items: [
      { productoId: 2, productNombre: 'Monitor 24" Full HD', cantidad: 1, precioUnitario: 45900, subtotal: 45900 }
    ]
  },
  {
    id: 3, numero: 'ORD-2024-003',
    clienteId: 'user-003', cliente: 'Ana Martínez', clienteEmail: 'ana@example.com',
    estado: 'Pendiente', fecha: '2024-09-10T08:30:00Z', fechaActualizacion: '2024-09-10T08:30:00Z',
    total: 23750,
    items: [
      { productoId: 3, productNombre: 'Teclado mecánico RGB', cantidad: 1, precioUnitario: 18500, subtotal: 18500 },
      { productoId: 5, productNombre: 'Mouse inalámbrico', cantidad: 1, precioUnitario: 4250, subtotal: 4250 },
      { productoId: 7, productNombre: 'Pad de escritorio XL', cantidad: 1, precioUnitario: 1000, subtotal: 1000 }
    ]
  },
  {
    id: 4, numero: 'ORD-2024-004',
    clienteId: 'user-001', cliente: 'María González', clienteEmail: 'maria@example.com',
    estado: 'Enviado', fecha: '2024-09-08T14:00:00Z', fechaActualizacion: '2024-09-09T09:00:00Z',
    total: 9900,
    items: [
      { productoId: 6, productNombre: 'Webcam HD 1080p', cantidad: 1, precioUnitario: 9900, subtotal: 9900 }
    ]
  },
  {
    id: 5, numero: 'ORD-2024-005',
    clienteId: 'user-004', cliente: 'Pedro Soto', clienteEmail: 'pedro@example.com',
    estado: 'Cancelado', fecha: '2024-09-03T16:45:00Z', fechaActualizacion: '2024-09-04T10:20:00Z',
    total: 55000,
    items: [
      { productoId: 4, productNombre: 'SSD 1TB NVMe', cantidad: 2, precioUnitario: 27500, subtotal: 55000 }
    ]
  },
  {
    id: 6, numero: 'ORD-2024-006',
    clienteId: 'user-005', cliente: 'Lucía Fernández', clienteEmail: 'lucia@example.com',
    estado: 'Pendiente', fecha: '2024-09-12T10:00:00Z', fechaActualizacion: '2024-09-12T10:00:00Z',
    total: 34200,
    items: [
      { productoId: 8, productNombre: 'Hub USB-C 7 en 1', cantidad: 2, precioUnitario: 12600, subtotal: 25200 },
      { productoId: 5, productNombre: 'Mouse inalámbrico', cantidad: 2, precioUnitario: 4250, subtotal: 8500 },
    ]
  }
];

// ── Servicio ─────────────────────────────────────────────────────────────────
@Injectable({ providedIn: 'root' })
export class OrdersService {
  private useMock = true; // Cambiar a false para conectar al BFF real
  private baseUrl = `${environment.apiConfig.bffEndpoint}/api/orders`;

  constructor(private http: HttpClient) {}

  getOrders(): Observable<Order[]> {
    if (this.useMock) return of([...MOCK_ORDERS]);
    return this.http.get<Order[]>(this.baseUrl);
  }

  getOrdersByCustomer(clienteId: string): Observable<Order[]> {
    if (this.useMock) return of(MOCK_ORDERS.filter(o => o.clienteId === clienteId));
    return this.http.get<Order[]>(`${this.baseUrl}?clienteId=${clienteId}`);
  }

  getOrderById(id: number): Observable<Order | undefined> {
    if (this.useMock) return of(MOCK_ORDERS.find(o => o.id === id));
    return this.http.get<Order>(`${this.baseUrl}/${id}`);
  }

  createOrder(order: Partial<Order>): Observable<Order> {
    if (this.useMock) {
      const newOrder: Order = {
        ...order,
        id: MOCK_ORDERS.length + 1,
        numero: `ORD-2024-00${MOCK_ORDERS.length + 1}`,
        estado: 'Pendiente',
        fecha: new Date().toISOString(),
        fechaActualizacion: new Date().toISOString(),
        items: order.items || [],
        total: order.total || 0,
      } as Order;
      MOCK_ORDERS.push(newOrder);
      return of(newOrder);
    }
    return this.http.post<Order>(this.baseUrl, order);
  }

  updateOrderStatus(id: number, estado: OrderStatus): Observable<Order> {
    if (this.useMock) {
      const order = MOCK_ORDERS.find(o => o.id === id);
      if (order) {
        order.estado = estado;
        order.fechaActualizacion = new Date().toISOString();
        return of({ ...order });
      }
    }
    return this.http.patch<Order>(`${this.baseUrl}/${id}/status`, { estado });
  }

  // Estadísticas de resumen (para Dashboard)
  getSummaryStats() {
    const orders = MOCK_ORDERS;
    return of({
      totalPedidos: orders.length,
      pedidosPendientes: orders.filter(o => o.estado === 'Pendiente').length,
      pedidosEnProceso: orders.filter(o => o.estado === 'En proceso').length,
      pedidosCompletados: orders.filter(o => o.estado === 'Completado').length,
      ventasDelDia: orders
        .filter(o => o.estado === 'Completado')
        .reduce((sum, o) => sum + o.total, 0),
    });
  }
}