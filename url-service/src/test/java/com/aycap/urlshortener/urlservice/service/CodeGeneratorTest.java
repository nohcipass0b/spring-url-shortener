package com.aycap.urlshortener.urlservice.service;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CodeGeneratorTest {

	private final CodeGenerator generator = new CodeGenerator(7);

	@Test
	void aCodeIsExactlyTheConfiguredLength() {
		assertThat(this.generator.generate()).hasSize(7);
		assertThat(new CodeGenerator(3).generate()).hasSize(3);
		assertThat(new CodeGenerator(22).generate()).hasSize(22);
	}

	@Test
	void everyCharacterIsUrlSafeBase62() {
		for (int i = 0; i < 200; i++) {
			assertThat(this.generator.generate()).matches("[A-Za-z0-9]+");
		}
	}

	@Test
	void codesDoNotRepeatAcrossAThousandDraws() {
		Set<String> seen = new HashSet<>();
		for (int i = 0; i < 1000; i++) {
			seen.add(this.generator.generate());
		}

		assertThat(seen).hasSize(1000);
	}

	@Test
	void codesAreNotSequentialSoOneCannotBeWalkedFromAnother() {
		String first = this.generator.generate();
		String second = this.generator.generate();

		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void aZeroLengthGeneratorProducesAnEmptyCode() {
		assertThat(new CodeGenerator(0).generate()).isEmpty();
	}

}
