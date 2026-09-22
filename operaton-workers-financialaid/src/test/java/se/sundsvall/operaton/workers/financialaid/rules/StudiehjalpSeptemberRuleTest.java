package se.sundsvall.operaton.workers.financialaid.rules;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class StudiehjalpSeptemberRuleTest {

	private static final YearMonth OCTOBER = YearMonth.of(2026, 10);

	private static SsbtekIncome studiehjalp(final String paidOn) {
		return new SsbtekIncome("Studiehjalp", null, "Månad", new BigDecimal("1250"), LocalDate.parse(paidOn), ApplicantRole.APPLICANT);
	}

	private static SsbtekIncome other(final String benefit, final String paidOn) {
		return new SsbtekIncome(benefit, null, "Månad", new BigDecimal("900"), LocalDate.parse(paidOn), ApplicantRole.APPLICANT);
	}

	@Test
	void warnsWhenJunePaidAndSeptemberDidNot() {
		final var warning = StudiehjalpSeptemberRule.evaluate(OCTOBER, List.of(studiehjalp("2026-06-30")));

		assertThat(warning).contains("Studiehjälp utbetalades i juni men utbetalning saknas i september – kontrollera om ungdom ska ha studiehjälp");
	}

	@Test
	void silentWhenSeptemberPaidToo() {
		assertThat(StudiehjalpSeptemberRule.evaluate(OCTOBER, List.of(studiehjalp("2026-06-30"), studiehjalp("2026-09-29")))).isEmpty();
	}

	@Test
	void silentWhenJuneNeverPaid() {
		// No studiehjälp before the summer — there is nothing that can have stopped.
		assertThat(StudiehjalpSeptemberRule.evaluate(OCTOBER, List.of(studiehjalp("2026-09-29")))).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"2026-09", "2026-11", "2026-01", "2027-06"
	})
	void appliesOnlyToAnOctoberApplication(final String applicationMonth) {
		assertThat(StudiehjalpSeptemberRule.appliesTo(YearMonth.parse(applicationMonth))).isFalse();
		assertThat(StudiehjalpSeptemberRule.evaluate(YearMonth.parse(applicationMonth), List.of(studiehjalp("2026-06-30")))).isEmpty();
	}

	@Test
	void appliesToOctober() {
		assertThat(StudiehjalpSeptemberRule.appliesTo(OCTOBER)).isTrue();
	}

	@Test
	void ignoresOtherBenefitsEntirely() {
		// Studiemedel is a different benefit with its own rhythm; it must not stand in for studiehjälp in either month.
		assertThat(StudiehjalpSeptemberRule.evaluate(OCTOBER, List.of(other("Studiemedel", "2026-06-30")))).isEmpty();
		assertThat(StudiehjalpSeptemberRule.evaluate(OCTOBER, List.of(studiehjalp("2026-06-30"), other("Studiemedel", "2026-09-29")))).isPresent();
	}

	@Test
	void looksAtTheApplicationYearOnly() {
		// A payment in June of some earlier year says nothing about this autumn.
		assertThat(StudiehjalpSeptemberRule.evaluate(OCTOBER, List.of(studiehjalp("2025-06-30")))).isEmpty();
	}

	@Test
	void matchesTheBenefitNameCaseInsensitively() {
		final var oddCase = new SsbtekIncome("STUDIEHJALP", null, null, BigDecimal.ONE, LocalDate.parse("2026-06-30"), ApplicantRole.APPLICANT);

		assertThat(StudiehjalpSeptemberRule.evaluate(OCTOBER, List.of(oddCase))).isPresent();
	}

	@Test
	void survivesNullsAndAnEmptyAnswer() {
		assertThat(StudiehjalpSeptemberRule.evaluate(OCTOBER, null)).isEmpty();
		assertThat(StudiehjalpSeptemberRule.evaluate(OCTOBER, List.of())).isEmpty();
		assertThat(StudiehjalpSeptemberRule.evaluate(null, List.of(studiehjalp("2026-06-30")))).isEmpty();
		assertThat(StudiehjalpSeptemberRule.evaluate(OCTOBER, Arrays.asList((SsbtekIncome) null))).isEmpty();
	}

	@Test
	void ignoresAPaymentWithNoDateAtAll() {
		final var undated = new SsbtekIncome("Studiehjalp", null, null, BigDecimal.ONE, null, ApplicantRole.APPLICANT);

		assertThat(StudiehjalpSeptemberRule.evaluate(OCTOBER, List.of(undated))).isEmpty();
	}

	@Test
	void readsFromJuneOfTheApplicationYear() {
		assertThat(StudiehjalpSeptemberRule.readFrom(OCTOBER)).isEqualTo(YearMonth.of(2026, 6));
	}
}
