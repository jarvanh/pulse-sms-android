package xyz.klinker.messenger.fragment.message

import android.app.Activity
import android.view.MenuItem
import android.view.View
import com.miguelcatalan.materialsearchview.MaterialSearchView
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.MessengerActivity
import xyz.klinker.messenger.fragment.SearchFragment

@Suppress("DEPRECATION")
class MessageSearchHelper(private val fragment: MessageListFragment) : MaterialSearchView.OnQueryTextListener, MaterialSearchView.SearchViewListener {
    
    private val activity: MessengerActivity?
        get() = fragment.activity as? MessengerActivity
    
    private lateinit var searchView: MaterialSearchView
    private var searchFragment: SearchFragment? = null
    private val conversationSearchListHolder: View? by lazy { activity?.findViewById<View>(R.id.conversation_search_list) }

    fun setup(item: MenuItem, searchView: MaterialSearchView) {
        this.searchView = searchView

        if (activity != null) {
            searchView.setBackgroundColor(activity!!.resources.getColor(R.color.drawerBackground))
        }

        searchView.setVoiceSearch(false)
        searchView.setOnQueryTextListener(this)
        searchView.setOnSearchViewListener(this)

        searchView.setMenuItem(item)
    }
    
    fun closeSearch(): Boolean {
        if (!this::searchView.isInitialized) return false

        if (searchView.isSearchOpen) {
            searchFragment?.search(null)
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
            searchFragment?.search(newText)
            if (searchFragment?.isAdded == false) {
                displaySearchFragment()
            }
        } else {
            // display message list fragment
            ensureSearchFragment()
            searchFragment?.search(null)
            onSearchViewClosed()
        }

        return true
    }

    override fun onSearchViewShown() {
        ensureSearchFragment()
    }

    override fun onSearchViewClosed() {
        ensureSearchFragment()

        if (searchFragment?.isSearching == false) {
            conversationSearchListHolder?.visibility = View.GONE
            try {
                activity?.supportFragmentManager?.beginTransaction()
                        ?.remove(searchFragment!!)
                        ?.commit()
                searchFragment = null
            } catch (e: Exception) {
            }
        }
    }

    private fun ensureSearchFragment() {
        if (searchFragment == null) {
            searchFragment = SearchFragment.newInstance(fragment.argManager.conversationId, fragment.argManager.color)
        }
    }

    private fun displaySearchFragment() {
        if (searchFragment != null) {
            conversationSearchListHolder?.visibility = View.VISIBLE

            try {
                activity?.supportFragmentManager?.beginTransaction()
                        ?.replace(R.id.conversation_search_list, searchFragment!!)
                        ?.commit()
            } catch (e: Exception) {
            }
        }
    }

}