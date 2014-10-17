/*
 * Copyright 2014, Stratio.
 * Modification and adapations - Copyright 2014, Tuplejump Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tuplejump.stargate.lucene.query;

import com.tuplejump.stargate.lucene.Options;
import com.tuplejump.stargate.lucene.Properties;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.automaton.LevenshteinAutomata;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * A {@link Condition} that implements the fuzzy search query. The similarity measurement is based on the
 * Damerau-Levenshtein (optimal string alignment) algorithm, though you can explicitly choose classic Levenshtein by
 * passing {@code false} to the {@code transpositions} parameter.
 */
public class FuzzyCondition extends Condition {

    public final static int DEFAULT_MAX_EDITS = LevenshteinAutomata.MAXIMUM_SUPPORTED_DISTANCE;
    public final static int DEFAULT_PREFIX_LENGTH = 0;
    public final static int DEFAULT_MAX_EXPANSIONS = 50;
    public final static boolean DEFAULT_TRANSPOSITIONS = true;

    /**
     * The field name
     */
    private final String field;

    /**
     * The field value
     */
    private String value;

    private final Integer maxEdits;
    private final Integer prefixLength;
    private final Integer maxExpansions;
    private final Boolean transpositions;

    /**
     * Returns a new {@link FuzzyCondition}.
     *
     * @param boost          The boost for this query clause. Documents matching this clause will (in addition to the normal
     *                       weightings) have their score multiplied by {@code boost}. If {@code null}, then  DEFAULT_BOOST
     *                       is used as default.
     * @param field          The field name.
     * @param value          The field fuzzy value.
     * @param maxEdits       Must be >= 0 and <= {@link LevenshteinAutomata#MAXIMUM_SUPPORTED_DISTANCE}.
     * @param prefixLength   Length of common (non-fuzzy) prefix
     * @param maxExpansions  The maximum number of terms to match. If this number is greater than
     *                       {@link BooleanQuery#getMaxClauseCount} when the query is rewritten, then the maxClauseCount will be
     *                       used instead.
     * @param transpositions True if transpositions should be treated as a primitive edit operation. If this is false, comparisons
     *                       will implement the classic Levenshtein algorithm.
     */
    @JsonCreator
    public FuzzyCondition(@JsonProperty("boost") Float boost,
                          @JsonProperty("field") String field,
                          @JsonProperty("value") String value,
                          @JsonProperty("maxEdits") Integer maxEdits,
                          @JsonProperty("prefixLength") Integer prefixLength,
                          @JsonProperty("maxExpansions") Integer maxExpansions,
                          @JsonProperty("transpositions") Boolean transpositions) {
        super(boost);

        this.field = field != null ? field.toLowerCase() : null;
        this.value = value;
        this.maxEdits = maxEdits == null ? DEFAULT_MAX_EDITS : maxEdits;
        this.prefixLength = prefixLength == null ? DEFAULT_PREFIX_LENGTH : prefixLength;
        this.maxExpansions = maxExpansions == null ? DEFAULT_MAX_EXPANSIONS : maxExpansions;
        this.transpositions = transpositions == null ? DEFAULT_TRANSPOSITIONS : transpositions;
    }

    /**
     * Returns the field name.
     *
     * @return the field name.
     */
    public String getField() {
        return field;
    }

    /**
     * Returns the field value.
     *
     * @return the field value.
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the Damerau-Levenshtein max distance.
     *
     * @return The Damerau-Levenshtein max distance.
     */
    public Integer getMaxEdits() {
        return maxEdits;
    }

    /**
     * Returns the length of common (non-fuzzy) prefix.
     *
     * @return The length of common (non-fuzzy) prefix.
     */
    public Integer getPrefixLength() {
        return prefixLength;
    }

    /**
     * Returns the maximum number of terms to match.
     *
     * @return The maximum number of terms to match.
     */
    public Integer getMaxExpansions() {
        return maxExpansions;
    }

    /**
     * Returns if transpositions should be treated as a primitive edit operation.
     *
     * @return If transpositions should be treated as a primitive edit operation.
     */
    public Boolean getTranspositions() {
        return transpositions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query query(Options schema) {

        if (field == null || field.trim().isEmpty()) {
            throw new IllegalArgumentException("Field name required");
        }
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Field value required");
        }
        if (maxEdits < 0 || maxEdits > 2) {
            throw new IllegalArgumentException("max_edits must be between 0 and 2");
        }
        if (prefixLength < 0) {
            throw new IllegalArgumentException("prefix_length must be positive.");
        }
        if (maxExpansions < 0) {
            throw new IllegalArgumentException("max_expansions must be positive.");
        }

        Properties properties = schema.getProperties(field);
        String message;
        Properties.Type fieldType = properties != null ? properties.getType() : Properties.Type.text;
        if (fieldType == Properties.Type.string || fieldType == Properties.Type.text) {
            String analyzedValue = analyze(field, value, schema.analyzer);
            Term term = new Term(field, analyzedValue);
            Query query = new FuzzyQuery(term, maxEdits, prefixLength, maxExpansions, transpositions);
            query.setBoost(boost);
            return query;
        }
        message = String.format("Fuzzy queries cannot be supported for field type %s", fieldType);
        throw new UnsupportedOperationException(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append(" [boost=");
        builder.append(boost);
        builder.append(", field=");
        builder.append(field);
        builder.append(", value=");
        builder.append(value);
        builder.append(", maxEdits=");
        builder.append(maxEdits);
        builder.append(", prefixLength=");
        builder.append(prefixLength);
        builder.append(", maxExpansions=");
        builder.append(maxExpansions);
        builder.append(", transpositions=");
        builder.append(transpositions);
        builder.append("]");
        return builder.toString();
    }

}