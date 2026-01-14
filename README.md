David: Auth + Profile + Music APIs
Authentication Flow

LoginFragment - Email/password form, validation, error states
RegisterFragment - New user signup with validation
SplashFragment - Check auth state, route to login or feed
AuthViewModel - Login/register/logout logic, auth state LiveData
UserRepository - Firebase Auth integration, Firestore sync, Room caching
UserDao + UserEntity - Local user storage and queries

Profile System

ProfileFragment - Display user info, profile pic, stats, logout
EditProfileFragment - Update username, change profile picture, select genres
ProfileViewModel - Load/update profile, handle image upload
UserRepository (extend) - Update Firestore + Room, handle profile images
ImageRepository - Download images, save to device cache, store paths in Room
ImageCacheDao + ImageCacheEntity - Track cached images, file paths, expiry

Music API Integration

SpotifyApiService - OAuth flow, search tracks, get metadata, extract album art
YouTubeApiService - API key auth, get video details, extract thumbnails
LinkParser - Detect Spotify vs YouTube, extract track/video IDs
MusicApiRepository - Unified API interface, rate limiting, error handling
SearchFragment - Search bar, results list, click to select track
SearchViewModel - Search with debounce, display results, handle loading

My Posts Management

MyPostsFragment - Grid layout, show user's posts, edit/delete actions
MyPostsViewModel - Load user posts, handle delete
PostViewModel (partial) - Load single post, update post
PostRepository (partial) - Query by userId, delete from Room + Firestore, update logic


Dan: Feed + Post Creation + Discovery
Project Foundation (Both)

Android project setup, dependencies (Retrofit, Room, Navigation, Firebase)
Firebase Console setup (Auth, Firestore)
Navigation Graph - All destinations and actions defined
Room Database - UserEntity, PostEntity, ImageCacheEntity schemas
BaseFragment + BaseViewModel - Shared base classes

Feed System

FeedFragment - RecyclerView feed, pull-to-refresh, genre chips
PostAdapter - ViewHolder for post cards, click handling
FeedViewModel - AllPosts LiveData, progressive loading (cache → network)
PostRepository - Fetch Firestore, cache in Room, query by genre
PostDao + PostEntity - getAllPostsWithUsers(), getPostsByGenre()
PostWithUser - Relation class joining posts with user data

Post Creation

CreatePostFragment - Link input, auto-fetch button, title/artist fields, genre picker, description, image preview
PostViewModel - createPost(), autoFetchMetadata(), field validation
PostRepository (extend) - Save to Room + Firestore, offline queue, download cover images
Integration with MusicApiRepository (David's code) for auto-fetch
Integration with ImageRepository (David's code) for caching

Post Detail

PostDetailFragment - Large cover art, song info, description, edit/delete buttons
PostDetailViewModel - Load post + author, check ownership
Safe Args - Pass postId from Feed → Detail → Edit

Genre System

Genre enum - Hardcoded list (Rock, Metal, Pop, Hip-Hop, Indie, etc.)
GenreFilterBottomSheet - Chip group, "All" option
FeedViewModel (extend) - filterByGenre() logic

Trending (Optional)

TrendingFragment - Display API trending songs, "Trending" badge
TrendingViewModel - Load trending from MusicApiRepository (David's code)

Infrastructure

Bottom Navigation - Wire up Feed, Search, MyPosts, Profile tabs
Background Sync - Queue unsynced posts, retry on network restore
Testing - Progressive loading, post creation, genre filter, API integration


Shared Responsibilities
Both David & Dan

Daily 5-min sync - Progress check, blockers, handoffs
Code reviews - Review each other's PRs before merge
Integration testing - Test complete flows together
Bug fixes - Pair on critical issues
Git hygiene - Clean commits, meaningful messages, feature branches
Documentation - README, setup instructions, API keys guide


Integration Points
Week 2 Handoff
David → Dan: MusicApiRepository interface, ImageRepository interface
Dan → David: PostRepository interface, Room schema finalized
Week 4 Handoff
David → Dan: Working Spotify/YouTube APIs, SearchFragment ready
Dan → David: Feed with mock data, PostRepository skeleton
Week 6 Integration
Both: Merge branches, wire CreatePost to MusicApiRepository, test end-to-end

Git Strategy
main
  └── develop
      ├── feature/david-auth
      ├── feature/david-profile  
      ├── feature/david-music-api
      ├── feature/david-my-posts
      ├── feature/dan-feed
      ├── feature/dan-post-creation
      └── feature/dan-genres
Ownership:
David: ui/auth/, ui/profile/, ui/search/, ui/myposts/, data/remote/api/
Dan: ui/feed/, ui/post/, data/repository/PostRepository.kt
Shared: data/local/ (coordinate), domain/models/

Testing Split
David Tests

Login/register/logout flows
Auto-login on restart
Profile updates (local + remote)
Spotify API metadata extraction
YouTube API metadata extraction
Image caching (download → save → retrieve)
Search functionality
My Posts screen
Edit/delete own posts

Dan Tests

Feed displays all posts
Progressive loading (cache → network)
Pull-to-refresh
Genre filtering
Post creation (manual input)
Post creation (auto-fetch via David's API)
Post detail view
Navigation with Safe Args
Offline mode
Background sync

Both Test Together

Complete user flow (register → search → create → view feed)
Auto-fetch integration
Image caching across screens
No synchronous network calls
All spinners working
Error handling
Claude is AI and can make mistakes. Please double-check responses.
