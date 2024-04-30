package com.example.clevercards

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.clevercards.ui.theme.CleverCardsTheme
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavType
import androidx.navigation.navArgument
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        val window: Window = this.window
//        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
//        window.statusBarColor = getColor(R.color.teal_200)
        setContent {
            CleverCardsTheme {
                val auth = rememberFirebaseAuth()
                val coroutineScope = rememberCoroutineScope()
                var user by remember { mutableStateOf(auth.currentUser) }

                LaunchedEffect(Unit) {
                    auth.addAuthStateListener { auth ->
                        coroutineScope.launch {
                            user = auth.currentUser
                        }
                    }
                }

                if (user != null) {
                    CleverCardsApp(
                        onLogout = {
                            auth.signOut()
                        }
                    )
                } else {
                    LoginScreen(onLoginSuccess = {})
                }
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleverCardsApp(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userData = remember { mutableStateOf<User?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (currentUser != null) {
            DatabaseHelper.getUser(currentUser.uid) { user ->
                userData.value = user
            }
        }
    }
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Your Account",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Name: ${userData.value?.name}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        FirebaseAuth.getInstance().signOut()
                        onLogout()
                    }
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                title = {
                    when (currentDestination?.route) {
                        "create" -> Text("Create Quiz")
                        "home" -> Text("CleverCards")
                        "practice" -> Text("Practice")
                        else -> Text("CleverCards")
                    }
                },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Default.Person, contentDescription = "More options", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,

                tonalElevation = 8.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    label = { Text("Create") },
                    selected = currentDestination?.hierarchy?.any { it.route == "create" } == true,
                    onClick = {
                        navController.navigate("create") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home") },
                    selected = currentDestination?.hierarchy?.any { it.route == "home" } == true,
                    onClick = {
                        navController.navigate("home") {
//                            popUpTo(navController.graph.findStartDestination().id) {
//                                saveState = true
//                            }
                            popUpTo("home") { inclusive = true }

                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Done, contentDescription = null) },
                    label = { Text("Practice") },
                    selected = currentDestination?.hierarchy?.any { it.route == "practice" || it.route?.startsWith("practice/") == true } == true,
                    onClick = {
                        navController.navigate("practice") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("create") {
                Create(
                    onQuizCompleted = {
                        // Navigate to the home screen after quiz creation is completed
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                ) }
            composable("home") {
                Home(
                    onLogout = onLogout,
                    onQuizClick = { quizId ->
                        // Navigate to the quiz practice screen
                        navController.navigate("practice/$quizId")
                    }
                )
            }

            composable("practice") {
                PracticeListScreen(
                    onQuizClick = { quizId ->
                        navController.navigate("practice/$quizId")
                    }
                )
            }
            composable(
                route = "practice/{quizId}",
                arguments = listOf(navArgument("quizId") { type = NavType.StringType })
            ) { backStackEntry ->
                val quizId = backStackEntry.arguments?.getString("quizId") ?: ""
                PracticeScreen(
                    quizId = quizId,
                    onBack = { navController.popBackStack() }
                )
            }

        }
        }
    }

@Composable
fun PracticeListScreen(onQuizClick: (String) -> Unit) {
    val context = LocalContext.current
    val recentlyPracticedQuizIds = remember { mutableStateListOf<String>() }
    val recentlyPracticedQuizzes = remember { mutableStateListOf<Quiz>() }

    LaunchedEffect(Unit) {
        val quizIds = RecentlyPracticedQuizzes.getRecentlyPracticedQuizIds(context)
        recentlyPracticedQuizIds.addAll(quizIds)

        quizIds.forEach { quizId ->
            DatabaseHelper.getQuiz(quizId) { quiz ->
                if (quiz != null) {
                    DatabaseHelper.getQuizQuestions(quiz.id) { questions ->
                        val updatedQuiz = quiz.copy(questions = questions)
                        recentlyPracticedQuizzes.add(updatedQuiz)
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "Recently Practiced Quizzes",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(recentlyPracticedQuizzes) { quiz ->
                QuizCard(quiz = quiz, onClick = { onQuizClick(quiz.id) }, authorName = null)
            }
        }
    }
}

object RecentlyPracticedQuizzes {
    private const val RECENTLY_PRACTICED_QUIZZES_KEY = "recently_practiced_quizzes"
    private val Context.dataStore by preferencesDataStore(RECENTLY_PRACTICED_QUIZZES_KEY)

    suspend fun saveRecentlyPracticedQuizId(context: Context, quizId: String) {
        context.dataStore.edit { preferences ->
            val currentQuizIds = preferences[stringSetPreferencesKey(RECENTLY_PRACTICED_QUIZZES_KEY)]?.toMutableSet() ?: mutableSetOf()
            currentQuizIds.add(quizId)
            preferences[stringSetPreferencesKey(RECENTLY_PRACTICED_QUIZZES_KEY)] = currentQuizIds
        }
    }

    suspend fun getRecentlyPracticedQuizIds(context: Context): List<String> {
        return context.dataStore.data.map { preferences ->
            preferences[stringSetPreferencesKey(RECENTLY_PRACTICED_QUIZZES_KEY)]?.toList() ?: emptyList()
        }.first()
    }
}

@Composable
fun Home(onLogout: () -> Unit, onQuizClick: (String) -> Unit) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userData = remember { mutableStateOf<User?>(null) }
    val userQuizzes = remember { mutableStateListOf<Quiz>() }
    val publicQuizzes = remember { mutableStateListOf<Quiz>() }
    var showUserQuizzes by remember { mutableStateOf(true) }
    val hasUserQuizzes = userQuizzes.isNotEmpty()


    LaunchedEffect(Unit) {
        if (currentUser != null) {
            DatabaseHelper.getUser(currentUser.uid) { user ->
                userData.value = user
                user?.quizzes?.forEach { quizId ->
                    DatabaseHelper.getQuiz(quizId) { quiz ->
                        if (quiz != null) {
                            DatabaseHelper.getQuizQuestions(quiz.id) { questions ->
                                val updatedQuiz = quiz.copy(questions = questions)
                                userQuizzes.add(updatedQuiz)
                            }
                        }
                    }
                }
            }
            DatabaseHelper.getPublicQuizzes { quizzes ->
                quizzes.forEach { quiz ->
                    DatabaseHelper.getQuizQuestions(quiz.id) { questions ->
                        val updatedQuiz = quiz.copy(questions = questions)
                        publicQuizzes.add(updatedQuiz)
                    }
                }
            }
        }
    }
    val sortedUserQuizzes = userQuizzes.sortedBy { it.title }
    val sortedPublicQuizzes = publicQuizzes.sortedBy { it.title }


    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        TabRow(
            selectedTabIndex = if (showUserQuizzes) 0 else 1,
            modifier = Modifier.padding(16.dp),
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[if (showUserQuizzes) 0 else 1])
                )
            }
        ) {
            Tab(
                selected = showUserQuizzes,
                onClick = { showUserQuizzes = true },
                text = { Text("Your Quizzes") }
            )
            Tab(
                selected = !showUserQuizzes,
                onClick = { showUserQuizzes = false },
                text = { Text("Public Quizzes") }
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.padding(8.dp)
        ) {
        if (showUserQuizzes && !hasUserQuizzes) {
            item(span = { GridItemSpan(2) }) {
                NoQuizzesMessage()
            }
        } else {
            items(if (showUserQuizzes) sortedUserQuizzes else sortedPublicQuizzes) { quiz ->
                val authorName = if (showUserQuizzes) null else getUserName(quiz.authorId)
                QuizCard(quiz = quiz, authorName = authorName, onClick = { onQuizClick(quiz.id) })
            }
            }
        }
    }
}

@Composable
fun NoQuizzesMessage() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Looks like there are no quizzes here. Explore the Public Quizzes section or craft your own!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp).fillMaxWidth()

        )
    }
}


