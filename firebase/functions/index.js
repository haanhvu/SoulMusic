const functions = require("firebase-functions");
const axios = require("axios");

exports.searchYoutube = functions.https.onCall(async (data, context) => {
  const searchTerm = data.query;

  const apiKey = functions.config().api.key;
  const url = "https://www.googleapis.com/youtube/v3/search";

  try {
    const response = await axios.get(url, {
      params: {
        part: "snippet",
        q: searchTerm,
        type: "video",
        maxResults: 1,
        videoCategoryId: 10,
        key: apiKey,
      },
    });

    return response.data;
  } catch (error) {
    throw new functions.https.HttpsError("Youtube", error.message);
  }
});
