package se.sundsvall.operaton.workers.financialaid.rules;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SsbtekAvailabilityTest {

	private static final String ERROR = "{\"kalla\":\"FK\",\"felkod\":\"2001\",\"felmeddelande\":[\"Tjänsten svarar inte\"]}";

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void topLevelErrorIsAReadFailure() {
		assertThat(SsbtekAvailability.hasReadFailure(parse("{\"error\":" + ERROR + "}"))).isTrue();
	}

	@Test
	void incomeBearingAgencyErrorIsAReadFailure() {
		assertThat(SsbtekAvailability.hasReadFailure(parse("{\"fk\":{\"error\":" + ERROR + "}}"))).isTrue();
		assertThat(SsbtekAvailability.hasReadFailure(parse("{\"so\":{\"error\":" + ERROR + "}}"))).isTrue();
		assertThat(SsbtekAvailability.hasReadFailure(parse("{\"csn\":{\"error\":" + ERROR + "}}"))).isTrue();
	}

	@Test
	void errorInAnAgencyTheRulesDoNotReadIsNotAReadFailure() {
		// af/skv/tns/miv carry no income the rules evaluate, so stopping the whole run over one would leave the
		// handläggare without an assessment for a block we never use.
		assertThat(SsbtekAvailability.hasReadFailure(parse("{\"af\":{\"error\":" + ERROR + "}}"))).isFalse();
		assertThat(SsbtekAvailability.hasReadFailure(parse("{\"skv\":{\"error\":" + ERROR + "}}"))).isFalse();
		assertThat(SsbtekAvailability.hasReadFailure(parse("{\"tns\":{\"error\":" + ERROR + "}}"))).isFalse();
		assertThat(SsbtekAvailability.hasReadFailure(parse("{\"miv\":{\"error\":" + ERROR + "}}"))).isFalse();
	}

	@Test
	void oneFailedAgencyAmongAnsweringOnesIsStillAReadFailure() {
		assertThat(SsbtekAvailability.hasReadFailure(parse("{\"fk\":{\"utbetalningar\":[]},\"csn\":{\"error\":" + ERROR + "}}"))).isTrue();
	}

	@Test
	void answeredAgenciesAreNotAReadFailure() {
		assertThat(SsbtekAvailability.hasReadFailure(parse("{\"fk\":{\"utbetalningar\":[]},\"so\":{}}"))).isFalse();
	}

	@Test
	void anEmptyOrAbsentBasisIsNotAReadFailure() {
		// A person with nothing at any agency, or a co-applicant who does not exist — both are answers, not failures.
		assertThat(SsbtekAvailability.hasReadFailure(parse("{}"))).isFalse();
		assertThat(SsbtekAvailability.hasReadFailure(null)).isFalse();
	}

	@Test
	void anAgencyBlockThatIsNotAnObjectIsNotAReadFailure() {
		assertThat(SsbtekAvailability.hasReadFailure(parse("{\"fk\":\"unexpected\"}"))).isFalse();
	}

	private JsonNode parse(final String json) {
		try {
			return objectMapper.readTree(json);
		} catch (final JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}
}
