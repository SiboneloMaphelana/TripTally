package com.tripTally.service.split;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CalculatedOwed {

	Long tripMemberId;
	BigDecimal owedAmount;
	BigDecimal splitInputStored;
}
