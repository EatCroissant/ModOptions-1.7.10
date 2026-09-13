package com.garden.modoptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OptionRegistry {
    private static final Map<String, List<ModOption>> MODS =
            new LinkedHashMap<String, List<ModOption>>();
    private static final Map<String, LinkedHashMap<String, OptionGroup>> GROUPS =
            new LinkedHashMap<String, LinkedHashMap<String, OptionGroup>>();

    public static synchronized void register(String modId, ModOption option) {
        if (modId == null || modId.length() == 0 || option == null) return;
        List<ModOption> options = MODS.get(modId);
        if (options == null) {
            options = new ArrayList<ModOption>();
            MODS.put(modId, options);
        }
        options.add(option);
        ensureGroup(modId, option.getGroupId());
    }

    public static synchronized void register(String modId, String groupId, ModOption option) {
        if (option == null) return;
        option.group(groupId);
        register(modId, option);
    }

    public static synchronized void registerGroup(String modId, OptionGroup group) {
        if (modId == null || modId.length() == 0 || group == null) return;
        LinkedHashMap<String, OptionGroup> groups = GROUPS.get(modId);
        if (groups == null) {
            groups = new LinkedHashMap<String, OptionGroup>();
            GROUPS.put(modId, groups);
        }
        groups.put(group.getId(), group);
    }

    public static synchronized Map<String, List<ModOption>> mods() {
        Map<String, List<ModOption>> result =
                new LinkedHashMap<String, List<ModOption>>();
        for (Map.Entry<String, List<ModOption>> entry : MODS.entrySet()) {
            List<ModOption> options = new ArrayList<ModOption>(entry.getValue());
            Collections.sort(options, new Comparator<ModOption>() {
                @Override
                public int compare(ModOption a, ModOption b) {
                    int order = Integer.compare(a.getOrder(), b.getOrder());
                    return order != 0 ? order : 0;
                }
            });
            result.put(entry.getKey(), Collections.unmodifiableList(options));
        }
        return result;
    }

    public static synchronized List<OptionGroup> groups(String modId) {
        LinkedHashMap<String, OptionGroup> groups = GROUPS.get(modId);
        List<OptionGroup> result = new ArrayList<OptionGroup>();
        if (groups != null) result.addAll(groups.values());
        Collections.sort(result, new Comparator<OptionGroup>() {
            @Override
            public int compare(OptionGroup a, OptionGroup b) {
                return Integer.compare(a.getOrder(), b.getOrder());
            }
        });
        return Collections.unmodifiableList(result);
    }

    public static synchronized boolean hasOptions(String modId) {
        List<ModOption> options = MODS.get(modId);
        return options != null && !options.isEmpty();
    }

    private static void ensureGroup(String modId, String groupId) {
        String id = groupId == null || groupId.length() == 0 ? "general" : groupId;
        LinkedHashMap<String, OptionGroup> groups = GROUPS.get(modId);
        if (groups == null) {
            groups = new LinkedHashMap<String, OptionGroup>();
            GROUPS.put(modId, groups);
        }
        if (!groups.containsKey(id)) {
            groups.put(id, new OptionGroup(id, "modoptions.group." + modId + "." + id));
        }
    }

    private OptionRegistry() {}
}
