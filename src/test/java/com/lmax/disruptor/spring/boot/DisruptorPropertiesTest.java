package com.lmax.disruptor.spring.boot;

import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.thread.DisruptorThreadFactory;
import com.lmax.disruptor.thread.DisruptorWaitStrategy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DisruptorProperties}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
class DisruptorPropertiesTest {

    @Test
    void prefix_shouldBeCorrect() {
        assertThat(DisruptorProperties.PREFIX).isEqualTo("spring.disruptor");
    }

    @Test
    void defaultValues_shouldBeCorrect() {
        DisruptorProperties props = new DisruptorProperties();
        assertThat(props.isEnabled()).isFalse();
        assertThat(props.getThreadFactory()).isEqualTo(DisruptorThreadFactory.DEFAULT_THREAD_FACTORY);
        assertThat(props.getWaitStrategy()).isEqualTo(DisruptorWaitStrategy.YIELDING_WAIT);
        assertThat(props.getProducerType()).isEqualTo(ProducerType.SINGLE);
        assertThat(props.isRingBuffer()).isFalse();
        assertThat(props.getRingBufferSize()).isEqualTo(1024);
        assertThat(props.getMaxBatchSize()).isEqualTo(Integer.MAX_VALUE);
        assertThat(props.getRingThreadNumbers()).isEqualTo(4);
        assertThat(props.isMultiProducer()).isFalse();
        assertThat(props.getHandlerDefinitions()).isEmpty();
    }

    @Test
    void settersAndGetters_shouldWork() {
        DisruptorProperties props = new DisruptorProperties();
        props.setEnabled(true);
        props.setRingBufferSize(2048);
        props.setRingBuffer(true);
        props.setProducerType(ProducerType.MULTI);
        props.setWaitStrategy(DisruptorWaitStrategy.BLOCKING_WAIT);
        props.setThreadFactory(DisruptorThreadFactory.DEFAULT_THREAD_FACTORY);
        props.setMaxBatchSize(100);
        props.setRingThreadNumbers(8);
        props.setMultiProducer(true);

        assertThat(props.isEnabled()).isTrue();
        assertThat(props.getRingBufferSize()).isEqualTo(2048);
        assertThat(props.isRingBuffer()).isTrue();
        assertThat(props.getProducerType()).isEqualTo(ProducerType.MULTI);
        assertThat(props.getWaitStrategy()).isEqualTo(DisruptorWaitStrategy.BLOCKING_WAIT);
        assertThat(props.getMaxBatchSize()).isEqualTo(100);
        assertThat(props.getRingThreadNumbers()).isEqualTo(8);
        assertThat(props.isMultiProducer()).isTrue();
    }

    @Test
    void dataAnnotation_shouldGenerateEqualsAndHashCode() {
        DisruptorProperties props1 = new DisruptorProperties();
        DisruptorProperties props2 = new DisruptorProperties();
        assertThat(props1).isEqualTo(props2);
        assertThat(props1.hashCode()).isEqualTo(props2.hashCode());
    }

    @Test
    void dataAnnotation_shouldGenerateToString() {
        DisruptorProperties props = new DisruptorProperties();
        String str = props.toString();
        assertThat(str).contains("enabled=false");
        assertThat(str).contains("ringBufferSize=1024");
    }
}
