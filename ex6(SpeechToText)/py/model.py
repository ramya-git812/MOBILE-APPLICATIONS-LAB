from fastapi import FastAPI, UploadFile, File
import whisper

app = FastAPI()
model = whisper.load_model("base")

@app.post("/transcribe/")
async def transcribe_audio(file: UploadFile = File(...)):
    try:
        contents = await file.read()
        print(f"Received file of size: {len(contents)} bytes")
        
        # Save the uploaded file temporarily as WAV
        with open("temp_audio.wav", "wb") as audio_file:
            audio_file.write(contents)
            print("Audio file saved successfully.")

        # Transcribe the audio file
        result = model.transcribe("temp_audio.wav")
        print("Transcription result:", result)

        # Return the transcription result
        return {"transcription": result["text"]}

    except Exception as e:
        print(f"Error: {str(e)}")
        return {"error": str(e)}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
