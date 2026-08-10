package com.lmax.disruptor.spring.boot.context;

import com.lmax.disruptor.event.DisruptorEventPublisher;
import com.lmax.disruptor.event.DisruptorEventPublisherAware;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.Aware;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link DisruptorEventAwareProcessor}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
class DisruptorEventAwareProcessorTest {

    /** Test bean that implements both Spring Aware and DisruptorEventPublisherAware. */
    static class TestAwareBean implements Aware, DisruptorEventPublisherAware {
        private DisruptorEventPublisher publisher;
        @Override
        public void setDisruptorEventPublisher(DisruptorEventPublisher publisher) {
            this.publisher = publisher;
        }
        public DisruptorEventPublisher getPublisher() { return publisher; }
    }

    @Test
    void afterPropertiesSet_shouldCreateDisruptorApplicationContext() throws Exception {
        DisruptorEventAwareProcessor processor = new DisruptorEventAwareProcessor();
        ApplicationContext mockCtx = mock(ApplicationContext.class);
        processor.setApplicationContext(mockCtx);
        processor.afterPropertiesSet();
        assertThat(processor).isNotNull();
    }

    @Test
    void postProcessBeforeInitialization_withAwareBean_shouldInvokeAware() throws Exception {
        DisruptorEventAwareProcessor processor = new DisruptorEventAwareProcessor();
        ApplicationContext mockCtx = mock(ApplicationContext.class);
        processor.setApplicationContext(mockCtx);
        processor.afterPropertiesSet();

        TestAwareBean awareBean = new TestAwareBean();
        Object result = processor.postProcessBeforeInitialization(awareBean, "testBean");
        assertThat(result).isSameAs(awareBean);
        assertThat(awareBean.getPublisher()).isNotNull();
    }

    @Test
    void postProcessBeforeInitialization_withNonAwareBean_shouldReturnBean() throws Exception {
        DisruptorEventAwareProcessor processor = new DisruptorEventAwareProcessor();
        ApplicationContext mockCtx = mock(ApplicationContext.class);
        processor.setApplicationContext(mockCtx);
        processor.afterPropertiesSet();

        String nonAwareBean = "notAware";
        Object result = processor.postProcessBeforeInitialization(nonAwareBean, "testBean");
        assertThat(result).isSameAs(nonAwareBean);
    }

    @Test
    void postProcessAfterInitialization_shouldReturnBean() {
        DisruptorEventAwareProcessor processor = new DisruptorEventAwareProcessor();
        String bean = "test";
        Object result = processor.postProcessAfterInitialization(bean, "testBean");
        assertThat(result).isSameAs(bean);
    }

    @Test
    void setApplicationContext_shouldStoreContext() throws Exception {
        DisruptorEventAwareProcessor processor = new DisruptorEventAwareProcessor();
        ApplicationContext mockCtx = mock(ApplicationContext.class);
        processor.setApplicationContext(mockCtx);
        processor.afterPropertiesSet();
        Object result = processor.postProcessAfterInitialization("bean", "name");
        assertThat(result).isEqualTo("bean");
    }
}
