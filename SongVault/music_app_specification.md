# Music Genre Sharing Platform - Mobile App
## Project Specification Document

---

## 1. Application Goal Definition

**Primary Purpose:**
A social music discovery app where users share songs within their favorite music genres and discover new music from people with similar taste. Users can post songs (title, artist, link, description) and explore content shared by other users in their preferred genres.

**Target Users:**
- Music enthusiasts aged 16+
- People looking to discover new songs in specific genres
- Music communities (metal fans, lo-fi listeners, indie rock fans, etc.)

**Core Value Proposition:**
Find music recommendations from real people who are obsessed with the same genres you love.

---

## 2. Functional Requirements

### Must Have (Threshold Requirements)

#### 2.1 User Authentication & Profile Management
- **User Registration**: Users can create account with email/password
- **User Login**: Persistent login with automatic recognition on app restart
- **Firebase Authentication**: Backend authentication using Firebase Auth
- **User Logout**: Ability to sign out of the application
- **User Profile Screen**: Display user information (name, profile picture)
- **Edit Profile**: Update name and profile picture
- **Profile Picture Storage**: Store locally on device (not Firebase Storage for images)

#### 2.2 Content Sharing (Threshold)
- **Post Creation**: Users can share songs by:
  - Pasting YouTube link OR Spotify link
  - Entering song title (auto-filled from metadata if possible)
  - Entering artist name (auto-filled from metadata if possible)
  - Adding description/review (optional)
  - Selecting genre tag
  - **Automatic Image Fetching**: System automatically fetches and displays:
    - Spotify: Album art from Spotify metadata
    - YouTube: Thumbnail from YouTube video
- **Image Auto-Fetch & Cache**: 
  - Detect link type (YouTube vs Spotify)
  - Call appropriate API to fetch image
  - Save image locally to device cache
  - Display on post card
- **Post Visibility**: Users can view posts shared by other users
- **User-Specific Posts**: Dedicated screen showing only posts created by the logged-in user
- **Edit Posts**: Users can update their own posts (description and music link)
- **Delete Posts**: Users can delete their own posts
- **Image Handling**: Store image file paths in Room database, actual images in device cache

#### 2.3 Remote API Integration (Threshold)
- **Fetch External Music Data**: Integration with music API
  - Option: Spotify API, YouTube Music API, or Genius API
  - Display trending/recommended songs based on genre
  - Show recommendations from external source (separate from user posts)
- **No Rate Limiting Issues**: Implement caching to avoid excessive API calls

#### 2.4 Content Display & Discovery
- **Feed Screen**: Scrollable feed showing all user posts
- **Pagination/Progressive Loading**: Implement lazy loading (not page-based)
- **Filter by Genre**: View posts filtered by music genre
- **Search Functionality**: Search posts by song title, artist, or description
- **Trending Section**: Fetch and display trending songs from external API
- **Post Details**: Click post to see full details

#### 2.5 User Identification & Management
- **User Differentiation**: App identifies different users and tracks their posts
- **Post Attribution**: Every post shows which user created it
- **User-Only Edit/Delete**: Users can only edit/delete their own posts

### Should Have (Nice to Have)

- **Comments**: Users can comment on posts from other users
- **Like/Heart Posts**: Users can like posts, with count displayed
- **User Profiles**: View other users' profile and their posts
- **Genre Communities**: Filter by genre to find communities
- **Follow Users**: Follow other users with similar music taste
- **Search by Mood/Vibe**: Smart search (using NLP or tags)

### Future Improvements (Nice to Have)

- **Real-time Chat**: Socket.io chat between users (learning requirement for large groups)
- **Notifications**: Notify users when someone comments/likes their post
- **Recommendations Algorithm**: ML-based song recommendations
- **Music Playlist Creation**: Create playlists from shared songs
- **Social Features**: Share to other platforms

---

## 3. Technical Architecture

### Frontend (Mobile - Android)

**Framework:**
- Kotlin/Java with Android SDK
- Jetpack Libraries (mandatory):
  - MVVM Architecture (ViewModel + LiveData)
  - Navigation Component (Graph Nav + SafeArgs)
  - Room Database (local caching)
  - Data Binding

**UI Framework:**
- Material Design 3
- RecyclerView for lists
- Fragments (no Activities for screen navigation)
- Spinners for async operations

