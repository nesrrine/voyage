package com.voyagesaffaires.ai.client;

import com.voyagesaffaires.common.feign.FeignFallbacks;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/** Sans la discussion du forum, le resume est impossible : on renvoie une erreur explicite. */
@Component
public class ForumClientFallback implements FallbackFactory<ForumClient> {

    @Override
    public ForumClient create(Throwable cause) {
        return id -> {
            throw FeignFallbacks.propager(cause, "service forum");
        };
    }
}
