package org.example.knockin.alarm.entity;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum AlarmType {
    DEFAULT(Values.DEFAULT),
    ROOM_MATCHING(Values.ROOM_MATCHING),
    CHATTING_REQUIRED(Values.CHATTING_REQUIRED),
    ROOMMATE_CALENDAR(Values.ROOMMATE_CALENDAR),
    ;

    private final String value;

    public static class Values {
        public static final String DEFAULT = "DEFAULT";
        public static final String ROOM_MATCHING = "ROOM_MATCHING";
        public static final String CHATTING_REQUIRED = "CHATTING_REQUIRED";
        public static final String ROOMMATE_CALENDAR = "ROOMMATE_CALENDAR";
    }
}
