package com.vishalgoel.meditationtimer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BackupMerger {
    private BackupMerger() {}

    public static List<MeditationLog> mergeLogs(List<MeditationLog> local,
                                                 List<MeditationLog> incoming) {
        Map<String, MeditationLog> byId = new LinkedHashMap<>();
        Set<String> content = new HashSet<>();
        for (MeditationLog value : safe(local)) {
            byId.put(value.id(), value);
            content.add(logContentKey(value));
        }
        for (MeditationLog value : safe(incoming)) {
            if (!byId.containsKey(value.id()) && content.add(logContentKey(value))) {
                byId.put(value.id(), value);
            }
        }
        return MeditationLogCodec.decode(MeditationLogCodec.encode(
                new ArrayList<>(byId.values())));
    }

    public static List<Resolution> mergeResolutions(List<Resolution> local,
                                                     List<Resolution> incoming) {
        Map<String, Resolution> byId = new LinkedHashMap<>();
        Set<String> content = new HashSet<>();
        for (Resolution value : safe(local)) {
            byId.put(value.id(), value);
            content.add(resolutionContentKey(value));
        }
        for (Resolution value : safe(incoming)) {
            if (!byId.containsKey(value.id()) && content.add(resolutionContentKey(value))) {
                byId.put(value.id(), value);
            }
        }
        return ResolutionCodec.decode(ResolutionCodec.encode(
                new ArrayList<>(byId.values())));
    }

    private static <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private static String logContentKey(MeditationLog value) {
        return value.startTimeMs() + ":" + value.endTimeMs() + ":" + value.durationMs();
    }

    private static String resolutionContentKey(Resolution value) {
        return value.dateMs() + ":" + value.comment().strip().replaceAll("\\s+", " ");
    }
}
