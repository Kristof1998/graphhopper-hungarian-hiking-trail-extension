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
 * Stores the Hungarian-specific jel tag value from hiking OSM relations on each edge.
 * Examples: "k" (kék/blue sáv), "p" (piros/red sáv), "s+" (sárga/yellow kereszt),
 * "z3" (zöld/green háromszög).
 */
public class HikeJel {
    public static final String KEY = "hike_jel";
    public static final int MAX_UNIQUE_VALUES = 8000;

    private HikeJel() {
    }

    public static StringEncodedValue create() {
        return new StringEncodedValue(KEY, MAX_UNIQUE_VALUES);
    }
}
