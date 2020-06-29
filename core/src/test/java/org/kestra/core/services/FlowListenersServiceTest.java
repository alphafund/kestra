package org.kestra.core.services;

import com.devskiller.friendly_id.FriendlyId;
import com.google.common.collect.ImmutableList;
import io.micronaut.test.annotation.MicronautTest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.kestra.core.models.flows.Flow;
import org.kestra.core.models.flows.Input;
import org.kestra.core.repositories.FlowRepositoryInterface;
import org.kestra.core.tasks.debugs.Return;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@MicronautTest
class FlowListenersServiceTest {
    @Inject
    FlowListenersService flowListenersService;

    @Inject
    FlowRepositoryInterface flowRepository;

    private static Flow create() {
        return Flow.builder()
            .id(FriendlyId.createFriendlyId())
            .namespace("org.kestra.unittest")
            .revision(1)
            .tasks(Collections.singletonList(Return.builder().id("test").type(Return.class.getName()).format("test").build()))
            .build();
    }

    @Test
    void all() {
        AtomicInteger count = new AtomicInteger();
        var ref = new Ref();

        flowListenersService.listen(flows -> {
            count.set(flows.size());
            ref.countDownLatch.countDown();
        });

        wait(ref, () -> {
            assertThat(count.get(), is(0));
        });

        Flow first = create();

        flowRepository.create(first);
        wait(ref, () -> {
            assertThat(count.get(), is(1));
        });

        flowRepository.update(first, first);
        wait(ref, () -> {
            assertThat(count.get(), is(1));
        });


        flowRepository.update(first.withRevision(2), first);
        wait(ref, () -> {
            assertThat(count.get(), is(1));
        });

        flowRepository.create(create());
        wait(ref, () -> {
            assertThat(count.get(), is(2));
        });

        flowRepository.delete(first);
        wait(ref, () -> {
            assertThat(count.get(), is(1));
        });
    }

    public static class Ref {
        CountDownLatch countDownLatch = new CountDownLatch(1);
    }

    @SneakyThrows
    private void wait(Ref ref, Runnable run)  {
        ref.countDownLatch.await();
        run.run();
        ref.countDownLatch = new CountDownLatch(1);
    }
}