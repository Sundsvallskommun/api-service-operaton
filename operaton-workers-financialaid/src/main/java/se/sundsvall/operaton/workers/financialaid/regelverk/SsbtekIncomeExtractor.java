package se.sundsvall.operaton.workers.financialaid.regelverk;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Optional.ofNullable;

/**
 * Turns the api-service-financial-aid SSBTEK basis — the untyped, per-agency map (parsed from the worker's
 * {@code financialAidBasis} JSON) — into normalised {@link SsbtekIncome}s for the regelverk. Most agencies are a
 * generic
 * XML→JSON conversion of the SSBTEK SOAP (keys mirror the XML element names, PascalCase); <b>fk</b> is delivered as
 * LEFI
 * JSON, so its keys are the LEFI property names (lowercase). Repeated elements arrive as a single object or a list, so
 * navigation is defensive throughout. One SSBTEK basis is one person's data, so the caller supplies the
 * {@link ApplicantRole}.
 *
 * <p>
 * Grounded agencies: <b>fk</b> (Försäkringskassan förmånsutbetalningar, from {@code utbetalningar}) and <b>so</b>
 * (arbetslöshetsersättning). The FK förmån comes from the payment's {@code formansfamilj.beskrivning}; FK förmåner not
 * on
 * the rålista surface as warnings downstream. <b>csn</b> amounts are not yet grounded — an extension point. Non-income
 * agencies (af/tns/miv, skv capital) are intentionally not read.
 */
public final class SsbtekIncomeExtractor {

	private static final String AGENCY_FK = "fk";
	private static final String AGENCY_SO = "so";

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

		final var incomes = new ArrayList<SsbtekIncome>();
		incomes.addAll(extractFkUtbetalningar(asMap(agencyBasis.get(AGENCY_FK)), role));
		incomes.addAll(extractArbetsloshetsersattning(asMap(agencyBasis.get(AGENCY_SO)), role));
		return List.copyOf(incomes);
	}

	/**
	 * fk → utbetalningar(*): each effectuated FK/PM payment carries {@code nettobelopp.summa}, {@code datum}, the förmån
	 * via {@code formansfamilj.beskrivning} and the beloppstyp via {@code typ.beskrivning}.
	 */
	private static List<SsbtekIncome> extractFkUtbetalningar(final Map<String, Object> fk, final ApplicantRole role) {
		final var incomes = new ArrayList<SsbtekIncome>();
		for (final var utbetalning : asList(fk.get("utbetalningar"))) {
			final var payment = asMap(utbetalning);
			final var amount = decimal(asMap(payment.get("nettobelopp")).get("summa"));
			if (amount != null) {
				final var formansfamilj = asMap(payment.get("formansfamilj"));
				final var forman = ofNullable(str(formansfamilj.get("beskrivning"))).orElseGet(() -> str(formansfamilj.get("id")));
				final var beloppstyp = str(asMap(payment.get("typ")).get("beskrivning"));
				incomes.add(new SsbtekIncome(forman, null, beloppstyp, amount, date(payment.get("datum")), role));
			}
		}
		return incomes;
	}

	/**
	 * so → ArbetsloshetsersattningLista → Arbetsloshetsersattning(*) → Utbetalningar(*) → NettoEfterSkatt /
	 * Utbetalningsdatum. Förmån "Arbetslöshetsersättning" (→ FC "A-kassa/Alfa").
	 */
	private static List<SsbtekIncome> extractArbetsloshetsersattning(final Map<String, Object> so, final ApplicantRole role) {
		final var incomes = new ArrayList<SsbtekIncome>();
		for (final var ersattning : asList(asMap(so.get("ArbetsloshetsersattningLista")).get("Arbetsloshetsersattning"))) {
			for (final var utbetalning : asList(asMap(ersattning).get("Utbetalningar"))) {
				final var payment = asMap(utbetalning);
				final var amount = decimal(payment.get("NettoEfterSkatt"));
				if (amount != null) {
					incomes.add(new SsbtekIncome("Arbetslöshetsersättning", null, null, amount, date(payment.get("Utbetalningsdatum")), role));
				}
			}
		}
		return incomes;
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
}
