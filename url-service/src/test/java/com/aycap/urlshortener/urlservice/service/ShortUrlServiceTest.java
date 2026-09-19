package com.aycap.urlshortener.urlservice.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aycap.urlshortener.urlservice.exception.ShortUrlNotFoundException;
import com.aycap.urlshortener.urlservice.model.entity.ShortUrl;
import com.aycap.urlshortener.urlservice.repository.ShortUrlRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShortUrlServiceTest {

	private ShortUrlRepository repository;

	private CodeGenerator codeGenerator;

	private ShortUrlService service;

	private final UUID owner = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		this.repository = mock(ShortUrlRepository.class);
		this.codeGenerator = mock(CodeGenerator.class);
		this.service = new ShortUrlService(this.repository, this.codeGenerator, "http://localhost:8081");
		when(this.repository.save(any(ShortUrl.class))).thenAnswer(i -> i.getArgument(0));
	}

	@Test
	void shortenStoresTheLinkUnderAFreshCode() {
		when(this.codeGenerator.generate()).thenReturn("abc1234");
		when(this.repository.existsByCode("abc1234")).thenReturn(false);

		ShortUrl created = this.service.shorten("https://example.com", this.owner);

		assertThat(created.getCode()).isEqualTo("abc1234");
		assertThat(created.getOriginalUrl()).isEqualTo("https://example.com");
		assertThat(created.getUserId()).isEqualTo(this.owner);
		assertThat(created.isActive()).isTrue();
	}

	@Test
	void aCodeThatIsAlreadyTakenIsDiscardedAndAnotherIsDrawn() {
		when(this.codeGenerator.generate()).thenReturn("taken1", "taken2", "free123");
		when(this.repository.existsByCode("taken1")).thenReturn(true);
		when(this.repository.existsByCode("taken2")).thenReturn(true);
		when(this.repository.existsByCode("free123")).thenReturn(false);

		ShortUrl created = this.service.shorten("https://example.com", this.owner);

		assertThat(created.getCode()).isEqualTo("free123");
		verify(this.codeGenerator, times(3)).generate();
		verify(this.repository, times(1)).save(any(ShortUrl.class));
	}

	@Test
	void aGeneratorThatNeverProducesAFreeCodeGivesUpInsteadOfSpinning() {
		when(this.codeGenerator.generate()).thenReturn("always");
		when(this.repository.existsByCode(anyString())).thenReturn(true);

		assertThatThrownBy(() -> this.service.shorten("https://example.com", this.owner))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("5 attempts");

		verify(this.repository, never()).save(any());
	}

	@Test
	void resolvingAnActiveCodeReturnsTheTargetAndCountsTheClick() {
		ShortUrl url = new ShortUrl("abc1234", "https://example.com", this.owner);
		when(this.repository.findByCodeAndActiveTrue("abc1234")).thenReturn(Optional.of(url));

		String target = this.service.resolve("abc1234");

		assertThat(target).isEqualTo("https://example.com");
		assertThat(url.getClickCount()).isEqualTo(1);
	}

	@Test
	void resolvingAnUnknownOrDeactivatedCodeIsANotFound() {
		when(this.repository.findByCodeAndActiveTrue("missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> this.service.resolve("missing")).isInstanceOf(ShortUrlNotFoundException.class);
	}

	@Test
	void listingReturnsOnlyWhatTheCallerOwns() {
		ShortUrl url = new ShortUrl("abc1234", "https://example.com", this.owner);
		when(this.repository.findByUserIdOrderByCreatedAtDesc(this.owner)).thenReturn(List.of(url));

		assertThat(this.service.listOwnedBy(this.owner)).containsExactly(url);
	}

	@Test
	void deactivatingStampsTheTimeAndTakesTheLinkOutOfUse() {
		ShortUrl url = new ShortUrl("abc1234", "https://example.com", this.owner);
		UUID id = UUID.randomUUID();
		when(this.repository.findByIdAndUserId(id, this.owner)).thenReturn(Optional.of(url));

		this.service.deactivate(id, this.owner);

		assertThat(url.isActive()).isFalse();
		assertThat(url.getDeactivatedAt()).isNotNull();
	}

	@Test
	void reActivatingClearsTheDeactivationStampSoTheRowIsNotInBothStates() {
		ShortUrl url = new ShortUrl("abc1234", "https://example.com", this.owner);
		url.deactivate();
		UUID id = UUID.randomUUID();
		when(this.repository.findByIdAndUserId(id, this.owner)).thenReturn(Optional.of(url));

		this.service.activate(id, this.owner);

		assertThat(url.isActive()).isTrue();
		assertThat(url.getDeactivatedAt()).isNull();
	}

	@Test
	void bothTransitionsCanBeRepeatedWithoutChangingAnythingFurther() {
		ShortUrl url = new ShortUrl("abc1234", "https://example.com", this.owner);

		url.activate();
		assertThat(url.isActive()).isTrue();
		assertThat(url.getDeactivatedAt()).isNull();

		url.deactivate();
		Instant first = url.getDeactivatedAt();
		url.deactivate();

		assertThat(url.getDeactivatedAt()).isEqualTo(first);
	}

	@Test
	void someoneElsesLinkIsANotFoundRatherThanAForbidden() {
		UUID id = UUID.randomUUID();
		UUID stranger = UUID.randomUUID();
		when(this.repository.findByIdAndUserId(id, stranger)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> this.service.deactivate(id, stranger)).isInstanceOf(ShortUrlNotFoundException.class);
		assertThatThrownBy(() -> this.service.activate(id, stranger)).isInstanceOf(ShortUrlNotFoundException.class);
	}

	@Test
	void theShortUrlIsTheBaseUrlPlusTheCode() {
		ShortUrl url = new ShortUrl("abc1234", "https://example.com", this.owner);

		assertThat(this.service.shortUrlFor(url)).isEqualTo("http://localhost:8081/r/abc1234");
	}

	@Test
	void trailingSlashesOnTheBaseUrlDoNotProduceADoubleSlash() {
		ShortUrlService trailing = new ShortUrlService(this.repository, this.codeGenerator, "http://localhost:8081///");
		ShortUrl url = new ShortUrl("abc1234", "https://example.com", this.owner);

		assertThat(trailing.shortUrlFor(url)).isEqualTo("http://localhost:8081/r/abc1234");
	}

}
