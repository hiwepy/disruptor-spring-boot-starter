package com.lmax.disruptor.spring.boot.event;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DisruptorApplicationEvent}.
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 */
class DisruptorApplicationEventTest {

    @Test
    void constructorWithSource_shouldSetSource() {
        Object source = new Object();
        DisruptorApplicationEvent event = new DisruptorApplicationEvent(source);
        assertThat(event.getSource()).isSameAs(source);
        assertThat(event.getBind()).isNull();
    }

    @Test
    void constructorWithSourceAndBind_shouldSetBoth() {
        Object source = "source";
        Object bind = "data";
        DisruptorApplicationEvent event = new DisruptorApplicationEvent(source, bind);
        assertThat(event.getSource()).isSameAs(source);
        assertThat(event.getBind()).isSameAs(bind);
    }

    @Test
    void bind_shouldSetBindObject() {
        DisruptorApplicationEvent event = new DisruptorApplicationEvent("source");
        assertThat(event.getBind()).isNull();
        Object bind = new Object();
        event.bind(bind);
        assertThat(event.getBind()).isSameAs(bind);
    }

    @Test
    void getBind_shouldReturnBindObject() {
        DisruptorApplicationEvent event = new DisruptorApplicationEvent("source", "myData");
        assertThat(event.getBind()).isEqualTo("myData");
    }
}
