package io.github.junjun1z.whathitme.core.cmd;

import java.util.List;

import io.github.junjun1z.whathitme.core.text.Txt;

/** Where command output goes */
public interface Sink {

    void send(Txt line);

    /** An error line (already red); the version may show it as a command failure */
    default void fail(Txt line) {
        send(line);
    }

    default void send(List<Txt> lines) {
        for (Txt l : lines) send(l);
    }
}
