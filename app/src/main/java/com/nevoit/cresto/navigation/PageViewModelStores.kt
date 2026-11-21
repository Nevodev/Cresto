package com.nevoit.cresto.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * Owns one [ViewModelStore] per navigation entry.
 *
 * A page's store outlives its composition: it is kept while the page is merely covered, and cleared
 * only when the host releases the entry after its exit transition.
 */
class PageViewModelStores : ViewModel() {
    private val owners = mutableMapOf<Long, PageViewModelStoreOwner>()

    fun owner(entryId: Long): ViewModelStoreOwner = owners.getOrPut(entryId) {
        PageViewModelStoreOwner()
    }

    fun remove(entryId: Long) {
        owners.remove(entryId)?.viewModelStore?.clear()
    }

    override fun onCleared() {
        owners.values.forEach { it.viewModelStore.clear() }
        owners.clear()
    }
}

private class PageViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}
