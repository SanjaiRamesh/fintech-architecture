package com.payment.shared.dto.response;

import com.payment.shared.enums.WorkflowStep;
import com.payment.shared.enums.WorkflowStepStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStepDetail {

    private WorkflowStep step;
    private WorkflowStepStatus status;
    private Instant timestamp;
    private String failureReason;
}
