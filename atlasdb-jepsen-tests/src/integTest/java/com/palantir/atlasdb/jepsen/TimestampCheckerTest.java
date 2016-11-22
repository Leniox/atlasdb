/**
 * Copyright 2016 Palantir Technologies
 *
 * Licensed under the BSD-3 License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.atlasdb.jepsen;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;

import clojure.lang.Keyword;
import one.util.streamex.EntryStream;

public class TimestampCheckerTest {
    @Test
    public void correctHistoryShouldPass() throws IOException {
        List<Map<Keyword, ?>> convertedAllEvents = getClojureMapFromFile("history.json");

        assertThat(TimestampChecker.checkClojureHistory(convertedAllEvents)).isTrue();
    }

    private static List<Map<Keyword, ?>> getClojureMapFromFile(String resourcePath) throws IOException {
        List<Map<String, ?>> allEvents = new ObjectMapper().readValue(Resources.getResource(resourcePath),
                new TypeReference<List<Map<String, ?>>>() {});
        return allEvents.stream()
                .map(singleEvent -> {
                    Map<Keyword, Object> convertedEvent = new HashMap<>();
                    EntryStream.of(singleEvent)
                            .mapKeys(Keyword::intern)
                            .mapValues(value -> value instanceof String ? Keyword.intern((String) value) : value)
                            .forKeyValue(convertedEvent::put);
                    return convertedEvent;
                })
                .collect(Collectors.toList());
    }
}
