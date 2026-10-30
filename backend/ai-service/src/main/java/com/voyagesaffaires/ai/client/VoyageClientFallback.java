package com.voyagesaffaires.ai.client;

import com.voyagesaffaires.common.dto.MatchDto;
import com.voyagesaffaires.common.dto.VoyageDto;
import com.voyagesaffaires.common.feign.FeignFallbacks;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Le contexte (voyages, programmes, invitations) n'est qu'un plus pour l'IA : en cas de panne du
 * voyage-service, l'assistant repond sans ce contexte. Le detail d'un match, lui, est indispensable.
 */
@Component
@Slf4j
public class VoyageClientFallback implements FallbackFactory<VoyageClient> {

    @Override
    public VoyageClient create(Throwable cause) {
        return new VoyageClient() {
            @Override
            public List<VoyageDto> planning() {
                return contexteVide();
            }

            @Override
            public List<VoyageDto> programmes() {
                return contexteVide();
            }

            @Override
            public List<MatchDto> invitationsRecues() {
                return contexteVide();
            }

            @Override
            public MatchDto match(Long id) {
                throw FeignFallbacks.propager(cause, "service voyages");
            }

            private <T> List<T> contexteVide() {
                if (!FeignFallbacks.estPanne(cause)) {
                    throw FeignFallbacks.propager(cause, "service voyages");
                }
                log.warn("voyage-service indisponible, l'IA repond sans contexte : {}", cause.toString());
                return List.of();
            }
        };
    }
}
