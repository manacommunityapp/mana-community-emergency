package com.manacommunity.emergency.dto;

import lombok.Data;

@Data
public class ReviewClosureRequest {
    private Boolean isApproved;
    private String adminNotes;
}
