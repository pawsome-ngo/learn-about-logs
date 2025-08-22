package com.example.spring_aop_logback.service;

import com.example.spring_aop_logback.dto.DelegateCreationRequest;
import com.example.spring_aop_logback.dto.DelegateCreationResponse;
import com.example.spring_aop_logback.dto.UceRequest;
import com.example.spring_aop_logback.util.web.UceServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import java.util.Map;
import java.util.UUID;

@Service
public class DelegateService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private UceServiceClient uceServiceClient;

    public Mono<DelegateCreationResponse> createDelegate(DelegateCreationRequest request) {
        // Use deferContextual to gain access to the reactive context
        return Mono.deferContextual(contextView -> {
            UceRequest uceRequest = new UceRequest(request.cardId());

            return uceServiceClient.checkEntitlement(uceRequest)
                    .flatMap(uceResponse -> {
                        // --- CRITICAL FIX: Restore the MDC from the context ---
                        // This ensures the context is available on the current thread,
                        // no matter which thread the flatMap is executing on.
                        restoreMdcFromContext(contextView);

                        if (uceResponse.isEntitled()) {
                            String sharingId = UUID.randomUUID().toString();
                            // This log will now correctly have the CR_ID prefix
                            log.info("Entitlement successful for cardId: {}. Created sharingId: {}", request.cardId(), sharingId);
                            return Mono.just(new DelegateCreationResponse("SUCCESS", sharingId, "Delegate card has been created."));
                        } else {
                            log.warn("Entitlement failed for cardId: {}", request.cardId());
                            return Mono.just(new DelegateCreationResponse("FAILED", null, "Card is not entitled for delegation."));
                        }
                    });
        });
    }

    private void restoreMdcFromContext(ContextView contextView) {
        Map<String, String> mdcContextMap = contextView.getOrDefault("mdcContext", null);
        if (mdcContextMap != null) {
            MDC.setContextMap(mdcContextMap);
        }
    }
}