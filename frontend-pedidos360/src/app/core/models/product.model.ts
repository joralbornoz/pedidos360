export interface Product {
  id: number;
  sku: string;
  nombre: string;
  categoria: string;
  descripcion: string;
  precio: number;
  stock: number;
  activo: boolean;
  creadoEn: string;  // ISO 8601
  actualizadoEn: string;
}
