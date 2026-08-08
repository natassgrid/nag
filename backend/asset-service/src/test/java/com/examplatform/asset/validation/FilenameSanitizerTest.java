package com.examplatform.asset.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link FilenameSanitizer}.
 */
@DisplayName("FilenameSanitizer")
class FilenameSanitizerTest {

    private FilenameSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new FilenameSanitizer();
    }

    @Nested
    @DisplayName("sanitize")
    class Sanitize {

        @Test
        @DisplayName("preserves simple valid filename")
        void preservesSimpleFilename() {
            assertThat(sanitizer.sanitize("photo.png")).isEqualTo("photo.png");
        }

        @Test
        @DisplayName("preserves filename with hyphens and underscores")
        void preservesHyphensAndUnderscores() {
            assertThat(sanitizer.sanitize("my-file_2024.jpeg")).isEqualTo("my-file_2024.jpeg");
        }

        @Test
        @DisplayName("strips path components (forward slash)")
        void stripsForwardSlashPath() {
            assertThat(sanitizer.sanitize("/etc/passwd/evil.png")).isEqualTo("evil.png");
        }

        @Test
        @DisplayName("strips path components (backslash)")
        void stripsBackslashPath() {
            assertThat(sanitizer.sanitize("C:\\Users\\test\\image.jpg")).isEqualTo("image.jpg");
        }

        @Test
        @DisplayName("removes path traversal sequences")
        void removesPathTraversal() {
            String result = sanitizer.sanitize("../../etc/passwd.png");
            assertThat(result).doesNotContain("..");
        }

        @Test
        @DisplayName("replaces unsafe characters with underscore")
        void replacesUnsafeChars() {
            String result = sanitizer.sanitize("file<with>special|chars.png");
            assertThat(result).doesNotContain("<").doesNotContain(">").doesNotContain("|");
            assertThat(result).endsWith(".png");
        }

        @Test
        @DisplayName("removes leading dots")
        void removesLeadingDots() {
            String result = sanitizer.sanitize(".hidden-file.txt");
            assertThat(result).doesNotStartWith(".");
        }

        @Test
        @DisplayName("truncates filename exceeding 255 characters")
        void truncatesLongFilename() {
            String longName = "a".repeat(300) + ".png";
            String result = sanitizer.sanitize(longName);
            assertThat(result.length()).isLessThanOrEqualTo(255);
            assertThat(result).endsWith(".png");
        }

        @Test
        @DisplayName("throws on null filename")
        void throwsOnNull() {
            assertThatThrownBy(() -> sanitizer.sanitize(null))
                    .isInstanceOf(AssetValidationException.class)
                    .hasMessageContaining("required");
        }

        @Test
        @DisplayName("throws on blank filename")
        void throwsOnBlank() {
            assertThatThrownBy(() -> sanitizer.sanitize("   "))
                    .isInstanceOf(AssetValidationException.class)
                    .hasMessageContaining("required");
        }

        @Test
        @DisplayName("throws when filename becomes empty after sanitization")
        void throwsWhenEmptyAfterSanitization() {
            assertThatThrownBy(() -> sanitizer.sanitize("///"))
                    .isInstanceOf(AssetValidationException.class)
                    .hasMessageContaining("invalid");
        }

        @Test
        @DisplayName("preserves spaces in filename")
        void preservesSpaces() {
            assertThat(sanitizer.sanitize("my file name.png")).isEqualTo("my file name.png");
        }
    }

    @Nested
    @DisplayName("extractExtension")
    class ExtractExtension {

        @Test
        @DisplayName("extracts simple extension")
        void extractsSimpleExtension() {
            assertThat(sanitizer.extractExtension("photo.png")).isEqualTo("png");
        }

        @Test
        @DisplayName("extracts extension and lowercases it")
        void lowercasesExtension() {
            assertThat(sanitizer.extractExtension("Photo.JPEG")).isEqualTo("jpeg");
        }

        @Test
        @DisplayName("extracts last extension for double extensions")
        void extractsLastExtension() {
            assertThat(sanitizer.extractExtension("archive.tar.gz")).isEqualTo("gz");
        }

        @Test
        @DisplayName("returns empty string when no extension")
        void returnsEmptyForNoExtension() {
            assertThat(sanitizer.extractExtension("noext")).isEmpty();
        }

        @Test
        @DisplayName("returns empty string for null")
        void returnsEmptyForNull() {
            assertThat(sanitizer.extractExtension(null)).isEmpty();
        }

        @Test
        @DisplayName("returns empty string when dot is last char")
        void returnsEmptyForTrailingDot() {
            assertThat(sanitizer.extractExtension("file.")).isEmpty();
        }
    }
}
