package com.tripTally.repository;

import com.tripTally.domain.entity.Expense;
import com.tripTally.domain.entity.ExpenseCategory;
import com.tripTally.domain.entity.ExpenseParticipant;
import com.tripTally.domain.entity.Trip;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

public final class ExpenseSpecifications {

	private ExpenseSpecifications() {
	}

	public static Specification<Expense> forTrip(Trip trip) {
		return (root, q, cb) -> cb.equal(root.get("trip"), trip);
	}

	public static Specification<Expense> categoryEquals(ExpenseCategory category) {
		return (root, q, cb) -> cb.equal(root.get("category"), category);
	}

	public static Specification<Expense> payerEquals(Long payerMemberId) {
		return (root, q, cb) -> cb.equal(root.get("payer").get("id"), payerMemberId);
	}

	public static Specification<Expense> hasParticipant(Long tripMemberId) {
		return (root, query, cb) -> {
			Subquery<Long> sq = query.subquery(Long.class);
			var ep = sq.from(ExpenseParticipant.class);
			sq.select(ep.get("expense").get("id"));
			sq.where(cb.and(
					cb.equal(ep.get("expense"), root),
					cb.equal(ep.get("tripMember").get("id"), tripMemberId)));
			return cb.exists(sq);
		};
	}

	public static Specification<Expense> expenseDateFrom(LocalDate from) {
		return (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("expenseDate"), from);
	}

	public static Specification<Expense> expenseDateTo(LocalDate to) {
		return (root, q, cb) -> cb.lessThanOrEqualTo(root.get("expenseDate"), to);
	}

	public static Specification<Expense> settledEquals(boolean settled) {
		return (root, q, cb) -> cb.equal(root.get("settled"), settled);
	}

	public static Specification<Expense> build(
			Trip trip,
			ExpenseCategory category,
			Long payerMemberId,
			Long participantMemberId,
			LocalDate dateFrom,
			LocalDate dateTo,
			Boolean settled) {
		Specification<Expense> spec = forTrip(trip);
		if (category != null) {
			spec = spec.and(categoryEquals(category));
		}
		if (payerMemberId != null) {
			spec = spec.and(payerEquals(payerMemberId));
		}
		if (participantMemberId != null) {
			spec = spec.and(hasParticipant(participantMemberId));
		}
		if (dateFrom != null) {
			spec = spec.and(expenseDateFrom(dateFrom));
		}
		if (dateTo != null) {
			spec = spec.and(expenseDateTo(dateTo));
		}
		if (settled != null) {
			spec = spec.and(settledEquals(settled));
		}
		return spec;
	}
}
