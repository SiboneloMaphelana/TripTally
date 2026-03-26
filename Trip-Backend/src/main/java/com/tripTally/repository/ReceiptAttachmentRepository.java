package com.tripTally.repository;

import com.tripTally.domain.entity.Expense;
import com.tripTally.domain.entity.ReceiptAttachment;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReceiptAttachmentRepository extends JpaRepository<ReceiptAttachment, Long> {

	Optional<ReceiptAttachment> findByExpense(Expense expense);

	void deleteByExpense(Expense expense);

	@Query("select r.expense.id from ReceiptAttachment r where r.expense.id in :ids")
	List<Long> findExpenseIdsHavingReceipt(@Param("ids") Collection<Long> ids);
}
