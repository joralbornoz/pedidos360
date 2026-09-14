import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Product } from '../../../core/models/product.model';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './product-card.component.html',
  styleUrl: './product-card.component.scss'
})
export class ProductCardComponent {
  @Input({ required: true }) product!: Product;
  @Input() viewMode: 'grid' | 'list' = 'grid';

  @Output() editClicked         = new EventEmitter<Product>();
  @Output() toggleActiveClicked = new EventEmitter<Product>();

  get stockClass(): string {
    if (this.product.stock === 0)               return 'stock-none';
    if (this.product.stock > 0 && this.product.stock <= 5) return 'stock-low';
    return 'stock-ok';
  }

  get stockLabel(): string {
    if (this.product.stock === 0) return 'Sin stock';
    if (this.product.stock <= 5)  return `Stock bajo (${this.product.stock})`;
    return `${this.product.stock} en stock`;
  }

  formatCLP(value: number): string {
    return new Intl.NumberFormat('es-CL', { style: 'currency', currency: 'CLP', maximumFractionDigits: 0 }).format(value);
  }
}
