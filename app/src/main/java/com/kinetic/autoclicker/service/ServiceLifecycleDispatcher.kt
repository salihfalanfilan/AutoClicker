package com.kinetic.autoclicker.service

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy

/**
 * WindowManager üzerinde çalışan ComposeView pencerelerini bir Android Service yaşam döngüsüne
 * güvenle bağlayan ve LifecycleOwner + ViewModelStoreOwner + SavedStateRegistryOwner sağlayan sınıf.
 */
class ServiceLifecycleDispatcher :
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val serviceViewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private var isRestored = false

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = serviceViewModelStore

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    fun onCreate() {
        if (!isRestored) {
            savedStateRegistryController.performRestore(Bundle())
            isRestored = true
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun onPause() {
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }
    }

    fun onResume() {
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
    }

    fun onDestroy() {
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
            if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            }
            if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            }
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        serviceViewModelStore.clear()
    }

    /**
     * ComposeView penceresine LifecycleOwner, ViewModelStoreOwner ve SavedStateRegistryOwner
     * ağaç referanslarını bağlar (IllegalStateException hatasını önler).
     * Ayrıca pencere yönetici ayrılmalarında kompozisyonun yok edilmesini önler.
     */
    fun attachToView(composeView: View) {
        if (composeView is ComposeView) {
            composeView.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(this)
            )
        }
        composeView.setViewTreeLifecycleOwner(this)
        composeView.setViewTreeViewModelStoreOwner(this)
        composeView.setViewTreeSavedStateRegistryOwner(this)
    }
}

typealias ComposeOverlayLifecycleOwner = ServiceLifecycleDispatcher
