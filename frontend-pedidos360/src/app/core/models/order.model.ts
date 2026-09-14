export type OrderStatus =
  | 'Pendiente'
  | 'En proceso'
  | 'Enviado'
  | 'Completado'
  | 'Cancelado';

export interface OrderItem {
  productoId: number;
  productNombre?: string; // opcional: el backend puede no enviarlo
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

export interface Order {
  id: number;
  numero?: string;           // opcional: el backend puede usar otro campo
  clienteId?: string;        // opcional
  cliente?: string;          // opcional
  clienteEmail?: string;     // opcional
  estado: OrderStatus;
  fecha: string;             // ISO 8601
  fechaActualizacion?: string;
  items?: OrderItem[];       // opcional: puede no venir en listado
  total: number;
  notas?: string;
}
