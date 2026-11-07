package com.township.service.ai;

import com.township.dto.ComplaintClassificationDto;

public interface ComplaintClassificationService {

    /**
     * Classifies a complaint title and description into suggested Category and Urgency/Priority.
     * Enforces mandatory safety keyword override rules.
     */
    ComplaintClassificationDto classify(String title, String description);
}

