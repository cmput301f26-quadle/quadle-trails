// US 02.05.01: durable, retry-safe lottery draws. No device clock or open app required.
const { randomInt } = require("node:crypto");

function selectWinners(ids, count, random = randomInt) {
  if (!Number.isInteger(count) || count < 1 || count > 999) throw new Error("Invalid winner count");
  const shuffled = [...new Set(ids)];
  for (let i = shuffled.length - 1; i > 0; i--) {
    const j = random(i + 1);
    [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
  }
  return shuffled.slice(0, count);
}

/** A single durable choice, followed by resumable chunks (unlimited waitlists can exceed 500 writes). */
async function drawLottery(db, ref, now, random = randomInt) {
  const plan = await db.runTransaction(async tx => {
    const snapshot = await tx.get(ref);
    if (!snapshot.exists) return null;
    const lottery = snapshot.data();
    if (lottery.drawStatus === "COMPLETED") return null;
    if (lottery.drawStatus === "DRAWING") return lottery;
    const cutoff = lottery.registrationEnd?.toMillis();
    if (cutoff == null || cutoff > now.toMillis()) return null;
    const entries = await tx.get(ref.collection("entries"));
    const eligible = entries.docs.filter(entry => {
      const e = entry.data();
      return e.status === "WAITING" && e.joinedAt?.toMillis() <= cutoff;
    });
    // Preserve historical outcomes if a migrated lottery already contains them.
    const previous = entries.docs.filter(e => ["WON", "RESERVED", "DECLINED"].includes(e.data().status)).map(e => e.id);
    const slots = Math.max(0, lottery.numWinners - previous.length);
    if (!Number.isInteger(lottery.numWinners) || lottery.numWinners < 1 || lottery.numWinners > 999) {
      throw new Error("Invalid winner count");
    }
    const winnerIds = [...previous, ...(slots ? selectWinners(eligible.map(e => e.id), slots, random) : [])];
    const result = {
      drawStatus: "DRAWING", winnerIds,
      drawEntrantCount: eligible.length + previous.length,
      drawStartedAt: now
    };
    tx.update(ref, result);
    return result;
  });
  if (!plan) return false;

  // Each chunk re-reads both the lottery and entries. Concurrent invocations and retries
  // cannot overwrite an outcome or a later booking/decline with an older WAITING snapshot.
  const winners = new Set(plan.winnerIds);
  while (true) {
    const pending = await ref.collection("entries").where("status", "==", "WAITING").limit(200).get();
    if (pending.empty) break;
    const continued = await db.runTransaction(async tx => {
      const parent = await tx.get(ref);
      if (!parent.exists || parent.data().drawStatus !== "DRAWING") return false;
      const current = await Promise.all(pending.docs.map(entry => tx.get(entry.ref)));
      for (const entry of current) {
        if (entry.exists && entry.data().status === "WAITING") {
          tx.update(entry.ref, { status: winners.has(entry.id) ? "WON" : "LOST" });
        }
      }
      return true;
    });
    if (!continued) return false;
  }
  await db.runTransaction(async tx => {
    const parent = await tx.get(ref);
    if (!parent.exists || parent.data().drawStatus !== "DRAWING") return;
    const pending = await tx.get(ref.collection("entries").where("status", "==", "WAITING").limit(1));
    if (!pending.empty) throw new Error("Entries changed while completing draw; retry required");
    tx.update(ref, { drawStatus: "COMPLETED", drawnAt: now });
  });
  return true;
}

module.exports = { selectWinners, drawLottery };
