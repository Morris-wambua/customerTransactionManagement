package com.morrislab.customertransactionmanagement.repository;

import com.morrislab.customertransactionmanagement.entity.FundTransfer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FundTransferRepository extends JpaRepository<FundTransfer, Long> {

    boolean existsByTransferReference(String transferReference);

    Optional<FundTransfer> findByIdempotencyKey(String idempotencyKey);
}
