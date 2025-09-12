    package com.project.korex.ForeignTransfer.controller;

    import com.project.korex.ForeignTransfer.dto.request.RecipientRequest;
    import com.project.korex.ForeignTransfer.dto.response.RecipientResponse;
    import com.project.korex.ForeignTransfer.service.RecipientService;
    import com.project.korex.common.security.user.CustomUserPrincipal;
    import io.swagger.v3.oas.annotations.Operation;
    import io.swagger.v3.oas.annotations.Parameter;
    import io.swagger.v3.oas.annotations.media.Content;
    import io.swagger.v3.oas.annotations.media.Schema;
    import io.swagger.v3.oas.annotations.responses.ApiResponse;
    import io.swagger.v3.oas.annotations.responses.ApiResponses;
    import lombok.RequiredArgsConstructor;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.ResponseEntity;
    import org.springframework.security.core.annotation.AuthenticationPrincipal;
    import org.springframework.web.bind.annotation.*;

    import java.util.List;

    @RestController
    @RequestMapping("/api/foreign-transfer/recipients")
    @RequiredArgsConstructor
    public class RecipientController {

        private final RecipientService recipientService;

        @PostMapping
        public ResponseEntity<RecipientResponse> createRecipient(
                @RequestBody RecipientRequest request,
                @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal principal
        ) {
            String loginId = principal.getName();
            RecipientResponse response = recipientService.createRecipient(loginId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @GetMapping("/{recipientId}")
        public ResponseEntity<RecipientResponse> getRecipient(
                @Parameter(description = "조회할 수취인 ID") @PathVariable Long recipientId,
                @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal principal
        ) {
            String loginId = principal.getName();
            RecipientResponse response = recipientService.getRecipientById(loginId, recipientId);
            return ResponseEntity.ok(response);
        }

        @GetMapping
        public ResponseEntity<List<RecipientResponse>> getRecipientsByUser(
                @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal principal
        ) {
            String loginId = principal.getName();
            List<RecipientResponse> recipients = recipientService.getRecipientsByLoginId(loginId);
            return ResponseEntity.ok(recipients);
        }

        @GetMapping("/active")
        public ResponseEntity<List<RecipientResponse>> getActiveRecipients(
                @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal principal
        ) {
            String loginId = principal.getName();
            List<RecipientResponse> recipients = recipientService.getActiveRecipientsByLoginId(loginId);
            return ResponseEntity.ok(recipients);
        }

        @PutMapping("/{recipientId}")
        public ResponseEntity<RecipientResponse> updateRecipient(
                @Parameter(description = "수정할 수취인 ID") @PathVariable Long recipientId,
                @RequestBody RecipientRequest request,
                @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal principal
        ) {
            String loginId = principal.getName();
            RecipientResponse response = recipientService.updateRecipient(loginId, recipientId, request);
            return ResponseEntity.ok(response);
        }

        @PatchMapping("/{id}/deactivate")
        public ResponseEntity<Void> deactivateRecipient(
                @Parameter(description = "비활성화할 수취인 ID") @PathVariable Long id
        ) {
            recipientService.deactivateRecipient(id);
            return ResponseEntity.ok().build();
        }

        @GetMapping("/check")
        public ResponseEntity<Boolean> hasRecipients(
                @AuthenticationPrincipal CustomUserPrincipal principal) {
            String loginId = principal.getName();
            List<RecipientResponse> activeRecipients = recipientService.getActiveRecipientsByLoginId(loginId);
            boolean hasRecipients = !activeRecipients.isEmpty();
            return ResponseEntity.ok(hasRecipients);
        }
    }
