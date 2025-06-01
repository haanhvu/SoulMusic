const functions = require("firebase-functions");
const axios = require("axios");
const {SecretManagerServiceClient} = require("@google-cloud/secret-manager");

const client = new SecretManagerServiceClient();

// eslint-disable-next-line require-jsdoc
async function getApiKey() {
  const [version] = await client.accessSecretVersion({
    name: "projects/soulmusic-d8c81/secrets/API_KEY/versions/latest",
  });
  return version.payload.data.toString();
}

exports.callGemini = functions.https.onRequest(async (req, res) => {
  try {
    const userPrompt = req.body.prompt;
    const apiKey = await getApiKey();

    const geminiResponse = await axios.post(
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent",
        {
          contents: [{parts: [{text: userPrompt}]}],
        },
        {
          params: {
            key: apiKey,
          },
        },
    );

    res.json(geminiResponse.data);
  } catch (error) {
    console.error("Error calling Gemini:", error);
    res.status(500).json({error: "Failed to fetch from Gemini API"});
  }
});

exports.callYoutube = functions.https.onRequest(async (req, res) => {
  try {
    const query = req.body.query;
    const apiKey = await getApiKey();

    const youtubeResponse = await axios.get(
        "https://www.googleapis.com/youtube/v3/search",
        {
          params: {
            part: "snippet",
            q: query,
            type: "video",
            maxResults: 1,
            videoCategoryId: 10,
            key: apiKey,
          },
        },
    );

    res.json(youtubeResponse.data);
  } catch (error) {
    console.error("Error calling YouTube API:", error);
    res.status(500).json({error: "Failed to fetch from YouTube API"});
  }
});
