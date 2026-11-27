package se.sundsvall.operaton.workers.financialaid.rules;

import java.time.LocalDate;
import java.util.List;

/**
 * The SSBTEK facts that gate caremanagement's dagersättning day check (verksamhetens svar 2026-09-23 §2): the
 * ekonomiska beslut Arbetsförmedlingen reports, and how many jobb- och utvecklingsgaranti days Försäkringskassan says
 * are used up. Neither is an income, so neither is an {@link SsbtekIncome}.
 * <p>
 * Every field is tri-state: {@code null} means "this agency was not read or could not answer", which caremanagement
 * treats as a closed gate - no check and no warning. An empty {@code economicDecisionPeriods} means AF answered and
 * reports no decision; {@code allDaysConsumed = false} with {@code consumedDays = null} means FK answered without any
 * programjobdagar.
 *
 * @param economicDecisionPeriods af Svar.BeslutInfo.EkonomiskaBeslut.Beslut, or {@code null} when AF was not read
 * @param consumedDays            the highest fk formansinformation.programjobdagar antalForbrukade, or {@code null}
 * @param allDaysConsumed         whether any programjobdagar row says harForbrukatMaxAntal, or {@code null} when FK
 *                                was not read
 */
public record DayCheckFacts(List<DecisionPeriod> economicDecisionPeriods, Integer consumedDays, Boolean allDaysConsumed) {

	/** An AF ekonomiskt beslut period; {@code to} is {@code null} for an open period. */
	public record DecisionPeriod(LocalDate from, LocalDate to) {
	}
}
