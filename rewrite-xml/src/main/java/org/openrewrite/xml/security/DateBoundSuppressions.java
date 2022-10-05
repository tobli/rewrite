/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.xml.security;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class DateBoundSuppressions extends Recipe {

    @Override
    public String getDisplayName() {
        return "Add date bounds to OWASP suppressions";
    }

    @Override
    public String getDescription() {
        return "Adds an expiration date to all OWASP suppressions in order to ensure that they are periodically reviewed. " +
                "For use with the OWASP `dependency-check` tool. " +
                "More details: https://jeremylong.github.io/DependencyCheck/general/suppression.html";
    }

    @Option(displayName = "Until date",
            description = "Optional. The date to add to the suppression. Default will be 30 days from today.",
            example = "2023-01-01")
    @Nullable
    String untilDate;

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new DateBoundSuppressionsVisitor();
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new IsOwaspSuppressionsFile();
    }

    private class DateBoundSuppressionsVisitor extends XmlIsoVisitor<ExecutionContext> {
        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag t = super.visitTag(tag, ctx);
            if (new XPathMatcher("/suppressions/suppress").matches(getCursor())) {
                boolean hasUntil = false;
                List<Xml.Attribute> attributes = t.getAttributes();
                for (Xml.Attribute attribute : attributes) {
                    if (attribute.getKeyAsString().equals("until")) {
                        hasUntil = true;
                    }
                }
                if (!hasUntil) {
                    String date = untilDate != null ? untilDate : LocalDate.now().plus(30, ChronoUnit.DAYS).toString();
                    return t.withAttributes(ListUtils.concat(attributes, autoFormat(new Xml.Attribute(Tree.randomId(), "", Markers.EMPTY,
                            new Xml.Ident(Tree.randomId(), "", Markers.EMPTY, "until"),
                            "",
                            autoFormat(new Xml.Attribute.Value(Tree.randomId(), "", Markers.EMPTY,
                                    Xml.Attribute.Value.Quote.Double,
                                    date + "Z"), ctx)), ctx)));
                }
            }
            return t;
        }
    }
}