**Libraries:**
- Picasso (image loading and caching)
- Retrofit (REST API calls)
- OkHttp (interceptors for caching)

### Backend (Node.js - Web/Mobile shared)

**Framework:**
- Node.js + Express.js (TypeScript)
- RESTful API

**Authentication:**
- Firebase Authentication (for both web and mobile)
- JWT tokens (optional secondary auth)

**Database:**
- Firebase Realtime Database or Firestore (post data)
- Local SQLite via Room (mobile)

**External APIs:**
- Music API (Spotify, YouTube Music, or Genius)

### Local Storage (Mobile - Room Database)

**Tables:**
- `users` - cached user data
- `posts` - cached posts
- `images` - cached image data (as BLOBs or file paths)
- `genres` - available genres

**Caching Strategy:**
- Cache posts when fetched from server
- Cache images locally with file paths in Room
- Implement offline-first approach

---

## 4. App Structure & Navigation

### Navigation Graph (MVVM + Graph Nav)

```
SplashScreen
  ↓
AuthActivity (Fragment-based)
  ├── LoginFragment
  ├── RegisterFragment
  └── ForgotPasswordFragment
  ↓
MainActivity (Fragment-based)
  ├── FeedFragment (Home)
  │   ├── PostDetailFragment
  │   └── GenreFilterFragment
  ├── TrendingFragment
  ├── MyPostsFragment
  │   ├── EditPostFragment
  │   └── CreatePostFragment
  ├── SearchFragment
  ├── ProfileFragment
  │   └── EditProfileFragment
  └── SettingsFragment
```

### Data Flow (MVVM)

```
Fragment
  ↓
ViewModel (LiveData)
  ↓
Repository (abstracts data sources)
  ├── Local (Room DAO)
  └── Remote (Retrofit API)
  ↓
Database (Room) / API (Retrofit)
```

---

## 5. Screen Specifications

### 5.1 Authentication Screens

**Login Screen**
- Email input
- Password input
- Login button
- Register link
- Firebase Auth integration
- Error handling & validation

**Register Screen**
- Email input
- Password input
- Confirm password
- Register button
- Terms & conditions checkbox
- Firebase Auth integration

**Profile Setup Screen** (after registration)
- Name input
- Profile picture upload
- Preferred genres (multi-select)
- Done button

### 5.2 Main App Screens

**Feed Screen (Home)**
- List of all posts with pagination
- Post card shows:
  - Song title & artist
  - Cover image
  - Description/review
  - Genre tag
  - Posted by (username)
  - Posted date
  - Like count
- Pull-to-refresh
- Search bar at top
- Genre filter button
- Trending tab

**My Posts Screen**
- List of user's own posts
- Edit button per post
- Delete button per post
- Create new post button (floating action button)
- Empty state if no posts

**Create/Edit Post Screen**
- Song title input
- Artist name input
- Music link input
- Genre dropdown (single select)
- Description textarea
- Image picker (from device)
- Submit button
- Cancel button

**Profile Screen**
- Profile picture (larger)
- Username
- Edit profile button
- List of user's posts
- Logout button
- Account settings

**Edit Profile Screen**
- Edit name
- Edit profile picture (camera/gallery)
- Save changes button

**Trending Screen**
- Fetch from external music API
- Display trending/recommended songs
- Show source (Spotify/YouTube/etc.)
- Genre-based trending options

**Search Screen**
- Search query input
- Search by song title
- Search by artist
- Search by description
- Filter by genre
- Results list

---

## 6. Data Models

### User
```
{
  id: String (Firebase UID)
  email: String
  username: String
  profilePicture: String (file path)
  createdAt: Long
  updatedAt: Long
}
```

### Post
```
{
  id: String (unique)
  userId: String (who posted)
  songTitle: String
  artistName: String
  musicLink: String (YouTube or Spotify URL)
  linkType: String (YOUTUBE or SPOTIFY)
  description: String (optional review)
  genre: String
  coverImage: String (file path to auto-fetched image)
  imageUrl: String (original image URL from API)
  likeCount: Int
  createdAt: Long
  updatedAt: Long
}
```

### Genre
```
{
  id: String
  name: String (e.g., "Metal", "Lo-Fi", "Indie Rock")
}
```

