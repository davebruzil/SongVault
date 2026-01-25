# SongVault

**A social music sharing platform for Android** — share tracks from Spotify & YouTube with genre-based discovery.

---

## Team Split

**David** — Auth, Profile, Music APIs, My Posts  
**Dan** — Feed, Post Creation, Post Detail, Genres

---

## David's Scope

### Authentication
- **`LoginFragment`** — email/password form, validation, error states
- **`RegisterFragment`** — new user signup with validation
- **`SplashFragment`** — check auth state, route to login or feed
- **`AuthViewModel`** — login/register/logout logic, auth state LiveData
- **`UserRepository`** — Firebase Auth integration, Firestore sync, Room caching
- **`UserDao`** / **`UserEntity`** — local user storage

### Profile
- **`ProfileFragment`** — display user info, profile pic, stats, logout
- **`EditProfileFragment`** — update username, profile picture, genres
- **`ProfileViewModel`** — load/update profile, handle image upload
- **`ImageRepository`** — download images, cache locally, store paths in Room
- **`ImageCacheDao`** / **`ImageCacheEntity`** — track cached images

### Music APIs
- **`SpotifyApiService`** — OAuth flow, search tracks, get metadata, album art
- **`YouTubeApiService`** — API key auth, video details, thumbnails
- **`LinkParser`** — detect Spotify vs YouTube, extract IDs
- **`MusicApiRepository`** — unified interface, rate limiting, error handling
- **`SearchFragment`** — search bar, results list
- **`SearchViewModel`** — debounced search, loading states

### My Posts
- **`MyPostsFragment`** — grid layout, edit/delete actions
- **`MyPostsViewModel`** — load user posts, handle delete
- **`PostViewModel`** *(partial)* — load/update single post
- **`PostRepository`** *(partial)* — query by userId, delete ops

---

## Dan's Scope

### Feed
- **`FeedFragment`** — RecyclerView, pull-to-refresh, genre chips
- **`PostAdapter`** — ViewHolder for post cards
- **`FeedViewModel`** — AllPosts LiveData, progressive loading (cache → network)
- **`PostRepository`** — Firestore fetch, Room cache, genre queries
- **`PostDao`** / **`PostEntity`** — `getAllPostsWithUsers()`, `getPostsByGenre()`
- **`PostWithUser`** — relation class joining posts with user data

### Post Creation
- **`CreatePostFragment`** — link input, auto-fetch, title/artist fields, genre picker
- **`PostViewModel`** — `createPost()`, `autoFetchMetadata()`, validation
- **`PostRepository`** *(extend)* — save to Room + Firestore, offline queue

### Post Detail
- **`PostDetailFragment`** — cover art, song info, edit/delete buttons
- **`PostDetailViewModel`** — load post + author, check ownership
- **`Safe Args`** — pass postId between screens

### Genres
- **`Genre`** enum — Rock, Metal, Pop, Hip-Hop, Indie, etc.
- **`GenreFilterBottomSheet`** — chip group, "All" option
- **`FeedViewModel`** — `filterByGenre()` logic

### Trending *(optional)*
- **`TrendingFragment`** — API trending songs, badge
- **`TrendingViewModel`** — load from MusicApiRepository

---

## Shared

- Project setup — Retrofit, Room, Navigation, Firebase
- Navigation Graph — all destinations defined
- Room Database — `UserEntity`, `PostEntity`, `ImageCacheEntity`
- **`BaseFragment`** / **`BaseViewModel`** — shared base classes
- Bottom Navigation, Background Sync

---

## Structure

```
app/
├── ui/
│   ├── auth/           # David
│   ├── profile/        # David
│   ├── search/         # David
│   ├── myposts/        # David
│   ├── feed/           # Dan
│   └── post/           # Dan
├── data/
│   ├── local/          # Shared
│   ├── remote/api/     # David
│   └── repository/     # Dan
└── domain/models/      # Shared
```

---

## Git

```
main
└── develop
    ├── feature/david-auth
    ├── feature/david-profile
    ├── feature/david-music-api
    ├── feature/david-my-posts
    ├── feature/dan-feed
    ├── feature/dan-post-creation
    └── feature/dan-genres
```

---

## Handoffs

**Week 2**
- David → Dan: `MusicApiRepository` interface, `ImageRepository` interface
- Dan → David: `PostRepository` interface, Room schema

**Week 4**
- David → Dan: Working Spotify/YouTube APIs, SearchFragment
- Dan → David: Feed with mock data, PostRepository skeleton

**Week 6**
- Integration — merge branches, wire CreatePost to MusicApiRepository, E2E testing

---

## Tests

**David**
- Login/register/logout flows
- Auto-login on restart
- Profile updates (local + remote)
- Spotify/YouTube metadata extraction
- Image caching
- Search functionality
- My Posts CRUD

**Dan**
- Feed displays all posts
- Progressive loading
- Pull-to-refresh
- Genre filtering
- Post creation (manual + auto-fetch)
- Post detail view
- Safe Args navigation
- Offline mode
- Background sync

**Both**
- Full flow: register → search → create → feed
- Auto-fetch integration
- Image caching across screens
- No sync network calls
- Loading indicators
- Error handling

---

## Stack

- **Kotlin** / **MVVM** / **Hilt**
- **Retrofit** / **Room** / **Jetpack Navigation**
- **Firebase Auth** / **Firestore**
- **Spotify API** / **YouTube API**
