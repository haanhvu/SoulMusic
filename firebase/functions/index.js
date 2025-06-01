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
    res.status(500).json({error: "Something went wrong"});
  }
});
