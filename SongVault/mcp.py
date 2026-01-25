"""
MCP Music Server - Connects AI Agent to Your Music App
This server exposes tools that the AI agent can call to send music data to your app.
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import List, Optional
import httpx
import os
from datetime import datetime

app = FastAPI(
    title="Music Discovery MCP Server",
    description="MCP server for sending discovered music tracks to your music app",
    version="1.0.0"
)

# Enable CORS for agent access
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Your music app's API endpoint (configure via environment variable)
MUSIC_APP_API_URL = os.getenv("MUSIC_APP_API_URL", "http://localhost:3000/api/tracks")
MUSIC_APP_API_KEY = os.getenv("MUSIC_APP_API_KEY", "")


# Data models
class Track(BaseModel):
    artist: str = Field(..., description="Artist name")
    title: str = Field(..., description="Song title")
    genre: Optional[str] = Field(None, description="Genre or style tags")
    source_url: Optional[str] = Field(None, description="Source URL where track was found")
    note: Optional[str] = Field(None, description="Description or note about the track")


class SendTracksRequest(BaseModel):
    tracks: List[Track] = Field(..., description="List of music tracks to send to your app")


class SendTracksResponse(BaseModel):
    success: bool
    message: str
    tracks_sent: int
    timestamp: str


# MCP Tool Endpoint
@app.post("/send-tracks", response_model=SendTracksResponse)
async def send_tracks(request: SendTracksRequest):
    """
    Send discovered music tracks to your music app.
    The AI agent will call this endpoint with a list of tracks.
    """
    try:
        # Prepare data for your music app
        tracks_data = {
            "tracks": [track.dict() for track in request.tracks],
            "source": "ai_music_discovery",
            "timestamp": datetime.utcnow().isoformat()
        }
        
        # Send to your music app API
        async with httpx.AsyncClient(timeout=30.0) as client:
            headers = {}
            if MUSIC_APP_API_KEY:
                headers["Authorization"] = f"Bearer {MUSIC_APP_API_KEY}"
            
            response = await client.post(
                MUSIC_APP_API_URL,
                json=tracks_data,
                headers=headers
            )
            response.raise_for_status()
        
        return SendTracksResponse(
            success=True,
            message=f"Successfully sent {len(request.tracks)} tracks to your music app",
            tracks_sent=len(request.tracks),
            timestamp=datetime.utcnow().isoformat()
        )
    
    except httpx.HTTPError as e:
        raise HTTPException(
            status_code=500,
            detail=f"Failed to send tracks to music app: {str(e)}"
        )
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Unexpected error: {str(e)}"
        )


@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "timestamp": datetime.utcnow().isoformat(),
        "music_app_configured": bool(MUSIC_APP_API_URL)
    }


@app.get("/.well-known/mcp.json")
async def mcp_manifest():
    """
    MCP manifest - tells the agent what tools are available
    """
    return {
        "schema_version": "1.0",
        "name": "music-discovery-server",
        "description": "MCP server for sending discovered music tracks to your music app",
        "tools": [
            {
                "name": "send_tracks_to_app",
                "description": "Send a list of discovered music tracks to the user's music app for playback and storage",
                "input_schema": {
                    "type": "object",
                    "properties": {
                        "tracks": {
                            "type": "array",
                            "description": "List of music tracks to send",
                            "items": {
                                "type": "object",
                                "properties": {
                                    "artist": {"type": "string", "description": "Artist name"},
                                    "title": {"type": "string", "description": "Song title"},
                                    "genre": {"type": "string", "description": "Genre or style tags"},
                                    "source_url": {"type": "string", "description": "Source URL"},
                                    "note": {"type": "string", "description": "Description"}
                                },
                                "required": ["artist", "title"]
                            }
                        }
                    },
                    "required": ["tracks"]
                }
            }
        ]
    }


if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("PORT", 8000))
    uvicorn.run(app, host="0.0.0.0", port=port)