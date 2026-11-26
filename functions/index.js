const { initializeApp } = require("firebase-admin/app");
const { getFirestore, Timestamp } = require("firebase-admin/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const logger = require("firebase-functions/logger");
const { drawLottery } = require("./draw");

initializeApp();

exports.drawClosedLotteries = onSchedule({
  schedule: "every 60 minutes", timeZone: "UTC", region: "us-central1",
  timeoutSeconds: 540, maxInstances: 1, retryCount: 3
}, async () => {
  const db = getFirestore();
  const now = Timestamp.now();
  // Query by deadline also includes legacy documents without drawStatus. Page past
  // completed lotteries so an old record cannot starve newer lotteries of a draw.
  const base = db.collection("lotteries").where("registrationEnd", "<=", now)
    .orderBy("registrationEnd").limit(100);
  let cursor;
  let failed = false;
  do {
    const page = await (cursor ? base.startAfter(cursor) : base).get();
    for (const lottery of page.docs) {
      if (lottery.data().drawStatus === "COMPLETED") continue;
      try { await drawLottery(db, lottery.ref, now); }
      catch (error) {
        failed = true;
        logger.error("Lottery draw will be retried", { lotteryId: lottery.id, error: String(error) });
      }
    }
    cursor = page.size === 100 ? page.docs.at(-1) : null;
  } while (cursor);
  if (failed) throw new Error("Some lotteries need a retry; their saved winner selections are retained.");
});
