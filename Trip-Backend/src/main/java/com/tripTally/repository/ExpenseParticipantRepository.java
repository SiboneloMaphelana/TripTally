package com.tripTally.repository;

import com.tripTally.domain.entity.Expense;
import com.tripTally.domain.entity.ExpenseParticipant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseParticipantRepository extends JpaRepository<ExpenseParticipant, Long> {

	List<ExpenseParticipant> findByExpense(Expense expense);

	void deleteByExpense(Expense expense);
}
