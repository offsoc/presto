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
import com.facebook.presto.spi.relation.RowExpression;
import com.facebook.presto.spi.relation.VariableReferenceExpression;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.concurrent.Immutable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.facebook.presto.common.Utils.checkArgument;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

@Immutable
public class MergeJoinNode
        extends PlanNode
{
    private final JoinType type;
    private final PlanNode left;
    private final PlanNode right;
    private final List<EquiJoinClause> criteria;
    private final Optional<RowExpression> filter;
    private final List<VariableReferenceExpression> outputVariables;
    private final Optional<VariableReferenceExpression> leftHashVariable;
    private final Optional<VariableReferenceExpression> rightHashVariable;

    @JsonCreator
    public MergeJoinNode(
            Optional<SourceLocation> sourceLocation,
            @JsonProperty("id") PlanNodeId id,
            @JsonProperty("type") JoinType type,
            @JsonProperty("left") PlanNode left,
            @JsonProperty("right") PlanNode right,
            @JsonProperty("criteria") List<EquiJoinClause> criteria,
            @JsonProperty("outputVariables") List<VariableReferenceExpression> outputVariables,
            @JsonProperty("filter") Optional<RowExpression> filter,
            @JsonProperty("leftHashVariable") Optional<VariableReferenceExpression> leftHashVariable,
            @JsonProperty("rightHashVariable") Optional<VariableReferenceExpression> rightHashVariable)
    {
        this(sourceLocation, id, Optional.empty(), type, left, right, criteria, outputVariables, filter, leftHashVariable, rightHashVariable);
    }

    public MergeJoinNode(
            Optional<SourceLocation> sourceLocation,
            PlanNodeId id,
            Optional<PlanNode> statsEquivalentPlanNode,
            JoinType type,
            PlanNode left,
            PlanNode right,
            List<EquiJoinClause> criteria,
            List<VariableReferenceExpression> outputVariables,
            Optional<RowExpression> filter,
            Optional<VariableReferenceExpression> leftHashVariable,
            Optional<VariableReferenceExpression> rightHashVariable)
    {
        super(sourceLocation, id, statsEquivalentPlanNode);
        this.type = requireNonNull(type, "type is null");
        this.left = requireNonNull(left, "left is null");
        this.right = requireNonNull(right, "right is null");
        this.criteria = unmodifiableList(new ArrayList<>(requireNonNull(criteria, "criteria is null")));
        this.outputVariables = unmodifiableList(new ArrayList<>(requireNonNull(outputVariables, "outputVariables is null")));
        this.filter = requireNonNull(filter, "filter is null");
        this.leftHashVariable = requireNonNull(leftHashVariable, "leftHashVariable is null");
        this.rightHashVariable = requireNonNull(rightHashVariable, "rightHashVariable is null");
    }

    @JsonProperty
    public JoinType getType()
    {
        return type;
    }

    @JsonProperty
    public PlanNode getLeft()
    {
        return left;
    }

    @JsonProperty
    public PlanNode getRight()
    {
        return right;
    }

    @JsonProperty
    public List<EquiJoinClause> getCriteria()
    {
        return criteria;
    }

    @Override
    @JsonProperty
    public List<VariableReferenceExpression> getOutputVariables()
    {
        return outputVariables;
    }

    @JsonProperty
    public Optional<RowExpression> getFilter()
    {
        return filter;
    }

    @JsonProperty
    public Optional<VariableReferenceExpression> getLeftHashVariable()
    {
        return leftHashVariable;
    }

    @JsonProperty
    public Optional<VariableReferenceExpression> getRightHashVariable()
    {
        return rightHashVariable;
    }

    @Override
    public List<PlanNode> getSources()
    {
        List<PlanNode> sources = new ArrayList<>();
        sources.add(left);
        sources.add(right);
        return unmodifiableList(sources);
    }

    @Override
    public PlanNode replaceChildren(List<PlanNode> newChildren)
    {
        checkArgument(newChildren.size() == 2, "expected newChildren to contain 2 nodes");
        return new MergeJoinNode(getSourceLocation(), getId(), getStatsEquivalentPlanNode(), type, newChildren.get(0), newChildren.get(1), criteria, outputVariables, filter, leftHashVariable, rightHashVariable);
    }

    @Override
    public PlanNode assignStatsEquivalentPlanNode(Optional<PlanNode> statsEquivalentPlanNode)
    {
        return new MergeJoinNode(getSourceLocation(), getId(), statsEquivalentPlanNode, type, left, right, criteria, outputVariables, filter, leftHashVariable, rightHashVariable);
    }

    @Override
    public <R, C> R accept(PlanVisitor<R, C> visitor, C context)
    {
        return visitor.visitMergeJoin(this, context);
    }
}