### Comment (Nice to Have)
```
{
  id: String
  postId: String
  userId: String
  text: String
  createdAt: Long
}
```

---

## 7. API Endpoints

### Authentication
- `POST /auth/register` - Register new user
- `POST /auth/login` - Login user
- `POST /auth/logout` - Logout user
- `GET /auth/me` - Get current user info

### Posts
- `GET /posts` - Get all posts with pagination
- `GET /posts?genre=metal` - Get posts by genre
- `GET /posts/my` - Get current user's posts
- `POST /posts` - Create new post
- `PUT /posts/:id` - Update post
- `DELETE /posts/:id` - Delete post
- `GET /posts/:id` - Get post details
- `POST /posts/:id/like` - Like post

### External Music APIs

**Spotify Web API Integration:**
- Endpoint: `https://api.spotify.com/v1/tracks/{id}`
- Authentication: Bearer token (get from Spotify Developer Dashboard)
- Use case: Extract metadata (title, artist, album art) from Spotify links
- Image: High-quality album art (640x640px)
- Rate limit: 429 responses, implement exponential backoff

**YouTube Data API Integration:**
- Endpoint: `https://www.googleapis.com/youtube/v3/videos`
- Authentication: API key (get from Google Cloud Console)
- Parameters: `?id={videoId}&part=snippet`
- Use case: Extract metadata (title, thumbnail) from YouTube links
- Image: High thumbnail (480x360px)
- Rate limit: 10,000 units/day, implement caching

**Setup Instructions:**
1. Spotify Developer Account:
   - Register at https://developer.spotify.com
   - Create app
   - Get Client ID & Client Secret
   - Use Client Credentials OAuth flow

