package com.a.eye.skywalking.plugin.httpClient.v4;

import com.a.eye.skywalking.api.boot.ServiceManager;
import com.a.eye.skywalking.api.context.TracerContext;
import com.a.eye.skywalking.api.plugin.interceptor.EnhancedClassInstanceContext;
import com.a.eye.skywalking.api.plugin.interceptor.enhance.InstanceMethodInvokeContext;
import com.a.eye.skywalking.sniffer.mock.context.MockTracerContextListener;
import com.a.eye.skywalking.sniffer.mock.context.SegmentAssert;
import com.a.eye.skywalking.trace.LogData;
import com.a.eye.skywalking.trace.Span;
import com.a.eye.skywalking.trace.TraceSegment;
import com.a.eye.skywalking.trace.tag.Tags;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.StatusLine;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(HttpHost.class)
public class HttpClientExecuteInterceptorTest {

    private HttpClientExecuteInterceptor httpClientExecuteInterceptor;
    private MockTracerContextListener mockTracerContextListener;
    @Mock
    private EnhancedClassInstanceContext classInstanceContext;
    @Mock
    private InstanceMethodInvokeContext instanceMethodInvokeContext;
    @Mock
    private HttpHost httpHost;
    @Mock
    private HttpRequest request;
    @Mock
    private HttpResponse httpResponse;
    @Mock
    private StatusLine statusLine;

    @Before
    public void setUp() throws Exception {
        mockTracerContextListener = new MockTracerContextListener();

        ServiceManager.INSTANCE.boot();
        httpClientExecuteInterceptor = new HttpClientExecuteInterceptor();

        PowerMockito.mock(HttpHost.class);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(instanceMethodInvokeContext.allArguments()).thenReturn(new Object[]{httpHost, request});
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(httpHost.getHostName()).thenReturn("127.0.0.1");
        when(httpHost.getSchemeName()).thenReturn("http");
        when(request.getRequestLine()).thenReturn(new RequestLine() {
            @Override
            public String getMethod() {
                return "GET";
            }

            @Override
            public ProtocolVersion getProtocolVersion() {
                return new ProtocolVersion("http", 1, 1);
            }

            @Override
            public String getUri() {
                return "http://127.0.0.1:8080/test-web/test";
            }
        });
        when(httpHost.getPort()).thenReturn(8080);

        TracerContext.ListenerManager.add(mockTracerContextListener);
    }

    @Test
    public void testHttpClient() {
        httpClientExecuteInterceptor.beforeMethod(classInstanceContext, instanceMethodInvokeContext, null);
        httpClientExecuteInterceptor.afterMethod(classInstanceContext, instanceMethodInvokeContext, httpResponse);

        mockTracerContextListener.assertSize(1);
        mockTracerContextListener.assertTraceSegment(0, new SegmentAssert() {
            @Override
            public void call(TraceSegment traceSegment) {
                assertThat(traceSegment.getSpans().size(), is(1));
                assertHttpSpan(traceSegment.getSpans().get(0));
                verify(request, times(1)).setHeader(anyString(), anyString());
            }
        });
    }


    @Test
    public void testStatusCodeNotEquals200() {
        when(statusLine.getStatusCode()).thenReturn(500);
        httpClientExecuteInterceptor.beforeMethod(classInstanceContext, instanceMethodInvokeContext, null);
        httpClientExecuteInterceptor.afterMethod(classInstanceContext, instanceMethodInvokeContext, httpResponse);

        mockTracerContextListener.assertSize(1);
        mockTracerContextListener.assertTraceSegment(0, new SegmentAssert() {
            @Override
            public void call(TraceSegment traceSegment) {
                assertThat(traceSegment.getSpans().size(), is(1));
                assertHttpSpan(traceSegment.getSpans().get(0));
                assertThat(Tags.ERROR.get(traceSegment.getSpans().get(0)), is(true));
                verify(request, times(1)).setHeader(anyString(), anyString());
            }
        });
    }

    @Test
    public void testHttpClientWithException() {
        httpClientExecuteInterceptor.beforeMethod(classInstanceContext, instanceMethodInvokeContext, null);
        httpClientExecuteInterceptor.handleMethodException(new RuntimeException(), classInstanceContext, instanceMethodInvokeContext);
        httpClientExecuteInterceptor.afterMethod(classInstanceContext, instanceMethodInvokeContext, httpResponse);

        mockTracerContextListener.assertSize(1);
        mockTracerContextListener.assertTraceSegment(0, new SegmentAssert() {
            @Override
            public void call(TraceSegment traceSegment) {
                assertThat(traceSegment.getSpans().size(), is(1));
                Span span = traceSegment.getSpans().get(0);
                assertHttpSpan(span);
                assertThat(Tags.ERROR.get(span), is(true));
                assertHttpSpanErrorLog(span.getLogs());
                verify(request, times(1)).setHeader(anyString(), anyString());

            }

            private void assertHttpSpanErrorLog(List<LogData> logs) {
                assertThat(logs.size(), is(1));
                LogData logData = logs.get(0);
                assertThat(logData.getFields().size(), is(4));
            }
        });

    }


    private void assertHttpSpan(Span span) {
        assertThat(span.getOperationName(), is("/test-web/test"));
        assertThat(Tags.COMPONENT.get(span), is("HttpClient"));
        assertThat(Tags.PEER_HOST.get(span), is("127.0.0.1"));
        assertThat(Tags.PEER_PORT.get(span), is(8080));
        assertThat(Tags.URL.get(span), is("http://127.0.0.1:8080/test-web/test"));
        assertThat(Tags.SPAN_KIND.get(span), is(Tags.SPAN_KIND_CLIENT));
    }

    @After
    public void tearDown() throws Exception {
        TracerContext.ListenerManager.remove(mockTracerContextListener);
    }

}
