package com.voyagesaffaires.ai.client;

import com.voyagesaffaires.common.dto.MatchDto;
import com.voyagesaffaires.common.dto.VoyageDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/** Donnees du voyage-service utilisees comme contexte par l'IA (jeton de l'utilisateur relaye). */
@FeignClient(name = "voyage-service")
public interface VoyageClient {

    @GetMapping("/api/voyages")
    List<VoyageDto> planning();

    @GetMapping("/api/voyages/programmes")
    List<VoyageDto> programmes();

    @GetMapping("/api/matchs/{id}")
    MatchDto match(@PathVariable("id") Long id);

    @GetMapping("/api/matchs/recues")
    List<MatchDto> invitationsRecues();
}
