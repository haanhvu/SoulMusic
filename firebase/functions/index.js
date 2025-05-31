const functions = require("firebase-functions");
const axios = require("axios");
require("dotenv").config();

exports.callGemini = functions.https.onRequest(async (req, res) => {
  try {
    const userPrompt = req.body.prompt;

    const geminiResponse = await axios.post(
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent",
        {
          contents: [{parts: [{text: userPrompt}]}],
        },
        {
          params: {
            key: process.env.API_KEY,
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

    const youtubeResponse = await axios.get(
        "https://www.googleapis.com/youtube/v3/search",
        {
          params: {
            part: "snippet",
            q: query,
            type: "video",
            maxResults: 1,
            videoCategoryId: 10,
            key: process.env.API_KEY,
          },
        },
    );

    res.json(youtubeResponse.data);
  } catch (error) {
    console.error("Error calling YouTube API:", error);
    res.status(500).json({error: "Failed to fetch from YouTube API"});
  }
});
