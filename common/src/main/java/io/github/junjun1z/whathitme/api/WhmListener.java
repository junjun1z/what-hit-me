package io.github.junjun1z.whathitme.api;

/** Receives one kind of event ({@link DamageRecordedEvent}, {@link DeathEvent}, {@link RespawnEvent}, {@link AlertEvent}). */
@FunctionalInterface
public interface WhmListener<E> {

    /** An exception is caught and logged once per listener */
    void handle(E event) throws Exception;
}
