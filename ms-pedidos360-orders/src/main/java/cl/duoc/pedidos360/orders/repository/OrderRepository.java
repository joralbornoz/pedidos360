package cl.duoc.pedidos360.orders.repository;

import cl.duoc.pedidos360.orders.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository 
public interface OrderRepository extends JpaRepository<Order, Long> {
    
}