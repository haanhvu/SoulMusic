const functions = require("firebase-functions");

const apiKey = functions.config().api.key;

exports.getApiKey = functions.https.onCall(async (data, context) => {
  return apiKey;
});
