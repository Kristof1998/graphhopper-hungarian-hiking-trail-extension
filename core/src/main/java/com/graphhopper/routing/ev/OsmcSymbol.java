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
package com.graphhopper.routing.ev;

/**
 * Stores the osmc:symbol tag value from hiking OSM relations on each edge.
 * Format example: "blue:white:blue_bar" (waycolor:background:foreground).
 * Used for Hungarian and international hiking trail marking information.
 */
public class OsmcSymbol {
    public static final String KEY = "osmc_symbol";
    public static final int MAX_UNIQUE_VALUES = 8000;

    private OsmcSymbol() {
    }

    public static StringEncodedValue create() {
        return new StringEncodedValue(KEY, MAX_UNIQUE_VALUES);
    }
}
