package org.jbehave.core.model;

import static java.lang.Boolean.parseBoolean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * <p>
 * Facade responsible for transforming table string representations. It allows
 * the registration of several {@link TableTransformer} instances by name.
 * </p>
 * <p>
 * Some Transformers are provided out-of-the-box:
 * <ul>
 * <li>{@link TableTransformers.FromLandscape FromLandscape}: registered under
 * name {@link TableTransformers#FROM_LANDSCAPE}</li>
 * <li>{@link TableTransformers.Formatting Formatting}: registered under name
 * {@link TableTransformers#FORMATTING}</li>
 * </ul>
 * </p>
 */
public class TableTransformers {

	public static final String FROM_LANDSCAPE = "FROM_LANDSCAPE";
	public static final String FORMATTING = "FORMATTING";
	private final Map<String, TableTransformer> transformers = new HashMap<String, TableTransformer>();

	public TableTransformers() {
		useTransformer(FROM_LANDSCAPE, new FromLandscape());
		useTransformer(FORMATTING, new Formatting());
	}

	public String transform(String transformerName, String tableAsString,
			Properties properties) {
		TableTransformer transformer = transformers.get(transformerName);
		if (transformer != null) {
			return transformer.transform(tableAsString, properties);
		}
		return tableAsString;
	}

	public void useTransformer(String name, TableTransformer transformer) {
		transformers.put(name, transformer);
	}

	public interface TableTransformer {
		String transform(String tableAsString, Properties properties);
	}

	public static class FromLandscape implements TableTransformer {

		private static final String ROW_SEPARATOR = "\n";

		public String transform(String tableAsString, Properties properties) {
			boolean trim = parseBoolean(properties.getProperty("trim", "true"));
			String ignorableSeparator = properties.getProperty(
					"ignorableSeparator", "|--");
			String headerSeparator = properties.getProperty("headerSeparator",
					"|");
			String valueSeparator = properties.getProperty("valueSeparator",
					"|");
			Map<String, List<String>> data = new LinkedHashMap<String, List<String>>();
			for (String rowAsString : tableAsString.split(ROW_SEPARATOR)) {
				if (ignoreRow(rowAsString, ignorableSeparator)) {
					continue;
				} else {
					List<String> values = TableUtils.parseRow(rowAsString,
							valueSeparator, trim);
					String header = values.get(0);
					List<String> rowValues = new ArrayList<String>(values);
					rowValues.remove(0);
					data.put(header, rowValues);
				}
			}
			StringBuilder builder = new StringBuilder();
			int numberOfRows = 1;
			builder.append(headerSeparator);
			for (String header : data.keySet()) {
				builder.append(header).append(headerSeparator);
				numberOfRows = data.get(header).size();
			}
			builder.append(ROW_SEPARATOR);
			for (int r = 0; r < numberOfRows; r++) {
				builder.append(valueSeparator);
				for (List<String> rows : data.values()) {
					builder.append(rows.get(r)).append(valueSeparator);
				}
				builder.append(ROW_SEPARATOR);
			}
			return builder.toString();
		}

		private boolean ignoreRow(String rowAsString, String ignorableSeparator) {
			return rowAsString.startsWith(ignorableSeparator)
					|| rowAsString.length() == 0;
		}

	}

	public static class Formatting implements TableTransformer {

		private static final String ROW_SEPARATOR = "\n";

		public String transform(String tableAsString, Properties properties) {
			boolean trim = parseBoolean(properties.getProperty("trim", "true"));
			String ignorableSeparator = properties.getProperty(
					"ignorableSeparator", "|--");
			String headerSeparator = properties.getProperty("headerSeparator",
					"|");
			String valueSeparator = properties.getProperty("valueSeparator",
					"|");
			List<List<String>> data = new ArrayList<List<String>>();
			for (String rowAsString : tableAsString.split(ROW_SEPARATOR)) {
				if (ignoreRow(rowAsString, ignorableSeparator)) {
					continue;
				} else if (rowAsString.contains(headerSeparator)) {
					data.add(TableUtils.parseRow(rowAsString, headerSeparator,
							trim));
				} else {
					data.add(TableUtils.parseRow(rowAsString, valueSeparator,
							trim));
				}

			}

			StringBuilder builder = new StringBuilder();
			Map<Integer, Integer> maxWidths = maxWidth(data);
			for (int r = 0; r < data.size(); r++) {
				String formattedRow = formatRow(data.get(r), maxWidths,
						(r == 0 ? headerSeparator : valueSeparator));
				builder.append(formattedRow).append(ROW_SEPARATOR);
			}
			return builder.toString();
		}

		private boolean ignoreRow(String rowAsString, String ignorableSeparator) {
			return rowAsString.startsWith(ignorableSeparator)
					|| rowAsString.length() == 0;
		}

		private Map<Integer, Integer> maxWidth(List<List<String>> data) {
			Map<Integer, Integer> maxWidths = new HashMap<Integer, Integer>();
			for (List<String> row : data) {
				for (int c = 0; c < row.size(); c++) {
					String cell = row.get(c).trim();
					Integer width = maxWidths.get(c);
					int length = cell.length();
					if (width == null || length > width) {
						width = length;
						maxWidths.put(c, width);
					}
				}
			}

			return maxWidths;

		}

		private String formatRow(List<String> row,
				Map<Integer, Integer> maxWidths, String separator) {
			StringBuilder builder = new StringBuilder();
			builder.append(separator);
			for (int c = 0; c < row.size(); c++) {
				builder.append(formatValue(row.get(c).trim(), maxWidths.get(c)))
						.append(separator);
			}
			return builder.toString();
		}

		private String formatValue(String value, int width) {
			if (value.length() < width) {
				return value + padding(width - value.length());
			}
			return value;
		}

		private String padding(int size) {
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < size; i++) {
				builder.append(' ');
			}
			return builder.toString();
		}

	}

}
