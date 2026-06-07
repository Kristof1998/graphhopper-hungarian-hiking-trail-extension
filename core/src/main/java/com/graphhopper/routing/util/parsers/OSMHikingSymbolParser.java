/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.routing.util.parsers;

import com.graphhopper.reader.ReaderRelation;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.*;
import com.graphhopper.storage.IntsRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

import static com.graphhopper.routing.util.EncodingManager.getKey;

/**
 * Reads osmc:symbol, jel, and name tags from hiking OSM relations and stores
 * them as pipe-separated StringEncodedValues on graph edges.
 * <p>
 * When a way belongs to multiple relations, ALL relation values are accumulated
 * and stored as "|"-delimited strings (e.g. "k|s|z3"). Duplicate values within
 * the same field are suppressed.
 * <p>
 * Activate by adding osmc_symbol, hike_jel, and/or hike_route_name to
 * graph.encoded_values in the configuration. Each is optional independently.
 */
public class OSMHikingSymbolParser implements RelationTagParser {

    private static final Logger logger = LoggerFactory.getLogger(OSMHikingSymbolParser.class);

    private static final int MAX_OSMC_LENGTH = 30;
    private static final int MAX_JEL_LENGTH = 10;
    private static final int MAX_NAME_LENGTH = 50;

    // transformer key suffixes stored in relFlags during first OSM pass
    private static final String OSMC_TRANS_KEY = getKey("osmc_symbol", "route_relation");
    private static final String JEL_TRANS_KEY = getKey("hike_jel", "route_relation");
    private static final String NAME_TRANS_KEY = getKey("hike_route_name", "route_relation");

    // transformers written into relFlags (always registered for stable bit layout)
    private final StringEncodedValue osmcTransformerEnc;
    private final StringEncodedValue jelTransformerEnc;
    private final StringEncodedValue nameTransformerEnc;

    // edge-level encoded values (null when not configured)
    private final StringEncodedValue osmcSymbolEnc;
    private final StringEncodedValue hikeJelEnc;
    private final StringEncodedValue hikeRouteNameEnc;

    // collected during import for diagnostics
    private final Set<String> seenJel = new LinkedHashSet<>();
    private final Set<String> seenOsmc = new LinkedHashSet<>();

    public OSMHikingSymbolParser(StringEncodedValue osmcSymbolEnc,
                                  StringEncodedValue hikeJelEnc,
                                  StringEncodedValue hikeRouteNameEnc,
                                  EncodedValue.InitializerConfig relConfig) {
        this.osmcSymbolEnc = osmcSymbolEnc;
        this.hikeJelEnc = hikeJelEnc;
        this.hikeRouteNameEnc = hikeRouteNameEnc;

        this.osmcTransformerEnc = new StringEncodedValue(OSMC_TRANS_KEY, 50000);
        this.osmcTransformerEnc.init(relConfig);

        this.jelTransformerEnc = new StringEncodedValue(JEL_TRANS_KEY, 8000);
        this.jelTransformerEnc.init(relConfig);

        this.nameTransformerEnc = new StringEncodedValue(NAME_TRANS_KEY, 100000);
        this.nameTransformerEnc.init(relConfig);

        if (Boolean.getBoolean("hiking.exportJelValues")) {
            Runtime.getRuntime().addShutdownHook(new Thread(this::dumpSeenValues, "hiking-symbol-dump"));
        }
    }

    @Override
    public void handleRelationTags(IntsRef relFlags, ReaderRelation relation) {
        if (!relation.hasTag("route", "hiking") && !relation.hasTag("route", "foot"))
            return;

        IntsRefEdgeIntAccess relAccess = new IntsRefEdgeIntAccess(relFlags);

        String osmc = truncate(relation.getTag("osmc:symbol"), MAX_OSMC_LENGTH);
        if (osmc != null) seenOsmc.add(osmc);
        appendUnique(osmcTransformerEnc, relAccess, osmc);

        String jel = truncate(relation.getTag("jel"), MAX_JEL_LENGTH);
        if (jel != null) seenJel.add(jel);
        appendUnique(jelTransformerEnc, relAccess, jel);

        appendUnique(nameTransformerEnc, relAccess, truncate(relation.getTag("name"), MAX_NAME_LENGTH));
    }

    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay way, IntsRef relFlags) {
        IntsRefEdgeIntAccess relAccess = new IntsRefEdgeIntAccess(relFlags);

        if (osmcSymbolEnc != null) {
            String value = osmcTransformerEnc.getString(false, -1, relAccess);
            osmcSymbolEnc.setString(false, edgeId, edgeIntAccess, value != null ? value : "");
        }
        if (hikeJelEnc != null) {
            String value = jelTransformerEnc.getString(false, -1, relAccess);
            hikeJelEnc.setString(false, edgeId, edgeIntAccess, value != null ? value : "");
        }
        if (hikeRouteNameEnc != null) {
            String value = nameTransformerEnc.getString(false, -1, relAccess);
            hikeRouteNameEnc.setString(false, edgeId, edgeIntAccess, value != null ? value : "");
        }
    }

    private static void appendUnique(StringEncodedValue enc, IntsRefEdgeIntAccess relAccess, String newVal) {
        if (newVal == null) return;
        String existing = enc.getString(false, -1, relAccess);
        if (existing == null || existing.isEmpty()) {
            enc.setString(false, -1, relAccess, newVal);
        } else if (!containsToken(existing, newVal)) {
            enc.setString(false, -1, relAccess, existing + "|" + newVal);
        }
    }

    private static boolean containsToken(String haystack, String needle) {
        int start = 0;
        while (start <= haystack.length()) {
            int end = haystack.indexOf('|', start);
            if (end == -1) end = haystack.length();
            if (haystack.substring(start, end).equals(needle)) return true;
            start = end + 1;
        }
        return false;
    }

    private void dumpSeenValues() {
        try {
            Files.write(Paths.get("seen_jel_values.txt"), seenJel);
            logger.info("hike_jel: {} unique values written to seen_jel_values.txt", seenJel.size());
        } catch (IOException e) {
            logger.warn("Could not write seen_jel_values.txt: {}", e.getMessage());
        }
        try {
            Files.write(Paths.get("seen_osmc_values.txt"), seenOsmc);
            logger.info("osmc_symbol: {} unique values written to seen_osmc_values.txt", seenOsmc.size());
        } catch (IOException e) {
            logger.warn("Could not write seen_osmc_values.txt: {}", e.getMessage());
        }
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
