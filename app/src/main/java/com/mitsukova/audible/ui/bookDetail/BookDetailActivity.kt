package com.mitsukova.audible.ui.bookDetail

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.ActionMode
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.SearchView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mitsukova.audible.R
import com.mitsukova.audible.data.database.AudibleDatabase
import com.mitsukova.audible.data.database.BookDao
import com.mitsukova.audible.data.database.BookSettingsDao
import com.mitsukova.audible.data.database.NotesDao
import com.mitsukova.audible.data.model.NotesEntity
import com.mitsukova.audible.data.repository.bookDetail.BookDetailRepositoryImpl
import com.mitsukova.audible.data.repository.bookDetail.WebViewListener
import com.mitsukova.audible.ui.bookSettings.SettingsDialogFragment
import com.mitsukova.audible.ui.notes.NotesActivity
import com.mitsukova.audible.ui.toc.TableOfContentsActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URLEncoder

class BookDetailActivity : AppCompatActivity(), WebViewListener {

    private val IAM_TOKEN: String =
        "t1.9euelZqUicuTnZfGmZqTmZmKlpnLje3rnpWai4yano7Li8rMz52SxsmVy5Ll9PdTCC9M-e87djPQ3fT3EzcsTPnvO3Yz0M3n9euelZqZmI6TyInJmpyNz46Omp6Lk-_8xeuelZqZmI6TyInJmpyNz46Omp6Lkw.UG5Pk0RwjZRENmnZp2wVeyicsVnWTWJ9K-ShgU1UzasIsZf5qdNqLoJHrKO_b2mCzhViQ1H3Wr2HB_r-wBtpBA"
    private lateinit var viewPager: ViewPager
    private var webView: WebView? = null
    private lateinit var pageAdapter: PageAdapter

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var bottomSheet: ViewGroup

    private lateinit var audioBottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var audioBottomSheet: ViewGroup

    private lateinit var audioSettingsBottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var audioSettingsBottomSheet: ViewGroup

    private lateinit var mainBottomSheetContent: View

    private var selectedVoice: String = "alena"
    private var selectedSpeed: String = "1.0"

    private var currentPage: Int = 0

    private var filePath: String? = null

    private var isAudioSheetVisible = false
    private var isAudioSettingsSheetVisible = false

    private lateinit var seekBar: SeekBar
    private lateinit var settingsButton: ImageButton
    private lateinit var audioButton: ImageButton
    private lateinit var chapterNameTextView: TextView

    private lateinit var gestureDetector: GestureDetector

    private lateinit var settingsDao: BookSettingsDao
    private lateinit var bookDao: BookDao
    private lateinit var notesDao: NotesDao

    private lateinit var bookDetailViewModel: BookDetailViewModel

    private var webViewText: String = ""
    private var currentSelectedText: String = ""
    private var currentWebView: WebView? = null

    private lateinit var mediaPlayer: MediaPlayer

    private var isAudioOn = false

    private lateinit var stopAudioButton: ImageButton
    private lateinit var audioProgressSeekBar: SeekBar
    private lateinit var playPauseButton: ImageButton
    private lateinit var forward15Button: ImageButton
    private lateinit var rewind15Button: ImageButton
    private lateinit var nextChapterButton: ImageButton
    private lateinit var previousChapterButton: ImageButton
    private lateinit var settingsAudioButton: ImageButton
    private lateinit var backButton: ImageButton
    private lateinit var voiceSpinner: Spinner
    private lateinit var speedSpinner: Spinner

