/*
Copyright 2022 The OpenFunction Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package dev.openfunction.samples.hooks;

import dev.openfunction.functions.Context;
import dev.openfunction.functions.Hook;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class ExampleHook implements Hook {
    private int seq = 0;

    @Override
    public String name() {
        return "hook-example";
    }

    @Override
    public String version() {
        return "v1.0.0";
    }

    @Override
    public Hook init() {
        return this;
    }

    @Override
    public Error execute(Context ctx) {
        String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.XXX").format(new Date());
        if (ctx.getBindingEvent() != null) {
            System.out.printf("hook %s:%s exec for binding %s at %s, seq %d", name(), version(), ctx.getBindingEvent().getName(), ts, seq).println();
        } else if (ctx.getTopicEvent() != null) {
            System.out.printf("hook %s:%s exec for pubsub %s at %s, seq %d", name(), version(), ctx.getTopicEvent().getName(), ts, seq).println();
        } else if (ctx.getHttpRequest() != null) {
            if (ctx.getCloudEvent() != null) {
                System.out.printf("hook %s:%s exec for cloudevent function at %s, seq %d", name(), version(), ts, seq).println();
            } else {
                System.out.printf("hook %s:%s exec for http function at %s, seq %d", name(), version(), ts, seq).println();
            }
        } else {
            System.out.println("unknown function type");
        }
        seq++;

        return null;
    }

    @Override
    public Boolean needToTracing() {
        return true;
    }

    @Override
    public Map<String, String> tagsAddToTracing() {
        return null;
    }
}
