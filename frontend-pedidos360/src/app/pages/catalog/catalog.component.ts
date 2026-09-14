import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CatalogService } from '../../core/catalog.service';
import { Product } from '../../core/models/product.model';
import { TopbarComponent } from '../../shared/topbar/topbar.component';
import { ProductCardComponent } from './product-card/product-card.component';
import { ProductFormComponent } from './product-form/product-form.component';

type ViewMode = 'grid' | 'list';

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [CommonModule, FormsModule, TopbarComponent, ProductCardComponent, ProductFormComponent],
  templateUrl: './catalog.component.html',
  styleUrl: './catalog.component.scss'
})
export class CatalogComponent implements OnInit {
  allProducts: Product[] = [];
  filteredProducts: Product[] = [];

  searchText   = '';
  filterCat    = '';
  filterStatus = '';

  viewMode: ViewMode = 'grid';
  loading = true;
  error   = '';

  showForm    = false;
  editProduct: Product | null = null;

  categories: string[] = [];

  // Stats
  stats = { totalProductos: 0, activos: 0, sinStock: 0, stockBajo: 0 };

  constructor(private catalogService: CatalogService) {}

  ngOnInit(): void {
    this.loadProducts();
    this.catalogService.getCatalogStats().subscribe(s => this.stats = s);
  }

  loadProducts(): void {
    this.loading = true;
    this.catalogService.getProducts().subscribe({
      next: products => {
        this.allProducts = products;
        this.categories  = [...new Set(products.map(p => p.categoria).filter((c): c is string => !!c))].sort();
        this.applyFilters();
        this.loading = false;
      },
      error: err => {
        this.error   = `Error al cargar productos: ${err.message || err.statusText}`;
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    let result = [...this.allProducts];

    if (this.searchText.trim()) {
      const q = this.searchText.toLowerCase();
      result = result.filter(p =>
        p.nombre.toLowerCase().includes(q) ||
        (p.sku ?? '').toLowerCase().includes(q) ||
        (p.categoria ?? '').toLowerCase().includes(q)
      );
    }

    if (this.filterCat)    result = result.filter(p => p.categoria === this.filterCat);
    if (this.filterStatus === 'activo')    result = result.filter(p => p.activo);
    if (this.filterStatus === 'inactivo')  result = result.filter(p => !p.activo);
    if (this.filterStatus === 'sin-stock') result = result.filter(p => p.stock === 0);
    if (this.filterStatus === 'stock-bajo') result = result.filter(p => p.stock > 0 && p.stock <= 5);

    this.filteredProducts = result;
  }

  clearFilters(): void {
    this.searchText   = '';
    this.filterCat    = '';
    this.filterStatus = '';
    this.applyFilters();
  }

  openCreateForm():              void { this.editProduct = null; this.showForm = true; }
  openEditForm(p: Product):      void { this.editProduct = p;    this.showForm = true; }
  closeForm():                   void { this.showForm = false; this.editProduct = null; }

  onFormSaved(product: Product): void {
    if (this.editProduct) {
      this.catalogService.updateProduct(product.id, product).subscribe(() => {
        this.loadProducts();
        this.closeForm();
      });
    } else {
      this.catalogService.createProduct(product).subscribe(() => {
        this.loadProducts();
        this.closeForm();
      });
    }
  }

  toggleActive(product: Product): void {
    this.catalogService.toggleActive(product.id).subscribe(() => this.loadProducts());
  }

  toggleView(mode: ViewMode): void { this.viewMode = mode; }

  get hasFilters(): boolean {
    return !!(this.searchText || this.filterCat || this.filterStatus);
  }
}
