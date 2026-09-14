export type OrderStatus =
  | 'Pendiente'
  | 'En proceso'
  | 'Enviado'
  | 'Completado'
  | 'Cancelado';

export interface OrderItem {
  productoId: number;
  productNombre: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

export interface Order {
  id: number;
  numero: string;
  clienteId: string;
  cliente: string;
  clienteEmail: string;
  estado: OrderStatus;
  fecha: string;           // ISO 8601
  fechaActualizacion: string;
  items: OrderItem[];
  total: number;
  notas?: string;
}
