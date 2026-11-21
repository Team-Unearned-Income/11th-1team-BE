package org.example.knockin.declaration.event;

import lombok.RequiredArgsConstructor;
import org.example.knockin.declaration.service.DeclarationService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberReportRequestedEventListener {
    private final DeclarationService declarationService;

    @EventListener
    public void handle(MemberReportRequestedEvent event) {
        declarationService.reportMember(event.getReporterId(), event.getReportedId(), event.getReason());
        event.complete();
    }
}
