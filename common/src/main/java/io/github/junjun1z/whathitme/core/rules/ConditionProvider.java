package io.github.junjun1z.whathitme.core.rules;

import io.github.junjun1z.whathitme.core.server.PlayerHandle;
import io.github.junjun1z.whathitme.core.server.ServerPort;

/** Evaluates the conditions of one prefix ("advancement", "ftbquests", ...). */
public interface ConditionProvider {

    /** Why the provider cannot work on this server (mod missing, version not supported), null = available */
    String unavailable(ServerPort srv);

    /** Does the player fulfil the condition? May throw on an API mismatch (counted as false, logged once). */
    boolean test(PlayerHandle p, String arg) throws Exception;

    /** Syntax problem of an argument, null = fine */
    default String checkArg(String arg) {
        return arg == null || arg.isEmpty() ? "empty argument" : null;
    }

    /** Arguments the pack editor offers ("id" or "id	title"); empty = none known */
    default java.util.List<String> suggestions() throws Exception {
        return java.util.Collections.emptyList();
    }
}
