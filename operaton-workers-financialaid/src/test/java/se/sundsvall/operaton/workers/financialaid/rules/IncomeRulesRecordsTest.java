package se.sundsvall.operaton.workers.financialaid.rules;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IncomeRulesRecordsTest {

	private static final SsbtekIncome INCOME = new SsbtekIncome("Bostadsbidrag", null, null, BigDecimal.valueOf(4500), null, null, null, null, null);

	@Test
	void aControlPeriodIncomeIsNeverFromTheComparisonPeriod() {
		final var classified = new ClassifiedIncome(INCOME, "TA_MED", "Bostadsbidrag", true, "Ta med");

		assertThat(classified).isEqualTo(new ClassifiedIncome(INCOME, "TA_MED", "Bostadsbidrag", true, "Ta med", false));
	}

	@Test
	void theIncomeOnlyResultCarriesNoAgencyAnswers() {
		final var classified = List.of(new ClassifiedIncome(INCOME, "TA_MED", "Bostadsbidrag", false, "Ta med"));

		final var result = new IncomeRulesResult(classified, List.of());

		assertThat(result.classified()).isEqualTo(classified);
		assertThat(result.changeWarnings()).isEmpty();
		assertThat(result.answers()).isEmpty();
	}
}
