package com.voyagesaffaires.ai.client;

import com.voyagesaffaires.common.dto.ForumDtos.PostDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "forum-service")
public interface ForumClient {

    @GetMapping("/api/forum/posts/{id}")
    PostDto post(@PathVariable("id") Long id);
}
