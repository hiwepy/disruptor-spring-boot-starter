package com.lmax.disruptor.spring.boot;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.EventTranslatorThreeArg;
import com.lmax.disruptor.EventTranslatorTwoArg;
import com.lmax.disruptor.event.DisruptorEvent;
import com.lmax.disruptor.spring.boot.context.DisruptorEventAwareProcessor;
import com.lmax.disruptor.spring.boot.event.DisruptorApplicationEvent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationListener;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DisruptorAutoConfiguration}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
class DisruptorAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DisruptorAutoConfiguration.class));

    @Test
    void whenEnabledPropertyIsFalse_shouldNotCreateBeans() {
        contextRunner
                .withPropertyValues("spring.disruptor.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("disruptor");
                    assertThat(context).doesNotHaveBean("disruptorTemplate");
                });
    }

    @Test
    void whenEnabledPropertyIsTrue_shouldCreateBeans() {
        contextRunner
                .withPropertyValues("spring.disruptor.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(EventFactory.class);
                    assertThat(context).hasSingleBean(EventTranslatorOneArg.class);
                    assertThat(context).hasSingleBean(EventTranslatorTwoArg.class);
                    assertThat(context).hasSingleBean(EventTranslatorThreeArg.class);
                    assertThat(context).hasSingleBean(DisruptorEventAwareProcessor.class);
                    assertThat(context).hasBean("disruptor");
                    assertThat(context).hasBean("disruptorTemplate");
                    assertThat(context).hasBean("disruptorEventListener");
                });
    }

    @Test
    void autoConfiguration_shouldBindProperties() {
        contextRunner
                .withPropertyValues(
                        "spring.disruptor.enabled=true",
                        "spring.disruptor.ring-buffer-size=2048",
                        "spring.disruptor.ring-thread-numbers=8"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(DisruptorProperties.class);
                    DisruptorProperties props = context.getBean(DisruptorProperties.class);
                    assertThat(props.getRingBufferSize()).isEqualTo(2048);
                    assertThat(props.getRingThreadNumbers()).isEqualTo(8);
                });
    }

    @Test
    void eventFactory_shouldCreateDisruptorEvent() {
        contextRunner
                .withPropertyValues("spring.disruptor.enabled=true")
                .run(context -> {
                    EventFactory<DisruptorEvent> factory = context.getBean(EventFactory.class);
                    DisruptorEvent event = factory.newInstance();
                    assertThat(event).isNotNull();
                    assertThat(event).isInstanceOf(DisruptorEvent.class);
                });
    }

    @Test
    void disruptorEventAwareProcessor_shouldBeRegistered() {
        contextRunner
                .withPropertyValues("spring.disruptor.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(DisruptorEventAwareProcessor.class);
                });
    }

    @Test
    void setApplicationContext_shouldStoreContext() {
        DisruptorAutoConfiguration config = new DisruptorAutoConfiguration();
        org.springframework.context.ApplicationContext mockCtx = org.mockito.Mockito.mock(org.springframework.context.ApplicationContext.class);
        config.setApplicationContext(mockCtx);
        assertThat(config.getApplicationContext()).isSameAs(mockCtx);
    }
}
