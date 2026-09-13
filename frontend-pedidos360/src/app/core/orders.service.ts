import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Order {
  id: number;
  cliente: string;
  estado: string;
}

@Injectable({
  providedIn: 'root'
})
export class OrdersService {

  constructor(private http: HttpClient) {}

  getOrders(): Observable<Order[]> {
  return this.http.get<Order[]>(`${environment.apiConfig.bffEndpoint}/api/orders`);
     }
}