package com.vishalgoel.meditationtimer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BackupMerger {
    private BackupMerger() {}

    public static List<MeditationLog> mergeLogs(List<MeditationLog> local,
                                                 List<MeditationLog> incoming) {
        Map<String, MeditationLog> byId = new LinkedHashMap<>();
        for (MeditationLog value : safe(local)) {
            byId.put(value.id(), value);
        }
        for (MeditationLog value : safe(incoming)) {
            byId.putIfAbsent(value.id(), value);
        }
        return MeditationLogCodec.decode(MeditationLogCodec.encode(
                new ArrayList<>(byId.values())));
    }

    public static List<Resolution> mergeResolutions(List<Resolution> local,
                                                     List<Resolution> incoming) {
        Map<String, Resolution> byId = new LinkedHashMap<>();
        for (Resolution value : safe(local)) {
            byId.put(value.id(), value);
        }
        for (Resolution value : safe(incoming)) {
            byId.putIfAbsent(value.id(), value);
        }
        return ResolutionCodec.decode(ResolutionCodec.encode(
                new ArrayList<>(byId.values())));
    }

    private static <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }
}
