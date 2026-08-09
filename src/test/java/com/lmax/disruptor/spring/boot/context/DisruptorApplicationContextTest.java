package com.lmax.disruptor.spring.boot.context;

import com.lmax.disruptor.event.DisruptorEvent;
import com.lmax.disruptor.spring.boot.event.DisruptorApplicationEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link DisruptorApplicationContext}.
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 */
class DisruptorApplicationContextTest {

    @Test
    void setApplicationContext_shouldStoreContext() throws Exception {
        DisruptorApplicationContext ctx = new DisruptorApplicationContext();
        ApplicationContext mockCtx = mock(ApplicationContext.class);
        ctx.setApplicationContext(mockCtx);
        assertThat(ctx.getApplicationContext()).isSameAs(mockCtx);
    }

    @Test
    void publishEvent_shouldDelegateToApplicationContext() {
        DisruptorApplicationContext ctx = new DisruptorApplicationContext();
        ApplicationContext mockCtx = mock(ApplicationContext.class);
        ctx.setApplicationContext(mockCtx);

        DisruptorEvent event = new DisruptorEvent();
        ctx.publishEvent(event);

        verify(mockCtx).publishEvent(any(DisruptorApplicationEvent.class));
    }

    @Test
    void getApplicationContext_shouldReturnNullBeforeSet() {
        DisruptorApplicationContext ctx = new DisruptorApplicationContext();
        assertThat(ctx.getApplicationContext()).isNull();
    }
}
