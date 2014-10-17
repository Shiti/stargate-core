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
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/**
 * A {@link Condition} implementation that matches documents containing a particular sequence of terms.
 */
public class PhraseCondition extends Condition {

    public static final int DEFAULT_SLOP = 0;

    /**
     * The field name
     */
    private final String field;

    /**
     * The field values
     */
    private List<String> values;

    /**
     * The slop
     */
    private final Integer slop;

    /**
     * Constructor using the field name and the value to be matched.
     *
     * @param boost  The boost for this query clause. Documents matching this clause will (in addition to the normal
     *               weightings) have their score multiplied by {@code boost}. If {@code null}, then DEFAULT_BOOST
     *               is used as default.
     * @param field  The field name.
     * @param values The field values.
     * @param slop   The slop.
     */
    @JsonCreator
    public PhraseCondition(@JsonProperty("boost") Float boost,
                           @JsonProperty("field") String field,
                           @JsonProperty("values") List<String> values,
                           @JsonProperty("slop") Integer slop) {
        super(boost);

        this.field = field != null ? field.toLowerCase() : null;
        this.values = values;
        this.slop = slop == null ? DEFAULT_SLOP : slop;
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
     * Returns the field values.
     *
     * @return the field values.
     */
    public List<String> getValues() {
        return values;
    }

    /**
     * Returns the slop.
     *
     * @return the slop.
     */
    public int getSlop() {
        return slop;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query query(Options schema) {

        if (field == null || field.trim().isEmpty()) {
            throw new IllegalArgumentException("Field name required");
        }
        if (values == null) {
            throw new IllegalArgumentException("Field values required");
        }
        if (slop == null) {
            throw new IllegalArgumentException("Slop required");
        }
        if (slop < 0) {
            throw new IllegalArgumentException("Slop must be positive");
        }
        Properties properties = schema.getProperties(field);
        Properties.Type fieldType = properties != null ? properties.getType() : Properties.Type.text;
        if (fieldType.isCharSeq()) {
            Analyzer analyzer = schema.analyzer;
            PhraseQuery query = new PhraseQuery();
            query.setSlop(slop);
            query.setBoost(boost);
            int count = 0;
            for (String value : values) {
                if (value != null) {
                    String analyzedValue = analyze(field, value, analyzer);
                    if (analyzedValue != null) {
                        Term term = new Term(field, analyzedValue);
                        query.add(term, count);
                    }
                }
                count++;
            }
            return query;
        }
        String message = String.format("Phrase queries cannot be supported until mapping is defined");
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
        builder.append(", values=");
        builder.append(values);
        builder.append(", slop=");
        builder.append(slop);
        builder.append("]");
        return builder.toString();
    }

}