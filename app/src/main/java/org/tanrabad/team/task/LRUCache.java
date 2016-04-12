/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.tanrabad.team.task;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LruCache {
    private final int maxSize;
    private int totalSize;
    private final ConcurrentLinkedQueue<String> queue;
    private final ConcurrentHashMap<String, String> map;

    public LruCache(final int maxSize) {
        this.maxSize = maxSize;
        this.queue = new ConcurrentLinkedQueue<>();
        this.map = new ConcurrentHashMap<>();
    }

    public String get(final String key) {
        if (map.containsKey(key)) {
            queue.remove(key);
            queue.add(key);
        }

        return map.get(key);
    }

    public void put(final String key, final String value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Key or Value is null");
        }

        if (map.containsKey(key)) {
            queue.remove(key);
        }

        queue.add(key);
        map.put(key, value);
        totalSize = totalSize + getSize(value);

        while (totalSize >= maxSize) {
            String expiredKey = queue.poll();
            if (expiredKey != null) {
                totalSize = totalSize - getSize(map.remove(expiredKey));
            }
        }
    }

    private int getSize(String value) {
        return value.length();
    }
}
