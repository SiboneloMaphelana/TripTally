package com.tripTally.config;

import com.tripTally.domain.entity.Expense;
import com.tripTally.domain.entity.ExpenseCategory;
import com.tripTally.domain.entity.ExpenseParticipant;
import com.tripTally.domain.entity.Settlement;
import com.tripTally.domain.entity.SplitMode;
import com.tripTally.domain.entity.Trip;
import com.tripTally.domain.entity.TripMember;
import com.tripTally.domain.entity.TripMemberRole;
import com.tripTally.domain.entity.User;
import com.tripTally.repository.ExpenseParticipantRepository;
import com.tripTally.repository.ExpenseRepository;
import com.tripTally.repository.SettlementRepository;
import com.tripTally.repository.TripMemberRepository;
import com.tripTally.repository.TripRepository;
import com.tripTally.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("demo")
public class DemoDataLoader implements CommandLineRunner {

	private final UserRepository userRepository;
	private final TripRepository tripRepository;
	private final TripMemberRepository tripMemberRepository;
	private final ExpenseRepository expenseRepository;
	private final ExpenseParticipantRepository expenseParticipantRepository;
	private final SettlementRepository settlementRepository;
	private final PasswordEncoder passwordEncoder;

	public DemoDataLoader(
			UserRepository userRepository,
			TripRepository tripRepository,
			TripMemberRepository tripMemberRepository,
			ExpenseRepository expenseRepository,
			ExpenseParticipantRepository expenseParticipantRepository,
			SettlementRepository settlementRepository,
			PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.tripRepository = tripRepository;
		this.tripMemberRepository = tripMemberRepository;
		this.expenseRepository = expenseRepository;
		this.expenseParticipantRepository = expenseParticipantRepository;
		this.settlementRepository = settlementRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(String... args) {
		if (userRepository.count() > 0) {
			return;
		}
		User alex = userRepository.save(User.builder()
				.email("alex@triptally.demo")
				.passwordHash(passwordEncoder.encode("Demo123!"))
				.displayName("Alex")
				.build());
		User samUser = userRepository.save(User.builder()
				.email("sam@triptally.demo")
				.passwordHash(passwordEncoder.encode("Demo123!"))
				.displayName("Sam")
				.build());

		Trip trip = tripRepository.save(Trip.builder()
				.owner(alex)
				.title("Cape Town summer weekend")
				.destination("Cape Town, South Africa")
				.startDate(LocalDate.of(2025, 1, 10))
				.endDate(LocalDate.of(2025, 1, 13))
				.currencyCode("ZAR")
				.build());

		TripMember mAlex = tripMemberRepository.save(TripMember.builder()
				.trip(trip)
				.user(alex)
				.displayName("Alex")
				.role(TripMemberRole.OWNER)
				.build());
		TripMember mSam = tripMemberRepository.save(TripMember.builder()
				.trip(trip)
				.user(samUser)
				.displayName("Sam")
				.invitedEmail("sam@triptally.demo")
				.role(TripMemberRole.MEMBER)
				.build());
		TripMember mJules = tripMemberRepository.save(TripMember.builder()
				.trip(trip)
				.user(null)
				.displayName("Jules")
				.role(TripMemberRole.MEMBER)
				.build());

		Expense hotel = expenseRepository.save(Expense.builder()
				.trip(trip)
				.payer(mAlex)
				.amount(new BigDecimal("2700.00"))
				.category(ExpenseCategory.ACCOMMODATION)
				.description("Boutique stay — 3 nights")
				.expenseDate(LocalDate.of(2025, 1, 10))
				.splitMode(SplitMode.EQUAL)
				.settled(false)
				.build());
		addEqualParticipants(hotel, mAlex, mSam, mJules);

		Expense dinner = expenseRepository.save(Expense.builder()
				.trip(trip)
				.payer(mSam)
				.amount(new BigDecimal("450.00"))
				.category(ExpenseCategory.FOOD)
				.description("Harbour-side dinner")
				.expenseDate(LocalDate.of(2025, 1, 11))
				.splitMode(SplitMode.PERCENTAGE)
				.settled(true)
				.build());
		saveParticipant(dinner, mAlex, new BigDecimal("150.00"), new BigDecimal("33.34"));
		saveParticipant(dinner, mSam, new BigDecimal("150.00"), new BigDecimal("33.33"));
		saveParticipant(dinner, mJules, new BigDecimal("150.00"), new BigDecimal("33.33"));

		Expense uber = expenseRepository.save(Expense.builder()
				.trip(trip)
				.payer(mJules)
				.amount(new BigDecimal("180.00"))
				.category(ExpenseCategory.TRANSPORT)
				.description("Airport shuttles")
				.expenseDate(LocalDate.of(2025, 1, 10))
				.splitMode(SplitMode.SHARES)
				.settled(false)
				.build());
		saveParticipant(uber, mAlex, new BigDecimal("60.00"), new BigDecimal("1"));
		saveParticipant(uber, mSam, new BigDecimal("60.00"), new BigDecimal("1"));
		saveParticipant(uber, mJules, new BigDecimal("60.00"), new BigDecimal("1"));

		settlementRepository.save(Settlement.builder()
				.trip(trip)
				.fromMember(mSam)
				.toMember(mAlex)
				.amount(new BigDecimal("200.00"))
				.note("Cash for activities kitty")
				.recordedBy(samUser)
				.build());
	}

	private void addEqualParticipants(Expense expense, TripMember... members) {
		int n = members.length;
		BigDecimal each = expense.getAmount().divide(BigDecimal.valueOf(n), 2, java.math.RoundingMode.DOWN);
		BigDecimal allocated = BigDecimal.ZERO;
		for (int i = 0; i < n; i++) {
			BigDecimal owed = (i == n - 1) ? expense.getAmount().subtract(allocated) : each;
			saveParticipant(expense, members[i], owed, BigDecimal.ONE);
			if (i < n - 1) {
				allocated = allocated.add(each);
			}
		}
	}

	private void saveParticipant(Expense expense, TripMember member, BigDecimal owed, BigDecimal splitInput) {
		expenseParticipantRepository.save(ExpenseParticipant.builder()
				.expense(expense)
				.tripMember(member)
				.owedAmount(owed)
				.splitInput(splitInput)
				.build());
	}
}
