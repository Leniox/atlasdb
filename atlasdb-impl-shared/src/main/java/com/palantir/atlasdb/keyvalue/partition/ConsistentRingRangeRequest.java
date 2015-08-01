package com.palantir.atlasdb.keyvalue.partition;

import com.palantir.atlasdb.keyvalue.api.RangeRequest;
import com.palantir.common.annotation.Immutable;

@Immutable public class ConsistentRingRangeRequest {
    private final RangeRequest rangeRequest;
    public RangeRequest get() {
        return rangeRequest;
    }
    private ConsistentRingRangeRequest(RangeRequest rangeRequest) {
        this.rangeRequest = rangeRequest;
    }
    public static ConsistentRingRangeRequest of(RangeRequest rangeRequest) {
        return new ConsistentRingRangeRequest(rangeRequest);
    }
    public String toString() {
        return "CRRR=[" + rangeRequest + "]";
    }
}
