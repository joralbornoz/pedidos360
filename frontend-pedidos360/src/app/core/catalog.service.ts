import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { environment } from '../../environments/environment';
import { Product } from './models/product.model';

// ── Mock data realista (ready to swap with BFF) ──────────────────────────────
const MOCK_PRODUCTS: Product[] = [
  {
    id: 1, sku: 'LAP-LEN-001', nombre: 'Laptop Lenovo IdeaPad 3', categoria: 'Computadoras',
    descripcion: 'Laptop 15.6" Intel Core i5, 8GB RAM, 512GB SSD, Windows 11',
    precio: 120000, stock: 15, activo: true,
    creadoEn: '2024-01-10T00:00:00Z', actualizadoEn: '2024-08-20T00:00:00Z'
  },
  {
    id: 2, sku: 'MON-SAM-24', nombre: 'Monitor Samsung 24" Full HD',  categoria: 'Monitores',
    descripcion: 'Panel IPS 1920x1080, 75Hz, tiempo de respuesta 5ms, FreeSync',
    precio: 45900, stock: 8, activo: true,
    creadoEn: '2024-01-15T00:00:00Z', actualizadoEn: '2024-08-15T00:00:00Z'
  },
  {
    id: 3, sku: 'TEC-MEC-RGB', nombre: 'Teclado Mecánico RGB TK-800', categoria: 'Periféricos',
    descripcion: 'Switches blue, retroiluminación RGB programable, USB-C, NKRO',
    precio: 18500, stock: 22, activo: true,
    creadoEn: '2024-02-01T00:00:00Z', actualizadoEn: '2024-09-01T00:00:00Z'
  },
  {
    id: 4, sku: 'SSD-1TB-NVM', nombre: 'SSD 1TB NVMe PCIe Gen4', categoria: 'Almacenamiento',
    descripcion: 'M.2 2280, hasta 7000 MB/s lectura, compatible PS5 y PC',
    precio: 27500, stock: 0, activo: false,
    creadoEn: '2024-03-05T00:00:00Z', actualizadoEn: '2024-07-10T00:00:00Z'
  },
  {
    id: 5, sku: 'MOU-INL-001', nombre: 'Mouse Inalámbrico Ergonómico', categoria: 'Periféricos',
    descripcion: 'Sensor óptico 1600 DPI, receptor USB nano, batería AA 18 meses',
    precio: 4250, stock: 45, activo: true,
    creadoEn: '2024-01-20T00:00:00Z', actualizadoEn: '2024-08-01T00:00:00Z'
  },
  {
    id: 6, sku: 'CAM-HD-1080', nombre: 'Webcam HD 1080p Streaming', categoria: 'Periféricos',
    descripcion: 'Autofocus, micrófono dual, compatible OBS/Zoom/Teams',
    precio: 9900, stock: 3, activo: true,
    creadoEn: '2024-04-12T00:00:00Z', actualizadoEn: '2024-09-05T00:00:00Z'
  },
  {
    id: 7, sku: 'PAD-XL-001', nombre: 'Pad de Escritorio XL 900x400', categoria: 'Accesorios',
    descripcion: 'Tela suave de alta densidad, base antideslizante, bordes cosidos',
    precio: 1000, stock: 60, activo: true,
    creadoEn: '2024-05-01T00:00:00Z', actualizadoEn: '2024-08-28T00:00:00Z'
  },
  {
    id: 8, sku: 'HUB-USB-C7', nombre: 'Hub USB-C 7 en 1', categoria: 'Accesorios',
    descripcion: 'HDMI 4K, 3x USB-A 3.0, lector SD/microSD, USB-C PD 100W',
    precio: 12600, stock: 0, activo: true,
    creadoEn: '2024-06-10T00:00:00Z', actualizadoEn: '2024-09-10T00:00:00Z'
  }
];

// ── Servicio ─────────────────────────────────────────────────────────────────
@Injectable({ providedIn: 'root' })
export class CatalogService {
  private useMock = false; // Mock desactivado — conectado al BFF real
  private baseUrl = `${environment.apiConfig.bffEndpoint}/api/catalog`;

  constructor(private http: HttpClient) {}

  getProducts(): Observable<Product[]> {
    if (this.useMock) return of([...MOCK_PRODUCTS]);
    return this.http.get<Product[]>(this.baseUrl);
  }

  getProductById(id: number): Observable<Product | undefined> {
    if (this.useMock) return of(MOCK_PRODUCTS.find(p => p.id === id));
    return this.http.get<Product>(`${this.baseUrl}/${id}`);
  }

  createProduct(product: Partial<Product>): Observable<Product> {
    if (this.useMock) {
      const newProduct: Product = {
        ...product,
        id: MOCK_PRODUCTS.length + 1,
        activo: true,
        creadoEn: new Date().toISOString(),
        actualizadoEn: new Date().toISOString(),
      } as Product;
      MOCK_PRODUCTS.push(newProduct);
      return of(newProduct);
    }
    return this.http.post<Product>(this.baseUrl, product);
  }

  updateProduct(id: number, changes: Partial<Product>): Observable<Product> {
    if (this.useMock) {
      const idx = MOCK_PRODUCTS.findIndex(p => p.id === id);
      if (idx !== -1) {
        MOCK_PRODUCTS[idx] = { ...MOCK_PRODUCTS[idx], ...changes, actualizadoEn: new Date().toISOString() };
        return of({ ...MOCK_PRODUCTS[idx] });
      }
    }
    return this.http.put<Product>(`${this.baseUrl}/${id}`, changes);
  }

  toggleActive(id: number): Observable<Product> {
    return new Observable(observer => {
      this.getProductById(id).subscribe(product => {
        this.updateProduct(id, { activo: !product?.activo }).subscribe({
          next: updated => { observer.next(updated); observer.complete(); },
          error: err => observer.error(err)
        });
      }, err => observer.error(err));
    });
  }

  getCatalogStats(): Observable<{
    totalProductos: number;
    activos: number;
    sinStock: number;
    stockBajo: number;
  }> {
    return new Observable(observer => {
      this.getProducts().subscribe(products => {
        observer.next({
          totalProductos: products.length,
          activos: products.filter(p => p.activo).length,
          sinStock: products.filter(p => p.stock === 0).length,
          stockBajo: products.filter(p => p.stock > 0 && p.stock <= 5).length,
        });
        observer.complete();
      }, err => observer.error(err));
    });
  }
}
