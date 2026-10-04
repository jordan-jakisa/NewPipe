package dev.jordanempire.youflow.ui.util

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/** A ViewModel scoped to [key] (one per channel/playlist url) that needs the Application. */
@Composable
inline fun <reified VM : ViewModel> keyedViewModel(key: String, crossinline create: (Application) -> VM): VM = viewModel(
    key = key,
    factory = viewModelFactory { initializer { create(this[APPLICATION_KEY] as Application) } }
)
