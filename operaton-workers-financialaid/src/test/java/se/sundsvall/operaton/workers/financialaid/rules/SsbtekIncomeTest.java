package se.sundsvall.operaton.workers.financialaid.rules;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SsbtekIncomeTest {

	@Test
	void asChildKeepsTheIncomeAndTagsItWithTheChild() {
		final var income = new SsbtekIncome("Barnpension", "Skattepliktig barnpension", "Månad", new BigDecimal("1200"), LocalDate.of(2026, 6, 25),
			LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), BigDecimal.TEN, ApplicantRole.APPLICANT);

		final var child = income.asChild("child-1");

		assertThat(income.partyId()).isNull();
		assertThat(child.role()).isEqualTo(ApplicantRole.CHILD);
		assertThat(child.partyId()).isEqualTo("child-1");
		assertThat(child).usingRecursiveComparison().ignoringFields("role", "partyId").isEqualTo(income);
	}
}
