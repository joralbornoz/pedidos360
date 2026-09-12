package cl.duoc.pedidos360.bff.repository;

import cl.duoc.pedidos360.bff.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository 
public interface OrderRepository extends JpaRepository<Order, Long> {
    
}