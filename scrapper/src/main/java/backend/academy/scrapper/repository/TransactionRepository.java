package backend.academy.scrapper.repository;

import backend.academy.scrapper.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
