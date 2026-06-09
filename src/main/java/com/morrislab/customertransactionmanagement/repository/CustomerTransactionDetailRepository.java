package com.morrislab.customertransactionmanagement.repository;

import com.morrislab.customertransactionmanagement.entity.CustomerTransactionDetail;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerTransactionDetailRepository extends JpaRepository<CustomerTransactionDetail, Long> {

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByTransactionReference(String transactionReference);

    Optional<CustomerTransactionDetail> findByIdempotencyKey(String idempotencyKey);
}
