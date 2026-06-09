package com.morrislab.customertransactionmanagement.repository;

import com.morrislab.customertransactionmanagement.entity.CustomerTransactionDetail;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerTransactionDetailRepository extends JpaRepository<CustomerTransactionDetail, Long> {

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByTransactionReference(String transactionReference);

    Optional<CustomerTransactionDetail> findByIdempotencyKey(String idempotencyKey);

    Optional<CustomerTransactionDetail> findByAccountNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select transactionDetail from CustomerTransactionDetail transactionDetail "
            + "where transactionDetail.accountNumber = :accountNumber")
    Optional<CustomerTransactionDetail> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);
}
