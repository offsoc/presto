/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.spi.plan;

import com.facebook.presto.spi.SourceLocation;
import com.facebook.presto.spi.relation.VariableReferenceExpression;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.concurrent.Immutable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.facebook.presto.common.Utils.checkArgument;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

@Immutable
public final class SemiJoinNode
        extends AbstractJoinNode
{
    private final PlanNode source;
    private final PlanNode filteringSource;
    private final VariableReferenceExpression sourceJoinVariable;
    private final VariableReferenceExpression filteringSourceJoinVariable;
    private final VariableReferenceExpression semiJoinOutput;
    private final Optional<VariableReferenceExpression> sourceHashVariable;
    private final Optional<VariableReferenceExpression> filteringSourceHashVariable;
    private final Optional<DistributionType> distributionType;
    private final Map<String, VariableReferenceExpression> dynamicFilters;

    @JsonCreator
    public SemiJoinNode(
            Optional<SourceLocation> sourceLocation,
            @JsonProperty("id") PlanNodeId id,
            @JsonProperty("source") PlanNode source,
            @JsonProperty("filteringSource") PlanNode filteringSource,
            @JsonProperty("sourceJoinVariable") VariableReferenceExpression sourceJoinVariable,
            @JsonProperty("filteringSourceJoinVariable") VariableReferenceExpression filteringSourceJoinVariable,
            @JsonProperty("semiJoinOutput") VariableReferenceExpression semiJoinOutput,
            @JsonProperty("sourceHashVariable") Optional<VariableReferenceExpression> sourceHashVariable,
            @JsonProperty("filteringSourceHashVariable") Optional<VariableReferenceExpression> filteringSourceHashVariable,
            @JsonProperty("distributionType") Optional<DistributionType> distributionType,
            @JsonProperty("dynamicFilters") Map<String, VariableReferenceExpression> dynamicFilters)
    {
        this(sourceLocation, id, Optional.empty(), source, filteringSource, sourceJoinVariable, filteringSourceJoinVariable, semiJoinOutput, sourceHashVariable, filteringSourceHashVariable, distributionType, dynamicFilters);
    }

    public SemiJoinNode(
            Optional<SourceLocation> sourceLocation,
            PlanNodeId id,
            Optional<PlanNode> statsEquivalentPlanNode,
            PlanNode source,
            PlanNode filteringSource,
            VariableReferenceExpression sourceJoinVariable,
            VariableReferenceExpression filteringSourceJoinVariable,
            VariableReferenceExpression semiJoinOutput,
            Optional<VariableReferenceExpression> sourceHashVariable,
            Optional<VariableReferenceExpression> filteringSourceHashVariable,
            Optional<DistributionType> distributionType,
            Map<String, VariableReferenceExpression> dynamicFilters)
    {
        super(sourceLocation, id, statsEquivalentPlanNode);
        this.source = requireNonNull(source, "source is null");
        this.filteringSource = requireNonNull(filteringSource, "filteringSource is null");
        this.sourceJoinVariable = requireNonNull(sourceJoinVariable, "sourceJoinVariable is null");
        this.filteringSourceJoinVariable = requireNonNull(filteringSourceJoinVariable, "filteringSourceJoinVariable is null");
        this.semiJoinOutput = requireNonNull(semiJoinOutput, "semiJoinOutput is null");
        this.sourceHashVariable = requireNonNull(sourceHashVariable, "sourceHashVariable is null");
        this.filteringSourceHashVariable = requireNonNull(filteringSourceHashVariable, "filteringSourceHashVariable is null");
        this.distributionType = requireNonNull(distributionType, "distributionType is null");
        this.dynamicFilters = requireNonNull(dynamicFilters, "dynamicFilters is null");

        checkArgument(source.getOutputVariables().contains(sourceJoinVariable), "Source does not contain join symbol");
        checkArgument(filteringSource.getOutputVariables().contains(filteringSourceJoinVariable), "Filtering source does not contain filtering join symbol");
    }

    public enum DistributionType
    {
        PARTITIONED,
        REPLICATED
    }

    @JsonProperty
    public PlanNode getSource()
    {
        return source;
    }

    @Override
    public PlanNode getProbe()
    {
        return source;
    }

    @JsonProperty
    public PlanNode getFilteringSource()
    {
        return filteringSource;
    }

    @Override
    public PlanNode getBuild()
    {
        return filteringSource;
    }

    @JsonProperty
    public VariableReferenceExpression getSourceJoinVariable()
    {
        return sourceJoinVariable;
    }

    @JsonProperty
    public VariableReferenceExpression getFilteringSourceJoinVariable()
    {
        return filteringSourceJoinVariable;
    }

    @JsonProperty
    public VariableReferenceExpression getSemiJoinOutput()
    {
        return semiJoinOutput;
    }

    @JsonProperty
    public Optional<VariableReferenceExpression> getSourceHashVariable()
    {
        return sourceHashVariable;
    }

    @JsonProperty
    public Optional<VariableReferenceExpression> getFilteringSourceHashVariable()
    {
        return filteringSourceHashVariable;
    }

    @JsonProperty
    public Optional<DistributionType> getDistributionType()
    {
        return distributionType;
    }

    @Override
    @JsonProperty
    public Map<String, VariableReferenceExpression> getDynamicFilters()
    {
        return dynamicFilters;
    }

    @Override
    public List<PlanNode> getSources()
    {
        List<PlanNode> sources = new ArrayList<>();
        sources.add(source);
        sources.add(filteringSource);
        return unmodifiableList(sources);
    }

    @Override
    public List<VariableReferenceExpression> getOutputVariables()
    {
        List<VariableReferenceExpression> outputVariables = new ArrayList<>();
        outputVariables.addAll(source.getOutputVariables());
        outputVariables.add(semiJoinOutput);
        return unmodifiableList(outputVariables);
    }

    @Override
    public <R, C> R accept(PlanVisitor<R, C> visitor, C context)
    {
        return visitor.visitSemiJoin(this, context);
    }

    @Override
    public PlanNode replaceChildren(List<PlanNode> newChildren)
    {
        checkArgument(newChildren.size() == 2, "expected newChildren to contain 2 nodes");
        return new SemiJoinNode(
                getSourceLocation(),
                getId(),
                getStatsEquivalentPlanNode(),
                newChildren.get(0),
                newChildren.get(1),
                sourceJoinVariable,
                filteringSourceJoinVariable,
                semiJoinOutput,
                sourceHashVariable,
                filteringSourceHashVariable,
                distributionType,
                dynamicFilters);
    }

    @Override
    public PlanNode assignStatsEquivalentPlanNode(Optional<PlanNode> statsEquivalentPlanNode)
    {
        return new SemiJoinNode(
                getSourceLocation(),
                getId(),
                statsEquivalentPlanNode,
                source,
                filteringSource,
                sourceJoinVariable,
                filteringSourceJoinVariable,
                semiJoinOutput,
                sourceHashVariable,
                filteringSourceHashVariable,
                distributionType,
                dynamicFilters);
    }

    @Override
    public LogicalProperties computeLogicalProperties(LogicalPropertiesProvider logicalPropertiesProvider)
    {
        requireNonNull(logicalPropertiesProvider, "logicalPropertiesProvider cannot be null.");
        return logicalPropertiesProvider.getSemiJoinProperties(this);
    }

    public SemiJoinNode withDistributionType(DistributionType distributionType)
    {
        return new SemiJoinNode(
                getSourceLocation(),
                getId(),
                source,
                filteringSource,
                sourceJoinVariable,
                filteringSourceJoinVariable,
                semiJoinOutput,
                sourceHashVariable,
                filteringSourceHashVariable,
                Optional.of(distributionType),
                dynamicFilters);
    }
}
