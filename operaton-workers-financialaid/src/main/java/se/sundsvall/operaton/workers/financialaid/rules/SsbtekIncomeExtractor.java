package se.sundsvall.operaton.workers.financialaid.rules;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * Turns the api-service-financial-aid SSBTEK basis — the untyped, per-agency map (parsed from the worker's
 * {@code financialAidBasis} JSON) — into normalised {@link SsbtekIncome}s for the income rules. Most agencies are a
 * generic XML-to-JSON conversion of the SSBTEK SOAP response; keys mirror the XML element names. <b>fk</b> is delivered
 * as LEFI JSON, so its keys are the LEFI property names. Repeated elements arrive as a single object or a list, so
 * navigation is defensive throughout. One SSBTEK basis is one person's data, so the caller supplies the
 * {@link ApplicantRole}.
 *
 * <p>
 * Grounded agencies: <b>fk</b> (Försäkringskassans utbetalningar under {@code formansinformation}, and
 * Pensionsmyndighetens utbetalningar under the top-level {@code utbetalningar}/{@code preliminaraUtbetalningar}),
 * <b>so</b> (unemployment benefit payments) and <b>csn</b> (study support payments). The FK benefit comes from the
 * payment's {@code formansfamilj.beskrivning}; FK benefits not on the allow list surface as warnings downstream. The
 * Pensionsmyndigheten benefit is fixed as {@code "PM"}/{@code "PM-Prel"} - the payload carries no benefit name to read.
 * Non-income agencies (tns/miv, skv capital) are intentionally not read; af is read only for the day-check gate
 * ({@link #extractDayCheckFacts}).
 */
public final class SsbtekIncomeExtractor {

	private static final String AGENCY_AF = "af";
	private static final String AGENCY_FK = "fk";
	private static final String AGENCY_SO = "so";
	private static final String AGENCY_CSN = "csn";

	private static final String PM_BENEFIT = "PM";
	private static final String PM_BENEFIT_PRELIMINARY = "PM-Prel";

	private static final String CSN_STUDIEMEDEL = "Studiemedel";
	/**
	 * Deliberately spelled without the umlaut - Decision_inkomstRalista's Rule_ral_studiehjalp keys on this exact
	 * string. Public because the september rule has to recognise the same benefit by the same name; two spellings of
	 * one benefit is how a rule silently stops matching.
	 */
	public static final String CSN_STUDIEHJALP = "Studiehjalp";
	private static final String CSN_STUDIESTARTSSTOD = "Studiestartsstöd";
	private static final String CSN_OMSTALLNINGSSTUDIESTOD = "Omställningsstudiestöd";

	private SsbtekIncomeExtractor() {}

	/**
	 * Extract the incomes from one person's SSBTEK basis.
	 *
	 * @param  agencyBasis the per-agency SSBTEK basis (af/csn/fk/skv/so/tns/miv); may be {@code null}
	 * @param  role        whose basis this is (one SSBTEK call = one person)
	 * @return             the normalised incomes found
	 */
	public static List<SsbtekIncome> extract(final Map<String, ?> agencyBasis, final ApplicantRole role) {
		if (agencyBasis == null) {
			return List.of();
		}

		final var fk = asMap(agencyBasis.get(AGENCY_FK));
		final var incomes = new ArrayList<SsbtekIncome>();
		incomes.addAll(extractSocialInsurancePayments(fk, role));
		incomes.addAll(extractPensionAuthorityPayments(fk, role));
		incomes.addAll(extractUnemploymentBenefitPayments(asMap(agencyBasis.get(AGENCY_SO)), role));
		incomes.addAll(extractStudySupportPayments(asMap(agencyBasis.get(AGENCY_CSN)), role));
		return List.copyOf(incomes);
	}

	/**
	 * fk.formansinformation -> utbetalningsuppgift(*) (effectuated) and preliminarautbetalningar(*) (preliminary):
	 * Försäkringskassans utbetalningar. Both are {@code LefiUtbetalningUppgift} - the same shape - each carrying
	 * {@code nettobelopp.summa}, {@code datum}, the benefit via {@code formansfamilj.beskrivning}, and the period it
	 * covers via {@code period}.
	 * <p>
	 * Reading both lists cannot double-count: per the contract, a payment stops appearing under
	 * {@code preliminarautbetalningar} once it is effectuated and moves to {@code utbetalningsuppgift}, so the same
	 * payment is never present in both at once.
	 * <p>
	 * The sub-benefit, the regelverk's amount type and the day count live one level down, on the payment's
	 * {@code utbetalningsdetalj} rows - {@code forman} is the sub-benefit, {@code beloppstyp} is the rålista's
	 * Beloppstyp column, and {@code dagar} is "antal avsedda dagar". The payment's own {@code typ} is the payout method
	 * (Månad / Daglig / Retro), which is the regelverk's Typ column and not its Beloppstyp; feeding it as the amount
	 * type is why the rålista's kvittning and Avdrag Soc rows could never match.
	 * <p>
	 * The detail fields are only lifted when the payment has exactly one detail row. The contract calls the relationship
	 * between a detail's benefit and the payment's benefit family "nästan ett ett-till-ett förhållande", and a payment
	 * split over several rows (an amount plus its deductions) has no single sub-benefit or amount type to speak of. The
	 * amount itself always stays the payment-level net: summing detail rows would change what is transferred into the
	 * calculation, which is a decision about money and not one to take while reading a schema.
	 */
	private static List<SsbtekIncome> extractSocialInsurancePayments(final Map<String, Object> fk, final ApplicantRole role) {
		final var formansinformation = asMap(fk.get("formansinformation"));
		final var incomes = new ArrayList<SsbtekIncome>();
		incomes.addAll(socialInsurancePayments(asList(formansinformation.get("utbetalningsuppgift")), role));
		incomes.addAll(socialInsurancePayments(asList(formansinformation.get("preliminarautbetalningar")), role));
		return incomes;
	}

	private static List<SsbtekIncome> socialInsurancePayments(final List<Object> paymentItems, final ApplicantRole role) {
		final var incomes = new ArrayList<SsbtekIncome>();
		for (final var paymentItem : paymentItems) {
			final var payment = asMap(paymentItem);
			final var amount = decimal(asMap(payment.get("nettobelopp")).get("summa"));
			if (amount != null) {
				final var benefitFamily = asMap(payment.get("formansfamilj"));
				final var benefit = ofNullable(str(benefitFamily.get("beskrivning"))).orElseGet(() -> str(benefitFamily.get("id")));
				final var details = asList(payment.get("utbetalningsdetalj"));
				final var detail = singleDetail(details);
				final var period = asMap(payment.get("period"));
				incomes.add(new SsbtekIncome(
					benefit,
					code(detail.get("forman")),
					code(detail.get("beloppstyp")),
					amount,
					date(payment.get("datum")),
					date(period.get("fran")),
					date(period.get("till")),
					decimal(detail.get("dagar")),
					role));
			}
		}
		return incomes;
	}

	/**
	 * fk -> utbetalningar(*) (effectuated) and preliminaraUtbetalningar(*) (preliminary): Pensionsmyndighetens
	 * utbetalningar, {@code LefiPmFormanUtbetalning}. Unlike Försäkringskassans payments, {@code nettobelopp} here is a
	 * plain integer rather than a {@code {summa}} object, and the period keys are {@code from}/{@code tom} rather than
	 * {@code fran}/{@code till} - a schema quirk that silently dropped every PM row before this method existed.
	 * <p>
	 * The payload carries no benefit name for a PM payment, so the benefit is fixed per source list: {@code "PM"} for
	 * the effectuated list, {@code "PM-Prel"} for the preliminary one. These are the exact strings
	 * {@code Decision_inkomstRalista} (rows Rule_ral_pm / Rule_ral_pmprel) and {@code Decision_inkomstTroskel} key off,
	 * not values read from the response.
	 * <p>
	 * Sub-benefit and amount type follow the same single-row rule as the FK detail rows: only lifted from
	 * {@code utbetalningsrader} when the payment has exactly one row, via {@code utbetalningsforman}/{@code beloppstyp}.
	 * The rows are never summed into the amount - the row amounts can exceed the payment's own gross, and summing them
	 * would be a decision about money rather than a reading of the schema. PM carries no day count.
	 */
	private static List<SsbtekIncome> extractPensionAuthorityPayments(final Map<String, Object> fk, final ApplicantRole role) {
		final var incomes = new ArrayList<SsbtekIncome>();
		incomes.addAll(pensionAuthorityPayments(asList(fk.get("utbetalningar")), PM_BENEFIT, role));
		incomes.addAll(pensionAuthorityPayments(asList(fk.get("preliminaraUtbetalningar")), PM_BENEFIT_PRELIMINARY, role));
		return incomes;
	}

	private static List<SsbtekIncome> pensionAuthorityPayments(final List<Object> paymentItems, final String benefit, final ApplicantRole role) {
		final var incomes = new ArrayList<SsbtekIncome>();
		for (final var paymentItem : paymentItems) {
			final var payment = asMap(paymentItem);
			final var amount = decimal(payment.get("nettobelopp"));
			if (amount != null) {
				final var period = asMap(payment.get("utbetalningsperiod"));
				final var row = singleDetail(asList(payment.get("utbetalningsrader")));
				incomes.add(new SsbtekIncome(
					benefit,
					code(row.get("utbetalningsforman")),
					code(row.get("beloppstyp")),
					amount,
					date(payment.get("utbetalningsdatum")),
					date(period.get("from")),
					date(period.get("tom")),
					null,
					role));
			}
		}
		return incomes;
	}

	/**
	 * so -> ArbetsloshetsersattningLista -> Arbetsloshetsersattning(*) -> Utbetalningar(*) -> NettoEfterSkatt /
	 * Utbetalningsdatum. Benefit "Arbetslöshetsersättning" maps to the financial calculation category "A-kassa/Alfa".
	 */
	private static List<SsbtekIncome> extractUnemploymentBenefitPayments(final Map<String, Object> so, final ApplicantRole role) {
		final var incomes = new ArrayList<SsbtekIncome>();
		for (final var benefitItem : asList(asMap(so.get("ArbetsloshetsersattningLista")).get("Arbetsloshetsersattning"))) {
			for (final var paymentItem : asList(asMap(benefitItem).get("Utbetalningar"))) {
				final var payment = asMap(paymentItem);
				final var amount = decimal(payment.get("NettoEfterSkatt"));
				if (amount != null) {
					incomes.add(new SsbtekIncome("Arbetslöshetsersättning", null, null, amount,
						date(payment.get("Utbetalningsdatum")),
						date(payment.get("AvserFrom")), date(payment.get("AvserTom")),
						decimal(payment.get("Ersattningsdagar")), role));
				}
			}
		}
		return incomes;
	}

	/**
	 * csn -> Personer.Person(*) -> {@code Studiemedel}/{@code Studiehjalp}/{@code Studiestartsstod}/
	 * {@code Omstallningsstudiestod} -> Arenden.Arende(*) -> UtbetalningsPlan.Utbetalning(*). One income per payment;
	 * the amount is always the payment's own {@code totbelopp}, never a sum of the underlying {@code Belopp} rows -
	 * the same payment-level-only reasoning as the FK/PM extraction, since the rows can add up to more than the
	 * payment's own total and summing them is a decision about money, not a schema reading.
	 * <p>
	 * Both {@code Utbetald} and {@code Planerad} payments are included; {@code utbetstatus} is deliberately not used to
	 * filter. A payment planned for the control period is still income for that period, and the period selection
	 * already filters by month - this is the one place to change that if a later decision needs to treat planned
	 * payments differently.
	 * <p>
	 * A sub-tree the person has nothing in (e.g. no studiehjälp) converts from XML to {@code null}, not an empty map,
	 * so every lookup here goes through the defensive {@code asMap}/{@code asList} helpers.
	 */
	private static List<SsbtekIncome> extractStudySupportPayments(final Map<String, Object> csn, final ApplicantRole role) {
		final var incomes = new ArrayList<SsbtekIncome>();
		for (final var personItem : asList(asMap(csn.get("Personer")).get("Person"))) {
			final var person = asMap(personItem);
			incomes.addAll(studySupportPayments(person.get("Studiemedel"), CSN_STUDIEMEDEL, role));
			incomes.addAll(studySupportPayments(person.get("Studiehjalp"), CSN_STUDIEHJALP, role));
			incomes.addAll(studySupportPayments(person.get("Studiestartsstod"), CSN_STUDIESTARTSSTOD, role));
			incomes.addAll(studySupportPayments(person.get("Omstallningsstudiestod"), CSN_OMSTALLNINGSSTUDIESTOD, role));
		}
		return incomes;
	}

	/**
	 * One study-support sub-tree's ärenden and their payments. The sub-benefit is the ärende's {@code klartext}
	 * (falling back to {@code stodform}); the amount type follows the same single-row rule as FK/PM, but resolved
	 * across all of a payment's {@code Utbetaldtid}s - a payment covering several weeks still has one amount type when
	 * every one of those weeks resolves to the same single {@code Belopp} row.
	 */
	private static List<SsbtekIncome> studySupportPayments(final Object supportTree, final String benefit, final ApplicantRole role) {
		final var incomes = new ArrayList<SsbtekIncome>();
		for (final var arendeItem : asList(asMap(asMap(supportTree).get("Arenden")).get("Arende"))) {
			final var arende = asMap(arendeItem);
			final var subBenefit = ofNullable(str(arende.get("klartext"))).orElseGet(() -> str(arende.get("stodform")));
			final var plan = asMap(arende.get("UtbetalningsPlan"));
			for (final var paymentItem : asList(plan.get("Utbetalning"))) {
				final var payment = asMap(paymentItem);
				final var amount = decimal(payment.get("totbelopp"));
				if (amount != null) {
					final var weeks = asList(asMap(payment.get("Utbetaldatider")).get("Utbetaldtid"));
					incomes.add(new SsbtekIncome(
						benefit,
						subBenefit,
						studySupportAmountType(weeks),
						amount,
						csnDate(payment.get("utbetdatum")),
						earliestWeekStart(weeks),
						latestWeekEnd(weeks),
						null,
						role));
				}
			}
		}
		return incomes;
	}

	/** The amount type from the payment's single {@code Belopp} row, across all its {@code Utbetaldtid}s, or nothing. */
	private static String studySupportAmountType(final List<Object> weeks) {
		final var allAmounts = new ArrayList<Object>();
		for (final var weekItem : weeks) {
			allAmounts.addAll(asList(asMap(asMap(weekItem).get("Beloppen")).get("Belopp")));
		}
		final var row = singleDetail(allAmounts);
		return ofNullable(str(row.get("klartext"))).orElseGet(() -> str(row.get("beloppstyp")));
	}

	/** The earliest week's Monday across the payment's {@code Utbetaldtid}s, or {@code null} when there are none. */
	private static LocalDate earliestWeekStart(final List<Object> weeks) {
		LocalDate earliest = null;
		for (final var weekItem : weeks) {
			final var start = isoWeekDate(str(asMap(weekItem).get("startvecka")), DayOfWeek.MONDAY);
			if ((start != null) && ((earliest == null) || start.isBefore(earliest))) {
				earliest = start;
			}
		}
		return earliest;
	}

	/** The latest week's Sunday across the payment's {@code Utbetaldtid}s, or {@code null} when there are none. */
	private static LocalDate latestWeekEnd(final List<Object> weeks) {
		LocalDate latest = null;
		for (final var weekItem : weeks) {
			final var end = isoWeekDate(str(asMap(weekItem).get("slutvecka")), DayOfWeek.SUNDAY);
			if ((end != null) && ((latest == null) || end.isAfter(latest))) {
				latest = end;
			}
		}
		return latest;
	}

	/**
	 * Extract each responding organisation's answer <em>quality inputs</em> from one person's SSBTEK basis.
	 * <p>
	 * Kept separate from the incomes on purpose: an organisation that could not answer contributes no incomes, and a
	 * caller that only looks at the income list cannot tell that apart from an organisation that answered "this person
	 * has nothing with us". Reading the payments without reading the status is how an unanswerable a-kassa silently
	 * becomes "ingen a-kassa-inkomst" in the normberäkning.
	 * <p>
	 * The status codes are <em>not</em> interpreted here - {@code Decision_ssbtekSvarKvalitet} does that, at runtime.
	 *
	 * @param  agencyBasis the per-agency SSBTEK basis (af/csn/fk/skv/so/tns/miv); may be {@code null}
	 * @return             one entry per responding organisation, in the order SSBTEK returned them
	 */
	public static List<AgencyAnswer> extractAnswers(final Map<String, ?> agencyBasis) {
		if (agencyBasis == null) {
			return List.of();
		}
		return extractUnemploymentBenefitAnswers(asMap(agencyBasis.get(AGENCY_SO)));
	}

	/**
	 * The AF/FK facts that gate the dagersättning day check. An agency that is absent from the basis or answered with
	 * financial-aid's {@code error} object is <em>unread</em> ({@code null}), never "answered with nothing" - an unread
	 * gate makes caremanagement skip the check, while a wrongly answered one would raise a warning.
	 * <p>
	 * af: {@code Svar.BeslutInfo.EkonomiskaBeslut.Beslut(*)} with {@code BeslutFrom}/{@code BeslutTom}. {@code BeslutInfo}
	 * is optional in the contract, so an AF answer without it is "no decision". An af block without {@code Svar} is not
	 * an answer at all. fk: {@code formansinformation.programjobdagar(*)} with {@code antalForbrukade} and
	 * {@code harForbrukatMaxAntal}; several rows are folded into the highest count and "any row says all used".
	 * <p>
	 * Neither path has been seen in a real SSBTEK answer yet - they are read from the AF XSD and the LEFI schema.
	 *
	 * @param  agencyBasis the per-agency SSBTEK basis (af/csn/fk/skv/so/tns/miv); may be {@code null}
	 * @return             the facts, with {@code null} for every agency that was not read
	 */
	public static DayCheckFacts extractDayCheckFacts(final Map<String, ?> agencyBasis) {
		if (agencyBasis == null) {
			return new DayCheckFacts(null, null, null);
		}
		final var fk = answered(agencyBasis.get(AGENCY_FK));
		final var programDays = asList(asMap(fk.get("formansinformation")).get("programjobdagar")).stream()
			.map(SsbtekIncomeExtractor::asMap)
			.toList();

		Integer consumedDays = null;
		Boolean allDaysConsumed = null;
		if (!fk.isEmpty()) {
			consumedDays = programDays.stream()
				.map(row -> decimal(row.get("antalForbrukade")))
				.filter(Objects::nonNull)
				.map(BigDecimal::intValue)
				.max(Integer::compare)
				.orElse(null);
			allDaysConsumed = programDays.stream().anyMatch(row -> "true".equalsIgnoreCase(str(row.get("harForbrukatMaxAntal"))));
		}
		return new DayCheckFacts(economicDecisionPeriods(asMap(answered(agencyBasis.get(AGENCY_AF)).get("Svar"))), consumedDays, allDaysConsumed);
	}

	/** {@code null} when AF gave no {@code Svar}; otherwise its decision periods, empty when it reports none. */
	private static List<DayCheckFacts.DecisionPeriod> economicDecisionPeriods(final Map<String, Object> svar) {
		if (svar.isEmpty()) {
			return null;
		}
		return asList(asMap(asMap(svar.get("BeslutInfo")).get("EkonomiskaBeslut")).get("Beslut")).stream()
			.map(SsbtekIncomeExtractor::asMap)
			.map(beslut -> new DayCheckFacts.DecisionPeriod(date(beslut.get("BeslutFrom")), date(beslut.get("BeslutTom"))))
			.filter(period -> (period.from() != null) || (period.to() != null))
			.toList();
	}

	/** The agency's answer, or an empty map when it is absent or financial-aid reported it as an {@code error}. */
	private static Map<String, Object> answered(final Object agency) {
		final var map = asMap(agency);
		if (map.containsKey("error")) {
			return Map.of();
		}
		return map;
	}

	/**
	 * so -> ArbetsloshetsersattningLista -> Arbetsloshetsersattning(*) -> SvarandeOrganisation /
	 * StatusSvarandeOrganisation.
	 */
	private static List<AgencyAnswer> extractUnemploymentBenefitAnswers(final Map<String, Object> so) {
		final var answers = new ArrayList<AgencyAnswer>();
		for (final var benefitItem : asList(asMap(so.get("ArbetsloshetsersattningLista")).get("Arbetsloshetsersattning"))) {
			final var benefit = asMap(benefitItem);
			answers.add(new AgencyAnswer(
				AGENCY_SO,
				str(benefit.get("SvarandeOrganisation")),
				str(benefit.get("StatusSvarandeOrganisation")),
				benefit.get("AnsoktOmErsattning") != null,
				!asList(benefit.get("Utbetalningar")).isEmpty()));
		}
		return List.copyOf(answers);
	}

	/** The Swedish label of a code object ({@code beskrivning}), falling back to {@code kod}, then to {@code id}. */
	private static String code(final Object value) {
		final var map = asMap(value);
		return ofNullable(str(map.get("beskrivning")))
			.or(() -> ofNullable(str(map.get("kod"))))
			.orElseGet(() -> str(map.get("id")));
	}

	// ---- defensive untyped-map navigation -----------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private static Map<String, Object> asMap(final Object value) {
		if (value instanceof final Map<?, ?> map) {
			return (Map<String, Object>) map;
		}
		return Map.of();
	}

	/** A repeated XML element is a single object or a list once converted to JSON; normalise both to a list. */
	private static List<Object> asList(final Object value) {
		if (value == null) {
			return List.of();
		}
		if (value instanceof final List<?> list) {
			return List.copyOf(list);
		}
		return List.of(value);
	}

	private static String str(final Object value) {
		return ofNullable(value).map(Object::toString).map(String::trim).filter(text -> !text.isEmpty()).orElse(null);
	}

	/** The single detail row's fields, or nothing at all when the payment is split over several rows. */
	private static Map<String, Object> singleDetail(final List<Object> details) {
		if (details.size() == 1) {
			return asMap(details.getFirst());
		}
		return Map.of();
	}

	private static BigDecimal decimal(final Object value) {
		return ofNullable(value).map(Object::toString).map(String::trim).map(SsbtekIncomeExtractor::parseDecimal).orElse(null);
	}

	private static BigDecimal parseDecimal(final String text) {
		try {
			return new BigDecimal(text);
		} catch (final NumberFormatException e) {
			return null;
		}
	}

	private static LocalDate date(final Object value) {
		return ofNullable(value).map(Object::toString).map(String::trim).map(SsbtekIncomeExtractor::parseDate).orElse(null);
	}

	/** SSBTEK dates are ISO calendar dates; some carry a trailing time/offset, so parse the leading {@code yyyy-MM-dd}. */
	private static LocalDate parseDate(final String text) {
		if (text.length() < 10) {
			return null;
		}
		try {
			return LocalDate.parse(text.substring(0, 10));
		} catch (final RuntimeException e) {
			return null;
		}
	}

	/**
	 * CSN payment dates are {@code yyyyMMdd}. The contract's literal {@code "0"} means "no payment date decided yet"
	 * and must become {@code null} explicitly, rather than relying on it happening to fail to parse.
	 */
	private static LocalDate csnDate(final Object value) {
		final var text = str(value);
		if ((text == null) || "0".equals(text)) {
			return null;
		}
		try {
			return LocalDate.parse(text, DateTimeFormatter.BASIC_ISO_DATE);
		} catch (final RuntimeException e) {
			return null;
		}
	}

	/**
	 * The Monday ({@link DayOfWeek#MONDAY}) or Sunday ({@link DayOfWeek#SUNDAY}) of a CSN {@code yyyyWW} ISO
	 * week-of-week-based-year, e.g. {@code "202635"} -> week 35 of 2026.
	 */
	private static LocalDate isoWeekDate(final String yyyyWw, final DayOfWeek dayOfWeek) {
		if ((yyyyWw == null) || (yyyyWw.length() != 6)) {
			return null;
		}
		try {
			final var year = Integer.parseInt(yyyyWw.substring(0, 4));
			final var week = Integer.parseInt(yyyyWw.substring(4, 6));
			return LocalDate.of(year, 1, 1)
				.with(IsoFields.WEEK_BASED_YEAR, (long) year)
				.with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, (long) week)
				.with(ChronoField.DAY_OF_WEEK, (long) dayOfWeek.getValue());
		} catch (final RuntimeException e) {
			return null;
		}
	}
}
