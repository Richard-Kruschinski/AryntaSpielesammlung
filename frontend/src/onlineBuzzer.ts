// Online-Ansichten für den Buzzer: Einstellung in der Lobby, Knopf für Spieler, Reihenfolge für die Spielleitung.
import { esc } from "./html";
import { viewSummary } from "./sharedViews";
import type { BuzzerSettings, BuzzerView, RoomView } from "./types";

export function viewBuzzerSettings(settings: BuzzerSettings, host: boolean): string {
  if (!host) {
    return viewSummary([settings.lockout ? "Nach dem ersten Druck sind alle anderen gesperrt." : "Jeder kann einmal drücken."]);
  }
  return `
    <label class="check">
      <input type="checkbox" id="buzzer-lockout" data-setting="buzzerLockout" ${settings.lockout ? "checked" : ""} />
      <span>Lock-out: Nach dem ersten Druck sind alle anderen gesperrt</span>
    </label>`;
}

export function viewBuzzer(room: RoomView, game: BuzzerView): string {
  const label = `<p class="game-label">Buzzer · Frage ${room.round}</p>`;
  const leave = room.you.host
    ? `<button class="btn quiet" data-action="backToLobby">Spiel beenden</button>`
    : `<button class="btn quiet" data-action="leave">Raum verlassen</button>`;
  const main = room.you.host ? viewOrder(room, game) : viewButton(game);
  return `
    ${label}
    ${main}
    <div class="actions">${leave}</div>`;
}

function viewButton(game: BuzzerView): string {
  let status: string;
  if (game.yourRank !== null) status = game.yourRank === 1 ? "Du warst Erster!" : `Du bist auf Platz ${game.yourRank}.`;
  else if (!game.canPress) status = "Gesperrt. Warte auf die Freigabe.";
  else status = "Drück, sobald du die Antwort weißt.";
  return `
    <button class="buzzer-button" data-buzzer ${game.canPress ? "" : "disabled"} aria-label="Buzzer">Buzz!</button>
    <p class="lead buzzer-status" aria-live="polite">${status}</p>`;
}

function viewOrder(room: RoomView, game: BuzzerView): string {
  const nameOf = (id: string) => room.players.find((p) => p.id === id)?.name ?? "?";
  const rows = game.presses
    .map((p) => `
      <li>
        <span class="buzzer-rank">${p.rank}.</span>
        <span class="player-name">${esc(nameOf(p.playerId))}</span>
        <span class="buzzer-offset">${p.rank === 1 ? "" : `+${p.offsetMs} ms`}</span>
      </li>`)
    .join("");
  const list = rows ? `<ol class="buzzer-order">${rows}</ol>` : `<p class="turn-role">Noch hat niemand gedrückt.</p>`;
  return `
    <header class="phase-head">
      <h1 class="title">${game.presses.length ? `${esc(nameOf(game.presses[0].playerId))} ist dran` : "Warte auf den Buzzer"}</h1>
      <p class="lead">${game.lockout ? "Lock-out ist an: Nach dem ersten Druck sind alle anderen gesperrt." : "Jeder kann einmal drücken."}</p>
    </header>
    ${list}
    <div class="actions">
      <button class="btn primary" data-action="releaseBuzzer" ${game.presses.length ? "" : "disabled"}>Buzzer freigeben</button>
    </div>`;
}
