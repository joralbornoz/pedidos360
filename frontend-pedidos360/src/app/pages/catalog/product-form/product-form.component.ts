import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl } from '@angular/forms';
import { Product } from '../../../core/models/product.model';

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './product-form.component.html',
  styleUrl: './product-form.component.scss'
})
export class ProductFormComponent implements OnInit, OnChanges {
  @Input() product: Product | null = null; // null = create, Product = edit
  @Output() saved     = new EventEmitter<Product>();
  @Output() cancelled = new EventEmitter<void>();

  form!: FormGroup;

  readonly categories = [
    'Computadoras', 'Monitores', 'Periféricos', 'Almacenamiento', 'Accesorios', 'Redes', 'Audio'
  ];

  constructor(private fb: FormBuilder) {}

  ngOnInit(): void {
    this.buildForm();
    if (this.product) this.patchForm(this.product);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['product'] && this.form) {
      if (this.product) this.patchForm(this.product);
      else this.form.reset({ activo: true, stock: 0 });
    }
  }

  get isEditing(): boolean { return !!this.product; }
  get title(): string { return this.isEditing ? 'Editar producto' : 'Nuevo producto'; }

  private buildForm(): void {
    this.form = this.fb.group({
      nombre:    ['', [Validators.required, Validators.minLength(3)]],
      sku:       ['', [Validators.required, Validators.pattern(/^[A-Z0-9\-]+$/)]],
      categoria: ['', Validators.required],
      descripcion: ['', [Validators.required, Validators.maxLength(300)]],
      precio:    [null, [Validators.required, Validators.min(1)]],
      stock:     [0,    [Validators.required, Validators.min(0)]],
      activo:    [true],
    });
  }

  private patchForm(p: Product): void {
    this.form.patchValue({
      nombre:     p.nombre,
      sku:        p.sku,
      categoria:  p.categoria,
      descripcion: p.descripcion,
      precio:     p.precio,
      stock:      p.stock,
      activo:     p.activo,
    });
  }

  fieldError(name: string): string | null {
    const ctrl: AbstractControl | null = this.form.get(name);
    if (!ctrl || !ctrl.invalid || !ctrl.touched) return null;
    if (ctrl.errors?.['required'])   return 'Este campo es requerido.';
    if (ctrl.errors?.['minlength'])  return `Mínimo ${ctrl.errors['minlength'].requiredLength} caracteres.`;
    if (ctrl.errors?.['maxlength'])  return `Máximo ${ctrl.errors['maxlength'].requiredLength} caracteres.`;
    if (ctrl.errors?.['min'])        return `El valor mínimo es ${ctrl.errors['min'].min}.`;
    if (ctrl.errors?.['pattern'])    return 'Solo letras mayúsculas, números y guiones (ej: LAP-001).';
    return 'Valor no válido.';
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value: Partial<Product> = {
      ...this.form.value,
      id: this.product?.id ?? 0,
      creadoEn: this.product?.creadoEn ?? new Date().toISOString(),
      actualizadoEn: new Date().toISOString(),
    };
    this.saved.emit(value as Product);
  }

  cancel(): void { this.cancelled.emit(); }
}
