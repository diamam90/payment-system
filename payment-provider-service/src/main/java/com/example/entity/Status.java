package com.example.entity;

import com.example.fake.dto.StatusUpdate;

public enum Status {
    PENDING,
    SUCCESS,
    FAILED;

    public static Status fromRequest(StatusUpdate.StatusEnum statusEnum) {
        return Status.valueOf(statusEnum.name());
    }
}
