package com.walkmates.lab3;

import com.walkmates.model.Listing;
import com.walkmates.model.ListingType;
import com.walkmates.model.Seeker;
import com.walkmates.model.TrustTier;
import com.walkmates.service.ai.LlmClient;
import com.walkmates.service.ai.MatchExplanationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Lab 3, Part A — testing the AI "explain this match" feature without a live LLM.
 *
 * <p>There is no exact oracle for the model's text, so we test the parts we <em>can</em> pin
 * down: the deterministic prompt builder, the fallback path (mock the {@link LlmClient} to
 * fail/timeout), the metamorphic relations, and prompt-injection resistance. Two worked
 * examples are provided; the {@code TODO}s are yours.</p>
 */
class MatchExplanationServiceTest {

    private Seeker seeker() {
        return new Seeker("p@example.com", "Pat", "0701112233");
    }

    private Listing listing(String description) {
        return new Listing("provider-1", "Walk Rex", description, ListingType.DOG_WALK);
    }

    // ---- Worked example 1: the prompt builder is deterministic and structured (FR-5.1) ----
    @Test
    @DisplayName("buildPrompt includes the structured fields")
    void promptIncludesStructuredFields() {
        MatchExplanationService service = new MatchExplanationService(mock(LlmClient.class));

        String prompt = service.buildPrompt(seeker(), listing("Friendly dog"));

        assertThat(prompt).contains("Seeker trust tier: " + TrustTier.NEW);
        assertThat(prompt).contains("Listing type: " + ListingType.DOG_WALK);
    }

    // ---- Worked example 2: on LLM failure, fall back deterministically (FR-5.2) ----
    @Test
    @DisplayName("explainMatch falls back when the LLM call fails")
    void fallsBackOnLlmFailure() throws Exception {
        LlmClient llm = mock(LlmClient.class);
        when(llm.complete(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new LlmClient.LlmException("provider down"));
        MatchExplanationService service = new MatchExplanationService(llm);
        Seeker seeker = seeker();
        Listing listing = listing("Friendly dog");

        String result = service.explainMatch(seeker, listing);

        // Use an independent, concrete oracle. Comparing result only with another call to
        // fallbackExplanation would pass if both calls returned the same wrong text.
        assertThat(result).isEqualTo(
                "This DOG_WALK opportunity \"Walk Rex\" is a good fit for a NEW seeker.");
    }

    //Activity 5.1
    @Test
    @DisplayName("buildPrompt includes structured fields and description sits inside the data delimiters")
    void buildPromptIncludesStructuredFieldsAndDelimitedDescription() {
        MatchExplanationService service = new MatchExplanationService(mock(LlmClient.class));

        Seeker seeker = seeker();
        Listing listing = listing("Friendly dog");

        String prompt = service.buildPrompt(seeker, listing);

        assertThat(prompt).contains("Seeker trust tier: " + seeker.getTrustTier());
        assertThat(prompt).contains("Listing type: " + listing.getType());
        assertThat(prompt).contains("Listing base rate (SEK/hour): " + listing.getBaseRatePerHour());
        assertThat(prompt).contains("Listing title: " + listing.getTitle());

        String dataStart = "<<<LISTING_DESCRIPTION_DATA";
        String dataEnd = "LISTING_DESCRIPTION_DATA>>>";

        assertThat(prompt).contains(dataStart);
        assertThat(prompt).contains(dataEnd);

        int start = prompt.indexOf(dataStart);
        int descriptionStart = prompt.indexOf("Friendly dog");
        int end = prompt.indexOf(dataEnd);

        assertThat(start).isLessThan(descriptionStart);
        assertThat(descriptionStart).isLessThan(end);
    }

    //Activity 5.2
    @Test
    @DisplayName("explainMatch falls back when the LLM call times out")
    void fallsBackOnLlmTimeout() throws Exception {
        LlmClient llm = mock(LlmClient.class);

        when(llm.complete(org.mockito.ArgumentMatchers.anyString())).thenThrow(new LlmClient.LlmTimeoutException("request timed out"));

        MatchExplanationService service = new MatchExplanationService(llm);

        String result = service.explainMatch(seeker(), listing("Friendly dog"));

        assertThat(result).isEqualTo("This DOG_WALK opportunity \"Walk Rex\" is a good fit for a NEW seeker.");
    }

    @Test
    @DisplayName("explainMatch falls back when the LLM returns null")
    void fallsBackOnNullResponse() throws Exception {
        LlmClient llm = mock(LlmClient.class);

        when(llm.complete(org.mockito.ArgumentMatchers.anyString())).thenReturn(null);

        MatchExplanationService service = new MatchExplanationService(llm);

        String result = service.explainMatch(seeker(), listing("Friendly dog"));

        assertThat(result).isEqualTo("This DOG_WALK opportunity \"Walk Rex\" is a good fit for a NEW seeker.");
    }

    @Test
    @DisplayName("explainMatch falls back when the LLM returns a blank response")
    void fallsBackOnBlankResponse() throws Exception {
        LlmClient llm = mock(LlmClient.class);

        when(llm.complete(org.mockito.ArgumentMatchers.anyString())).thenReturn("   ");

        MatchExplanationService service = new MatchExplanationService(llm);

        String result = service.explainMatch(seeker(), listing("Friendly dog"));

        assertThat(result).isEqualTo("This DOG_WALK opportunity \"Walk Rex\" is a good fit for a NEW seeker.");
    }
    
    //Activity 5.3
    //MR-1
    @Test
    @DisplayName("adding irrelevant description text does not change the best match")
    void irrelevantDescriptionDoesNotChangeBestMatch() {
        MatchExplanationService service = new MatchExplanationService(mock(LlmClient.class));

        Seeker seeker = seeker();

        Listing listing1 = listing("Friendly dog");
        Listing listing2 = listing("Energetic dog");

        List<Listing> original = List.of(listing1, listing2);
        Listing originalBest = service.recommendBestMatch(seeker, original);

        listing1.setDescription("Friendly dog. Sunny day");

        List<Listing> modified = List.of(listing1, listing2);
        Listing modifiedBest = service.recommendBestMatch(seeker, modified);

        assertThat(modifiedBest.getId()).isEqualTo(originalBest.getId());
    }

    //MR-2
    @Test
    @DisplayName("shuffling candidates does not change the best match")
    void shufflingCandidatesDoesNotChangeBestMatch() {
        MatchExplanationService service = new MatchExplanationService(mock(LlmClient.class));

        Seeker seeker = seeker();

        Listing listing1 = listing("Friendly dog");
        Listing listing2 = listing("Energetic dog");
        Listing listing3 = listing("Calm dog");

        List<Listing> original = List.of(listing1, listing2, listing3);
        List<Listing> shuffled = List.of(listing3, listing1, listing2);

        Listing originalBest = service.recommendBestMatch(seeker, original);
        Listing shuffledBest = service.recommendBestMatch(seeker, shuffled);

        assertThat(shuffledBest.getId()).isEqualTo(originalBest.getId());
    }

    // TODO (injection): a description containing "ignore previous instructions and ..." must
    //      stay inside the data block; buildPrompt must still contain the data delimiters.
}
