package com.lmax.disruptor.spring.boot.context.event;

import com.lmax.disruptor.event.DisruptorEvent;
import com.lmax.disruptor.event.DisruptorEventPublisher;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link EventPublicationInterceptor}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
class EventPublicationInterceptorTest {

    public static class TestEvent extends DisruptorEvent {
        public TestEvent(Object source) {
            super(source);
        }
    }

    public static class NotAnEvent {
    }

    @Test
    void setApplicationEventClass_withValidClass_shouldAccept() {
        EventPublicationInterceptor interceptor = new EventPublicationInterceptor();
        interceptor.setApplicationEventClass(TestEvent.class);
        // Should not throw
    }

    @Test
    void setApplicationEventClass_withNonEventClass_shouldThrow() {
        EventPublicationInterceptor interceptor = new EventPublicationInterceptor();
        assertThatThrownBy(() -> interceptor.setApplicationEventClass(NotAnEvent.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("applicationEventClass needs to extend DisruptorEvent");
    }

    @Test
    void setApplicationEventClass_withDisruptorEventClass_shouldThrow() {
        EventPublicationInterceptor interceptor = new EventPublicationInterceptor();
        assertThatThrownBy(() -> interceptor.setApplicationEventClass(DisruptorEvent.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("applicationEventClass needs to extend DisruptorEvent");
    }

    @Test
    void afterPropertiesSet_withoutEventClass_shouldThrow() {
        EventPublicationInterceptor interceptor = new EventPublicationInterceptor();
        assertThatThrownBy(() -> interceptor.afterPropertiesSet())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("applicationEventClass is required");
    }

    @Test
    void afterPropertiesSet_withEventClass_shouldNotThrow() throws Exception {
        EventPublicationInterceptor interceptor = new EventPublicationInterceptor();
        interceptor.setApplicationEventClass(TestEvent.class);
        interceptor.afterPropertiesSet();
    }

    @Test
    void invoke_shouldProceedAndPublishEvent() throws Throwable {
        EventPublicationInterceptor interceptor = new EventPublicationInterceptor();
        interceptor.setApplicationEventClass(TestEvent.class);

        DisruptorEventPublisher mockPublisher = mock(DisruptorEventPublisher.class);
        interceptor.setDisruptorEventPublisher(mockPublisher);
        interceptor.afterPropertiesSet();

        MethodInvocation mockInvocation = mock(MethodInvocation.class);
        Object target = new Object();
        when(mockInvocation.proceed()).thenReturn("result");
        when(mockInvocation.getThis()).thenReturn(target);

        Object result = interceptor.invoke(mockInvocation);

        assertThat(result).isEqualTo("result");
        verify(mockPublisher).publishEvent(any(DisruptorEvent.class));
    }

    @Test
    void setDisruptorEventPublisher_shouldStorePublisher() {
        EventPublicationInterceptor interceptor = new EventPublicationInterceptor();
        DisruptorEventPublisher mockPublisher = mock(DisruptorEventPublisher.class);
        interceptor.setDisruptorEventPublisher(mockPublisher);
        // Should not throw
    }
}