    companion object {
        private const val REQUEST_CODE_TOC = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AudibleDatabase.getDatabase(this)
        settingsDao = db.bookSettingsDao()
        bookDao = db.bookDao()
        notesDao = db.notesDao()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        setContentView(R.layout.activity_book_detail)

        val receivedIntent = intent
        filePath = receivedIntent.getStringExtra("PATH")

        viewPager = findViewById(R.id.viewPager)
        viewPager.setOffscreenPageLimit(1)
        seekBar = findViewById(R.id.menuSeekBar)
        audioProgressSeekBar = findViewById(R.id.audioProgressSeekBar)
        chapterNameTextView = findViewById(R.id.chapterNameTextView)
        webView = findViewById(R.id.webView)

        playPauseButton = findViewById(R.id.playPauseButton)
        forward15Button = findViewById(R.id.forward15Button)
        rewind15Button = findViewById(R.id.rewind15Button)
        nextChapterButton = findViewById(R.id.nextChapterButton)
        previousChapterButton = findViewById(R.id.prevChapterButton)
        stopAudioButton = findViewById(R.id.stopAudioButton)
        settingsAudioButton = findViewById(R.id.settingsAudioButton)
        settingsButton = findViewById(R.id.settingsButton)
        audioButton = findViewById(R.id.audioButton)
        backButton = findViewById(R.id.backButton)
        voiceSpinner = findViewById(R.id.voiceSpinner)
        speedSpinner = findViewById(R.id.speedSpinner)
        bottomSheet = findViewById(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.isHideable = true

        mainBottomSheetContent =
            LayoutInflater.from(this).inflate(R.layout.activity_book_detail, null)

        audioBottomSheet = findViewById(R.id.audioBottomSheet)
        audioSettingsBottomSheet = findViewById(R.id.audioSettingsSheet)

        audioBottomSheetBehavior = BottomSheetBehavior.from(audioBottomSheet)
        audioSettingsBottomSheetBehavior = BottomSheetBehavior.from(audioSettingsBottomSheet)
        audioSettingsBottomSheet.visibility = View.GONE
        audioBottomSheet.visibility = View.GONE

        audioBottomSheetBehavior.isHideable = true
        audioSettingsBottomSheetBehavior.isHideable = true

        val voiceMapping = mapOf(
            "Алёна" to "alena",
            "Филипп" to "filipp",
            "Эрмиль" to "ermil",
            "Джейн" to "jane",
            "Мадирус" to "madirus",
            "Омаж" to "omazh",
            "Захар" to "zahar",
            "Марина" to "marina"
        )

        val voiceOptions = voiceMapping.keys.toTypedArray()
        val voiceAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, voiceOptions)
        voiceSpinner.adapter = voiceAdapter

        val speedOptions = arrayOf("0.25", "0.5", "0.75", "1", "1.25", "1.5", "2")
        val speedAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, speedOptions)
        speedSpinner.adapter = speedAdapter

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                toggleBottomSheet(bottomSheetBehavior)
                toggleBottomSheet(audioBottomSheetBehavior)
                return true
            }
        })

        pageAdapter = PageAdapter(
            emptyList(),
            viewPager,
            null,
            this
        ) // Инициализируем pageAdapter с пустым списком
        viewPager.adapter = pageAdapter

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                setCurrentPage(progress - 1)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        audioProgressSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                setCurrentPage(progress - 1)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        settingsButton.setOnClickListener {
            val fragment = SettingsDialogFragment()

            val bundle = Bundle()
            bundle.putInt("ViewPager_ID", viewPager.id)
            fragment.arguments = bundle

            fragment.show(supportFragmentManager, "SettingsDialog")
        }

        val bookDetailRepository = BookDetailRepositoryImpl(AudibleDatabase.getDatabase(this))
        val factory = BookDetailViewModelFactory(bookDetailRepository)
        bookDetailViewModel = ViewModelProvider(this, factory).get(BookDetailViewModel::class.java)

        bookDetailViewModel.loadBook(filePath!!)

        bookDetailViewModel.pages.observe(this) { pages ->
            if (pages != null && pages.isNotEmpty()) {
                bookDetailViewModel.bookSettingsEntity.observe(this) { settings ->
                    if (settings != null) {
                        pageAdapter = PageAdapter(pages, viewPager, settings, this)
                        viewPager.adapter = pageAdapter

                        val pageCount = pages.size
                        seekBar.max = pageCount - 1 // Устанавливаем максимальное значение в количество страниц
                        audioProgressSeekBar.max = pageCount - 1 // Аналогично
                    }

                    bookDetailViewModel.setTotalPages(pageAdapter.count)
                    setupViewPager()
                }
            }
        }

        lifecycleScope.launch {
            bookDetailViewModel.getAudioSettings()
        }

        bookDetailViewModel.audioSettings.observe(this) { audioSettings ->
            if (audioSettings != null && isAudioOn) {
                selectedVoice = audioSettings.voice
                selectedSpeed = audioSettings.speed

                voiceSpinner.setSelection(voiceOptions.indexOf(selectedVoice))
                speedSpinner.setSelection(speedOptions.indexOf(selectedSpeed))
            }
        }

        if (filePath != null) {
            CoroutineScope(Dispatchers.Main).launch {
                val savedChapter = bookDetailViewModel.loadReadingProgress(filePath!!)
                savedChapter?.let { chapter ->
                    seekBar.progress = chapter + 1
                    audioProgressSeekBar.progress = chapter + 1
                    setCurrentPage(chapter)
                }
            }
        }

        mediaPlayer = MediaPlayer()

        audioButton = findViewById(R.id.audioButton)
        audioButton.setOnClickListener {

            toggleAudioSheet()
            isAudioOn = !isAudioOn

            CoroutineScope(Dispatchers.IO).launch {
                synthesizeSpeechAndPlay(webViewText, selectedVoice, selectedSpeed)
            }
        }

        stopAudioButton.setOnClickListener {
            mediaPlayer.stop()
            isAudioOn = false
            toggleAudioSheet()
        }

        playPauseButton.setOnClickListener {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                playPauseButton.setImageResource(R.drawable.ic_play)
            } else {
                mediaPlayer.start()
                playPauseButton.setImageResource(R.drawable.ic_pause)
            }
        }

        forward15Button.setOnClickListener {
            val currentPos = mediaPlayer.currentPosition
            val maxPos = mediaPlayer.duration
            val newPos = (currentPos + 15000).coerceAtMost(maxPos)
            mediaPlayer.seekTo(newPos)
        }

        rewind15Button.setOnClickListener {
            val currentPos = mediaPlayer.currentPosition
            val newPos = (currentPos - 15000).coerceAtMost(0)
            mediaPlayer.seekTo(newPos)
        }

        nextChapterButton.setOnClickListener {
            mediaPlayer.stop()
            val nextPage = currentPage + 1
            if (nextPage < (viewPager.adapter?.count ?: 0))
                viewPager.currentItem = nextPage
        }

        previousChapterButton.setOnClickListener {
            mediaPlayer.stop()
            val previousPage = currentPage - 1
            if (previousPage >= 0)
                viewPager.currentItem = previousPage
        }

        settingsAudioButton.setOnClickListener {
            toggleAudioSettingsSheet()
        }

        backButton.setOnClickListener {
            toggleAudioSettingsSheet()
        }

        voiceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedReadableVoice = voiceOptions[position]
                selectedVoice = voiceMapping[selectedReadableVoice] ?: "alena"
                bookDetailViewModel.saveAudioSettings(selectedVoice, selectedSpeed)
                if (isAudioOn)
                    CoroutineScope(Dispatchers.IO).launch {
                        synthesizeSpeechAndPlay(webViewText, selectedVoice, selectedSpeed)
                    }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        speedSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedSpeed = speedOptions[position]
                bookDetailViewModel.saveAudioSettings(selectedVoice, selectedSpeed)
                if (isAudioOn)
                    CoroutineScope(Dispatchers.IO).launch {
                        synthesizeSpeechAndPlay(webViewText, selectedVoice, selectedSpeed)
                    }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_TOC && resultCode == RESULT_OK) {
            val href = data?.getStringExtra("tocReferenceHref")
            if (href != null) {
                navigateToChapter(href)
            }
        }
    }

    private fun navigateToChapter(href: String) {
        val chapterIndex = pageAdapter.getPageIndexByHref(href)
        if (chapterIndex != -1) {
            viewPager.currentItem = chapterIndex
            currentPage = chapterIndex
        }
    }

    private fun toggleAudioSettingsSheet() {
        if (isAudioSettingsSheetVisible) {
            audioSettingsBottomSheet.visibility = View.GONE
            audioSettingsBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            audioBottomSheet.visibility = View.VISIBLE
            audioBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            isAudioSettingsSheetVisible = false
        } else {
            audioBottomSheet.visibility = View.GONE
            audioBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            audioSettingsBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            audioSettingsBottomSheet.visibility = View.VISIBLE
            isAudioSettingsSheetVisible = true
        }
    }

    private suspend fun synthesizeSpeechAndPlay(text: String, voice: String, speed: String) {
        if (text.isNotBlank()) {
            val synthesisRequestBody = mapOf(
                "text" to text,
                "voice" to voice,
                "format" to "mp3",
                "folderId" to "b1g1ddlkc9drq0ca0cf2",
                "lang" to "ru-RU",
                "speed" to speed,
                "emotion" to "neutral"
            )

            val encodedBody = synthesisRequestBody.entries.joinToString("&") {
                "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}"
            }

            val contentType = "application/x-www-form-urlencoded".toMediaType()
            val requestBody = encodedBody.toRequestBody(contentType)

            val client = OkHttpClient()

            val request = Request.Builder()
                .url("https://tts.api.cloud.yandex.net/speech/v1/tts:synthesize")
                .addHeader("Authorization", "Bearer $IAM_TOKEN")
                .post(requestBody)
                .build()

            try {
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e(
                        "SpeechKit",
                        "Synthesis failed with status: ${response.code} and message: $errorBody"
                    )
                    return
                }

                val audioContent = response.body?.bytes() ?: throw IOException("No response body")
                val outputFile = File(filesDir, "output.mp3")

                FileOutputStream(outputFile).use {
                    it.write(audioContent)
                }

                withContext(Dispatchers.IO) {
                    mediaPlayer.reset()
                    mediaPlayer.setDataSource(outputFile.absolutePath)
                    mediaPlayer.prepare()
                    mediaPlayer.start()
                }
            } catch (e: Exception) {
                Log.e("SpeechKit", "Synthesis error", e)
            }
        }
    }

    override fun onTextExtracted(text: String) {
        webViewText = text
    }

    override fun onWebViewCreated(webView: WebView) {
        currentWebView = webView
    }

    private fun setupViewPager() {
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                mediaPlayer.stop()
                currentPage = position
                updatePageCountText(position) // Обновляем отображение номера страницы

                val currentWebView = getCurrentWebView()

                currentWebView.clearHistory()
                currentWebView.clearCache(true)

                currentWebView?.postDelayed({
                    currentWebView.loadUrl("javascript:window.AndroidInterface.onTextExtracted(document.body.innerText);")
                }, 500)
                val getSelectedTextScript = """
                document.addEventListener("selectionchange", function() {
                var selectedText = window.getSelection().toString();
                if (selectedText.length > 0) {
                    window.AndroidInterface.onTextSelected(selectedText);
                    }
                    });
                """.trimIndent()
                currentWebView.postDelayed({
                    currentWebView.evaluateJavascript(getSelectedTextScript, null)
                }, 1000)

                if (isAudioOn) {
                    CoroutineScope(Dispatchers.IO).launch {
                        synthesizeSpeechAndPlay(webViewText, selectedVoice, selectedSpeed)
                    }
                }
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    private fun toggleAudioSheet() {
        if (isAudioSheetVisible) {
            audioBottomSheet.visibility = View.GONE
            audioBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            bottomSheet.visibility = View.VISIBLE
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            isAudioSheetVisible = false
        } else {
            bottomSheet.visibility = View.GONE
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            audioBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            audioBottomSheet.visibility = View.VISIBLE
            isAudioSheetVisible = true
        }
    }

    override fun onResume() {
        super.onResume()
        filePath?.let { path ->
            CoroutineScope(Dispatchers.Main).launch {
                val savedProgress = bookDetailViewModel.loadReadingProgress(path)
                savedProgress?.let { chapter ->
                    setCurrentPage(chapter)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        CoroutineScope(Dispatchers.IO).launch {
            filePath?.let { path ->
                bookDetailViewModel.saveReadingProgress(path, currentPage)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        CoroutineScope(Dispatchers.IO).launch {
            filePath?.let { path ->
                bookDetailViewModel.saveReadingProgress(path, currentPage)
            }
        }
    }

    override fun onDestroy() {
        if (webView != null) {
            webView!!.clearHistory()
            webView!!.clearCache(true)
            if (webView!!.parent != null) {
                (webView!!.parent as ViewGroup).removeView(webView)
            }
            webView!!.destroy()
            webView = null
        }

        mediaPlayer.release()
        super.onDestroy()
    }

    private fun setCurrentPage(page: Int) {
        viewPager.currentItem = currentPage
        updatePageCountText(currentPage)
        currentPage = page
    }

    private fun toggleBottomSheet(bottomSheetBehavior: BottomSheetBehavior<*>) {
        when (bottomSheetBehavior.state) {
            BottomSheetBehavior.STATE_EXPANDED -> {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                bottomSheetBehavior.setPeekHeight(0, true)
            }

            BottomSheetBehavior.STATE_HIDDEN, BottomSheetBehavior.STATE_COLLAPSED -> {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null) {
            gestureDetector.onTouchEvent(ev)
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_book_detail, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) {
                    searchInWebView(newText)
                }
                return true
            }
        })

        val contentsItem = menu.findItem(R.id.action_contents)
        contentsItem.setOnMenuItemClickListener {
            val intent = Intent(this, TableOfContentsActivity::class.java)
            intent.putExtra("filePath", filePath)
            startActivityForResult(intent, REQUEST_CODE_TOC)
            true
        }
        return true
    }

    private fun searchInWebView(query: String) {
        getCurrentWebView().findAllAsync(query)
    }

    private fun getCurrentWebView(): WebView {
        val position = viewPager.currentItem
        val currentWebView = viewPager.findViewWithTag<WebView>(position)
        return currentWebView
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }

            R.id.action_notes -> {
                val intent = Intent(this, NotesActivity::class.java)
                intent.putExtra("filePath", filePath)
                startActivity(intent)
                return true
            }

            R.id.action_contents -> {
                val intent = Intent(this, TableOfContentsActivity::class.java)
                intent.putExtra("filePath", filePath)
                startActivity(intent)
                return true
            }

            R.id.action_search -> {
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onActionModeStarted(mode: ActionMode?) {
        super.onActionModeStarted(mode)
        mode?.menuInflater?.inflate(R.menu.custom_webview_context_menu, mode.menu)
        val saveToNotesItem = mode?.menu?.findItem(R.id.save_to_notes)

        saveToNotesItem?.setOnMenuItemClickListener {
            saveTextToNotes(currentSelectedText)
            mode.finish()
            true
        }
    }

    private fun saveTextToNotes(text: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val bookId = filePath?.let { bookDao.getBook(it).id }
            val note = bookId?.let { NotesEntity(bookId = it, text = text) }
            if (note != null) {
                notesDao.insertNote(note)
            }
        }
    }

    override fun onTextSelected(text: String) {
        currentSelectedText = text
    }

    private fun updatePageCountText(currentPage: Int) {
        val adapter = viewPager.adapter as? PageAdapter
        val pageCount = adapter?.getPageCount() ?: 0
        chapterNameTextView.text = "Страница ${currentPage + 1} из ${pageCount - 1}"
        seekBar.progress = currentPage + 1
        audioProgressSeekBar.progress = currentPage + 1
    }
}