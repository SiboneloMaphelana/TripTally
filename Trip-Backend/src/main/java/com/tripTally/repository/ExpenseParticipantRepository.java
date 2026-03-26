package com.triptally.repository;

import com.triptally.domain.entity.Expense;
import com.triptally.domain.entity.ExpenseParticipant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseParticipantRepository extends JpaRepository<ExpenseParticipant, Long> {

	List<ExpenseParticipant> findByExpense(Expense expense);

	void deleteByExpense(Expense expense);
}
