package com.tripTally.dto.balance;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TripBalancesResponse {

	String currencyCode;
	List<MemberBalanceResponse> members;
}
