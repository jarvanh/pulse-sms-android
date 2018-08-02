package xyz.klinker.messenger.activity.main

import android.support.v4.app.Fragment
import android.view.MenuItem
import android.view.View
import com.miguelcatalan.materialsearchview.MaterialSearchView
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.MessengerActivity
import xyz.klinker.messenger.fragment.SearchFragment

@Suppress("DEPRECATION")
class MainSearchHelper(private val activity: MessengerActivity) : MaterialSearchView.OnQueryTextListener, MaterialSearchView.SearchViewListener {
    
    private val navController
        get() = activity.navController
    
    private val searchView: MaterialSearchView by lazy { activity.findViewById<View>(R.id.search_view) as MaterialSearchView }
    private var searchFragment: SearchFragment? = null
    
    fun setup(item: MenuItem) {
        searchView.setVoiceSearch(false)
        searchView.setBackgroundColor(activity.resources.getColor(R.color.drawerBackground))
        searchView.setOnQueryTextListener(this)
        searchView.setOnSearchViewListener(this)
        
        searchView.setMenuItem(item)
    }
    
    fun closeSearch(): Boolean {
        if (searchView.isSearchOpen) {
            searchView.closeSearch()
            return true
        }
        
        return false
    }
    
    override fun onQueryTextSubmit(query: String): Boolean {
        ensureSearchFragment()
        searchFragment?.search(query)
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        if (newText.isNotEmpty()) {
            // display search fragment
            ensureSearchFragment()
            searchFragment!!.search(newText)
            if (!searchFragment!!.isAdded) {
                displaySearchFragment()
            }
        } else {
            // display conversation fragment
            ensureSearchFragment()
            searchFragment?.search(null)

            if (navController.conversationListFragment != null && !navController.conversationListFragment!!.isAdded) {
                activity.displayConversations()
                activity.fab.hide()
            }
        }

        return true
    }

    override fun onSearchViewShown() {
        activity.fab.hide()
        ensureSearchFragment()
    }

    override fun onSearchViewClosed() {
        ensureSearchFragment()

        if (!searchFragment!!.isSearching) {
            activity.fab.show()

            if (navController.conversationListFragment != null && !navController.conversationListFragment!!.isAdded) {
                activity.displayConversations()
            }
        }
    }

    private fun ensureSearchFragment() {
        if (searchFragment == null) {
            searchFragment = SearchFragment.newInstance()
        }
    }

    private fun displaySearchFragment() {
        navController.otherFragment = null

        if (searchFragment != null) {
            try {
                activity.supportFragmentManager.beginTransaction()
                        .replace(R.id.conversation_list_container, searchFragment!!)
                        .commit()
            } catch (e: Exception) {
            }
        }
    }

}