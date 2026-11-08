package org.example.knockin.alarm.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AlarmSettingType {
    NOTIFICATION("알림");

    private final String message;
}