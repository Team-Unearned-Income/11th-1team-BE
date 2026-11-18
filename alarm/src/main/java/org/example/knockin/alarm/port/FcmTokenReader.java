package org.example.knockin.alarm.port;

import java.util.Optional;

public interface FcmTokenReader {
    Optional<String> readFcmTokenById(Long id);
}
