package se.sundsvall.operaton.workers.financialaid.rules;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * Whether a financial-aid basis is fit to run the SSBTEK income rules over.
 *
 * <p>
 * Every {@code *Svar} in the SSBTEK contract is a choice of {@code data} or {@code error}, and financial-aid surfaces a
 * failed agency as <code>{"error": {"kalla", "felkod", "felmeddelande"}}</code> under that agency's key. Without this
 * check an agency that could not answer is indistinguishable from an agency that answered "this person has nothing
 * with us" — the difference between a case that needs a retry and a normberäkning that is simply missing an income.
 * Verksamhetens regelverk is explicit about it: on a read failure the rules must not run at all, and the handläggare
 * gets a warning instead.
 * </p>
 *
 * <p>
 * The check reads the parsed JSON tree rather than a {@code Map}, deliberately: the FEEL engine on the classpath
 * contributes a Jackson module, so {@code readValue} into a {@code Map<String, Object>} binds nested objects to that
 * module's own map type and an {@code instanceof java.util.Map} test silently misses every agency error.
 * </p>
 *
 * <p>
 * Only the agencies {@link SsbtekIncomeExtractor} actually reads count. A failure at Arbetsförmedlingen,
 * Skatteverket, Transportstyrelsen or Migrationsverket changes nothing in the income rules today, and stopping the
 * whole evaluation over one would leave the handläggare with no assessment at all for a block we do not use.
 * <strong>This split is an assumption</strong> pending verksamhetens answer on whether a partial failure counts as
 * "fel att läsa SSBTEK".
 * </p>
 */
public final class SsbtekAvailability {

	/** Key financial-aid nests a failed agency's error under. Mirrors {@code ResponseMapper.KEY_ERROR}. */
	public static final String KEY_ERROR = "error";

	/** The agencies the income rules read. A failure in any of them makes the basis unusable. */
	private static final List<String> INCOME_BEARING_AGENCIES = List.of("fk", "so", "csn");

	private SsbtekAvailability() {}

	/**
	 * {@code true} when the basis carries a read failure: either the whole fetch failed (a top-level {@code error},
	 * which is also how a failed call to financial-aid itself is represented) or one of the income-bearing agencies did.
	 *
	 * @param basis the parsed financial-aid basis; an empty object is <em>not</em> a failure — that is a person with no
	 *              data, or a household member who does not exist
	 */
	public static boolean hasReadFailure(final JsonNode basis) {
		if (basis == null || basis.isNull() || basis.isMissingNode() || basis.isEmpty()) {
			return false;
		}
		if (basis.has(KEY_ERROR)) {
			return true;
		}
		return INCOME_BEARING_AGENCIES.stream().anyMatch(agency -> basis.path(agency).has(KEY_ERROR));
	}
}
