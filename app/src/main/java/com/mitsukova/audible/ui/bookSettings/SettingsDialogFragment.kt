package com.mitsukova.audible.ui.bookSettings

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mitsukova.audible.R
import com.mitsukova.audible.data.database.AudibleDatabase
import com.mitsukova.audible.data.database.BookSettingsDao
import com.mitsukova.audible.data.model.BookSettingsEntity
import com.mitsukova.audible.databinding.SettingsDialogBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsDialogFragment : DialogFragment() {
    private lateinit var viewPager: ViewPager
    private lateinit var binding: SettingsDialogBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private val handler = Handler(Looper.getMainLooper())
    private var autoScrollSpeed = 1.0f
    private var isAutoScrolling = false
    private var autoScrollRunnable: Runnable? = null
    private lateinit var settingsDao: BookSettingsDao

    private var selectedBackgroundColor: String? = null
    private var selectedTextColor: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SettingsDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewPagerId = arguments?.getInt("ViewPager_ID") ?: -1
        if (viewPagerId != -1) {
            viewPager = requireActivity().findViewById(viewPagerId)
        }

        setupBottomSheet()
        setupThemeButtons()
        setupFontSizeButtons()
        setupLineSpacingButtons()
        setupAutoScrollSwitch()
        setupScrollSpeedSpinner()

        val db = AudibleDatabase.getDatabase(requireContext())
        settingsDao = db.bookSettingsDao()

        CoroutineScope(Dispatchers.IO).launch {
            val currentSettings = settingsDao.getBookSettings()

            val fontSize = currentSettings.fontSize
            val lineSpacing = currentSettings.lineSpacing
            val backgroundColor = currentSettings.backgroundColor
            val textColor = currentSettings.textColor
            val autoScrollEnabled = currentSettings.autoScrollEnabled
            val autoScrollSpeed = currentSettings.autoScrollSpeed

            withContext(Dispatchers.Main) {
                binding.fontSizeTextView.text = fontSize.toString()
                binding.lineSpacingTextView.text = lineSpacing.toString()
                applyFontSize(fontSize)
                applyLineSpacing(lineSpacing)

                changeBackgroundColor(backgroundColor, textColor)
                binding.autoScrollSwitch.isChecked = autoScrollEnabled
                binding.scrollSpeedSpinner.setSelection(
                    when (autoScrollSpeed) {
                        0.25f -> 0
                        0.5f -> 1
                        1.0f -> 2
                        1.5f -> 3
                        2.0f -> 4
                        else -> 2
                    }
                )

                if (autoScrollEnabled)
                    startAutoScroll()
                else
                    stopAutoScroll()
            }
        }

        binding.increaseFontSizeButton.setOnClickListener {
            val newFontSize = (binding.fontSizeTextView.text.toString().toInt() + 1)
            binding.fontSizeTextView.text = newFontSize.toString()
            applyFontSize(newFontSize)
            saveSettings()
        }

        binding.increaseLineSpacingButton.setOnClickListener {
            val newLineSpacingSize = (binding.lineSpacingTextView.text.toString().toFloat() + 0.1f)
            binding.lineSpacingTextView.text = String.format("%.1f", newLineSpacingSize)
            applyLineSpacing(newLineSpacingSize)
            saveSettings()
        }

        binding.autoScrollSwitch.setOnCheckedChangeListener { _, isChecked ->
                CoroutineScope(Dispatchers.IO).launch {
                    if (isChecked) {
                        startAutoScroll()
                    } else {
                        stopAutoScroll()
                    }
                    saveSettings()
                }
        }

        binding.scrollSpeedSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val speedMultiplier = when (position) {
                        0 -> 0.25f
                        1 -> 0.5f
                        2 -> 1.0f
                        3 -> 1.5f
                        4 -> 2.0f
                        else -> 1.0f
                    }
                    setAutoScrollSpeed(speedMultiplier)
                    saveSettings()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        setupThemeButtons()
    }


    private fun setupThemeButtons() {
        binding.lightThemeRadioButton.setOnClickListener {
            selectedBackgroundColor = "#FFFFFF"
            selectedTextColor = "#000000"
            changeBackgroundColor(selectedBackgroundColor!!, selectedTextColor!!)
            saveSettings()
        }

        binding.darkThemeRadioButton.setOnClickListener {
            selectedBackgroundColor = "#222222"
            selectedTextColor = "#E1E3E6"
            changeBackgroundColor(selectedBackgroundColor!!, selectedTextColor!!)
            saveSettings()
        }

        binding.sepiaRadioButton.setOnClickListener {
            selectedBackgroundColor = "#FCF6E1"
            selectedTextColor = "#40331B"
            changeBackgroundColor(selectedBackgroundColor!!, selectedTextColor!!)
            saveSettings()
        }

        binding.oldPaperRadioButton.setOnClickListener {
            selectedBackgroundColor = "#E3C798"
            selectedTextColor = "#000000"
            changeBackgroundColor(selectedBackgroundColor!!, selectedTextColor!!)
            saveSettings()
        }
    }

    private fun changeBackgroundColor(backgroundColor: String, textColor: String) {
        val cssContent = """
        <style>
            body {
                background-color: $backgroundColor;
                color: $textColor;
            }
        </style>
        """

        for (i in 0 until viewPager.childCount) {
            (viewPager.getChildAt(i) as? WebView)?.evaluateJavascript(
                """
                        (function() {
                            document.head.insertAdjacentHTML('beforeend', `${cssContent}`);
                        })();
                        """.trimIndent(), null
            )
        }
    }

    private fun saveSettings() {
        CoroutineScope(Dispatchers.IO).launch {
            val fontSize = binding.fontSizeTextView.text.toString().toInt()
            val lineSpacing = binding.lineSpacingTextView.text.toString().toFloat()
            val autoScrollEnabled = binding.autoScrollSwitch.isChecked
            val autoScrollSpeed = when (binding.scrollSpeedSpinner.selectedItemPosition) {
                0 -> 0.25f
                1 -> 0.5f
                2 -> 1.0f
                3 -> 1.5f
                4 -> 2.0f
                else -> 1.0f
            }
            val bookSettingsEntity = selectedBackgroundColor?.let {
                selectedTextColor?.let { it1 ->
                    BookSettingsEntity(
                        id = 0,
                        fontSize = fontSize,
                        lineSpacing = lineSpacing,
                        backgroundColor = it,
                        textColor = it1,
                        autoScrollEnabled = autoScrollEnabled,
                        autoScrollSpeed = autoScrollSpeed
                    )
                }
            }
            if (bookSettingsEntity != null) {
                Log.e("Color", "${bookSettingsEntity.backgroundColor}")
            }

            if (bookSettingsEntity != null) {
                settingsDao.insertBookSettings(bookSettingsEntity)
            }
        }
    }

    private fun setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.settingsBottomSheet)
        bottomSheetBehavior.peekHeight = BottomSheetBehavior.PEEK_HEIGHT_AUTO
        bottomSheetBehavior.isFitToContents = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun setupFontSizeButtons() {
        binding.increaseFontSizeButton.setOnClickListener { changeFontSize(true) }
        binding.decreaseFontSizeButton.setOnClickListener { changeFontSize(false) }
    }

    private fun changeFontSize(increase: Boolean) {
        val currentSize = binding.fontSizeTextView.text.toString().toInt()
        val newSize = if (increase) currentSize + 1 else currentSize - 1
        if (newSize in 10..30) {
            binding.fontSizeTextView.text = newSize.toString()
            applyFontSize(newSize)
        }
    }

    private fun applyFontSize(fontSize: Int) {
        for (i in 0 until viewPager.childCount) {
            val webView = viewPager.getChildAt(i) as? WebView
            webView?.evaluateJavascript("document.body.style.fontSize='${fontSize}px'") {}
        }
    }

    private fun setupLineSpacingButtons() {
        binding.increaseLineSpacingButton.setOnClickListener { changeLineSpacing(true) }
        binding.decreaseLineSpacingButton.setOnClickListener { changeLineSpacing(false) }
    }

    private fun changeLineSpacing(increase: Boolean) {
        val currentSpacing = binding.lineSpacingTextView.text.toString().toFloat()
        val newSpacing = if (increase) currentSpacing + 0.1f else currentSpacing - 0.1f
        if (newSpacing >= 1.0f) {
            binding.lineSpacingTextView.text = String.format("%.1f", newSpacing)
            applyLineSpacing(newSpacing)
        }
    }

    private fun applyLineSpacing(lineSpacing: Float) {
        val customCss = "<style>body { line-height: ${lineSpacing}em; }</style>"
        for (i in 0 until viewPager.childCount) {
            val webView = viewPager.getChildAt(i) as? WebView
            webView?.evaluateJavascript("document.head.innerHTML += '$customCss'") {}
        }
    }

    private fun setupAutoScrollSwitch() {
        binding.autoScrollSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startAutoScroll()
            } else {
                stopAutoScroll()
            }
        }
    }

    private fun startAutoScroll() {
        if (isAutoScrolling) return
        isAutoScrolling = true
        autoScrollRunnable = Runnable {
            for (i in 0 until viewPager.childCount) {
                val webView = viewPager.getChildAt(i) as? WebView
                webView?.evaluateJavascript("window.scrollBy(0, 2)") {}
            }
            handler.postDelayed(autoScrollRunnable!!, (500 * autoScrollSpeed).toLong())
        }
        handler.post(autoScrollRunnable!!)
    }

    private fun stopAutoScroll() {
        if (!isAutoScrolling) return
        isAutoScrolling = false
        if (autoScrollRunnable != null) {
            handler.removeCallbacks(autoScrollRunnable!!)
            autoScrollRunnable = null
        }
    }

    private fun setupScrollSpeedSpinner() {
        val scrollSpeedAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.scroll_speed_options,
            android.R.layout.simple_spinner_item
        )
        scrollSpeedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.scrollSpeedSpinner.adapter = scrollSpeedAdapter

        binding.scrollSpeedSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val speedMultiplier = when (position) {
                    0 -> 0.25f
                    1 -> 0.5f
                    2 -> 1.0f
                    3 -> 1.5f
                    4 -> 2.0f
                    else -> 1.0f
                }
                setAutoScrollSpeed(speedMultiplier)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setAutoScrollSpeed(speedMultiplier: Float) {
        autoScrollSpeed = 1 / speedMultiplier
    }
}