2. YouTube Data API:
   - Enable in Google Cloud Console
   - Create API key
   - Store in BuildConfig (don't hardcode)

3. Store API keys in:
   - `local.properties` (git ignored)
   - BuildConfig fields
   - Never commit to git

---

## 8. Implementation Details

### Async Operations
- **No synchronous network calls** - All API calls must be asynchronous
- **Spinners/Progress Indicators**: Show loading state during API calls
- **Error Handling**: Display user-friendly error messages
- **Retrofit + Coroutines** for async API calls

### Caching Strategy
- **Room Database**: Cache posts and user data
- **Image Caching**: Store image file paths in Room, actual images in device cache
- **Picasso**: Automatic image caching
- **Offline Support**: Show cached data if network unavailable

### Authentication Flow
1. User registers via Firebase Auth
2. Firebase returns user UID
3. App creates user profile in backend database
4. Store auth token locally (SharedPreferences)
5. On app restart, check if token exists
6. If token valid, auto-login; if not, show login screen
7. On logout, clear token and data

### Image Handling
- Pick image from device gallery or camera
- Compress image before saving
- Store file path in Room database
- Store actual image in app's cache directory
- Use Picasso to load and display images
- **NOT** uploading to Firebase Storage

### Link Handling & Automatic Image Fetching

**Spotify Link Processing:**
1. User pastes Spotify link: `https://open.spotify.com/track/[TRACK_ID]`
2. Extract track ID from URL
3. Call Spotify Web API: `GET /v1/tracks/{id}`
4. Response includes:
   - `name` (song title)
   - `artists[0].name` (artist name)
   - `album.images[0].url` (cover image URL - 640x640px)
5. Auto-fill song title and artist
6. Fetch image from URL
7. Save image locally to cache directory
8. Store image file path in Room database
9. Display on post

**YouTube Link Processing:**
1. User pastes YouTube link: `https://www.youtube.com/watch?v=[VIDEO_ID]`
2. Extract video ID from URL (handle youtube.com and youtu.be formats)
3. Call YouTube Data API: `GET /youtube/v3/videos?id={id}&part=snippet`
4. Response includes:
   - `snippet.title` (video title - parse for song info)
   - `snippet.thumbnails.high.url` (thumbnail URL - 480x360px)
5. Auto-fill based on title parsing
6. Fetch thumbnail image from URL
7. Save image locally to cache directory
8. Store image file path in Room database
9. Display on post

**Error Handling:**
- If image fetch fails: Show placeholder/default image
- If link invalid: Show error message, allow user to enter manually
- If API call fails: Cache data locally, display with generic image
- Network timeout: Show retry option

**Network Optimization:**
- Fetch images asynchronously (don't block UI)
- Cache fetched images so they're not re-downloaded
- Compress images before saving to cache
- Set image timeout (5 seconds) to prevent hanging

---

## 9. Design Requirements

**Mandatory:**
- Follow Material Design 3 guidelines
- Consistent color scheme (recommend: dark mode + vibrant accent for music theme)
- Use Google's Design System
- Proper spacing and typography
- Responsive layouts for different screen sizes
- Custom app icon
- Splash screen

**Recommended Color Scheme (Music App Themed):**
- Primary: Dark purple/indigo
- Secondary: Vibrant accent (neon green, cyan, or orange)
- Background: Dark (dark gray/black)
- Text: White/light gray

---

## 10. Code Structure

### Directory Layout
```
app/
├── data/
│   ├── local/
│   │   ├── database/
│   │   │   ├── AppDatabase.kt
│   │   │   ├── dao/
│   │   │   │   ├── UserDao.kt
│   │   │   │   ├── PostDao.kt
│   │   │   │   └── GenreDao.kt
│   │   │   └── entity/
│   │   │       ├── UserEntity.kt
│   │   │       ├── PostEntity.kt
│   │   │       └── GenreEntity.kt
│   │   └── SharedPreferences.kt
│   ├── remote/
│   │   ├── api/
│   │   │   ├── PostApi.kt
│   │   │   ├── UserApi.kt
│   │   │   ├── SpotifyApi.kt
│   │   │   └── YouTubeApi.kt
│   │   └── dto/
│   │       ├── PostDto.kt
│   │       ├── SpotifyTrackDto.kt
│   │       └── YouTubeVideoDto.kt
│   └── repository/
│       ├── PostRepository.kt
│       ├── UserRepository.kt
│       └── MusicMetadataRepository.kt
├── ui/
│   ├── fragments/
│   │   ├── auth/
│   │   │   ├── LoginFragment.kt
│   │   │   └── RegisterFragment.kt
│   │   ├── feed/
│   │   │   ├── FeedFragment.kt
│   │   │   ├── PostDetailFragment.kt
│   │   │   └── CreatePostFragment.kt
│   │   ├── profile/
│   │   │   ├── ProfileFragment.kt
│   │   │   └── EditProfileFragment.kt
│   │   └── trending/
│   │       └── TrendingFragment.kt
│   ├── viewmodel/
│   │   ├── AuthViewModel.kt
│   │   ├── PostViewModel.kt
│   │   ├── UserViewModel.kt
│   │   └── TrendingViewModel.kt
│   ├── adapter/
│   │   ├── PostAdapter.kt
│   │   └── GenreAdapter.kt
│   └── activity/
│       ├── AuthActivity.kt
│       └── MainActivity.kt
├── navigation/
│   └── nav_graph.xml
├── utils/
│   ├── Constants.kt
│   ├── Extensions.kt
│   ├── ImageUtils.kt
│   ├── LinkParser.kt
│   ├── MusicMetadataFetcher.kt
│   └── ImageDownloader.kt
└── AndroidManifest.xml
```

### Utility Functions (Code Examples)

**LinkParser.kt** - Detect and parse music links
```kotlin
object LinkParser {
    enum class LinkType {
        SPOTIFY, YOUTUBE, INVALID
    }
    
    fun getLinkType(url: String): LinkType {
        return when {
            url.contains("spotify.com") -> LinkType.SPOTIFY
            url.contains("youtube.com") || url.contains("youtu.be") -> LinkType.YOUTUBE
            else -> LinkType.INVALID
        }
    }
    
    fun extractSpotifyTrackId(url: String): String? {
        // Extract from: https://open.spotify.com/track/[ID]
        val regex = """spotify\.com/track/([a-zA-Z0-9]+)""".toRegex()
        return regex.find(url)?.groupValues?.get(1)
    }
    
    fun extractYouTubeVideoId(url: String): String? {
        // Extract from: youtube.com/watch?v=[ID] or youtu.be/[ID]
        val regexLong = """youtube\.com/watch\?v=([a-zA-Z0-9_-]+)""".toRegex()
        val regexShort = """youtu\.be/([a-zA-Z0-9_-]+)""".toRegex()
        
        return regexLong.find(url)?.groupValues?.get(1)
            ?: regexShort.find(url)?.groupValues?.get(1)
    }
}
```

**MusicMetadataFetcher.kt** - Fetch metadata from APIs
```kotlin
class MusicMetadataFetcher(
    private val spotifyApi: SpotifyApi,
    private val youtubeApi: YouTubeApi,
    private val imageDownloader: ImageDownloader
) {
    
    suspend fun fetchMetadata(url: String): MusicMetadata? {
        return try {
            val linkType = LinkParser.getLinkType(url)
            when (linkType) {
                LinkParser.LinkType.SPOTIFY -> fetchSpotifyMetadata(url)
                LinkParser.LinkType.YOUTUBE -> fetchYouTubeMetadata(url)
                LinkParser.LinkType.INVALID -> null
            }
        } catch (e: Exception) {
            Log.e("MusicMetadataFetcher", "Error fetching metadata", e)
            null
        }
    }
    
    private suspend fun fetchSpotifyMetadata(url: String): MusicMetadata? {
        val trackId = LinkParser.extractSpotifyTrackId(url) ?: return null
        val response = spotifyApi.getTrack(trackId)
        
        val imageUrl = response.album.images.firstOrNull()?.url ?: return null
        val localImagePath = imageDownloader.downloadAndSave(imageUrl)
        
        return MusicMetadata(
            title = response.name,
            artist = response.artists.firstOrNull()?.name ?: "Unknown",
            imageUrl = imageUrl,
            localImagePath = localImagePath,
            linkType = "SPOTIFY"
        )
    }
    
    private suspend fun fetchYouTubeMetadata(url: String): MusicMetadata? {
        val videoId = LinkParser.extractYouTubeVideoId(url) ?: return null
        val response = youtubeApi.getVideoMetadata(videoId)
        
        val imageUrl = response.snippet.thumbnails.high.url
        val localImagePath = imageDownloader.downloadAndSave(imageUrl)
        
        // Parse title to extract song info (basic parsing)
        val title = response.snippet.title
        
        return MusicMetadata(
            title = title,
            artist = "YouTube", // Could parse further
            imageUrl = imageUrl,
            localImagePath = localImagePath,
            linkType = "YOUTUBE"
        )
    }
}

data class MusicMetadata(
    val title: String,
    val artist: String,
    val imageUrl: String,
    val localImagePath: String,
    val linkType: String
)
```

**ImageDownloader.kt** - Download and cache images
```kotlin
class ImageDownloader(private val context: Context) {
    
    suspend fun downloadAndSave(imageUrl: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val cacheDir = context.cacheDir
                val fileName = imageUrl.hashCode().toString() + ".jpg"
                val cacheFile = File(cacheDir, fileName)
                
                // If already cached, return path
                if (cacheFile.exists()) {
                    return@withContext cacheFile.absolutePath
                }
                
                // Download image
                val url = URL(imageUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                
                val inputStream = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                connection.disconnect()
                
                // Compress and save
                FileOutputStream(cacheFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                
                cacheFile.absolutePath
            } catch (e: Exception) {
                Log.e("ImageDownloader", "Error downloading image", e)
                null
            }
        }
    }
}
```

**ViewModel Integration:**
```kotlin
class PostViewModel(
    private val postRepository: PostRepository,
    private val musicMetadataFetcher: MusicMetadataFetcher
) : ViewModel() {
    
    private val _createPostState = MutableLiveData<CreatePostState>()
    val createPostState: LiveData<CreatePostState> = _createPostState
    
    fun parseAndFetchMetadata(musicLink: String) {
        viewModelScope.launch {
            _createPostState.value = CreatePostState.Loading
            
            val metadata = musicMetadataFetcher.fetchMetadata(musicLink)
            
            if (metadata != null) {
                _createPostState.value = CreatePostState.MetadataFetched(metadata)
            } else {
                _createPostState.value = CreatePostState.Error("Could not fetch metadata")
            }
        }
    }
    
    fun createPost(
        link: String,
        title: String,
        artist: String,
        genre: String,
        description: String,
        imagePath: String
    ) {
        viewModelScope.launch {
            _createPostState.value = CreatePostState.Creating
            
            val success = postRepository.createPost(
                musicLink = link,
                title = title,
                artist = artist,
                genre = genre,
                description = description,
                imagePath = imagePath
            )
            
            if (success) {
                _createPostState.value = CreatePostState.Success
            } else {
                _createPostState.value = CreatePostState.Error("Failed to create post")
            }
        }
    }
}

sealed class CreatePostState {
    object Loading : CreatePostState()
    data class MetadataFetched(val metadata: MusicMetadata) : CreatePostState()
    object Creating : CreatePostState()
    object Success : CreatePostState()
    data class Error(val message: String) : CreatePostState()
}
```

### Code Quality Guidelines
- Use MVVM architecture strictly
- Short, focused functions (max 20 lines)
- No code duplication (extract to utilities)
- Proper naming conventions (camelCase for variables/functions, PascalCase for classes)
- Google's Kotlin style guide
- Comments for complex logic only
- Proper error handling and logging

---

## 11. Testing Requirements

**Unit Tests (ViewModel & Repository):**
- Test ViewModel functions
- Test Repository data flow
- Test API error handling
- Minimum 70% code coverage

**UI Tests (Optional):**
- Test Fragment navigation
- Test user interactions

---

## 12. Git & Version Control

**Mandatory:**
- Use Git from project start (not just at the end)
- Branch-based development (feature branches)
- Meaningful commit messages
- Pull requests for code review (even solo)
- Proper `.gitignore` file
- README with setup instructions

**Branching Strategy:**
```
main (production)
  └── develop (development)
      ├── feature/auth
      ├── feature/posts
      ├── feature/profile
      └── feature/trending
```

**Commit Message Format:**
```
[FEATURE] Add login screen
[FIX] Fix image caching issue
[REFACTOR] Extract image loading logic
[DOCS] Update README
```

---

## 13. Deliverables

### Phase 1: Project Definition ✅
- [ ] This specification document
- [ ] Use case document
- [ ] Wireframes/Mockups (low-fidelity)
- [ ] Data model diagram

### Phase 2: Application Design
- [ ] Use case stories (detailed user journeys)
- [ ] High-fidelity mockups (Figma/Adobe XD)
- [ ] Navigation graph diagram
- [ ] MVVM architecture diagram

### Phase 3: Working Application
- [ ] Fully functional Android app
- [ ] All must-have features implemented
- [ ] Clean, modular code following MVVM
- [ ] Proper error handling and loading states
- [ ] Local database (Room) integration
- [ ] Firebase Auth integration
- [ ] External API integration
- [ ] Git repository with proper history
- [ ] Unit tests (minimum 70% coverage)
- [ ] README with setup instructions
- [ ] Signed APK ready for testing

---

## 14. Timeline Estimate

| Phase | Duration | Deliverables |
|-------|----------|--------------|
| Design & Setup | 1 week | Spec, mockups, project setup |
| Auth & UI | 1.5 weeks | Login/Register, navigation, fragments |
| Posts & Database | 2 weeks | Create/read/update/delete posts, Room DB |
| Images & Caching | 1 week | Image handling, Picasso integration |
| External API | 1 week | Trending, music API integration |
| Profile & Refinement | 1 week | Profile management, polish UI |
| Testing & Optimization | 1 week | Unit tests, bug fixes, optimization |

**Total: ~8-9 weeks**

---

## 15. Success Criteria

✅ All threshold requirements implemented (auth, sharing, external API)
✅ MVVM architecture followed strictly
✅ No synchronous network calls
✅ Proper image and data caching
✅ Firebase Auth working
✅ All 4 screens functional and polished
✅ Code is clean, modular, and well-organized
✅ Minimum 70% test coverage
✅ Git history shows proper development process
✅ Material Design 3 compliance
✅ No crashes or major bugs

---

## 16. Notes

- **Do NOT** create Instagram clone - this is a music-specific discovery app
- **Do NOT** use Firebase Storage for images - store locally only
- **Do NOT** make synchronous API calls
- **Do NOT** put all code in one file or activity
- **DO** use all learned Jetpack components (MVVM, Room, Navigation, LiveData)
- **DO** follow Google's Material Design system
- **DO** implement proper error handling
- **DO** cache data intelligently
- **DO** write clean, testable code
