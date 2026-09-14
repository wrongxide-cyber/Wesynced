const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.onMoodChanged = functions.database
  .ref("/pairs/{pairingId}/members/{deviceId}/mood")
  .onWrite(async (change, context) => {
    const { pairingId, deviceId } = context.params;
    const newMood = change.after.val();
    if (!newMood) return null;

    const membersSnap = await admin
      .database()
      .ref(`/pairs/${pairingId}/members`)
      .once("value");

    const members = membersSnap.val();
    if (!members) return null;

    const sendPromises = [];
    for (const otherDeviceId of Object.keys(members)) {
      if (otherDeviceId === deviceId) continue;
      const token = members[otherDeviceId]?.fcmToken;
      if (!token) continue;

      sendPromises.push(
        admin.messaging().send({
          token,
          data: { emoji: newMood },
          android: { priority: "high" },
        }).catch((err) => {
          console.error(`Failed to send to ${otherDeviceId}:`, err);
        })
      );
    }
    return Promise.all(sendPromises);
  });
