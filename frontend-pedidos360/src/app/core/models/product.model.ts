export interface Product {
  id: number;
  nombre: string;
  precio: number;
  stock: number;
  sku?: string;          // opcional
  categoria?: string;    // opcional
  descripcion?: string;  // opcional
  activo?: boolean;      // opcional
  creadoEn?: string;     // ISO 8601
  actualizadoEn?: string;
}