@Composable
fun getUserName(userId: String): String {
    val authorName = remember { mutableStateOf("") }

    LaunchedEffect(userId) {
        DatabaseHelper.getUser(userId) { user ->
            authorName.value = user?.name ?: ""
        }
    }

    return authorName.value
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizCard(quiz: Quiz, authorName: String?, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.padding(8.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (quiz.coverImageUrl.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(quiz.coverImageUrl),
                    contentDescription = "Quiz Cover Image",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.carddefault),
                    contentDescription = "Default Card Image",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = quiz.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = quiz.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "${quiz.questions.size} questions",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (authorName != null) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "By $authorName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun getRandomColor(): Color {
    val colors = listOf(
        Color.Red,
        Color.Blue,
        Color.Green,
        Color.Yellow,
        Color.Magenta,
        Color.Cyan
    )
//    return colors.random()
    return Color.Magenta
}

@Composable
fun Create(
    onQuizCompleted: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "createQuiz") {
        composable("createQuiz") {
            CreateQuizScreen(onQuizCreated = { quizId, quizTitle ->
                navController.navigate("quizDetails/$quizId/$quizTitle")
            })
        }
        composable(
            route = "quizDetails/{quizId}/{quizTitle}",
            arguments = listOf(
                navArgument("quizId") { type = NavType.StringType },
                navArgument("quizTitle") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getString("quizId") ?: ""
            val quizTitle = backStackEntry.arguments?.getString("quizTitle") ?: ""
            QuizDetailsScreen(quizId, quizTitle, onQuizUpdated = {
                navController.navigate("quizQuestions/$quizId")
            })
        }
        composable(
            route = "quizQuestions/{quizId}",
            arguments = listOf(navArgument("quizId") { type = NavType.StringType })
        ) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getString("quizId") ?: ""
            QuizQuestionsScreen(
                quizId = quizId,
                onQuizCompleted = {
                    onQuizCompleted()
                    navController.navigate("createQuiz") {
                        popUpTo("createQuiz") { inclusive = true }
                    }
                }
            )
        }
    }
}