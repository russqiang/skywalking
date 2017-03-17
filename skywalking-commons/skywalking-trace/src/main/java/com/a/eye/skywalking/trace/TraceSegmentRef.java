package com.a.eye.skywalking.trace;

import com.a.eye.skywalking.messages.ISerializable;
import com.a.eye.skywalking.trace.proto.SegmentRefMessage;
import com.a.eye.skywalking.trace.tag.Tags;

/**
 * {@link TraceSegmentRef} is like a pointer, which ref to another {@link TraceSegment},
 * use {@link #spanId} point to the exact span of the ref {@link TraceSegment}.
 *
 * Created by wusheng on 2017/2/17.
 */
public class TraceSegmentRef implements ISerializable<SegmentRefMessage> {
    /**
     * {@link TraceSegment#traceSegmentId}
     */
    private String traceSegmentId;

    /**
     * {@link Span#spanId}
     */
    private int spanId = -1;

    /**
     * {@link TraceSegment#applicationCode}
     */
    private String applicationCode;

    /**
     * {@link Tags#PEER_HOST}
     */
    private String peerHost;

    /**
     * Create a {@link TraceSegmentRef} instance, without any data.
     */
    public TraceSegmentRef() {
    }

    public String getTraceSegmentId() {
        return traceSegmentId;
    }

    public void setTraceSegmentId(String traceSegmentId) {
        this.traceSegmentId = traceSegmentId;
    }

    public int getSpanId() {
        return spanId;
    }

    public void setSpanId(int spanId) {
        this.spanId = spanId;
    }

    public String getApplicationCode() {
        return applicationCode;
    }

    public void setApplicationCode(String applicationCode) {
        this.applicationCode = applicationCode;
    }

    public String getPeerHost() {
        return peerHost;
    }

    public void setPeerHost(String peerHost) {
        this.peerHost = peerHost;
    }

    @Override public String toString() {
        return "TraceSegmentRef{" +
            "traceSegmentId='" + traceSegmentId + '\'' +
            ", spanId=" + spanId +
            ", applicationCode='" + applicationCode + '\'' +
            ", peerHost='" + peerHost + '\'' +
            '}';
    }

    @Override
    public SegmentRefMessage serialize() {
        SegmentRefMessage.Builder builder = SegmentRefMessage.newBuilder();
        builder.setTraceSegmentId(traceSegmentId);
        builder.setSpanId(spanId);
        builder.setApplicationCode(applicationCode);
        if(peerHost != null) {
            builder.setPeerHost(peerHost);
        }
        return builder.build();
    }

    @Override
    public void deserialize(SegmentRefMessage message) {
        traceSegmentId = message.getTraceSegmentId();
        spanId = message.getSpanId();
        applicationCode = message.getApplicationCode();
        peerHost = message.getPeerHost();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        TraceSegmentRef ref = (TraceSegmentRef)o;

        if (spanId != ref.spanId)
            return false;
        if (traceSegmentId != null ? !traceSegmentId.equals(ref.traceSegmentId) : ref.traceSegmentId != null)
            return false;
        if (applicationCode != null ? !applicationCode.equals(ref.applicationCode) : ref.applicationCode != null)
            return false;
        return peerHost != null ? peerHost.equals(ref.peerHost) : ref.peerHost == null;
    }

    @Override
    public int hashCode() {
        int result = traceSegmentId != null ? traceSegmentId.hashCode() : 0;
        result = 31 * result + spanId;
        result = 31 * result + (applicationCode != null ? applicationCode.hashCode() : 0);
        result = 31 * result + (peerHost != null ? peerHost.hashCode() : 0);
        return result;
    }
}
