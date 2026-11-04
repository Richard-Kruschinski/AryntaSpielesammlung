package de.kruschinski.werwolf.game;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuzzerTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-04T18:00:00Z"));

    /** p1 (Anna) ist Host, p2 bis p4 buzzern. */
    private Room started(boolean lockout) {
        Room room = new Room("TEST", clock);
        List.of("Anna", "Ben", "Cem", "Dora").forEach(room::addPlayer);
        room.selectGame("p1", GameType.BUZZER);
        room.configureBuzzer("p1", lockout);
        room.players().forEach(p -> room.setReady(p.id(), true));
        room.start("p1", new Random(1));
        return room;
    }

    private static List<RoomView.BuzzerPress> hostSees(Room room) {
        return room.viewFor("p1").buzzer().presses();
    }

    @Test
    void ohneLockoutSiehtDerHostDieReihenfolgeMitAbstand() {
        Room room = started(false);
        room.pressBuzzer("p3");
        clock.advance(Duration.ofMillis(132));
        room.pressBuzzer("p2");
        clock.advance(Duration.ofMillis(40));
        room.pressBuzzer("p4");

        assertEquals(List.of(
                new RoomView.BuzzerPress("p3", 1, 0),
                new RoomView.BuzzerPress("p2", 2, 132),
                new RoomView.BuzzerPress("p4", 3, 172)), hostSees(room));
    }

    @Test
    void mitLockoutZaehltNurDerErste() {
        Room room = started(true);
        room.pressBuzzer("p2");
        room.pressBuzzer("p3");

        assertEquals(1, hostSees(room).size());
        assertFalse(room.viewFor("p3").buzzer().canPress());
        assertNull(room.viewFor("p3").buzzer().yourRank());
    }

    @Test
    void doppeltDrueckenZaehltEinmal() {
        Room room = started(false);
        room.pressBuzzer("p2");
        room.pressBuzzer("p2");

        assertEquals(1, hostSees(room).size());
        assertEquals(1, room.viewFor("p2").buzzer().yourRank());
    }

    @Test
    void freigabeLeertDieListeUndEntsperrt() {
        Room room = started(true);
        room.pressBuzzer("p2");
        room.releaseBuzzer("p1");

        assertTrue(hostSees(room).isEmpty());
        assertTrue(room.viewFor("p3").buzzer().canPress());
        assertEquals(2, room.round());
    }

    @Test
    void hostHatKeinenBuzzer() {
        Room room = started(false);
        assertThrows(GameException.class, () -> room.pressBuzzer("p1"));
        assertFalse(room.viewFor("p1").buzzer().canPress());
    }

    @Test
    void nurDerHostGibtFrei() {
        Room room = started(false);
        assertThrows(GameException.class, () -> room.releaseBuzzer("p2"));
    }

    @Test
    void spielerSehenNichtDieGanzeReihenfolge() {
        Room room = started(false);
        room.pressBuzzer("p2");
        room.pressBuzzer("p3");

        RoomView.BuzzerView view = room.viewFor("p3").buzzer();
        assertTrue(view.presses().isEmpty());
        assertEquals(2, view.yourRank());
    }

    @Test
    void brauchtZweiSpielerOhneHost() {
        Room room = new Room("TEST", clock);
        room.addPlayer("Anna");
        room.addPlayer("Ben");
        room.selectGame("p1", GameType.BUZZER);
        room.players().forEach(p -> room.setReady(p.id(), true));

        assertThrows(GameException.class, () -> room.start("p1", new Random(1)));
    }
}
