package com.triptally.service.split;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SplitParticipantInput {

	Long tripMemberId;
	BigDecimal splitInput;
}
