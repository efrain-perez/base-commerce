package dev.efrain.gilacommerce.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductCsvRowParserTest {

    private static final String[] HEADERS =
            {"name", "sku", "description", "category", "price", "stock", "weight_kg"};

    @Test
    void parsesAValidRow() throws IOException {
        CSVRecord record = recordOf("Laptop Pro", "SKU-1", "A laptop", "Electronics", "1299.99", "10", "2.1");

        ProductCsvRow row = ProductCsvRowParser.parse(record);

        assertThat(row.sku()).isEqualTo("SKU-1");
        assertThat(row.name()).isEqualTo("Laptop Pro");
        assertThat(row.description()).isEqualTo("A laptop");
        assertThat(row.category()).isEqualTo("Electronics");
        assertThat(row.price()).isEqualByComparingTo(new BigDecimal("1299.99"));
        assertThat(row.stock()).isEqualTo(10);
        assertThat(row.weightKg()).isEqualByComparingTo(new BigDecimal("2.1"));
    }

    @Test
    void blankOptionalFieldsBecomeNull() throws IOException {
        CSVRecord record = recordOf("Mouse", "SKU-2", "", "", "9.99", "5", "");

        ProductCsvRow row = ProductCsvRowParser.parse(record);

        assertThat(row.description()).isNull();
        assertThat(row.category()).isNull();
        assertThat(row.weightKg()).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "'', SKU-3, price is required",
            "abc, SKU-3, price must be a valid decimal number",
            "-1.00, SKU-3, price must not be negative"
    })
    void rejectsInvalidPrice(String price, String sku, String expectedMessage) throws IOException {
        CSVRecord record = recordOf("Widget", sku, "", "", price, "1", "");

        assertThatThrownBy(() -> ProductCsvRowParser.parse(record))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
    }

    @ParameterizedTest
    @CsvSource({
            "'', stock is required",
            "abc, stock must be a valid integer",
            "-5, stock must not be negative"
    })
    void rejectsInvalidStock(String stock, String expectedMessage) throws IOException {
        CSVRecord record = recordOf("Widget", "SKU-4", "", "", "1.00", stock, "");

        assertThatThrownBy(() -> ProductCsvRowParser.parse(record))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
    }

    @Test
    void rejectsBlankSku() throws IOException {
        CSVRecord record = recordOf("Widget", "", "", "", "1.00", "1", "");

        assertThatThrownBy(() -> ProductCsvRowParser.parse(record))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("sku is required");
    }

    @Test
    void rejectsBlankName() throws IOException {
        CSVRecord record = recordOf("", "SKU-5", "", "", "1.00", "1", "");

        assertThatThrownBy(() -> ProductCsvRowParser.parse(record))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name is required");
    }

    @Test
    void rejectsInvalidWeight() throws IOException {
        CSVRecord record = recordOf("Widget", "SKU-6", "", "", "1.00", "1", "abc");

        assertThatThrownBy(() -> ProductCsvRowParser.parse(record))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("weight_kg must be a valid decimal number");
    }

    private CSVRecord recordOf(String... values) throws IOException {
        String line = String.join(",", HEADERS) + "\n" + String.join(",", values);
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(HEADERS)
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .build();
        try (CSVParser parser = CSVParser.parse(new StringReader(line), format)) {
            List<CSVRecord> records = parser.getRecords();
            return records.get(0);
        }
    }
}
