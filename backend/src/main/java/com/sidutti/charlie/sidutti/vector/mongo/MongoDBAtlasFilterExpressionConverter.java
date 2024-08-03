/*
 * Copyright 2023 - 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sidutti.charlie.sidutti.vector.mongo;

import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.converter.AbstractFilterExpressionConverter;

import static org.springframework.ai.vectorstore.filter.Filter.ExpressionType.AND;
import static org.springframework.ai.vectorstore.filter.Filter.ExpressionType.OR;


public class MongoDBAtlasFilterExpressionConverter extends AbstractFilterExpressionConverter {

        @Override
        protected void doExpression(Filter.Expression expression, StringBuilder context) {
                // Handling AND/OR
                if (AND.equals(expression.type()) || OR.equals(expression.type())) {
                        doCompoundExpressionType(expression, context);
                } else {
                        doSingleExpressionType(expression, context);
                }
        }

        private void doCompoundExpressionType(Filter.Expression expression, StringBuilder context) {
                context.append("{");
                context.append(getOperationSymbol(expression));
                context.append(":[");
                this.convertOperand(expression.left(), context);
                context.append(",");
                this.convertOperand(expression.right(), context);
                context.append("]}");
        }

        private void doSingleExpressionType(Filter.Expression expression, StringBuilder context) {
                context.append("{");
                this.convertOperand(expression.left(), context);
                context.append(":{");
                context.append(getOperationSymbol(expression));
                context.append(":");
                this.convertOperand(expression.right(), context);
                context.append("}}");
        }

        private String getOperationSymbol(Filter.Expression exp) {
                return switch (exp.type()) {
                        case AND -> "$and";
                        case OR -> "$or";
                        case EQ -> "$eq";
                        case NE -> "$ne";
                        case LT -> "$lt";
                        case LTE -> "$lte";
                        case GT -> "$gt";
                        case GTE -> "$gte";
                        case IN -> "$in";
                        case NIN -> "$nin";
                        default -> throw new RuntimeException("Not supported expression type:" + exp.type());
                };
        }

        @Override
        protected void doKey(Filter.Key filterKey, StringBuilder context) {
                var identifier = (hasOuterQuotes(filterKey.key())) ? removeOuterQuotes(filterKey.key()) : filterKey.key();
                context.append("\"metadata.").append(identifier).append("\"");
        }

}
